package br.com.calibra.core.calculo;

import static br.com.calibra.core.util.NumeroBr.formatar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.calibra.core.MotoresDoMercado;
import br.com.calibra.core.MotoresDoMercado.Motor;
import br.com.calibra.core.combustivel.CombustiveisPadrao;
import br.com.calibra.core.motor.Aspiracao;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.projeto.Meta;
import br.com.calibra.core.projeto.Uso;
import br.com.calibra.core.unidades.Conversoes;

/**
 * Evidência do bloco 2.4: o G4GC (i30 CW) com 0,5 e 0,6 bar, pela meta de pressão, ao nível do mar e a 600 m. As
 * premissas são as da pesquisa de mercado (seção 4): gasolina E32, lambda 0,80, potência a 6.000 rpm, ar a 25 °C,
 * intercooler ar-ar e os padrões da spec. A VE 0,90 é um chute, a confirmar no dinamômetro. A pesquisa chegou aos
 * números pela meta de potência (tentativa e erro); aqui a pressão vai direto, e o resultado tem de bater com ela.
 * Grava a tabela em target/ para conferência.
 */
class EvidenciaG4gcTest {

    private static final double VE_CHUTE = 0.90;
    private static final int RPM_POTENCIA = 6000;
    /** Rotação do torque máximo de série (19 kgfm a 4.600 rpm); com turbo o pico costuma vir em outro giro. */
    private static final int RPM_TORQUE = 4600;

    private record Linha(double bar, double altitudeM, Dimensionamento d) {
    }

    private static Dimensionamento calcular(double bar, double altitudeM) {
        Motor g4gc = MotoresDoMercado.TODOS.stream().filter(m -> m.nome().startsWith("Hyundai G4GC")).findFirst()
                .orElseThrow();
        ConfiguracaoMotor motor = g4gc.configuracao().aspiracao(Aspiracao.TURBO).taxaCompressao(10.1)
                .combustivel(CombustiveisPadrao.E32).altitudeLocalM(altitudeM).construir();
        Meta meta = Meta.pelaPressao(Conversoes.barParaKpa(bar), null, RPM_POTENCIA, RPM_TORQUE, Uso.RUA);
        return CalculoDimensionamento.calcular(EntradaDimensionamento.de(motor, meta, 0.80, VE_CHUTE, 25, 300,
                Parametros.padrao())).valor();
    }

    @Test
    void g4gcCom05E06BarPelaMetaDePressao() throws IOException {
        List<Linha> linhas = new ArrayList<>();
        for (double altitude : new double[] {0, 600}) {
            for (double bar : new double[] {0.5, 0.6}) {
                linhas.add(new Linha(bar, altitude, calcular(bar, altitude)));
            }
        }
        // O que a pesquisa achou pela meta de potência (seção 4): ~210 e ~225 cv no nível do mar; ~200 e 210–215 a 600 m.
        double[] daPesquisa = {210, 225, 200, 212.5};
        for (int i = 0; i < linhas.size(); i++) {
            Linha l = linhas.get(i);
            assertEquals(daPesquisa[i], l.d().potencia().cv(), daPesquisa[i] * 0.05,
                    formatar(l.bar(), 1) + " bar a " + formatar(l.altitudeM(), 0) + " m");
            assertEquals(l.bar() * 100, l.d().ladoDoAr().mapKpa() - l.d().ladoDoAr().patmKpa(), 1e-6,
                    "a pressão pedida é a do cálculo");
        }
        assertTrue(linhas.get(1).d().potencia().cv() > linhas.get(0).d().potencia().cv(), "mais pressão, mais potência");
        assertTrue(linhas.get(2).d().potencia().cv() < linhas.get(0).d().potencia().cv(), "a 600 m, menos potência");

        Path destino = Path.of(System.getProperty("basedir", "")).toAbsolutePath().resolve("target")
                .resolve("evidencia-g4gc-meta-pela-pressao.md");
        Files.createDirectories(destino.getParent());
        Files.writeString(destino, tabela(linhas));
    }

    private static String tabela(List<Linha> linhas) {
        StringBuilder texto = new StringBuilder("""
                # G4GC (i30 CW) pela meta de pressão — estimativa

                Premissas: G4GC 82 × 93,5 mm (1.975 cm³), taxa 10,1, gasolina E32, lambda 0,80, potência a 6.000 rpm, \
                ar a 25 °C, intercooler ar-ar e padrões da spec (k_ar 10,5, eta_c 0,72, eps 0,80, perdas 2 e 1 psi). \
                **VE 0,90 é chute, a confirmar no dinamômetro** (também no torque, a 4.600 rpm). Sem turbo com mapa e \
                sem limite de câmbio cadastrado: os indicadores do mapa e do câmbio não entram. Núcleo \
                """).append(br.com.calibra.core.memoria.VersaoNucleo.ATUAL).append(".\n\n");
        texto.append("| Pressão | Altitude | Potência | Torque a 6.000 rpm | Torque a 4.600 rpm | PR | Wa | "
                + "Bico (nominal, sem tempo morto) | Bomba |\n");
        texto.append("|---|---|---|---|---|---|---|---|---|\n");
        for (Linha l : linhas) {
            Dimensionamento d = l.d();
            texto.append("| ").append(formatar(l.bar(), 1)).append(" bar | ").append(formatar(l.altitudeM(), 0))
                    .append(" m | ").append(formatar(d.potencia().cv(), 0)).append(" cv | ")
                    .append(formatar(d.esforco().torqueNm(), 0)).append(" N·m | ")
                    .append(formatar(d.pontoTorque().torqueNm(), 0)).append(" N·m | ")
                    .append(formatar(d.ladoDoAr().razaoPressao(), 2)).append(" | ")
                    .append(formatar(d.combustao().vazaoArLbMin(), 1)).append(" lb/min | ")
                    .append(formatar(d.bicoNecessario().vazaoNominalCm3Min(), 0)).append(" cm³/min | ")
                    .append(formatar(d.bomba().vazaoLh(), 0)).append(" L/h |\n");
        }
        texto.append("""

                O torque sai igual nas duas rotações porque a conta usa a mesma pressão e a mesma VE: a vazão de ar \
                cresce junto com o giro, e o torque fica constante. A diferença real vem da VE no torque máximo \
                (informe no campo "VE no torque máximo") e da pressão que o turbo de fato entrega a 4.600 rpm (veja \
                no mapa do compressor).

                De série: 145 cv a 6.000 rpm e 186 N·m a 4.600 rpm. O acerto final é do preparador.
                """);
        return texto.toString();
    }
}
