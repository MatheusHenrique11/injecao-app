package br.com.calibra.app.casos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.calibra.app.catalogo.CampoForm;
import br.com.calibra.app.catalogo.ValoresForm;
import br.com.calibra.core.cadastro.Instalacao;
import br.com.calibra.core.cadastro.TipoInstalacao;
import br.com.calibra.core.oficina.NumeroOs;
import br.com.calibra.core.oficina.StatusOs;
import br.com.calibra.dados.oficina.ResumoOs;

/** A janela da instalação, sem a tela (Fase 3B, bloco 3B.5): os campos e a leitura do texto. */
class FormularioInstalacaoTest {

    private static ResumoOs os(long id, int sequencial, String servico, StatusOs status) {
        return new ResumoOs(id, new NumeroOs(2026, sequencial), status, "Ana Costa", "VW Gol GTi", "ABC1D23", servico,
                null, null, null, LocalDateTime.of(2026, 10, 20, 9, 0));
    }

    private static final List<CampoForm> CAMPOS = FormularioInstalacao.campos(List.of(
            os(7, 2, "Instalar o som\ne conferir o alternador", StatusOs.ENTREGUE),
            os(3, 1, "Diagnóstico de uma falha elétrica que aparece só com o motor quente e o ar ligado",
                    StatusOs.CANCELADA)));

    @Test
    void camposDaJanela() {
        assertEquals(List.of("Tipo", "Descrição", "Marca", "Modelo", "Potência ou especificação",
                "Data da instalação", "OS de origem", "Observações"), CAMPOS.stream().map(CampoForm::rotulo).toList());
        CampoForm origem = CAMPOS.get(6);
        assertEquals(List.of("7", "3"), origem.opcoes().stream().map(CampoForm.Opcao::valor).toList());
        assertEquals(List.of("OS-2026-0002 · Instalar o som (entregue)",
                "OS-2026-0001 · Diagnóstico de uma falha elétrica que aparece só… (cancelada)"),
                origem.opcoes().stream().map(CampoForm.Opcao::rotulo).toList(),
                "a primeira linha do serviço pedido, encurtada");
        assertEquals(List.of("SOM", "ALARME_RASTREADOR", "ILUMINACAO", "VIDRO_TRAVA", "INJECAO_PROGRAMAVEL", "OUTRA"),
                CAMPOS.getFirst().opcoes().stream().map(CampoForm.Opcao::valor).toList());
    }

    @Test
    void vaiEVoltaPelaJanela() {
        Instalacao som = new Instalacao(TipoInstalacao.SOM, "Módulo no porta-malas", "Taramps", "TS 800x4",
                "800 W RMS", LocalDate.of(2026, 10, 18), "Cabo de 21 mm²");
        ValoresForm valores = FormularioInstalacao.valores(som, 7L);
        assertEquals("18/10/2026", valores.texto(FormularioInstalacao.DATA));
        FormularioInstalacao.Leitura lida = FormularioInstalacao.ler(valores, CAMPOS);
        assertEquals(List.of(), lida.erros());
        assertEquals(som, lida.instalacao());
        assertEquals(7L, lida.osId());

        Instalacao semNada = new Instalacao(TipoInstalacao.OUTRA, "Tomada USB", "", "", "", null, "");
        FormularioInstalacao.Leitura semOs = FormularioInstalacao.ler(FormularioInstalacao.valores(semNada, null),
                CAMPOS);
        assertEquals(semNada, semOs.instalacao());
        assertNull(semOs.osId(), "sem OS de origem");
    }

    /** A data digitada errada e os campos obrigatórios aparecem juntos. */
    @Test
    void errosDeUmaVez() {
        ValoresForm valores = new ValoresForm().texto(FormularioInstalacao.DESCRICAO, "  ")
                .texto(FormularioInstalacao.DATA, "31/02/2026");
        FormularioInstalacao.Leitura lida = FormularioInstalacao.ler(valores, CAMPOS);
        assertEquals(List.of("\"Data da instalação\": use a data no formato dd/mm/aaaa.",
                "Escolha o tipo da instalação.", "Descreva o que foi instalado e onde."), lida.erros());
        assertNull(lida.instalacao().dataInstalacao());
    }
}
