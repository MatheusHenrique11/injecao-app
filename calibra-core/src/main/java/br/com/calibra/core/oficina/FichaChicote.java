package br.com.calibra.core.oficina;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A ficha de um chicote para injeção programável (SPEC 6.6.5, Fase 3B): a injeção, o motor, a versão do chicote, de
 * onde veio o pinout, os pontos, os materiais, as observações e a situação. Uma por chicote, da OS; a versão nova não
 * apaga a anterior. Os testes de bancada vêm do checklist da OS, e as fotos, dos anexos. O app não valida pinagem nem
 * bitola.
 *
 * @param injecao o nome da injeção (do catálogo, como estava, ou o texto livre); vazio = não informada
 * @param injecaoItemId a injeção do catálogo de injeções; null = texto livre
 * @param motorVersaoId a versão do motor do projeto de preparação da OS; null = sem projeto (ou não escolhida)
 * @param versaoChicote o nome da versão (ex.: "v1")
 * @param fontePinout de onde veio o pinout (manual da injeção, modelo do fabricante), obrigatório
 */
public record FichaChicote(String injecao, Long injecaoItemId, Long motorVersaoId, String versaoChicote,
        String fontePinout, List<PontoChicote> pontos, String materiais, String observacoes,
        SituacaoChicote situacao) {

    /** A versão de quem não escreveu outra. */
    public static final String PRIMEIRA_VERSAO = "v1";

    private static final Pattern VERSAO_NUMERADA = Pattern.compile("(?i)v\\s*(\\d+)");

    public FichaChicote {
        injecao = limpo(injecao);
        versaoChicote = limpo(versaoChicote);
        fontePinout = limpo(fontePinout);
        pontos = pontos == null ? List.of() : List.copyOf(pontos);
        materiais = limpo(materiais);
        observacoes = limpo(observacoes);
    }

    /** O que está errado, em português, tudo de uma vez; vazio = pode gravar. */
    public List<String> validar() {
        List<String> erros = new ArrayList<>();
        if (versaoChicote.isEmpty()) {
            erros.add("Escreva a versão do chicote (ex.: v1).");
        }
        if (fontePinout.isEmpty()) {
            erros.add("Escreva de onde veio o pinout (ex.: o manual da injeção, o modelo do fabricante).");
        }
        for (int i = 0; i < pontos.size(); i++) {
            if (pontos.get(i).funcao().isEmpty()) {
                erros.add("Ponto " + (i + 1) + ": escreva a função do fio (ex.: bico do cilindro 1, TPS).");
            }
        }
        if (situacao == null) {
            erros.add("Escolha a situação do chicote (em confecção ou testado).");
        }
        return erros;
    }

    /**
     * A ficha da versão nova, para começar dela: tudo igual, a próxima versão ("v1" vira "v2"; outro nome ganha " v2")
     * e a situação de volta a em confecção.
     */
    public FichaChicote versaoNova() {
        Matcher numerada = VERSAO_NUMERADA.matcher(versaoChicote);
        String proxima = numerada.matches() ? "v" + (Integer.parseInt(numerada.group(1)) + 1)
                : (versaoChicote + " v2").strip();
        return new FichaChicote(injecao, injecaoItemId, motorVersaoId, proxima, fontePinout, pontos, materiais,
                observacoes, SituacaoChicote.EM_CONFECCAO);
    }

    /** "a definir" sem situação; senão, o rótulo dela. */
    public String rotuloDaSituacao() {
        return situacao == null ? "a definir" : situacao.rotulo();
    }

    private static String limpo(String texto) {
        return texto == null ? "" : texto.strip();
    }
}
