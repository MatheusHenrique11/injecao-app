package br.com.calibra.app.casos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.com.calibra.app.casos.CasosProjeto.ClienteExistente;
import br.com.calibra.app.casos.CasosProjeto.EscolhaCliente;
import br.com.calibra.app.casos.CasosProjeto.EscolhaVeiculo;
import br.com.calibra.app.casos.CasosProjeto.VeiculoExistente;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.oficina.EntradaVeiculo;
import br.com.calibra.core.oficina.Entrega;
import br.com.calibra.core.oficina.NumeroOs;
import br.com.calibra.core.oficina.Pagamento;
import br.com.calibra.core.oficina.SituacaoPagamento;
import br.com.calibra.core.oficina.StatusOs;
import br.com.calibra.core.oficina.TipoAnexo;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.projeto.Meta;
import br.com.calibra.core.projeto.StatusProjeto;
import br.com.calibra.core.util.NumeroBr;
import br.com.calibra.dados.BancoDados;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.cadastro.ClienteRepositorio;
import br.com.calibra.dados.cadastro.ClienteSalvo;
import br.com.calibra.dados.cadastro.VeiculoRepositorio;
import br.com.calibra.dados.cadastro.VeiculoSalvo;
import br.com.calibra.dados.oficina.AnexoRepositorio;
import br.com.calibra.dados.oficina.AnexoSalvo;
import br.com.calibra.dados.oficina.DadosAbertura;
import br.com.calibra.dados.oficina.FiltroOs;
import br.com.calibra.dados.oficina.MecanicoRepositorio;
import br.com.calibra.dados.oficina.MecanicoSalvo;
import br.com.calibra.dados.oficina.MudancaStatusOs;
import br.com.calibra.dados.oficina.OrdemServicoRepositorio;
import br.com.calibra.dados.oficina.OrdemServicoSalva;
import br.com.calibra.dados.oficina.PagamentoRepositorio;
import br.com.calibra.dados.oficina.PagamentoSalvo;
import br.com.calibra.dados.oficina.ResumoOs;
import br.com.calibra.dados.orcamento.OrcamentoRepositorio;
import br.com.calibra.dados.orcamento.OrcamentoSalvo;
import br.com.calibra.dados.projeto.MotorRepositorio;
import br.com.calibra.dados.projeto.ProjetoRepositorio;
import br.com.calibra.dados.projeto.ProjetoSalvo;

/**
 * Casos de uso da oficina (fase 3, SPEC 6.5): abrir a OS (com ou sem a entrada do veículo), registrar a entrada, a
 * execução, a entrega com garantia, o cancelamento e os recebimentos. Até APROVADA, quem move a OS é o orçamento
 * ({@link CasosOrcamento}); só se executa com orçamento aprovado e com a entrada registrada. Sem JavaFX.
 */
public final class CasosOficina {

    /**
     * O que se informa ao abrir uma OS.
     *
     * @param mecanicoId null = nenhum
     * @param previsaoEntrega null = sem previsão
     * @param entrada null = o veículo chega depois (orçamento antes do veículo)
     */
    public record NovaOs(EscolhaCliente cliente, EscolhaVeiculo veiculo, String servicoPedido, Long mecanicoId,
            LocalDate previsaoEntrega, String observacoes, EntradaVeiculo entrada) {
    }

    /** O que foi gravado e os avisos que não impediram (ex.: km menor que o do cadastro do veículo). */
    public record Gravado(long osId, List<String> avisos) {

        public Gravado {
            avisos = List.copyOf(avisos);
        }
    }

    /**
     * A situação do pagamento da OS.
     *
     * @param valor o total da última revisão aprovada do orçamento; null sem orçamento aprovado
     * @param falta null sem orçamento aprovado
     * @param situacao null sem orçamento aprovado
     */
    public record SituacaoDoPagamento(Dinheiro valor, Dinheiro recebido, Dinheiro falta, SituacaoPagamento situacao,
            List<PagamentoSalvo> recebimentos) {

        public SituacaoDoPagamento {
            recebimentos = List.copyOf(recebimentos);
        }
    }

    /** A OS com o que a tela mostra junto: o cliente, o veículo, o mecânico (null = nenhum) e o pagamento. */
    public record OsAberta(OrdemServicoSalva os, ClienteSalvo cliente, VeiculoSalvo veiculo, MecanicoSalvo mecanico,
            SituacaoDoPagamento pagamento) {
    }

    /**
     * Um arquivo para anexar.
     *
     * @param validade null = sem validade
     * @param pessoal documento com dados pessoais (sai na anonimização do cliente)
     */
    public record NovoAnexo(Path arquivo, TipoAnexo tipo, String descricao, LocalDate validade, boolean pessoal) {
    }

    /** Um projeto de preparação do veículo, para ligar à OS: o id, o motor ("AP 2.0 turbo v2") e o status. */
    public record ProjetoDoVeiculo(long projetoId, String motor, StatusProjeto status) {
    }

    /**
     * O que o Painel mostra (SPEC 8.8).
     *
     * @param porStatus quantas OS há em cada status de quem está em andamento (zero quando não há nenhuma)
     * @param naOficina as OS em andamento com o veículo na oficina, de quem chegou primeiro para quem chegou por último
     * @param entregas as OS em andamento com a entrega prevista para hoje ou para antes, da previsão mais antiga
     */
    public record Painel(Map<StatusOs, Integer> porStatus, List<ResumoOs> naOficina, List<ResumoOs> entregas) {

        public Painel {
            porStatus = Map.copyOf(porStatus);
            naOficina = List.copyOf(naOficina);
            entregas = List.copyOf(entregas);
        }
    }

    private static final Logger LOG = Logger.getLogger(CasosOficina.class.getName());

    /** O máximo de linhas da lista da Oficina: uma oficina só não tem tanta OS em andamento. */
    private static final int LIMITE_DA_LISTA = 500;

    private final BancoDados banco;
    private final Clock relogio;
    private final CasosProjeto projetos;
    private final Path pastaAnexos;
    private final OrdemServicoRepositorio ordens;
    private final PagamentoRepositorio pagamentos;
    private final MecanicoRepositorio mecanicos;
    private final OrcamentoRepositorio orcamentos;
    private final AnexoRepositorio anexos;
    private final ClienteRepositorio clientes;
    private final VeiculoRepositorio veiculos;
    private final ProjetoRepositorio projetoRepositorio;
    private final MotorRepositorio motores;
    private final OrdensDoProjeto ordensDoProjeto;

    /** @param pastaAnexos a pasta de anexos dos dados, para onde vão as cópias das fotos e dos documentos */
    public CasosOficina(BancoDados banco, Clock relogio, CasosProjeto projetos, Path pastaAnexos) {
        this.banco = Objects.requireNonNull(banco, "banco");
        this.relogio = Objects.requireNonNull(relogio, "relogio");
        this.projetos = Objects.requireNonNull(projetos, "projetos");
        this.pastaAnexos = Objects.requireNonNull(pastaAnexos, "pastaAnexos");
        this.ordens = new OrdemServicoRepositorio(relogio);
        this.pagamentos = new PagamentoRepositorio(relogio);
        this.mecanicos = new MecanicoRepositorio(relogio);
        this.orcamentos = new OrcamentoRepositorio(relogio);
        this.anexos = new AnexoRepositorio(relogio);
        this.clientes = new ClienteRepositorio(relogio);
        this.veiculos = new VeiculoRepositorio(relogio);
        this.projetoRepositorio = new ProjetoRepositorio(relogio);
        this.motores = new MotorRepositorio(relogio);
        this.ordensDoProjeto = new OrdensDoProjeto(relogio);
    }

    // ===================== Abertura e entrada =====================

    /**
     * Abre a OS, ABERTA, com o cliente e o veículo (novos ou existentes) e, se o veículo já chegou, a entrada.
     *
     * @throws EntradaInvalidaException com todas as mensagens, se algo não passar (nada é gravado)
     */
    public Gravado abrir(NovaOs nova) {
        return abrir(nova, List.of());
    }

    /**
     * Abre a OS junto com os anexos da abertura (as fotos da entrada): cada arquivo é copiado para a pasta de anexos
     * do veículo e tudo é gravado de uma vez. Se um arquivo não servir, nada é gravado, nem a OS, e as cópias feitas
     * são apagadas: dá para tirar o arquivo e tentar de novo sem criar OS repetida.
     *
     * @throws EntradaInvalidaException com todas as mensagens, se algo não passar
     * @throws OperacaoRecusadaException se um arquivo não for JPG, PNG ou PDF, ou passar de 20 MB
     */
    public Gravado abrir(NovaOs nova, List<NovoAnexo> anexosDaAbertura) {
        List<NovoAnexo> novos = anexosDaAbertura == null ? List.of() : List.copyOf(anexosDaAbertura);
        if (nova.cliente() == null || nova.veiculo() == null) {
            throw new EntradaInvalidaException("Escolha o cliente e o veículo da OS.");
        }
        List<String> erros = new ArrayList<>(projetos.errosDaEscolha(nova.cliente(), nova.veiculo()));
        if (nova.servicoPedido() == null || nova.servicoPedido().isBlank()) {
            erros.add("Descreva o serviço pedido pelo cliente.");
        }
        if (nova.entrada() != null) {
            erros.addAll(nova.entrada().validar(agora()));
        }
        erros.addAll(errosDosAnexos(novos));
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
        List<AnexosDaOficina.Copia> copias = new ArrayList<>();
        try {
            return banco.transacao(c -> {
                CasosProjeto.ClienteEVeiculo escolhidos = projetos.gravarEscolha(c, nova.cliente(), nova.veiculo(),
                        "OS nova");
                exigirMecanicoAtivo(c, nova.mecanicoId());
                int ano = LocalDate.now(relogio).getYear();
                long id = ordens.inserir(c, new NumeroOs(ano, ordens.proximoSequencial(c, ano)), new DadosAbertura(
                        escolhidos.clienteId(), escolhidos.veiculoId(), null, nova.servicoPedido(),
                        nova.mecanicoId(), nova.previsaoEntrega(), nova.observacoes()));
                List<String> avisos = nova.entrada() == null ? List.of()
                        : gravarEntrada(c, id, escolhidos.veiculoId(), nova.entrada());
                for (NovoAnexo novo : novos) {
                    AnexosDaOficina.Copia copia = copiar(novo.arquivo(), escolhidos.veiculoId());
                    copias.add(copia);
                    inserirAnexo(c, escolhidos.veiculoId(), id, novo, copia);
                }
                return new Gravado(id, avisos);
            });
        } catch (RuntimeException erro) {
            apagarSobras(copias);
            throw erro;
        }
    }

    /** Muda o que foi informado na abertura (o serviço pedido, o mecânico, a previsão e as observações). */
    public void editarAbertura(long osId, String servicoPedido, Long mecanicoId, LocalDate previsaoEntrega,
            String observacoes) {
        if (servicoPedido == null || servicoPedido.isBlank()) {
            throw new EntradaInvalidaException("Descreva o serviço pedido pelo cliente.");
        }
        banco.transacao(c -> {
            OrdemServicoSalva os = exigir(c, osId);
            if (!Objects.equals(mecanicoId, os.mecanicoId())) {
                exigirMecanicoAtivo(c, mecanicoId);
            }
            ordens.atualizarAbertura(c, osId, servicoPedido, mecanicoId, previsaoEntrega, observacoes);
            return null;
        });
    }

    /**
     * Registra (ou corrige) a chegada do veículo. O km atualiza o uso acumulado do veículo, com histórico; menor que o
     * do cadastro, vira aviso e é gravado assim mesmo (troca de painel acontece).
     */
    public Gravado registrarEntrada(long osId, EntradaVeiculo entrada) {
        List<String> erros = entrada == null ? List.of("Informe a entrada do veículo.") : entrada.validar(agora());
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
        return banco.transacao(c -> {
            OrdemServicoSalva os = exigir(c, osId);
            return new Gravado(osId, gravarEntrada(c, osId, os.veiculoId(), entrada));
        });
    }

    // ===================== Execução, entrega e cancelamento =====================

    /**
     * Um passo da execução: em execução, aguardando peças ou pronta (SPEC 6.5). Os passos até APROVADA vêm do
     * orçamento; a entrega e o cancelamento têm métodos próprios. Começar o serviço exige a entrada do veículo.
     */
    public void mudarStatus(long osId, StatusOs destino, String observacao) {
        if (destino == null) {
            throw new EntradaInvalidaException("Escolha o novo status da OS.");
        }
        switch (destino) {
            case ABERTA, AGUARDANDO_APROVACAO, APROVADA -> throw new OperacaoRecusadaException(
                    "Esse passo vem do orçamento: marque como enviado, ou registre a aprovação ou a recusa.");
            case ENTREGUE -> throw new OperacaoRecusadaException("Use a entrega, com o km de saída e a garantia.");
            case CANCELADA -> throw new OperacaoRecusadaException("Use o cancelamento, com o motivo.");
            default -> {
            }
        }
        banco.transacao(c -> {
            OrdemServicoSalva os = exigir(c, osId);
            if (destino == StatusOs.EM_EXECUCAO && !os.veiculoChegou()) {
                throw new OperacaoRecusadaException("Registre a entrada do veículo (km, tanque e vistoria) antes de "
                        + "começar o serviço.");
            }
            ordens.mudarStatus(c, osId, destino, observacao);
            return null;
        });
    }

    /**
     * Entrega a OS pronta, com o km de saída e a garantia. O km atualiza o uso acumulado do veículo. Depois disso a OS
     * não muda mais (só os recebimentos).
     */
    public Gravado entregar(long osId, Entrega entrega) {
        List<String> erros = entrega == null ? List.of("Informe a entrega.") : entrega.validar(agora());
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
        return banco.transacao(c -> {
            OrdemServicoSalva os = exigir(c, osId);
            if (os.entrada() != null && entrega.quando().isBefore(os.entrada().quando())) {
                throw new EntradaInvalidaException("A entrega não pode ser antes da entrada do veículo.");
            }
            List<String> avisos = new ArrayList<>(entrega.avisos(os.entrada() == null ? null : os.entrada().uso()));
            ordens.entregar(c, osId, entrega);
            List<String> doCadastro = atualizarUso(c, os.veiculoId(), entrega.usoSaida(), "da saída");
            if (avisos.isEmpty()) {
                avisos.addAll(doCadastro);
            }
            return new Gravado(osId, avisos);
        });
    }

    /** Cancela a OS (a qualquer momento antes da entrega), com o motivo. */
    public void cancelar(long osId, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new EntradaInvalidaException("Escreva o motivo do cancelamento.");
        }
        banco.transacao(c -> {
            ordens.cancelar(c, osId, motivo);
            return null;
        });
    }

    // ===================== Pagamento =====================

    /** Registra um recebimento (ex.: o sinal na aprovação, o resto na entrega); vale também depois da entrega. */
    public long registrarPagamento(long osId, Pagamento pagamento) {
        List<String> erros = pagamento == null ? List.of("Informe o recebimento.") : pagamento.validar(agora());
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
        return banco.transacao(c -> {
            OrdemServicoSalva os = exigir(c, osId);
            if (os.status() == StatusOs.CANCELADA) {
                throw new OperacaoRecusadaException("A OS " + os.numero().texto() + " está cancelada e não recebe "
                        + "pagamento.");
            }
            return pagamentos.registrar(c, osId, pagamento);
        });
    }

    /** Cancela um recebimento lançado errado; ele fica no histórico, com o motivo, e deixa de contar. */
    public void cancelarPagamento(long pagamentoId, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new EntradaInvalidaException("Escreva o motivo do cancelamento do recebimento.");
        }
        banco.transacao(c -> {
            pagamentos.cancelar(c, pagamentoId, motivo);
            return null;
        });
    }

    /** O valor da OS (a última revisão aprovada do orçamento), o que foi recebido e o que falta. */
    public SituacaoDoPagamento pagamento(long osId) {
        return banco.ler(c -> situacaoDoPagamento(c, osId));
    }

    // ===================== Consultas =====================

    public List<ResumoOs> listar(FiltroOs filtro) {
        return banco.ler(c -> ordens.pesquisar(c, filtro, LIMITE_DA_LISTA));
    }

    /**
     * O Painel (SPEC 8.8): as OS em andamento por status, os veículos que estão na oficina e as entregas previstas
     * para hoje, com as atrasadas junto (a previsão já passou e a OS não foi entregue).
     */
    public Painel painel() {
        LocalDate hoje = LocalDate.now(relogio);
        return banco.ler(c -> {
            Map<StatusOs, Integer> contagem = ordens.contarPorStatus(c);
            Map<StatusOs, Integer> porStatus = new EnumMap<>(StatusOs.class);
            for (StatusOs status : StatusOs.values()) {
                if (!status.finalizada()) {
                    porStatus.put(status, contagem.getOrDefault(status, 0));
                }
            }
            List<ResumoOs> emAndamento = ordens.pesquisar(c, FiltroOs.emAndamento(), LIMITE_DA_LISTA);
            List<ResumoOs> naOficina = emAndamento.stream().filter(os -> os.entradaEm() != null)
                    .sorted(Comparator.comparing(ResumoOs::entradaEm)).toList();
            List<ResumoOs> entregas = emAndamento.stream()
                    .filter(os -> os.previsaoEntrega() != null && !os.previsaoEntrega().isAfter(hoje))
                    .sorted(Comparator.comparing(ResumoOs::previsaoEntrega)).toList();
            return new Painel(porStatus, naOficina, entregas);
        });
    }

    /** A OS com o cliente, o veículo, o mecânico e o pagamento. */
    public OsAberta buscar(long osId) {
        return banco.ler(c -> {
            OrdemServicoSalva os = exigir(c, osId);
            ClienteSalvo cliente = clientes.buscar(c, os.clienteId()).orElseThrow();
            VeiculoSalvo veiculo = veiculos.buscar(c, os.veiculoId()).orElseThrow();
            MecanicoSalvo mecanico = os.mecanicoId() == null ? null
                    : mecanicos.buscar(c, os.mecanicoId()).orElse(null);
            return new OsAberta(os, cliente, veiculo, mecanico, situacaoDoPagamento(c, osId));
        });
    }

    public List<MudancaStatusOs> historico(long osId) {
        return banco.ler(c -> ordens.historico(c, osId));
    }

    /** A OS atual do projeto de preparação; sem nenhuma em andamento, abre uma para ele (ABERTA, sem a entrada). */
    public long osAtualDoProjeto(long projetoId) {
        return banco.transacao(c -> ordensDoProjeto.atual(c, projetoId));
    }

    /** As OS do projeto, da mais nova para a mais antiga (o cabeçalho do projeto mostra a atual). */
    public List<OrdemServicoSalva> ordensDoProjeto(long projetoId) {
        return banco.ler(c -> ordens.doProjeto(c, projetoId));
    }

    // ===================== Preparação =====================

    /**
     * Liga a OS a um projeto de preparação do mesmo veículo (feito antes, ou criado a partir da OS). O projeto não pode
     * estar arquivado nem em outra OS em andamento.
     */
    public void ligarProjeto(long osId, long projetoId) {
        banco.transacao(c -> {
            OrdemServicoSalva os = exigir(c, osId);
            ProjetoSalvo projeto = projetoRepositorio.buscar(c, projetoId).orElseThrow(
                    () -> new OperacaoRecusadaException("Projeto #" + projetoId + " não encontrado."));
            if (projeto.veiculoId() != os.veiculoId()) {
                throw new OperacaoRecusadaException("O projeto #" + projetoId + " é de outro veículo.");
            }
            if (projeto.status() == StatusProjeto.ARQUIVADO) {
                throw new OperacaoRecusadaException("O projeto #" + projetoId + " está arquivado: reative-o antes.");
            }
            Optional<OrdemServicoSalva> outra = ordens.atualDoProjeto(c, projetoId);
            if (outra.isPresent() && outra.get().id() != osId) {
                throw new OperacaoRecusadaException("O projeto #" + projetoId + " já está na OS "
                        + outra.get().numero().texto() + ", em andamento.");
            }
            ordens.ligarProjeto(c, osId, projetoId);
            return null;
        });
    }

    /**
     * Cria o projeto de preparação para a OS (o assistente aberto pela aba Preparação), com o cliente e o veículo
     * dela, e já o liga à OS: tudo ou nada.
     *
     * @param injecaoItemId null = nenhuma por enquanto
     * @throws EntradaInvalidaException se o motor ou a meta não passarem
     * @throws OperacaoRecusadaException se a OS já tem projeto ou não está em andamento
     */
    public long criarProjetoNaOs(long osId, String descricaoMotor, ConfiguracaoMotor motor, Meta meta,
            Long injecaoItemId) {
        OrdemServicoSalva os = banco.ler(c -> exigir(c, osId));
        CasosProjeto.NovoProjeto novo = new CasosProjeto.NovoProjeto(new ClienteExistente(os.clienteId()),
                new VeiculoExistente(os.veiculoId()), descricaoMotor, motor, meta, injecaoItemId);
        List<String> erros = projetos.errosDoProjeto(novo);
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
        return banco.transacao(c -> {
            long projetoId = projetos.inserirProjeto(c, novo);
            ordens.ligarProjeto(c, osId, projetoId);
            return projetoId;
        });
    }

    /** Os projetos de preparação do veículo, do mais novo para o mais antigo, sem os arquivados. */
    public List<ProjetoDoVeiculo> projetosDoVeiculo(long veiculoId) {
        return banco.ler(c -> {
            List<ProjetoDoVeiculo> lista = new ArrayList<>();
            for (ProjetoSalvo projeto : projetoRepositorio.doVeiculo(c, veiculoId)) {
                if (projeto.status() == StatusProjeto.ARQUIVADO) {
                    continue;
                }
                String motor = motores.atual(c, projeto.id()).map(v -> (v.descricao().isBlank() ? "Motor"
                        : v.descricao()) + " v" + v.numero()).orElse("Motor");
                lista.add(new ProjetoDoVeiculo(projeto.id(), motor, projeto.status()));
            }
            lista.sort(Comparator.comparingLong(ProjetoDoVeiculo::projetoId).reversed());
            return lista;
        });
    }

    // ===================== Anexos =====================

    /**
     * Anexa os arquivos à OS: cada um é copiado para a pasta de anexos do veículo (o mesmo arquivo duas vezes é uma
     * cópia só). Vale em qualquer status, também depois da entrega.
     *
     * @throws OperacaoRecusadaException se um arquivo não for JPG, PNG ou PDF, ou passar de 20 MB (nada é gravado)
     */
    public List<AnexoSalvo> anexar(long osId, List<NovoAnexo> novos) {
        if (novos == null || novos.isEmpty()) {
            return List.of();
        }
        List<String> erros = errosDosAnexos(novos);
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
        OrdemServicoSalva os = banco.ler(c -> exigir(c, osId));
        List<AnexosDaOficina.Copia> copias = new ArrayList<>();
        try {
            for (NovoAnexo novo : novos) {
                copias.add(copiar(novo.arquivo(), os.veiculoId()));
            }
            return banco.transacao(c -> {
                List<AnexoSalvo> gravados = new ArrayList<>();
                for (int i = 0; i < novos.size(); i++) {
                    long id = inserirAnexo(c, os.veiculoId(), osId, novos.get(i), copias.get(i));
                    gravados.add(anexos.buscar(c, id).orElseThrow());
                }
                return gravados;
            });
        } catch (RuntimeException erro) {
            apagarSobras(copias);
            throw erro;
        }
    }

    /** Arquivo que não existe e anexo sem tipo (o formato e o tamanho do arquivo são conferidos na cópia). */
    private static List<String> errosDosAnexos(List<NovoAnexo> novos) {
        List<String> erros = new ArrayList<>();
        for (NovoAnexo novo : novos) {
            if (novo.arquivo() == null || !Files.isRegularFile(novo.arquivo())) {
                erros.add("Arquivo não encontrado: " + novo.arquivo() + ".");
            } else if (novo.tipo() == null) {
                erros.add("Escolha o tipo do anexo " + novo.arquivo().getFileName() + ".");
            }
        }
        return erros;
    }

    /** Copia o arquivo para a pasta de anexos do veículo; um erro de disco vira {@link ErroDados}. */
    private AnexosDaOficina.Copia copiar(Path arquivo, long veiculoId) {
        try {
            return AnexosDaOficina.guardar(arquivo, pastaAnexos, veiculoId);
        } catch (IOException erro) {
            throw new ErroDados("Não foi possível copiar o arquivo para a pasta de anexos: " + erro.getMessage(), erro);
        }
    }

    private long inserirAnexo(Connection conexao, long veiculoId, long osId, NovoAnexo novo,
            AnexosDaOficina.Copia copia) throws SQLException {
        return anexos.inserir(conexao, veiculoId, osId, novo.tipo(), novo.descricao(), copia.arquivo(),
                novo.arquivo().getFileName().toString(), copia.tamanhoBytes(), copia.sha256(), novo.validade(),
                novo.pessoal());
    }

    /** Muda o tipo, a descrição, a validade ou a marca de documento pessoal do anexo. */
    public void editarAnexo(long anexoId, TipoAnexo tipo, String descricao, LocalDate validade, boolean pessoal) {
        if (tipo == null) {
            throw new EntradaInvalidaException("Escolha o tipo do anexo.");
        }
        banco.transacao(c -> {
            exigirAnexo(c, anexoId);
            anexos.atualizar(c, anexoId, tipo, descricao, validade, pessoal);
            return null;
        });
    }

    /**
     * Remove o anexo (ex.: a foto errada). Com a OS entregue ou cancelada, não: o que ficou registrado na entrega não
     * some. O arquivo é apagado quando nenhum outro anexo o usa.
     */
    public void removerAnexo(long anexoId) {
        AnexoSalvo anexo = banco.transacao(c -> {
            AnexoSalvo atual = exigirAnexo(c, anexoId);
            if (atual.osId() != null) {
                OrdemServicoSalva os = exigir(c, atual.osId());
                if (os.status().finalizada()) {
                    throw new OperacaoRecusadaException("A OS " + os.numero().texto() + " está \""
                            + os.status().rotulo() + "\": os anexos dela não se removem.");
                }
            }
            anexos.apagar(c, anexoId);
            return atual;
        });
        apagarSobras(List.of(new AnexosDaOficina.Copia(anexo.arquivo(), anexo.tamanhoBytes(), anexo.sha256())));
    }

    /** Os anexos da OS, do mais antigo para o mais novo. */
    public List<AnexoSalvo> anexosDaOs(long osId) {
        return banco.ler(c -> anexos.daOs(c, osId));
    }

    /** O arquivo do anexo na pasta de anexos (para abrir e para a miniatura); vazio se sumiu da pasta. */
    public Optional<Path> arquivoDoAnexo(AnexoSalvo anexo) {
        return AnexosDaOficina.arquivo(anexo.arquivo(), pastaAnexos);
    }

    /** Apaga as cópias que nenhum anexo usa (depois de remover, ou de uma gravação que não deu certo). */
    private void apagarSobras(List<AnexosDaOficina.Copia> copias) {
        for (AnexosDaOficina.Copia copia : copias) {
            try {
                if (banco.ler(c -> anexos.quantosUsam(c, copia.arquivo())) == 0) {
                    AnexosDaOficina.apagar(copia.arquivo(), pastaAnexos);
                }
            } catch (IOException | RuntimeException erro) {
                LOG.log(Level.WARNING, "Não foi possível apagar a cópia sem uso " + copia.arquivo(), erro);
            }
        }
    }

    private AnexoSalvo exigirAnexo(Connection conexao, long anexoId) throws SQLException {
        return anexos.buscar(conexao, anexoId).orElseThrow(
                () -> new OperacaoRecusadaException("Anexo #" + anexoId + " não encontrado."));
    }

    // ===================== Mecânicos =====================

    public List<MecanicoSalvo> mecanicos(boolean incluirInativos) {
        return banco.ler(c -> mecanicos.listar(c, incluirInativos));
    }

    public long novoMecanico(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new EntradaInvalidaException("Informe o nome do mecânico.");
        }
        return banco.transacao(c -> mecanicos.inserir(c, nome));
    }

    /** Quem saiu da oficina fica inativo: some da escolha nas OS novas, e as antigas continuam com ele. */
    public void ativarMecanico(long mecanicoId, boolean ativo) {
        banco.transacao(c -> {
            mecanicos.ativar(c, mecanicoId, ativo);
            return null;
        });
    }

    // ===================== Regras =====================

    private List<String> gravarEntrada(Connection conexao, long osId, long veiculoId, EntradaVeiculo entrada)
            throws SQLException {
        ordens.registrarEntrada(conexao, osId, entrada);
        return atualizarUso(conexao, veiculoId, entrada.uso(), "da entrada");
    }

    /** O km (ou as horas de motor) da OS vira o uso acumulado do veículo, com histórico; menor que o atual, avisa. */
    private List<String> atualizarUso(Connection conexao, long veiculoId, Double uso, String deOnde)
            throws SQLException {
        if (uso == null) {
            return List.of();
        }
        VeiculoSalvo salvo = veiculos.buscar(conexao, veiculoId).orElseThrow();
        Double atual = salvo.veiculo().usoAcumulado();
        if (Objects.equals(atual, uso)) {
            return List.of();
        }
        veiculos.atualizar(conexao, veiculoId, salvo.veiculo().comUsoAcumulado(uso), salvo.cambioItemId());
        if (atual == null || uso >= atual) {
            return List.of();
        }
        String unidade = salvo.veiculo().tipo() != null && salvo.veiculo().tipo().nautico() ? " h" : " km";
        return List.of("O uso " + deOnde + " (" + NumeroBr.formatar(uso, 0) + unidade + ") é menor que o do cadastro "
                + "do veículo (" + NumeroBr.formatar(atual, 0) + unidade + "). Gravei assim mesmo: confira se não foi "
                + "erro de digitação ou troca de painel.");
    }

    private SituacaoDoPagamento situacaoDoPagamento(Connection conexao, long osId) throws SQLException {
        Optional<OrcamentoSalvo> aprovado = orcamentos.ultimoAprovado(conexao, osId);
        Dinheiro recebido = pagamentos.recebido(conexao, osId);
        List<PagamentoSalvo> lista = pagamentos.daOs(conexao, osId);
        if (aprovado.isEmpty()) {
            return new SituacaoDoPagamento(null, recebido, null, null, lista);
        }
        Dinheiro valor = aprovado.get().conteudo().total();
        return new SituacaoDoPagamento(valor, recebido, SituacaoPagamento.falta(valor, recebido),
                SituacaoPagamento.de(valor, recebido), lista);
    }

    private OrdemServicoSalva exigir(Connection conexao, long osId) throws SQLException {
        return ordens.buscar(conexao, osId).orElseThrow(
                () -> new OperacaoRecusadaException("OS #" + osId + " não encontrada."));
    }

    private void exigirMecanicoAtivo(Connection conexao, Long mecanicoId) throws SQLException {
        if (mecanicoId == null) {
            return;
        }
        MecanicoSalvo mecanico = mecanicos.buscar(conexao, mecanicoId).orElseThrow(
                () -> new OperacaoRecusadaException("Mecânico #" + mecanicoId + " não encontrado."));
        if (!mecanico.ativo()) {
            throw new OperacaoRecusadaException("O mecânico " + mecanico.nome() + " está inativo.");
        }
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }
}
