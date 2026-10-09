package br.com.calibra.core.motor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import br.com.calibra.core.combustivel.Combustivel;

/**
 * Configuração do motor (SPEC 6.3). É só dado: quem confere as regras é o {@link ValidadorMotor}.
 * Campos opcionais usam {@code Double} e podem ser null.
 *
 * @param anguloEntreBancadas graus; obrigatório para V e VR/W
 * @param cilindradaInformadaCc opcional; comparada com a calculada
 * @param duracaoComandoGraus opcional
 * @param cruzamentoGraus opcional
 * @param ordemIgnicao n números, permutação de 1..n começando em 1
 * @param angulosPms n ângulos do virabrequim (0 a 719°), um por cilindro, na ordem de ignição
 * @param bicosPorCilindro 1 ou 2 (bancadas primária e secundária)
 * @param bicosExtrasCorpoBorboleta bicos no corpo de borboleta, contados à parte
 * @param altitudeLocalM altitude da oficina, de 0 a 3.000 m
 */
public record ConfiguracaoMotor(
        int numeroCilindros,
        Arranjo arranjo,
        Double anguloEntreBancadas,
        double diametroMm,
        double cursoMm,
        Double cilindradaInformadaCc,
        double taxaCompressao,
        int valvulasPorCilindro,
        Comando comando,
        Double duracaoComandoGraus,
        Double cruzamentoGraus,
        Aspiracao aspiracao,
        Construcao pistoes,
        Construcao bielas,
        int rotacaoMaxima,
        int rotacaoMarchaLenta,
        List<Integer> ordemIgnicao,
        List<Double> angulosPms,
        int bicosPorCilindro,
        int bicosExtrasCorpoBorboleta,
        ModoInjecao modoInjecao,
        ModoIgnicao modoIgnicao,
        RodaFonica rodaFonica,
        Combustivel combustivel,
        boolean sensorEtanol,
        double altitudeLocalM) {

    public ConfiguracaoMotor {
        ordemIgnicao = ordemIgnicao == null ? List.of() : List.copyOf(ordemIgnicao);
        angulosPms = angulosPms == null ? List.of() : List.copyOf(angulosPms);
    }

    /** Cilindrada calculada = π/4 × D² × S × n (SPEC 6.3), em cm³. */
    public double cilindradaCc() {
        return volumeCilindroCc() * numeroCilindros;
    }

    /** Volume de um cilindro = π/4 × D² × S, em cm³. */
    public double volumeCilindroCc() {
        return Math.PI / 4.0 * diametroMm * diametroMm * cursoMm / 1000.0;
    }

    public static Construtor construtor() {
        return new Construtor();
    }

    /** Copia esta configuração num construtor, para criar uma variação dela. */
    public Construtor paraConstrutor() {
        return new Construtor()
                .cilindros(numeroCilindros)
                .arranjo(arranjo)
                .anguloEntreBancadas(anguloEntreBancadas)
                .diametroMm(diametroMm)
                .cursoMm(cursoMm)
                .cilindradaInformadaCc(cilindradaInformadaCc)
                .taxaCompressao(taxaCompressao)
                .valvulasPorCilindro(valvulasPorCilindro)
                .comando(comando)
                .duracaoComandoGraus(duracaoComandoGraus)
                .cruzamentoGraus(cruzamentoGraus)
                .aspiracao(aspiracao)
                .pistoes(pistoes)
                .bielas(bielas)
                .rotacaoMaxima(rotacaoMaxima)
                .rotacaoMarchaLenta(rotacaoMarchaLenta)
                .ordemIgnicao(ordemIgnicao)
                .angulosPms(angulosPms)
                .bicosPorCilindro(bicosPorCilindro)
                .bicosExtrasCorpoBorboleta(bicosExtrasCorpoBorboleta)
                .modoInjecao(modoInjecao)
                .modoIgnicao(modoIgnicao)
                .rodaFonica(rodaFonica)
                .combustivel(combustivel)
                .sensorEtanol(sensorEtanol)
                .altitudeLocalM(altitudeLocalM);
    }

    /** Monta uma configuração campo a campo. Campo numérico não preenchido fica inválido (NaN ou 0). */
    public static final class Construtor {

        private int numeroCilindros;
        private Arranjo arranjo;
        private Double anguloEntreBancadas;
        private double diametroMm = Double.NaN;
        private double cursoMm = Double.NaN;
        private Double cilindradaInformadaCc;
        private double taxaCompressao = Double.NaN;
        private int valvulasPorCilindro;
        private Comando comando;
        private Double duracaoComandoGraus;
        private Double cruzamentoGraus;
        private Aspiracao aspiracao;
        private Construcao pistoes;
        private Construcao bielas;
        private int rotacaoMaxima;
        private int rotacaoMarchaLenta;
        private List<Integer> ordemIgnicao = List.of();
        private List<Double> angulosPms = List.of();
        private int bicosPorCilindro;
        private int bicosExtrasCorpoBorboleta;
        private ModoInjecao modoInjecao;
        private ModoIgnicao modoIgnicao;
        private RodaFonica rodaFonica;
        private Combustivel combustivel;
        private boolean sensorEtanol;
        private double altitudeLocalM = Double.NaN;

        private Construtor() {
        }

        public Construtor cilindros(int valor) {
            numeroCilindros = valor;
            return this;
        }

        public Construtor arranjo(Arranjo valor) {
            arranjo = valor;
            return this;
        }

        public Construtor anguloEntreBancadas(Double valor) {
            anguloEntreBancadas = valor;
            return this;
        }

        public Construtor diametroMm(double valor) {
            diametroMm = valor;
            return this;
        }

        public Construtor cursoMm(double valor) {
            cursoMm = valor;
            return this;
        }

        public Construtor cilindradaInformadaCc(Double valor) {
            cilindradaInformadaCc = valor;
            return this;
        }

        public Construtor taxaCompressao(double valor) {
            taxaCompressao = valor;
            return this;
        }

        public Construtor valvulasPorCilindro(int valor) {
            valvulasPorCilindro = valor;
            return this;
        }

        public Construtor comando(Comando valor) {
            comando = valor;
            return this;
        }

        public Construtor duracaoComandoGraus(Double valor) {
            duracaoComandoGraus = valor;
            return this;
        }

        public Construtor cruzamentoGraus(Double valor) {
            cruzamentoGraus = valor;
            return this;
        }

        public Construtor aspiracao(Aspiracao valor) {
            aspiracao = valor;
            return this;
        }

        public Construtor pistoes(Construcao valor) {
            pistoes = valor;
            return this;
        }

        public Construtor bielas(Construcao valor) {
            bielas = valor;
            return this;
        }

        public Construtor rotacaoMaxima(int valor) {
            rotacaoMaxima = valor;
            return this;
        }

        public Construtor rotacaoMarchaLenta(int valor) {
            rotacaoMarchaLenta = valor;
            return this;
        }

        public Construtor ordemIgnicao(List<Integer> valor) {
            ordemIgnicao = valor == null ? List.of() : new ArrayList<>(valor);
            return this;
        }

        public Construtor ordemIgnicao(int... cilindros) {
            return ordemIgnicao(Arrays.stream(cilindros).boxed().toList());
        }

        public Construtor angulosPms(List<Double> valor) {
            angulosPms = valor == null ? List.of() : new ArrayList<>(valor);
            return this;
        }

        public Construtor angulosPms(double... graus) {
            return angulosPms(Arrays.stream(graus).boxed().toList());
        }

        public Construtor bicosPorCilindro(int valor) {
            bicosPorCilindro = valor;
            return this;
        }

        public Construtor bicosExtrasCorpoBorboleta(int valor) {
            bicosExtrasCorpoBorboleta = valor;
            return this;
        }

        public Construtor modoInjecao(ModoInjecao valor) {
            modoInjecao = valor;
            return this;
        }

        public Construtor modoIgnicao(ModoIgnicao valor) {
            modoIgnicao = valor;
            return this;
        }

        public Construtor rodaFonica(RodaFonica valor) {
            rodaFonica = valor;
            return this;
        }

        public Construtor combustivel(Combustivel valor) {
            combustivel = valor;
            return this;
        }

        public Construtor sensorEtanol(boolean valor) {
            sensorEtanol = valor;
            return this;
        }

        public Construtor altitudeLocalM(double valor) {
            altitudeLocalM = valor;
            return this;
        }

        public ConfiguracaoMotor construir() {
            return new ConfiguracaoMotor(numeroCilindros, arranjo, anguloEntreBancadas, diametroMm, cursoMm,
                    cilindradaInformadaCc, taxaCompressao, valvulasPorCilindro, comando, duracaoComandoGraus,
                    cruzamentoGraus, aspiracao, pistoes, bielas, rotacaoMaxima, rotacaoMarchaLenta, ordemIgnicao,
                    angulosPms, bicosPorCilindro, bicosExtrasCorpoBorboleta, modoInjecao, modoIgnicao, rodaFonica,
                    combustivel, sensorEtanol, altitudeLocalM);
        }
    }
}
