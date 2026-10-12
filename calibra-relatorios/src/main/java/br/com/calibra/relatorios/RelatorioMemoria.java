package br.com.calibra.relatorios;

import static br.com.calibra.core.util.NumeroBr.formatar;
import static br.com.calibra.core.util.NumeroBr.percentual;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import br.com.calibra.core.calculo.Dimensionamento;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.core.combustivel.Combustivel;
import br.com.calibra.core.memoria.Aviso;
import br.com.calibra.core.memoria.Entrada;
import br.com.calibra.core.memoria.Passo;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.motor.ModoInjecao;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.projeto.Meta;
import br.com.calibra.core.projeto.PontoMedicao;
import br.com.calibra.core.semaforo.Avaliacao;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.semaforo.Justificativa;
import br.com.calibra.core.semaforo.LimiaresSemaforo;
import br.com.calibra.core.semaforo.Semaforo;
import br.com.calibra.core.unidades.Conversoes;
import org.apache.pdfbox.pdmodel.PDDocument;

/**
 * PDF da memória de cálculo (SPEC fase 1): entradas, semáforo com justificativas, resultados,
 * recomendações, avisos e cada passo da conta com fórmula e entradas.
 */
public final class RelatorioMemoria {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String SEPARADOR = " · ";

    static final String RESPONSABILIDADE = "Estimativa para dimensionar peças e preparar o mapa base. O acerto final é "
            + "feito pelo calibrador, com wideband, no dinamômetro, na rua ou na água. O Calibra não grava nada na "
            + "injeção. Itens vermelhos só seguem com justificativa escrita.";

    private RelatorioMemoria() {
    }

    /**
     * Gera o PDF.
     *
     * @throws IllegalStateException se houver indicador vermelho sem justificativa (regra 2.4)
     */
    public static void gerar(DadosRelatorio dados, Path destino) throws IOException {
        Dimensionamento d = dados.resultado().valor();
        List<Indicador> pendentes = Semaforo.vermelhosSemJustificativa(d.semaforo(), dados.justificativas());
        if (!pendentes.isEmpty()) {
            throw new IllegalStateException("Há itens vermelhos sem justificativa: "
                    + pendentes.stream().map(Indicador::rotulo).collect(Collectors.joining(", ")) + ".");
        }
        try (PDDocument documento = new PDDocument()) {
            EscritorPdf pdf = new EscritorPdf(documento);
            cabecalho(pdf, dados);
            motor(pdf, dados.entrada().motor());
            metaEParametros(pdf, dados.entrada());
            semaforo(pdf, d, dados.justificativas(), dados.entrada().parametros().limiares());
            resultados(pdf, d);
            listas(pdf, d, dados.resultado());
            memoria(pdf, dados.resultado());
            pdf.terminar("Calibra · Memória de cálculo · gerado em " + dados.geradoEm().format(DATA_HORA));
            // Salva num temporário e troca depois: um PDF aberto no leitor nunca fica pela metade.
            Path temporario = destino.resolveSibling(destino.getFileName() + ".tmp");
            documento.save(temporario.toFile());
            Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void cabecalho(EscritorPdf pdf, DadosRelatorio dados) throws IOException {
        pdf.titulo("Calibra — Memória de cálculo");
        pdf.subtitulo("Gerado em " + dados.geradoEm().format(DATA_HORA) + SEPARADOR + "Calibra " + dados.versaoApp()
                + SEPARADOR + "núcleo " + dados.resultado().versaoNucleo());
        IdentificacaoProjeto projeto = dados.projeto();
        if (projeto != null) {
            pdf.espaco(4f);
            pdf.item("Projeto", "#" + projeto.projetoId());
            pdf.item("Cliente", projeto.cliente());
            pdf.item("Veículo", projeto.veiculo());
            pdf.item("Motor", projeto.motor());
        }
        pdf.espaco(6f);
        pdf.caixa(RESPONSABILIDADE);
    }

    private static void motor(EscritorPdf pdf, ConfiguracaoMotor m) throws IOException {
        pdf.secao("1. Motor");
        String arranjo = m.arranjo().rotulo() + (m.anguloEntreBancadas() != null
                ? " " + formatar(m.anguloEntreBancadas(), 0) + "°" : "");
        pdf.item("Cilindros", m.numeroCilindros() + SEPARADOR + arranjo + SEPARADOR + "cilindrada "
                + formatar(m.cilindradaCc(), 0) + " cm³ (diâmetro " + formatar(m.diametroMm(), 2) + " × curso "
                + formatar(m.cursoMm(), 2) + " mm)"
                + (m.cilindradaInformadaCc() != null
                        ? SEPARADOR + "informada " + formatar(m.cilindradaInformadaCc(), 0) + " cm³" : ""));
        pdf.item("Construção", "taxa " + formatar(m.taxaCompressao(), 1) + ":1" + SEPARADOR + m.valvulasPorCilindro()
                + " válvulas por cilindro" + SEPARADOR + "comando " + m.comando().rotulo().toLowerCase(Locale.ROOT)
                + SEPARADOR + "pistões " + m.pistoes().rotulo().toLowerCase(Locale.ROOT) + SEPARADOR + "bielas "
                + m.bielas().rotulo().toLowerCase(Locale.ROOT));
        pdf.item("Aspiração e rotações", m.aspiracao().rotulo() + SEPARADOR + "máxima " + formatar(m.rotacaoMaxima(), 0)
                + " rpm" + SEPARADOR + "marcha lenta " + formatar(m.rotacaoMarchaLenta(), 0) + " rpm");
        pdf.item("Ignição", "ordem " + m.ordemIgnicao().stream().map(String::valueOf).collect(Collectors.joining("-"))
                + SEPARADOR + "ângulos dos PMS " + m.angulosPms().stream().map(a -> formatar(a, 1))
                        .collect(Collectors.joining("; ")) + "°" + SEPARADOR + m.modoIgnicao().rotulo().toLowerCase(Locale.ROOT));
        pdf.item("Injeção", m.modoInjecao().rotulo() + SEPARADOR + m.bicosPorCilindro() + " bico(s) por cilindro"
                + (m.bicosExtrasCorpoBorboleta() > 0 ? " + " + m.bicosExtrasCorpoBorboleta() + " no corpo de borboleta" : "")
                + SEPARADOR + "roda fônica " + m.rodaFonica());
        Combustivel c = m.combustivel();
        pdf.item("Combustível", c.nome() + SEPARADOR + "AFR " + formatar(c.afrEstequiometrico(), 2) + ":1"
                + SEPARADOR + formatar(c.densidadeKgL(), 3) + " kg/L"
                + (c.vigenteDesde() != null ? SEPARADOR + "vigente desde " + c.vigenteDesde().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "")
                + SEPARADOR + "fonte: " + c.fonte() + ", versão " + c.versao()
                + (m.sensorEtanol() ? SEPARADOR + "com sensor de etanol" : ""));
        pdf.item("Altitude da oficina", formatar(m.altitudeLocalM(), 0) + " m");
    }

    private static void metaEParametros(EscritorPdf pdf, EntradaDimensionamento e) throws IOException {
        pdf.secao("2. Meta e parâmetros");
        Meta meta = e.meta();
        String ponto = meta.pontoMedicao() == PontoMedicao.RODA
                ? "na roda (perda de transmissão de " + formatar(meta.perdaTransmissaoPct(), 1) + "%)"
                : "no virabrequim";
        pdf.item("Meta", formatar(Conversoes.kwParaCv(meta.potenciaKw()), 1) + " cv " + ponto + " a "
                + formatar(meta.rotacaoPotenciaMaxima(), 0) + " rpm"
                + (meta.rotacaoTorqueMaximo() != null ? SEPARADOR + "torque máximo a "
                        + formatar(meta.rotacaoTorqueMaximo(), 0) + " rpm" : "")
                + SEPARADOR + "uso: " + meta.uso().rotulo().toLowerCase(Locale.ROOT));
        pdf.item("Condições", "lambda alvo " + formatar(e.lambdaAlvo(), 2) + SEPARADOR + "VE " + formatar(e.ve(), 2)
                + SEPARADOR + "ar a " + formatar(e.temperaturaAmbienteC(), 1) + " °C" + SEPARADOR
                + "pressão de base " + formatar(Conversoes.kpaParaBar(e.pressaoBaseKpa()), 2) + " bar");
        Parametros p = e.parametros();
        String ar = switch (p.caminhoAr()) {
            case K_AR -> "k_ar " + formatar(p.kAr(), 2) + " hp por lb/min";
            case BSFC -> "BSFC " + formatar(p.bsfc(), 3) + " lb/hp·h";
        };
        pdf.item("Parâmetros", ar + SEPARADOR + "DC máximo " + percentual(p.dcMax(), 0) + SEPARADOR + "eta_c "
                + formatar(p.etaC(), 2) + SEPARADOR + "eps " + formatar(p.eps(), 2) + SEPARADOR + "perdas "
                + formatar(Conversoes.kpaParaPsi(p.perdaDescargaKpa()), 1) + " psi (descarga) e "
                + formatar(Conversoes.kpaParaPsi(p.perdaAdmissaoKpa()), 1) + " psi (admissão)" + SEPARADOR
                + "aspirado: T_amb + " + formatar(p.acrescimoTmAspiradoC(), 1) + " °C" + SEPARADOR
                + "TI mínimo na marcha lenta " + formatar(p.tiMinimoMarchaLentaMs(), 1) + " ms" + SEPARADOR
                + "fluido de teste padrão " + formatar(p.densidadeFluidoTesteKgL(), 2) + " kg/L"
                + (e.motor().modoInjecao() == ModoInjecao.SIMULTANEO
                        ? SEPARADOR + "simultâneo com " + p.gruposSimultaneo() + " grupo(s) de saídas" : ""));
        if (e.bico() != null) {
            pdf.item("Bico informado", descreverBico(e.bico())
                    + (e.bicoSecundario() != null ? "; secundário: " + descreverBico(e.bicoSecundario()) : ""));
        }
        if (e.marchaLenta() != null) {
            pdf.item("Marcha lenta", "MAP " + formatar(e.marchaLenta().mapKpa(), 1) + " kPa" + SEPARADOR + "VE "
                    + formatar(e.marchaLenta().ve(), 2));
        }
        if (e.vazaoBombaDisponivelLh() != null) {
            pdf.item("Bomba informada", formatar(e.vazaoBombaDisponivelLh(), 0) + " L/h na pressão exigida");
        }
        if (e.faixaSensorMapKpa() != null) {
            pdf.item("Sensor MAP informado", formatar(e.faixaSensorMapKpa(), 0) + " kPa absolutos");
        }
        if (e.injecao() != null) {
            pdf.item("Injeção informada", e.injecao().nome() + SEPARADOR + e.injecao().saidasBico()
                    + " saídas de bico" + SEPARADOR + e.injecao().saidasIgnicao() + " saídas de ignição");
        }
    }

    private static String descreverBico(Bico b) {
        String tempoMorto = b.tempoMorto().eFixo()
                ? "tempo morto " + formatar(b.tempoMorto().pontos().getFirst().tempoMs(), 2) + " ms"
                : "tempo morto por tabela (" + b.tempoMorto().pontos().size() + " pontos)";
        return b.nome() + ": " + formatar(b.vazaoNominalCm3Min(), 0) + " cm³/min a "
                + formatar(Conversoes.kpaParaBar(b.pressaoTesteKpa()), 2) + " bar, fluido de teste "
                + formatar(b.densidadeFluidoTesteKgL(), 2) + " kg/L, " + tempoMorto + " (fonte: " + b.fonte() + ")";
    }

    private static void semaforo(EscritorPdf pdf, Dimensionamento d, List<Justificativa> justificativas,
            LimiaresSemaforo limiares) throws IOException {
        pdf.secao("3. Semáforo");
        pdf.item("Limiares", descreverLimiares(limiares));
        pdf.espaco(4f);
        List<Avaliacao> ordenadas = d.semaforo().stream()
                .sorted(Comparator.comparing(Avaliacao::cor).reversed())
                .toList();
        for (Avaliacao a : ordenadas) {
            pdf.semaforo(a.cor(), a.indicador().rotulo(), a.mensagem());
            for (Justificativa j : justificativas) {
                if (j.indicador() == a.indicador()) {
                    pdf.italico("Justificativa (" + j.dataHora().format(DATA_HORA) + "): " + j.texto(), 66f);
                    pdf.espaco(3f);
                }
            }
        }
    }

    /** Os limiares usados no semáforo, para o PDF mostrar com que régua cada cor foi dada. */
    private static String descreverLimiares(LimiaresSemaforo l) {
        String origem = l.equals(LimiaresSemaforo.padrao()) ? "padrão da SPEC 7.4" : "editados";
        return origem + ": DC do bico verde até " + percentual(l.dcVerdeAte(), 0) + " e amarelo até "
                + percentual(l.dcAmareloAte(), 0) + SEPARADOR + "folga da bomba verde a partir de "
                + percentual(l.folgaBombaVerde(), 0) + " e amarela a partir de " + percentual(l.folgaBombaAmarelo(), 0)
                + SEPARADOR + "BMEP de referência " + formatar(l.bmepReferenciaAspiradoBar(), 0) + " bar (aspirado) e "
                + formatar(l.bmepReferenciaSobrealimentadoBar(), 0) + " bar (sobrealimentado), amarela até "
                + percentual(l.bmepFatorAmarelo() - 1.0, 0) + " acima" + SEPARADOR + "pistão verde até "
                + formatar(l.pistaoVerdeAte(), 0) + " m/s e amarelo até " + formatar(l.pistaoAmareloAte(), 0) + " m/s"
                + SEPARADOR + "sensor MAP verde a partir de " + formatar(l.sensorMapVerde(), 1) + " × e amarelo a partir de "
                + formatar(l.sensorMapAmarelo(), 1) + " ×" + SEPARADOR + "aspirado verde até "
                + formatar(l.aspiradoVerdeAte(), 2) + " e amarelo até " + formatar(l.aspiradoAmareloAte(), 2) + " × P_atm";
    }

    private static void resultados(EscritorPdf pdf, Dimensionamento d) throws IOException {
        pdf.secao("4. Resultados");
        pdf.item("Potência", Apresentacao.potencia(d.potencia().virabrequimKw()) + SEPARADOR
                + formatar(d.potencia().especificaCvPorLitro(), 1) + " cv/L");
        Dimensionamento.Combustao c = d.combustao();
        pdf.item("Ar e combustível", "Wa " + Apresentacao.vazaoAr(c.vazaoArKgS()) + SEPARADOR + "combustível "
                + Apresentacao.combustivel(c.combustivelKgH()) + SEPARADOR + "BSFC " + formatar(c.bsfcLbHpH(), 3)
                + " lb/hp·h" + SEPARADOR + "AFR alvo " + formatar(c.afrAlvo(), 2) + ":1");
        Dimensionamento.LadoDoAr ar = d.ladoDoAr();
        String pressoes = "P_atm " + formatar(ar.patmKpa(), 1) + " kPa" + SEPARADOR + "MAP "
                + Apresentacao.pressaoAbsoluta(ar.mapKpa());
        if (ar.sobrealimentado()) {
            pressoes += SEPARADOR + "pressão do turbo " + Apresentacao.pressaoManometrica(Math.max(0, ar.mapKpa() - ar.patmKpa()))
                    + SEPARADOR + "PR " + formatar(ar.razaoPressao(), 2);
        } else {
            pressoes += SEPARADOR + "MAP ÷ P_atm " + formatar(ar.mapSobrePatm(), 3);
        }
        pdf.item("Pressões", pressoes);
        pdf.item("Temperaturas", (ar.sobrealimentado() ? "T2 " + Apresentacao.temperatura(ar.t2C()) + SEPARADOR : "")
                + "Tm " + Apresentacao.temperatura(ar.tmC())
                + (ar.sobrealimentado() ? SEPARADOR + ar.iteracoes() + " passos de iteração" : ""));
        Dimensionamento.BicoNecessario bn = d.bicoNecessario();
        pdf.item("Bico necessário", bn.numeroBicos() + " bicos" + SEPARADOR + formatar(bn.vazaoMassaGMin(), 0)
                + " g/min" + SEPARADOR + formatar(bn.vazaoCombustivelCm3Min(), 0) + " cm³/min de combustível"
                + SEPARADOR + "nominal " + Apresentacao.vazaoBico(bn.vazaoNominalCm3Min(), bn.densidadeTesteKgL())
                + " a " + formatar(Conversoes.kpaParaBar(bn.pressaoTesteKpa()), 2) + " bar (sem tempo morto)");
        Dimensionamento.BicoAvaliado b = d.bicoAvaliado();
        if (b != null) {
            String texto = "TI " + formatar(b.tiEfetivoMs(), 2) + " + " + formatar(b.tempoMortoMs(), 2) + " = "
                    + formatar(b.tiTotalMs(), 2) + " ms num ciclo de " + formatar(d.porInjecao().cicloMs(), 2)
                    + " ms" + SEPARADOR + "DC " + percentual(b.dutyCycle(), 1);
            if (Double.isFinite(b.vazaoNominalMinimaCm3Min())) {
                texto += SEPARADOR + "vazão nominal mínima com tempo morto " + formatar(b.vazaoNominalMinimaCm3Min(), 0)
                        + " cm³/min";
            }
            if (Double.isFinite(b.tiEfetivoMarchaLentaMs())) {
                texto += SEPARADOR + "TI efetivo na marcha lenta " + formatar(b.tiEfetivoMarchaLentaMs(), 2) + " ms";
            }
            pdf.item("Bico informado", texto);
        }
        Dimensionamento.BombaNecessaria bomba = d.bomba();
        pdf.item("Bomba", formatar(bomba.vazaoLh(), 1) + " L/h a " + Apresentacao.pressaoManometrica(bomba.pressaoKpa())
                + SEPARADOR + "com folga verde: " + formatar(bomba.vazaoComFolgaVerdeLh(), 0) + " L/h"
                + (Double.isFinite(bomba.folga()) ? SEPARADOR + "folga da bomba informada " + percentual(bomba.folga(), 0) : ""));
        Dimensionamento.Esforco esforco = d.esforco();
        pdf.item("Esforço", "BMEP " + formatar(esforco.bmepBar(), 1) + " bar" + SEPARADOR + "torque "
                + formatar(esforco.torqueNm(), 0) + " N·m" + SEPARADOR + "pistão " + formatar(esforco.velocidadePistaoMS(), 1)
                + " m/s");
        pdf.item("Saídas exigidas", d.saidas().bicos() + " de bico"
                + (d.saidas().bicosExtras() > 0 ? " + " + d.saidas().bicosExtras() + " extras" : "") + SEPARADOR
                + d.saidas().ignicao() + " de ignição");
        pdf.item("Sensor MAP", "MAP máximo previsto " + formatar(d.sensorMap().mapMaximoPrevistoKpa(), 1) + " kPa"
                + SEPARADOR + "faixa mínima " + formatar(d.sensorMap().faixaMinimaKpa(), 0) + " kPa absolutos");
        if (d.pontoOperacao() != null) {
            pdf.item("Ponto do compressor", "vazão " + Apresentacao.vazaoAr(d.pontoOperacao().vazaoArKgS()) + SEPARADOR
                    + "PR " + formatar(d.pontoOperacao().razaoPressao(), 2) + " (conferir no mapa do fabricante)");
        }
    }

    private static void listas(EscritorPdf pdf, Dimensionamento d, Resultado<Dimensionamento> resultado)
            throws IOException {
        pdf.secao("5. Recomendações");
        for (String recomendacao : d.recomendacoes()) {
            pdf.paragrafo("• " + recomendacao, 6f);
        }
        pdf.secao("6. Avisos");
        if (resultado.avisos().isEmpty()) {
            pdf.paragrafo("Nenhum aviso.");
        }
        for (Aviso aviso : resultado.avisos()) {
            pdf.paragrafo("• [" + aviso.nivel().rotulo() + "] " + aviso.mensagem(), 6f);
        }
    }

    private static void memoria(EscritorPdf pdf, Resultado<Dimensionamento> resultado) throws IOException {
        pdf.secao("7. Memória de cálculo");
        pdf.paragrafo("Cada passo mostra a fórmula usada, as entradas e o resultado. Núcleo de cálculo "
                + resultado.versaoNucleo() + ".");
        pdf.espaco(4f);
        int numero = 1;
        for (Passo passo : resultado.memoria()) {
            pdf.negrito(numero + ". " + passo.grandeza() + " = " + Apresentacao.valor(passo.resultado()), 0f);
            pdf.paragrafo("Fórmula: " + passo.formula(), 14f);
            if (!passo.entradas().isEmpty()) {
                pdf.paragrafo("Entradas: " + passo.entradas().stream()
                        .map(RelatorioMemoria::descreverEntrada)
                        .collect(Collectors.joining("; ")), 14f);
            }
            pdf.espaco(3f);
            numero++;
        }
    }

    private static String descreverEntrada(Entrada entrada) {
        return entrada.simbolo() + " = " + Apresentacao.valor(entrada.valor());
    }
}
