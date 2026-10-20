package br.com.calibra.core.oficina;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.com.calibra.core.util.TextoBusca;

/**
 * Indícios de dado pessoal num texto livre (LGPD, SPEC 6.6.3, Fase 3B). O laudo é dado técnico e fica quando o cliente
 * é anonimizado; por isso a queixa não deve levar o nome, o telefone, o documento ou o e-mail dele. É só um aviso: o
 * app não tem como ter certeza, nem para mais nem para menos.
 */
public final class DadosPessoais {

    /** Telefone, CPF ou CNPJ: uma sequência de dígitos separados só por ponto, traço, parênteses ou espaço. */
    private static final Pattern NUMERO = Pattern.compile("[\\d(][\\d.\\-() ]*\\d");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    private static final Pattern NAO_LETRA = Pattern.compile("[^\\p{L}\\p{N}]");
    /** Palavras de ligação do nome com 3 letras, que também aparecem em qualquer texto. */
    private static final Set<String> LIGACOES = Set.of("das", "dos");
    private static final int MINIMO_DE_DIGITOS = 8;
    private static final int MINIMO_DE_LETRAS = 3;

    private DadosPessoais() {
    }

    /**
     * O que o texto parece ter de dado pessoal, na ordem: o nome do cliente (cada palavra do nome com 3 letras ou
     * mais, sem diferença de acento ou maiúscula), um telefone ou documento (8 dígitos ou mais) e um e-mail. Cada
     * indício diz o que achou, ex.: "o nome do cliente (Ana)". Vazio = nada parecido.
     *
     * @param nomeCliente null para não procurar o nome (ex.: o cliente já anonimizado)
     */
    public static List<String> indicios(String texto, String nomeCliente) {
        List<String> indicios = new ArrayList<>();
        if (texto == null || texto.isBlank()) {
            return indicios;
        }
        palavraDoNome(texto, nomeCliente).ifPresent(palavra -> indicios.add("o nome do cliente (" + palavra + ")"));
        Matcher numero = NUMERO.matcher(texto);
        while (numero.find()) {
            if (numero.group().chars().filter(Character::isDigit).count() >= MINIMO_DE_DIGITOS) {
                indicios.add("um telefone ou documento (" + numero.group().strip() + ")");
                break;
            }
        }
        Matcher email = EMAIL.matcher(texto);
        if (email.find()) {
            indicios.add("um e-mail (" + email.group() + ")");
        }
        return indicios;
    }

    /** A primeira palavra do nome do cliente que aparece no texto, como está escrita no nome. */
    private static Optional<String> palavraDoNome(String texto, String nomeCliente) {
        if (nomeCliente == null || nomeCliente.isBlank()) {
            return Optional.empty();
        }
        Set<String> palavrasDoTexto = new HashSet<>(TextoBusca.palavras(texto));
        for (String palavra : nomeCliente.strip().split("\\s+")) {
            String normalizada = TextoBusca.normalizar(palavra);
            if (normalizada.length() >= MINIMO_DE_LETRAS && !LIGACOES.contains(normalizada)
                    && palavrasDoTexto.contains(normalizada)) {
                return Optional.of(NAO_LETRA.matcher(palavra).replaceAll(""));
            }
        }
        return Optional.empty();
    }
}
