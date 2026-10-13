package br.com.calibra.core.pecas;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Turbo do catálogo (SPEC 7.3 e 8.5), com o mapa do compressor digitalizado da imagem do fabricante. Os dados vêm
 * do fabricante ou do usuário, nunca do app.
 *
 * @param mapa null enquanto o mapa não foi digitalizado (o semáforo fica amarelo: "turbo sem mapa")
 * @param arCaixaQuente A/R da caixa quente; opcional
 * @param imagemMapa nome do arquivo da imagem do mapa, na pasta de anexos dos turbos; null sem imagem
 */
public record Turbo(String nome, MapaCompressor mapa, Double arCaixaQuente, String imagemMapa, String fonte,
        LocalDate data) {

    /** O compromisso da caixa quente (SPEC 7.3), mostrado junto da escolha do turbo. */
    public static final String COMPROMISSO_AR = "A/R da caixa quente: menor faz a pressão chegar mais cedo e limita o "
            + "pico; maior faz o contrário.";

    public Turbo {
        Objects.requireNonNull(nome, "nome");
        Objects.requireNonNull(fonte, "fonte");
    }

    public List<String> validar() {
        List<String> erros = new ArrayList<>();
        if (arCaixaQuente != null && !(arCaixaQuente > 0)) {
            erros.add("O A/R da caixa quente deve ser maior que zero.");
        }
        if (mapa != null) {
            erros.addAll(mapa.validar());
        }
        return erros;
    }
}
