package br.com.calibra.app.casos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.app.casos.CasosMapaBase.Escolhas;
import br.com.calibra.app.casos.CasosMapaBase.SituacaoMapa;
import br.com.calibra.app.casos.CasosProjeto.ClienteNovo;
import br.com.calibra.app.casos.CasosProjeto.NovoProjeto;
import br.com.calibra.app.casos.CasosProjeto.VeiculoNovo;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.mapa.AlertaMapa;
import br.com.calibra.core.mapa.JustificativaMapa;
import br.com.calibra.core.mapa.MapaBase;
import br.com.calibra.core.mapa.ModeloVe;
import br.com.calibra.core.mapa.RegrasMapaBase;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.pecas.ModeloCombustivel;
import br.com.calibra.core.pecas.PerfilInjecao;
import br.com.calibra.core.projeto.StatusProjeto;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.semaforo.Justificativa;
import br.com.calibra.dados.BancoDados;
import br.com.calibra.dados.Migracoes;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.backup.Backup;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;
import br.com.calibra.dados.projeto.MapaBaseSalvo;
import br.com.calibra.dados.projeto.PapelPeca;
import br.com.calibra.dados.projeto.ProjetoRepositorio;

/**
 * O mapa base do projeto (fase 4, bloco 4.4; decisão 296): gerar com o último cálculo e as peças do catálogo, salvar
 * em versões com as justificativas dos vermelhos, o status MAPA_BASE e o "desatualizado". Peças fictícias.
 */
class CasosMapaBaseTest {

    private static final String FONTE = "Manual fictício de teste";
    private static final LocalDateTime HORA = LocalDateTime.of(2026, 10, 22, 9, 0);

    @TempDir
    Path raiz;

    private final RelogioAjustavel relogio = new RelogioAjustavel(HORA);
    private BancoDados banco;
    private CasosProjeto casos;
    private CasosCatalogo catalogo;
    private CasosMapaBase mapas;
    private long projeto;
    private long calculo;

    @BeforeEach
    void abrirBanco() throws IOException {
        PastasDados pastas = new PastasDados(raiz);
        pastas.criar();
        banco = new BancoDados(pastas.banco());
        Migracoes.migrar(banco, new Backup(pastas, banco, "0.4.0-teste", relogio));
        casos = new CasosProjeto(banco, relogio, "0.4.0-teste");
        catalogo = new CasosCatalogo(banco, relogio);
        mapas = new CasosMapaBase(banco, relogio, "0.4.0-teste");
        EntradaDimensionamento entrada = casoBComTorque();
        projeto = casos.criarProjeto(new NovoProjeto(new ClienteNovo(CasosProjetoTest.cliente("João da Silva")),
                new VeiculoNovo(CasosProjetoTest.carro("ABC1D23")), "AP 2.0 turbo", entrada.motor(), entrada.meta()));
        calculo = salvarCalculo();
    }

    private static EntradaDimensionamento casoBComTorque() {
        EntradaDimensionamento caso = CasosProjetoTest.casoB();
        return caso.comMeta(caso.meta().comRotacaoTorque(5000));
    }

    private long salvarCalculo() {
        EntradaDimensionamento entrada = casoBComTorque().comMotor(casos.abrir(projeto).motor().configuracao());
        return casos.salvarCalculo(projeto, entrada, List.of(new Justificativa(Indicador.DC_BICO, "Bico provisório.",
                HORA)));
    }

    private ItemCatalogo<ModeloVe> modeloSugerido() {
        return mapas.modeloVeSugerido(casos.abrir(projeto).motor().configuracao()).orElseThrow();
    }

    private ItemCatalogo<Injecao> injecaoComPerfil() {
        PerfilInjecao perfil = new PerfilInjecao(null, null, ModeloCombustivel.MS, List.of(), 25.0, true, "", "");
        Injecao injecao = new Injecao("Marca Teste", "ECU 8", 8, 8, null, Set.of(), perfil, FONTE, null);
        return catalogo.criar(TipoCatalogo.INJECAO, injecao,
                new InfoCatalogo(injecao.nome(), FONTE, null, false, null, ""));
    }

    private static List<JustificativaMapa> justificativas() {
        return List.of(new JustificativaMapa(AlertaMapa.Codigo.IGNICAO_SEM_FONTE, "Ignição pelo assistente da injeção.",
                HORA), new JustificativaMapa(AlertaMapa.Codigo.DC_ALTO, "Bico maior encomendado.", HORA));
    }

    private StatusProjeto status() {
        return casos.abrir(projeto).projeto().status();
    }

    @Test
    void sugereOModeloDaFamiliaDoMotor() {
        ItemCatalogo<ModeloVe> sugerido = modeloSugerido();
        assertEquals("modelo-ve:4v-normal", sugerido.chaveSemente(), "4 válvulas e comando original");
    }

    @Test
    void geraSalvaEmVersoesComAsJustificativasEOStatus() {
        ItemCatalogo<Injecao> injecao = injecaoComPerfil();
        Escolhas escolhas = new Escolhas(injecao.id(), modeloSugerido().id(), null, null, 50.0);
        RegrasMapaBase regras = RegrasMapaBase.padrao();
        Resultado<MapaBase> gerado = mapas.gerar(projeto, calculo, escolhas, regras);
        assertTrue(gerado.valor().vermelhos().stream().anyMatch(a -> a.codigo() == AlertaMapa.Codigo.IGNICAO_SEM_FONTE));
        assertEquals(50.0, gerado.valor().travas().pressaoAlvoKpa(), 1e-9, "a mola de 0,5 bar");

        EntradaInvalidaException semJustificar = assertThrows(EntradaInvalidaException.class,
                () -> mapas.salvar(projeto, calculo, escolhas, regras, List.of()));
        assertEquals("Para salvar, escreva a justificativa de: DC do bico no mapa, Ignição sem modelo (regra 2.4: "
                + "vermelho só avança com justificativa).", semJustificar.getMessage());

        long primeiro = mapas.salvar(projeto, calculo, escolhas, regras, justificativas());
        assertEquals(StatusProjeto.DIMENSIONADO, status(), "antes da aprovação, o status fica");
        MapaBaseSalvo salvo = mapas.mapa(primeiro).orElseThrow();
        assertEquals(1, salvo.numero());
        assertEquals(calculo, salvo.calculoId());
        assertEquals(gerado.valor(), salvo.resultado().valor(), "as tabelas voltam iguais do banco (células vazias)");
        assertEquals(2, salvo.justificativas().size());
        assertEquals(List.of(PapelPeca.INJECAO, PapelPeca.MODELO_VE),
                salvo.pecas().stream().map(p -> p.papel()).toList());

        banco.transacao(c -> {
            ProjetoRepositorio repositorio = new ProjetoRepositorio(relogio);
            repositorio.mudarStatus(c, projeto, StatusProjeto.ORCADO);
            repositorio.mudarStatus(c, projeto, StatusProjeto.APROVADO);
            return null;
        });
        long segundo = mapas.salvar(projeto, calculo, escolhas, regras, justificativas());
        assertEquals(StatusProjeto.MAPA_BASE, status(), "aprovado: salvar o mapa leva o projeto a Mapa base");
        assertEquals(2, mapas.mapa(segundo).orElseThrow().numero());

        List<SituacaoMapa> situacoes = mapas.mapas(projeto, regras);
        assertEquals(List.of(2, 1), situacoes.stream().map(s -> s.mapa().numero()).toList(), "o mais novo primeiro");
        assertFalse(situacoes.getFirst().desatualizado(), situacoes.getFirst().motivos()::toString);
    }

    @Test
    void mapaFicaDesatualizadoComMotorCalculoPecasERegras() {
        ItemCatalogo<Injecao> injecao = injecaoComPerfil();
        ItemCatalogo<ModeloVe> modelo = modeloSugerido();
        Escolhas escolhas = new Escolhas(injecao.id(), modelo.id(), null, null, null);
        mapas.salvar(projeto, calculo, escolhas, RegrasMapaBase.padrao(), justificativas());

        RegrasMapaBase outras = RegrasMapaBase.padrao().comReducoes(1.0, null, null);
        assertEquals(List.of("as regras do mapa base mudaram em Configurações"),
                mapas.mapas(projeto, outras).getFirst().motivos());

        ModeloVe editado = new ModeloVe(modelo.dados().nome(), modelo.dados().valvulasPorCilindro(),
                modelo.dados().comandos(), modelo.dados().ve(), "Medido na wastegate do cliente", null);
        catalogo.salvar(TipoCatalogo.MODELO_VE, modelo.id(), editado, modelo.info());
        assertEquals(List.of("o modelo de VE mudou no catálogo: " + modelo.info().nome() + " v1 → v2"),
                mapas.mapas(projeto, RegrasMapaBase.padrao()).getFirst().motivos());

        relogio.andar(java.time.Duration.ofMinutes(5));
        ConfiguracaoMotor motor = casos.abrir(projeto).motor().configuracao();
        casos.salvarMotor(projeto, "taxa nova", motor.paraConstrutor().taxaCompressao(9.0).construir());
        List<String> motivos = mapas.mapas(projeto, RegrasMapaBase.padrao()).getFirst().motivos();
        assertTrue(motivos.contains("o motor mudou: este mapa usou a v1 e o motor está na v2"), motivos::toString);
        EntradaInvalidaException velho = assertThrows(EntradaInvalidaException.class,
                () -> mapas.salvar(projeto, calculo, escolhas, RegrasMapaBase.padrao(), justificativas()));
        assertTrue(velho.getMessage().startsWith("O último cálculo está desatualizado"), velho.getMessage());

        long novo = salvarCalculo();
        motivos = mapas.mapas(projeto, RegrasMapaBase.padrao()).getFirst().motivos();
        assertTrue(motivos.contains("há um cálculo mais novo no projeto (#" + novo + "; este mapa usou o #" + calculo
                + ")"), motivos::toString);
        EntradaInvalidaException antigo = assertThrows(EntradaInvalidaException.class,
                () -> mapas.salvar(projeto, calculo, escolhas, RegrasMapaBase.padrao(), justificativas()));
        assertEquals("Há um cálculo mais novo no projeto: gere o mapa de novo com ele.", antigo.getMessage());
        mapas.salvar(projeto, novo, escolhas, RegrasMapaBase.padrao(), justificativas());
        assertFalse(mapas.mapas(projeto, RegrasMapaBase.padrao()).getFirst().desatualizado());
    }
}
