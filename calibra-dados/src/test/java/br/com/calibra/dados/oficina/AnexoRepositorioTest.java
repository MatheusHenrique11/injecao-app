package br.com.calibra.dados.oficina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.oficina.NumeroOs;
import br.com.calibra.core.oficina.TipoAnexo;
import br.com.calibra.dados.BaseDeTeste;
import br.com.calibra.dados.Cadastros;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.Migracoes;

/** Anexos (V7, bloco 3.2): da OS ou só do veículo, o mesmo arquivo em dois anexos, e a V7 sobre um banco na V6. */
class AnexoRepositorioTest {

    @TempDir
    Path pasta;

    private record Fixture(BaseDeTeste base, long veiculo, long os) {
    }

    private static Fixture abrir(BaseDeTeste base) {
        Cadastros.Repositorios cadastros = Cadastros.Repositorios.de(base);
        return base.banco.transacao(c -> {
            long cliente = cadastros.clientes().inserir(c, Cadastros.cliente("João da Silva"));
            long veiculo = cadastros.veiculos().inserir(c, cliente, Cadastros.carro("ABC1D23"), null);
            long os = new OrdemServicoRepositorio(base.relogio).inserir(c, new NumeroOs(2026, 1),
                    new DadosAbertura(cliente, veiculo, null, "Revisão", null, null, ""));
            return new Fixture(base, veiculo, os);
        });
    }

    @Test
    void anexoDaOsEDoVeiculo() throws Exception {
        Fixture f = abrir(BaseDeTeste.criada(pasta));
        AnexoRepositorio anexos = new AnexoRepositorio(f.base().relogio);
        long foto = f.base().banco.transacao(c -> anexos.inserir(c, f.veiculo(), f.os(), TipoAnexo.FOTO_ENTRADA,
                " Porta direita ", "veiculos/1/aaaa.jpg", "IMG_0001.jpg", 2_345_678, "aaaa", null, false));
        long crlv = f.base().banco.transacao(c -> anexos.inserir(c, f.veiculo(), null, TipoAnexo.DOCUMENTO, "CRLV 2026",
                "veiculos/1/bbbb.pdf", "crlv.pdf", 120_000, "bbbb", LocalDate.of(2027, 1, 31), true));

        AnexoSalvo lida = f.base().banco.ler(c -> anexos.buscar(c, foto)).orElseThrow();
        assertEquals("Porta direita", lida.descricao());
        assertEquals(TipoAnexo.FOTO_ENTRADA, lida.tipo());
        assertEquals(f.os(), lida.osId());
        assertNull(lida.validade());
        assertEquals(List.of(foto), f.base().banco.ler(c -> anexos.daOs(c, f.os())).stream().map(AnexoSalvo::id)
                .toList(), "o documento sem OS não aparece na OS");
        assertEquals(List.of(foto, crlv), f.base().banco.ler(c -> anexos.doVeiculo(c, f.veiculo())).stream()
                .map(AnexoSalvo::id).toList(), "a ficha do veículo vê os dois");

        f.base().banco.transacao(c -> {
            anexos.atualizar(c, crlv, TipoAnexo.DOCUMENTO, "CRLV 2026 (cópia)", LocalDate.of(2027, 2, 28), true);
            return null;
        });
        AnexoSalvo documento = f.base().banco.ler(c -> anexos.buscar(c, crlv)).orElseThrow();
        assertEquals("CRLV 2026 (cópia)", documento.descricao());
        assertEquals(LocalDate.of(2027, 2, 28), documento.validade());
        assertTrue(documento.pessoal());
    }

    /** O mesmo arquivo anexado duas vezes é uma cópia só: ela só pode ir embora quando ninguém mais a usa. */
    @Test
    void quantosUsamOMesmoArquivo() throws Exception {
        Fixture f = abrir(BaseDeTeste.criada(pasta));
        AnexoRepositorio anexos = new AnexoRepositorio(f.base().relogio);
        long primeiro = f.base().banco.transacao(c -> anexos.inserir(c, f.veiculo(), f.os(), TipoAnexo.FOTO_ENTRADA,
                "", "veiculos/1/cccc.jpg", "a.jpg", 10, "cccc", null, false));
        f.base().banco.transacao(c -> anexos.inserir(c, f.veiculo(), f.os(), TipoAnexo.FOTO_SERVICO, "",
                "veiculos/1/cccc.jpg", "a (2).jpg", 10, "cccc", null, false));
        assertEquals(2, (int) f.base().banco.ler(c -> anexos.quantosUsam(c, "veiculos/1/cccc.jpg")));
        f.base().banco.transacao(c -> {
            anexos.apagar(c, primeiro);
            return null;
        });
        assertEquals(1, (int) f.base().banco.ler(c -> anexos.quantosUsam(c, "veiculos/1/cccc.jpg")));
    }

    /** A V7 sobre um banco que já está na V6 (com uma OS): só cria a tabela, e as chaves valem. */
    @Test
    void v7SobreUmBancoNaV6() throws Exception {
        BaseDeTeste base = BaseDeTeste.semMigrar(pasta);
        Flyway.configure().dataSource(base.banco.fonte()).locations(Migracoes.LOCAL_PADRAO).target("6").load()
                .migrate();
        Fixture f = abrir(base);
        Migracoes.Resultado resultado = Migracoes.migrar(base.banco, base.backup);
        assertEquals("6", resultado.versaoAntes());
        assertEquals("7", resultado.versaoDepois());
        AnexoRepositorio anexos = new AnexoRepositorio(base.relogio);
        assertEquals(List.of(), base.banco.ler(c -> anexos.daOs(c, f.os())));
        assertThrows(ErroDados.class, () -> base.banco.transacao(c -> anexos.inserir(c, 999, null, TipoAnexo.OUTRO,
                "", "x", "x", 1, "x", null, false)), "veículo que não existe");
    }
}
