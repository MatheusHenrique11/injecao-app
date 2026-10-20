package br.com.calibra.core.oficina;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * O laudo de diagnóstico elétrico da OS (SPEC 6.6.3, Fase 3B): quando, a queixa do cliente, as medições, os códigos
 * de falha, os testes feitos, a causa, a solução, as recomendações e o resultado. Dado técnico: fica quando o cliente
 * é anonimizado (decisão 219). Quem fez o laudo (o mecânico) fica com a OS, no banco.
 *
 * @param resultado null = a definir (o diagnóstico ainda não terminou)
 */
public record LaudoEletrico(LocalDateTime dataHora, String queixa, List<Medicao> medicoes, List<CodigoFalha> codigos,
        String testesRealizados, String causa, String solucao, String recomendacoes, ResultadoLaudo resultado) {

    public LaudoEletrico {
        queixa = limpo(queixa);
        medicoes = medicoes == null ? List.of() : List.copyOf(medicoes);
        codigos = codigos == null ? List.of() : List.copyOf(codigos);
        testesRealizados = limpo(testesRealizados);
        causa = limpo(causa);
        solucao = limpo(solucao);
        recomendacoes = limpo(recomendacoes);
    }

    /** O que está errado, em português, tudo de uma vez; vazio = pode gravar. */
    public List<String> validar() {
        List<String> erros = new ArrayList<>();
        if (dataHora == null) {
            erros.add("Informe a data e a hora do laudo.");
        }
        if (queixa.isEmpty()) {
            erros.add("Escreva a queixa do cliente, nas palavras dele.");
        }
        for (int i = 0; i < medicoes.size(); i++) {
            Medicao medicao = medicoes.get(i);
            String onde = "Medição " + (i + 1) + (medicao.nome().isEmpty() ? "" : " (" + medicao.nome() + ")") + ": ";
            if (medicao.nome().isEmpty()) {
                erros.add(onde + "escreva o que foi medido.");
            }
            if (medicao.valor().isEmpty()) {
                erros.add(onde + "escreva o valor.");
            }
            if (medicao.condicao() == null) {
                erros.add(onde + "escolha a condição (repouso, motor ligado, carga acionada ou outra).");
            }
        }
        for (int i = 0; i < codigos.size(); i++) {
            CodigoFalha codigo = codigos.get(i);
            String onde = "Código " + (i + 1) + (codigo.codigo().isEmpty() ? "" : " (" + codigo.codigo() + ")") + ": ";
            if (codigo.codigo().isEmpty()) {
                erros.add(onde + "escreva o código.");
            }
            if (codigo.situacao() == null) {
                erros.add(onde + "escolha a situação (ativo, histórico ou apagado).");
            }
        }
        return erros;
    }

    /** "resolvido", "resolvido em parte", "não resolvido" ou "a definir". */
    public String rotuloDoResultado() {
        return resultado == null ? "a definir" : resultado.rotulo();
    }

    private static String limpo(String texto) {
        return texto == null ? "" : texto.strip();
    }
}
