package br.com.calibra.core.cadastro;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Um item instalado ou modificado no veículo, pela oficina ou informado pelo cliente (SPEC 6.2, Fase 3B): o histórico
 * do que já foi mexido no carro, para a garantia e o diagnóstico. A OS de origem e a retirada ficam no banco. Não é
 * dado pessoal: fica igual quando o cliente é anonimizado.
 *
 * @param potenciaOuEspecificacao texto livre, como no manual ou na nota (ex.: "800 W RMS"); o app não confere
 * @param dataInstalacao null = sem data (ex.: o item informado pelo cliente)
 */
public record Instalacao(TipoInstalacao tipo, String descricao, String marca, String modelo,
        String potenciaOuEspecificacao, LocalDate dataInstalacao, String observacoes) {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public Instalacao {
        descricao = Contato.limpo(descricao);
        marca = Contato.limpo(marca);
        modelo = Contato.limpo(modelo);
        potenciaOuEspecificacao = Contato.limpo(potenciaOuEspecificacao);
        observacoes = Contato.limpo(observacoes);
    }

    /**
     * O que está errado, em português, tudo de uma vez; vazio = pode gravar. {@code hoje} vem de fora: o núcleo não lê
     * o relógio.
     *
     * @param retiradaEm a data em que o item saiu do veículo; null se ainda está nele
     */
    public List<String> validar(LocalDate hoje, LocalDate retiradaEm) {
        List<String> erros = validarCampos();
        if (dataInstalacao != null && dataInstalacao.isAfter(hoje)) {
            erros.add("A data da instalação não pode estar no futuro.");
        }
        if (dataInstalacao != null && retiradaEm != null && dataInstalacao.isAfter(retiradaEm)) {
            erros.add("A data da instalação não pode ser depois da retirada (" + retiradaEm.format(DIA) + ").");
        }
        return erros;
    }

    /** Só os campos obrigatórios (o tipo e a descrição), sem as datas: o que a janela confere junto com a digitação. */
    public List<String> validarCampos() {
        List<String> erros = new ArrayList<>();
        if (tipo == null) {
            erros.add("Escolha o tipo da instalação.");
        }
        if (descricao.isEmpty()) {
            erros.add("Descreva o que foi instalado e onde.");
        }
        return erros;
    }

    /** O que impede a retirada nesta data: nem no futuro, nem antes da instalação. Vazio = pode retirar. */
    public List<String> validarRetirada(LocalDate retirada, LocalDate hoje) {
        List<String> erros = new ArrayList<>();
        if (retirada == null) {
            erros.add("Informe a data em que o item saiu do veículo.");
            return erros;
        }
        if (retirada.isAfter(hoje)) {
            erros.add("A data da retirada não pode estar no futuro.");
        }
        if (dataInstalacao != null && retirada.isBefore(dataInstalacao)) {
            erros.add("A retirada não pode ser antes da instalação (" + dataInstalacao.format(DIA) + ").");
        }
        return erros;
    }

    /** "Pioneer DEH-X50BR", só a marca ou só o modelo; vazio sem nenhum dos dois. */
    public String marcaEModelo() {
        return (marca + " " + modelo).strip();
    }
}
