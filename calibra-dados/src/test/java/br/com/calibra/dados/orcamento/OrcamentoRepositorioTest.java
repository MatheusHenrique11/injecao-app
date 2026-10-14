package br.com.calibra.dados.orcamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.calculo.CalculoDimensionamento;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.orcamento.Desconto;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.ItemOrcamento;
import br.com.calibra.core.orcamento.MeioAprovacao;
import br.com.calibra.core.orcamento.NumeroOrcamento;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.orcamento.StatusOrcamento;
import br.com.calibra.core.orcamento.TipoItem;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.semaforo.Justificativa;
import br.com.calibra.dados.BaseDeTeste;
import br.com.calibra.dados.Cadastros;
import br.com.calibra.dados.CasoBDados;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.projeto.CalculoRepositorio;
import br.com.calibra.dados.projeto.MotorRepositorio;
import br.com.calibra.dados.projeto.VersaoMotor;

/** Orçamentos no banco (bloco 2.5): numeração por ano, rascunho editável, status e aprovado que não muda. */
class OrcamentoRepositorioTest {

    @TempDir
    Path pasta;

    private BaseDeTeste base;
    private OrcamentoRepositorio orcamentos;
    private long projeto;
    private long calculo;
    private long motorVersao;

    @BeforeEach
    void abrir() throws Exception {
        base = BaseDeTeste.criada(pasta);
        Cadastros.Repositorios repositorios = Cadastros.Repositorios.de(base);
        MotorRepositorio motores = repositorios.motores();
        CalculoRepositorio calculos = new CalculoRepositorio(base.relogio);
        orcamentos = new OrcamentoRepositorio(base.relogio);
        projeto = base.banco.transacao(c -> repositorios.projetoCasoB(c, "João da Silva", "ABC1D23"));
        VersaoMotor v1 = base.banco.ler(c -> motores.atual(c, projeto)).orElseThrow();
        motorVersao = v1.id();
        EntradaDimensionamento entrada = CasoBDados.entrada();
        calculo = base.banco.transacao(c -> calculos.salvar(c, projeto, v1.id(), entrada,
                CalculoDimensionamento.calcular(entrada), List.of(new Justificativa(Indicador.DC_BICO, "Teste.",
                        LocalDateTime.of(2026, 10, 11, 18, 0))), List.of(), BaseDeTeste.VERSAO_APP));
    }

    private static Orcamento conteudo(String valorBico) {
        return new Orcamento(List.of(
                ItemOrcamento.digitado(TipoItem.PECA, "Bico teste", BigDecimal.valueOf(4), "un", Dinheiro.ler(valorBico)),
                ItemOrcamento.digitado(TipoItem.SERVICO, "Mão de obra", new BigDecimal("2.5"), "h",
                        Dinheiro.ler("180,00"))),
                Desconto.emPercentual(BigDecimal.TEN), 10, "a partir da aprovação", 15, "50% na aprovação", "");
    }

    private long novo(NumeroOrcamento numero, Orcamento conteudo) {
        return base.banco.transacao(c -> orcamentos.inserir(c, projeto, calculo, motorVersao, numero, conteudo));
    }

    private OrcamentoSalvo buscar(long id) {
        return base.banco.ler(c -> orcamentos.buscar(c, id)).orElseThrow();
    }

    private void mudar(long id, StatusOrcamento status, MeioAprovacao meio) {
        base.banco.transacao(c -> {
            orcamentos.mudarStatus(c, id, status, LocalDateTime.of(2026, 10, 12, 14, 30, 15, 99), meio, "Pix na hora.");
            return null;
        });
    }

    private int proximo(int ano) {
        return base.banco.ler(c -> orcamentos.proximoSequencial(c, ano));
    }

    @Test
    void numeracaoRecomecaACadaAno() {
        assertEquals(1, proximo(2026));
        novo(new NumeroOrcamento(2026, 1, 1), conteudo("450,00"));
        novo(new NumeroOrcamento(2026, 1, 2), conteudo("450,00"));
        assertEquals(2, proximo(2026), "a revisão não gasta número");
        assertEquals(1, proximo(2027));
        assertThrows(ErroDados.class, () -> novo(new NumeroOrcamento(2026, 1, 2), conteudo("450,00")),
                "número e revisão não se repetem");
    }

    @Test
    void rascunhoVoltaInteiroESeEdita() {
        long id = novo(new NumeroOrcamento(2026, 1, 1), conteudo("450,00"));
        OrcamentoSalvo salvo = buscar(id);
        assertEquals(conteudo("450,00"), salvo.conteudo());
        assertEquals(StatusOrcamento.RASCUNHO, salvo.status());
        assertEquals(calculo, salvo.calculoId());
        assertEquals(motorVersao, salvo.motorVersaoId());
        assertNull(salvo.enviadoEm());

        base.banco.transacao(c -> {
            orcamentos.atualizarConteudo(c, id, conteudo("500,00"));
            return null;
        });
        assertEquals(Dinheiro.ler("2.205,00"), buscar(id).conteudo().total(), "(4 × 500 + 450) − 10%");
        assertEquals(id, base.banco.ler(c -> orcamentos.doProjeto(c, projeto)).getFirst().id());
    }

    @Test
    void aprovadoNaoMuda() {
        long id = novo(new NumeroOrcamento(2026, 1, 1), conteudo("450,00"));
        assertThrows(OperacaoRecusadaException.class, () -> mudar(id, StatusOrcamento.APROVADO,
                MeioAprovacao.PESSOALMENTE), "aprovar só depois de enviar");
        mudar(id, StatusOrcamento.ENVIADO, null);
        assertNotNull(buscar(id).enviadoEm());
        assertThrows(OperacaoRecusadaException.class, () -> mudar(id, StatusOrcamento.APROVADO, null),
                "a aprovação pede como o cliente respondeu");
        mudar(id, StatusOrcamento.APROVADO, MeioAprovacao.WHATSAPP);

        OrcamentoSalvo aprovado = buscar(id);
        assertEquals(StatusOrcamento.APROVADO, aprovado.status());
        assertEquals(MeioAprovacao.WHATSAPP, aprovado.meioResposta());
        assertEquals(LocalDateTime.of(2026, 10, 12, 14, 30, 15), aprovado.respondidoEm());
        assertEquals("Pix na hora.", aprovado.observacaoResposta());
        assertEquals(List.of(StatusOrcamento.RASCUNHO, StatusOrcamento.ENVIADO, StatusOrcamento.APROVADO),
                base.banco.ler(c -> orcamentos.historico(c, id)).stream().map(MudancaStatusOrcamento::status).toList());

        OperacaoRecusadaException erro = assertThrows(OperacaoRecusadaException.class, () -> base.banco.transacao(c -> {
            orcamentos.atualizarConteudo(c, id, conteudo("1,00"));
            return null;
        }));
        assertTrue(erro.getMessage().contains("abra uma revisão nova"), erro.getMessage());
        assertThrows(ErroDados.class, () -> executar("UPDATE orcamento SET conteudo_json = '{}'"),
                "o banco também recusa");
        assertThrows(ErroDados.class, () -> executar("DELETE FROM orcamento"));
        assertEquals(conteudo("450,00"), buscar(id).conteudo());

        mudar(id, StatusOrcamento.SUBSTITUIDO, null);
        assertEquals(StatusOrcamento.SUBSTITUIDO, buscar(id).status());
        assertEquals(MeioAprovacao.WHATSAPP, buscar(id).meioResposta(), "a aprovação antiga fica registrada");
    }

    private void executar(String sql) {
        base.banco.transacao(c -> {
            try (PreparedStatement comando = c.prepareStatement(sql)) {
                return comando.executeUpdate();
            }
        });
    }
}
