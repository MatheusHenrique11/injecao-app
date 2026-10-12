package br.com.calibra.core.motor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import br.com.calibra.core.combustivel.Combustivel;
import br.com.calibra.core.util.NumeroBr;

/**
 * Os campos do motor em português, com o valor como aparece na tela, e a diferença entre duas versões
 * (SPEC 6.3: "a aba Motor mostra o histórico com campo, antes, depois e data").
 */
public final class ComparacaoMotor {

    /** Um campo do motor e o valor dele. */
    public record Linha(String campo, String valor) {
    }

    /** Um campo que mudou de uma versão para a outra. */
    public record Diferenca(String campo, String antes, String depois) {
    }

    private record Campo(String rotulo, Function<ConfiguracaoMotor, String> valor) {
    }

    private static final String VAZIO = "—";

    private static final List<Campo> CAMPOS = List.of(
            new Campo("Número de cilindros", m -> inteiro(m.numeroCilindros())),
            new Campo("Arranjo", m -> m.arranjo() == null ? VAZIO : m.arranjo().rotulo()),
            new Campo("Ângulo entre bancadas (°)", m -> numero(m.anguloEntreBancadas())),
            new Campo("Diâmetro (mm)", m -> numero(m.diametroMm())),
            new Campo("Curso (mm)", m -> numero(m.cursoMm())),
            new Campo("Cilindrada calculada (cm³)", m -> NumeroBr.formatar(m.cilindradaCc(), 0)),
            new Campo("Cilindrada informada (cm³)", m -> numero(m.cilindradaInformadaCc())),
            new Campo("Taxa de compressão (:1)", m -> numero(m.taxaCompressao())),
            new Campo("Válvulas por cilindro", m -> inteiro(m.valvulasPorCilindro())),
            new Campo("Comando", m -> m.comando() == null ? VAZIO : m.comando().rotulo()),
            new Campo("Duração do comando (°)", m -> numero(m.duracaoComandoGraus())),
            new Campo("Cruzamento (°)", m -> numero(m.cruzamentoGraus())),
            new Campo("Aspiração", m -> m.aspiracao() == null ? VAZIO : m.aspiracao().rotulo()),
            new Campo("Pistões", m -> m.pistoes() == null ? VAZIO : m.pistoes().rotulo()),
            new Campo("Bielas", m -> m.bielas() == null ? VAZIO : m.bielas().rotulo()),
            new Campo("Rotação máxima (rpm)", m -> inteiro(m.rotacaoMaxima())),
            new Campo("Marcha lenta (rpm)", m -> inteiro(m.rotacaoMarchaLenta())),
            new Campo("Ordem de ignição", m -> m.ordemIgnicao().isEmpty() ? VAZIO
                    : m.ordemIgnicao().stream().map(String::valueOf).collect(Collectors.joining("-"))),
            new Campo("Ângulos dos PMS (°)", m -> m.angulosPms().isEmpty() ? VAZIO
                    : m.angulosPms().stream().map(ComparacaoMotor::numero).collect(Collectors.joining("; "))),
            new Campo("Bicos por cilindro", m -> inteiro(m.bicosPorCilindro())),
            new Campo("Bicos extras no corpo de borboleta", m -> String.valueOf(m.bicosExtrasCorpoBorboleta())),
            new Campo("Modo de injeção", m -> m.modoInjecao() == null ? VAZIO : m.modoInjecao().rotulo()),
            new Campo("Modo de ignição", m -> m.modoIgnicao() == null ? VAZIO : m.modoIgnicao().rotulo()),
            new Campo("Roda fônica (dentes − faltantes)", m -> m.rodaFonica() == null ? VAZIO
                    : m.rodaFonica().dentesTotais() + "-" + m.rodaFonica().dentesFaltantes()),
            new Campo("Sensor de fase", m -> m.rodaFonica() == null ? VAZIO : simNao(m.rodaFonica().sensorFase())),
            new Campo("Combustível", m -> combustivel(m.combustivel())),
            new Campo("Sensor de etanol", m -> simNao(m.sensorEtanol())),
            new Campo("Altitude da oficina (m)", m -> numero(m.altitudeLocalM())));

    private ComparacaoMotor() {
    }

    /** Todos os campos do motor, na ordem da tela. */
    public static List<Linha> descrever(ConfiguracaoMotor motor) {
        Objects.requireNonNull(motor, "motor");
        return CAMPOS.stream().map(campo -> new Linha(campo.rotulo(), campo.valor().apply(motor))).toList();
    }

    /**
     * Os campos que mudaram de {@code antes} para {@code depois}, na ordem da tela. A comparação é pelo valor
     * mostrado na tela: diferença menor que a última casa mostrada (0,001 mm, por exemplo) não conta.
     */
    public static List<Diferenca> comparar(ConfiguracaoMotor antes, ConfiguracaoMotor depois) {
        List<Linha> velhas = descrever(antes);
        List<Linha> novas = descrever(depois);
        List<Diferenca> diferencas = new ArrayList<>();
        for (int i = 0; i < velhas.size(); i++) {
            if (!velhas.get(i).valor().equals(novas.get(i).valor())) {
                diferencas.add(new Diferenca(velhas.get(i).campo(), velhas.get(i).valor(), novas.get(i).valor()));
            }
        }
        return diferencas;
    }

    private static String combustivel(Combustivel c) {
        return c == null ? VAZIO : c.nome() + " (" + c.codigo() + ", versão " + c.versao() + ")";
    }

    private static String inteiro(int valor) {
        return valor <= 0 ? VAZIO : NumeroBr.formatar(valor, 0);
    }

    private static String numero(Double valor) {
        return valor == null || !Double.isFinite(valor) ? VAZIO : NumeroBr.formatar(valor, 0, 3);
    }

    private static String simNao(boolean valor) {
        return valor ? "sim" : "não";
    }
}
