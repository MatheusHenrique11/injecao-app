package br.com.calibra.relatorios;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.core.calculo.Dimensionamento;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.orcamento.NumeroOrcamento;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.orcamento.StatusOrcamento;
import br.com.calibra.core.semaforo.Justificativa;

/**
 * Tudo que entra no PDF e no texto de WhatsApp do orçamento (bloco 2.5). O orçamento da OS sem projeto (bloco 3.3)
 * não tem a parte de preparação: sai sem o motor, a meta, o resumo técnico e a nota do mapa base.
 *
 * @param logo o arquivo do logo da oficina; null sem logo
 * @param cliente o nome como está no cadastro (anonimizado, se o cliente foi anonimizado)
 * @param veiculo ex.: "VW Gol GTi 1994 · ABC1D23"
 * @param emissao a data do envio; no rascunho, a data da geração (a validade conta a partir dela)
 * @param preparacao o motor e o cálculo do orçamento de preparação; null no orçamento da OS sem projeto
 */
public record DadosOrcamento(
        DadosOficina oficina,
        Path logo,
        String cliente,
        String veiculo,
        NumeroOrcamento numero,
        StatusOrcamento status,
        Orcamento conteudo,
        LocalDate emissao,
        Preparacao preparacao,
        String versaoApp,
        LocalDateTime geradoEm) {

    /**
     * O que o orçamento de preparação tem a mais: o motor e o cálculo em que ele se baseia.
     *
     * @param motor ex.: "AP 2.0 turbo (v1)"
     * @param entrada o cálculo em que o orçamento se baseia (meta e peças)
     * @param justificativas as dos indicadores vermelhos desse cálculo
     */
    public record Preparacao(String motor, EntradaDimensionamento entrada, Resultado<Dimensionamento> resultado,
            List<Justificativa> justificativas) {

        public Preparacao {
            Objects.requireNonNull(motor, "motor");
            Objects.requireNonNull(entrada, "entrada");
            Objects.requireNonNull(resultado, "resultado");
            justificativas = justificativas == null ? List.of() : List.copyOf(justificativas);
        }
    }

    public DadosOrcamento {
        oficina = oficina == null ? DadosOficina.vazia() : oficina;
        Objects.requireNonNull(cliente, "cliente");
        Objects.requireNonNull(veiculo, "veiculo");
        Objects.requireNonNull(numero, "numero");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(conteudo, "conteudo");
        Objects.requireNonNull(emissao, "emissao");
        Objects.requireNonNull(versaoApp, "versaoApp");
        Objects.requireNonNull(geradoEm, "geradoEm");
    }

    /** Orçamento de preparação (com o motor e o cálculo). */
    public boolean dePreparacao() {
        return preparacao != null;
    }
}
