package br.com.calibra.app.casos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.calibra.core.oficina.CodigoFalha;
import br.com.calibra.core.oficina.CondicaoMedicao;
import br.com.calibra.core.oficina.LaudoEletrico;
import br.com.calibra.core.oficina.Medicao;
import br.com.calibra.core.oficina.ResultadoLaudo;
import br.com.calibra.core.oficina.SituacaoCodigo;

/** A janela do laudo sem a tela (Fase 3B): as linhas de texto viram o laudo, e o laudo gravado volta às linhas. */
class FormularioLaudoTest {

    @Test
    void linhasViramMedicoesECodigos() {
        List<Medicao> medicoes = FormularioLaudo.medicoes(List.of(
                List.of(" Tensão da bateria ", "12,1", "V", "REPOUSO", "12,4 a 12,7 V", "true"),
                List.of("", " ", "", "REPOUSO", "", "false"),
                List.of("Consumo", "0,45", "A", "", "", "false")));
        assertEquals(List.of(new Medicao("Tensão da bateria", "12,1", "V", CondicaoMedicao.REPOUSO, "12,4 a 12,7 V",
                true), new Medicao("Consumo", "0,45", "A", null, "", false)), medicoes,
                "a linha sem texto fica de fora, mesmo com a condição que veio da de cima; sem condição, null");

        List<CodigoFalha> codigos = FormularioLaudo.codigos(List.of(List.of("P0562", "Tensão baixa", "HISTORICO"),
                List.of(" ", "", "ATIVO"), List.of("", "Sem código", "")));
        assertEquals(List.of(new CodigoFalha("P0562", "Tensão baixa", SituacaoCodigo.HISTORICO),
                new CodigoFalha("", "Sem código", null)), codigos, "com a descrição, a linha fica (e o núcleo cobra)");
    }

    @Test
    void laudoGravadoVoltaIgualPelasLinhas() {
        LaudoEletrico laudo = new LaudoEletrico(LocalDateTime.of(2026, 10, 20, 10, 30), "Bateria descarrega",
                List.of(new Medicao("Tensão", "12,1", "V", CondicaoMedicao.REPOUSO, "12,4 a 12,7 V", true)),
                List.of(new CodigoFalha("P0562", "", SituacaoCodigo.ATIVO)), "Teste", "Causa", "Solução", "",
                ResultadoLaudo.PARCIAL);
        LaudoEletrico deNovo = FormularioLaudo.ler(laudo.dataHora(), laudo.queixa(),
                FormularioLaudo.linhasDasMedicoes(laudo.medicoes()), FormularioLaudo.linhasDosCodigos(laudo.codigos()),
                laudo.testesRealizados(), laudo.causa(), laudo.solucao(), laudo.recomendacoes(), laudo.resultado());
        assertEquals(laudo, deNovo);
        assertEquals(List.of(List.of("Tensão", "12,1", "V", "REPOUSO", "12,4 a 12,7 V", "true")),
                FormularioLaudo.linhasDasMedicoes(laudo.medicoes()));
        assertEquals(List.of(List.of("", "", "")), FormularioLaudo.linhasDosCodigos(List.of(
                new CodigoFalha("", "", null))), "sem situação, vazio");
    }
}
