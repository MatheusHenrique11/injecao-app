package br.com.calibra.dados.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.combustivel.CombustiveisPadrao;
import br.com.calibra.core.combustivel.Combustivel;
import br.com.calibra.core.motor.ModeloMotor;
import br.com.calibra.dados.BaseDeTeste;

/**
 * Sementes do catálogo (migração V4): os combustíveis são os da SPEC 7.2 e os motores e montagens batem com a
 * pesquisa (docs/pesquisa/mercado-preparacao-brasil.md).
 */
class SementesTest {

    @TempDir
    Path pasta;

    private BaseDeTeste base;
    private CatalogoRepositorio catalogo;

    @BeforeEach
    void abrir() throws Exception {
        base = BaseDeTeste.criada(pasta);
        catalogo = new CatalogoRepositorio(base.relogio);
    }

    @Test
    void combustiveisSaoOsDaSpec() {
        List<Combustivel> gravados = base.banco.ler(c -> catalogo.listar(c, TipoCatalogo.COMBUSTIVEL, "", false))
                .stream().map(ItemCatalogo::dados).toList();
        assertEquals(CombustiveisPadrao.todos().size(), gravados.size());
        for (Combustivel padrao : CombustiveisPadrao.todos()) {
            assertTrue(gravados.contains(padrao), "falta ou mudou: " + padrao.codigo());
        }
    }

    @Test
    void motoresTemFonteEstaoAVerificarEPassamNaValidacao() {
        List<ItemCatalogo<ModeloMotor>> motores = base.banco.ler(c -> catalogo.listar(c, TipoCatalogo.MOTOR, "", false));
        assertEquals(19, motores.stream().filter(m -> !m.dados().montagem()).count(), "motores da pesquisa");
        assertEquals(6, motores.stream().filter(m -> m.dados().montagem()).count(), "montagens do AP");
        for (ItemCatalogo<ModeloMotor> motor : motores) {
            assertTrue(motor.info().aVerificar(), motor.info().nome());
            assertFalse(motor.info().fonte().isBlank(), motor.info().nome());
            assertEquals(motor.info().fonte(), motor.dados().fonte(), motor.info().nome());
            assertTrue(motor.dados().validar().isEmpty(), motor.info().nome() + ": " + motor.dados().validar());
        }
    }

    /** A cilindrada calculada pela geometria fica a até 1% da ficha. */
    @Test
    void cilindradaATe1PorCentoDaFicha() {
        for (ItemCatalogo<ModeloMotor> motor : base.banco.ler(c -> catalogo.listar(c, TipoCatalogo.MOTOR, "", false))) {
            if (motor.dados().cilindradaFichaCc() != null) {
                assertTrue(Math.abs(motor.dados().diferencaDaFicha()) <= 0.01,
                        motor.info().nome() + ": " + motor.dados().cilindradaCc() + " × " + motor.dados().cilindradaFichaCc());
            }
        }
    }

    /** As montagens do AP com as cilindradas da tabela da pesquisa (seção 2.1). */
    @Test
    void montagensComAsCilindradasDaPesquisa() {
        Map<String, ModeloMotor> porChave = base.banco.ler(c -> catalogo.listar(c, TipoCatalogo.MOTOR, "", false))
                .stream().collect(Collectors.toMap(ItemCatalogo::chaveSemente, ItemCatalogo::dados));
        Map<String, Double> pesquisa = Map.of(
                "montagem:ap-milenove", 1847.0,
                "montagem:ap-1.9-bloco-1.8", 1913.0,
                "montagem:ap-2.0-pistao-83", 2008.0,
                "montagem:ap-2.1-virabrequim-96", 2078.0,
                "montagem:ap-2.1-virabrequim-95.5", 2117.0,
                "montagem:ap-2.2-virabrequim-99", 2247.0);
        pesquisa.forEach((chave, cilindrada) -> {
            ModeloMotor montagem = porChave.get(chave);
            assertTrue(montagem.montagem(), chave);
            assertEquals(cilindrada, montagem.cilindradaCc(), 1.0, chave);
            assertEquals(144.0, montagem.bielaMm(), 1e-9, chave + ": todo AP usa biela de 144 mm");
        });
        assertEquals(1.45, porChave.get("montagem:ap-2.2-virabrequim-99").relacaoBielaCurso(), 0.005);
    }

    @Test
    void chavesUnicasEArquivoLegivel() {
        List<Sementes.Semente> sementes = Sementes.ler();
        assertEquals(31, sementes.size());
        Map<String, Long> repetidas = sementes.stream()
                .collect(Collectors.groupingBy(Sementes.Semente::chave, Collectors.counting()));
        assertTrue(repetidas.values().stream().allMatch(n -> n == 1), repetidas::toString);
        Map<String, ItemCatalogo<ModeloMotor>> gravados = base.banco.ler(c -> catalogo.listar(c, TipoCatalogo.MOTOR,
                "", false)).stream().collect(Collectors.toMap(ItemCatalogo::chaveSemente, Function.identity()));
        assertEquals(25, gravados.size());
    }
}
