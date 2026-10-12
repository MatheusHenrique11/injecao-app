package br.com.calibra.dados.cadastro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.cadastro.Cliente;
import br.com.calibra.core.cadastro.Veiculo;
import br.com.calibra.dados.BaseDeTeste;
import br.com.calibra.dados.Cadastros;
import br.com.calibra.dados.OperacaoRecusadaException;

/** Clientes no banco: gravação, histórico, busca sem acento, anonimização (LGPD) e apagar. */
class ClienteRepositorioTest {

    @TempDir
    Path pasta;

    private BaseDeTeste base;
    private Cadastros.Repositorios repositorios;
    private ClienteRepositorio clientes;

    @BeforeEach
    void abrir() throws Exception {
        base = BaseDeTeste.criada(pasta);
        repositorios = Cadastros.Repositorios.de(base);
        clientes = repositorios.clientes();
    }

    @Test
    void gravaELeOMesmoCliente() {
        Cliente joao = Cadastros.cliente("João da Silva");
        long id = base.banco.transacao(c -> clientes.inserir(c, joao));
        ClienteSalvo salvo = base.banco.ler(c -> clientes.buscar(c, id)).orElseThrow();
        assertEquals(joao, salvo.cliente());
        List<EventoAuditoria> historico = base.banco.ler(c -> clientes.historico(c, id));
        assertEquals(1, historico.size());
        assertEquals("Cadastro", historico.getFirst().campo());
    }

    @Test
    void editarGravaCampoAntesEDepoisNoHistorico() {
        long id = base.banco.transacao(c -> clientes.inserir(c, Cadastros.cliente("João da Silva")));
        Cliente original = base.banco.ler(c -> clientes.buscar(c, id)).orElseThrow().cliente();
        Cliente novo = Cliente.novo(original.nome(), "(11) 91234-5678", original.email(), original.documento(),
                original.consentimento(), original.observacoes());
        base.banco.transacao(c -> {
            clientes.atualizar(c, id, novo);
            return null;
        });

        List<EventoAuditoria> historico = base.banco.ler(c -> clientes.historico(c, id));
        assertEquals(2, historico.size(), historico::toString);
        EventoAuditoria mudanca = historico.getFirst();
        assertEquals("Telefone", mudanca.campo());
        assertEquals("(11) 98765-4321", mudanca.valorAntigo());
        assertEquals("(11) 91234-5678", mudanca.valorNovo());
        assertEquals("(11) 91234-5678", base.banco.ler(c -> clientes.buscar(c, id)).orElseThrow().cliente().telefone());
    }

    @Test
    void buscaSemAcentoNoNomeTelefoneOuDocumento() {
        base.banco.transacao(c -> {
            clientes.inserir(c, Cadastros.cliente("João da Silva"));
            clientes.inserir(c, Cadastros.cliente("Maria Antônia"));
            return null;
        });
        assertEquals(List.of("João da Silva"), nomes("joao"));
        assertEquals(List.of("Maria Antônia"), nomes("ANTONIA"));
        assertEquals(List.of("João da Silva", "Maria Antônia"), nomes("98765-4321"), "telefone sem pontuação");
        assertEquals(List.of("João da Silva", "Maria Antônia"), nomes(""), "busca vazia traz todos");
        assertEquals(List.of(), nomes("joao maria"), "todas as palavras precisam aparecer");
    }

    /** SPEC 6.1 e regra 2.8: anonimizar apaga os dados pessoais, também no histórico, e mantém o resto. */
    @Test
    void anonimizarApagaDadosPessoaisDoClienteDosVeiculosEDoHistorico() {
        long[] ids = base.banco.transacao(c -> {
            long cliente = clientes.inserir(c, Cadastros.cliente("João da Silva"));
            long carro = repositorios.veiculos().inserir(c, cliente, Cadastros.carro("ABC1D23"));
            long lancha = repositorios.veiculos().inserir(c, cliente, Cadastros.lancha("Maré Alta"));
            return new long[] {cliente, carro, lancha};
        });
        long cliente = ids[0];
        long carro = ids[1];
        long lancha = ids[2];
        // Edições antes de anonimizar deixam valores pessoais no histórico.
        base.banco.transacao(c -> {
            Cliente atual = clientes.buscar(c, cliente).orElseThrow().cliente();
            clientes.atualizar(c, cliente, Cliente.novo("João Silva", "(11) 90000-0000", atual.email(),
                    atual.documento(), atual.consentimento(), atual.observacoes()));
            Veiculo gol = repositorios.veiculos().buscar(c, carro).orElseThrow().veiculo();
            repositorios.veiculos().atualizar(c, carro, new Veiculo(gol.tipo(), gol.marca(), gol.modelo(),
                    gol.versao(), gol.anoFabricacao(), gol.anoModelo(), "XYZ9A87", gol.chassi(), "", "", "",
                    gol.usoAcumulado(), gol.cambio(), false, gol.tracao(), gol.pesoKg(), "Vermelho", ""));
            clientes.anonimizar(c, cliente);
            return null;
        });

        Cliente anonimo = base.banco.ler(c -> clientes.buscar(c, cliente)).orElseThrow().cliente();
        assertEquals("Cliente anonimizado #" + cliente, anonimo.nome());
        assertTrue(anonimo.anonimizado());
        assertEquals("", anonimo.telefone());
        assertEquals("", anonimo.documento());
        assertEquals("", anonimo.email());

        Veiculo gol = base.banco.ler(c -> repositorios.veiculos().buscar(c, carro)).orElseThrow().veiculo();
        assertEquals("", gol.placa());
        assertEquals("", gol.chassi());
        assertEquals("Gol", gol.modelo(), "dados técnicos ficam");
        assertEquals("Vermelho", gol.cor());
        Veiculo focker = base.banco.ler(c -> repositorios.veiculos().buscar(c, lancha)).orElseThrow().veiculo();
        assertEquals("", focker.nomeEmbarcacao());
        assertEquals("", focker.numeroInscricao());
        assertEquals("", focker.numeroCasco());

        List<EventoAuditoria> historicoCliente = base.banco.ler(c -> clientes.historico(c, cliente));
        assertFalse(contem(historicoCliente, "João"), historicoCliente::toString);
        assertFalse(contem(historicoCliente, "90000"), historicoCliente::toString);
        assertTrue(historicoCliente.stream().anyMatch(e -> e.campo().equals("Anonimização")));
        List<EventoAuditoria> historicoCarro = base.banco.ler(c -> repositorios.veiculos().historico(c, carro));
        assertFalse(contem(historicoCarro, "ABC1D23") || contem(historicoCarro, "XYZ9A87"), historicoCarro::toString);
        assertTrue(contem(historicoCarro, "Vermelho"), "a cor não identifica o dono e fica no histórico");

        assertEquals(List.of(), nomes(""), "anonimizado não aparece na busca de clientes");
        assertThrows(OperacaoRecusadaException.class, () -> base.banco.transacao(c -> {
            clientes.atualizar(c, cliente, Cadastros.cliente("Outro Nome"));
            return null;
        }));
    }

    @Test
    void clienteComVeiculoNaoEApagadoSemVeiculoE() {
        long comVeiculo = base.banco.transacao(c -> {
            long id = clientes.inserir(c, Cadastros.cliente("João da Silva"));
            repositorios.veiculos().inserir(c, id, Cadastros.carro("ABC1D23"));
            return id;
        });
        OperacaoRecusadaException erro = assertThrows(OperacaoRecusadaException.class,
                () -> base.banco.transacao(c -> {
                    clientes.apagar(c, comVeiculo);
                    return null;
                }));
        assertTrue(erro.getMessage().contains("Anonimizar"), erro.getMessage());

        long semVeiculo = base.banco.transacao(c -> clientes.inserir(c, Cadastros.cliente("Cadastro por engano")));
        base.banco.transacao(c -> {
            clientes.apagar(c, semVeiculo);
            return null;
        });
        assertTrue(base.banco.ler(c -> clientes.buscar(c, semVeiculo)).isEmpty());
        assertTrue(base.banco.ler(c -> clientes.historico(c, semVeiculo)).isEmpty(), "o histórico vai junto");
    }

    @Test
    void anonimizarSoTrocaValoresPessoaisPreenchidos() {
        long id = base.banco.transacao(c -> clientes.inserir(c, Cliente.novo("Ana", "11987654321", "", "", null, "")));
        base.banco.transacao(c -> {
            clientes.atualizar(c, id, Cliente.novo("Ana", "11987654321", "ana@exemplo.com.br", "", null, ""));
            clientes.anonimizar(c, id);
            return null;
        });
        List<EventoAuditoria> historico = base.banco.ler(c -> clientes.historico(c, id));
        EventoAuditoria email = historico.stream().filter(e -> e.campo().equals("E-mail")).findFirst().orElseThrow();
        assertEquals("", email.valorAntigo(), "valor vazio continua vazio");
        assertEquals(Auditoria.APAGADO, email.valorNovo());
        EventoAuditoria criacao = historico.getLast();
        assertNull(criacao.valorAntigo());
        assertEquals("criado", criacao.valorNovo(), "a linha da criação não tem dado pessoal");
    }

    private List<String> nomes(String termo) {
        return base.banco.ler(c -> clientes.pesquisar(c, termo, 50)).stream().map(s -> s.cliente().nome()).toList();
    }

    private static boolean contem(List<EventoAuditoria> historico, String trecho) {
        return historico.stream().anyMatch(e -> (e.valorAntigo() != null && e.valorAntigo().contains(trecho))
                || (e.valorNovo() != null && e.valorNovo().contains(trecho)));
    }
}
