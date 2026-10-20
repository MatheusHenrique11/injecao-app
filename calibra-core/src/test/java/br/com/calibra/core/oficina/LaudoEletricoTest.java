package br.com.calibra.core.oficina;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * O laudo de diagnóstico elétrico (SPEC 6.6.3, Fase 3B): as regras para gravar e os indícios de dado pessoal na
 * queixa (LGPD).
 */
class LaudoEletricoTest {

    private static final LocalDateTime QUANDO = LocalDateTime.of(2026, 10, 20, 10, 30);

    private static LaudoEletrico laudo(String queixa, List<Medicao> medicoes, List<CodigoFalha> codigos) {
        return new LaudoEletrico(QUANDO, queixa, medicoes, codigos, "", "", "", "", null);
    }

    @Test
    void laudoCompletoPassa() {
        LaudoEletrico laudo = new LaudoEletrico(QUANDO, "  Bateria descarrega de um dia para o outro  ", List.of(
                new Medicao(" Tensão da bateria ", "12,1", "V", CondicaoMedicao.REPOUSO, "12,4 a 12,7 V", true),
                new Medicao("Consumo em repouso", "sem leitura", "", CondicaoMedicao.OUTRA, "", false)), List.of(
                new CodigoFalha("P0562", "Tensão do sistema baixa", SituacaoCodigo.ATIVO)),
                "Teste de fuga com o amperímetro.", "Módulo do som ligado direto na bateria.",
                "Ligação pelo pós-chave.", "", ResultadoLaudo.RESOLVIDO);
        assertEquals(List.of(), laudo.validar());
        assertEquals("Bateria descarrega de um dia para o outro", laudo.queixa(), "sem espaços nas pontas");
        assertEquals("Tensão da bateria", laudo.medicoes().getFirst().nome());
        assertEquals("12,1 V", laudo.medicoes().getFirst().valorComUnidade());
        assertEquals("sem leitura", laudo.medicoes().get(1).valorComUnidade(), "sem unidade, só o valor");
        assertEquals("resolvido", laudo.rotuloDoResultado());
        assertEquals("a definir", laudo("Queixa", List.of(), List.of()).rotuloDoResultado(), "sem resultado ainda");
    }

    @Test
    void regrasDoLaudo() {
        assertEquals(List.of("Informe a data e a hora do laudo.", "Escreva a queixa do cliente, nas palavras dele."),
                new LaudoEletrico(null, "  ", null, null, null, null, null, null, null).validar());
        assertEquals(List.of(
                "Medição 1: escreva o que foi medido.",
                "Medição 2 (Tensão de carga): escreva o valor.",
                "Medição 2 (Tensão de carga): escolha a condição (repouso, motor ligado, carga acionada ou outra).",
                "Código 1: escreva o código.",
                "Código 2 (P0300): escolha a situação (ativo, histórico ou apagado)."),
                laudo("Luz da bateria acesa", List.of(
                        new Medicao("", "12,6", "V", CondicaoMedicao.REPOUSO, "", false),
                        new Medicao("Tensão de carga", " ", "V", null, "", false)), List.of(
                        new CodigoFalha("", "Sem descrição", SituacaoCodigo.HISTORICO),
                        new CodigoFalha("P0300", "", null))).validar());
    }

    /** O nome do cliente na queixa, sem diferença de acento ou maiúscula; as palavras curtas do nome não contam. */
    @Test
    void nomeDoClienteNaQueixa() {
        assertEquals(List.of("o nome do cliente (Ana)"),
                DadosPessoais.indicios("A cliente ana disse que a bateria descarrega", "Ana Costa"));
        assertEquals(List.of("o nome do cliente (João)"),
                DadosPessoais.indicios("Seu Joao trouxe o carro com a luz acesa", "João da Silva"));
        assertEquals(List.of("o nome do cliente (fictícia)"),
                DadosPessoais.indicios("Queixa fictícia de teste", "Ana Costa (fictícia)"), "sem os parênteses");
        assertEquals(List.of(), DadosPessoais.indicios("Falha das luzes e dos faróis", "Maria das Dores"),
                "\"das\" e \"dos\" são palavras de ligação");
        assertEquals(List.of(), DadosPessoais.indicios("Li o código no scanner", "Li Wu"),
                "nome com menos de 3 letras");
        assertEquals(List.of(), DadosPessoais.indicios("A cliente Ana disse", null), "o cliente anonimizado não conta");
    }

    @Test
    void telefoneDocumentoEEmail() {
        assertEquals(List.of("um telefone ou documento ((11) 91234-5678)"),
                DadosPessoais.indicios("Ligar para (11) 91234-5678 quando terminar", "Ana Costa"));
        assertEquals(List.of("um telefone ou documento (11 91234 5678)"),
                DadosPessoais.indicios("whats 11 91234 5678", null));
        assertEquals(List.of("um telefone ou documento (123.456.789-09)"),
                DadosPessoais.indicios("CPF 123.456.789-09 na nota", null));
        assertEquals(List.of("um telefone ou documento (11.222.333)"),
                DadosPessoais.indicios("CNPJ 11.222.333/0001-81", null));
        assertEquals(List.of("um e-mail (ana.costa@exemplo.com.br)"),
                DadosPessoais.indicios("Mandar o laudo para ana.costa@exemplo.com.br", null));
        assertEquals(List.of("o nome do cliente (Costa)", "um telefone ou documento (11 91234-5678)",
                "um e-mail (dona@exemplo.com)"), DadosPessoais.indicios("Dona Costa, 11 91234-5678, dona@exemplo.com",
                        "Ana Costa"), "os três, na ordem");
    }

    /** O que aparece num laudo e não é dado pessoal não dispara o aviso. */
    @Test
    void textoTecnicoNaoDispara() {
        assertEquals(List.of(), DadosPessoais.indicios("Bateria com 12,6 V em repouso e 14,2 V com o motor ligado; "
                + "P0300 e P0171 ativos; trazido em 19/10/2026 com 120.500 km; 2.000 rpm; 10:30", "Ana Costa"));
        assertEquals(List.of(), DadosPessoais.indicios("   ", "Ana Costa"));
        assertEquals(List.of(), DadosPessoais.indicios(null, "Ana Costa"));
    }
}
