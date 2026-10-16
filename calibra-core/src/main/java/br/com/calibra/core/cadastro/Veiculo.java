package br.com.calibra.core.cadastro;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Veículo (SPEC 6.2), editável a qualquer momento; o histórico das mudanças fica no banco. Campos
 * opcionais vazios são texto vazio ou null.
 *
 * @param placa só terrestres; guardada em maiúsculas, sem traço nem espaço
 * @param nomeEmbarcacao só náuticos (também {@code numeroInscricao} e {@code numeroCasco})
 * @param usoAcumulado km nos terrestres, horas de motor nos náuticos ({@link TipoVeiculo#unidadeUso()})
 * @param cambioControladoPelaInjecao só para automático, automatizado e dupla embreagem
 * @param tracao só terrestres
 */
public record Veiculo(
        TipoVeiculo tipo,
        String marca,
        String modelo,
        String versao,
        Integer anoFabricacao,
        Integer anoModelo,
        String placa,
        String chassi,
        String nomeEmbarcacao,
        String numeroInscricao,
        String numeroCasco,
        Double usoAcumulado,
        TipoCambio cambio,
        boolean cambioControladoPelaInjecao,
        Tracao tracao,
        Double pesoKg,
        String cor,
        String observacoes) {

    /** Ano mais antigo aceito (SPEC 6.2). */
    public static final int ANO_MINIMO = 1900;

    public Veiculo {
        marca = Contato.limpo(marca);
        modelo = Contato.limpo(modelo);
        versao = Contato.limpo(versao);
        placa = semEspacos(placa).replace("-", "");
        chassi = semEspacos(chassi);
        nomeEmbarcacao = Contato.limpo(nomeEmbarcacao);
        numeroInscricao = Contato.limpo(numeroInscricao);
        numeroCasco = Contato.limpo(numeroCasco);
        cor = Contato.limpo(cor);
        observacoes = Contato.limpo(observacoes);
    }

    /**
     * Erros de cadastro, em português (regras da SPEC 6.2). {@code anoAtual} vem de fora: o núcleo não lê o
     * relógio.
     */
    public List<String> validar(int anoAtual) {
        List<String> erros = new ArrayList<>();
        if (tipo == null) {
            erros.add("Escolha o tipo do veículo.");
        }
        if (marca.isEmpty()) {
            erros.add("Informe a marca do veículo.");
        }
        if (modelo.isEmpty()) {
            erros.add("Informe o modelo do veículo.");
        }
        conferirAno(anoFabricacao, "de fabricação", anoAtual, erros);
        conferirAno(anoModelo, "do modelo", anoAtual, erros);
        if (tipo != null && tipo.nautico()) {
            if (!placa.isEmpty() || !chassi.isEmpty()) {
                erros.add("Placa e chassi são só para veículos terrestres.");
            }
            if (tracao != null) {
                erros.add("Tração é só para veículos terrestres.");
            }
        }
        if (tipo != null && tipo.terrestre()
                && (!nomeEmbarcacao.isEmpty() || !numeroInscricao.isEmpty() || !numeroCasco.isEmpty())) {
            erros.add("Nome da embarcação, número de inscrição e número do casco são só para embarcações.");
        }
        if (tipo != null && cambio != null && cambio.nautico() != tipo.nautico()) {
            erros.add("O câmbio \"" + cambio.rotulo() + "\" não combina com " + tipo.rotulo().toLowerCase(Locale.ROOT)
                    + ".");
        }
        if (cambioControladoPelaInjecao && (cambio == null || !cambio.podeSerControladoPelaInjecao())) {
            erros.add("Câmbio controlado pela injeção só vale para câmbio automático, automatizado ou de dupla "
                    + "embreagem.");
        }
        if (usoAcumulado != null && !(usoAcumulado >= 0)) {
            erros.add("O uso acumulado não pode ser negativo.");
        }
        if (pesoKg != null && !(pesoKg > 0)) {
            erros.add("O peso do veículo deve ser maior que zero.");
        }
        return erros;
    }

    /**
     * O mesmo veículo só com os campos que o tipo usa: ao trocar de carro para lancha, a placa, o chassi, a
     * tração e um câmbio de carro saem (a tela não mostra esses campos para lancha), e vice-versa.
     */
    public Veiculo ajustadoAoTipo() {
        if (tipo == null) {
            return this;
        }
        boolean nautico = tipo.nautico();
        TipoCambio cambioDoTipo = cambio != null && cambio.nautico() == nautico ? cambio : null;
        boolean controlado = cambioControladoPelaInjecao && cambioDoTipo != null
                && cambioDoTipo.podeSerControladoPelaInjecao();
        return new Veiculo(tipo, marca, modelo, versao, anoFabricacao, anoModelo,
                nautico ? "" : placa, nautico ? "" : chassi,
                nautico ? nomeEmbarcacao : "", nautico ? numeroInscricao : "", nautico ? numeroCasco : "",
                usoAcumulado, cambioDoTipo, controlado, nautico ? null : tracao, pesoKg, cor, observacoes);
    }

    /** O mesmo veículo com outro uso acumulado (km ou horas de motor), como na entrada de uma OS. */
    public Veiculo comUsoAcumulado(Double uso) {
        return new Veiculo(tipo, marca, modelo, versao, anoFabricacao, anoModelo, placa, chassi, nomeEmbarcacao,
                numeroInscricao, numeroCasco, uso, cambio, cambioControladoPelaInjecao, tracao, pesoKg, cor, observacoes);
    }

    /** Marca, modelo e versão: "VW Gol GTi". */
    public String descricao() {
        return Stream.of(marca, modelo, versao).filter(texto -> !texto.isEmpty()).collect(Collectors.joining(" "));
    }

    /** O que identifica o veículo numa lista: a placa nos terrestres, o nome nos náuticos. */
    public String identificacao() {
        return tipo != null && tipo.nautico() ? nomeEmbarcacao : placa;
    }

    private static void conferirAno(Integer ano, String qual, int anoAtual, List<String> erros) {
        if (ano != null && (ano < ANO_MINIMO || ano > anoAtual + 1)) {
            erros.add("O ano " + qual + " deve ficar entre " + ANO_MINIMO + " e " + (anoAtual + 1) + ".");
        }
    }

    private static String semEspacos(String texto) {
        return texto == null ? "" : texto.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }
}
