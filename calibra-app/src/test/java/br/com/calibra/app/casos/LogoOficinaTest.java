package br.com.calibra.app.casos;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.cadastro.DadosOficina;

class LogoOficinaTest {

    private static DadosOficina comLogo(String logo) {
        return new DadosOficina("Oficina", "", "", "", "", logo);
    }

    @Test
    void copiaAImagemParaOsAnexosComNomeQueNaoSobrescreve(@TempDir Path pasta) throws IOException {
        Path anexos = pasta.resolve("anexos");
        Path imagem = Files.write(pasta.resolve("Logo Oficina.PNG"), new byte[] {1, 2, 3, 4});

        String nome = LogoOficina.guardar(imagem, anexos);
        assertTrue(nome.matches("oficina/logo-[0-9a-f]{12}\\.png"), nome);
        assertArrayEquals(Files.readAllBytes(imagem), Files.readAllBytes(anexos.resolve(nome)));
        assertEquals(nome, LogoOficina.guardar(imagem, anexos), "a mesma imagem ganha o mesmo nome");

        Path outra = Files.write(pasta.resolve("outra.jpg"), new byte[] {5, 6, 7});
        String outroNome = LogoOficina.guardar(outra, anexos);
        assertNotEquals(nome, outroNome);
        assertTrue(Files.exists(anexos.resolve(nome)), "o logo anterior continua lá até a configuração mudar");
    }

    @Test
    void recusaOutrosFormatos(@TempDir Path pasta) throws IOException {
        Path documento = Files.write(pasta.resolve("logo.pdf"), new byte[] {1});
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> LogoOficina.guardar(documento, pasta.resolve("anexos")));
        assertTrue(erro.getMessage().contains("PNG ou JPG"));
    }

    @Test
    void achaOArquivoDoLogoSoDentroDosAnexos(@TempDir Path pasta) throws IOException {
        Path anexos = pasta.resolve("anexos");
        String nome = LogoOficina.guardar(Files.write(pasta.resolve("a.png"), new byte[] {9}), anexos);

        assertEquals(anexos.resolve(nome), LogoOficina.arquivo(comLogo(nome), anexos).orElseThrow());
        assertTrue(LogoOficina.arquivo(comLogo(null), anexos).isEmpty(), "sem logo");
        assertTrue(LogoOficina.arquivo(comLogo("oficina/sumiu.png"), anexos).isEmpty(), "arquivo apagado");
        Files.write(pasta.resolve("fora.png"), new byte[] {1});
        assertTrue(LogoOficina.arquivo(comLogo("../fora.png"), anexos).isEmpty(), "fora da pasta de anexos");
    }
}
