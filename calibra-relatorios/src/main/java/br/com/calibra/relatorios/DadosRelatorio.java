package br.com.calibra.relatorios;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import br.com.calibra.core.calculo.Dimensionamento;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.semaforo.Justificativa;

/**
 * Tudo que entra no PDF da memória de cálculo.
 *
 * @param justificativas uma para cada indicador vermelho (regra 2.4)
 * @param versaoApp versão do Calibra que gerou o PDF
 * @param geradoEm data e hora da geração
 * @param projeto cliente, veículo e motor do projeto; null na calculadora avulsa
 */
public record DadosRelatorio(
        EntradaDimensionamento entrada,
        Resultado<Dimensionamento> resultado,
        List<Justificativa> justificativas,
        String versaoApp,
        LocalDateTime geradoEm,
        IdentificacaoProjeto projeto) {

    public DadosRelatorio {
        Objects.requireNonNull(entrada, "entrada");
        Objects.requireNonNull(resultado, "resultado");
        Objects.requireNonNull(versaoApp, "versaoApp");
        Objects.requireNonNull(geradoEm, "geradoEm");
        justificativas = justificativas == null ? List.of() : List.copyOf(justificativas);
    }

    /** Memória de cálculo da calculadora avulsa, sem projeto. */
    public DadosRelatorio(EntradaDimensionamento entrada, Resultado<Dimensionamento> resultado,
            List<Justificativa> justificativas, String versaoApp, LocalDateTime geradoEm) {
        this(entrada, resultado, justificativas, versaoApp, geradoEm, null);
    }
}
