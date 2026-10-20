package br.com.calibra.relatorios;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;

import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.core.oficina.LaudoEletrico;

/**
 * Tudo que entra no PDF do laudo de diagnóstico elétrico (Fase 3B, SPEC 6.6.3).
 *
 * @param logo o arquivo do logo da oficina; null sem logo
 * @param numeroOs ex.: "OS-2026-0001"
 * @param cliente o nome como está no cadastro (anonimizado, se o cliente foi anonimizado)
 * @param veiculo ex.: "VW Gol GTi 1994 · ABC1D23"
 * @param mecanico o nome; vazio sem mecânico
 */
public record DadosLaudo(DadosOficina oficina, Path logo, String numeroOs, String cliente, String veiculo,
        String mecanico, LaudoEletrico laudo, String versaoApp, LocalDateTime geradoEm) {

    public DadosLaudo {
        oficina = oficina == null ? DadosOficina.vazia() : oficina;
        Objects.requireNonNull(numeroOs, "numeroOs");
        Objects.requireNonNull(cliente, "cliente");
        Objects.requireNonNull(veiculo, "veiculo");
        mecanico = mecanico == null ? "" : mecanico.strip();
        Objects.requireNonNull(laudo, "laudo");
        Objects.requireNonNull(versaoApp, "versaoApp");
        Objects.requireNonNull(geradoEm, "geradoEm");
    }
}
