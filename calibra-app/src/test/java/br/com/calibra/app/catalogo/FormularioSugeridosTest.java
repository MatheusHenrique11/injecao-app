package br.com.calibra.app.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import br.com.calibra.app.catalogo.FormularioSugeridos.Leitura;
import br.com.calibra.app.catalogo.FormularioSugeridos.Linha;
import br.com.calibra.core.orcamento.CategoriaServico;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.ServicoPadrao;
import br.com.calibra.core.orcamento.ServicosSugeridos;
import br.com.calibra.core.orcamento.ServicosSugeridos.Sugestao;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/** A janela de serviços sugeridos (SPEC 6.6.1) sem a tela: só as linhas marcadas, com horas ou preço. */
class FormularioSugeridosTest {

    /** Nenhum modelo de checklist ativo: os sugeridos entram sem checklist. */
    private static final Map<String, Long> SEM_CHECKLISTS = Map.of();

    private static Sugestao sugestao(String nome) {
        return ServicosSugeridos.todos().stream().filter(s -> s.nome().equals(nome)).findFirst().orElseThrow();
    }

    @Test
    void linhasMarcadasViramServicosDoCatalogo() {
        Leitura leitura = FormularioSugeridos.ler(List.of(
                new Linha(sugestao("Troca de alto-falantes"), true, " 1,5 ", ""),
                new Linha(sugestao("Instalação de kit de trava elétrica"), false, "3", ""),
                new Linha(sugestao("Confecção de chicote para injeção programável"), true, "", "1.800,00")),
                SEM_CHECKLISTS);
        assertEquals(List.of(), leitura.erros());
        assertEquals(2, leitura.servicos().size(), "a linha desmarcada fica de fora, mesmo preenchida");

        FormularioSugeridos.Novo falantes = leitura.servicos().get(0);
        assertEquals(new ServicoPadrao("Troca de alto-falantes", 1.5, "Tabela da oficina", null, CategoriaServico.SOM,
                "", true, null, null), falantes.dados());
        assertEquals(new InfoCatalogo("Troca de alto-falantes", "Tabela da oficina", null, false, null, ""),
                falantes.info());

        FormularioSugeridos.Novo chicote = leitura.servicos().get(1);
        assertEquals(CategoriaServico.INJECAO_PROGRAMAVEL, chicote.dados().categoria());
        assertFalse(chicote.dados().exigeEntradaDoVeiculo(), "a confecção de chicote é de bancada");
        assertEquals(Dinheiro.ler("1.800,00"), chicote.info().precoReferencia());
        assertNull(chicote.dados().horas());
        assertEquals(List.of(), FormasCatalogo.de(TipoCatalogo.SERVICO)
                .validar(chicote.dados(), chicote.info()), "passa nas regras do formulário de serviço");
    }

    /** O app não inventa tempo de serviço (decisão 216): a linha marcada precisa de horas ou de preço. */
    @Test
    void erradasMostramTudoDeUmaVezENadaPassa() {
        Leitura leitura = FormularioSugeridos.ler(List.of(
                new Linha(sugestao("Troca de alto-falantes"), true, "", ""),
                new Linha(sugestao("Instalação de módulo de potência"), true, "1.5", ""),
                new Linha(sugestao("Instalação de subwoofer e caixa"), true, "0", ""),
                new Linha(sugestao("Instalação de multimídia ou central"), true, "", "-10,00"),
                new Linha(sugestao("Instalação de som completa"), true, "", "duzentos"),
                new Linha(sugestao("Reparo de chave ou botoeira"), true, "1", "")), SEM_CHECKLISTS);
        assertEquals(List.of(
                "Troca de alto-falantes: informe as horas ou o preço.",
                "Instalação de módulo de potência, horas: \"1.5\" não é um número válido: use vírgula para decimais "
                        + "(ex.: 3,5).",
                "Instalação de subwoofer e caixa: as horas devem ser maiores que zero.",
                "Instalação de multimídia ou central: o preço não pode ser negativo.",
                "Instalação de som completa, preço: \"duzentos\" não é um valor em reais: use vírgula para os centavos "
                        + "(ex.: 1.234,56)."), leitura.erros());
        assertFalse(leitura.ok());
        assertTrue(leitura.servicos().isEmpty(), "com erro, nenhum serviço passa (nem o certo)");
    }

    @Test
    void semNenhumaMarcadaAvisa() {
        Leitura leitura = FormularioSugeridos.ler(List.of(new Linha(sugestao("Troca de alto-falantes"), false, "", "")),
                SEM_CHECKLISTS);
        assertEquals(List.of("Marque ao menos um serviço e preencha as horas ou o preço dele."), leitura.erros());
    }

    /**
     * Bloco 3B.3: cada sugerido entra com o modelo de checklist da categoria dele, pela chave da semente; o modelo que
     * não está ativo (arquivado) fica de fora, e o diagnóstico de injeção programável não tem modelo.
     */
    @Test
    void cadaUmComOChecklistDaCategoria() {
        Map<String, Long> checklists = Map.of(ServicosSugeridos.CHECKLIST_SOM, 41L,
                ServicosSugeridos.CHECKLIST_CHICOTE, 45L);
        Leitura leitura = FormularioSugeridos.ler(List.of(
                new Linha(sugestao("Troca de alto-falantes"), true, "1,5", ""),
                new Linha(sugestao("Confecção de chicote para injeção programável"), true, "", "1.800,00"),
                new Linha(sugestao("Instalação de LED ou xenon em farol"), true, "1", ""),
                new Linha(sugestao("Diagnóstico de injeção programável (falhas, leitura de sensores e log)"), true,
                        "2", "")), checklists);
        assertEquals(List.of(), leitura.erros());
        assertEquals(Arrays.asList(41L, 45L, null, null),
                leitura.servicos().stream().map(novo -> novo.dados().checklistModeloId()).toList(),
                "som e chicote com o modelo; iluminação sem (modelo fora da lista); diagnóstico sem modelo");
    }

    @Test
    void nomeParaCompararSemAcentoNemMaiuscula() {
        assertEquals(FormularioSugeridos.chave("Troca de regulador (máquina de vidro)"),
                FormularioSugeridos.chave("TROCA DE REGULADOR (MAQUINA DE VIDRO)"));
        assertFalse(FormularioSugeridos.chave("Troca de alto-falantes").equals(
                FormularioSugeridos.chave("Troca de alto-falantes dianteiros")));
    }
}
