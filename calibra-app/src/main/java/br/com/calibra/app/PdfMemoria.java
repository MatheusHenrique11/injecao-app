package br.com.calibra.app;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import javafx.application.HostServices;
import javafx.stage.Window;

import br.com.calibra.relatorios.DadosRelatorio;
import br.com.calibra.relatorios.RelatorioMemoria;

/** PDF da memória de cálculo ({@link ArquivoPdf}). Usado pela calculadora e pelo histórico de cálculos do projeto. */
final class PdfMemoria {

    private static final DateTimeFormatter NOME_ARQUIVO = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm");

    private PdfMemoria() {
    }

    /**
     * @param estado recebe as mensagens para a barra de estado ("Gerando o PDF…", "PDF salvo em…")
     * @param aoTerminar chamado no fim, deu certo ou não (para liberar botões)
     * @return false se a pessoa desistiu na escolha do arquivo
     */
    static boolean gerar(Window janela, HostServices servicos, DadosRelatorio dados, String prefixoNome,
            Consumer<String> estado, Runnable aoTerminar) {
        return ArquivoPdf.gerar(janela, servicos, "Memória de cálculo",
                prefixoNome + "-" + dados.geradoEm().format(NOME_ARQUIVO) + ".pdf",
                destino -> RelatorioMemoria.gerar(dados, destino), estado, aoTerminar);
    }
}
