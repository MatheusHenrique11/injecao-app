package br.com.calibra.app.catalogo;

import java.util.List;

import br.com.calibra.core.orcamento.CategoriaServico;
import br.com.calibra.core.orcamento.ServicoPadrao;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/**
 * Serviço da oficina (bloco 3.3; SPEC 6.6.1 na Fase 3B): a categoria, o texto da linha do orçamento, o tempo padrão
 * em horas, a garantia própria e se o serviço precisa do veículo na oficina. O preço, quando há, é o preço de
 * referência comum a todo item; sem preço, as horas são obrigatórias (é por elas que o serviço entra no orçamento).
 */
final class FormaServico implements FormaCatalogo<ServicoPadrao> {

    static final String CATEGORIA = "categoria";
    static final String DESCRICAO = "descricaoPadrao";
    static final String HORAS = "horas";
    static final String GARANTIA = "garantiaDias";
    static final String EXIGE_VEICULO = "exigeEntradaDoVeiculo";
    /** A fonte que um serviço novo já traz: a tabela da própria oficina. */
    static final String FONTE_PADRAO = "Tabela da oficina";

    @Override
    public TipoCatalogo<ServicoPadrao> tipo() {
        return TipoCatalogo.SERVICO;
    }

    @Override
    public List<CampoForm> campos() {
        return List.of(
                CampoForm.escolha(CATEGORIA, "Categoria", true, CategoriaServico.values(), CategoriaServico::rotulo),
                CampoForm.texto(DESCRICAO, "Descrição no orçamento", false)
                        .comDica("O texto da linha do orçamento (vai também para o PDF, o WhatsApp e o comprovante "
                                + "de entrega). Em branco, entra o nome. Dá para mudar em cada orçamento."),
                CampoForm.numero(HORAS, "Horas (tempo padrão)", false)
                        .comDica("No orçamento: com preço de referência, entra 1 serviço pelo preço; sem preço, "
                                + "entram estas horas × o valor da hora de Configurações."),
                CampoForm.inteiro(GARANTIA, "Garantia própria (dias)", false)
                        .comDica("Em branco, vale a garantia da OS (Configurações › OS). 0 = sem garantia para este "
                                + "serviço. Sai no comprovante de entrega."),
                CampoForm.marcar(EXIGE_VEICULO, "Exige o veículo na oficina")
                        .comDica("Desmarque no serviço de bancada, feito sem o veículo (ex.: confecção de chicote)."));
    }

    @Override
    public void escrever(ServicoPadrao servico, ValoresForm valores) {
        valores.texto(CATEGORIA, servico.categoria().name())
                .texto(DESCRICAO, servico.descricaoPadrao())
                .texto(HORAS, TextoForm.numero(servico.horas()))
                .texto(GARANTIA, TextoForm.inteiro(servico.garantiaDias()))
                .texto(EXIGE_VEICULO, String.valueOf(servico.exigeEntradaDoVeiculo()));
    }

    @Override
    public ServicoPadrao ler(LeituraForm leitura, InfoCatalogo info) {
        CategoriaServico categoria = leitura.escolha(CATEGORIA, CategoriaServico.class, true);
        Double horas = leitura.numero(HORAS, false);
        Integer garantia = leitura.inteiro(GARANTIA, false);
        // Só cobra quando os dois campos estão em branco: com erro de digitação, a mensagem do campo já diz o que é.
        if (leitura.texto(HORAS).isEmpty() && leitura.texto(FormularioCatalogo.PRECO).isEmpty()) {
            leitura.erro("Informe as horas ou o preço de referência do serviço.");
        }
        if (leitura.temErros()) {
            return null;
        }
        return new ServicoPadrao(info.nome(), horas, info.fonte(), info.data(), categoria, leitura.texto(DESCRICAO),
                leitura.marcado(EXIGE_VEICULO), garantia);
    }

    @Override
    public List<String> validar(ServicoPadrao servico) {
        return servico.validar();
    }

    @Override
    public List<String> colunas() {
        return List.of("Categoria", "Horas", "Garantia");
    }

    @Override
    public List<String> resumo(ServicoPadrao servico) {
        return List.of(servico.categoria().rotulo(), TextoForm.coluna(servico.horas(), "h"),
                garantia(servico.garantiaDias()));
    }

    @Override
    public void novo(ValoresForm valores) {
        valores.texto(FormularioCatalogo.FONTE, FONTE_PADRAO).texto(EXIGE_VEICULO, "true");
    }

    /** "da OS" (em branco), "sem garantia" (0), "1 dia" ou "180 dias". */
    static String garantia(Integer dias) {
        if (dias == null) {
            return "da OS";
        }
        return dias == 0 ? "sem garantia" : dias + (dias == 1 ? " dia" : " dias");
    }
}
