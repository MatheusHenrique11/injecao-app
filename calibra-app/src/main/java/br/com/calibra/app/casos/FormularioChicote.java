package br.com.calibra.app.casos;

import java.util.ArrayList;
import java.util.List;

import br.com.calibra.app.casos.CasosChicote.OpcaoInjecao;
import br.com.calibra.core.oficina.FichaChicote;
import br.com.calibra.core.oficina.PontoChicote;
import br.com.calibra.core.oficina.SituacaoChicote;

/**
 * A leitura da janela da ficha do chicote (Fase 3B, SPEC 6.6.5), sem JavaFX: as linhas dos pontos, como texto, viram a
 * {@link FichaChicote}, e a ficha gravada vira as linhas de novo, para editar. A linha totalmente vazia fica de fora;
 * as regras (o que falta) ficam com o núcleo.
 *
 * <p>Cada linha de ponto é uma lista de texto: a função, o conector, o pino da injeção, a observação e "a verificar"
 * ("true" ou "false").
 */
public final class FormularioChicote {

    public static final int FUNCAO = 0;
    public static final int CONECTOR = 1;
    public static final int PINO = 2;
    public static final int OBSERVACAO = 3;
    public static final int A_VERIFICAR = 4;

    private FormularioChicote() {
    }

    /**
     * A ficha com o que está na janela, ainda sem conferir (quem grava confere).
     *
     * @param injecaoDoCatalogo a injeção escolhida no catálogo; null = a do texto livre
     * @param injecaoTexto a injeção escrita, quando não é do catálogo
     * @param motorVersaoId a versão do motor do projeto; null = nenhuma
     */
    public static FichaChicote ler(OpcaoInjecao injecaoDoCatalogo, String injecaoTexto, Long motorVersaoId,
            String versao, String fontePinout, List<List<String>> pontos, String materiais, String observacoes,
            SituacaoChicote situacao) {
        String injecao = injecaoDoCatalogo == null ? injecaoTexto : injecaoDoCatalogo.nome();
        Long injecaoId = injecaoDoCatalogo == null ? null : injecaoDoCatalogo.id();
        return new FichaChicote(injecao, injecaoId, motorVersaoId, versao, fontePinout, pontos(pontos), materiais,
                observacoes, situacao);
    }

    /** Os pontos das linhas; a linha sem nenhum texto (só a marca "a verificar") fica de fora. */
    public static List<PontoChicote> pontos(List<List<String>> linhas) {
        List<PontoChicote> pontos = new ArrayList<>();
        for (List<String> linha : linhas) {
            if (celula(linha, FUNCAO).isEmpty() && celula(linha, CONECTOR).isEmpty() && celula(linha, PINO).isEmpty()
                    && celula(linha, OBSERVACAO).isEmpty()) {
                continue;
            }
            pontos.add(new PontoChicote(celula(linha, FUNCAO), celula(linha, CONECTOR), celula(linha, PINO),
                    celula(linha, OBSERVACAO), Boolean.parseBoolean(celula(linha, A_VERIFICAR))));
        }
        return pontos;
    }

    /** As linhas dos pontos da ficha gravada, para editar. */
    public static List<List<String>> linhasDosPontos(List<PontoChicote> pontos) {
        return pontos.stream().map(p -> List.of(p.funcao(), p.conector(), p.pinoDaInjecao(), p.observacao(),
                Boolean.toString(p.aVerificar()))).toList();
    }

    private static String celula(List<String> linha, int indice) {
        String valor = indice < linha.size() ? linha.get(indice) : null;
        return valor == null ? "" : valor.strip();
    }
}
