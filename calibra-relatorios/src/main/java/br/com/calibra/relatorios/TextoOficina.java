package br.com.calibra.relatorios;

import java.util.ArrayList;
import java.util.List;

import br.com.calibra.core.cadastro.DadosOficina;

/** Os dados da oficina como aparecem nos documentos: o cabeçalho do PDF e o contato no fim do texto de WhatsApp. */
final class TextoOficina {

    private static final String SEPARADOR = " · ";

    private TextoOficina() {
    }

    /** O nome (ou o aviso para preencher), o CNPJ, o endereço e o contato, um por linha; o que está vazio fica fora. */
    static List<String> cabecalho(DadosOficina oficina) {
        List<String> linhas = new ArrayList<>();
        linhas.add(oficina.nome().isEmpty() ? "Oficina (preencha os dados em Configurações)" : oficina.nome());
        if (!oficina.cnpj().isEmpty()) {
            linhas.add("CNPJ " + oficina.cnpj());
        }
        if (!oficina.endereco().isEmpty()) {
            linhas.add(oficina.endereco());
        }
        List<String> contato = new ArrayList<>();
        if (!oficina.telefone().isEmpty()) {
            contato.add(oficina.telefone());
        }
        if (!oficina.email().isEmpty()) {
            contato.add(oficina.email());
        }
        if (!contato.isEmpty()) {
            linhas.add(String.join(SEPARADOR, contato));
        }
        return linhas;
    }

    /** "Oficina Teste · (11) 99999-0000" (ou só o telefone); vazio sem telefone. */
    static String contato(DadosOficina oficina) {
        if (oficina.telefone().isEmpty()) {
            return "";
        }
        return oficina.nome().isEmpty() ? oficina.telefone() : oficina.nome() + SEPARADOR + oficina.telefone();
    }

    /** " — Oficina Teste", para o título do texto de WhatsApp; vazio sem o nome. */
    static String noTitulo(DadosOficina oficina) {
        return oficina.nome().isEmpty() ? "" : " — " + oficina.nome();
    }
}
