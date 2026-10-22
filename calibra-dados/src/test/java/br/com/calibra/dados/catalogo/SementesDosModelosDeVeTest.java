package br.com.calibra.dados.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.mapa.ModeloVe;
import br.com.calibra.core.motor.Comando;
import br.com.calibra.dados.BaseDeTeste;

/**
 * Os modelos de VE que vêm com o app (migração V15, bloco 4.2; decisão 293): só os valores da SPEC 7.5, todos "a
 * verificar". Marcha lenta 50% (comando agressivo 40%), pico de 97% com 4 válvulas e de 88% com 2.
 */
class SementesDosModelosDeVeTest {

    private static final double EPS = 1e-9;

    @TempDir
    Path pasta;

    /**
     * O checksum da V15. Se este teste falhar, o arquivo dos modelos de VE mudou: depois que a V15 chegar aos bancos
     * (MSI 0.4.0), migração aplicada não se edita, e modelo novo ou corrigido entra numa migração nova.
     */
    @Test
    void checksumDaV15NaoMuda() {
        assertEquals(756_830_906, Sementes.checksum(Sementes.MODELOS_VE));
    }

    @Test
    void osQuatroModelosComOsValoresDaSpec() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta);
        CatalogoRepositorio catalogo = new CatalogoRepositorio(base.relogio);
        Map<String, ItemCatalogo<ModeloVe>> porChave = base.banco
                .ler(c -> catalogo.listar(c, TipoCatalogo.MODELO_VE, "", false)).stream()
                .collect(Collectors.toMap(ItemCatalogo::chaveSemente, Function.identity()));
        assertEquals(4, porChave.size());

        for (ItemCatalogo<ModeloVe> item : porChave.values()) {
            String nome = item.info().nome();
            assertTrue(item.info().aVerificar(), nome);
            assertTrue(item.info().fonte().startsWith("SPEC 7.5"), nome);
            assertEquals(nome, item.dados().nome(), nome);
            assertNull(item.info().precoReferencia(), nome);
            assertEquals(List.of(), item.dados().validar(), nome);
        }

        ModeloVe quatro = porChave.get("modelo-ve:4v-normal").dados();
        assertTrue(quatro.serve(4, Comando.ORIGINAL) && quatro.serve(4, Comando.MEDIO));
        assertEquals(0.50, quatro.ve(0.2, 0.35), EPS, "marcha lenta");
        assertEquals(0.97, quatro.ve(1.0, 1.0), EPS, "pico na plena carga");
        assertEquals(0.97, quatro.ve(1.0, 2.5), EPS, "acima de 100 kPa, o da plena carga");

        ModeloVe quatroAgressivo = porChave.get("modelo-ve:4v-agressivo").dados();
        assertTrue(quatroAgressivo.serve(4, Comando.AGRESSIVO));
        assertEquals(0.40, quatroAgressivo.ve(0.2, 0.35), EPS, "comando agressivo: menos VE em baixa");
        assertEquals(0.97, quatroAgressivo.ve(1.0, 1.0), EPS);

        assertEquals(0.88, porChave.get("modelo-ve:2v-normal").dados().ve(1.0, 1.0), EPS);
        assertEquals(0.50, porChave.get("modelo-ve:2v-normal").dados().ve(0.2, 0.35), EPS);
        assertEquals(0.40, porChave.get("modelo-ve:2v-agressivo").dados().ve(0.2, 0.35), EPS);
        assertEquals(0.88, porChave.get("modelo-ve:2v-agressivo").dados().ve(1.4, 1.5), EPS);
    }
}
