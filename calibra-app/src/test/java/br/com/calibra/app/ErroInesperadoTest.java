package br.com.calibra.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import br.com.calibra.dados.MigracaoFalhouException;

class ErroInesperadoTest {

    private static final Path LOGS = Path.of("C:", "dados", "Calibra", "logs");

    @Test
    void erroDoBancoMostraAMensagemDele() {
        RuntimeException erro = new RuntimeException("embrulho",
                new MigracaoFalhouException("A atualização do banco de dados falhou.", null));
        assertEquals("A atualização do banco de dados falhou.", ErroInesperado.mensagem(erro, LOGS));
    }

    @Test
    void outroErroMostraMensagemGeralSemDetalheTecnico() {
        String mensagem = ErroInesperado.mensagem(new NullPointerException("campo x"), LOGS);
        assertTrue(mensagem.startsWith("Aconteceu um erro inesperado."), mensagem);
        assertTrue(mensagem.contains(LOGS.toString()), mensagem);
        assertFalse(mensagem.contains("NullPointerException"), mensagem);
    }
}
