package br.com.calibra.app.casos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.dados.BancoDados;
import br.com.calibra.dados.Migracoes;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.backup.Backup;
import br.com.calibra.dados.backup.InfoBackup;
import br.com.calibra.dados.backup.MotivoBackup;
import br.com.calibra.dados.config.Chave;
import br.com.calibra.dados.config.ConfiguracaoRepositorio;

/** Regras do backup com o app aberto (SPEC 10), sem JavaFX. */
class ServicoBackupTest {

    @TempDir
    Path raiz;

    private final RelogioAjustavel relogio = new RelogioAjustavel(LocalDateTime.of(2026, 10, 12, 9, 0));
    private PastasDados pastas;
    private ConfiguracaoRepositorio configuracao;
    private ServicoBackup servico;

    @BeforeEach
    void abrirBanco() throws IOException {
        pastas = new PastasDados(raiz.resolve("Calibra"));
        pastas.criar();
        BancoDados banco = new BancoDados(pastas.banco());
        Backup backup = new Backup(pastas, banco, "0.2.0-teste", relogio);
        Migracoes.migrar(banco, backup);
        configuracao = new ConfiguracaoRepositorio(banco, relogio);
        servico = new ServicoBackup(backup, configuracao, relogio);
    }

    private void gravarOficina(String nome) {
        configuracao.gravar(Chave.OFICINA, new DadosOficina(nome, "", "", "", "", null));
    }

    private Path penDrive() throws IOException {
        return Files.createDirectories(raiz.resolve("pen-drive"));
    }

    @Test
    void automaticoVenceSemBackupEDepoisDe24Horas() {
        assertTrue(servico.automaticoVencido(), "pasta sem backup");
        InfoBackup primeiro = servico.fazerAutomaticoSeVencido().orElseThrow();
        assertEquals(MotivoBackup.AUTOMATICO, primeiro.manifesto().motivo());
        assertEquals(pastas.backupsPadrao(), primeiro.arquivo().getParent());

        assertTrue(servico.fazerAutomaticoSeVencido().isEmpty(), "acabou de fazer");
        relogio.andar(Duration.ofHours(23));
        assertTrue(servico.fazerAutomaticoSeVencido().isEmpty(), "23 h depois ainda não venceu");
        relogio.andar(Duration.ofHours(2));
        assertTrue(servico.fazerAutomaticoSeVencido().isPresent(), "25 h depois venceu");
        assertEquals(2, Backup.listar(pastas.backupsPadrao()).size());
    }

    @Test
    void qualquerBackupRecenteContaComoODoDia() {
        servico.fazer(MotivoBackup.MANUAL);
        relogio.andar(Duration.ofHours(12));
        assertFalse(servico.automaticoVencido());
    }

    @Test
    void backupComDataNoFuturoNaoTravaOAutomatico() {
        servico.fazer(MotivoBackup.MANUAL);
        relogio.andar(Duration.ofDays(-2)); // o relógio do computador foi atrasado
        assertTrue(servico.automaticoVencido());
    }

    @Test
    void aoFecharSoFazBackupSeAlgoMudou() {
        assertFalse(servico.precisaBackupAoFechar(), "nada gravado desde que abriu");
        assertTrue(servico.fazerAoFecharSePreciso().isEmpty());

        gravarOficina("Oficina A");
        assertTrue(servico.precisaBackupAoFechar());
        InfoBackup feito = servico.fazerAoFecharSePreciso().orElseThrow();
        assertEquals(MotivoBackup.AO_FECHAR, feito.manifesto().motivo());
        assertFalse(servico.precisaBackupAoFechar(), "o backup já tem a mudança");

        gravarOficina("Oficina B");
        servico.fazerAutomaticoSeVencido();
        assertTrue(servico.precisaBackupAoFechar(), "o automático não estava vencido: a mudança não tem backup");
        servico.fazer(MotivoBackup.MANUAL);
        assertFalse(servico.precisaBackupAoFechar());
    }

    @Test
    void pastaEscolhidaDesligadaVoltaParaAPadraoComAviso() throws IOException {
        Path pen = penDrive();
        servico.escolherPasta(pen.toString());
        assertEquals(pen, servico.pasta());
        assertTrue(servico.avisoDaPasta().isEmpty());
        assertEquals(pen, servico.fazer(MotivoBackup.MANUAL).arquivo().getParent());

        Files.move(pen, raiz.resolve("pen-drive-fora")); // pen drive tirado
        assertEquals(pastas.backupsPadrao(), servico.pasta());
        String aviso = servico.avisoDaPasta().orElseThrow();
        assertTrue(aviso.contains(pen.toString()) && aviso.contains("pasta padrão"), aviso);
        assertEquals(pastas.backupsPadrao(), servico.fazer(MotivoBackup.MANUAL).arquivo().getParent());

        servico.usarPastaPadrao();
        assertTrue(servico.configuracaoAtual().usaPastaPadrao());
        assertTrue(servico.avisoDaPasta().isEmpty());
    }

    @Test
    void pastaQueNaoExisteERecusada() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> servico.escolherPasta(raiz.resolve("nao-existe").toString()));
        assertTrue(erro.getMessage().contains("não existe"), erro.getMessage());
        assertTrue(servico.configuracaoAtual().usaPastaPadrao(), "nada mudou");
    }

    @Test
    void listaJuntaAPastaAtualEAPadraoDoMaisNovoParaOMaisAntigo() throws IOException {
        InfoBackup naPadrao = servico.fazer(MotivoBackup.MANUAL);
        servico.escolherPasta(penDrive().toString());
        relogio.andar(Duration.ofHours(1));
        InfoBackup noPen = servico.fazer(MotivoBackup.MANUAL);

        List<InfoBackup> lista = servico.listar();
        assertEquals(List.of(noPen.arquivo(), naPadrao.arquivo()), lista.stream().map(InfoBackup::arquivo).toList());
        assertEquals(noPen.arquivo(), servico.ultimo().orElseThrow().arquivo());
    }

    @Test
    void restaurarDevolveOsDadosENaoPedeBackupAoFechar() {
        gravarOficina("Antes");
        InfoBackup antes = servico.fazer(MotivoBackup.MANUAL);
        relogio.andar(Duration.ofMinutes(5));
        gravarOficina("Depois");

        InfoBackup seguranca = servico.restaurar(antes.arquivo());
        assertEquals("Antes", configuracao.ler(Chave.OFICINA).nome());
        assertEquals(MotivoBackup.ANTES_DE_RESTAURAR, seguranca.manifesto().motivo());
        assertFalse(servico.precisaBackupAoFechar(), "os dados restaurados já são um backup");
    }

    @Test
    void descreveOBackupParaATela() {
        InfoBackup feito = servico.fazer(MotivoBackup.MANUAL);
        assertTrue(ServicoBackup.descrever(feito).startsWith("12/10/2026 09:00 · feito por você · "),
                ServicoBackup.descrever(feito));
        assertEquals("1 KB", ServicoBackup.tamanho(300));
        assertEquals("35 KB", ServicoBackup.tamanho(35 * 1024));
        assertEquals("1,5 MB", ServicoBackup.tamanho(1_572_864));
    }
}
