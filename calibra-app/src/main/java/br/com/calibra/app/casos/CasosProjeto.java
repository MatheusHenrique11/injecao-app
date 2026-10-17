package br.com.calibra.app.casos;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import br.com.calibra.app.catalogo.PecasEscolhidas;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.cadastro.Cliente;
import br.com.calibra.core.cadastro.TipoCambio;
import br.com.calibra.core.cadastro.Veiculo;
import br.com.calibra.core.calculo.CalculoDimensionamento;
import br.com.calibra.core.calculo.Dimensionamento;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.combustivel.Combustivel;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.memoria.VersaoNucleo;
import br.com.calibra.core.motor.ComparacaoMotor;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.motor.ValidadorMotor;
import br.com.calibra.core.pecas.Cambio;
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
import br.com.calibra.dados.catalogo.CatalogoRepositorio;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.ResumoItem;
import br.com.calibra.dados.catalogo.TipoCatalogo;
import br.com.calibra.dados.projeto.CalculoRepositorio;
import br.com.calibra.dados.projeto.CalculoSalvo;
import br.com.calibra.dados.projeto.FiltroProjetos;
import br.com.calibra.dados.projeto.MotorRepositorio;
import br.com.calibra.dados.projeto.MudancaStatus;
import br.com.calibra.dados.projeto.PapelPeca;
import br.com.calibra.dados.projeto.PecaUsada;
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

    /** @param cambioItemId câmbio do catálogo; null se não escolhido */
    public record VeiculoNovo(Veiculo dados, Long cambioItemId) implements EscolhaVeiculo {

        public VeiculoNovo(Veiculo dados) {
            this(dados, null);
        }
    }

    /**
     * O que o assistente de novo projeto junta (SPEC 8.2): cliente, veículo, motor, meta e, se escolhida, a
     * injeção do catálogo.
     */
    public record NovoProjeto(EscolhaCliente cliente, EscolhaVeiculo veiculo, String descricaoMotor,
            ConfiguracaoMotor motor, Meta meta, Long injecaoItemId) {

        public NovoProjeto(EscolhaCliente cliente, EscolhaVeiculo veiculo, String descricaoMotor,
                ConfiguracaoMotor motor, Meta meta) {
            this(cliente, veiculo, descricaoMotor, motor, meta, null);
        }
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
    private final CatalogoRepositorio catalogo;

    public CasosProjeto(BancoDados banco, Clock relogio, String versaoApp) {
        this.banco = Objects.requireNonNull(banco, "banco");
        this.relogio = Objects.requireNonNull(relogio, "relogio");
        this.versaoApp = Objects.requireNonNull(versaoApp, "versaoApp");
        this.clientes = new ClienteRepositorio(relogio);
        this.veiculos = new VeiculoRepositorio(relogio);
        this.projetos = new ProjetoRepositorio(relogio);
        this.motores = new MotorRepositorio(relogio);
        this.calculos = new CalculoRepositorio(relogio);
        this.catalogo = new CatalogoRepositorio(relogio);
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

    /**
     * Cálculos salvos do projeto, do mais novo para o mais antigo, com o que mudou desde cada um: o motor, o
     * núcleo, uma peça do catálogo que ganhou versão nova ou o câmbio do veículo.
     */
    public List<SituacaoCalculo> calculos(long projetoId) {
        return banco.ler(c -> {
            VersaoMotor atual = motores.atual(c, projetoId).orElseThrow();
            ProjetoSalvo projeto = projetos.buscar(c, projetoId).orElseThrow();
            Long cambioDoVeiculo = veiculos.buscar(c, projeto.veiculoId()).orElseThrow().cambioItemId();
            Map<Long, List<PecaUsada>> pecas = calculos.pecasDoProjeto(c, projetoId);
            Set<Long> ids = pecas.values().stream().flatMap(List::stream).map(PecaUsada::itemId)
                    .collect(Collectors.toCollection(HashSet::new));
            if (cambioDoVeiculo != null) {
                ids.add(cambioDoVeiculo);
            }
            Map<Long, ResumoItem> itens = catalogo.resumos(c, ids);
            Optional<PecaUsada> combustivelDoMotor = combustivelUsado(c, atual.configuracao());
            List<SituacaoCalculo> situacoes = new ArrayList<>();
            for (ResumoCalculo calculo : calculos.doProjeto(c, projetoId)) {
                List<PecaUsada> doCalculo = pecas.getOrDefault(calculo.id(), List.of());
                List<String> motivos = new ArrayList<>(motivos(calculo, atual));
                motivos.addAll(motivosDasPecas(doCalculo, itens, combustivelDoMotor));
                motivoDoCambio(doCalculo, cambioDoVeiculo, itens).ifPresent(motivos::add);
                situacoes.add(new SituacaoCalculo(calculo, motivos));
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
        List<String> erros = errosDoProjeto(novo);
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
        return banco.transacao(c -> inserirProjeto(c, novo));
    }

    /** As regras do projeto novo: o cliente, o veículo, o motor e a meta. Vazio = pode gravar. */
    List<String> errosDoProjeto(NovoProjeto novo) {
        if (novo.cliente() == null || novo.veiculo() == null) {
            return List.of("Escolha o cliente e o veículo do projeto.");
        }
        List<String> erros = new ArrayList<>(errosDaEscolha(novo.cliente(), novo.veiculo()));
        erros.addAll(errosDoMotor(novo.motor()));
        if (novo.motor() != null) {
            erros.addAll(CalculoDimensionamento.validarMeta(novo.meta(), novo.motor()));
        }
        return erros;
    }

    /** Grava o projeto novo com a primeira versão do motor, na transação de quem chama (a OS liga o projeto junto). */
    long inserirProjeto(Connection conexao, NovoProjeto novo) throws SQLException {
        ClienteEVeiculo escolhidos = gravarEscolha(conexao, novo.cliente(), novo.veiculo(), "projeto novo");
        conferirInjecao(conexao, novo.injecaoItemId());
        long projetoId = projetos.inserir(conexao, escolhidos.veiculoId(), novo.meta(), "", novo.injecaoItemId());
        motores.salvarVersao(conexao, projetoId, novo.descricaoMotor(), novo.motor());
        return projetoId;
    }

    /** O cliente e o veículo gravados (novos) ou conferidos (existentes). */
    record ClienteEVeiculo(long clienteId, long veiculoId) {
    }

    /**
     * As regras do cliente e do veículo escolhidos no assistente ou na Nova OS: os novos passam pela validação do
     * cadastro, e cliente novo pede veículo novo. Vazio = pode gravar.
     */
    List<String> errosDaEscolha(EscolhaCliente cliente, EscolhaVeiculo veiculo) {
        LocalDate hoje = LocalDate.now(relogio);
        List<String> erros = new ArrayList<>();
        if (cliente instanceof ClienteNovo(Cliente dados)) {
            erros.addAll(dados.validar(hoje));
        }
        if (veiculo instanceof VeiculoNovo novo) {
            erros.addAll(novo.dados().ajustadoAoTipo().validar(hoje.getYear()));
        }
        if (cliente instanceof ClienteNovo && veiculo instanceof VeiculoExistente) {
            erros.add("Cliente novo precisa de veículo novo.");
        }
        return erros;
    }

    /**
     * Grava o cliente e o veículo novos ou confere os existentes (o cliente não pode estar anonimizado, e o veículo
     * tem de ser dele). Roda dentro da transação de quem chama.
     *
     * @param paraQue o que vai ser criado, para a mensagem (ex.: "projeto novo", "OS nova")
     */
    ClienteEVeiculo gravarEscolha(Connection c, EscolhaCliente cliente, EscolhaVeiculo veiculo, String paraQue)
            throws SQLException {
        long clienteId = switch (cliente) {
            case ClienteNovo(Cliente dados) -> clientes.inserir(c, dados);
            case ClienteExistente(long id) -> {
                ClienteSalvo existente = clientes.buscar(c, id).orElseThrow(
                        () -> new OperacaoRecusadaException("Cliente #" + id + " não encontrado."));
                if (existente.cliente().anonimizado()) {
                    throw new OperacaoRecusadaException("Cliente anonimizado não recebe " + paraQue + ".");
                }
                yield id;
            }
        };
        long veiculoId = switch (veiculo) {
            case VeiculoNovo novo -> {
                Veiculo ajustado = novo.dados().ajustadoAoTipo();
                conferirCambio(c, novo.cambioItemId(), ajustado);
                yield veiculos.inserir(c, clienteId, ajustado, novo.cambioItemId());
            }
            case VeiculoExistente(long id) -> {
                VeiculoSalvo existente = veiculos.buscar(c, id).orElseThrow(
                        () -> new OperacaoRecusadaException("Veículo #" + id + " não encontrado."));
                if (existente.clienteId() != clienteId) {
                    throw new OperacaoRecusadaException("O veículo escolhido é de outro cliente.");
                }
                yield id;
            }
        };
        return new ClienteEVeiculo(clienteId, veiculoId);
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

    /** Grava o veículo editado, mantendo o câmbio do catálogo que ele já tinha. */
    public void salvarVeiculo(long veiculoId, Veiculo dados) {
        salvarVeiculo(veiculoId, dados, true, null);
    }

    /**
     * Grava o veículo editado, só com os campos do tipo dele; cada campo mudado vai para o histórico.
     *
     * @param cambioItemId câmbio do catálogo (o tipo de câmbio do veículo precisa ser o dele); null = nenhum
     */
    public void salvarVeiculo(long veiculoId, Veiculo dados, Long cambioItemId) {
        salvarVeiculo(veiculoId, dados, false, cambioItemId);
    }

    private void salvarVeiculo(long veiculoId, Veiculo dados, boolean manterCambio, Long cambioItemId) {
        Veiculo ajustado = dados.ajustadoAoTipo();
        List<String> erros = ajustado.validar(LocalDate.now(relogio).getYear());
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
        banco.transacao(c -> {
            Long cambio = manterCambio ? veiculos.buscar(c, veiculoId).orElseThrow().cambioItemId() : cambioItemId;
            conferirCambio(c, cambio, ajustado);
            veiculos.atualizar(c, veiculoId, ajustado, cambio);
            return null;
        });
    }

    /** Troca a injeção do catálogo escolhida para o projeto (null = nenhuma). */
    public void trocarInjecao(long projetoId, Long injecaoItemId) {
        banco.transacao(c -> {
            conferirInjecao(c, injecaoItemId);
            projetos.atualizarInjecao(c, projetoId, injecaoItemId);
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
        return salvarCalculo(projetoId, entrada, justificativas, PecasEscolhidas.NENHUMA);
    }

    /**
     * Como {@link #salvarCalculo(long, EntradaDimensionamento, List)}, gravando também as peças do catálogo usadas
     * (com a versão de cada uma) e o combustível do motor. Se uma delas ganhar versão nova, o cálculo aparece como
     * desatualizado.
     *
     * @param pecas as peças do catálogo escolhidas na tela; precisam ser as que estão na entrada
     */
    public long salvarCalculo(long projetoId, EntradaDimensionamento entrada, List<Justificativa> justificativas,
            PecasEscolhidas pecas) {
        VersaoMotor atual = banco.ler(c -> motores.atual(c, projetoId).orElseThrow());
        if (!atual.configuracao().equals(entrada.motor())) {
            throw new EntradaInvalidaException("O motor do projeto mudou depois deste cálculo (agora está na v"
                    + atual.numero() + "). Calcule de novo antes de salvar.");
        }
        conferirPecas(entrada, pecas);
        Resultado<Dimensionamento> resultado = CalculoDimensionamento.calcular(entrada);
        List<Justificativa> necessarias = justificativasDosVermelhos(resultado, justificativas);
        return banco.transacao(c -> {
            List<PecaUsada> usadas = new ArrayList<>(pecas.usadas());
            combustivelUsado(c, entrada.motor()).ifPresent(usadas::add);
            long id = calculos.salvar(c, projetoId, atual.id(), entrada, resultado, necessarias, usadas, versaoApp);
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

    /**
     * As peças do catálogo que ganharam versão nova depois do cálculo. O combustível vem com o motor: para usar a
     * versão nova, o motor precisa ser gravado de novo (a menos que a versão atual do motor já a use).
     *
     * @param combustivelDoMotor o combustível da versão atual do motor, como peça do catálogo
     */
    static List<String> motivosDasPecas(List<PecaUsada> pecas, Map<Long, ResumoItem> itens,
            Optional<PecaUsada> combustivelDoMotor) {
        List<String> motivos = new ArrayList<>();
        for (PecaUsada peca : pecas) {
            ResumoItem item = itens.get(peca.itemId());
            if (item == null || item.versaoAtual() == peca.versao()) {
                continue;
            }
            String motivo = peca.papel().comArtigo() + " mudou no catálogo: " + item.nome() + " v" + peca.versao()
                    + " → v" + item.versaoAtual();
            boolean motorJaUsa = combustivelDoMotor
                    .filter(c -> c.itemId() == item.id() && c.versao() == item.versaoAtual()).isPresent();
            if (peca.papel() == PapelPeca.COMBUSTIVEL && !motorJaUsa) {
                motivo += " (grave o motor de novo, na aba Motor, para usar a versão nova)";
            }
            motivos.add(motivo);
        }
        return motivos;
    }

    /**
     * No projeto vale o câmbio do veículo (decisão 130). Se ele foi trocado, tirado ou posto depois do cálculo, o
     * torque × câmbio do cálculo não vale mais. Versão nova do mesmo câmbio já entra em {@link #motivosDasPecas}.
     */
    static Optional<String> motivoDoCambio(List<PecaUsada> pecas, Long cambioDoVeiculo, Map<Long, ResumoItem> itens) {
        Long doCalculo = pecas.stream().filter(peca -> peca.papel() == PapelPeca.CAMBIO).map(PecaUsada::itemId)
                .findFirst().orElse(null);
        if (Objects.equals(doCalculo, cambioDoVeiculo)) {
            return Optional.empty();
        }
        return Optional.of("o câmbio do veículo mudou: " + nomeDoItem(doCalculo, itens) + " → "
                + nomeDoItem(cambioDoVeiculo, itens));
    }

    private static String nomeDoItem(Long itemId, Map<Long, ResumoItem> itens) {
        if (itemId == null) {
            return "nenhum";
        }
        ResumoItem item = itens.get(itemId);
        return item == null ? "item #" + itemId : item.nome();
    }

    /** O combustível do motor como peça do catálogo (item pelo código, na versão que o motor guarda). */
    private Optional<PecaUsada> combustivelUsado(Connection conexao, ConfiguracaoMotor motor) throws SQLException {
        Combustivel combustivel = motor.combustivel();
        if (combustivel == null) {
            return Optional.empty();
        }
        return catalogo.listar(conexao, TipoCatalogo.COMBUSTIVEL, "", true).stream()
                .filter(item -> item.dados().codigo().equals(combustivel.codigo()))
                .findFirst()
                .map(item -> new PecaUsada(PapelPeca.COMBUSTIVEL, item.id(), combustivel.versao()));
    }

    /** As peças escolhidas precisam ser as que foram calculadas (o cálculo grava a versão de cada uma). */
    static void conferirPecas(EntradaDimensionamento entrada, PecasEscolhidas pecas) {
        List<String> erros = new ArrayList<>();
        conferir(erros, PapelPeca.BICO, pecas.bico(), entrada.bico());
        conferir(erros, PapelPeca.BICO_SECUNDARIO, pecas.bicoSecundario(), entrada.bicoSecundario());
        conferir(erros, PapelPeca.BOMBA, pecas.bomba(), entrada.bomba());
        conferir(erros, PapelPeca.INJECAO, pecas.injecao(), entrada.injecao());
        conferir(erros, PapelPeca.TURBO, pecas.turbo(), entrada.turbo());
        conferir(erros, PapelPeca.CAMBIO, pecas.cambio(), entrada.cambio());
        if (pecas.sensorMap() != null
                && !Objects.equals(pecas.sensorMap().dados().faixaMaxima(), entrada.faixaSensorMapKpa())) {
            erros.add(diferente(PapelPeca.SENSOR_MAP));
        }
        if (pecas.intercooler() != null && pecas.intercooler().dados().eficiencia() != null
                && pecas.intercooler().dados().eficiencia() != entrada.parametros().eps()) {
            erros.add(diferente(PapelPeca.INTERCOOLER));
        }
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
    }

    private static void conferir(List<String> erros, PapelPeca papel, ItemCatalogo<?> item, Object naEntrada) {
        if (item != null && !item.dados().equals(naEntrada)) {
            erros.add(diferente(papel));
        }
    }

    private static String diferente(PapelPeca papel) {
        return "A peça do catálogo (" + papel.rotulo().toLowerCase(Locale.ROOT) + ") não é a da entrada do cálculo: "
                + "calcule de novo antes de salvar.";
    }

    /** O câmbio do catálogo precisa existir e ser do mesmo tipo que o câmbio do veículo. */
    private void conferirCambio(Connection conexao, Long cambioItemId, Veiculo veiculo) throws SQLException {
        if (cambioItemId == null) {
            return;
        }
        ItemCatalogo<Cambio> item = catalogo.buscar(conexao, TipoCatalogo.CAMBIO, cambioItemId).orElseThrow(
                () -> new OperacaoRecusadaException("Câmbio do catálogo #" + cambioItemId + " não encontrado."));
        TipoCambio tipo = item.dados().tipo();
        if (tipo != veiculo.cambio()) {
            throw new EntradaInvalidaException("O câmbio " + item.info().nome() + " é "
                    + (tipo == null ? "sem tipo" : tipo.rotulo()) + ", mas o veículo está com câmbio "
                    + (veiculo.cambio() == null ? "não informado" : veiculo.cambio().rotulo()) + ".");
        }
    }

    private void conferirInjecao(Connection conexao, Long injecaoItemId) throws SQLException {
        if (injecaoItemId != null && catalogo.buscar(conexao, TipoCatalogo.INJECAO, injecaoItemId).isEmpty()) {
            throw new OperacaoRecusadaException("Injeção do catálogo #" + injecaoItemId + " não encontrada.");
        }
    }

    /** Agora, para quem precisa datar uma justificativa. */
    public LocalDateTime agora() {
        return LocalDateTime.now(relogio).withNano(0);
    }
}
