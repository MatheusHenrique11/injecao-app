package br.com.calibra.core.calculo;

/** A iteração MAP ↔ Tm não convergiu no máximo de passos (SPEC 7.1: "se não convergir, erro"). */
public final class CalculoNaoConvergiuException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CalculoNaoConvergiuException(String mensagem) {
        super(mensagem);
    }
}
