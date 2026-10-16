package br.com.calibra.core.oficina;

/** Número da ordem de serviço: OS-AAAA-NNNN, com a sequência recomeçando a cada ano (SPEC 6.5). */
public record NumeroOs(int ano, int sequencial) {

    public NumeroOs {
        if (ano < 2000 || sequencial < 1) {
            throw new IllegalArgumentException("Número de OS inválido: " + ano + "/" + sequencial);
        }
    }

    /** "OS-2026-0007". */
    public String texto() {
        return "OS-%d-%04d".formatted(ano, sequencial);
    }
}
