package br.com.calibra.app.casos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import br.com.calibra.core.util.NumeroBr;
import br.com.calibra.dados.backup.Backup;
import br.com.calibra.dados.backup.InfoBackup;
import br.com.calibra.dados.backup.MotivoBackup;
import br.com.calibra.dados.backup.Restauracao;
import br.com.calibra.dados.config.Chave;
import br.com.calibra.dados.config.ConfiguracaoBackup;
import br.com.calibra.dados.config.ConfiguracaoRepositorio;

/**
 * Regras do backup com o app aberto (SPEC 10): para qual pasta vai, quando o automático vence, quando o
 * de fechar é preciso, e a restauração. Sem JavaFX: a tela roda estes métodos em segundo plano, um de cada
 * vez.
 */
public final class ServicoBackup {

    /** Backup automático: um por dia (SPEC 10). */
    public static final Duration INTERVALO_AUTOMATICO = Duration.ofHours(24);

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Backup backup;
    private final ConfiguracaoRepositorio configuracao;
    private final Clock relogio;
    /** Gravações no banco até o último backup ou restauração feitos com o app aberto. */
    private long gravacoesNoUltimoBackup;

    public ServicoBackup(Backup backup, ConfiguracaoRepositorio configuracao, Clock relogio) {
        this.backup = Objects.requireNonNull(backup, "backup");
        this.configuracao = Objects.requireNonNull(configuracao, "configuracao");
        this.relogio = Objects.requireNonNull(relogio, "relogio");
        this.gravacoesNoUltimoBackup = backup.banco().gravacoes();
    }

    // ===================== Pasta =====================

    public ConfiguracaoBackup configuracaoAtual() {
        return configuracao.ler(Chave.BACKUP);
    }

    /** Para onde vai o próximo backup: a pasta escolhida, se estiver acessível; senão, a padrão. */
    public Path pasta() {
        return configuracaoAtual().pastaEfetiva(backup.pastas());
    }

    public Path pastaPadrao() {
        return backup.pastas().backupsPadrao();
    }

    /** Aviso para a tela quando a pasta escolhida não está acessível (pen drive ou HD externo desligado). */
    public Optional<String> avisoDaPasta() {
        ConfiguracaoBackup atual = configuracaoAtual();
        if (atual.usaPastaPadrao() || atual.pastaEscolhidaDisponivel()) {
            return Optional.empty();
        }
        return Optional.of("A pasta de backup escolhida (" + atual.pasta() + ") não está acessível agora. "
                + "O pen drive ou o HD externo está ligado? Enquanto isso, os backups vão para a pasta padrão ("
                + pastaPadrao() + ").");
    }

    /**
     * Passa a gravar os backups nesta pasta.
     *
     * @throws IllegalArgumentException se a pasta não existir ou não der para gravar nela (mensagem para a tela)
     */
    public void escolherPasta(String pasta) {
        Path escolhida;
        try {
            escolhida = Path.of(pasta).toAbsolutePath().normalize();
        } catch (InvalidPathException erro) {
            throw new IllegalArgumentException("Caminho de pasta inválido: " + pasta, erro);
        }
        if (!Files.isDirectory(escolhida)) {
            throw new IllegalArgumentException("A pasta " + escolhida + " não existe ou não está acessível.");
        }
        try {
            Files.delete(Files.createTempFile(escolhida, "calibra-teste-", ".tmp"));
        } catch (IOException | SecurityException erro) {
            throw new IllegalArgumentException("Não dá para gravar na pasta " + escolhida
                    + ". Escolha outra pasta ou confira se o pen drive não está protegido contra gravação.", erro);
        }
        configuracao.gravar(Chave.BACKUP, new ConfiguracaoBackup(escolhida.toString()));
    }

    public void usarPastaPadrao() {
        configuracao.gravar(Chave.BACKUP, ConfiguracaoBackup.padrao());
    }

    // ===================== Backup =====================

    /**
     * O backup automático está vencido? Vence quando a pasta atual não tem backup ou o mais recente tem mais
     * de 24 h. Backup com data no futuro (relógio do computador mudou) também conta como vencido.
     */
    public boolean automaticoVencido() {
        LocalDateTime agora = LocalDateTime.now(relogio);
        return Backup.maisRecente(pasta())
                .map(InfoBackup::manifesto)
                .map(manifesto -> manifesto.criadoEm().isBefore(agora.minus(INTERVALO_AUTOMATICO))
                        || manifesto.criadoEm().isAfter(agora.plusHours(1)))
                .orElse(true);
    }

    /** Faz o backup automático se ele estiver vencido. */
    public synchronized Optional<InfoBackup> fazerAutomaticoSeVencido() {
        return automaticoVencido() ? Optional.of(fazer(MotivoBackup.AUTOMATICO)) : Optional.empty();
    }

    /** Ao fechar, só precisa de backup se algo foi gravado depois do último backup feito com o app aberto. */
    public boolean precisaBackupAoFechar() {
        return backup.banco().gravacoes() > gravacoesNoUltimoBackup;
    }

    /** Faz o backup de fechar, se for preciso. */
    public synchronized Optional<InfoBackup> fazerAoFecharSePreciso() {
        return precisaBackupAoFechar() ? Optional.of(fazer(MotivoBackup.AO_FECHAR)) : Optional.empty();
    }

    /** Faz um backup agora, na pasta atual. */
    public synchronized InfoBackup fazer(MotivoBackup motivo) {
        long gravacoes = backup.banco().gravacoes();
        InfoBackup feito = backup.fazer(pasta(), motivo);
        gravacoesNoUltimoBackup = gravacoes;
        return feito;
    }

    /** O backup mais recente da pasta atual. */
    public Optional<InfoBackup> ultimo() {
        return Backup.maisRecente(pasta());
    }

    /**
     * Backups que dá para restaurar: os da pasta atual e os da pasta padrão (onde ficam os backups feitos
     * antes de atualizar o banco ou de restaurar), do mais novo para o mais antigo.
     */
    public List<InfoBackup> listar() {
        Set<Path> pastas = new LinkedHashSet<>(List.of(pasta(), pastaPadrao()));
        return pastas.stream()
                .flatMap(pasta -> Backup.listar(pasta).stream())
                .sorted(Comparator.comparing((InfoBackup info) -> info.manifesto().criadoEm()).reversed())
                .toList();
    }

    // ===================== Restauração =====================

    /**
     * Troca os dados atuais pelos do backup (os atuais vão antes para um backup de segurança).
     *
     * @return o backup de segurança com os dados de antes
     * @throws br.com.calibra.dados.backup.BackupInvalidoException se o arquivo não servir (nada muda)
     */
    public synchronized InfoBackup restaurar(Path zip) {
        InfoBackup seguranca = new Restauracao(backup).restaurar(zip);
        gravacoesNoUltimoBackup = backup.banco().gravacoes();
        return seguranca;
    }

    // ===================== Texto para a tela =====================

    /** Ex.: "12/10/2026 14:50 · automático (diário) · 1,2 MB". */
    public static String descrever(InfoBackup info) {
        return DATA_HORA.format(info.manifesto().criadoEm()) + " · " + info.manifesto().motivo().rotulo() + " · "
                + NumeroBr.tamanhoArquivo(info.tamanhoBytes());
    }
}
