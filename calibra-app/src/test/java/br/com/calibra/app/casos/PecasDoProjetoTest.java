package br.com.calibra.app.casos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.app.casos.CasosProjeto.ClienteNovo;
import br.com.calibra.app.casos.CasosProjeto.NovoProjeto;
import br.com.calibra.app.casos.CasosProjeto.VeiculoNovo;
import br.com.calibra.app.catalogo.PecasEscolhidas;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.cadastro.TipoCambio;
import br.com.calibra.core.cadastro.Veiculo;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.combustivel.Combustivel;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.Cambio;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.pecas.TempoMorto;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.semaforo.Justificativa;
import br.com.calibra.dados.BancoDados;
import br.com.calibra.dados.Migracoes;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.backup.Backup;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;
import br.com.calibra.dados.projeto.CalculoSalvo;
import br.com.calibra.dados.projeto.PapelPeca;
import br.com.calibra.dados.projeto.PecaUsada;

/**
 * Peças do catálogo no projeto (bloco 2.3): o cálculo grava as peças e o combustível com a versão, peça com
 * versão nova deixa o cálculo desatualizado, peça arquivada continua no cálculo antigo, e o câmbio e a injeção do
 * catálogo. Peças fictícias; o bico é o do caso golden B.
 */
class PecasDoProjetoTest {

    private static final String FONTE = "Ficha fictícia de teste";

    @TempDir
    Path raiz;

    private final RelogioAjustavel relogio = new RelogioAjustavel(LocalDateTime.of(2026, 10, 12, 9, 0));
    private CasosProjeto casos;
    private CasosCatalogo catalogo;

    @BeforeEach
    void abrirBanco() throws IOException {
        PastasDados pastas = new PastasDados(raiz);
        pastas.criar();
        BancoDados banco = new BancoDados(pastas.banco());
        Migracoes.migrar(banco, new Backup(pastas, banco, "0.2.0-teste", relogio));
        casos = new CasosProjeto(banco, relogio, "0.2.0-teste");
        catalogo = new CasosCatalogo(banco, relogio);
    }

    private long novoProjeto() {
        EntradaDimensionamento entrada = CasosProjetoTest.casoB();
        return casos.criarProjeto(new NovoProjeto(new ClienteNovo(CasosProjetoTest.cliente("João da Silva")),
                new VeiculoNovo(CasosProjetoTest.carro("ABC1D23")), "AP 2.0 turbo", entrada.motor(), entrada.meta()));
    }

    /** O bico do caso B (1.050 cm³/min a 3 bar, 1,0 ms) cadastrado no catálogo. */
    private ItemCatalogo<Bico> bicoDoCasoB() {
        Bico bico = new Bico("Bico teste 1050", 1050.0, 300.0, 0.70, TempoMorto.fixo(1.0), null, FONTE, null);
        return catalogo.criar(TipoCatalogo.BICO, bico, new InfoCatalogo("Bico teste 1050", FONTE, null, false, null, ""));
    }

    private static PecasEscolhidas soOBico(ItemCatalogo<Bico> bico) {
        return new PecasEscolhidas(bico, null, null, null, null, null);
    }

    private static List<Justificativa> justificativas() {
        return List.of(new Justificativa(Indicador.DC_BICO, "Bico maior encomendado; este é provisório.",
                LocalDateTime.of(2026, 10, 12, 9, 30)));
    }

    /** O cálculo do projeto com o bico do catálogo no lugar do digitado. */
    private EntradaDimensionamento entradaCom(ItemCatalogo<Bico> bico, long projetoId) {
        return CasosProjetoTest.casoB().comMotor(casos.abrir(projetoId).motor().configuracao()).comBico(bico.dados());
    }

    @Test
    void calculoGravaAsPecasDoCatalogoEOCombustivelComAVersao() {
        long projeto = novoProjeto();
        ItemCatalogo<Bico> bico = bicoDoCasoB();
        long id = casos.salvarCalculo(projeto, entradaCom(bico, projeto), justificativas(), soOBico(bico));

        CalculoSalvo salvo = casos.calculo(id).orElseThrow();
        long etanol = catalogo.combustivel("EHC").orElseThrow().id();
        assertEquals(List.of(new PecaUsada(PapelPeca.BICO, bico.id(), 1), new PecaUsada(PapelPeca.COMBUSTIVEL, etanol, 1)),
                salvo.pecas());
        assertFalse(casos.calculos(projeto).getFirst().desatualizado());
    }

    @Test
    void pecaComVersaoNovaNoCatalogoDeixaOCalculoDesatualizado() {
        long projeto = novoProjeto();
        ItemCatalogo<Bico> bico = bicoDoCasoB();
        casos.salvarCalculo(projeto, entradaCom(bico, projeto), justificativas(), soOBico(bico));

        relogio.andar(Duration.ofMinutes(5));
        Bico tabelaNova = new Bico(bico.dados().nome(), 1050.0, 300.0, 0.70, TempoMorto.fixo(0.95), null, FONTE, null);
        catalogo.salvar(TipoCatalogo.BICO, bico.id(), tabelaNova, bico.info());

        CasosProjeto.SituacaoCalculo situacao = casos.calculos(projeto).getFirst();
        assertTrue(situacao.desatualizado());
        assertEquals(List.of("o bico mudou no catálogo: Bico teste 1050 v1 → v2"), situacao.motivos());
    }

    /** O combustível vem com o motor: a versão nova só entra quando o motor é gravado de novo. */
    @Test
    void combustivelComVersaoNovaPedeGravarOMotorDeNovo() {
        long projeto = novoProjeto();
        casos.salvarCalculo(projeto, CasosProjetoTest.casoB().comMotor(casos.abrir(projeto).motor().configuracao()),
                justificativas());
        ItemCatalogo<Combustivel> ehc = catalogo.combustivel("EHC").orElseThrow();
        Combustivel atual = ehc.dados();
        Combustivel maisDenso = new Combustivel(atual.codigo(), atual.nome(), atual.composicao(), 0.810,
                atual.lambdaSobPressao(), atual.lambdaAspiradoPlenaCarga(), atual.avisoLambda(), atual.vigenteDesde(),
                atual.fonte(), 1);
        Combustivel v2 = catalogo.salvar(TipoCatalogo.COMBUSTIVEL, ehc.id(), maisDenso, ehc.info()).item().dados();

        assertEquals(List.of("o combustível mudou no catálogo: Etanol hidratado (93% em massa) v1 → v2 (grave o motor "
                + "de novo, na aba Motor, para usar a versão nova)"), casos.calculos(projeto).getFirst().motivos());

        ConfiguracaoMotor comV2 = casos.abrir(projeto).motor().configuracao().paraConstrutor().combustivel(v2).construir();
        casos.salvarMotor(projeto, "AP 2.0 turbo", comV2).orElseThrow();
        assertEquals(List.of("o motor mudou: este cálculo usou a v1 e o motor está na v2",
                "o combustível mudou no catálogo: Etanol hidratado (93% em massa) v1 → v2"),
                casos.calculos(projeto).getFirst().motivos());
    }

    @Test
    void pecaArquivadaContinuaNoCalculoAntigo() {
        long projeto = novoProjeto();
        ItemCatalogo<Bico> bico = bicoDoCasoB();
        EntradaDimensionamento entrada = entradaCom(bico, projeto);
        long id = casos.salvarCalculo(projeto, entrada, justificativas(), soOBico(bico));

        catalogo.arquivar(bico.id(), true);
        assertTrue(catalogo.ativos(TipoCatalogo.BICO).isEmpty(), "sai das escolhas");
        CalculoSalvo salvo = casos.calculo(id).orElseThrow();
        assertEquals(entrada.bico(), salvo.entrada().bico());
        assertTrue(salvo.pecas().contains(new PecaUsada(PapelPeca.BICO, bico.id(), 1)));
        assertFalse(casos.calculos(projeto).getFirst().desatualizado(), "arquivar não muda a peça");
    }

    @Test
    void pecaQueNaoEADaEntradaERecusada() {
        long projeto = novoProjeto();
        ItemCatalogo<Bico> bico = bicoDoCasoB();
        EntradaDimensionamento digitada = CasosProjetoTest.casoB().comMotor(casos.abrir(projeto).motor().configuracao());
        EntradaInvalidaException erro = assertThrows(EntradaInvalidaException.class,
                () -> casos.salvarCalculo(projeto, digitada, justificativas(), soOBico(bico)));
        assertEquals(List.of("A peça do catálogo (bico) não é a da entrada do cálculo: calcule de novo antes de salvar."),
                erro.erros());
    }

    @Test
    void cambioDoCatalogoNoVeiculoPrecisaSerDoMesmoTipo() {
        ItemCatalogo<Cambio> cambio = catalogo.criar(TipoCatalogo.CAMBIO, new Cambio("Câmbio teste 5M", TipoCambio.MANUAL,
                350.0, FONTE, null), new InfoCatalogo("Câmbio teste 5M", FONTE, null, false, null, ""));
        EntradaDimensionamento entrada = CasosProjetoTest.casoB();
        Veiculo carro = CasosProjetoTest.carro("ABC1D23");
        long projeto = casos.criarProjeto(new NovoProjeto(new ClienteNovo(CasosProjetoTest.cliente("João da Silva")),
                new VeiculoNovo(carro, cambio.id()), "AP 2.0 turbo", entrada.motor(), entrada.meta()));
        long veiculo = casos.abrir(projeto).veiculo().id();
        assertEquals(cambio.id(), casos.abrir(projeto).veiculo().cambioItemId());

        Veiculo automatico = new Veiculo(carro.tipo(), carro.marca(), carro.modelo(), carro.versao(),
                carro.anoFabricacao(), carro.anoModelo(), carro.placa(), carro.chassi(), carro.nomeEmbarcacao(),
                carro.numeroInscricao(), carro.numeroCasco(), carro.usoAcumulado(), TipoCambio.AUTOMATICO, false,
                carro.tracao(), carro.pesoKg(), carro.cor(), carro.observacoes());
        EntradaInvalidaException erro = assertThrows(EntradaInvalidaException.class,
                () -> casos.salvarVeiculo(veiculo, automatico, cambio.id()));
        assertEquals("O câmbio Câmbio teste 5M é Manual, mas o veículo está com câmbio Automático.", erro.getMessage());

        casos.salvarVeiculo(veiculo, automatico, null);
        assertNull(casos.abrir(projeto).veiculo().cambioItemId());
        assertTrue(casos.historicoVeiculo(veiculo).stream().anyMatch(e -> e.campo().equals("Câmbio do catálogo")),
                "a troca vai para o histórico");
    }

    @Test
    void injecaoDoCatalogoNoProjeto() {
        Injecao dados = new Injecao("Marca Teste", "ECU 8", 8, 8, 8, Set.of(), FONTE, LocalDate.of(2026, 10, 1));
        ItemCatalogo<Injecao> injecao = catalogo.criar(TipoCatalogo.INJECAO, dados,
                new InfoCatalogo(dados.nome(), FONTE, null, false, null, ""));
        EntradaDimensionamento entrada = CasosProjetoTest.casoB();
        long projeto = casos.criarProjeto(new NovoProjeto(new ClienteNovo(CasosProjetoTest.cliente("João da Silva")),
                new VeiculoNovo(CasosProjetoTest.carro("ABC1D23")), "AP 2.0 turbo", entrada.motor(), entrada.meta(),
                injecao.id()));
        assertEquals(injecao.id(), casos.abrir(projeto).projeto().injecaoItemId());

        casos.trocarInjecao(projeto, null);
        assertNull(casos.abrir(projeto).projeto().injecaoItemId());
        assertThrows(OperacaoRecusadaException.class, () -> casos.trocarInjecao(projeto, 999L));
    }
}
