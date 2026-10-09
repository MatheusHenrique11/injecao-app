package br.com.calibra.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import br.com.calibra.core.calculo.CaminhoAr;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.calculo.MarchaLenta;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.core.combustivel.CombustiveisPadrao;
import br.com.calibra.core.combustivel.Combustivel;
import br.com.calibra.core.motor.Arranjo;
import br.com.calibra.core.motor.Aspiracao;
import br.com.calibra.core.motor.Comando;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.motor.Construcao;
import br.com.calibra.core.motor.ModoIgnicao;
import br.com.calibra.core.motor.ModoInjecao;
import br.com.calibra.core.motor.RodaFonica;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.projeto.Meta;
import br.com.calibra.core.projeto.PontoMedicao;
import br.com.calibra.core.projeto.Uso;
import br.com.calibra.core.semaforo.LimiaresSemaforo;
import br.com.calibra.core.unidades.Conversoes;
import br.com.calibra.core.util.NumeroBr;

/**
 * Transforma o texto dos campos da tela na entrada do núcleo, nas unidades do núcleo (kW, kPa...).
 * Não usa JavaFX: recebe um mapa campo → texto. Erros de digitação viram mensagens; as regras de
 * negócio ficam no núcleo.
 */
public final class LeitorFormulario {

    /** Resultado da leitura: a entrada (null se houve erro) e os erros encontrados. */
    public record Leitura(EntradaDimensionamento entrada, List<String> erros) {

        public Leitura {
            erros = List.copyOf(erros);
        }
    }

    /** Resultado da leitura só do motor. */
    public record LeituraMotor(ConfiguracaoMotor motor, List<String> erros) {

        public LeituraMotor {
            erros = List.copyOf(erros);
        }
    }

    private final Map<Campo, String> valores;
    private final List<String> erros = new ArrayList<>();

    private LeitorFormulario(Map<Campo, String> valores) {
        this.valores = valores;
    }

    public static Leitura ler(Map<Campo, String> valores) {
        LeitorFormulario leitor = new LeitorFormulario(valores);
        EntradaDimensionamento entrada = leitor.entrada();
        return new Leitura(leitor.erros.isEmpty() ? entrada : null, leitor.erros);
    }

    public static LeituraMotor lerMotor(Map<Campo, String> valores) {
        LeitorFormulario leitor = new LeitorFormulario(valores);
        ConfiguracaoMotor motor = leitor.motor();
        return new LeituraMotor(leitor.erros.isEmpty() ? motor : null, leitor.erros);
    }

    // ===================== Montagem =====================

    private EntradaDimensionamento entrada() {
        ConfiguracaoMotor motor = motor();
        Meta meta = meta();
        double lambda = numero(Campo.LAMBDA);
        double ve = numero(Campo.VE);
        double temperatura = numero(Campo.TEMPERATURA_AMBIENTE);
        double pressaoBase = Conversoes.barParaKpa(numero(Campo.PRESSAO_BASE_BAR));
        Parametros parametros = parametros();
        Bico bico = bico("Bico", Campo.BICO_VAZAO, Campo.BICO_PRESSAO_TESTE_BAR, Campo.BICO_DENSIDADE_TESTE,
                Campo.BICO_TEMPO_MORTO);
        Bico secundario = bico("Bico secundário", Campo.BICO2_VAZAO, Campo.BICO2_PRESSAO_TESTE_BAR,
                Campo.BICO2_DENSIDADE_TESTE, Campo.BICO2_TEMPO_MORTO);
        MarchaLenta marchaLenta = marchaLenta();
        Double bomba = opcional(Campo.BOMBA_VAZAO);
        Double sensor = opcional(Campo.SENSOR_MAP_KPA);
        Injecao injecao = injecao();
        if (!erros.isEmpty()) {
            return null;
        }
        return new EntradaDimensionamento(motor, meta, lambda, ve, temperatura, pressaoBase, parametros, bico,
                secundario, null, marchaLenta, bomba, sensor, injecao);
    }

    private ConfiguracaoMotor motor() {
        String codigoCombustivel = texto(Campo.COMBUSTIVEL);
        Combustivel combustivel = null;
        if (codigoCombustivel.isEmpty()) {
            erros.add("Escolha " + Campo.COMBUSTIVEL.nome() + ".");
        } else {
            combustivel = CombustiveisPadrao.porCodigo(codigoCombustivel).orElse(null);
            if (combustivel == null) {
                erros.add("Combustível desconhecido: " + codigoCombustivel + ".");
            }
        }
        return ConfiguracaoMotor.construtor()
                .cilindros(inteiro(Campo.CILINDROS))
                .arranjo(escolha(Campo.ARRANJO, Arranjo::valueOf))
                .anguloEntreBancadas(opcional(Campo.ANGULO_BANCADAS))
                .diametroMm(numero(Campo.DIAMETRO))
                .cursoMm(numero(Campo.CURSO))
                .cilindradaInformadaCc(opcional(Campo.CILINDRADA_INFORMADA))
                .taxaCompressao(numero(Campo.TAXA))
                .valvulasPorCilindro(inteiro(Campo.VALVULAS))
                .comando(escolha(Campo.COMANDO, Comando::valueOf))
                .duracaoComandoGraus(opcional(Campo.DURACAO_COMANDO))
                .cruzamentoGraus(opcional(Campo.CRUZAMENTO))
                .aspiracao(escolha(Campo.ASPIRACAO, Aspiracao::valueOf))
                .pistoes(escolha(Campo.PISTOES, Construcao::valueOf))
                .bielas(escolha(Campo.BIELAS, Construcao::valueOf))
                .rotacaoMaxima(inteiro(Campo.ROTACAO_MAXIMA))
                .rotacaoMarchaLenta(inteiro(Campo.MARCHA_LENTA_RPM))
                .ordemIgnicao(ordemIgnicao())
                .angulosPms(angulos())
                .bicosPorCilindro(inteiro(Campo.BICOS_POR_CILINDRO))
                .bicosExtrasCorpoBorboleta(inteiroOpcional(Campo.BICOS_EXTRAS, 0))
                .modoInjecao(escolha(Campo.MODO_INJECAO, ModoInjecao::valueOf))
                .modoIgnicao(escolha(Campo.MODO_IGNICAO, ModoIgnicao::valueOf))
                .rodaFonica(new RodaFonica(inteiro(Campo.DENTES_TOTAIS), inteiroOpcional(Campo.DENTES_FALTANTES, 0),
                        marcado(Campo.SENSOR_FASE)))
                .combustivel(combustivel)
                .sensorEtanol(marcado(Campo.SENSOR_ETANOL))
                .altitudeLocalM(numero(Campo.ALTITUDE))
                .construir();
    }

    private Meta meta() {
        double cv = numero(Campo.POTENCIA_CV);
        PontoMedicao ponto = escolha(Campo.PONTO_MEDICAO, PontoMedicao::valueOf);
        Double perda = ponto == PontoMedicao.RODA ? numero(Campo.PERDA_TRANSMISSAO) : null;
        int rpmPotencia = inteiro(Campo.RPM_POTENCIA);
        Integer rpmTorque = vazio(Campo.RPM_TORQUE) ? null : inteiro(Campo.RPM_TORQUE);
        Uso uso = escolha(Campo.USO, Uso::valueOf);
        return new Meta(Conversoes.cvParaKw(cv), ponto, perda, rpmPotencia, rpmTorque, uso);
    }

    private Parametros parametros() {
        CaminhoAr caminho = escolha(Campo.CAMINHO_AR, CaminhoAr::valueOf);
        double kAr = caminho == CaminhoAr.K_AR ? numero(Campo.K_AR) : Double.NaN;
        double bsfc = caminho == CaminhoAr.BSFC ? numero(Campo.BSFC) : Double.NaN;
        return new Parametros(caminho, kAr, bsfc,
                numero(Campo.DC_MAX_PCT) / 100.0,
                numero(Campo.ETA_C),
                numero(Campo.EPS),
                Conversoes.psiParaKpa(numero(Campo.PERDA_DESCARGA_PSI)),
                Conversoes.psiParaKpa(numero(Campo.PERDA_ADMISSAO_PSI)),
                numero(Campo.ACRESCIMO_TM),
                numero(Campo.DENSIDADE_TESTE_PADRAO),
                numero(Campo.TI_MINIMO_MARCHA_LENTA),
                1,
                LimiaresSemaforo.padrao());
    }

    /** Um bico existe quando a vazão está preenchida; aí os outros campos dele são obrigatórios. */
    private Bico bico(String nome, Campo vazao, Campo pressaoTeste, Campo densidade, Campo tempoMorto) {
        if (vazio(vazao)) {
            return null;
        }
        double vazaoCm3 = numero(vazao);
        double pressaoKpa = Conversoes.barParaKpa(numero(pressaoTeste));
        double densidadeKgL = numero(densidade);
        double tempoMortoMs = numero(tempoMorto);
        if (!erros.isEmpty()) {
            return null;
        }
        return Bico.informado(nome + " " + NumeroBr.formatar(vazaoCm3, 0) + " cm³/min", vazaoCm3, pressaoKpa,
                densidadeKgL, tempoMortoMs);
    }

    private MarchaLenta marchaLenta() {
        if (vazio(Campo.MARCHA_LENTA_MAP) && vazio(Campo.MARCHA_LENTA_VE)) {
            return null;
        }
        return new MarchaLenta(numero(Campo.MARCHA_LENTA_MAP), numero(Campo.MARCHA_LENTA_VE));
    }

    private Injecao injecao() {
        if (vazio(Campo.INJECAO_SAIDAS_BICO) && vazio(Campo.INJECAO_SAIDAS_IGNICAO)) {
            return null;
        }
        String nome = texto(Campo.INJECAO_NOME);
        return Injecao.informada(nome.isEmpty() ? "informada" : nome, inteiro(Campo.INJECAO_SAIDAS_BICO),
                inteiro(Campo.INJECAO_SAIDAS_IGNICAO));
    }

    // ===================== Leitura de cada tipo =====================

    private String texto(Campo campo) {
        String valor = valores.get(campo);
        return valor == null ? "" : valor.strip();
    }

    private boolean vazio(Campo campo) {
        return texto(campo).isEmpty();
    }

    /** Número obrigatório. Vazio ou inválido: anota o erro e devolve NaN. */
    private double numero(Campo campo) {
        if (vazio(campo)) {
            erros.add("Informe " + campo.nome() + ".");
            return Double.NaN;
        }
        try {
            return NumeroBr.ler(texto(campo));
        } catch (NumberFormatException erro) {
            erros.add(maiuscula(campo.nome()) + ": " + erro.getMessage());
            return Double.NaN;
        }
    }

    /** Número opcional: vazio = null. */
    private Double opcional(Campo campo) {
        return vazio(campo) ? null : numero(campo);
    }

    private int inteiro(Campo campo) {
        double valor = numero(campo);
        if (Double.isNaN(valor)) {
            return 0;
        }
        if (valor != Math.rint(valor)) {
            erros.add(maiuscula(campo.nome()) + ": use um número inteiro.");
            return 0;
        }
        return (int) valor;
    }

    private int inteiroOpcional(Campo campo, int padrao) {
        return vazio(campo) ? padrao : inteiro(campo);
    }

    private boolean marcado(Campo campo) {
        return Boolean.parseBoolean(texto(campo));
    }

    private <E> E escolha(Campo campo, Function<String, E> converter) {
        if (vazio(campo)) {
            erros.add("Escolha " + campo.nome() + ".");
            return null;
        }
        try {
            return converter.apply(texto(campo));
        } catch (IllegalArgumentException erro) {
            erros.add("Valor inválido para " + campo.nome() + ".");
            return null;
        }
    }

    /** Ordem de ignição: números inteiros separados por traço, vírgula, ponto e vírgula ou espaço. */
    private List<Integer> ordemIgnicao() {
        if (vazio(Campo.ORDEM_IGNICAO)) {
            erros.add("Informe " + Campo.ORDEM_IGNICAO.nome() + " (ex.: 1-3-4-2).");
            return List.of();
        }
        List<Integer> ordem = new ArrayList<>();
        for (String parte : texto(Campo.ORDEM_IGNICAO).split("[\\s,;\\-]+")) {
            if (parte.isEmpty()) {
                continue;
            }
            try {
                ordem.add(Integer.parseInt(parte));
            } catch (NumberFormatException erro) {
                erros.add("Ordem de ignição: use números inteiros separados por traço (ex.: 1-3-4-2).");
                return List.of();
            }
        }
        return ordem;
    }

    /** Ângulos dos PMS: números separados por ponto e vírgula, traço ou espaço (a vírgula é decimal). */
    private List<Double> angulos() {
        if (vazio(Campo.ANGULOS_PMS)) {
            erros.add("Informe " + Campo.ANGULOS_PMS.nome() + " (ex.: 0; 180; 360; 540).");
            return List.of();
        }
        List<Double> angulos = new ArrayList<>();
        for (String parte : texto(Campo.ANGULOS_PMS).split("[\\s;\\-]+")) {
            if (parte.isEmpty()) {
                continue;
            }
            try {
                angulos.add(NumeroBr.ler(parte));
            } catch (NumberFormatException erro) {
                erros.add("Ângulos dos PMS: " + erro.getMessage() + " Separe os ângulos com ponto e vírgula.");
                return List.of();
            }
        }
        return angulos;
    }

    private static String maiuscula(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
