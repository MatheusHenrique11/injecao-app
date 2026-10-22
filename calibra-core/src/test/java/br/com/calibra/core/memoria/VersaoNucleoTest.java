package br.com.calibra.core.memoria;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Decisão 295: um resultado só fica desatualizado pelo núcleo quando as contas dele mudaram depois. */
class VersaoNucleoTest {

    @Test
    void comparaNumeroANumero() {
        assertTrue(VersaoNucleo.anterior("1.0.0", "1.1.0"));
        assertTrue(VersaoNucleo.anterior("1.1.0", "1.2.0"));
        assertFalse(VersaoNucleo.anterior("1.2.0", "1.2.0"), "a mesma não é anterior");
        assertFalse(VersaoNucleo.anterior("1.10.0", "1.9.0"), "10 é maior que 9, não pelo texto");
        assertFalse(VersaoNucleo.anterior("1.2", "1.2.0"), "o que falta conta como zero");
        assertTrue(VersaoNucleo.anterior("", "1.0.0"), "vazia conta como anterior");
        assertTrue(VersaoNucleo.anterior(null, "1.0.0"));
    }

    @Test
    void oMapaBaseNaoDesatualizaODimensionamento() {
        assertFalse(VersaoNucleo.anterior("1.1.0", VersaoNucleo.ULTIMA_MUDANCA_DO_DIMENSIONAMENTO),
                "um cálculo da 1.1.0 continua em dia na 1.2.0");
        assertTrue(VersaoNucleo.anterior("1.0.0", VersaoNucleo.ULTIMA_MUDANCA_DO_DIMENSIONAMENTO));
        assertFalse(VersaoNucleo.anterior(VersaoNucleo.ATUAL, VersaoNucleo.ULTIMA_MUDANCA_DO_MAPA_BASE));
        assertFalse(VersaoNucleo.anterior(VersaoNucleo.ATUAL, VersaoNucleo.ULTIMA_MUDANCA_DO_DIMENSIONAMENTO));
    }
}
