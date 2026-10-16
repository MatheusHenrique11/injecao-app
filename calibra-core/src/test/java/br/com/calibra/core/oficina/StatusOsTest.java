package br.com.calibra.core.oficina;

import static br.com.calibra.core.oficina.StatusOs.ABERTA;
import static br.com.calibra.core.oficina.StatusOs.AGUARDANDO_APROVACAO;
import static br.com.calibra.core.oficina.StatusOs.AGUARDANDO_PECAS;
import static br.com.calibra.core.oficina.StatusOs.APROVADA;
import static br.com.calibra.core.oficina.StatusOs.CANCELADA;
import static br.com.calibra.core.oficina.StatusOs.EM_EXECUCAO;
import static br.com.calibra.core.oficina.StatusOs.ENTREGUE;
import static br.com.calibra.core.oficina.StatusOs.PRONTA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Os passos da OS (SPEC 6.5 e regras do bloco 3.1): cada status, o que pode vir depois e o que não pode. */
class StatusOsTest {

    private static final Map<StatusOs, Set<StatusOs>> PERMITIDOS = Map.of(
            ABERTA, Set.of(AGUARDANDO_APROVACAO, CANCELADA),
            AGUARDANDO_APROVACAO, Set.of(APROVADA, ABERTA, CANCELADA),
            APROVADA, Set.of(EM_EXECUCAO, CANCELADA),
            EM_EXECUCAO, Set.of(AGUARDANDO_PECAS, PRONTA, CANCELADA),
            AGUARDANDO_PECAS, Set.of(EM_EXECUCAO, CANCELADA),
            PRONTA, Set.of(ENTREGUE, EM_EXECUCAO, CANCELADA),
            ENTREGUE, Set.of(),
            CANCELADA, Set.of());

    @Test
    void cadaStatusSoVaiParaOsPassosPermitidos() {
        assertEquals(EnumSet.allOf(StatusOs.class), EnumSet.copyOf(PERMITIDOS.keySet()), "a tabela cobre todos");
        for (StatusOs origem : StatusOs.values()) {
            assertEquals(PERMITIDOS.get(origem), origem.proximos(), origem.name());
            for (StatusOs destino : StatusOs.values()) {
                assertEquals(PERMITIDOS.get(origem).contains(destino), origem.podeIrPara(destino),
                        origem + " → " + destino);
            }
            assertFalse(origem.podeIrPara(null));
        }
    }

    /** Executar exige orçamento aprovado: de aberta ou aguardando aprovação não se pula para a execução. */
    @Test
    void naoSeExecutaSemAprovacao() {
        assertFalse(ABERTA.podeIrPara(EM_EXECUCAO));
        assertFalse(AGUARDANDO_APROVACAO.podeIrPara(EM_EXECUCAO));
        assertFalse(ABERTA.podeIrPara(APROVADA), "aprovar exige o orçamento enviado");
        assertTrue(APROVADA.podeIrPara(EM_EXECUCAO));
    }

    @Test
    void cancelarValeAteAEntrega() {
        for (StatusOs status : StatusOs.values()) {
            assertEquals(!status.finalizada(), status.podeIrPara(CANCELADA), status.name());
        }
    }

    @Test
    void finalizadasEAntesDaAprovacao() {
        assertEquals(EnumSet.of(ENTREGUE, CANCELADA), EnumSet.copyOf(
                Arrays.stream(StatusOs.values()).filter(StatusOs::finalizada).toList()));
        assertEquals(EnumSet.of(ABERTA, AGUARDANDO_APROVACAO), EnumSet.copyOf(
                Arrays.stream(StatusOs.values()).filter(StatusOs::antesDaAprovacao).toList()));
    }

    @Test
    void rotulosEmPortugues() {
        assertEquals("Aguardando aprovação", AGUARDANDO_APROVACAO.rotulo());
        assertEquals("Em execução", EM_EXECUCAO.rotulo());
        assertEquals("Aguardando peças", AGUARDANDO_PECAS.rotulo());
    }
}
