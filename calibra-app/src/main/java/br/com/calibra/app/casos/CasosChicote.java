package br.com.calibra.app.casos;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import br.com.calibra.app.casos.CasosOficina.ChecklistDaOs;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.cadastro.Cliente;
import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.core.oficina.FichaChicote;
import br.com.calibra.core.oficina.SituacaoChicote;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.dados.BancoDados;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.cadastro.ClienteRepositorio;
import br.com.calibra.dados.cadastro.VeiculoRepositorio;
import br.com.calibra.dados.catalogo.CatalogoRepositorio;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;
import br.com.calibra.dados.oficina.FichaChicoteRepositorio;
import br.com.calibra.dados.oficina.FichaChicoteSalva;
import br.com.calibra.dados.oficina.ItemChecklistSalvo;
import br.com.calibra.dados.oficina.OrdemServicoRepositorio;
import br.com.calibra.dados.oficina.OrdemServicoSalva;
import br.com.calibra.dados.projeto.MotorRepositorio;
import br.com.calibra.dados.projeto.ProjetoRepositorio;
import br.com.calibra.dados.projeto.VersaoMotor;
import br.com.calibra.relatorios.DadosFichaChicote;
import br.com.calibra.relatorios.DadosFichaChicote.ItemDeBancada;

/**
 * As fichas de chicote da OS (Fase 3B, SPEC 6.6.5): a ficha nova já preenchida pelo projeto de preparação (a injeção e
 * o motor), criar, salvar, a versão nova (a anterior fica só para ver, teste nº 13 da fase) e apagar, as fichas da OS e
 * as do veículo, os testes de bancada (o checklist da OS) e os dados do PDF. A ficha se muda até a entrega; depois, só
 * se lê, e a situação "entregue" vem com a entrega. Sem JavaFX.
 */
public final class CasosChicote {

    /** Uma injeção do catálogo, para a escolha. */
    public record OpcaoInjecao(long id, String nome) {
    }

    /** Uma versão do motor do projeto de preparação da OS, para a escolha ("v2 · AP 2.0 turbo"). */
    public record OpcaoMotor(long id, String rotulo) {
    }

    /**
     * O que a janela oferece para escolher.
     *
     * @param injecoes as injeções do catálogo que não estão arquivadas, por nome
     * @param motores as versões do motor do projeto da OS, da mais nova para a mais antiga; vazia sem projeto
     */
    public record Opcoes(List<OpcaoInjecao> injecoes, List<OpcaoMotor> motores) {

        public Opcoes {
            injecoes = List.copyOf(injecoes);
            motores = List.copyOf(motores);
        }
    }

    private final BancoDados banco;
    private final Clock relogio;
    private final FichaChicoteRepositorio fichas;
    private final OrdemServicoRepositorio ordens;
    private final CatalogoRepositorio catalogo;
    private final ProjetoRepositorio projetos;
    private final MotorRepositorio motores;
    private final ClienteRepositorio clientes;
    private final VeiculoRepositorio veiculos;
    private final ChecklistsDaOs checklists;

    public CasosChicote(BancoDados banco, Clock relogio) {
        this.banco = Objects.requireNonNull(banco, "banco");
        this.relogio = Objects.requireNonNull(relogio, "relogio");
        this.fichas = new FichaChicoteRepositorio(relogio);
        this.ordens = new OrdemServicoRepositorio(relogio);
        this.catalogo = new CatalogoRepositorio(relogio);
        this.projetos = new ProjetoRepositorio(relogio);
        this.motores = new MotorRepositorio(relogio);
        this.clientes = new ClienteRepositorio(relogio);
        this.veiculos = new VeiculoRepositorio(relogio);
        this.checklists = new ChecklistsDaOs(relogio);
    }

    // ===================== Consultas =====================

    /** As fichas da OS, na ordem em que foram feitas (a versão nova depois da anterior). */
    public List<FichaChicoteSalva> daOs(long osId) {
        return banco.ler(c -> fichas.daOs(c, osId));
    }

    /** As fichas de todas as OS do veículo, da mais nova para a mais antiga (para a ficha do veículo). */
    public List<FichaChicoteSalva> doVeiculo(long veiculoId) {
        return banco.ler(c -> fichas.doVeiculo(c, veiculoId));
    }

    public Optional<FichaChicoteSalva> buscar(long id) {
        return banco.ler(c -> fichas.buscar(c, id));
    }

    /** As injeções do catálogo e as versões do motor do projeto da OS, para a janela. */
    public Opcoes opcoes(long osId) {
        return banco.ler(c -> {
            OrdemServicoSalva os = exigirOs(c, osId);
            List<OpcaoInjecao> injecoes = catalogo.listar(c, TipoCatalogo.INJECAO, "", false).stream()
                    .map(item -> new OpcaoInjecao(item.id(), item.info().nome())).toList();
            List<OpcaoMotor> versoes = new ArrayList<>();
            if (os.projetoId() != null) {
                for (VersaoMotor versao : motores.versoes(c, os.projetoId())) {
                    versoes.add(new OpcaoMotor(versao.id(), rotulo(versao)));
                }
            }
            return new Opcoes(injecoes, versoes);
        });
    }

    /**
     * A ficha nova, já preenchida: a versão "v1", em confecção e, com o projeto de preparação ligado à OS, a injeção
     * dele e a versão de agora do motor. Com a OS entregue ou cancelada, é recusada.
     */
    public FichaChicote nova(long osId) {
        return banco.ler(c -> {
            OrdemServicoSalva os = exigirEmAndamento(c, osId, "não ganha ficha do chicote nova.");
            String injecao = "";
            Long injecaoId = null;
            Long motor = null;
            if (os.projetoId() != null) {
                injecaoId = projetos.buscar(c, os.projetoId()).map(p -> p.injecaoItemId()).orElse(null);
                if (injecaoId != null) {
                    Optional<ItemCatalogo<Injecao>> doCatalogo = catalogo.buscar(c, TipoCatalogo.INJECAO, injecaoId);
                    injecao = doCatalogo.map(item -> item.info().nome()).orElse("");
                    injecaoId = doCatalogo.isPresent() ? injecaoId : null;
                }
                motor = motores.atual(c, os.projetoId()).map(VersaoMotor::id).orElse(null);
            }
            return new FichaChicote(injecao, injecaoId, motor, FichaChicote.PRIMEIRA_VERSAO, "", List.of(), "", "",
                    SituacaoChicote.EM_CONFECCAO);
        });
    }

    /** A ficha da versão nova, começando desta (ainda não gravada): a próxima versão, em confecção. */
    public FichaChicote versaoNova(long id) {
        return banco.ler(c -> {
            FichaChicoteSalva salva = exigir(c, id);
            exigirEmAndamento(c, salva.osId(), "não ganha versão nova do chicote.");
            exigirUltimaVersao(salva);
            return salva.ficha().versaoNova();
        });
    }

    /**
     * Os testes de bancada da OS: os itens do checklist dos serviços de bancada (a confecção do chicote, que não exige
     * o veículo), como estão marcados na aba Execução. Vazio sem checklist de bancada.
     */
    public List<ItemDeBancada> bancada(long osId) {
        return banco.ler(c -> bancada(c, osId));
    }

    /**
     * Os dados do PDF da ficha: a oficina e a versão, como estão agora, e o cliente como está no cadastro (anonimizado,
     * se for o caso).
     *
     * @param logo o arquivo do logo da oficina; null sem logo
     */
    public DadosFichaChicote documento(long id, DadosOficina oficina, Path logo, String versaoApp) {
        return banco.ler(c -> {
            FichaChicoteSalva salva = exigir(c, id);
            OrdemServicoSalva os = ordens.buscar(c, salva.osId()).orElseThrow();
            Cliente cliente = clientes.buscar(c, os.clienteId()).orElseThrow().cliente();
            Long motorId = salva.ficha().motorVersaoId();
            String motor = motorId == null ? "" : motores.buscar(c, motorId).map(CasosChicote::rotulo).orElse("");
            return new DadosFichaChicote(oficina, logo, os.numero().texto(), cliente.nome(),
                    CasosOrcamento.textoVeiculo(veiculos.buscar(c, os.veiculoId()).orElseThrow().veiculo()), motor,
                    salva.ficha(), bancada(c, salva.osId()), versaoApp,
                    LocalDateTime.now(relogio).withSecond(0).withNano(0));
        });
    }

    // ===================== Gravação =====================

    /**
     * Ficha nova na OS (em qualquer etapa antes da entrega).
     *
     * @return o id da ficha
     * @throws EntradaInvalidaException com tudo o que falta ou está errado (nada é gravado)
     */
    public long criar(long osId, FichaChicote ficha) {
        exigirValida(ficha);
        return banco.transacao(c -> {
            OrdemServicoSalva os = exigirEmAndamento(c, osId, "não ganha ficha do chicote nova.");
            exigirEscolhas(c, os, ficha);
            return fichas.inserir(c, osId, null, ficha);
        });
    }

    /** A versão nova, começando da anterior, que fica guardada, só para ver (teste nº 13 da fase). */
    public long criarVersaoNova(long anteriorId, FichaChicote ficha) {
        exigirValida(ficha);
        return banco.transacao(c -> {
            FichaChicoteSalva anterior = exigir(c, anteriorId);
            OrdemServicoSalva os = exigirEmAndamento(c, anterior.osId(), "não ganha versão nova do chicote.");
            exigirUltimaVersao(anterior);
            exigirEscolhas(c, os, ficha);
            return fichas.inserir(c, anterior.osId(), anteriorId, ficha);
        });
    }

    /** Grava a ficha como está agora (até a entrega; a versão que já tem uma nova não muda). */
    public void salvar(long id, FichaChicote ficha) {
        exigirValida(ficha);
        banco.transacao(c -> {
            FichaChicoteSalva salva = exigir(c, id);
            OrdemServicoSalva os = exigirEmAndamento(c, salva.osId(), "a ficha do chicote não muda mais.");
            exigirUltimaVersao(salva);
            exigirEscolhas(c, os, ficha);
            fichas.salvar(c, id, ficha);
            return null;
        });
    }

    /** Apaga a ficha feita por engano: só a última versão, com a OS em andamento (a anterior nunca sai). */
    public void apagar(long id) {
        banco.transacao(c -> {
            FichaChicoteSalva salva = exigir(c, id);
            exigirEmAndamento(c, salva.osId(), "a ficha do chicote não é apagada.");
            exigirUltimaVersao(salva);
            fichas.apagar(c, id);
            return null;
        });
    }

    // ===================== Regras =====================

    private static void exigirValida(FichaChicote ficha) {
        List<String> erros = ficha == null ? List.of("Preencha a ficha do chicote.") : new ArrayList<>(ficha.validar());
        if (ficha != null && ficha.situacao() == SituacaoChicote.ENTREGUE) {
            erros.add("A situação \"entregue\" vem sozinha quando a OS é entregue: escolha em confecção ou testado.");
        }
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
    }

    /** A injeção escolhida precisa estar no catálogo, e a versão do motor, no projeto de preparação da OS. */
    private void exigirEscolhas(Connection c, OrdemServicoSalva os, FichaChicote ficha) throws SQLException {
        if (ficha.injecaoItemId() != null
                && catalogo.buscar(c, TipoCatalogo.INJECAO, ficha.injecaoItemId()).isEmpty()) {
            throw new OperacaoRecusadaException("A injeção escolhida não está no catálogo de injeções.");
        }
        if (ficha.motorVersaoId() != null) {
            boolean doProjeto = os.projetoId() != null && motores.buscar(c, ficha.motorVersaoId())
                    .map(versao -> versao.projetoId() == os.projetoId()).orElse(false);
            if (!doProjeto) {
                throw new OperacaoRecusadaException("A versão do motor precisa ser do projeto de preparação da OS.");
            }
        }
    }

    private static void exigirUltimaVersao(FichaChicoteSalva salva) {
        if (salva.temVersaoNova()) {
            throw new OperacaoRecusadaException("A versão " + salva.ficha().versaoChicote() + " do chicote já tem "
                    + "uma versão nova: ela fica só para ver.");
        }
    }

    private OrdemServicoSalva exigirEmAndamento(Connection c, long osId, String oQue) throws SQLException {
        OrdemServicoSalva os = exigirOs(c, osId);
        if (os.status().finalizada()) {
            throw new OperacaoRecusadaException("A OS " + os.numero().texto() + " está \"" + os.status().rotulo()
                    + "\": " + oQue);
        }
        return os;
    }

    private OrdemServicoSalva exigirOs(Connection c, long osId) throws SQLException {
        return ordens.buscar(c, osId).orElseThrow(
                () -> new OperacaoRecusadaException("OS #" + osId + " não encontrada."));
    }

    private FichaChicoteSalva exigir(Connection c, long id) throws SQLException {
        return fichas.buscar(c, id).orElseThrow(
                () -> new OperacaoRecusadaException("Ficha do chicote #" + id + " não encontrada."));
    }

    private List<ItemDeBancada> bancada(Connection c, long osId) throws SQLException {
        Set<Long> deBancada = checklists.necessidadeDoVeiculo(c, osId).bancada();
        List<ItemDeBancada> itens = new ArrayList<>();
        for (ChecklistDaOs checklist : checklists.daOs(c, osId)) {
            if (!deBancada.contains(checklist.checklist().servicoItemId())) {
                continue;
            }
            for (ItemChecklistSalvo item : checklist.checklist().itens()) {
                itens.add(new ItemDeBancada(item.item().texto(), item.item().marcacao().situacao(),
                        item.item().marcacao().observacao(), item.item().marcacao().justificativa(), item.marcadoPor(),
                        item.marcadoEm()));
            }
        }
        return itens;
    }

    /** "v2 · AP 2.0 turbo", ou só "v2" sem descrição. */
    private static String rotulo(VersaoMotor versao) {
        String descricao = versao.descricao() == null ? "" : versao.descricao().strip();
        return "v" + versao.numero() + (descricao.isEmpty() ? "" : " · " + descricao);
    }
}
