package br.com.calibra.app.catalogo;

import java.util.List;

import br.com.calibra.core.cadastro.TipoCambio;
import br.com.calibra.core.pecas.Cambio;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/** Câmbio: tipo e o torque máximo que ele aguenta (N·m). */
final class FormaCambio implements FormaCatalogo<Cambio> {

    static final String TIPO = "tipo";
    static final String TORQUE_MAXIMO = "torqueMaximo";

    @Override
    public TipoCatalogo<Cambio> tipo() {
        return TipoCatalogo.CAMBIO;
    }

    @Override
    public List<CampoForm> campos() {
        return List.of(
                CampoForm.escolha(TIPO, "Tipo", true, TipoCambio.values(), TipoCambio::rotulo),
                CampoForm.numero(TORQUE_MAXIMO, "Torque máximo (N·m)", false)
                        .comDica("O que o câmbio aguenta, segundo a fonte. Em branco, o indicador de torque só avisa."));
    }

    @Override
    public void escrever(Cambio cambio, ValoresForm valores) {
        valores.texto(TIPO, cambio.tipo() == null ? "" : cambio.tipo().name())
                .texto(TORQUE_MAXIMO, TextoForm.numero(cambio.torqueMaximoNm()));
    }

    @Override
    public Cambio ler(LeituraForm leitura, InfoCatalogo info) {
        TipoCambio tipo = leitura.escolha(TIPO, TipoCambio.class, true);
        Double torque = leitura.numero(TORQUE_MAXIMO, false);
        if (leitura.temErros()) {
            return null;
        }
        return new Cambio(info.nome(), tipo, torque, info.fonte(), info.data());
    }

    @Override
    public List<String> validar(Cambio cambio) {
        return cambio.validar();
    }

    @Override
    public List<String> colunas() {
        return List.of("Tipo", "Torque máximo");
    }

    @Override
    public List<String> resumo(Cambio cambio) {
        return List.of(cambio.tipo() == null ? "—" : cambio.tipo().rotulo(),
                TextoForm.coluna(cambio.torqueMaximoNm(), "N·m"));
    }
}
