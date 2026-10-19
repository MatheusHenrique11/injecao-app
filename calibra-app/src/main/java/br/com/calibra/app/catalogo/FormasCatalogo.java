package br.com.calibra.app.catalogo;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import br.com.calibra.dados.catalogo.TipoCatalogo;

/** O formulário de cada tipo de catálogo, na ordem das abas da tela. */
public final class FormasCatalogo {

    private static final List<FormularioCatalogo<?>> TODOS = List.of(
            new FormularioCatalogo<>(new FormaBico()),
            new FormularioCatalogo<>(new FormaBomba()),
            new FormularioCatalogo<>(new FormaInjecao()),
            new FormularioCatalogo<>(new FormaTurbo()),
            new FormularioCatalogo<>(new FormaCombustivel()),
            new FormularioCatalogo<>(new FormaIntercooler()),
            new FormularioCatalogo<>(new FormaSensor()),
            new FormularioCatalogo<>(new FormaCambio()),
            new FormularioCatalogo<>(new FormaMotor()),
            new FormularioCatalogo<>(new FormaServico()),
            new FormularioCatalogo<>(new FormaItem()),
            new FormularioCatalogo<>(new FormaKit()));

    private static final Map<TipoCatalogo<?>, FormularioCatalogo<?>> POR_TIPO =
            TODOS.stream().collect(Collectors.toUnmodifiableMap(FormularioCatalogo::tipo, Function.identity()));

    private FormasCatalogo() {
    }

    /** Os formulários na ordem das abas. */
    public static List<FormularioCatalogo<?>> todos() {
        return TODOS;
    }

    @SuppressWarnings("unchecked")
    public static <T> FormularioCatalogo<T> de(TipoCatalogo<T> tipo) {
        FormularioCatalogo<?> formulario = POR_TIPO.get(tipo);
        if (formulario == null) {
            throw new IllegalArgumentException("Sem formulário para o catálogo " + tipo.codigo() + ".");
        }
        // O mapa só liga cada tipo ao formulário do mesmo tipo (montado acima).
        return (FormularioCatalogo<T>) formulario;
    }
}
