package br.com.calibra.app.catalogo;

import java.util.List;

import br.com.calibra.core.pecas.CalibracaoImagem;
import br.com.calibra.core.pecas.MapaCompressor;
import br.com.calibra.core.pecas.Turbo;
import br.com.calibra.core.util.NumeroBr;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;
import br.com.calibra.dados.json.Json;

/**
 * Turbo: o A/R da caixa quente se digita aqui; o mapa do compressor se desenha no digitalizador ("Mapa do
 * compressor…" na lista) e vai junto no formulário, oculto, para editar o resto sem perder o mapa.
 */
final class FormaTurbo implements FormaCatalogo<Turbo> {

    static final String AR = "arCaixaQuente";
    static final String MAPA = "mapa";
    static final String IMAGEM = "imagemMapa";
    static final String CALIBRACAO = "calibracao";

    @Override
    public TipoCatalogo<Turbo> tipo() {
        return TipoCatalogo.TURBO;
    }

    @Override
    public List<CampoForm> campos() {
        return List.of(
                CampoForm.numero(AR, "A/R da caixa quente", false).comDica(Turbo.COMPROMISSO_AR + " O mapa do "
                        + "compressor se desenha em \"Mapa do compressor…\", na lista."),
                CampoForm.oculto(MAPA, "Mapa do compressor"),
                CampoForm.oculto(IMAGEM, "Imagem do mapa"),
                CampoForm.oculto(CALIBRACAO, "Calibração da imagem"));
    }

    @Override
    public void escrever(Turbo turbo, ValoresForm valores) {
        valores.texto(AR, TextoForm.numero(turbo.arCaixaQuente()))
                .texto(MAPA, turbo.mapa() == null ? "" : Json.escrever(turbo.mapa()))
                .texto(IMAGEM, turbo.imagemMapa())
                .texto(CALIBRACAO, turbo.calibracao() == null ? "" : Json.escrever(turbo.calibracao()));
    }

    @Override
    public Turbo ler(LeituraForm leitura, InfoCatalogo info) {
        Double ar = leitura.numero(AR, false);
        MapaCompressor mapa = json(leitura, MAPA, MapaCompressor.class);
        CalibracaoImagem calibracao = json(leitura, CALIBRACAO, CalibracaoImagem.class);
        String imagem = leitura.texto(IMAGEM);
        if (leitura.temErros()) {
            return null;
        }
        return new Turbo(info.nome(), mapa, ar, imagem.isEmpty() ? null : imagem, calibracao, info.fonte(),
                info.data());
    }

    private static <T> T json(LeituraForm leitura, String chave, Class<T> tipo) {
        String texto = leitura.texto(chave);
        if (texto.isEmpty()) {
            return null;
        }
        try {
            return Json.ler(texto, tipo);
        } catch (RuntimeException erro) {
            leitura.erro("O mapa gravado deste turbo não pôde ser lido: desenhe de novo em \"Mapa do compressor…\".");
            return null;
        }
    }

    @Override
    public List<String> validar(Turbo turbo) {
        return turbo.validar();
    }

    @Override
    public String exibirOculto(String chave, String valor) {
        return switch (chave) {
            case MAPA -> resumoDoMapa(Json.ler(valor, MapaCompressor.class));
            case CALIBRACAO -> "calibrada";
            default -> valor;
        };
    }

    static String resumoDoMapa(MapaCompressor mapa) {
        return mapa.surge().size() + " pontos de surge, " + mapa.choke().size() + " de choke, " + mapa.ilhas().size()
                + " ilhas, " + mapa.rotacoes().size() + " linhas de rotação"
                + (mapa.rotacaoMaximaRpm() == null ? "" : "; máxima " + NumeroBr.formatar(mapa.rotacaoMaximaRpm(), 0)
                        + " rpm");
    }

    @Override
    public List<String> colunas() {
        return List.of("A/R", "Mapa", "Pico de eficiência", "Rotação máxima");
    }

    @Override
    public List<String> resumo(Turbo turbo) {
        MapaCompressor mapa = turbo.mapa();
        Double pico = mapa == null ? null : mapa.eficienciaPico();
        Double maxima = mapa == null ? null : mapa.rotacaoMaximaRpm();
        return List.of(TextoForm.coluna(turbo.arCaixaQuente(), ""), mapa == null ? "sem mapa" : "digitalizado",
                pico == null ? "—" : NumeroBr.percentual(pico, 0), TextoForm.coluna(maxima, "rpm"));
    }
}
