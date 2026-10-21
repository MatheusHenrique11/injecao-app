package br.com.calibra.app.casos;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import br.com.calibra.app.casos.CasosOficina.ChecklistDaOs;
import br.com.calibra.app.casos.CasosOficina.MarcacaoDoItem;
import br.com.calibra.app.casos.CasosOficina.NecessidadeDoVeiculo;
import br.com.calibra.app.casos.CasosOficina.Pendencia;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.oficina.ChecklistModelo;
import br.com.calibra.core.orcamento.ItemOrcamento;
import br.com.calibra.core.orcamento.ServicoPadrao;
import br.com.calibra.dados.catalogo.CatalogoRepositorio;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;
import br.com.calibra.dados.catalogo.VersaoCatalogo;
import br.com.calibra.dados.oficina.ChecklistRepositorio;
import br.com.calibra.dados.oficina.ItemChecklistSalvo;
import br.com.calibra.dados.oficina.OrdemServicoRepositorio;
import br.com.calibra.dados.oficina.OrdemServicoSalva;
import br.com.calibra.dados.orcamento.OrcamentoRepositorio;
import br.com.calibra.dados.orcamento.OrcamentoSalvo;

/**
 * Os checklists da OS (SPEC 6.5, Fase 3B, bloco 3B.3), dentro da transação de quem chama.
 *
 * <ul>
 *   <li><b>A cópia:</b> cada serviço do catálogo da última revisão aprovada ganha a cópia da versão de agora do modelo
 *   que o serviço tem no catálogo (o arquivado também: o serviço ainda aponta para ele). A linha digitada não tem
 *   checklist. Uma cópia só por serviço: copiar de novo só cria as que faltam.</li>
 *   <li><b>O que trava o "pronto":</b> os itens obrigatórios pendentes das cópias dos serviços que estão na última
 *   revisão aprovada. A cópia do serviço que saiu do orçamento numa revisão nova fica guardada, mas não trava.</li>
 *   <li><b>O veículo</b> (bloco 3B.6): sem a entrada, só os checklists dos serviços de bancada (ex.: a confecção de
 *   chicote) se marcam; os outros ficam bloqueados até a entrada.</li>
 * </ul>
 */
final class ChecklistsDaOs {

    private final CatalogoRepositorio catalogo;
    private final OrcamentoRepositorio orcamentos;
    private final ChecklistRepositorio checklists;
    private final OrdemServicoRepositorio ordens;

    ChecklistsDaOs(Clock relogio) {
        this.catalogo = new CatalogoRepositorio(relogio);
        this.orcamentos = new OrcamentoRepositorio(relogio);
        this.checklists = new ChecklistRepositorio(relogio);
        this.ordens = new OrdemServicoRepositorio(relogio);
    }

    /**
     * Cria as cópias que faltam (ao entrar em execução e quando uma revisão é aprovada com o serviço já começado).
     *
     * @return quantas cópias foram criadas
     */
    int copiar(Connection c, long osId) throws SQLException {
        Set<Long> comCopia = new LinkedHashSet<>();
        checklists.daOs(c, osId).forEach(copia -> comCopia.add(copia.servicoItemId()));
        int criadas = 0;
        for (long servicoId : servicosAprovados(c, osId)) {
            if (comCopia.contains(servicoId)) {
                continue;
            }
            Optional<ItemCatalogo<ServicoPadrao>> servico = catalogo.buscar(c, TipoCatalogo.SERVICO, servicoId);
            Long modeloId = servico.map(s -> s.dados().checklistModeloId()).orElse(null);
            if (modeloId == null) {
                continue;
            }
            Optional<ItemCatalogo<ChecklistModelo>> modelo = catalogo.buscar(c, TipoCatalogo.CHECKLIST, modeloId);
            if (modelo.isPresent()) {
                checklists.copiar(c, osId, servicoId, servico.get().info().nome(), modeloId, modelo.get().versao(),
                        modelo.get().dados());
                criadas++;
            }
        }
        return criadas;
    }

    /**
     * As cópias da OS, na ordem em que foram feitas, cada uma dizendo se o serviço está na última revisão aprovada e se
     * está bloqueada (sem a entrada do veículo, o serviço que não é de bancada).
     */
    List<ChecklistDaOs> daOs(Connection c, long osId) throws SQLException {
        Set<Long> aprovados = servicosAprovados(c, osId);
        Set<Long> liberados = liberadosSemEntrada(c, osId);
        return checklists.daOs(c, osId).stream()
                .map(copia -> new ChecklistDaOs(copia, aprovados.contains(copia.servicoItemId()),
                        liberados != null && !liberados.contains(copia.servicoItemId()))).toList();
    }

    /**
     * O que os serviços do catálogo da última revisão aprovada pedem do veículo, cada um como está na versão do
     * orçamento (como a garantia, decisão 231). A linha digitada não conta.
     */
    NecessidadeDoVeiculo necessidadeDoVeiculo(Connection c, long osId) throws SQLException {
        Set<Long> bancada = new LinkedHashSet<>();
        List<String> comVeiculo = new ArrayList<>();
        Optional<OrcamentoSalvo> aprovado = orcamentos.ultimoAprovado(c, osId);
        if (aprovado.isEmpty()) {
            return new NecessidadeDoVeiculo(bancada, comVeiculo);
        }
        for (ItemOrcamento servico : aprovado.get().conteudo().servicos()) {
            if (servico.catalogoItemId() == null || servico.catalogoVersao() == null) {
                continue;
            }
            Optional<ServicoPadrao> dados = catalogo.versao(c, TipoCatalogo.SERVICO, servico.catalogoItemId(),
                    servico.catalogoVersao()).map(VersaoCatalogo::dados);
            if (dados.isEmpty()) {
                continue;
            }
            if (!dados.get().exigeEntradaDoVeiculo()) {
                bancada.add(servico.catalogoItemId());
            } else if (!comVeiculo.contains(servico.descricao())) {
                comVeiculo.add(servico.descricao());
            }
        }
        return new NecessidadeDoVeiculo(bancada, comVeiculo);
    }

    /** Sem a entrada do veículo, os serviços de bancada, os únicos com o checklist liberado; null com a entrada. */
    private Set<Long> liberadosSemEntrada(Connection c, long osId) throws SQLException {
        boolean chegou = ordens.buscar(c, osId).map(OrdemServicoSalva::veiculoChegou).orElse(true);
        return chegou ? null : necessidadeDoVeiculo(c, osId).bancada();
    }

    /** Os itens obrigatórios que ainda travam o "pronto", na ordem das cópias e dos itens. */
    List<Pendencia> pendencias(Connection c, long osId) throws SQLException {
        return pendencias(c, osId, null);
    }

    /**
     * Os itens obrigatórios pendentes das cópias criadas depois desta hora: as que uma revisão aprovada trouxe com a OS
     * já pronta, que a trava do "pronto" não viu (versão 0.3.2, decisão 278).
     */
    List<Pendencia> pendenciasCriadasDepois(Connection c, long osId, LocalDateTime hora) throws SQLException {
        return pendencias(c, osId, Objects.requireNonNull(hora, "hora"));
    }

    /** @param depoisDe null = todas as cópias */
    private List<Pendencia> pendencias(Connection c, long osId, LocalDateTime depoisDe) throws SQLException {
        List<Pendencia> pendencias = new ArrayList<>();
        for (ChecklistDaOs checklist : daOs(c, osId)) {
            if (!checklist.noOrcamento() || (depoisDe != null && !checklist.checklist().criadoEm().isAfter(depoisDe))) {
                continue;
            }
            for (ItemChecklistSalvo item : checklist.checklist().itens()) {
                if (item.item().pendente()) {
                    pendencias.add(new Pendencia(checklist.checklist().servico(), item.item().texto()));
                }
            }
        }
        return pendencias;
    }

    /**
     * Grava as marcações que mudaram, com quem marcou e agora; as iguais ao gravado ficam como estão (sem mudar quem
     * marcou). Com um erro, nada é gravado.
     *
     * @return quantos itens mudaram
     * @throws EntradaInvalidaException com todos os erros ("não se aplica" sem justificativa, item de outra OS)
     */
    int marcar(Connection c, long osId, List<MarcacaoDoItem> marcacoes, Long mecanicoId) throws SQLException {
        Map<Long, ItemChecklistSalvo> itens = new HashMap<>();
        Map<Long, String> servicos = new HashMap<>();
        Set<Long> bloqueados = new HashSet<>();
        for (ChecklistDaOs copia : daOs(c, osId)) {
            for (ItemChecklistSalvo item : copia.checklist().itens()) {
                itens.put(item.id(), item);
                servicos.put(item.id(), copia.checklist().servico());
                if (copia.bloqueado()) {
                    bloqueados.add(item.id());
                }
            }
        }
        List<String> erros = new ArrayList<>();
        Set<String> semEntrada = new LinkedHashSet<>();
        for (MarcacaoDoItem marcacao : marcacoes) {
            ItemChecklistSalvo item = itens.get(marcacao.itemId());
            if (item == null) {
                erros.add("O item #" + marcacao.itemId() + " não é do checklist desta OS.");
            } else if (bloqueados.contains(item.id())) {
                semEntrada.add("O checklist de " + servicos.get(item.id()) + " fica bloqueado até a entrada do "
                        + "veículo (aba Entrada).");
            } else {
                erros.addAll(marcacao.marcacao().validar(item.item().texto() + " (" + servicos.get(item.id()) + ")"));
            }
        }
        erros.addAll(semEntrada);
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
        int mudaram = 0;
        for (MarcacaoDoItem marcacao : marcacoes) {
            if (!itens.get(marcacao.itemId()).item().marcacao().equals(marcacao.marcacao())) {
                checklists.marcar(c, marcacao.itemId(), marcacao.marcacao(), mecanicoId);
                mudaram++;
            }
        }
        return mudaram;
    }

    /** Os serviços do catálogo da última revisão aprovada, na ordem do orçamento e sem repetir; vazio sem aprovação. */
    private Set<Long> servicosAprovados(Connection c, long osId) throws SQLException {
        Set<Long> ids = new LinkedHashSet<>();
        orcamentos.ultimoAprovado(c, osId).ifPresent(aprovado -> aprovado.conteudo().servicos().stream()
                .map(ItemOrcamento::catalogoItemId).filter(Objects::nonNull).forEach(ids::add));
        return ids;
    }
}
