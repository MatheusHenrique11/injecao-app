package br.com.calibra.app.catalogo;

import java.util.List;
import java.util.Locale;

import br.com.calibra.core.mapa.ModeloIgnicao;
import br.com.calibra.core.mapa.OrientacaoTabela;
import br.com.calibra.core.mapa.Tabela;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/**
 * Modelo de ignição de uma família de motor (fase 4; decisão 293), digitado ou colado pelo mecânico: os eixos em rpm
 * e kPa, o avanço em graus e como está o texto colado (o que fica nas colunas e o sentido das linhas), para colar
 * direto da tela da injeção. O app não traz nenhum modelo de ignição.
 */
final class FormaModeloIgnicao implements FormaCatalogo<ModeloIgnicao> {

    static final String FAMILIA = "familia";
    static final String COMBUSTIVEL = "combustivel";
    static final String TEMPERATURA = "temperaturaArReferenciaC";
    static final String ROTACOES = "rotacoes";
    static final String CARGAS = "cargas";
    static final String COLUNAS = "colunas";
    static final String LINHAS = "linhas";
    static final String AVANCO = "avanco";

    /** Como o formulário escreve o avanço de um modelo gravado. */
    static final OrientacaoTabela ORIENTACAO_GRAVADA = new OrientacaoTabela(OrientacaoTabela.EixoDasColunas.ROTACAO,
            OrientacaoTabela.OrdemDasLinhas.CRESCE_PARA_BAIXO);

    @Override
    public TipoCatalogo<ModeloIgnicao> tipo() {
        return TipoCatalogo.MODELO_IGNICAO;
    }

    @Override
    public List<CampoForm> campos() {
        return List.of(
                CampoForm.texto(FAMILIA, "Família de motor", false).comDica("Ex.: VW AP 2.0 8v turbo."),
                CampoForm.texto(COMBUSTIVEL, "Combustível do mapa", false)
                        .comDica("O código do combustível em que o mapa foi feito (ex.: E32). Num projeto com outro "
                                + "combustível, o mapa base tira avanço."),
                CampoForm.numero(TEMPERATURA, "Temperatura do ar do mapa (°C)", false)
                        .comDica("Em que temperatura do ar o mapa foi feito. Em branco, se não se sabe."),
                CampoForm.area(ROTACOES, "Pontos de rotação (rpm)")
                        .comDica("Cole a linha ou a coluna do software, ou digite separando por espaço."),
                CampoForm.area(CARGAS, "Pontos de carga (MAP, kPa absolutos)").comDica("Do menor para o maior."),
                CampoForm.escolha(COLUNAS, "No texto colado, as colunas são", true,
                        OrientacaoTabela.EixoDasColunas.values(), OrientacaoTabela.EixoDasColunas::rotulo),
                CampoForm.escolha(LINHAS, "No texto colado, as linhas", true, OrientacaoTabela.OrdemDasLinhas.values(),
                        OrientacaoTabela.OrdemDasLinhas::rotulo),
                CampoForm.area(AVANCO, "Avanço (graus antes do PMS)")
                        .comDica("Cole as células como estão na tela da injeção, uma linha por linha. Ao editar, "
                                + "o texto volta com a rotação nas colunas e a carga crescendo para baixo."));
    }

    @Override
    public void novo(ValoresForm valores) {
        valores.texto(COLUNAS, ORIENTACAO_GRAVADA.colunas().name())
                .texto(LINHAS, ORIENTACAO_GRAVADA.linhas().name());
    }

    @Override
    public void escrever(ModeloIgnicao modelo, ValoresForm valores) {
        valores.texto(FAMILIA, modelo.familia())
                .texto(COMBUSTIVEL, modelo.combustivel())
                .texto(TEMPERATURA, TextoForm.numero(modelo.temperaturaArReferenciaC()))
                .texto(ROTACOES, GradeTexto.eixo(modelo.avanco().rotacoes(), v -> v))
                .texto(CARGAS, GradeTexto.eixo(modelo.avanco().cargas(), v -> v))
                .texto(COLUNAS, ORIENTACAO_GRAVADA.colunas().name())
                .texto(LINHAS, ORIENTACAO_GRAVADA.linhas().name())
                .texto(AVANCO, GradeTexto.celulas(modelo.avanco(), ORIENTACAO_GRAVADA, v -> v));
    }

    @Override
    public ModeloIgnicao ler(LeituraForm leitura, InfoCatalogo info) {
        Double temperatura = leitura.numero(TEMPERATURA, false);
        List<Double> rotacoes = GradeTexto.lerEixo(leitura, ROTACOES, "Pontos de rotação", v -> v);
        List<Double> cargas = GradeTexto.lerEixo(leitura, CARGAS, "Pontos de carga", v -> v);
        OrientacaoTabela.EixoDasColunas colunas = leitura.escolha(COLUNAS, OrientacaoTabela.EixoDasColunas.class,
                true);
        OrientacaoTabela.OrdemDasLinhas linhas = leitura.escolha(LINHAS, OrientacaoTabela.OrdemDasLinhas.class, true);
        Tabela avanco = rotacoes == null || cargas == null || colunas == null || linhas == null ? null
                : GradeTexto.lerCelulas(leitura, AVANCO, "Avanço", rotacoes, cargas,
                        new OrientacaoTabela(colunas, linhas), v -> v);
        if (leitura.temErros()) {
            return null;
        }
        String combustivel = leitura.texto(COMBUSTIVEL).toUpperCase(Locale.ROOT);
        return new ModeloIgnicao(info.nome(), leitura.texto(FAMILIA), combustivel, temperatura, avanco, info.fonte(),
                info.data());
    }

    @Override
    public List<String> validar(ModeloIgnicao modelo) {
        return modelo.validar();
    }

    @Override
    public boolean temPreco() {
        return false;
    }

    @Override
    public List<String> colunas() {
        return List.of("Família", "Combustível", "Avanço", "Pontos");
    }

    @Override
    public List<String> resumo(ModeloIgnicao modelo) {
        return List.of(modelo.familia().isEmpty() ? "—" : modelo.familia(),
                modelo.combustivel().isEmpty() ? "—" : modelo.combustivel(),
                FormaModeloVe.faixa(modelo.avanco(), 1.0, "°"),
                modelo.avanco().rotacoes().size() + " rotações × " + modelo.avanco().cargas().size() + " cargas");
    }
}
