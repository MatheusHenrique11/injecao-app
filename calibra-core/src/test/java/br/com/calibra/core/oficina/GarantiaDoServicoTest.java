package br.com.calibra.core.oficina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/** A garantia de cada serviço (SPEC 6.6.1): a própria, do catálogo, ou a da OS. */
class GarantiaDoServicoTest {

    private static final Garantia DA_OS = new Garantia(90);
    private static final LocalDate ENTREGA = LocalDate.of(2026, 10, 19);

    @Test
    void semGarantiaPropriaValeADaOs() {
        GarantiaDoServico servico = new GarantiaDoServico("Troca de alto-falantes", null);
        assertFalse(servico.propria());
        assertEquals(DA_OS, servico.garantia(DA_OS));
        assertEquals(LocalDate.of(2027, 1, 17), servico.garantia(DA_OS).vence(ENTREGA));
    }

    @Test
    void comGarantiaPropriaValeADoServico() {
        GarantiaDoServico alternador = new GarantiaDoServico("Reparo ou troca de alternador", 365);
        assertTrue(alternador.propria());
        assertEquals(new Garantia(365), alternador.garantia(DA_OS));
        assertEquals(LocalDate.of(2027, 10, 19), alternador.garantia(DA_OS).vence(ENTREGA));
    }

    /** 0 dias é uma garantia própria (o serviço não tem garantia), diferente de "em branco" (a da OS). */
    @Test
    void zeroDiasESemGarantiaParaAqueleServico() {
        GarantiaDoServico diagnostico = new GarantiaDoServico("Diagnóstico de falha elétrica", 0);
        assertTrue(diagnostico.propria());
        assertEquals(Garantia.SEM_GARANTIA, diagnostico.garantia(DA_OS));
        assertEquals(ENTREGA, diagnostico.garantia(DA_OS).vence(ENTREGA));
    }
}
