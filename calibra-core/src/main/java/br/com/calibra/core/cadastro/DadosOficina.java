package br.com.calibra.core.cadastro;

import java.util.ArrayList;
import java.util.List;

/**
 * Dados da oficina que aparecem nos PDFs (SPEC 8, Configurações). Todos opcionais.
 *
 * @param logo nome do arquivo do logo dentro da pasta de anexos, ou null
 */
public record DadosOficina(String nome, String cnpj, String endereco, String telefone, String email, String logo) {

    public DadosOficina {
        nome = limpo(nome);
        cnpj = limpo(cnpj);
        endereco = limpo(endereco);
        telefone = limpo(telefone);
        email = limpo(email);
        logo = logo == null || logo.isBlank() ? null : logo.strip();
    }

    public static DadosOficina vazia() {
        return new DadosOficina("", "", "", "", "", null);
    }

    /** Erros de digitação: CNPJ com dígito errado ou e-mail sem @. */
    public List<String> validar() {
        List<String> erros = new ArrayList<>();
        if (!cnpj.isEmpty() && !Documento.cnpjValido(cnpj)) {
            erros.add("CNPJ da oficina inválido: confira os números (dígitos verificadores).");
        }
        if (!email.isEmpty() && !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            erros.add("E-mail da oficina inválido.");
        }
        return erros;
    }

    private static String limpo(String texto) {
        return texto == null ? "" : texto.strip();
    }
}
