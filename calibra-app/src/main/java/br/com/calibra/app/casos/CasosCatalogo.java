package br.com.calibra.app.casos;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import br.com.calibra.app.catalogo.Combustiveis;
import br.com.calibra.app.catalogo.FormasCatalogo;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.combustivel.Combustivel;
import br.com.calibra.dados.BancoDados;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.catalogo.CatalogoRepositorio;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.ItemCatalogo;
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
            return catalogo.inserir(c, tipo, dados, info, null);
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
            if (tipo.comVersao(dados, atual.versao()).equals(atual.dados()) && info.equals(atual.info())) {
                return new Gravacao<>(atual, false);
            }
            if (dados instanceof Combustivel novo && atual.dados() instanceof Combustivel antigo
                    && !novo.codigo().equals(antigo.codigo())) {
                throw new EntradaInvalidaException("O código do combustível não muda depois de criado (os motores "
                        + "guardam o código " + antigo.codigo() + "). Para outro código, use Duplicar.");
            }
            return new Gravacao<>(catalogo.novaVersao(c, tipo, id, dados, info), true);
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
