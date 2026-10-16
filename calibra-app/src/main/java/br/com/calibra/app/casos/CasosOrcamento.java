package br.com.calibra.app.casos;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.core.cadastro.Veiculo;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.motor.Aspiracao;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.ItemOrcamento;
import br.com.calibra.core.orcamento.MeioAprovacao;
import br.com.calibra.core.orcamento.NumeroOrcamento;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.orcamento.PadroesOrcamento;
import br.com.calibra.core.orcamento.StatusOrcamento;
import br.com.calibra.core.orcamento.TipoItem;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.projeto.StatusProjeto;
import br.com.calibra.dados.BancoDados;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.catalogo.CatalogoRepositorio;
import br.com.calibra.dados.catalogo.TipoCatalogo;
import br.com.calibra.dados.catalogo.VersaoCatalogo;
import br.com.calibra.dados.orcamento.CalculoDoOrcamento;
import br.com.calibra.dados.orcamento.MudancaStatusOrcamento;
import br.com.calibra.dados.orcamento.OrcamentoRepositorio;
import br.com.calibra.dados.orcamento.OrcamentoSalvo;
import br.com.calibra.dados.projeto.CalculoSalvo;
import br.com.calibra.dados.projeto.PapelPeca;
import br.com.calibra.dados.projeto.PecaUsada;
import br.com.calibra.dados.projeto.ProjetoRepositorio;
import br.com.calibra.dados.projeto.ProjetoSalvo;
import br.com.calibra.dados.projeto.VersaoMotor;
import br.com.calibra.relatorios.DadosOrcamento;

/**
 * Casos de uso do orçamento (bloco 2.5): os itens saem do último cálculo do projeto (peças do catálogo com o preço de
 * referência da versão usada); cada OS tem um número de orçamento, com revisões (desde a fase 3, o orçamento
 * pertence à OS; o do projeto vai para a OS atual dele). Enviar leva o projeto a ORCADO; a aprovação, a APROVADO.
 * Sem JavaFX.
 */
public final class CasosOrcamento {

    /**
     * Uma revisão e o que mudou desde ela.
     *
     * @param motivos vazio = em dia com o projeto (o cálculo dela é o mais novo e está em dia)
     */
    public record SituacaoOrcamento(OrcamentoSalvo orcamento, List<String> motivos) {

        public SituacaoOrcamento {
            motivos = List.copyOf(motivos);
        }

        public boolean desatualizado() {
            return !motivos.isEmpty();
        }
    }

    private final BancoDados banco;
    private final Clock relogio;
    private final CasosProjeto projetos;
    private final OrcamentoRepositorio orcamentos;
    private final ProjetoRepositorio projetoRepositorio;
    private final OrdensDoProjeto ordensDoProjeto;
    private final CatalogoRepositorio catalogo;

    public CasosOrcamento(BancoDados banco, Clock relogio, CasosProjeto projetos) {
        this.banco = Objects.requireNonNull(banco, "banco");
        this.relogio = Objects.requireNonNull(relogio, "relogio");
        this.projetos = Objects.requireNonNull(projetos, "projetos");
        this.orcamentos = new OrcamentoRepositorio(relogio);
        this.projetoRepositorio = new ProjetoRepositorio(relogio);
        this.ordensDoProjeto = new OrdensDoProjeto(relogio);
        this.catalogo = new CatalogoRepositorio(relogio);
    }

    // ===================== Consultas =====================

    /** As revisões do projeto, da mais nova para a mais antiga, com o que mudou desde cada uma. */
    public List<SituacaoOrcamento> orcamentos(long projetoId) {
        List<CasosProjeto.SituacaoCalculo> situacoes = projetos.calculos(projetoId);
        Map<Long, CasosProjeto.SituacaoCalculo> porId = situacoes.stream()
                .collect(Collectors.toMap(s -> s.calculo().id(), Function.identity()));
        Long maisNovo = situacoes.isEmpty() ? null : situacoes.getFirst().calculo().id();
        List<SituacaoOrcamento> lista = new ArrayList<>();
        for (OrcamentoSalvo orcamento : banco.ler(c -> orcamentos.doProjeto(c, projetoId))) {
            List<String> motivos = new ArrayList<>();
            if (!Objects.equals(orcamento.calculo().calculoId(), maisNovo)) {
                motivos.add("há um cálculo salvo mais novo que o deste orçamento");
            }
            CasosProjeto.SituacaoCalculo calculo = porId.get(orcamento.calculo().calculoId());
            if (calculo != null) {
                motivos.addAll(calculo.motivos());
            }
            lista.add(new SituacaoOrcamento(orcamento, motivos));
        }
        return lista;
    }

    public Optional<OrcamentoSalvo> buscar(long orcamentoId) {
        return banco.ler(c -> orcamentos.buscar(c, orcamentoId));
    }

    public List<MudancaStatusOrcamento> historico(long orcamentoId) {
        return banco.ler(c -> orcamentos.historico(c, orcamentoId));
    }

    /**
     * O orçamento novo já preenchido: os itens do último cálculo e os padrões de Configurações. Peça sem preço de
     * referência vem sem valor (para a pessoa preencher), e o resultado pode ainda não passar na validação.
     *
     * @throws EntradaInvalidaException se o projeto não tem cálculo salvo em dia
     */
    public Orcamento sugestao(long projetoId, PadroesOrcamento padroes) {
        CalculoSalvo calculo = calculoEmDia(projetoId);
        return padroes.orcamento(banco.ler(c -> itensDoCalculo(c, calculo)));
    }

    /**
     * As peças do cálculo, na ordem bicos, bomba, turbo, intercooler, sensor MAP e injeção. As do catálogo levam a
     * versão e o preço de referência dela; as digitadas no cálculo (bico, injeção) vêm sem preço. O câmbio é do
     * veículo e o combustível é do motor: não entram.
     */
    List<ItemOrcamento> itensDoCalculo(Connection conexao, CalculoSalvo calculo) throws SQLException {
        EntradaDimensionamento e = calculo.entrada();
        int cilindros = e.motor().numeroCilindros();
        Map<PapelPeca, PecaUsada> pecas = calculo.pecas().stream()
                .collect(Collectors.toMap(PecaUsada::papel, Function.identity(), (a, b) -> a));
        List<ItemOrcamento> itens = new ArrayList<>();
        peca(conexao, itens, pecas.get(PapelPeca.BICO), TipoCatalogo.BICO, cilindros, digitado(e.bico()));
        peca(conexao, itens, pecas.get(PapelPeca.BICO_SECUNDARIO), TipoCatalogo.BICO, cilindros,
                digitado(e.bicoSecundario()));
        peca(conexao, itens, pecas.get(PapelPeca.BOMBA), TipoCatalogo.BOMBA, 1, null);
        peca(conexao, itens, pecas.get(PapelPeca.TURBO), TipoCatalogo.TURBO,
                e.motor().aspiracao() == Aspiracao.BITURBO ? 2 : 1, null);
        peca(conexao, itens, pecas.get(PapelPeca.INTERCOOLER), TipoCatalogo.INTERCOOLER, 1, null);
        peca(conexao, itens, pecas.get(PapelPeca.SENSOR_MAP), TipoCatalogo.SENSOR, 1, null);
        String injecaoDigitada = e.injecao() == null ? null
                : e.injecao().nome().equals("informada") ? "Injeção programável" : e.injecao().nome();
        peca(conexao, itens, pecas.get(PapelPeca.INJECAO), TipoCatalogo.INJECAO, 1, injecaoDigitada);
        return itens;
    }

    private static String digitado(Bico bico) {
        return bico == null ? null : bico.nome();
    }

    /** Uma peça: do catálogo (com o preço da versão usada) ou digitada no cálculo (sem preço); nada se não houver. */
    private <T> void peca(Connection conexao, List<ItemOrcamento> itens, PecaUsada usada, TipoCatalogo<T> tipo,
            int quantidade, String digitada) throws SQLException {
        BigDecimal qtd = BigDecimal.valueOf(quantidade);
        if (usada != null) {
            Optional<VersaoCatalogo<T>> versao = catalogo.versao(conexao, tipo, usada.itemId(), usada.versao());
            if (versao.isPresent()) {
                Dinheiro preco = versao.get().info().precoReferencia();
                itens.add(new ItemOrcamento(TipoItem.PECA, versao.get().info().nome(), qtd, "un", preco,
                        usada.itemId(), usada.versao()));
                return;
            }
        }
        if (digitada != null) {
            itens.add(new ItemOrcamento(TipoItem.PECA, digitada, qtd, "un", null, null, null));
        }
    }

    /**
     * Os dados do PDF e do texto de WhatsApp de uma revisão: o cliente como está agora no cadastro (anonimizado, se
     * for o caso), o veículo, a versão do motor e o cálculo que o orçamento usou.
     *
     * @param logo o arquivo do logo da oficina; null sem logo
     */
    public DadosOrcamento documento(long orcamentoId, DadosOficina oficina, Path logo, String versaoApp) {
        OrcamentoSalvo orcamento = exigir(orcamentoId);
        CalculoDoOrcamento usado = exigirPreparacao(orcamento);
        CasosProjeto.ProjetoAberto aberto = projetos.abrir(usado.projetoId());
        CalculoSalvo calculo = projetos.calculo(usado.calculoId()).orElseThrow();
        VersaoMotor motor = projetos.versoesMotor(usado.projetoId()).stream()
                .filter(v -> v.id() == usado.motorVersaoId()).findFirst().orElseThrow();
        Veiculo veiculo = aberto.veiculo().veiculo();
        String textoVeiculo = veiculo.descricao() + (veiculo.anoModelo() == null ? "" : " " + veiculo.anoModelo())
                + (veiculo.identificacao().isEmpty() ? "" : " · " + veiculo.identificacao());
        String textoMotor = (motor.descricao().isEmpty() ? "Motor" : motor.descricao()) + " (v" + motor.numero() + ")";
        LocalDateTime agora = LocalDateTime.now(relogio).withSecond(0).withNano(0);
        LocalDate emissao = orcamento.enviadoEm() == null ? agora.toLocalDate() : orcamento.enviadoEm().toLocalDate();
        return new DadosOrcamento(oficina, logo, aberto.cliente().cliente().nome(), textoVeiculo, textoMotor,
                orcamento.numero(), orcamento.status(), orcamento.conteudo(), emissao, calculo.entrada(),
                calculo.resultado(), calculo.justificativas(), versaoApp, agora);
    }

    // ===================== Rascunho =====================

    /**
     * Cria o orçamento do projeto (o primeiro; os seguintes são revisões dele), em RASCUNHO, com o último cálculo. Ele
     * vai para a OS atual do projeto; sem OS em andamento, uma OS nova é aberta para ele (ABERTA, sem a entrada).
     *
     * @throws EntradaInvalidaException se o conteúdo não passar, se o cálculo não estiver em dia ou se a OS atual do
     *     projeto já tiver orçamento
     */
    public OrcamentoSalvo criar(long projetoId, Orcamento conteudo) {
        exigirValido(conteudo);
        CalculoSalvo calculo = calculoEmDia(projetoId);
        return banco.transacao(c -> {
            exigirAtivo(c, projetoId);
            long osId = ordensDoProjeto.atual(c, projetoId);
            List<OrcamentoSalvo> existentes = orcamentos.daOs(c, osId);
            if (!existentes.isEmpty()) {
                OrcamentoSalvo ultimo = existentes.getFirst();
                throw new OperacaoRecusadaException("O projeto já tem o orçamento " + ultimo.numero().comRevisao()
                        + (ultimo.status().editavel() ? ": edite o rascunho." : ": abra uma revisão nova."));
            }
            int ano = LocalDate.now(relogio).getYear();
            NumeroOrcamento numero = new NumeroOrcamento(ano, orcamentos.proximoSequencial(c, ano), 1);
            long id = orcamentos.inserir(c, osId, new CalculoDoOrcamento(projetoId, calculo.id(),
                    calculo.versaoMotorId()), numero, conteudo);
            return orcamentos.buscar(c, id).orElseThrow();
        });
    }

    /** Grava o rascunho editado. */
    public void salvar(long orcamentoId, Orcamento conteudo) {
        exigirValido(conteudo);
        banco.transacao(c -> {
            orcamentos.atualizarConteudo(c, orcamentoId, conteudo);
            return null;
        });
    }

    /** O rascunho passa a usar o cálculo mais novo do projeto (os itens ficam como estão, para a pessoa revisar). */
    public void usarCalculoMaisNovo(long orcamentoId) {
        OrcamentoSalvo orcamento = exigir(orcamentoId);
        CalculoSalvo calculo = calculoEmDia(exigirPreparacao(orcamento).projetoId());
        banco.transacao(c -> {
            orcamentos.atualizarCalculo(c, orcamentoId, calculo.id(), calculo.versaoMotorId());
            return null;
        });
    }

    // ===================== Status =====================

    /**
     * Marca o rascunho como enviado (a pessoa mandou o PDF ou o texto). A validade conta a partir daqui, e o projeto
     * vai para ORCADO.
     *
     * @throws EntradaInvalidaException se o orçamento estiver desatualizado (motor ou cálculo mudaram)
     */
    public void marcarEnviado(long orcamentoId) {
        OrcamentoSalvo orcamento = exigir(orcamentoId);
        List<String> motivos = motivos(orcamento);
        if (!motivos.isEmpty()) {
            throw new EntradaInvalidaException("O orçamento está desatualizado (" + String.join("; ", motivos)
                    + "). Use o cálculo mais novo e confira os itens antes de enviar.");
        }
        banco.transacao(c -> {
            exigirAtivo(c, orcamento);
            orcamentos.mudarStatus(c, orcamentoId, StatusOrcamento.ENVIADO, null, null, null);
            if (orcamento.dePreparacao()) {
                avancarProjeto(c, orcamento.calculo().projetoId(), StatusProjeto.ORCADO);
            }
            return null;
        });
    }

    /**
     * O cliente aprovou: grava a data, a hora e o meio, e o projeto vai para APROVADO. Depois disso o orçamento não
     * muda (só com revisão nova).
     */
    public void aprovar(long orcamentoId, LocalDateTime quando, MeioAprovacao meio, String observacao) {
        responder(orcamentoId, StatusOrcamento.APROVADO, quando, meio, observacao);
    }

    /** O cliente recusou: grava a data, a hora e o meio; o projeto fica como está. */
    public void recusar(long orcamentoId, LocalDateTime quando, MeioAprovacao meio, String observacao) {
        responder(orcamentoId, StatusOrcamento.RECUSADO, quando, meio, observacao);
    }

    private void responder(long orcamentoId, StatusOrcamento resposta, LocalDateTime quando, MeioAprovacao meio,
            String observacao) {
        List<String> erros = new ArrayList<>();
        if (quando == null) {
            erros.add("Informe a data e a hora da resposta do cliente.");
        } else if (quando.isAfter(LocalDateTime.now(relogio).plusMinutes(1))) {
            erros.add("A data e a hora da resposta não podem estar no futuro.");
        }
        if (meio == null) {
            erros.add("Escolha como o cliente respondeu (pessoalmente, WhatsApp ou telefone).");
        }
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
        OrcamentoSalvo orcamento = exigir(orcamentoId);
        banco.transacao(c -> {
            exigirAtivo(c, orcamento);
            orcamentos.mudarStatus(c, orcamentoId, resposta, quando, meio, observacao);
            if (resposta == StatusOrcamento.APROVADO && orcamento.dePreparacao()) {
                avancarProjeto(c, orcamento.calculo().projetoId(), StatusProjeto.APROVADO);
            }
            return null;
        });
    }

    /**
     * Revisão nova de um orçamento enviado, aprovado ou recusado: o mesmo número na revisão seguinte, em RASCUNHO, com
     * o mesmo conteúdo e o cálculo mais novo do projeto; a anterior fica SUBSTITUIDO.
     */
    public OrcamentoSalvo novaRevisao(long orcamentoId) {
        OrcamentoSalvo anterior = exigir(orcamentoId);
        if (!anterior.status().aceitaRevisao()) {
            throw new OperacaoRecusadaException(anterior.status() == StatusOrcamento.RASCUNHO
                    ? "O rascunho se edita direto: não precisa de revisão."
                    : "O orçamento " + anterior.numero().comRevisao() + " já foi substituído.");
        }
        CalculoDoOrcamento usado = anterior.calculo();
        CalculoSalvo calculo = usado == null ? null : calculoEmDia(usado.projetoId());
        return banco.transacao(c -> {
            exigirAtivo(c, anterior);
            orcamentos.mudarStatus(c, orcamentoId, StatusOrcamento.SUBSTITUIDO, null, null, null);
            CalculoDoOrcamento novo = calculo == null ? null
                    : new CalculoDoOrcamento(usado.projetoId(), calculo.id(), calculo.versaoMotorId());
            long id = orcamentos.inserir(c, anterior.osId(), novo, anterior.numero().proximaRevisao(),
                    anterior.conteudo());
            return orcamentos.buscar(c, id).orElseThrow();
        });
    }

    // ===================== Regras =====================

    /** O último cálculo do projeto, se estiver em dia com o motor, o núcleo e o catálogo. */
    private CalculoSalvo calculoEmDia(long projetoId) {
        List<CasosProjeto.SituacaoCalculo> situacoes = projetos.calculos(projetoId);
        if (situacoes.isEmpty()) {
            throw new EntradaInvalidaException("Calcule e salve o dimensionamento do projeto antes do orçamento.");
        }
        CasosProjeto.SituacaoCalculo ultimo = situacoes.getFirst();
        if (ultimo.desatualizado()) {
            throw new EntradaInvalidaException("O último cálculo do projeto está desatualizado ("
                    + String.join("; ", ultimo.motivos()) + "). Calcule e salve de novo antes do orçamento.");
        }
        return projetos.calculo(ultimo.calculo().id()).orElseThrow();
    }

    /** O que mudou desde o orçamento de preparação; o da OS comum não depende de cálculo. */
    private List<String> motivos(OrcamentoSalvo orcamento) {
        if (!orcamento.dePreparacao()) {
            return List.of();
        }
        return orcamentos(orcamento.calculo().projetoId()).stream().filter(s -> s.orcamento().id() == orcamento.id())
                .findFirst().map(SituacaoOrcamento::motivos).orElse(List.of());
    }

    private static CalculoDoOrcamento exigirPreparacao(OrcamentoSalvo orcamento) {
        if (!orcamento.dePreparacao()) {
            throw new OperacaoRecusadaException("O orçamento " + orcamento.numero().comRevisao()
                    + " não usa cálculo de preparação.");
        }
        return orcamento.calculo();
    }

    private OrcamentoSalvo exigir(long orcamentoId) {
        return buscar(orcamentoId).orElseThrow(
                () -> new OperacaoRecusadaException("Orçamento #" + orcamentoId + " não encontrado."));
    }

    private static void exigirValido(Orcamento conteudo) {
        List<String> erros = conteudo.validar();
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
    }

    /** O orçamento de preparação exige o projeto ativo. */
    private void exigirAtivo(Connection conexao, OrcamentoSalvo orcamento) throws SQLException {
        if (orcamento.dePreparacao()) {
            exigirAtivo(conexao, orcamento.calculo().projetoId());
        }
    }

    private void exigirAtivo(Connection conexao, long projetoId) throws SQLException {
        ProjetoSalvo projeto = projetoRepositorio.buscar(conexao, projetoId).orElseThrow(
                () -> new OperacaoRecusadaException("Projeto #" + projetoId + " não encontrado."));
        if (projeto.status() == StatusProjeto.ARQUIVADO) {
            throw new OperacaoRecusadaException("O projeto está arquivado: reative-o antes de mexer no orçamento.");
        }
    }

    /** Leva o projeto para a frente, um passo de cada vez, até o alvo; se já passou dele, fica como está. */
    private void avancarProjeto(Connection conexao, long projetoId, StatusProjeto alvo) throws SQLException {
        StatusProjeto atual = projetoRepositorio.buscar(conexao, projetoId).orElseThrow().status();
        while (atual.ordinal() < alvo.ordinal()) {
            StatusProjeto proximo = atual.proximo().orElseThrow();
            projetoRepositorio.mudarStatus(conexao, projetoId, proximo);
            atual = proximo;
        }
    }
}
