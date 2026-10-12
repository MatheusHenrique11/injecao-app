package br.com.calibra.dados.projeto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import br.com.calibra.core.cadastro.TipoVeiculo;
import br.com.calibra.core.motor.Aspiracao;
import br.com.calibra.core.projeto.StatusProjeto;
import br.com.calibra.core.util.NumeroBr;

/**
 * Uma linha da lista de projetos (tela Início): cliente, veículo, motor na versão atual e status.
 *
 * @param identificacao placa (terrestre) ou nome da embarcação (náutico); pode ser vazio
 * @param cilindradaCc null se não deu para calcular
 */
public record ResumoProjeto(
        long projetoId,
        StatusProjeto status,
        String cliente,
        TipoVeiculo tipoVeiculo,
        String veiculo,
        String identificacao,
        String descricaoMotor,
        int cilindros,
        Double cilindradaCc,
        Aspiracao aspiracao,
        int versaoMotor,
        LocalDateTime atualizadoEm) {

    /** Ex.: "AP 2.0 8V · 4 cil. · 2,0 L · Turbo". */
    public String motor() {
        List<String> partes = new ArrayList<>();
        if (descricaoMotor != null && !descricaoMotor.isBlank()) {
            partes.add(descricaoMotor);
        }
        partes.add(cilindros + " cil.");
        if (cilindradaCc != null) {
            partes.add(NumeroBr.formatar(cilindradaCc / 1000.0, 1) + " L");
        }
        if (aspiracao != null) {
            partes.add(aspiracao.rotulo());
        }
        return String.join(" · ", partes);
    }
}
