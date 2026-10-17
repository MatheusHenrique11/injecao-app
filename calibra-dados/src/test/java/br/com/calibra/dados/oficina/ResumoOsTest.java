package br.com.calibra.dados.oficina;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import br.com.calibra.core.oficina.NumeroOs;
import br.com.calibra.core.oficina.StatusOs;

/** OS atrasada (Painel e Oficina): a previsão passou e o serviço não ficou pronto. */
class ResumoOsTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 16);

    private static ResumoOs resumo(StatusOs status, LocalDate previsao) {
        return new ResumoOs(1, new NumeroOs(2026, 1), status, "Ana Costa", "VW Gol GTi", "ABC1D23", "Revisão", null,
                null, previsao, LocalDateTime.of(2026, 10, 13, 9, 0));
    }

    @Test
    void atrasadaSoQuemNaoFicouPronta() {
        assertTrue(resumo(StatusOs.EM_EXECUCAO, HOJE.minusDays(1)).atrasada(HOJE));
        assertTrue(resumo(StatusOs.ABERTA, HOJE.minusDays(5)).atrasada(HOJE), "nem começou e já passou do dia");
        assertFalse(resumo(StatusOs.EM_EXECUCAO, HOJE).atrasada(HOJE), "o dia da entrega ainda não acabou");
        assertFalse(resumo(StatusOs.EM_EXECUCAO, null).atrasada(HOJE), "sem previsão");
        assertFalse(resumo(StatusOs.PRONTA, HOJE.minusDays(3)).atrasada(HOJE), "a pronta espera o cliente");
        assertFalse(resumo(StatusOs.ENTREGUE, HOJE.minusDays(3)).atrasada(HOJE));
        assertFalse(resumo(StatusOs.CANCELADA, HOJE.minusDays(3)).atrasada(HOJE));
    }
}
