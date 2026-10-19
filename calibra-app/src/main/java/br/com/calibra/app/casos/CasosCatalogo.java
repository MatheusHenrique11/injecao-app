package br.com.calibra.app.casos;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import br.com.calibra.app.catalogo.Combustiveis;
import br.com.calibra.app.catalogo.FormasCatalogo;
import br.com.calibra.app.catalogo.FormularioSugeridos;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.combustivel.Combustivel;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.ItemMaterial;
import br.com.calibra.core.orcamento.ItemOrcamento;
import br.com.calibra.core.orcamento.Kit;
import br.com.calibra.core.orcamento.ServicoPadrao;
import br.com.calibra.dados.BancoDados;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.catalogo.CatalogoRepositorio;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.ResumoItem;
import br.com.calibra.dados.catalogo.TipoCatalogo;
import br.com.calibra.dados.catalogo.VersaoCatalogo;

/**
 * Casos de uso dos catálogos (SPEC 8.5): listar, criar, editar (cada gravação com mudança vira versão nova),
 * arquivar e reativar. Confere as regras do formulário antes de gravar; nada é apagado. Sem JavaFX.
 */
public final class CasosCatalogo {

    /** Resultado de salvar: o item e se uma versão nova foi gravada (sem mudança, nada é gravado). */
    public record Gravacao<T>(ItemCatalogo<T> item, boolean versaoNova) {
    }

    /**
     * As linhas de um kit para o orçamento e o que conferir nelas.
     *
     * @param avisos o item ou o serviço arquivado no catálogo (a linha entra mesmo assim)
     */
    public record LinhasDoKit(List<ItemOrcamento> itens, List<String> avisos) {

        public LinhasDoKit {
            itens = List.copyOf(itens);
            avisos = List.copyOf(avisos);
        }
    }

    private final BancoDados banco;
    private final CatalogoRepositorio catalogo;

    public CasosCatalogo(BancoDados banco, Clock relogio) {
        this.banco = Objects.requireNonNull(banco, "banco");
        this.catalogo = new CatalogoRepositorio(relogio);
    }

    // ===================== Consultas =====================

    /** Itens em ordem de nome; cada palavra do termo precisa aparecer no nome, na fonte ou nas observações. */
    public <T> List<ItemCatalogo<T>> listar(TipoCatalogo<T> tipo, String termo, boolean incluirArquivados) {
        return banco.ler(c -> catalogo.listar(c, tipo, termo, incluirArquivados));
    }

    /** Os itens que podem ser escolhidos: os arquivados ficam de fora. */
    public <T> List<ItemCatalogo<T>> ativos(TipoCatalogo<T> tipo) {
        return listar(tipo, "", false);
    }

    public <T> Optional<ItemCatalogo<T>> buscar(TipoCatalogo<T> tipo, long id) {
        return banco.ler(c -> catalogo.buscar(c, tipo, id));
    }

    /** Todas as versões do item, da mais nova para a mais antiga. */
    public <T> List<VersaoCatalogo<T>> versoes(TipoCatalogo<T> tipo, long id) {
        return banco.ler(c -> catalogo.versoes(c, tipo, id));
    }

    /**
     * As linhas de um kit para o orçamento (SPEC 6.6.4, Fase 3B): uma para cada item e serviço dele, com a versão de
     * agora do catálogo e a quantidade do kit. O item entra como peça, pelo preço de referência; o serviço, pela regra
     * dele (preço próprio, ou horas × o valor da hora). O que estiver arquivado no catálogo entra mesmo assim, com um
     * aviso para conferir. O kit não muda.
     *
     * @param valorHora o valor da hora de Configurações; null = não configurado (o serviço por hora fica sem valor)
     */
    public LinhasDoKit linhasDoKit(long kitId, Dinheiro valorHora) {
        return banco.ler(c -> {
            ItemCatalogo<Kit> kit = catalogo.buscar(c, TipoCatalogo.KIT, kitId).orElseThrow(
                    () -> new OperacaoRecusadaException("Kit #" + kitId + " não encontrado."));
            String nomeDoKit = kit.info().nome();
            List<ItemOrcamento> itens = new ArrayList<>();
            List<String> avisos = new ArrayList<>();
            for (Kit.Linha linha : kit.dados().linhas()) {
                Optional<? extends ItemCatalogo<?>> noCatalogo;
                if (linha.tipo() == Kit.TipoLinha.ITEM) {
                    Optional<ItemCatalogo<ItemMaterial>> item = catalogo.buscar(c, TipoCatalogo.ITEM, linha.itemId());
                    item.ifPresent(i -> itens.add(ItemOrcamento.doItem(i.info().nome(), i.dados().unidade(),
                            i.info().precoReferencia(), i.id(), i.versao()).multiplicarQuantidade(linha.quantidade())));
                    noCatalogo = item;
                } else {
                    Optional<ItemCatalogo<ServicoPadrao>> servico = catalogo.buscar(c, TipoCatalogo.SERVICO,
                            linha.itemId());
                    servico.ifPresent(s -> itens.add(ItemOrcamento.doServico(s.dados().textoNoOrcamento(),
                            s.dados().horas(), s.info().precoReferencia(), valorHora, s.id(), s.versao())
                            .multiplicarQuantidade(linha.quantidade())));
                    noCatalogo = servico;
                }
                if (noCatalogo.isEmpty()) {
                    avisos.add("Kit " + nomeDoKit + ": o " + linha.tipo().rotulo() + " \"" + linha.nome()
                            + "\" não está mais no catálogo e ficou de fora.");
                } else if (noCatalogo.get().arquivado()) {
                    avisos.add("Kit " + nomeDoKit + ": o " + linha.tipo().rotulo() + " \""
                            + noCatalogo.get().info().nome() + "\" está arquivado no catálogo; confira a linha.");
                }
            }
            return new LinhasDoKit(itens, avisos);
        });
    }

    /** Quais destes ids são do tipo (ex.: as linhas do orçamento que vieram do catálogo de Itens). */
    public Set<Long> doTipo(TipoCatalogo<?> tipo, Collection<Long> ids) {
        return banco.ler(c -> catalogo.resumos(c, ids).values().stream()
                .filter(resumo -> resumo.tipo().equals(tipo.codigo())).map(ResumoItem::id)
                .collect(Collectors.toSet()));
    }

    /** Os combustíveis que podem ser escolhidos, em ordem de nome. */
    public List<ItemCatalogo<Combustivel>> combustiveis() {
        return ativos(TipoCatalogo.COMBUSTIVEL);
    }

    /** O combustível pelo código, mesmo arquivado (um motor antigo pode usar um combustível arquivado). */
    public Optional<ItemCatalogo<Combustivel>> combustivel(String codigo) {
        return listar(TipoCatalogo.COMBUSTIVEL, "", true).stream()
                .filter(item -> item.dados().codigo().equals(codigo))
                .findFirst();
    }

    /** Os combustíveis do formulário do motor, lidos do catálogo a cada vez (sempre na versão atual). */
    public Combustiveis combustiveisDoFormulario() {
        return new Combustiveis() {
            @Override
            public List<Combustivel> ativos() {
                return combustiveis().stream().map(ItemCatalogo::dados).toList();
            }

            @Override
            public Optional<Combustivel> porCodigo(String codigo) {
                return combustivel(codigo).map(ItemCatalogo::dados);
            }
        };
    }

    // ===================== Gravação =====================

    /**
     * Item novo, na versão 1.
     *
     * @throws EntradaInvalidaException se não passar nas regras (ou se o código do combustível já existir)
     */
    public <T> ItemCatalogo<T> criar(TipoCatalogo<T> tipo, T dados, InfoCatalogo info) {
        conferir(tipo, dados, info);
        return banco.transacao(c -> {
            if (dados instanceof Combustivel combustivel) {
                Optional<ItemCatalogo<Combustivel>> mesmoCodigo = catalogo.listar(c, TipoCatalogo.COMBUSTIVEL, "", true)
                        .stream().filter(item -> item.dados().codigo().equals(combustivel.codigo())).findFirst();
                if (mesmoCodigo.isPresent()) {
                    throw new EntradaInvalidaException("Já existe um combustível com o código " + combustivel.codigo()
                            + " (" + mesmoCodigo.get().info().nome() + "). Use outro código.");
                }
            }
            return catalogo.inserir(c, tipo, comOCatalogoDeAgora(c, tipo, dados), info, null);
        });
    }

    /**
     * Grava uma versão nova do item se algo mudou; a anterior fica guardada como estava.
     *
     * @throws EntradaInvalidaException se não passar nas regras ou se o código do combustível mudou
     */
    public <T> Gravacao<T> salvar(TipoCatalogo<T> tipo, long id, T dados, InfoCatalogo info) {
        conferir(tipo, dados, info);
        return banco.transacao(c -> {
            ItemCatalogo<T> atual = catalogo.buscar(c, tipo, id).orElseThrow(() -> new OperacaoRecusadaException(
                    "Item de catálogo #" + id + " (" + tipo.singular() + ") não encontrado."));
            T conferidos = comOCatalogoDeAgora(c, tipo, dados);
            if (tipo.comVersao(conferidos, atual.versao()).equals(atual.dados()) && info.equals(atual.info())) {
                return new Gravacao<>(atual, false);
            }
            if (conferidos instanceof Combustivel novo && atual.dados() instanceof Combustivel antigo
                    && !novo.codigo().equals(antigo.codigo())) {
                throw new EntradaInvalidaException("O código do combustível não muda depois de criado (os motores "
                        + "guardam o código " + antigo.codigo() + "). Para outro código, use Duplicar.");
            }
            return new Gravacao<>(catalogo.novaVersao(c, tipo, id, conferidos, info), true);
        });
    }

    /**
     * No kit (Fase 3B), cada linha precisa apontar para um item ou um serviço do catálogo, e o nome guardado em cada
     * linha (só para mostrar) passa a ser o de agora. Os outros tipos passam como vieram.
     *
     * @throws EntradaInvalidaException com as linhas que não estão no catálogo
     */
    private <T> T comOCatalogoDeAgora(Connection c, TipoCatalogo<T> tipo, T dados) throws SQLException {
        if (!(dados instanceof Kit kit)) {
            return dados;
        }
        List<String> erros = new ArrayList<>();
        List<Kit.Linha> linhas = new ArrayList<>();
        for (int i = 0; i < kit.linhas().size(); i++) {
            Kit.Linha linha = kit.linhas().get(i);
            Optional<String> nome = linha.tipo() == Kit.TipoLinha.ITEM
                    ? catalogo.buscar(c, TipoCatalogo.ITEM, linha.itemId()).map(item -> item.info().nome())
                    : catalogo.buscar(c, TipoCatalogo.SERVICO, linha.itemId()).map(item -> item.info().nome());
            if (nome.isEmpty()) {
                erros.add("Linha " + (i + 1) + (linha.nome().isEmpty() ? "" : " (" + linha.nome() + ")") + ": esse "
                        + linha.tipo().rotulo() + " não está no catálogo; escolha de novo.");
            } else {
                linhas.add(new Kit.Linha(linha.tipo(), linha.itemId(), nome.get(), linha.quantidade()));
            }
        }
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
        return tipo.converter(new Kit(kit.nome(), linhas, kit.fonte(), kit.data()));
    }

    /**
     * Os serviços sugeridos escolhidos (SPEC 6.6.1), todos numa gravação só. Se um deles não passar nas regras do
     * catálogo, ou se já existir um serviço com o mesmo nome (sem diferença de acento ou maiúscula, também os
     * arquivados), nada é gravado.
     *
     * @throws EntradaInvalidaException com todos os problemas
     */
    public List<ItemCatalogo<ServicoPadrao>> adicionarServicosSugeridos(List<FormularioSugeridos.Novo> novos) {
        List<String> erros = new ArrayList<>();
        if (novos.isEmpty()) {
            erros.add("Marque ao menos um serviço e preencha as horas ou o preço dele.");
        }
        for (FormularioSugeridos.Novo novo : novos) {
            FormasCatalogo.de(TipoCatalogo.SERVICO).validar(novo.dados(), novo.info())
                    .forEach(erro -> erros.add(novo.info().nome() + ": " + erro));
        }
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
        return banco.transacao(c -> {
            Set<String> nomes = new HashSet<>();
            catalogo.listar(c, TipoCatalogo.SERVICO, "", true)
                    .forEach(item -> nomes.add(FormularioSugeridos.chave(item.info().nome())));
            List<String> repetidos = new ArrayList<>();
            for (FormularioSugeridos.Novo novo : novos) {
                if (!nomes.add(FormularioSugeridos.chave(novo.info().nome()))) {
                    repetidos.add("Já existe um serviço chamado \"" + novo.info().nome() + "\" no catálogo.");
                }
            }
            if (!repetidos.isEmpty()) {
                throw new EntradaInvalidaException(repetidos);
            }
            List<ItemCatalogo<ServicoPadrao>> gravados = new ArrayList<>();
            for (FormularioSugeridos.Novo novo : novos) {
                gravados.add(catalogo.inserir(c, TipoCatalogo.SERVICO, novo.dados(), novo.info(), null));
            }
            return gravados;
        });
    }

    /** Arquiva (sai das escolhas, mas continua nos cálculos antigos) ou reativa. */
    public void arquivar(long id, boolean arquivado) {
        banco.transacao(c -> {
            catalogo.arquivar(c, id, arquivado);
            return null;
        });
    }

    private static <T> void conferir(TipoCatalogo<T> tipo, T dados, InfoCatalogo info) {
        List<String> erros = FormasCatalogo.de(tipo).validar(dados, info);
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
    }
}
