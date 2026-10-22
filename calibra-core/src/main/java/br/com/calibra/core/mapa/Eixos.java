package br.com.calibra.core.mapa;

import java.util.ArrayList;
import java.util.List;

/** A conferência dos pontos de um eixo de tabela: quantos, em que faixa e sempre crescendo. */
final class Eixos {

    private Eixos() {
    }

    /** As mensagens do que está errado no eixo; vazio se estiver certo. */
    static List<String> validar(String qual, List<Double> pontos, double minimo, double maximo) {
        List<String> erros = new ArrayList<>();
        if (pontos.size() < 2) {
            erros.add(qual + ": informe pelo menos 2 pontos.");
            return erros;
        }
        if (pontos.size() > PerfilTabela.MAXIMO_PONTOS) {
            erros.add(qual + ": no máximo " + PerfilTabela.MAXIMO_PONTOS + " pontos.");
        }
        for (int i = 0; i < pontos.size(); i++) {
            double ponto = pontos.get(i);
            if (!(ponto >= minimo && ponto <= maximo)) {
                erros.add(qual + ": o ponto " + (i + 1) + " está fora da faixa aceita.");
                return erros;
            }
            if (i > 0 && ponto <= pontos.get(i - 1)) {
                erros.add(qual + ": os pontos precisam crescer, do menor para o maior (o ponto " + (i + 1)
                        + " não é maior que o anterior).");
                return erros;
            }
        }
        return erros;
    }
}
