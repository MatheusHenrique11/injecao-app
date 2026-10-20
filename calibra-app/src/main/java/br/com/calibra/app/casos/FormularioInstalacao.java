package br.com.calibra.app.casos;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import br.com.calibra.app.catalogo.CampoForm;
import br.com.calibra.app.catalogo.LeituraForm;
import br.com.calibra.app.catalogo.ValoresForm;
import br.com.calibra.core.cadastro.Instalacao;
import br.com.calibra.core.cadastro.TipoInstalacao;
import br.com.calibra.dados.oficina.ResumoOs;

/**
 * A janela da instalação do veículo (Fase 3B, SPEC 6.2), sem JavaFX: a lista de campos, que a tela monta com o mesmo
 * formulário dos catálogos, e a leitura do texto para a {@link Instalacao}, com os erros de digitação e os campos
 * obrigatórios de uma vez. As regras das datas (nem no futuro, a retirada) ficam com quem grava.
 */
public final class FormularioInstalacao {

    public static final String TIPO = "tipo";
    public static final String DESCRICAO = "descricao";
    public static final String MARCA = "marca";
    public static final String MODELO = "modelo";
    public static final String ESPECIFICACAO = "potenciaOuEspecificacao";
    public static final String DATA = "dataInstalacao";
    public static final String OS = "os";
    public static final String OBSERVACOES = "observacoes";

    /**
     * O que está na janela.
     *
     * @param osId a OS de origem; null = sem OS
     * @param erros vazio = pode gravar
     */
    public record Leitura(Instalacao instalacao, Long osId, List<String> erros) {

        public Leitura {
            erros = List.copyOf(erros);
        }
    }

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int SERVICO_NO_ROTULO = 50;

    private FormularioInstalacao() {
    }

    /** Os campos da janela; a OS de origem é uma das OS do veículo. */
    public static List<CampoForm> campos(List<ResumoOs> ordensDoVeiculo) {
        List<CampoForm.Opcao> ordens = ordensDoVeiculo.stream()
                .map(os -> new CampoForm.Opcao(Long.toString(os.osId()), rotulo(os))).toList();
        return List.of(
                CampoForm.escolha(TIPO, "Tipo", true, TipoInstalacao.values(), TipoInstalacao::rotulo),
                CampoForm.texto(DESCRICAO, "Descrição", true)
                        .comDica("O que foi instalado e onde (ex.: módulo no porta-malas, com fusível na bateria)."),
                CampoForm.texto(MARCA, "Marca", false),
                CampoForm.texto(MODELO, "Modelo", false),
                CampoForm.texto(ESPECIFICACAO, "Potência ou especificação", false)
                        .comDica("Como está no manual ou na nota (ex.: 800 W RMS, 4 canais; firmware 2.3). O app não "
                                + "confere."),
                CampoForm.data(DATA, "Data da instalação").comDica("Opcional."),
                new CampoForm(OS, "OS de origem", CampoForm.Tipo.ESCOLHA, false, ordens, List.of(),
                        "Sem OS: o item informado pelo cliente ou feito antes do app.", false, null, null),
                CampoForm.area(OBSERVACOES, "Observações"));
    }

    /** A OS na escolha: "OS-2026-0001 · Instalar o som (entregue)". */
    static String rotulo(ResumoOs os) {
        String servico = os.servicoPedido() == null ? "" : os.servicoPedido().strip().lines().findFirst().orElse("");
        if (servico.length() > SERVICO_NO_ROTULO) {
            servico = servico.substring(0, SERVICO_NO_ROTULO - 1).strip() + "…";
        }
        return os.numero().texto() + (servico.isEmpty() ? "" : " · " + servico) + " ("
                + os.status().rotulo().toLowerCase(Locale.ROOT) + ")";
    }

    /** O que vai na janela para a instalação (a gravada ou a nova). */
    public static ValoresForm valores(Instalacao instalacao, Long osId) {
        return new ValoresForm()
                .texto(TIPO, instalacao.tipo() == null ? "" : instalacao.tipo().name())
                .texto(DESCRICAO, instalacao.descricao())
                .texto(MARCA, instalacao.marca())
                .texto(MODELO, instalacao.modelo())
                .texto(ESPECIFICACAO, instalacao.potenciaOuEspecificacao())
                .texto(DATA, instalacao.dataInstalacao() == null ? "" : instalacao.dataInstalacao().format(DIA))
                .texto(OS, osId == null ? "" : Long.toString(osId))
                .texto(OBSERVACOES, instalacao.observacoes());
    }

    /** Lê o que está na janela: a instalação, a OS de origem e todos os erros de uma vez (nada é gravado aqui). */
    public static Leitura ler(ValoresForm valores, List<CampoForm> campos) {
        List<String> erros = new ArrayList<>();
        LeituraForm leitura = new LeituraForm(valores, campos, erros);
        TipoInstalacao tipo = leitura.escolha(TIPO, TipoInstalacao.class, false);
        LocalDate data = leitura.data(DATA);
        Long osId = leitura.referencia(OS);
        Instalacao instalacao = new Instalacao(tipo, leitura.texto(DESCRICAO), leitura.texto(MARCA),
                leitura.texto(MODELO), leitura.texto(ESPECIFICACAO), data, leitura.texto(OBSERVACOES));
        erros.addAll(instalacao.validarCampos());
        return new Leitura(instalacao, osId, erros);
    }
}
