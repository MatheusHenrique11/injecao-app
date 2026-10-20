package br.com.calibra.relatorios;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.core.oficina.FichaChicote;
import br.com.calibra.core.oficina.SituacaoItem;

/**
 * Tudo que entra no PDF da ficha do chicote (Fase 3B, SPEC 6.6.5).
 *
 * @param logo o arquivo do logo da oficina; null sem logo
 * @param numeroOs ex.: "OS-2026-0001"
 * @param cliente o nome como está no cadastro (anonimizado, se o cliente foi anonimizado)
 * @param veiculo ex.: "VW Gol GTi 1994 · ABC1D23"
 * @param motor a versão do motor do projeto (ex.: "v2 · AP 2.0 turbo"); vazio sem projeto
 * @param bancada os itens do checklist dos serviços de bancada da OS, como estão marcados (os testes de bancada)
 */
public record DadosFichaChicote(DadosOficina oficina, Path logo, String numeroOs, String cliente, String veiculo,
        String motor, FichaChicote ficha, List<ItemDeBancada> bancada, String versaoApp, LocalDateTime geradoEm) {

    /**
     * Um item do checklist de bancada, como está marcado na aba Execução.
     *
     * @param marcadoPor o nome do mecânico; vazio se ninguém foi escolhido ou o item está pendente
     * @param marcadoEm null se nunca foi marcado
     */
    public record ItemDeBancada(String texto, SituacaoItem situacao, String observacao, String justificativa,
            String marcadoPor, LocalDateTime marcadoEm) {

        public ItemDeBancada {
            Objects.requireNonNull(texto, "texto");
            Objects.requireNonNull(situacao, "situacao");
            observacao = observacao == null ? "" : observacao.strip();
            justificativa = justificativa == null ? "" : justificativa.strip();
            marcadoPor = marcadoPor == null ? "" : marcadoPor.strip();
        }
    }

    public DadosFichaChicote {
        oficina = oficina == null ? DadosOficina.vazia() : oficina;
        Objects.requireNonNull(numeroOs, "numeroOs");
        Objects.requireNonNull(cliente, "cliente");
        Objects.requireNonNull(veiculo, "veiculo");
        motor = motor == null ? "" : motor.strip();
        Objects.requireNonNull(ficha, "ficha");
        bancada = bancada == null ? List.of() : List.copyOf(bancada);
        Objects.requireNonNull(versaoApp, "versaoApp");
        Objects.requireNonNull(geradoEm, "geradoEm");
    }
}
