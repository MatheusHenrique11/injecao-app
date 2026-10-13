package br.com.calibra.dados.projeto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.calculo.CalculoDimensionamento;
import br.com.calibra.core.calculo.Dimensionamento;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.memoria.VersaoNucleo;
import br.com.calibra.core.motor.Aspiracao;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.projeto.StatusProjeto;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.semaforo.Justificativa;
import br.com.calibra.dados.BaseDeTeste;
import br.com.calibra.dados.Cadastros;
import br.com.calibra.dados.CasoBDados;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.OperacaoRecusadaException;

/** Projetos, versões do motor e cálculos salvos no banco (SPEC 6.3, 6.4 e regras 2.4 e 2.5). */
class ProjetoRepositorioTest {

    @TempDir
    Path pasta;

    private BaseDeTeste base;
    private Cadastros.Repositorios repositorios;
    private ProjetoRepositorio projetos;
    private MotorRepositorio motores;
    private CalculoRepositorio calculos;

    @BeforeEach
    void abrir() throws Exception {
        base = BaseDeTeste.criada(pasta);
        repositorios = Cadastros.Repositorios.de(base);
        projetos = repositorios.projetos();
        motores = repositorios.motores();
        calculos = new CalculoRepositorio(base.relogio);
    }

    private long projetoCasoB(String cliente, String placa) {
        return base.banco.transacao(c -> repositorios.projetoCasoB(c, cliente, placa));
    }

    private void mudar(long projeto, StatusProjeto status) {
        base.banco.transacao(c -> {
            projetos.mudarStatus(c, projeto, status);
            return null;
        });
    }

    private StatusProjeto status(long projeto) {
        return base.banco.ler(c -> projetos.buscar(c, projeto)).orElseThrow().status();
    }

    @Test
    void projetoNovoComecaEmRascunhoComAMeta() {
        long id = projetoCasoB("João da Silva", "ABC1D23");
        ProjetoSalvo projeto = base.banco.ler(c -> projetos.buscar(c, id)).orElseThrow();
        assertEquals(StatusProjeto.RASCUNHO, projeto.status());
        assertEquals(CasoBDados.entrada().meta(), projeto.meta());
        assertEquals(List.of(StatusProjeto.RASCUNHO),
                base.banco.ler(c -> projetos.historicoStatus(c, id)).stream().map(MudancaStatus::status).toList());
    }

    /** SPEC 6.4: para a frente um passo de cada vez; arquivar a qualquer momento; reativar volta ao de antes. */
    @Test
    void transicoesDeStatus() {
        long id = projetoCasoB("João da Silva", "ABC1D23");
        assertThrows(OperacaoRecusadaException.class, () -> mudar(id, StatusProjeto.ORCADO), "pular passo");
        mudar(id, StatusProjeto.DIMENSIONADO);
        mudar(id, StatusProjeto.ARQUIVADO);
        assertEquals(StatusProjeto.ARQUIVADO, status(id));
        OperacaoRecusadaException erro =
                assertThrows(OperacaoRecusadaException.class, () -> mudar(id, StatusProjeto.ORCADO));
        assertTrue(erro.getMessage().contains("Dimensionado"), erro.getMessage());

        base.banco.transacao(c -> {
            projetos.reativar(c, id);
            return null;
        });
        assertEquals(StatusProjeto.DIMENSIONADO, status(id));
        assertEquals(List.of(StatusProjeto.RASCUNHO, StatusProjeto.DIMENSIONADO, StatusProjeto.ARQUIVADO,
                StatusProjeto.DIMENSIONADO),
                base.banco.ler(c -> projetos.historicoStatus(c, id)).stream().map(MudancaStatus::status).toList());
    }

    @Test
    void listaComBuscaSemAcentoStatusEArquivados() {
        long gol = projetoCasoB("João da Silva", "ABC1D23");
        long lancha = base.banco.transacao(c -> {
            long cliente = repositorios.clientes().inserir(c, Cadastros.cliente("Maria Antônia"));
            long veiculo = repositorios.veiculos().inserir(c, cliente, Cadastros.lancha("Maré Alta"), null);
            long projeto = projetos.inserir(c, veiculo, CasoBDados.entrada().meta(), "", null);
            motores.salvarVersao(c, projeto, "Mercury 4.5 V6",
                    CasoBDados.entrada().motor().paraConstrutor().aspiracao(Aspiracao.ASPIRADO).construir());
            return projeto;
        });
        mudar(gol, StatusProjeto.DIMENSIONADO);

        assertEquals(List.of(gol, lancha), ids(FiltroProjetos.todos()), "o mexido por último vem primeiro");
        assertEquals(List.of(gol), ids(new FiltroProjetos("joao", null, false)));
        assertEquals(List.of(gol), ids(new FiltroProjetos("abc-1d23", null, false)), "placa sem pontuação");
        assertEquals(List.of(lancha), ids(new FiltroProjetos("MARE alta", null, false)), "nome da embarcação");
        assertEquals(List.of(gol), ids(new FiltroProjetos("ap 2.0", null, false)), "motor");
        assertEquals(List.of(lancha), ids(new FiltroProjetos("antonia aspirado", null, false)));
        assertEquals(List.of(gol), ids(new FiltroProjetos("", StatusProjeto.DIMENSIONADO, false)));

        base.banco.transacao(c -> {
            projetos.arquivar(c, lancha);
            return null;
        });
        assertEquals(List.of(gol), ids(FiltroProjetos.todos()), "arquivado some da lista");
        assertEquals(List.of(lancha, gol), ids(new FiltroProjetos("", null, true)));
        assertEquals(List.of(lancha), ids(new FiltroProjetos("", StatusProjeto.ARQUIVADO, false)));

        ResumoProjeto linha = base.banco.ler(c -> projetos.pesquisar(c, new FiltroProjetos("joao", null, false), 10))
                .getFirst();
        assertEquals("João da Silva", linha.cliente());
        assertEquals("VW Gol GTi", linha.veiculo());
        assertEquals("ABC1D23", linha.identificacao());
        assertEquals("AP 2.0 turbo · 4 cil. · 2,0 L · Turbo", linha.motor());
        assertEquals(1, linha.versaoMotor());
    }

    @Test
    void cadaGravacaoDoMotorEUmaVersaoNovaEAListaMostraAUltima() {
        long id = projetoCasoB("João da Silva", "ABC1D23");
        ConfiguracaoMotor v1 = CasoBDados.entrada().motor();
        ConfiguracaoMotor v2 = v1.paraConstrutor().taxaCompressao(9.0).construir();
        VersaoMotor salva = base.banco.transacao(c -> motores.salvarVersao(c, id, "AP 2.0 turbo forjado", v2));
        assertEquals(2, salva.numero());

        List<VersaoMotor> versoes = base.banco.ler(c -> motores.versoes(c, id));
        assertEquals(List.of(1, 2), versoes.stream().map(VersaoMotor::numero).toList());
        assertEquals(v1, versoes.getFirst().configuracao(), "a v1 continua igual");
        assertEquals(v2, base.banco.ler(c -> motores.atual(c, id)).orElseThrow().configuracao());
        ResumoProjeto linha = base.banco.ler(c -> projetos.pesquisar(c, FiltroProjetos.todos(), 10)).getFirst();
        assertEquals(2, linha.versaoMotor());
        assertTrue(linha.motor().startsWith("AP 2.0 turbo forjado"), linha.motor());
    }

    /** SPEC 6.3: a versão do motor é imutável; o banco recusa UPDATE e DELETE. */
    @Test
    void bancoRecusaAlterarOuApagarVersaoDoMotor() {
        long id = projetoCasoB("João da Silva", "ABC1D23");
        ErroDados alterar = assertThrows(ErroDados.class, () -> executar("UPDATE motor_versao SET descricao = 'x'"));
        assertTrue(alterar.getMessage().contains("Versão do motor não muda"), alterar.getMessage());
        ErroDados apagar = assertThrows(ErroDados.class, () -> executar("DELETE FROM motor_versao"));
        assertTrue(apagar.getMessage().contains("não é apagada"), apagar.getMessage());
        assertEquals("AP 2.0 turbo", base.banco.ler(c -> motores.atual(c, id)).orElseThrow().descricao());
    }

    /** Regras 2.4 e 2.5: o cálculo salvo volta inteiro, com as justificativas, e não muda depois. */
    @Test
    void calculoSalvoVoltaInteiroENaoMuda() {
        long id = projetoCasoB("João da Silva", "ABC1D23");
        VersaoMotor v1 = base.banco.ler(c -> motores.atual(c, id)).orElseThrow();
        EntradaDimensionamento entrada = CasoBDados.entrada();
        Resultado<Dimensionamento> resultado = CalculoDimensionamento.calcular(entrada);
        Justificativa justificativa = new Justificativa(Indicador.DC_BICO, "Bico maior encomendado; usar este até "
                + "chegar.", LocalDateTime.of(2026, 10, 11, 18, 0));
        long calculo = base.banco.transacao(c -> calculos.salvar(c, id, v1.id(), entrada, resultado,
                List.of(justificativa), List.of(), BaseDeTeste.VERSAO_APP));

        CalculoSalvo salvo = base.banco.ler(c -> calculos.buscar(c, calculo)).orElseThrow();
        assertEquals(entrada, salvo.entrada());
        assertEquals(resultado, salvo.resultado());
        assertEquals(List.of(justificativa), salvo.justificativas());
        assertEquals(1, salvo.numeroVersaoMotor());
        assertEquals(VersaoNucleo.ATUAL, salvo.versaoNucleo());
        assertEquals(calculo, base.banco.ler(c -> calculos.ultimo(c, id)).orElseThrow().id());

        ResumoCalculo resumo = base.banco.ler(c -> calculos.doProjeto(c, id)).getFirst();
        assertEquals(1, resumo.vermelhos(), "o DC de 86% do caso B é vermelho");
        assertEquals(v1.id(), resumo.versaoMotorId());

        assertThrows(ErroDados.class, () -> executar("UPDATE calculo SET versao_app = 'x'"));
        assertThrows(ErroDados.class, () -> executar("DELETE FROM calculo"));
        assertThrows(ErroDados.class, () -> executar("UPDATE calculo_justificativa SET texto = 'outro'"));
    }

    @Test
    void projetoSemCalculo() {
        long id = projetoCasoB("João da Silva", "ABC1D23");
        assertTrue(base.banco.ler(c -> calculos.ultimo(c, id)).isEmpty());
        assertTrue(base.banco.ler(c -> calculos.doProjeto(c, id)).isEmpty());
    }

    private void executar(String sql) {
        base.banco.transacao(c -> {
            try (PreparedStatement comando = c.prepareStatement(sql)) {
                return comando.executeUpdate();
            }
        });
    }

    private List<Long> ids(FiltroProjetos filtro) {
        return base.banco.ler(c -> projetos.pesquisar(c, filtro, 50)).stream().map(ResumoProjeto::projetoId).toList();
    }
}
