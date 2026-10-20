package br.com.calibra.app.casos;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.cadastro.Cliente;
import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.core.oficina.DadosPessoais;
import br.com.calibra.core.oficina.LaudoEletrico;
import br.com.calibra.dados.BancoDados;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.cadastro.ClienteRepositorio;
import br.com.calibra.dados.cadastro.VeiculoRepositorio;
import br.com.calibra.dados.oficina.LaudoRepositorio;
import br.com.calibra.dados.oficina.LaudoSalvo;
import br.com.calibra.dados.oficina.MecanicoRepositorio;
import br.com.calibra.dados.oficina.MecanicoSalvo;
import br.com.calibra.dados.oficina.OrdemServicoRepositorio;
import br.com.calibra.dados.oficina.OrdemServicoSalva;
import br.com.calibra.relatorios.DadosLaudo;

/**
 * Os laudos de diagnóstico elétrico da OS (Fase 3B, SPEC 6.6.3): o laudo novo já preenchido, criar, salvar e apagar,
 * os laudos da OS e os do veículo, e os dados do PDF. O laudo se muda até a entrega; depois, só se lê (teste nº 6 da
 * fase). Ao gravar, avisa se a queixa parece ter dado pessoal do cliente (LGPD): o laudo é dado técnico e fica mesmo
 * se o cliente for anonimizado (teste nº 7). Sem JavaFX.
 */
public final class CasosLaudo {

    /**
     * O laudo novo, já preenchido: a data e a hora de agora, a queixa (o serviço pedido da OS, nas palavras do
     * cliente) e o mecânico da OS, se estiver ativo.
     *
     * @param mecanicoId null = nenhum
     */
    public record Rascunho(LaudoEletrico laudo, Long mecanicoId) {

        public Rascunho {
            Objects.requireNonNull(laudo, "laudo");
        }
    }

    /** O laudo gravado e os avisos que não impediram (o de LGPD). */
    public record Gravado(long laudoId, List<String> avisos) {

        public Gravado {
            avisos = List.copyOf(avisos);
        }
    }

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final BancoDados banco;
    private final Clock relogio;
    private final LaudoRepositorio laudos;
    private final OrdemServicoRepositorio ordens;
    private final MecanicoRepositorio mecanicos;
    private final ClienteRepositorio clientes;
    private final VeiculoRepositorio veiculos;

    public CasosLaudo(BancoDados banco, Clock relogio) {
        this.banco = Objects.requireNonNull(banco, "banco");
        this.relogio = Objects.requireNonNull(relogio, "relogio");
        this.laudos = new LaudoRepositorio(relogio);
        this.ordens = new OrdemServicoRepositorio(relogio);
        this.mecanicos = new MecanicoRepositorio(relogio);
        this.clientes = new ClienteRepositorio(relogio);
        this.veiculos = new VeiculoRepositorio(relogio);
    }

    // ===================== Consultas =====================

    /** Os laudos da OS, do mais antigo para o mais novo. */
    public List<LaudoSalvo> daOs(long osId) {
        return banco.ler(c -> laudos.daOs(c, osId));
    }

    /** Os laudos de todas as OS do veículo, do mais novo para o mais antigo (para a ficha do veículo). */
    public List<LaudoSalvo> doVeiculo(long veiculoId) {
        return banco.ler(c -> laudos.doVeiculo(c, veiculoId));
    }

    public Optional<LaudoSalvo> buscar(long laudoId) {
        return banco.ler(c -> laudos.buscar(c, laudoId));
    }

    /** O laudo novo, já preenchido (a data e a hora de agora, a queixa e o mecânico da OS). */
    public Rascunho rascunho(long osId) {
        return banco.ler(c -> {
            OrdemServicoSalva os = exigirEmAndamento(c, osId, "não ganha laudo novo.");
            Long mecanico = os.mecanicoId() == null ? null : mecanicos.buscar(c, os.mecanicoId())
                    .filter(MecanicoSalvo::ativo).map(MecanicoSalvo::id).orElse(null);
            LaudoEletrico laudo = new LaudoEletrico(agora(), os.servicoPedido(), List.of(), List.of(), "", "", "",
                    "", null);
            return new Rascunho(laudo, mecanico);
        });
    }

    /**
     * Os dados do PDF do laudo: a oficina e a versão, como estão agora, e o cliente como está no cadastro
     * (anonimizado, se for o caso).
     *
     * @param logo o arquivo do logo da oficina; null sem logo
     */
    public DadosLaudo documento(long laudoId, DadosOficina oficina, Path logo, String versaoApp) {
        return banco.ler(c -> {
            LaudoSalvo laudo = exigir(c, laudoId);
            OrdemServicoSalva os = ordens.buscar(c, laudo.osId()).orElseThrow();
            Cliente cliente = clientes.buscar(c, os.clienteId()).orElseThrow().cliente();
            return new DadosLaudo(oficina, logo, os.numero().texto(), cliente.nome(),
                    CasosOrcamento.textoVeiculo(veiculos.buscar(c, os.veiculoId()).orElseThrow().veiculo()),
                    laudo.mecanico(), laudo.laudo(), versaoApp, LocalDateTime.now(relogio).withSecond(0).withNano(0));
        });
    }

    // ===================== Gravação =====================

    /**
     * Laudo novo na OS (em qualquer etapa antes da entrega).
     *
     * @param mecanicoId null = sem mecânico
     * @throws EntradaInvalidaException com tudo o que falta ou está errado (nada é gravado)
     */
    public Gravado criar(long osId, LaudoEletrico laudo, Long mecanicoId) {
        exigirValido(laudo);
        return banco.transacao(c -> {
            OrdemServicoSalva os = exigirEmAndamento(c, osId, "não ganha laudo novo.");
            exigirMecanicoAtivo(c, mecanicoId);
            exigirData(laudo, os);
            long id = laudos.inserir(c, osId, mecanicoId, laudo);
            return new Gravado(id, avisos(c, os, laudo));
        });
    }

    /**
     * Grava o laudo como está agora (até a entrega). O mecânico que já estava no laudo continua valendo, mesmo se foi
     * desativado depois; o novo precisa estar ativo.
     *
     * @return os avisos que não impediram (o de LGPD)
     */
    public List<String> salvar(long laudoId, LaudoEletrico laudo, Long mecanicoId) {
        exigirValido(laudo);
        return banco.transacao(c -> {
            LaudoSalvo atual = exigir(c, laudoId);
            OrdemServicoSalva os = exigirEmAndamento(c, atual.osId(), "o laudo não muda mais.");
            if (!Objects.equals(mecanicoId, atual.mecanicoId())) {
                exigirMecanicoAtivo(c, mecanicoId);
            }
            exigirData(laudo, os);
            laudos.salvar(c, laudoId, mecanicoId, laudo);
            return avisos(c, os, laudo);
        });
    }

    /** Apaga o laudo feito por engano (só com a OS em andamento, como os anexos). */
    public void apagar(long laudoId) {
        banco.transacao(c -> {
            LaudoSalvo atual = exigir(c, laudoId);
            exigirEmAndamento(c, atual.osId(), "o laudo não é apagado.");
            laudos.apagar(c, laudoId);
            return null;
        });
    }

    // ===================== Regras =====================

    private static void exigirValido(LaudoEletrico laudo) {
        List<String> erros = laudo == null ? List.of("Preencha o laudo.") : laudo.validar();
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
    }

    /** A data e a hora do laudo: nem no futuro, nem antes da abertura da OS. */
    private void exigirData(LaudoEletrico laudo, OrdemServicoSalva os) {
        List<String> erros = new ArrayList<>();
        if (laudo.dataHora().isAfter(agora().plusMinutes(1))) {
            erros.add("A data e a hora do laudo não podem estar no futuro.");
        }
        LocalDateTime abertura = os.criadoEm().truncatedTo(ChronoUnit.MINUTES);
        if (laudo.dataHora().isBefore(abertura)) {
            erros.add("A data do laudo não pode ser antes da abertura da OS (" + abertura.format(DATA_HORA) + ").");
        }
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
    }

    /**
     * O aviso de LGPD (decisão do dono em 20/10/2026): só quando a queixa parece ter dado pessoal do cliente. O nome
     * do cliente já anonimizado não conta.
     */
    private List<String> avisos(Connection c, OrdemServicoSalva os, LaudoEletrico laudo) throws SQLException {
        Cliente cliente = clientes.buscar(c, os.clienteId()).orElseThrow().cliente();
        List<String> indicios = DadosPessoais.indicios(laudo.queixa(), cliente.anonimizado() ? null : cliente.nome());
        if (indicios.isEmpty()) {
            return List.of();
        }
        return List.of("A queixa parece ter dado pessoal do cliente: " + String.join(", ", indicios) + ". O laudo "
                + "fica mesmo se o cliente for anonimizado; se for dado pessoal, tire da queixa e salve de novo.");
    }

    private OrdemServicoSalva exigirEmAndamento(Connection c, long osId, String oQue) throws SQLException {
        OrdemServicoSalva os = ordens.buscar(c, osId).orElseThrow(
                () -> new OperacaoRecusadaException("OS #" + osId + " não encontrada."));
        if (os.status().finalizada()) {
            throw new OperacaoRecusadaException("A OS " + os.numero().texto() + " está \"" + os.status().rotulo()
                    + "\": " + oQue);
        }
        return os;
    }

    private LaudoSalvo exigir(Connection c, long laudoId) throws SQLException {
        return laudos.buscar(c, laudoId).orElseThrow(
                () -> new OperacaoRecusadaException("Laudo #" + laudoId + " não encontrado."));
    }

    private void exigirMecanicoAtivo(Connection c, Long mecanicoId) throws SQLException {
        if (mecanicoId == null) {
            return;
        }
        MecanicoSalvo mecanico = mecanicos.buscar(c, mecanicoId).orElseThrow(
                () -> new OperacaoRecusadaException("Mecânico #" + mecanicoId + " não encontrado."));
        if (!mecanico.ativo()) {
            throw new OperacaoRecusadaException("O mecânico " + mecanico.nome() + " está inativo.");
        }
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio).truncatedTo(ChronoUnit.MINUTES);
    }
}
