package br.com.calibra.app.catalogo;

import java.util.List;

import br.com.calibra.core.orcamento.CategoriaServico;
import br.com.calibra.core.orcamento.ServicoPadrao;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/**
 * Serviço da oficina (bloco 3.3): o tempo padrão em horas. O preço, quando há, é o preço de referência comum a todo
 * item; sem preço, as horas são obrigatórias (é por elas que o serviço entra no orçamento).
 */
final class FormaServico implements FormaCatalogo<ServicoPadrao> {

    static final String HORAS = "horas";
    /** A fonte que um serviço novo já traz: a tabela da própria oficina. */
    static final String FONTE_PADRAO = "Tabela da oficina";

    @Override
    public TipoCatalogo<ServicoPadrao> tipo() {
        return TipoCatalogo.SERVICO;
    }

    @Override
    public List<CampoForm> campos() {
        return List.of(CampoForm.numero(HORAS, "Horas (tempo padrão)", false)
                .comDica("No orçamento: com preço de referência, entra 1 serviço pelo preço; sem preço, entram estas "
                        + "horas × o valor da hora de Configurações."));
    }

    @Override
    public void escrever(ServicoPadrao servico, ValoresForm valores) {
        valores.texto(HORAS, TextoForm.numero(servico.horas()));
    }

    @Override
    public ServicoPadrao ler(LeituraForm leitura, InfoCatalogo info) {
        Double horas = leitura.numero(HORAS, false);
        // Só cobra quando os dois campos estão em branco: com erro de digitação, a mensagem do campo já diz o que é.
        if (leitura.texto(HORAS).isEmpty() && leitura.texto(FormularioCatalogo.PRECO).isEmpty()) {
            leitura.erro("Informe as horas ou o preço de referência do serviço.");
        }
        if (leitura.temErros()) {
            return null;
        }
        // A categoria, a descrição, o "exige o veículo" e a garantia entram no formulário no passo seguinte do bloco
        // 3B.1; até lá, o serviço fica com os valores de quem não tem esses campos (como os gravados pelo 0.3.0).
        return new ServicoPadrao(info.nome(), horas, info.fonte(), info.data(), CategoriaServico.OUTRO, "", true, null);
    }

    @Override
    public List<String> validar(ServicoPadrao servico) {
        return servico.validar();
    }

    @Override
    public List<String> colunas() {
        return List.of("Horas");
    }

    @Override
    public List<String> resumo(ServicoPadrao servico) {
        return List.of(TextoForm.coluna(servico.horas(), "h"));
    }

    @Override
    public void novo(ValoresForm valores) {
        valores.texto(FormularioCatalogo.FONTE, FONTE_PADRAO);
    }
}
