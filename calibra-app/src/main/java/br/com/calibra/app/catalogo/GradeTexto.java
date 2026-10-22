package br.com.calibra.app.catalogo;

import java.util.List;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Collectors;

import br.com.calibra.core.mapa.OrientacaoTabela;
import br.com.calibra.core.mapa.Tabela;
import br.com.calibra.core.util.NumeroBr;

/**
 * Eixos e células de uma tabela como texto do formulário (fase 4): o eixo numa linha, separado por espaço; as células
 * uma linha por linha da tabela, separadas por tabulação (cola direto no Excel e vem direto do software da injeção).
 */
final class GradeTexto {

    private GradeTexto() {
    }

    /** "500 1.000 1.500". */
    static String eixo(List<Double> pontos, DoubleUnaryOperator naTela) {
        return pontos.stream().map(p -> TextoForm.numero(naTela.applyAsDouble(p))).collect(Collectors.joining(" "));
    }

    /** As células como aparecem com a orientação, uma linha do texto por linha da tabela; vazia vira "-". */
    static String celulas(Tabela tabela, OrientacaoTabela orientacao, DoubleUnaryOperator naTela) {
        return orientacao.naTela(tabela).stream()
                .map(linha -> linha.stream()
                        .map(v -> v == null ? "-" : TextoForm.numero(arredondar(naTela.applyAsDouble(v))))
                        .collect(Collectors.joining("\t")))
                .collect(Collectors.joining("\n"));
    }

    /**
     * Lê o eixo do campo; com erro, anota na leitura e devolve null.
     *
     * @param doTexto converte o número digitado para a unidade do núcleo (ex.: % para fração)
     */
    static List<Double> lerEixo(LeituraForm leitura, String chave, String qual, DoubleUnaryOperator doTexto) {
        try {
            return NumeroBr.lerLista(leitura.texto(chave)).stream().map(doTexto::applyAsDouble).toList();
        } catch (NumberFormatException erro) {
            leitura.erro(qual + ", " + erro.getMessage());
            return null;
        }
    }

    /** Lê as células do campo com os eixos e a orientação; com erro, anota na leitura e devolve null. */
    static Tabela lerCelulas(LeituraForm leitura, String chave, String qual, List<Double> rotacoes,
            List<Double> cargas, OrientacaoTabela orientacao, DoubleUnaryOperator doTexto) {
        try {
            List<List<Double>> naTela = NumeroBr.lerGrade(leitura.texto(chave)).stream()
                    .map(linha -> linha.stream().map(doTexto::applyAsDouble).toList()).toList();
            if (naTela.isEmpty()) {
                leitura.erro(qual + ": cole ou digite as células.");
                return null;
            }
            return orientacao.daTela(rotacoes, cargas, naTela);
        } catch (IllegalArgumentException erro) {
            leitura.erro(qual + ": " + erro.getMessage());
            return null;
        }
    }

    /** Sem o ruído do ponto flutuante (0,97 × 100 = 97,00000000000001). */
    private static double arredondar(double valor) {
        return Math.round(valor * 1e6) / 1e6;
    }
}
