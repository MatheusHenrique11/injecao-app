package br.com.calibra.app.casos;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.cadastro.Cliente;
import br.com.calibra.core.cadastro.Veiculo;
import br.com.calibra.core.calculo.CalculoDimensionamento;
import br.com.calibra.core.calculo.Dimensionamento;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.memoria.VersaoNucleo;
import br.com.calibra.core.motor.ComparacaoMotor;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.motor.ValidadorMotor;
import br.com.calibra.core.projeto.Meta;
import br.com.calibra.core.projeto.StatusProjeto;
import br.com.calibra.core.semaforo.Avaliacao;
import br.com.calibra.core.semaforo.Cor;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.semaforo.Justificativa;
import br.com.calibra.dados.BancoDados;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.cadastro.ClienteRepositorio;
import br.com.calibra.dados.cadastro.ClienteSalvo;
import br.com.calibra.dados.cadastro.EventoAuditoria;
import br.com.calibra.dados.cadastro.VeiculoRepositorio;
import br.com.calibra.dados.cadastro.VeiculoSalvo;
import br.com.calibra.dados.projeto.CalculoRepositorio;
import br.com.calibra.dados.projeto.CalculoSalvo;
import br.com.calibra.dados.projeto.FiltroProjetos;
import br.com.calibra.dados.projeto.MotorRepositorio;
import br.com.calibra.dados.projeto.MudancaStatus;
import br.com.calibra.dados.projeto.ProjetoRepositorio;
import br.com.calibra.dados.projeto.ProjetoSalvo;
import br.com.calibra.dados.projeto.ResumoCalculo;
import br.com.calibra.dados.projeto.ResumoProjeto;
import br.com.calibra.dados.projeto.VersaoMotor;

/**
 * Casos de uso do projeto (SPEC 6 e 8): criar pelo assistente, editar cliente e veículo, gravar versões do
 * motor e salvar cálculos. Confere as regras do núcleo antes de gravar e grava tudo ou nada. Sem JavaFX.
 */
public final class CasosProjeto {

    /** Cliente do projeto novo: um que já existe ou um novo. */
    public sealed interface EscolhaCliente permits ClienteExistente, ClienteNovo {
    }

    public record ClienteExistente(long id) implements EscolhaCliente {
    }

    public record ClienteNovo(Cliente dados) implements EscolhaCliente {
    }

    /** Veículo do projeto novo: um do cliente ou um novo. */
    public sealed interface EscolhaVeiculo permits VeiculoExistente, VeiculoNovo {
    }

    public record VeiculoExistente(long id) implements EscolhaVeiculo {
    }

    public record VeiculoNovo(Veiculo dados) implements EscolhaVeiculo {
    }

    /** O que o assistente de novo projeto junta (SPEC 8.2): cliente, veículo, motor e meta. */
    public record NovoProjeto(EscolhaCliente cliente, EscolhaVeiculo veiculo, String descricaoMotor,
            ConfiguracaoMotor motor, Meta meta) {
    }

    /** O projeto aberto, com o cliente, o veículo e a versão atual do motor. */
    public record ProjetoAberto(ProjetoSalvo projeto, ClienteSalvo cliente, VeiculoSalvo veiculo, VersaoMotor motor) {
    }

    /**
     * Um cálculo salvo e o que mudou desde ele (SPEC 6.3: "desatualizado — recalcular").
     *
     * @param motivos vazio = em dia com o projeto
     */
    public record SituacaoCalculo(ResumoCalculo calculo, List<String> motivos) {

        public SituacaoCalculo {
            motivos = List.copyOf(motivos);
        }

        public boolean desatualizado() {
            return !motivos.isEmpty();
        }
    }

    private static final int LIMITE_LISTA = 500;
    private static final int LIMITE_CLIENTES = 50;

    private final BancoDados banco;
    private final Clock relogio;
    private final String versaoApp;
    private final ClienteRepositorio clientes;
    private final VeiculoRepositorio veiculos;
    private final ProjetoRepositorio projetos;
    private final MotorRepositorio motores;
    private final CalculoRepositorio calculos;

    public CasosProjeto(BancoDados banco, Clock relogio, String versaoApp) {
        this.banco = Objects.requireNonNull(banco, "banco");
        this.relogio = Objects.requireNonNull(relogio, "relogio");
        this.versaoApp = Objects.requireNonNull(versaoApp, "versaoApp");
        this.clientes = new ClienteRepositorio(relogio);
        this.veiculos = new VeiculoRepositorio(relogio);
        this.projetos = new ProjetoRepositorio(relogio);
        this.motores = new MotorRepositorio(relogio);
        this.calculos = new CalculoRepositorio(relogio);
    }

    // ===================== Consultas =====================

    public List<ResumoProjeto> pesquisar(FiltroProjetos filtro) {
        return banco.ler(c -> projetos.pesquisar(c, filtro, LIMITE_LISTA));
    }

    public List<ClienteSalvo> pesquisarClientes(String termo) {
        return banco.ler(c -> clientes.pesquisar(c, termo, LIMITE_CLIENTES));
    }

    public List<VeiculoSalvo> veiculosDoCliente(long clienteId) {
        return banco.ler(c -> veiculos.doCliente(c, clienteId, false));
    }

    public ProjetoAberto abrir(long projetoId) {
        return banco.ler(c -> {
            ProjetoSalvo projeto = projetos.buscar(c, projetoId).orElseThrow(
                    () -> new OperacaoRecusadaException("Projeto #" + projetoId + " não encontrado."));
            VeiculoSalvo veiculo = veiculos.buscar(c, projeto.veiculoId()).orElseThrow();
            ClienteSalvo cliente = clientes.buscar(c, veiculo.clienteId()).orElseThrow();
            VersaoMotor motor = motores.atual(c, projetoId).orElseThrow();
            return new ProjetoAberto(projeto, cliente, veiculo, motor);
        });
    }

    public List<MudancaStatus> historicoStatus(long projetoId) {
        return banco.ler(c -> projetos.historicoStatus(c, projetoId));
    }

    public List<EventoAuditoria> historicoCliente(long clienteId) {
        return banco.ler(c -> clientes.historico(c, clienteId));
    }

    public List<EventoAuditoria> historicoVeiculo(long veiculoId) {
        return banco.ler(c -> veiculos.historico(c, veiculoId));
    }

    public List<VersaoMotor> versoesMotor(long projetoId) {
        return banco.ler(c -> motores.versoes(c, projetoId));
    }

    /** Cálculos salvos do projeto, do mais novo para o mais antigo, com o que mudou desde cada um. */
    public List<SituacaoCalculo> calculos(long projetoId) {
        return banco.ler(c -> {
            VersaoMotor atual = motores.atual(c, projetoId).orElseThrow();
            List<SituacaoCalculo> situacoes = new ArrayList<>();
            for (ResumoCalculo calculo : calculos.doProjeto(c, projetoId)) {
                situacoes.add(new SituacaoCalculo(calculo, motivos(calculo, atual)));
            }
            return situacoes;
        });
    }

    public Optional<CalculoSalvo> calculo(long calculoId) {
        return banco.ler(c -> calculos.buscar(c, calculoId));
    }

    public Optional<CalculoSalvo> ultimoCalculo(long projetoId) {
        return banco.ler(c -> calculos.ultimo(c, projetoId));
    }

    // ===================== Novo projeto =====================

    /**
     * Cria o projeto do assistente (SPEC 8.2): cliente e veículo (novos ou existentes), o projeto em RASCUNHO e
     * a v1 do motor, tudo ou nada.
     *
     * @return o id do projeto
     * @throws EntradaInvalidaException com todas as mensagens, se algo não passar nas regras (nada é gravado)
     */
    public long criarProjeto(NovoProjeto novo) {
        LocalDate hoje = LocalDate.now(relogio);
        List<String> erros = new ArrayList<>();
        if (novo.cliente() == null || novo.veiculo() == null) {
            throw new EntradaInvalidaException("Escolha o cliente e o veículo do projeto.");
        }
        if (novo.cliente() instanceof ClienteNovo(Cliente dados)) {
            erros.addAll(dados.validar(hoje));
        }
        Veiculo veiculoNovo = novo.veiculo() instanceof VeiculoNovo(Veiculo dados) ? dados.ajustadoAoTipo() : null;
        if (veiculoNovo != null) {
            erros.addAll(veiculoNovo.validar(hoje.getYear()));
        }
        if (novo.cliente() instanceof ClienteNovo && novo.veiculo() instanceof VeiculoExistente) {
            erros.add("Cliente novo precisa de veículo novo.");
        }
        erros.addAll(errosDoMotor(novo.motor()));
        if (novo.motor() != null) {
            erros.addAll(CalculoDimensionamento.validarMeta(novo.meta(), novo.motor()));
        }
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
        return banco.transacao(c -> {
            long clienteId = switch (novo.cliente()) {
                case ClienteNovo(Cliente dados) -> clientes.inserir(c, dados);
                case ClienteExistente(long id) -> {
                    ClienteSalvo existente = clientes.buscar(c, id).orElseThrow(
                            () -> new OperacaoRecusadaException("Cliente #" + id + " não encontrado."));
                    if (existente.cliente().anonimizado()) {
                        throw new OperacaoRecusadaException("Cliente anonimizado não recebe projeto novo.");
                    }
                    yield id;
                }
            };
            long veiculoId = switch (novo.veiculo()) {
                case VeiculoNovo _ -> veiculos.inserir(c, clienteId, veiculoNovo, null);
                case VeiculoExistente(long id) -> {
                    VeiculoSalvo existente = veiculos.buscar(c, id).orElseThrow(
                            () -> new OperacaoRecusadaException("Veículo #" + id + " não encontrado."));
                    if (existente.clienteId() != clienteId) {
                        throw new OperacaoRecusadaException("O veículo escolhido é de outro cliente.");
                    }
                    yield id;
                }
            };
            long projetoId = projetos.inserir(c, veiculoId, novo.meta(), "", null);
            motores.salvarVersao(c, projetoId, novo.descricaoMotor(), novo.motor());
            return projetoId;
        });
    }

    // ===================== Cliente e veículo =====================

    /** Grava o cliente editado; cada campo mudado vai para o histórico. */
    public void salvarCliente(long clienteId, Cliente dados) {
        List<String> erros = dados.validar(LocalDate.now(relogio));
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
        banco.transacao(c -> {
            clientes.atualizar(c, clienteId, dados);
            return null;
        });
    }

    /** LGPD: apaga os dados pessoais do cliente e dos veículos dele; projetos e mapas ficam (SPEC 6.1). */
    public void anonimizarCliente(long clienteId) {
        banco.transacao(c -> {
            clientes.anonimizar(c, clienteId);
            return null;
        });
    }

    /** Grava o veículo editado, só com os campos do tipo dele; cada campo mudado vai para o histórico. */
    public void salvarVeiculo(long veiculoId, Veiculo dados) {
        Veiculo ajustado = dados.ajustadoAoTipo();
        List<String> erros = ajustado.validar(LocalDate.now(relogio).getYear());
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
        banco.transacao(c -> {
            veiculos.atualizar(c, veiculoId, ajustado, veiculos.buscar(c, veiculoId).orElseThrow().cambioItemId());
            return null;
        });
    }

    // ===================== Motor =====================

    /**
     * Grava o motor como versão nova (SPEC 6.3), se algo mudou em relação à versão atual. Os cálculos feitos com
     * a versão anterior passam a aparecer como desatualizados.
     *
     * @return a versão nova, ou vazio se nada mudou
     * @throws EntradaInvalidaException se o motor não passar nas regras da SPEC 6.3
     */
    public Optional<VersaoMotor> salvarMotor(long projetoId, String descricao, ConfiguracaoMotor motor) {
        List<String> erros = errosDoMotor(motor);
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
        String nome = descricao == null ? "" : descricao.strip();
        return banco.transacao(c -> {
            VersaoMotor atual = motores.atual(c, projetoId).orElseThrow();
            if (atual.descricao().equals(nome) && ComparacaoMotor.comparar(atual.configuracao(), motor).isEmpty()) {
                return Optional.empty();
            }
            VersaoMotor versao = motores.salvarVersao(c, projetoId, nome, motor);
            projetos.tocar(c, projetoId);
            return Optional.of(versao);
        });
    }

    // ===================== Cálculo =====================

    /**
     * Calcula e salva o dimensionamento do projeto com o motor atual (regra 2.5). Todo vermelho precisa de
     * justificativa escrita (regra 2.4). A meta do cálculo vira a meta do projeto, e um projeto em RASCUNHO passa
     * para DIMENSIONADO.
     *
     * @return o id do cálculo salvo
     * @throws EntradaInvalidaException se a entrada não passar, se o motor mudou ou se faltar justificativa
     */
    public long salvarCalculo(long projetoId, EntradaDimensionamento entrada, List<Justificativa> justificativas) {
        VersaoMotor atual = banco.ler(c -> motores.atual(c, projetoId).orElseThrow());
        if (!atual.configuracao().equals(entrada.motor())) {
            throw new EntradaInvalidaException("O motor do projeto mudou depois deste cálculo (agora está na v"
                    + atual.numero() + "). Calcule de novo antes de salvar.");
        }
        Resultado<Dimensionamento> resultado = CalculoDimensionamento.calcular(entrada);
        List<Justificativa> necessarias = justificativasDosVermelhos(resultado, justificativas);
        return banco.transacao(c -> {
            long id = calculos.salvar(c, projetoId, atual.id(), entrada, resultado, necessarias, List.of(), versaoApp);
            projetos.atualizarMeta(c, projetoId, entrada.meta());
            if (projetos.buscar(c, projetoId).orElseThrow().status() == StatusProjeto.RASCUNHO) {
                projetos.mudarStatus(c, projetoId, StatusProjeto.DIMENSIONADO);
            }
            return id;
        });
    }

    // ===================== Status e observações =====================

    public void arquivar(long projetoId) {
        banco.transacao(c -> {
            projetos.arquivar(c, projetoId);
            return null;
        });
    }

    public void reativar(long projetoId) {
        banco.transacao(c -> {
            projetos.reativar(c, projetoId);
            return null;
        });
    }

    public void salvarObservacoes(long projetoId, String observacoes) {
        banco.transacao(c -> {
            projetos.atualizarObservacoes(c, projetoId, observacoes);
            return null;
        });
    }

    // ===================== Regras =====================

    private static List<String> errosDoMotor(ConfiguracaoMotor motor) {
        if (motor == null) {
            return List.of("Preencha o motor.");
        }
        return ValidadorMotor.validar(motor).erros();
    }

    /**
     * As justificativas dos indicadores vermelhos, uma para cada; as de indicadores que não ficaram vermelhos são
     * deixadas de lado.
     *
     * @throws EntradaInvalidaException se algum vermelho ficou sem justificativa
     */
    static List<Justificativa> justificativasDosVermelhos(Resultado<Dimensionamento> resultado,
            List<Justificativa> justificativas) {
        Map<Indicador, Justificativa> porIndicador = new EnumMap<>(Indicador.class);
        for (Justificativa justificativa : justificativas) {
            porIndicador.put(justificativa.indicador(), justificativa);
        }
        List<Justificativa> necessarias = new ArrayList<>();
        List<String> faltando = new ArrayList<>();
        for (Avaliacao avaliacao : resultado.valor().semaforo()) {
            if (avaliacao.cor() != Cor.VERMELHO) {
                continue;
            }
            Justificativa justificativa = porIndicador.get(avaliacao.indicador());
            if (justificativa == null) {
                faltando.add(avaliacao.indicador().rotulo());
            } else {
                necessarias.add(justificativa);
            }
        }
        if (!faltando.isEmpty()) {
            throw new EntradaInvalidaException("Para salvar, escreva a justificativa de: "
                    + String.join(", ", faltando) + " (regra 2.4: vermelho só avança com justificativa).");
        }
        return necessarias;
    }

    /** Por que um cálculo salvo não está mais em dia com o projeto (vazio = em dia). */
    static List<String> motivos(ResumoCalculo calculo, VersaoMotor atual) {
        List<String> motivos = new ArrayList<>();
        if (calculo.versaoMotorId() != atual.id()) {
            motivos.add("o motor mudou: este cálculo usou a v" + calculo.numeroVersaoMotor() + " e o motor está na v"
                    + atual.numero());
        }
        if (!VersaoNucleo.ATUAL.equals(calculo.versaoNucleo())) {
            motivos.add("o núcleo de cálculo mudou: " + calculo.versaoNucleo() + " → " + VersaoNucleo.ATUAL);
        }
        return motivos;
    }

    /** Agora, para quem precisa datar uma justificativa. */
    public LocalDateTime agora() {
        return LocalDateTime.now(relogio).withNano(0);
    }
}
