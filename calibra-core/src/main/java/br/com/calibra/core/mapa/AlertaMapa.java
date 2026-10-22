package br.com.calibra.core.mapa;

import java.util.Objects;

import br.com.calibra.core.semaforo.Cor;

/**
 * Um alerta do mapa base, com a cor do semáforo (SPEC 2.4 e 7.5). O vermelho só passa com a justificativa escrita.
 */
public record AlertaMapa(Codigo codigo, Cor cor, String mensagem) {

    /** O que o alerta conta. A justificativa de um vermelho fica gravada com este código. */
    public enum Codigo {
        IGNICAO_SEM_FONTE("Ignição sem modelo"),
        IGNICAO_INCOMPLETA("Ignição incompleta"),
        REDUCOES_IGNICAO("Reduções de ignição"),
        DC_ALTO("DC do bico no mapa"),
        TI_BAIXO("TI na carga baixa"),
        EIXO_CURTO("Eixo da injeção"),
        TABELA_POR_TPS("Tabela por TPS"),
        DUAS_BANCADAS("Duas bancadas de bicos"),
        PERFIL_INCOMPLETO("Perfil da injeção"),
        MODELO_VE("Modelo de VE");

        private final String rotulo;

        Codigo(String rotulo) {
            this.rotulo = rotulo;
        }

        public String rotulo() {
            return rotulo;
        }
    }

    public AlertaMapa {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(cor, "cor");
        Objects.requireNonNull(mensagem, "mensagem");
    }
}
