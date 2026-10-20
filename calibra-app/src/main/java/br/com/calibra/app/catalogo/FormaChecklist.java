package br.com.calibra.app.catalogo;

import java.util.ArrayList;
import java.util.List;

import br.com.calibra.core.oficina.ChecklistModelo;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/**
 * Modelo de checklist (Fase 3B, SPEC 6.6.2): o nome e os itens, na ordem em que se confere, cada um com o texto e as
 * marcas "obrigatório" (pendente, trava o "pronto" da OS) e "a verificar" (o texto ainda não foi conferido). Sem preço.
 *
 * <p>Nos valores do formulário, cada item é uma lista de texto: o texto, "obrigatório" e "a verificar"
 * ({@link #linha}).
 */
public final class FormaChecklist implements FormaCatalogo<ChecklistModelo> {

    public static final String ITENS = "itens";

    private static final int TEXTO = 0;
    private static final int OBRIGATORIO = 1;
    private static final int A_VERIFICAR = 2;

    FormaChecklist() {
    }

    @Override
    public TipoCatalogo<ChecklistModelo> tipo() {
        return TipoCatalogo.CHECKLIST;
    }

    @Override
    public List<CampoForm> campos() {
        return List.of(CampoForm.itens(ITENS, "Itens")
                .comDica("Na ordem em que se confere. Obrigatório: pendente, trava o \"pronto\" da OS até uma "
                        + "justificativa. A verificar: o texto ainda não foi conferido. A OS copia o checklist quando "
                        + "o serviço começa; mudar o modelo depois não muda essa cópia."));
    }

    @Override
    public void escrever(ChecklistModelo modelo, ValoresForm valores) {
        valores.tabela(ITENS, modelo.itens().stream()
                .map(item -> linha(item.texto(), item.obrigatorio(), item.aVerificar())).toList());
    }

    /** O item sem texto fica de fora, como a linha vazia de uma tabela. */
    @Override
    public ChecklistModelo ler(LeituraForm leitura, InfoCatalogo info) {
        List<ChecklistModelo.Item> itens = leitura.linhasDeTexto(ITENS).stream()
                .filter(linha -> !texto(linha).isEmpty())
                .map(linha -> new ChecklistModelo.Item(texto(linha), obrigatorio(linha), aVerificar(linha)))
                .toList();
        return new ChecklistModelo(info.nome(), itens, info.fonte(), info.data());
    }

    @Override
    public List<String> validar(ChecklistModelo modelo) {
        return modelo.validar();
    }

    @Override
    public List<String> colunas() {
        return List.of("Itens", "Obrigatórios");
    }

    @Override
    public List<String> resumo(ChecklistModelo modelo) {
        long obrigatorios = modelo.itens().stream().filter(ChecklistModelo.Item::obrigatorio).count();
        return List.of(Integer.toString(modelo.itens().size()), Long.toString(obrigatorios));
    }

    @Override
    public void novo(ValoresForm valores) {
        valores.texto(FormularioCatalogo.FONTE, FormaServico.FONTE_PADRAO);
    }

    /** O checklist não tem preço. */
    @Override
    public boolean temPreco() {
        return false;
    }

    // ===================== Itens nos valores do formulário =====================

    /** Um item nos valores do formulário: o texto, "obrigatório" e "a verificar" (como na tela). */
    public static List<String> linha(String texto, boolean obrigatorio, boolean aVerificar) {
        return List.of(texto == null ? "" : texto, Boolean.toString(obrigatorio), Boolean.toString(aVerificar));
    }

    public static String texto(List<String> linha) {
        return celula(linha, TEXTO);
    }

    public static boolean obrigatorio(List<String> linha) {
        return Boolean.parseBoolean(celula(linha, OBRIGATORIO));
    }

    public static boolean aVerificar(List<String> linha) {
        return Boolean.parseBoolean(celula(linha, A_VERIFICAR));
    }

    /**
     * Os itens um por linha, para o histórico de versões: "1. Teste de ruído com o motor ligado (obrigatório, a
     * verificar)". Vazio se não houver item.
     */
    static String exibir(List<List<String>> linhas) {
        List<String> itens = new ArrayList<>();
        for (List<String> linha : linhas) {
            if (texto(linha).isEmpty()) {
                continue;
            }
            List<String> marcas = new ArrayList<>();
            if (obrigatorio(linha)) {
                marcas.add("obrigatório");
            }
            if (aVerificar(linha)) {
                marcas.add("a verificar");
            }
            itens.add((itens.size() + 1) + ". " + texto(linha)
                    + (marcas.isEmpty() ? "" : " (" + String.join(", ", marcas) + ")"));
        }
        return String.join("\n", itens);
    }

    private static String celula(List<String> linha, int indice) {
        String valor = indice < linha.size() ? linha.get(indice) : null;
        return valor == null ? "" : valor.strip();
    }
}
