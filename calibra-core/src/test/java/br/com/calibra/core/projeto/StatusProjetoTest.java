package br.com.calibra.core.projeto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Status do projeto (SPEC 6.4): a sequência, um passo de cada vez, e arquivar a qualquer momento. */
class StatusProjetoTest {

    private static final List<StatusProjeto> SEQUENCIA = List.of(StatusProjeto.RASCUNHO, StatusProjeto.DIMENSIONADO,
            StatusProjeto.ORCADO, StatusProjeto.APROVADO, StatusProjeto.MAPA_BASE, StatusProjeto.EM_ACERTO,
            StatusProjeto.CONCLUIDO);

    @Test
    void sequenciaDaSpec() {
        for (int i = 0; i < SEQUENCIA.size() - 1; i++) {
            assertEquals(Optional.of(SEQUENCIA.get(i + 1)), SEQUENCIA.get(i).proximo());
            assertTrue(SEQUENCIA.get(i).podeIrPara(SEQUENCIA.get(i + 1)));
        }
        assertTrue(StatusProjeto.CONCLUIDO.proximo().isEmpty());
        assertTrue(StatusProjeto.ARQUIVADO.proximo().isEmpty());
    }

    @Test
    void naoPulaPassosNemVoltaNaSequencia() {
        assertFalse(StatusProjeto.RASCUNHO.podeIrPara(StatusProjeto.ORCADO), "pular passo");
        assertFalse(StatusProjeto.ORCADO.podeIrPara(StatusProjeto.DIMENSIONADO), "voltar");
        assertFalse(StatusProjeto.RASCUNHO.podeIrPara(StatusProjeto.RASCUNHO), "ficar no mesmo");
        assertFalse(StatusProjeto.RASCUNHO.podeIrPara(null));
    }

    @ParameterizedTest
    @EnumSource(value = StatusProjeto.class, names = "ARQUIVADO", mode = EnumSource.Mode.EXCLUDE)
    void arquivaAQualquerMomentoEVoltaDoArquivo(StatusProjeto status) {
        assertTrue(status.podeIrPara(StatusProjeto.ARQUIVADO));
        assertTrue(StatusProjeto.ARQUIVADO.podeIrPara(status));
    }
}
