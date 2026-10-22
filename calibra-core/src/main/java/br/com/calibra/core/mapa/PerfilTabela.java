package br.com.calibra.core.mapa;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Uma tabela do software da injeção, como o mecânico vê na tela dela (SPEC 9): o nome, para que serve, os pontos de
 * rotação e de carga, a unidade e a orientação. Vem do software e do manual da injeção, nunca do app (SPEC 2.7).
 *
 * @param nome o nome da tabela no software (ex.: "Mapa principal de injeção")
 * @param unidade a unidade das células no software (ex.: "ms", "%", "°"); pode ficar vazia
 * @param rotacoes os pontos de rotação (rpm), do menor para o maior
 * @param cargas os pontos de carga (kPa absolutos ou % de TPS), do menor para o maior
 */
public record PerfilTabela(
        String nome,
        FuncaoTabela funcao,
        TipoCarga tipoCarga,
        String unidade,
        List<Double> rotacoes,
        List<Double> cargas,
        OrientacaoTabela orientacao) {

    /** Limite de pontos por eixo: as injeções do mercado ficam bem abaixo. */
    public static final int MAXIMO_PONTOS = 64;

    public PerfilTabela {
        Objects.requireNonNull(funcao, "funcao");
        Objects.requireNonNull(tipoCarga, "tipoCarga");
        Objects.requireNonNull(orientacao, "orientacao");
        nome = nome == null ? "" : nome.strip();
        unidade = unidade == null ? "" : unidade.strip();
        rotacoes = rotacoes == null ? List.of() : List.copyOf(rotacoes);
        cargas = cargas == null ? List.of() : List.copyOf(cargas);
    }

    /** O que está errado nesta tabela, em português; vazio se estiver certa. */
    public List<String> validar() {
        List<String> erros = new ArrayList<>();
        String qual = "Tabela de " + funcao.rotulo().toLowerCase();
        if (nome.isEmpty()) {
            erros.add(qual + ": informe o nome dela no software da injeção.");
        }
        erros.addAll(Eixos.validar(qual + ", pontos de rotação", rotacoes, 1, 20000));
        if (tipoCarga == TipoCarga.MAP_KPA) {
            erros.addAll(Eixos.validar(qual + ", pontos de carga", cargas, 1, 1000));
        } else {
            erros.addAll(Eixos.validar(qual + ", pontos de carga", cargas, 0, 100));
        }
        return erros;
    }
}
