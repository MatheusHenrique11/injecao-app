package br.com.calibra.dados.projeto;

import java.time.LocalDateTime;

/** Uma linha do histórico de cálculos do projeto, sem a memória (que é lida só ao abrir o cálculo). */
public record ResumoCalculo(
        long id,
        long versaoMotorId,
        int numeroVersaoMotor,
        String versaoNucleo,
        int vermelhos,
        int amarelos,
        LocalDateTime criadoEm) {
}
