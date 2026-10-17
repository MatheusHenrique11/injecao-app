package br.com.calibra.app;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

/** Datas como aparecem na tela: 12/10/2026 e 12/10/2026 09:30. */
final class Datas {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DIA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter POR_EXTENSO = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy",
            Locale.of("pt", "BR"));

    private Datas() {
    }

    static String dia(LocalDate data) {
        return data == null ? "—" : DIA.format(data);
    }

    /** "quinta-feira, 16 de outubro de 2026" (os nomes vêm do módulo jdk.localedata, que vai no instalador). */
    static String porExtenso(LocalDate data) {
        return POR_EXTENSO.format(data);
    }

    static String diaHora(LocalDateTime dataHora) {
        return dataHora == null ? "—" : DIA_HORA.format(dataHora);
    }

    /** O seletor de data lê e mostra dd/MM/aaaa, qualquer que seja o idioma do Windows. */
    static void emPortugues(DatePicker seletor) {
        seletor.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate data) {
                return data == null ? "" : DIA.format(data);
            }

            @Override
            public LocalDate fromString(String texto) {
                if (texto == null || texto.isBlank()) {
                    return null;
                }
                try {
                    return LocalDate.parse(texto.strip(), DIA);
                } catch (DateTimeParseException erro) {
                    return null;
                }
            }
        });
        seletor.setPromptText("dd/mm/aaaa");
    }
}
