package br.com.calibra.app.catalogo;

import java.util.List;

import br.com.calibra.core.pecas.Sensor;
import br.com.calibra.core.pecas.TipoSensor;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/** Sensor: tipo e faixa de medição. No MAP, a faixa máxima (kPa absolutos) alimenta o semáforo do sensor. */
final class FormaSensor implements FormaCatalogo<Sensor> {

    static final String TIPO = "tipo";
    static final String FAIXA_MINIMA = "faixaMinima";
    static final String FAIXA_MAXIMA = "faixaMaxima";

    @Override
    public TipoCatalogo<Sensor> tipo() {
        return TipoCatalogo.SENSOR;
    }

    @Override
    public List<CampoForm> campos() {
        return List.of(
                CampoForm.escolha(TIPO, "Tipo", true, TipoSensor.values(), FormaSensor::rotuloComUnidade),
                CampoForm.numero(FAIXA_MINIMA, "Faixa mínima", false),
                CampoForm.numero(FAIXA_MAXIMA, "Faixa máxima", true)
                        .comDica("Na unidade do tipo. MAP: kPa absolutos (ex.: 400 num sensor de 4 bar)."));
    }

    private static String rotuloComUnidade(TipoSensor tipo) {
        return tipo.unidade().isEmpty() ? tipo.rotulo() : tipo.rotulo() + ", em " + tipo.unidade();
    }

    @Override
    public void escrever(Sensor sensor, ValoresForm valores) {
        valores.texto(TIPO, sensor.tipo() == null ? "" : sensor.tipo().name())
                .texto(FAIXA_MINIMA, TextoForm.numero(sensor.faixaMinima()))
                .texto(FAIXA_MAXIMA, TextoForm.numero(sensor.faixaMaxima()));
    }

    @Override
    public Sensor ler(LeituraForm leitura, InfoCatalogo info) {
        TipoSensor tipo = leitura.escolha(TIPO, TipoSensor.class, true);
        Double minima = leitura.numero(FAIXA_MINIMA, false);
        Double maxima = leitura.numero(FAIXA_MAXIMA, true);
        if (leitura.temErros()) {
            return null;
        }
        return new Sensor(info.nome(), tipo, minima, maxima, info.fonte(), info.data());
    }

    @Override
    public List<String> validar(Sensor sensor) {
        return sensor.validar();
    }

    @Override
    public List<String> colunas() {
        return List.of("Tipo", "Faixa");
    }

    @Override
    public List<String> resumo(Sensor sensor) {
        String unidade = sensor.tipo() == null ? "" : sensor.tipo().unidade();
        String faixa = sensor.faixaMinima() == null
                ? "até " + TextoForm.coluna(sensor.faixaMaxima(), unidade)
                : TextoForm.numero(sensor.faixaMinima()) + " a " + TextoForm.coluna(sensor.faixaMaxima(), unidade);
        return List.of(sensor.tipo() == null ? "—" : sensor.tipo().rotulo(), faixa);
    }
}
