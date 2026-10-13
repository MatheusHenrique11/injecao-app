package br.com.calibra.app.catalogo;

import java.util.List;

import br.com.calibra.core.motor.Aspiracao;
import br.com.calibra.core.motor.ModeloMotor;
import br.com.calibra.core.util.NumeroBr;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/**
 * Motor de fábrica ou montagem: a geometria e o que tem fonte. A cilindrada e a relação biela/curso são
 * calculadas e aparecem na lista; o que não se sabe fica em branco.
 */
final class FormaMotor implements FormaCatalogo<ModeloMotor> {

    static final String MONTAGEM = "montagem";
    static final String CILINDROS = "cilindros";
    static final String DIAMETRO = "diametro";
    static final String CURSO = "curso";
    static final String BIELA = "biela";
    static final String TAXA = "taxa";
    static final String VALVULAS = "valvulas";
    static final String CILINDRADA_FICHA = "cilindradaFicha";
    static final String ASPIRACAO = "aspiracao";
    static final String DESCRICAO = "descricao";

    @Override
    public TipoCatalogo<ModeloMotor> tipo() {
        return TipoCatalogo.MOTOR;
    }

    @Override
    public List<CampoForm> campos() {
        return List.of(
                CampoForm.marcar(MONTAGEM, "É uma montagem (peças de motores diferentes)"),
                CampoForm.inteiro(CILINDROS, "Cilindros", true),
                CampoForm.numero(DIAMETRO, "Diâmetro (mm)", true),
                CampoForm.numero(CURSO, "Curso (mm)", true),
                CampoForm.numero(BIELA, "Biela, centro a centro (mm)", false),
                CampoForm.numero(TAXA, "Taxa de compressão", false),
                CampoForm.inteiro(VALVULAS, "Válvulas por cilindro", false),
                CampoForm.numero(CILINDRADA_FICHA, "Cilindrada da ficha (cm³)", false)
                        .comDica("A publicada pelo fabricante ou pela fonte; a lista mostra a diferença para a calculada."),
                CampoForm.escolha(ASPIRACAO, "Aspiração de fábrica", false, Aspiracao.values(), Aspiracao::rotulo),
                CampoForm.area(DESCRICAO, "Descrição técnica")
                        .comDica("Receita da montagem ou observação técnica (ex.: pistões do 2.0 + virabrequim do 1.8)."));
    }

    @Override
    public void escrever(ModeloMotor motor, ValoresForm valores) {
        valores.texto(MONTAGEM, String.valueOf(motor.montagem()))
                .texto(CILINDROS, String.valueOf(motor.cilindros()))
                .texto(DIAMETRO, TextoForm.numero(motor.diametroMm()))
                .texto(CURSO, TextoForm.numero(motor.cursoMm()))
                .texto(BIELA, TextoForm.numero(motor.bielaMm()))
                .texto(TAXA, TextoForm.numero(motor.taxaCompressao()))
                .texto(VALVULAS, TextoForm.inteiro(motor.valvulasPorCilindro()))
                .texto(CILINDRADA_FICHA, TextoForm.numero(motor.cilindradaFichaCc()))
                .texto(ASPIRACAO, motor.aspiracao() == null ? "" : motor.aspiracao().name())
                .texto(DESCRICAO, motor.descricao());
    }

    @Override
    public ModeloMotor ler(LeituraForm leitura, InfoCatalogo info) {
        boolean montagem = leitura.marcado(MONTAGEM);
        Integer cilindros = leitura.inteiro(CILINDROS, true);
        Double diametro = leitura.numero(DIAMETRO, true);
        Double curso = leitura.numero(CURSO, true);
        Double biela = leitura.numero(BIELA, false);
        Double taxa = leitura.numero(TAXA, false);
        Integer valvulas = leitura.inteiro(VALVULAS, false);
        Double ficha = leitura.numero(CILINDRADA_FICHA, false);
        Aspiracao aspiracao = leitura.escolha(ASPIRACAO, Aspiracao.class, false);
        String descricao = leitura.texto(DESCRICAO);
        if (leitura.temErros()) {
            return null;
        }
        return new ModeloMotor(info.nome(), montagem, cilindros, diametro, curso, biela, taxa, valvulas, ficha,
                aspiracao, descricao, info.fonte(), info.data());
    }

    @Override
    public List<String> validar(ModeloMotor motor) {
        return motor.validar();
    }

    @Override
    public List<String> colunas() {
        return List.of("Tipo", "Cilindros", "Diâmetro × curso", "Cilindrada", "Biela/curso");
    }

    @Override
    public List<String> resumo(ModeloMotor motor) {
        String cilindrada = NumeroBr.formatar(motor.cilindradaCc(), 0) + " cm³";
        if (motor.cilindradaFichaCc() != null) {
            cilindrada += " (ficha " + NumeroBr.formatar(motor.cilindradaFichaCc(), 0) + ")";
        }
        return List.of(motor.montagem() ? "Montagem" : "Fábrica", String.valueOf(motor.cilindros()),
                TextoForm.numero(motor.diametroMm()) + " × " + TextoForm.numero(motor.cursoMm()) + " mm", cilindrada,
                Double.isNaN(motor.relacaoBielaCurso()) ? "—" : NumeroBr.formatar(motor.relacaoBielaCurso(), 2));
    }
}
