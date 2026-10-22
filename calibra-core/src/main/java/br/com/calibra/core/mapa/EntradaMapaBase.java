package br.com.calibra.core.mapa;

import java.util.Objects;

import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.pecas.Injecao;

/**
 * Tudo que o gerador do mapa base precisa (SPEC 7.5; decisão 294).
 *
 * @param dimensionamento a entrada do cálculo do projeto: motor, meta, combustível, bico, VEs, lambda e padrões
 * @param injecao opcional: a injeção, com o perfil (eixos, modelo de combustível, temperatura de referência); sem
 *     ela, os eixos padrão da SPEC e a tabela em ms
 * @param modeloVe o modelo de VE da família
 * @param ignicao opcional: a fonte do avanço; sem ela, a ignição sai vazia e vermelha
 * @param pressaoMolaWastegateKpa opcional: a pressão (relativa) da mola da wastegate
 */
public record EntradaMapaBase(
        EntradaDimensionamento dimensionamento,
        Injecao injecao,
        ModeloVe modeloVe,
        FonteIgnicao ignicao,
        RegrasMapaBase regras,
        Double pressaoMolaWastegateKpa) {

    public EntradaMapaBase {
        Objects.requireNonNull(dimensionamento, "dimensionamento");
        Objects.requireNonNull(modeloVe, "modeloVe");
        Objects.requireNonNull(regras, "regras");
    }
}
