package br.com.calibra.dados.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.core.orcamento.Desconto;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.PadroesOrcamento;
import br.com.calibra.core.semaforo.LimiaresSemaforo;
import br.com.calibra.dados.BaseDeTeste;

class ConfiguracaoRepositorioTest {

    @TempDir
    Path pasta;

    @Test
    void semNadaSalvoVemOPadrao() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta);
        assertEquals(DadosOficina.vazia(), base.configuracao.ler(Chave.OFICINA));
        assertEquals(LimiaresSemaforo.padrao(), base.configuracao.ler(Chave.CALCULO).limiares());
        assertTrue(base.configuracao.ler(Chave.BACKUP).usaPastaPadrao());
    }

    /** Bloco 2.5: padrões do orçamento, com desconto em percentual e valor da hora. */
    @Test
    void padroesDoOrcamento() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta);
        assertEquals(PadroesOrcamento.padrao(), base.configuracao.ler(Chave.ORCAMENTO));
        PadroesOrcamento editados = new PadroesOrcamento(Desconto.emPercentual(new BigDecimal("7.5")), 12,
                "a partir da chegada das peças", 10, "50% na aprovação e 50% na entrega", Dinheiro.ler("180,00"));
        base.configuracao.gravar(Chave.ORCAMENTO, editados);
        assertEquals(editados, base.configuracao.ler(Chave.ORCAMENTO));
    }

    @Test
    void gravaLeEsubstitui() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta);
        DadosOficina oficina = new DadosOficina("Oficina", "11.222.333/0001-81", "Rua A, 10", "(11) 99999-0000",
                "contato@oficina.com.br", "oficina-logo.png");
        base.configuracao.gravar(Chave.OFICINA, oficina);
        assertEquals(oficina, base.configuracao.ler(Chave.OFICINA));

        DadosOficina outra = new DadosOficina("Outra", "", "", "", "", null);
        base.configuracao.gravar(Chave.OFICINA, outra);
        assertEquals(outra, base.configuracao.ler(Chave.OFICINA));
    }

    @Test
    void parametrosComLimiaresEditados() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta);
        LimiaresSemaforo editados = new LimiaresSemaforo(0.75, 0.82, 0.30, 0.15, 13.0, 20.0, 1.10, 20.0, 25.0, 1.2,
                1.0, 0.90, 0.98);
        Parametros p = Parametros.padrao().comKAr(9.0);
        Parametros comLimiares = new Parametros(p.caminhoAr(), p.kAr(), p.bsfc(), p.dcMax(), p.etaC(), p.eps(),
                p.perdaDescargaKpa(), p.perdaAdmissaoKpa(), p.acrescimoTmAspiradoC(), p.densidadeFluidoTesteKgL(),
                p.tiMinimoMarchaLentaMs(), p.gruposSimultaneo(), editados);
        base.configuracao.gravar(Chave.CALCULO, comLimiares);
        assertEquals(comLimiares, base.configuracao.ler(Chave.CALCULO));
    }

    /** Configuração salva por uma versão antiga, sem um campo novo: o campo vem com o padrão, não com zero. */
    @Test
    void campoQueFaltaVemDoPadrao() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta);
        gravarJsonCru(base, "calculo", "{\"kAr\":9.5,\"limiares\":{\"dcVerdeAte\":0.78},\"campoQueSumiu\":1}");

        Parametros lido = base.configuracao.ler(Chave.CALCULO);
        assertEquals(9.5, lido.kAr());
        assertEquals(0.78, lido.limiares().dcVerdeAte());
        assertEquals(LimiaresSemaforo.padrao().dcAmareloAte(), lido.limiares().dcAmareloAte());
        assertEquals(Parametros.padrao().etaC(), lido.etaC());
        assertEquals(Parametros.padrao().caminhoAr(), lido.caminhoAr());
    }

    @Test
    void pastaDeBackupEscolhidaSoValeSeEstiverDisponivel() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta.resolve("dados"));
        Path penDrive = Files.createDirectories(pasta.resolve("pen-drive"));
        ConfiguracaoBackup escolhida = new ConfiguracaoBackup(penDrive.toString());
        assertEquals(penDrive, escolhida.pastaEfetiva(base.pastas));

        ConfiguracaoBackup desligada = new ConfiguracaoBackup(pasta.resolve("pen-drive-fora").toString());
        assertFalse(desligada.pastaEscolhidaDisponivel());
        assertEquals(base.pastas.backupsPadrao(), desligada.pastaEfetiva(base.pastas));

        assertTrue(new ConfiguracaoBackup("  ").usaPastaPadrao());
        base.configuracao.gravar(Chave.BACKUP, escolhida);
        assertEquals(escolhida, base.configuracao.ler(Chave.BACKUP));
    }

    private static void gravarJsonCru(BaseDeTeste base, String chave, String json) {
        base.banco.transacao(conexao -> {
            try (PreparedStatement insercao = conexao.prepareStatement(
                    "INSERT INTO configuracao (chave, valor_json, atualizado_em) VALUES (?, ?, '2026-01-01T00:00:00')")) {
                insercao.setString(1, chave);
                insercao.setString(2, json);
                return insercao.executeUpdate();
            }
        });
    }
}
