package br.com.calibra.dados.projeto;

import java.time.LocalDateTime;
import java.util.List;

import br.com.calibra.core.calculo.Dimensionamento;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.semaforo.Justificativa;

/**
 * Um cálculo salvo no projeto (regra 2.5): a entrada, o resultado com a memória e os avisos, as justificativas
 * dos vermelhos, a versão do núcleo e a do motor usada. Nunca muda.
 */
public record CalculoSalvo(
        long id,
        long projetoId,
        long versaoMotorId,
        int numeroVersaoMotor,
        EntradaDimensionamento entrada,
        Resultado<Dimensionamento> resultado,
        List<Justificativa> justificativas,
        String versaoNucleo,
        String versaoApp,
        LocalDateTime criadoEm) {

    public CalculoSalvo {
        justificativas = List.copyOf(justificativas);
    }
}
