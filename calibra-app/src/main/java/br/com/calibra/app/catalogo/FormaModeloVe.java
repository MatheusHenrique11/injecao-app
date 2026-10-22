package br.com.calibra.app.catalogo;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;

import br.com.calibra.core.mapa.ModeloVe;
import br.com.calibra.core.mapa.OrientacaoTabela;
import br.com.calibra.core.mapa.Tabela;
import br.com.calibra.core.motor.Comando;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/**
 * Modelo de VE de uma família de motor (fase 4; decisão 293): as válvulas e os comandos da família, os eixos relativos
 * e a VE em %, uma linha por ponto de carga (do menor para o maior) e uma coluna por ponto de rotação.
 */
final class FormaModeloVe implements FormaCatalogo<ModeloVe> {

    static final String VALVULAS = "valvulasPorCilindro";
    static final String ROTACOES = "rotacoesRelativas";
    static final String CARGAS = "cargasRelativas";
    static final String VE = "ve";

    /** A VE aparece em linhas de carga crescendo para baixo e colunas de rotação. */
    static final OrientacaoTabela ORIENTACAO = new OrientacaoTabela(OrientacaoTabela.EixoDasColunas.ROTACAO,
            OrientacaoTabela.OrdemDasLinhas.CRESCE_PARA_BAIXO);

    private static final DoubleUnaryOperator PARA_PORCENTO = v -> v * 100.0;
    private static final DoubleUnaryOperator DE_PORCENTO = v -> v / 100.0;

    static String comando(Comando comando) {
        return "comando." + comando.name();
    }

    @Override
    public TipoCatalogo<ModeloVe> tipo() {
        return TipoCatalogo.MODELO_VE;
    }

    @Override
    public List<CampoForm> campos() {
        List<CampoForm> campos = new ArrayList<>();
        campos.add(CampoForm.inteiro(VALVULAS, "Válvulas por cilindro", false)
                .comDica("A família do modelo. Em branco, vale para qualquer motor. Marque embaixo os comandos "
                        + "(nenhum marcado: qualquer comando)."));
        for (Comando comando : Comando.values()) {
            campos.add(CampoForm.marcar(comando(comando), "Comando " + comando.rotulo().toLowerCase()));
        }
        campos.add(CampoForm.area(ROTACOES, "Rotação relativa (rpm ÷ rpm do torque máximo)")
                .comDica("Ex.: 0,15 0,25 0,4 ... 1 ... 2 (1 = a rotação de torque máximo do projeto)."));
        campos.add(CampoForm.area(CARGAS, "Carga relativa (MAP ÷ 100 kPa)")
                .comDica("Ex.: 0,2 0,35 ... 1 ... 3 (1 = MAP de 100 kPa; 2 = 200 kPa)."));
        campos.add(CampoForm.area(VE, "VE (%)")
                .comDica("Uma linha por ponto de carga, do menor para o maior; um número por ponto de rotação. "
                        + "Pode colar do Excel."));
        return campos;
    }

    @Override
    public void escrever(ModeloVe modelo, ValoresForm valores) {
        valores.texto(VALVULAS, TextoForm.inteiro(modelo.valvulasPorCilindro()))
                .texto(ROTACOES, GradeTexto.eixo(modelo.ve().rotacoes(), v -> v))
                .texto(CARGAS, GradeTexto.eixo(modelo.ve().cargas(), v -> v))
                .texto(VE, GradeTexto.celulas(modelo.ve(), ORIENTACAO, PARA_PORCENTO));
        for (Comando comando : Comando.values()) {
            valores.texto(comando(comando), String.valueOf(modelo.comandos().contains(comando)));
        }
    }

    @Override
    public ModeloVe ler(LeituraForm leitura, InfoCatalogo info) {
        Integer valvulas = leitura.inteiro(VALVULAS, false);
        Set<Comando> comandos = EnumSet.noneOf(Comando.class);
        for (Comando comando : Comando.values()) {
            if (leitura.marcado(comando(comando))) {
                comandos.add(comando);
            }
        }
        List<Double> rotacoes = GradeTexto.lerEixo(leitura, ROTACOES, "Rotação relativa", v -> v);
        List<Double> cargas = GradeTexto.lerEixo(leitura, CARGAS, "Carga relativa", v -> v);
        Tabela ve = rotacoes == null || cargas == null ? null
                : GradeTexto.lerCelulas(leitura, VE, "VE", rotacoes, cargas, ORIENTACAO, DE_PORCENTO);
        if (leitura.temErros()) {
            return null;
        }
        return new ModeloVe(info.nome(), valvulas, comandos, ve, info.fonte(), info.data());
    }

    @Override
    public List<String> validar(ModeloVe modelo) {
        return modelo.validar();
    }

    @Override
    public boolean temPreco() {
        return false;
    }

    @Override
    public List<String> colunas() {
        return List.of("Família", "VE", "Pontos");
    }

    @Override
    public List<String> resumo(ModeloVe modelo) {
        return List.of(familia(modelo), faixa(modelo.ve(), 100.0, "%"),
                modelo.ve().rotacoes().size() + " rotações × " + modelo.ve().cargas().size() + " cargas");
    }

    /** "4 válvulas · original, leve e médio" ou "qualquer motor". */
    static String familia(ModeloVe modelo) {
        List<String> partes = new ArrayList<>();
        if (modelo.valvulasPorCilindro() != null) {
            partes.add(modelo.valvulasPorCilindro() + " válvulas");
        }
        if (!modelo.comandos().isEmpty()) {
            List<String> nomes = modelo.comandos().stream().sorted().map(c -> c.rotulo().toLowerCase()).toList();
            partes.add("comando " + (nomes.size() == 1 ? nomes.getFirst()
                    : String.join(", ", nomes.subList(0, nomes.size() - 1)) + " e " + nomes.getLast()));
        }
        return partes.isEmpty() ? "qualquer motor" : String.join(" · ", partes);
    }

    /** "40% a 97%" ou "12° a 34°": o menor e o maior valor das células, vezes o fator. */
    static String faixa(Tabela tabela, double fator, String unidade) {
        DoubleSummaryStatistics valores = tabela.valores().stream().flatMap(List::stream).filter(Objects::nonNull)
                .mapToDouble(v -> Math.round(v * fator * 1e6) / 1e6).summaryStatistics();
        if (valores.getCount() == 0) {
            return "—";
        }
        String menor = TextoForm.numero(valores.getMin()) + unidade;
        String maior = TextoForm.numero(valores.getMax()) + unidade;
        return menor.equals(maior) ? menor : menor + " a " + maior;
    }
}
