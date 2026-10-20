package br.com.calibra.core.cadastro;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Instalações do veículo (SPEC 6.2, Fase 3B): as regras e os textos. */
class InstalacaoTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 20);

    private static Instalacao som(LocalDate data) {
        return new Instalacao(TipoInstalacao.SOM, "Módulo no porta-malas, com fusível de 80 A na bateria", "Taramps",
                "TS 800x4", "800 W RMS, 4 canais", data, "");
    }

    @Test
    void completaEValida() {
        Instalacao som = som(HOJE);
        assertEquals(List.of(), som.validar(HOJE, null));
        assertEquals("Taramps TS 800x4", som.marcaEModelo());
        assertEquals(List.of(), som.validarRetirada(HOJE, HOJE), "retirada no mesmo dia");
    }

    @Test
    void tipoEDescricaoObrigatorios() {
        Instalacao vazia = new Instalacao(null, "   ", null, null, null, null, null);
        assertEquals(List.of("Escolha o tipo da instalação.", "Descreva o que foi instalado e onde."),
                vazia.validar(HOJE, null));
        assertEquals("", vazia.descricao());
        assertEquals("", vazia.marca(), "texto ausente vira vazio");
        assertEquals("", vazia.marcaEModelo());
        assertEquals(List.of(), new Instalacao(TipoInstalacao.ALARME_RASTREADOR, "Alarme informado pelo cliente", "",
                "", "", null, "").validar(HOJE, null), "só o tipo e a descrição são obrigatórios");
    }

    @Test
    void textosSemEspacosNasPontas() {
        Instalacao instalacao = new Instalacao(TipoInstalacao.ILUMINACAO, "  Farol de milha  ", " Hella ", "  ",
                " 55 W ", null, " relé próprio ");
        assertEquals("Farol de milha", instalacao.descricao());
        assertEquals("Hella", instalacao.marcaEModelo(), "só a marca");
        assertEquals("55 W", instalacao.potenciaOuEspecificacao());
        assertEquals("relé próprio", instalacao.observacoes());
    }

    @Test
    void datas() {
        assertEquals(List.of("A data da instalação não pode estar no futuro."),
                som(HOJE.plusDays(1)).validar(HOJE, null));
        assertEquals(List.of("A data da instalação não pode ser depois da retirada (15/10/2026)."),
                som(LocalDate.of(2026, 10, 18)).validar(HOJE, LocalDate.of(2026, 10, 15)));
        assertEquals(List.of(), som(LocalDate.of(2026, 10, 15)).validar(HOJE, LocalDate.of(2026, 10, 15)));

        Instalacao som = som(LocalDate.of(2026, 10, 18));
        assertEquals(List.of("A data da retirada não pode estar no futuro."), som.validarRetirada(HOJE.plusDays(1),
                HOJE));
        assertEquals(List.of("A retirada não pode ser antes da instalação (18/10/2026)."),
                som.validarRetirada(LocalDate.of(2026, 10, 17), HOJE));
        assertEquals(List.of("Informe a data em que o item saiu do veículo."), som.validarRetirada(null, HOJE));
        assertEquals(List.of(), som(null).validarRetirada(LocalDate.of(2020, 1, 1), HOJE),
                "sem a data da instalação, qualquer data até hoje");
    }

    /** Os nomes são os da SPEC e vão assim para o banco; os rótulos são os da tela. */
    @Test
    void tipos() {
        assertEquals(List.of("SOM", "ALARME_RASTREADOR", "ILUMINACAO", "VIDRO_TRAVA", "INJECAO_PROGRAMAVEL", "OUTRA"),
                Arrays.stream(TipoInstalacao.values()).map(TipoInstalacao::name).toList());
        assertEquals(List.of("Som", "Alarme ou rastreador", "Iluminação", "Vidro ou trava", "Injeção programável",
                "Outra"), Arrays.stream(TipoInstalacao.values()).map(TipoInstalacao::rotulo).toList());
    }
}
