package br.com.calibra.core.cadastro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Veículo (SPEC 6.2): as regras da tabela. */
class VeiculoTest {

    private static final int ANO_ATUAL = 2026;

    private static Veiculo carro(String placa, TipoCambio cambio, boolean controlado, Tracao tracao) {
        return new Veiculo(TipoVeiculo.CARRO, "VW", "Gol", "GTi", 1994, 1994, placa, "9BWZZZ377VT004251", "", "",
                "", 250_000.0, cambio, controlado, tracao, 960.0, "Branco", "");
    }

    private static Veiculo lancha(String nome, TipoCambio cambio) {
        return new Veiculo(TipoVeiculo.LANCHA, "Focker", "240", "", 2019, 2020, "", "", nome, "123456789", "",
                350.0, cambio, false, null, null, "", "");
    }

    @Test
    void carroCompletoEValido() {
        Veiculo gol = carro("abc-1d23", TipoCambio.MANUAL, false, Tracao.DIANTEIRA);
        assertTrue(gol.validar(ANO_ATUAL).isEmpty(), gol.validar(ANO_ATUAL)::toString);
        assertEquals("ABC1D23", gol.placa(), "placa em maiúsculas, sem traço");
        assertEquals("VW Gol GTi", gol.descricao());
        assertEquals("ABC1D23", gol.identificacao());
        assertEquals("km", gol.tipo().unidadeUso());
    }

    @Test
    void lanchaUsaIdentificacaoNauticaEHoras() {
        Veiculo focker = lancha("Maré Alta", TipoCambio.NAUTICO_RABETA_OU_REVERSOR);
        assertTrue(focker.validar(ANO_ATUAL).isEmpty(), focker.validar(ANO_ATUAL)::toString);
        assertEquals("Maré Alta", focker.identificacao());
        assertEquals("h", focker.tipo().unidadeUso());
    }

    @Test
    void marcaEModeloSaoObrigatorios() {
        Veiculo vazio = new Veiculo(null, " ", "", "", null, null, "", "", "", "", "", null, null, false, null, null,
                "", "");
        assertEquals(List.of("Escolha o tipo do veículo.", "Informe a marca do veículo.",
                "Informe o modelo do veículo."), vazio.validar(ANO_ATUAL));
    }

    @Test
    void anosDe1900AteOAnoQueVem() {
        Veiculo velho = new Veiculo(TipoVeiculo.CARRO, "Ford", "T", "", 1899, 2028, "", "", "", "", "", null, null,
                false, null, null, "", "");
        assertEquals(List.of("O ano de fabricação deve ficar entre 1900 e 2027.",
                "O ano do modelo deve ficar entre 1900 e 2027."), velho.validar(ANO_ATUAL));
        Veiculo modeloQueVem = new Veiculo(TipoVeiculo.CARRO, "VW", "Polo", "", 2026, 2027, "", "", "", "", "",
                null, null, false, null, null, "", "");
        assertTrue(modeloQueVem.validar(ANO_ATUAL).isEmpty());
    }

    @Test
    void placaChassiETracaoSoEmTerrestres() {
        Veiculo errado = new Veiculo(TipoVeiculo.JET_SKI, "Sea-Doo", "RXP", "", 2020, 2020, "ABC1234", "", "", "",
                "", null, TipoCambio.JATO, false, Tracao.TRASEIRA, null, "", "");
        assertEquals(List.of("Placa e chassi são só para veículos terrestres.", "Tração é só para veículos terrestres."),
                errado.validar(ANO_ATUAL));
    }

    @Test
    void identificacaoNauticaSoEmEmbarcacoes() {
        Veiculo errado = new Veiculo(TipoVeiculo.MOTO, "Honda", "CB 500", "", 2020, 2020, "", "", "Maré Alta", "",
                "", null, null, false, null, null, "", "");
        assertEquals(List.of("Nome da embarcação, número de inscrição e número do casco são só para embarcações."),
                errado.validar(ANO_ATUAL));
    }

    @Test
    void cambioDeEmbarcacaoNaoServeEmCarroEViceVersa() {
        assertEquals(List.of("O câmbio \"Jato\" não combina com carro."),
                carro("", TipoCambio.JATO, false, null).validar(ANO_ATUAL));
        assertEquals(List.of("O câmbio \"Manual\" não combina com lancha."),
                lancha("Maré Alta", TipoCambio.MANUAL).validar(ANO_ATUAL));
    }

    @ParameterizedTest
    @EnumSource(TipoCambio.class)
    void cambioControladoPelaInjecaoSoNosAutomaticos(TipoCambio cambio) {
        boolean pode = cambio == TipoCambio.AUTOMATICO || cambio == TipoCambio.AUTOMATIZADO
                || cambio == TipoCambio.DUPLA_EMBREAGEM;
        assertEquals(pode, cambio.podeSerControladoPelaInjecao());
        if (!cambio.nautico()) {
            List<String> erros = carro("", cambio, true, null).validar(ANO_ATUAL);
            assertEquals(pode, erros.isEmpty(), erros::toString);
        }
    }

    @Test
    void usoEPesoNaoPodemSerNegativos() {
        Veiculo errado = new Veiculo(TipoVeiculo.CARRO, "VW", "Gol", "", null, null, "", "", "", "", "", -1.0, null,
                false, null, 0.0, "", "");
        assertEquals(List.of("O uso acumulado não pode ser negativo.", "O peso do veículo deve ser maior que zero."),
                errado.validar(ANO_ATUAL));
    }

    /** A entrada de uma OS atualiza só o km; o resto do veículo fica igual. */
    @Test
    void comUsoAcumuladoTrocaSoOUso() {
        Veiculo gol = carro("ABC1D23", TipoCambio.MANUAL, false, Tracao.DIANTEIRA);
        Veiculo depois = gol.comUsoAcumulado(251_500.0);
        assertEquals(251_500.0, depois.usoAcumulado());
        assertEquals(gol, depois.comUsoAcumulado(250_000.0), "voltando o km, é o mesmo veículo");
        assertNull(gol.comUsoAcumulado(null).usoAcumulado());
    }

    @Test
    void trocarDeCarroParaLanchaTiraOsCamposDeCarro() {
        Veiculo gol = carro("ABC1D23", TipoCambio.AUTOMATICO, true, Tracao.DIANTEIRA);
        Veiculo comoLancha = new Veiculo(TipoVeiculo.LANCHA, gol.marca(), gol.modelo(), gol.versao(),
                gol.anoFabricacao(), gol.anoModelo(), gol.placa(), gol.chassi(), "Maré Alta", "", "",
                gol.usoAcumulado(), gol.cambio(), gol.cambioControladoPelaInjecao(), gol.tracao(), gol.pesoKg(),
                gol.cor(), gol.observacoes()).ajustadoAoTipo();
        assertEquals("", comoLancha.placa());
        assertEquals("", comoLancha.chassi());
        assertNull(comoLancha.tracao());
        assertNull(comoLancha.cambio(), "câmbio de carro não serve na lancha");
        assertFalse(comoLancha.cambioControladoPelaInjecao());
        assertEquals("Maré Alta", comoLancha.nomeEmbarcacao());
        assertTrue(comoLancha.validar(ANO_ATUAL).isEmpty(), comoLancha.validar(ANO_ATUAL)::toString);

        Veiculo deVoltaACarro = new Veiculo(TipoVeiculo.CARRO, "VW", "Gol", "", null, null, "", "", "Maré Alta",
                "123", "C-9", null, TipoCambio.JATO, false, null, null, "", "").ajustadoAoTipo();
        assertEquals("", deVoltaACarro.nomeEmbarcacao());
        assertEquals("", deVoltaACarro.numeroInscricao());
        assertEquals("", deVoltaACarro.numeroCasco());
        assertNull(deVoltaACarro.cambio());
    }

    @Test
    void outroContaComoTerrestre() {
        assertTrue(TipoVeiculo.OUTRO.terrestre());
        assertEquals("km", TipoVeiculo.OUTRO.unidadeUso());
    }
}
