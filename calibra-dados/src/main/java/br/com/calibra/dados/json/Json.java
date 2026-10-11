package br.com.calibra.dados.json;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import br.com.calibra.core.combustivel.AfrFixo;
import br.com.calibra.core.combustivel.Composicao;
import br.com.calibra.core.combustivel.EtanolHidratado;
import br.com.calibra.core.combustivel.GasolinaComEtanol;
import br.com.calibra.dados.ErroDados;

/**
 * JSON dos dados técnicos (SPEC 5: coluna TEXT; ponto decimal, regra 2.9). Usa o Jackson 2 sem
 * anotações no core: o core continua sem dependência; o que o Jackson precisa saber fica aqui.
 *
 * <ul>
 *   <li>Records viram objetos com os nomes dos campos.</li>
 *   <li>Datas no formato ISO ("2026-08-01", "2026-10-11T18:30:00").</li>
 *   <li>NaN (valor "não se aplica" do core) vira o texto "NaN" e volta como NaN.</li>
 *   <li>Campo desconhecido é ignorado, para um dado antigo continuar sendo lido.</li>
 * </ul>
 */
public final class Json {

    private static final ObjectMapper MAPEADOR = criar();

    private Json() {
    }

    public static ObjectMapper mapeador() {
        return MAPEADOR;
    }

    public static String escrever(Object valor) {
        try {
            return MAPEADOR.writeValueAsString(valor);
        } catch (JsonProcessingException erro) {
            throw new ErroDados("Não foi possível gerar o JSON: " + erro.getOriginalMessage(), erro);
        }
    }

    /** JSON com quebras de linha, para arquivos que uma pessoa vai ler (manifesto, sementes). */
    public static String escreverFormatado(Object valor) {
        try {
            return MAPEADOR.writerWithDefaultPrettyPrinter().writeValueAsString(valor);
        } catch (JsonProcessingException erro) {
            throw new ErroDados("Não foi possível gerar o JSON: " + erro.getOriginalMessage(), erro);
        }
    }

    public static <T> T ler(String json, Class<T> tipo) {
        try {
            return MAPEADOR.readValue(json, tipo);
        } catch (JsonProcessingException erro) {
            throw new ErroDados("JSON inválido para " + tipo.getSimpleName() + ": " + erro.getOriginalMessage(), erro);
        }
    }

    public static <T> T ler(String json, TypeReference<T> tipo) {
        try {
            return MAPEADOR.readValue(json, tipo);
        } catch (JsonProcessingException erro) {
            throw new ErroDados("JSON inválido: " + erro.getOriginalMessage(), erro);
        }
    }

    public static JsonNode arvore(Object valor) {
        return MAPEADOR.valueToTree(valor);
    }

    public static JsonNode arvore(String json) {
        try {
            return MAPEADOR.readTree(json);
        } catch (JsonProcessingException erro) {
            throw new ErroDados("JSON inválido: " + erro.getOriginalMessage(), erro);
        }
    }

    public static <T> T converter(JsonNode arvore, Class<T> tipo) {
        try {
            return MAPEADOR.treeToValue(arvore, tipo);
        } catch (JsonProcessingException | IllegalArgumentException erro) {
            throw new ErroDados("JSON inválido para " + tipo.getSimpleName() + ": " + erro.getMessage(), erro);
        }
    }

    private static ObjectMapper criar() {
        SimpleModule datas = new SimpleModule("datas-iso");
        datas.addSerializer(LocalDate.class, ToStringSerializer.instance);
        datas.addSerializer(LocalDateTime.class, ToStringSerializer.instance);
        datas.addDeserializer(LocalDate.class, new LeitorDia());
        datas.addDeserializer(LocalDateTime.class, new LeitorDataHora());
        return JsonMapper.builder()
                .addModule(datas)
                .addMixIn(Composicao.class, ComposicaoJson.class)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    /** Lê uma data ISO ("2026-08-01"). */
    private static final class LeitorDia extends StdScalarDeserializer<LocalDate> {

        private static final long serialVersionUID = 1L;

        LeitorDia() {
            super(LocalDate.class);
        }

        @Override
        public LocalDate deserialize(JsonParser leitor, DeserializationContext contexto) throws IOException {
            String texto = leitor.getValueAsString();
            try {
                return LocalDate.parse(texto);
            } catch (RuntimeException erro) {
                return contexto.reportInputMismatch(this, "Data inválida: %s", texto);
            }
        }
    }

    /** Lê data e hora ISO ("2026-10-11T18:30:00"). */
    private static final class LeitorDataHora extends StdScalarDeserializer<LocalDateTime> {

        private static final long serialVersionUID = 1L;

        LeitorDataHora() {
            super(LocalDateTime.class);
        }

        @Override
        public LocalDateTime deserialize(JsonParser leitor, DeserializationContext contexto) throws IOException {
            String texto = leitor.getValueAsString();
            try {
                return LocalDateTime.parse(texto);
            } catch (RuntimeException erro) {
                return contexto.reportInputMismatch(this, "Data e hora inválidas: %s", texto);
            }
        }
    }

    /**
     * A composição do combustível é uma interface com três formas; no JSON ela leva o campo "tipo"
     * para saber qual delas ler de volta.
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "tipo")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = GasolinaComEtanol.class, name = "GASOLINA_COM_ETANOL"),
        @JsonSubTypes.Type(value = EtanolHidratado.class, name = "ETANOL_HIDRATADO"),
        @JsonSubTypes.Type(value = AfrFixo.class, name = "AFR_FIXO")
    })
    private interface ComposicaoJson {
    }
}
