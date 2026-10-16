package br.com.calibra.dados.oficina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.oficina.FormaPagamento;
import br.com.calibra.core.oficina.NumeroOs;
import br.com.calibra.core.oficina.Pagamento;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.dados.BaseDeTeste;
import br.com.calibra.dados.Cadastros;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.OperacaoRecusadaException;

/** Recebimentos da OS: em partes, a soma do que vale, e o cancelado que fica no histórico e não conta. */
class PagamentoRepositorioTest {

    @TempDir
    Path pasta;

    private BaseDeTeste base;
    private PagamentoRepositorio pagamentos;
    private long os;

    @BeforeEach
    void abrir() throws Exception {
        base = BaseDeTeste.criada(pasta);
        Cadastros.Repositorios cadastros = Cadastros.Repositorios.de(base);
        pagamentos = new PagamentoRepositorio(base.relogio);
        os = base.banco.transacao(c -> {
            long cliente = cadastros.clientes().inserir(c, Cadastros.cliente("João da Silva"));
            long veiculo = cadastros.veiculos().inserir(c, cliente, Cadastros.carro("ABC1D23"), null);
            return new OrdemServicoRepositorio(base.relogio).inserir(c, new NumeroOs(2026, 1),
                    new DadosAbertura(cliente, veiculo, null, "Revisão", null, LocalDate.of(2026, 10, 20), ""));
        });
    }

    private long receber(LocalDateTime quando, FormaPagamento forma, String valor, String observacao) {
        return base.banco.transacao(c -> pagamentos.registrar(c, os, new Pagamento(quando, forma, Dinheiro.ler(valor),
                observacao)));
    }

    private void executar(String sql) {
        base.banco.transacao(c -> {
            try (Statement comando = c.createStatement()) {
                return comando.executeUpdate(sql);
            }
        });
    }

    @Test
    void sinalEResto() {
        long resto = receber(LocalDateTime.of(2026, 10, 16, 17, 0), FormaPagamento.DINHEIRO, "1.012,50", "na entrega");
        long sinal = receber(LocalDateTime.of(2026, 10, 12, 10, 0), FormaPagamento.PIX, "1.012,50", "sinal");
        assertEquals(Dinheiro.ler("2.025,00"), base.banco.ler(c -> pagamentos.recebido(c, os)));
        List<PagamentoSalvo> lista = base.banco.ler(c -> pagamentos.daOs(c, os));
        assertEquals(List.of(sinal, resto), lista.stream().map(PagamentoSalvo::id).toList(), "na ordem do recebimento");
        assertEquals(new Pagamento(LocalDateTime.of(2026, 10, 12, 10, 0), FormaPagamento.PIX, Dinheiro.ler("1.012,50"),
                "sinal"), lista.getFirst().pagamento());
        assertFalse(lista.getFirst().cancelado());
    }

    @Test
    void canceladoFicaNoHistoricoENaoConta() {
        long errado = receber(LocalDateTime.of(2026, 10, 12, 10, 0), FormaPagamento.CARTAO_CREDITO, "2.025,00", "");
        receber(LocalDateTime.of(2026, 10, 12, 10, 5), FormaPagamento.CARTAO_DEBITO, "2.025,00", "");
        base.banco.transacao(c -> {
            pagamentos.cancelar(c, errado, " Lançado no crédito; foi no débito. ");
            return null;
        });
        assertEquals(Dinheiro.ler("2.025,00"), base.banco.ler(c -> pagamentos.recebido(c, os)));
        PagamentoSalvo cancelado = base.banco.ler(c -> pagamentos.buscar(c, errado)).orElseThrow();
        assertTrue(cancelado.cancelado());
        assertEquals("Lançado no crédito; foi no débito.", cancelado.motivoCancelamento());
        assertEquals(2, base.banco.ler(c -> pagamentos.daOs(c, os)).size(), "o cancelado continua na lista");
        assertThrows(OperacaoRecusadaException.class, () -> base.banco.transacao(c -> {
            pagamentos.cancelar(c, errado, "de novo");
            return null;
        }));
    }

    @Test
    void oBancoNaoDeixaMudarNemApagar() {
        long id = receber(LocalDateTime.of(2026, 10, 12, 10, 0), FormaPagamento.PIX, "100,00", "");
        assertThrows(ErroDados.class, () -> executar("UPDATE os_pagamento SET valor_centavos = 1 WHERE id = " + id));
        assertThrows(ErroDados.class, () -> executar("DELETE FROM os_pagamento"));
        assertThrows(ErroDados.class, () -> executar("INSERT INTO os_pagamento (os_id, recebido_em, forma, "
                + "valor_centavos, criado_em) VALUES (" + os + ", '2026-10-12T10:00:00', 'PIX', 0, "
                + "'2026-10-12T10:00:00')"), "valor maior que zero");
        executar("UPDATE os_pagamento SET cancelado_em = '2026-10-12T11:00:00', motivo_cancelamento = 'x' WHERE id = "
                + id);
        assertThrows(ErroDados.class, () -> executar("UPDATE os_pagamento SET cancelado_em = NULL WHERE id = " + id),
                "cancelado não volta");
    }
}
