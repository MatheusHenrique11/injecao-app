package br.com.calibra.dados.cadastro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.cadastro.TipoCambio;
import br.com.calibra.core.cadastro.Veiculo;
import br.com.calibra.dados.BaseDeTeste;
import br.com.calibra.dados.Cadastros;
import br.com.calibra.dados.OperacaoRecusadaException;

/** Veículos no banco (SPEC 6.2): ida e volta, histórico de cada salvamento e arquivar × apagar. */
class VeiculoRepositorioTest {

    @TempDir
    Path pasta;

    private BaseDeTeste base;
    private Cadastros.Repositorios repositorios;
    private VeiculoRepositorio veiculos;
    private long cliente;

    @BeforeEach
    void abrir() throws Exception {
        base = BaseDeTeste.criada(pasta);
        repositorios = Cadastros.Repositorios.de(base);
        veiculos = repositorios.veiculos();
        cliente = base.banco.transacao(c -> repositorios.clientes().inserir(c, Cadastros.cliente("João da Silva")));
    }

    @Test
    void gravaELeCarroELancha() {
        Veiculo gol = Cadastros.carro("ABC1D23");
        Veiculo focker = Cadastros.lancha("Maré Alta");
        long idGol = base.banco.transacao(c -> veiculos.inserir(c, cliente, gol, null));
        long idFocker = base.banco.transacao(c -> veiculos.inserir(c, cliente, focker, null));
        assertEquals(gol, base.banco.ler(c -> veiculos.buscar(c, idGol)).orElseThrow().veiculo());
        assertEquals(focker, base.banco.ler(c -> veiculos.buscar(c, idFocker)).orElseThrow().veiculo());
        assertEquals(List.of(idFocker, idGol),
                base.banco.ler(c -> veiculos.doCliente(c, cliente, false)).stream().map(VeiculoSalvo::id).toList());
    }

    /** SPEC 6.2: todo salvamento grava campo, valor antigo, valor novo, data e hora. */
    @Test
    void cadaSalvamentoGravaOQueMudouNoHistorico() {
        long id = base.banco.transacao(c -> veiculos.inserir(c, cliente, Cadastros.carro("ABC1D23"), null));
        Veiculo gol = Cadastros.carro("ABC1D23");
        Veiculo mudado = new Veiculo(gol.tipo(), gol.marca(), gol.modelo(), gol.versao(), gol.anoFabricacao(),
                gol.anoModelo(), gol.placa(), gol.chassi(), "", "", "", 251_500.0, TipoCambio.AUTOMATICO, true,
                gol.tracao(), gol.pesoKg(), gol.cor(), gol.observacoes());
        base.banco.transacao(c -> {
            veiculos.atualizar(c, id, mudado, null);
            return null;
        });

        List<EventoAuditoria> historico = base.banco.ler(c -> veiculos.historico(c, id));
        assertEquals(List.of("Uso acumulado", "Câmbio", "Câmbio controlado pela injeção", "Cadastro"),
                historico.stream().map(EventoAuditoria::campo).toList());
        EventoAuditoria uso = historico.getFirst();
        assertEquals("250.000 km", uso.valorAntigo());
        assertEquals("251.500 km", uso.valorNovo());
        assertEquals("Manual", historico.get(1).valorAntigo());
        assertEquals("Automático", historico.get(1).valorNovo());
        assertTrue(historico.getFirst().dataHora().isAfter(historico.getLast().dataHora()), "data e hora gravadas");
    }

    /** SPEC 6.2: veículo com projeto não pode ser apagado, só arquivado. */
    @Test
    void comProjetoSoArquivaSemProjetoApaga() {
        long projeto = base.banco.transacao(c -> repositorios.projetoCasoB(c, "Maria Souza", "XYZ9A87"));
        long comProjeto = base.banco.ler(c -> repositorios.projetos().buscar(c, projeto)).orElseThrow().veiculoId();

        OperacaoRecusadaException erro = assertThrows(OperacaoRecusadaException.class,
                () -> base.banco.transacao(c -> {
                    veiculos.apagar(c, comProjeto);
                    return null;
                }));
        assertTrue(erro.getMessage().contains("só arquivado"), erro.getMessage());

        base.banco.transacao(c -> {
            veiculos.arquivar(c, comProjeto, true);
            return null;
        });
        VeiculoSalvo arquivado = base.banco.ler(c -> veiculos.buscar(c, comProjeto)).orElseThrow();
        assertTrue(arquivado.arquivado());
        long dono = arquivado.clienteId();
        assertTrue(base.banco.ler(c -> veiculos.doCliente(c, dono, false)).isEmpty(), "sai da lista");
        assertEquals(1, base.banco.ler(c -> veiculos.doCliente(c, dono, true)).size());
        assertEquals("Arquivado", base.banco.ler(c -> veiculos.historico(c, comProjeto)).getFirst().campo());
        assertTrue(base.banco.ler(c -> repositorios.projetos().buscar(c, projeto)).isPresent(), "o projeto continua");

        long semProjeto = base.banco.transacao(c -> veiculos.inserir(c, cliente, Cadastros.carro("DEF4G56"), null));
        base.banco.transacao(c -> {
            veiculos.apagar(c, semProjeto);
            return null;
        });
        assertTrue(base.banco.ler(c -> veiculos.buscar(c, semProjeto)).isEmpty());
    }
}
