package br.com.calibra.core.cadastro;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente da oficina (SPEC 6.1). Dados pessoais mínimos (regra 2.8): nome e telefone obrigatórios; e-mail
 * e CPF ou CNPJ opcionais; consentimento com data e finalidade.
 *
 * @param documento CPF ou CNPJ, opcional; um documento válido fica formatado (000.000.000-00)
 * @param anonimizado os dados pessoais foram apagados (LGPD); ficam o nome genérico e os projetos
 */
public record Cliente(
        String nome,
        String telefone,
        String email,
        String documento,
        Consentimento consentimento,
        String observacoes,
        boolean anonimizado) {

    public Cliente {
        nome = Contato.limpo(nome);
        telefone = Contato.limpo(telefone);
        email = Contato.limpo(email);
        documento = Documento.valido(documento) ? Documento.formatar(documento) : Contato.limpo(documento);
        observacoes = Contato.limpo(observacoes);
    }

    /** Cliente novo (ainda com os dados pessoais). */
    public static Cliente novo(String nome, String telefone, String email, String documento,
            Consentimento consentimento, String observacoes) {
        return new Cliente(nome, telefone, email, documento, consentimento, observacoes, false);
    }

    /** Erros de cadastro, em português. {@code hoje} vem de fora: o núcleo não lê o relógio. */
    public List<String> validar(LocalDate hoje) {
        List<String> erros = new ArrayList<>();
        if (nome.isEmpty()) {
            erros.add("Informe o nome do cliente.");
        }
        if (anonimizado) {
            return erros;
        }
        if (telefone.isEmpty()) {
            erros.add("Informe o telefone do cliente.");
        } else if (Contato.digitos(telefone) < Contato.MINIMO_DIGITOS_TELEFONE) {
            erros.add("Telefone do cliente incompleto: confira os números.");
        }
        if (!email.isEmpty() && !Contato.emailValido(email)) {
            erros.add("E-mail do cliente inválido.");
        }
        if (!documento.isEmpty() && !Documento.valido(documento)) {
            erros.add("CPF ou CNPJ do cliente inválido: confira os números (dígitos verificadores).");
        }
        if (consentimento == null) {
            erros.add("Registre o consentimento do cliente: data e finalidade do uso dos dados (LGPD).");
        } else {
            erros.addAll(consentimento.validar(hoje));
        }
        return erros;
    }

    /**
     * O mesmo cliente sem os dados pessoais (SPEC 6.1): o nome vira "Cliente anonimizado #id" e telefone,
     * e-mail, documento e observações são apagados. O consentimento fica, porque diz por que os dados
     * existiram e não identifica ninguém.
     */
    public Cliente anonimizar(long id) {
        return new Cliente("Cliente anonimizado #" + id, "", "", "", consentimento, "", true);
    }
}
