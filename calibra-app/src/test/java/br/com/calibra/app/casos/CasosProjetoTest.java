package br.com.calibra.app.casos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.app.LeitorFormulario;
import br.com.calibra.app.ValoresDeTeste;
import br.com.calibra.app.casos.CasosProjeto.ClienteExistente;
import br.com.calibra.app.casos.CasosProjeto.ClienteNovo;
import br.com.calibra.app.casos.CasosProjeto.NovoProjeto;
import br.com.calibra.app.casos.CasosProjeto.ProjetoAberto;
import br.com.calibra.app.casos.CasosProjeto.SituacaoCalculo;
import br.com.calibra.app.casos.CasosProjeto.VeiculoExistente;
import br.com.calibra.app.casos.CasosProjeto.VeiculoNovo;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.cadastro.Cliente;
import br.com.calibra.core.cadastro.Consentimento;
import br.com.calibra.core.cadastro.TipoCambio;
import br.com.calibra.core.cadastro.TipoVeiculo;
import br.com.calibra.core.cadastro.Tracao;
import br.com.calibra.core.cadastro.Veiculo;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.motor.ModoInjecao;
import br.com.calibra.core.motor.RodaFonica;
import br.com.calibra.core.projeto.Meta;
import br.com.calibra.core.projeto.StatusProjeto;
import br.com.calibra.core.projeto.Uso;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.semaforo.Justificativa;
import br.com.calibra.dados.BancoDados;
import br.com.calibra.dados.Migracoes;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.backup.Backup;
import br.com.calibra.dados.cadastro.EventoAuditoria;
import br.com.calibra.dados.projeto.CalculoSalvo;
import br.com.calibra.dados.projeto.FiltroProjetos;
import br.com.calibra.dados.projeto.VersaoMotor;

/** Casos de uso do projeto, com o banco de verdade numa pasta temporária. Dados fictícios. */
class CasosProjetoTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 12);

    @TempDir
    Path raiz;

    private final RelogioAjustavel relogio = new RelogioAjustavel(LocalDateTime.of(2026, 10, 12, 9, 0));
    private CasosProjeto casos;
    private BancoDados banco;
    private PastasDados pastas;

    @BeforeEach
    void abrirBanco() throws IOException {
        pastas = new PastasDados(raiz);
        pastas.criar();
        banco = new BancoDados(pastas.banco());
        Migracoes.migrar(banco, new Backup(pastas, banco, "0.2.0-teste", relogio));
        casos = new CasosProjeto(banco, relogio, "0.2.0-teste");
    }

    static Cliente cliente(String nome) {
        return Cliente.novo(nome, "(11) 98765-4321", "", "", new Consentimento(HOJE, Consentimento.FINALIDADE_PADRAO),
                "");
    }

    static Veiculo carro(String placa) {
        return new Veiculo(TipoVeiculo.CARRO, "VW", "Gol", "GTi", 1994, 1994, placa, "", "", "", "", 250_000.0,
                TipoCambio.MANUAL, false, Tracao.DIANTEIRA, null, "Branco", "");
    }

    /** O caso golden B como digitado na calculadora. */
    static EntradaDimensionamento casoB() {
        return LeitorFormulario.ler(ValoresDeTeste.casoB(), Parametros.padrao()).entrada();
    }

    private long novoProjeto(String nomeCliente, String placa) {
        EntradaDimensionamento entrada = casoB();
        return casos.criarProjeto(new NovoProjeto(new ClienteNovo(cliente(nomeCliente)), new VeiculoNovo(carro(placa)),
                "AP 2.0 turbo", entrada.motor(), entrada.meta()));
    }

    private static Justificativa justificativaDoDc() {
        return new Justificativa(Indicador.DC_BICO, "Bico maior encomendado; este é provisório.",
                LocalDateTime.of(2026, 10, 12, 9, 30));
    }

    @Test
    void assistenteCriaClienteVeiculoProjetoEMotorV1() {
        long id = novoProjeto("João da Silva", "ABC1D23");
        ProjetoAberto aberto = casos.abrir(id);
        assertEquals(StatusProjeto.RASCUNHO, aberto.projeto().status());
        assertEquals("João da Silva", aberto.cliente().cliente().nome());
        assertEquals("ABC1D23", aberto.veiculo().veiculo().placa());
        assertEquals(1, aberto.motor().numero());
        assertEquals("AP 2.0 turbo", aberto.motor().descricao());
        assertEquals(casoB().motor(), aberto.motor().configuracao());
        assertEquals(casoB().meta(), aberto.projeto().meta());
        assertEquals(1, casos.pesquisar(FiltroProjetos.todos()).size());
    }

    @Test
    void assistenteJuntaTodosOsErrosENaoGravaNada() {
        EntradaDimensionamento entrada = casoB();
        Cliente semTelefone = Cliente.novo("João", "", "", "", new Consentimento(HOJE, "Orçamento"), "");
        Veiculo semMarca = new Veiculo(TipoVeiculo.CARRO, "", "Gol", "", null, null, "", "", "", "", "", null, null,
                false, null, null, "", "");
        ConfiguracaoMotor trezeCilindros = entrada.motor().paraConstrutor().cilindros(13).construir();
        Meta altaDemais = new Meta(entrada.meta().potenciaKw(), entrada.meta().pontoMedicao(), null, 8000, null,
                Uso.RUA);

        EntradaInvalidaException erro = assertThrows(EntradaInvalidaException.class, () -> casos.criarProjeto(
                new NovoProjeto(new ClienteNovo(semTelefone), new VeiculoNovo(semMarca), "", trezeCilindros,
                        altaDemais)));
        List<String> erros = erro.erros();
        assertTrue(erros.contains("Informe o telefone do cliente."), erros::toString);
        assertTrue(erros.contains("Informe a marca do veículo."), erros::toString);
        assertTrue(erros.stream().anyMatch(e -> e.contains("3 a 12 cilindros")), erros::toString);
        assertTrue(erros.stream().anyMatch(e -> e.contains("passa da rotação máxima")), erros::toString);
        assertTrue(casos.pesquisar(new FiltroProjetos("", null, true)).isEmpty(), "nada gravado");
        assertTrue(casos.pesquisarClientes("").isEmpty(), "nem o cliente");
    }

    @Test
    void segundoProjetoNoMesmoVeiculoEVeiculoDeOutroClienteRecusado() {
        long primeiro = novoProjeto("João da Silva", "ABC1D23");
        ProjetoAberto aberto = casos.abrir(primeiro);
        long joao = aberto.cliente().id();
        long gol = aberto.veiculo().id();
        EntradaDimensionamento entrada = casoB();

        long segundo = casos.criarProjeto(new NovoProjeto(new ClienteExistente(joao), new VeiculoExistente(gol),
                "AP 2.0 turbo, fase 2", entrada.motor(), entrada.meta()));
        assertEquals(gol, casos.abrir(segundo).veiculo().id());
        assertEquals(2, casos.pesquisar(FiltroProjetos.todos()).size());

        long maria = casos.abrir(novoProjeto("Maria Souza", "XYZ9A87")).cliente().id();
        OperacaoRecusadaException outroDono = assertThrows(OperacaoRecusadaException.class, () -> casos.criarProjeto(
                new NovoProjeto(new ClienteExistente(maria), new VeiculoExistente(gol), "", entrada.motor(),
                        entrada.meta())));
        assertTrue(outroDono.getMessage().contains("outro cliente"), outroDono.getMessage());

        EntradaInvalidaException clienteNovo = assertThrows(EntradaInvalidaException.class, () -> casos.criarProjeto(
                new NovoProjeto(new ClienteNovo(cliente("Pedro")), new VeiculoExistente(gol), "", entrada.motor(),
                        entrada.meta())));
        assertEquals(List.of("Cliente novo precisa de veículo novo."), clienteNovo.erros());
    }

    /** SPEC 6.3, teste da fase 2: editar o motor cria versão nova e marca os resultados antigos como desatualizados. */
    @Test
    void editarMotorCriaVersaoNovaEDeixaOsCalculosAntigosDesatualizados() {
        long id = novoProjeto("João da Silva", "ABC1D23");
        VersaoMotor v1 = casos.abrir(id).motor();
        EntradaDimensionamento entradaV1 = casoB().comMotor(v1.configuracao());
        long calculoV1 = casos.salvarCalculo(id, entradaV1, List.of(justificativaDoDc()));
        assertEquals(StatusProjeto.DIMENSIONADO, casos.abrir(id).projeto().status(), "salvar cálculo dimensiona");
        assertFalse(casos.calculos(id).getFirst().desatualizado());

        relogio.andar(Duration.ofMinutes(5));
        ConfiguracaoMotor comTaxaMenor = v1.configuracao().paraConstrutor().taxaCompressao(9.0).construir();
        VersaoMotor v2 = casos.salvarMotor(id, "AP 2.0 turbo", comTaxaMenor).orElseThrow();
        assertEquals(2, v2.numero());

        SituacaoCalculo antigo = casos.calculos(id).getFirst();
        assertTrue(antigo.desatualizado());
        assertEquals(List.of("o motor mudou: este cálculo usou a v1 e o motor está na v2"), antigo.motivos());

        // Regra 2.5: o cálculo antigo continua como foi salvo, preso à v1.
        CalculoSalvo salvo = casos.calculo(calculoV1).orElseThrow();
        assertEquals(1, salvo.numeroVersaoMotor());
        assertEquals(entradaV1, salvo.entrada());

        assertTrue(casos.salvarMotor(id, "AP 2.0 turbo", comTaxaMenor).isEmpty(), "nada mudou: sem v3");
        assertEquals(2, casos.versoesMotor(id).size());

        EntradaInvalidaException comMotorVelho = assertThrows(EntradaInvalidaException.class,
                () -> casos.salvarCalculo(id, entradaV1, List.of(justificativaDoDc())));
        assertTrue(comMotorVelho.getMessage().contains("mudou"), comMotorVelho.getMessage());

        casos.salvarCalculo(id, casoB().comMotor(v2.configuracao()), List.of(justificativaDoDc()));
        List<SituacaoCalculo> situacoes = casos.calculos(id);
        assertEquals(2, situacoes.size());
        assertFalse(situacoes.get(0).desatualizado(), "o novo, com a v2");
        assertTrue(situacoes.get(1).desatualizado(), "o antigo, com a v1");
    }

    /** Regra 2.4: vermelho só avança com justificativa escrita. */
    @Test
    void calculoComVermelhoSoSalvaComJustificativa() {
        long id = novoProjeto("João da Silva", "ABC1D23");
        EntradaDimensionamento entrada = casoB().comMotor(casos.abrir(id).motor().configuracao());
        EntradaInvalidaException erro = assertThrows(EntradaInvalidaException.class,
                () -> casos.salvarCalculo(id, entrada, List.of()));
        assertTrue(erro.getMessage().contains("DC do bico na potência máxima"), erro.getMessage());
        assertTrue(casos.calculos(id).isEmpty());
        assertEquals(StatusProjeto.RASCUNHO, casos.abrir(id).projeto().status());

        Justificativa sobra = new Justificativa(Indicador.VELOCIDADE_PISTAO, "Não é vermelho.",
                LocalDateTime.of(2026, 10, 12, 9, 31));
        long calculo = casos.salvarCalculo(id, entrada, List.of(justificativaDoDc(), sobra));
        assertEquals(List.of(justificativaDoDc()), casos.calculo(calculo).orElseThrow().justificativas(),
                "só a do vermelho é gravada");
    }

    @Test
    void motorQueNaoPassaNasRegrasNaoViraVersao() {
        long id = novoProjeto("João da Silva", "ABC1D23");
        ConfiguracaoMotor semFase = casos.abrir(id).motor().configuracao().paraConstrutor()
                .rodaFonica(new RodaFonica(60, 2, false)).modoInjecao(ModoInjecao.SEQUENCIAL).construir();
        assertThrows(EntradaInvalidaException.class, () -> casos.salvarMotor(id, "", semFase));
        assertEquals(1, casos.versoesMotor(id).size());
    }

    @Test
    void trocarCarroPorLanchaGuardaSoOsCamposDoTipoEOHistorico() {
        long id = novoProjeto("João da Silva", "ABC1D23");
        long veiculo = casos.abrir(id).veiculo().id();
        Veiculo gol = casos.abrir(id).veiculo().veiculo();
        Veiculo lancha = new Veiculo(TipoVeiculo.LANCHA, "Focker", "240", "", 2019, 2020, gol.placa(), gol.chassi(),
                "Maré Alta", "", "", 120.0, TipoCambio.NAUTICO_RABETA_OU_REVERSOR, false, gol.tracao(), null, "", "");
        casos.salvarVeiculo(veiculo, lancha);

        Veiculo salvo = casos.abrir(id).veiculo().veiculo();
        assertEquals("", salvo.placa());
        assertEquals(null, salvo.tracao());
        assertEquals("Maré Alta", salvo.identificacao());
        List<EventoAuditoria> historico = casos.historicoVeiculo(veiculo);
        assertTrue(historico.contains(new EventoAuditoria("Tipo", "Carro", "Lancha", historico.getFirst().dataHora())),
                historico::toString);
        assertTrue(historico.stream().anyMatch(e -> e.campo().equals("Placa") && "ABC1D23".equals(e.valorAntigo())));
    }

    @Test
    void anonimizarClienteMantemOProjeto() {
        long id = novoProjeto("João da Silva", "ABC1D23");
        new CasosOficina(banco, relogio, casos, pastas.anexos()).anonimizarCliente(casos.abrir(id).cliente().id());
        ProjetoAberto aberto = casos.abrir(id);
        assertTrue(aberto.cliente().cliente().nome().startsWith("Cliente anonimizado #"));
        assertEquals("", aberto.veiculo().veiculo().placa());
        assertEquals(1, aberto.motor().numero(), "motor e projeto ficam");
        assertTrue(casos.pesquisar(new FiltroProjetos("joao", null, false)).isEmpty());
        assertEquals(1, casos.pesquisar(FiltroProjetos.todos()).size());
        assertThrows(OperacaoRecusadaException.class, () -> casos.criarProjeto(new NovoProjeto(
                new ClienteExistente(aberto.cliente().id()), new VeiculoExistente(aberto.veiculo().id()), "",
                aberto.motor().configuracao(), aberto.projeto().meta())));

        // Revisão pelo agente: a placa não volta pela edição do veículo; sem identificação, a edição grava.
        long veiculo = aberto.veiculo().id();
        OperacaoRecusadaException recusa = assertThrows(OperacaoRecusadaException.class,
                () -> casos.salvarVeiculo(veiculo, carro("ABC1D23")));
        assertTrue(recusa.getMessage().startsWith("O dono deste veículo foi anonimizado"), recusa.getMessage());
        assertEquals("", casos.abrir(id).veiculo().veiculo().placa(), "nada gravado");
        casos.salvarVeiculo(veiculo, new Veiculo(TipoVeiculo.CARRO, "VW", "Gol", "GTi", 1994, 1995, "", "", "", "",
                "", 250_000.0, TipoCambio.MANUAL, false, Tracao.DIANTEIRA, null, "Branco", ""));
        assertEquals(1995, casos.abrir(id).veiculo().veiculo().anoModelo(), "os outros campos continuam editáveis");
    }

    @Test
    void arquivarEReativar() {
        long id = novoProjeto("João da Silva", "ABC1D23");
        casos.arquivar(id);
        assertTrue(casos.pesquisar(FiltroProjetos.todos()).isEmpty());
        assertEquals(1, casos.pesquisar(new FiltroProjetos("", null, true)).size());
        casos.reativar(id);
        assertEquals(StatusProjeto.RASCUNHO, casos.abrir(id).projeto().status());
        assertEquals(Optional.of(StatusProjeto.RASCUNHO),
                casos.historicoStatus(id).stream().map(m -> m.status()).reduce((a, b) -> b));
    }
}
