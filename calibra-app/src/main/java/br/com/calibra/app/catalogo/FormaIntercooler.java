package br.com.calibra.app.catalogo;

import java.util.List;

import br.com.calibra.core.pecas.Intercooler;
import br.com.calibra.core.pecas.TipoIntercooler;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/** Intercooler: tipo e eficiência (fração, como o eps da calculadora). */
final class FormaIntercooler implements FormaCatalogo<Intercooler> {

    static final String TIPO = "tipo";
    static final String EFICIENCIA = "eficiencia";

    @Override
    public TipoCatalogo<Intercooler> tipo() {
        return TipoCatalogo.INTERCOOLER;
    }

    @Override
    public List<CampoForm> campos() {
        return List.of(
                CampoForm.escolha(TIPO, "Tipo", true, TipoIntercooler.values(), TipoIntercooler::rotulo),
                CampoForm.numero(EFICIENCIA, "Eficiência (eps)", false)
                        .comDica("Fração: 0,80 = 80%. Vira o eps do cálculo; em branco, vale o padrão de Configurações."));
    }

    @Override
    public void escrever(Intercooler intercooler, ValoresForm valores) {
        valores.texto(TIPO, intercooler.tipo() == null ? "" : intercooler.tipo().name())
                .texto(EFICIENCIA, TextoForm.numero(intercooler.eficiencia()));
    }

    @Override
    public Intercooler ler(LeituraForm leitura, InfoCatalogo info) {
        TipoIntercooler tipo = leitura.escolha(TIPO, TipoIntercooler.class, true);
        Double eficiencia = leitura.numero(EFICIENCIA, false);
        if (leitura.temErros()) {
            return null;
        }
        return new Intercooler(info.nome(), tipo, eficiencia, info.fonte(), info.data());
    }

    @Override
    public List<String> validar(Intercooler intercooler) {
        return intercooler.validar();
    }

    @Override
    public List<String> colunas() {
        return List.of("Tipo", "Eficiência");
    }

    @Override
    public List<String> resumo(Intercooler intercooler) {
        return List.of(intercooler.tipo() == null ? "—" : intercooler.tipo().rotulo(),
                TextoForm.coluna(intercooler.eficiencia(), ""));
    }
}
