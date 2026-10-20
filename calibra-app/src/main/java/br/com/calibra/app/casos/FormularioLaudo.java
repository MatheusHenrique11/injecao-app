package br.com.calibra.app.casos;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import br.com.calibra.core.oficina.CodigoFalha;
import br.com.calibra.core.oficina.CondicaoMedicao;
import br.com.calibra.core.oficina.LaudoEletrico;
import br.com.calibra.core.oficina.Medicao;
import br.com.calibra.core.oficina.ResultadoLaudo;
import br.com.calibra.core.oficina.SituacaoCodigo;

/**
 * A leitura da janela do laudo (Fase 3B, SPEC 6.6.3), sem JavaFX: as linhas das medições e dos códigos de falha, como
 * texto, viram o {@link LaudoEletrico}, e o laudo gravado vira as linhas de novo, para editar. A linha totalmente
 * vazia fica de fora; as regras do laudo (o que falta numa linha começada) ficam com o núcleo.
 *
 * <p>Cada linha de medição é uma lista de texto: o que se mediu, o valor, a unidade, a condição (nome da constante,
 * ou vazio), a referência e "a verificar" ("true" ou "false"). Cada linha de código: o código, a descrição e a
 * situação (nome da constante, ou vazio).
 */
public final class FormularioLaudo {

    public static final int NOME = 0;
    public static final int VALOR = 1;
    public static final int UNIDADE = 2;
    public static final int CONDICAO = 3;
    public static final int REFERENCIA = 4;
    public static final int A_VERIFICAR = 5;

    public static final int CODIGO = 0;
    public static final int DESCRICAO = 1;
    public static final int SITUACAO = 2;

    private FormularioLaudo() {
    }

    /**
     * O laudo com o que está na janela, ainda sem conferir (quem grava confere).
     *
     * @param quando a data e a hora já lidas; null se estavam erradas
     * @param resultado null = a definir
     */
    public static LaudoEletrico ler(LocalDateTime quando, String queixa, List<List<String>> medicoes,
            List<List<String>> codigos, String testes, String causa, String solucao, String recomendacoes,
            ResultadoLaudo resultado) {
        return new LaudoEletrico(quando, queixa, medicoes(medicoes), codigos(codigos), testes, causa, solucao,
                recomendacoes, resultado);
    }

    /** As medições das linhas; a linha sem nenhum texto (só a condição e a marca que vêm prontas) fica de fora. */
    public static List<Medicao> medicoes(List<List<String>> linhas) {
        List<Medicao> medicoes = new ArrayList<>();
        for (List<String> linha : linhas) {
            if (vazia(linha, NOME, VALOR, UNIDADE, REFERENCIA)) {
                continue;
            }
            medicoes.add(new Medicao(celula(linha, NOME), celula(linha, VALOR), celula(linha, UNIDADE),
                    constante(CondicaoMedicao.class, celula(linha, CONDICAO)), celula(linha, REFERENCIA),
                    Boolean.parseBoolean(celula(linha, A_VERIFICAR))));
        }
        return medicoes;
    }

    /** Os códigos das linhas; a linha sem código nem descrição fica de fora. */
    public static List<CodigoFalha> codigos(List<List<String>> linhas) {
        List<CodigoFalha> codigos = new ArrayList<>();
        for (List<String> linha : linhas) {
            if (vazia(linha, CODIGO, DESCRICAO)) {
                continue;
            }
            codigos.add(new CodigoFalha(celula(linha, CODIGO), celula(linha, DESCRICAO),
                    constante(SituacaoCodigo.class, celula(linha, SITUACAO))));
        }
        return codigos;
    }

    /** As linhas das medições do laudo gravado, para editar. */
    public static List<List<String>> linhasDasMedicoes(List<Medicao> medicoes) {
        return medicoes.stream().map(m -> List.of(m.nome(), m.valor(), m.unidade(), nome(m.condicao()),
                m.referencia(), Boolean.toString(m.referenciaAVerificar()))).toList();
    }

    /** As linhas dos códigos do laudo gravado, para editar. */
    public static List<List<String>> linhasDosCodigos(List<CodigoFalha> codigos) {
        return codigos.stream().map(c -> List.of(c.codigo(), c.descricao(), nome(c.situacao()))).toList();
    }

    private static boolean vazia(List<String> linha, int... colunas) {
        for (int coluna : colunas) {
            if (!celula(linha, coluna).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static String celula(List<String> linha, int indice) {
        String valor = indice < linha.size() ? linha.get(indice) : null;
        return valor == null ? "" : valor.strip();
    }

    private static <E extends Enum<E>> E constante(Class<E> tipo, String nome) {
        if (nome.isEmpty()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, nome);
        } catch (IllegalArgumentException erro) {
            return null;
        }
    }

    private static String nome(Enum<?> constante) {
        return constante == null ? "" : constante.name();
    }
}
