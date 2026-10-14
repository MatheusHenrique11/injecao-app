package br.com.calibra.core.orcamento;

import java.math.BigDecimal;

/**
 * Desconto sobre o total de peças e serviços: em valor (reais) ou em percentual, nunca os dois.
 *
 * @param valor o desconto em reais; null no percentual ou sem desconto
 * @param percentual o desconto em % (ex.: 10 = 10%); null no valor ou sem desconto
 */
public record Desconto(Dinheiro valor, BigDecimal percentual) {

    public static final Desconto NENHUM = new Desconto(null, null);

    public Desconto {
        if (valor != null && percentual != null) {
            throw new IllegalArgumentException("Desconto em valor ou em percentual, não os dois.");
        }
        percentual = percentual == null ? null : percentual.stripTrailingZeros();
        if (percentual != null && percentual.scale() < 0) {
            percentual = percentual.setScale(0);
        }
    }

    public static Desconto emValor(Dinheiro valor) {
        return new Desconto(valor, null);
    }

    public static Desconto emPercentual(BigDecimal percentual) {
        return new Desconto(null, percentual);
    }

    public boolean nenhum() {
        return valor == null && percentual == null;
    }

    /** Quanto o desconto tira deste subtotal (o percentual arredonda no centavo). */
    public Dinheiro sobre(Dinheiro subtotal) {
        if (valor != null) {
            return valor;
        }
        return percentual == null ? Dinheiro.ZERO : subtotal.percentual(percentual);
    }
}
