package br.com.calibra.app.catalogo;

import java.util.List;
import java.util.Optional;

import br.com.calibra.core.combustivel.CombustiveisPadrao;
import br.com.calibra.core.combustivel.Combustivel;

/**
 * Os combustíveis que o formulário do motor oferece e como achar um pelo código (o motor grava o código). No app
 * eles vêm do catálogo, na versão atual; sem banco (testes), são os da SPEC 7.2.
 */
public interface Combustiveis {

    /** Os que podem ser escolhidos, em ordem de nome. */
    List<Combustivel> ativos();

    /** Pelo código, mesmo arquivado: um motor antigo pode usar um combustível que saiu das escolhas. */
    Optional<Combustivel> porCodigo(String codigo);

    /** Os 6 da SPEC 7.2, iguais às sementes do catálogo. */
    static Combustiveis padrao() {
        return new Combustiveis() {
            @Override
            public List<Combustivel> ativos() {
                return CombustiveisPadrao.todos();
            }

            @Override
            public Optional<Combustivel> porCodigo(String codigo) {
                return CombustiveisPadrao.porCodigo(codigo);
            }
        };
    }
}
