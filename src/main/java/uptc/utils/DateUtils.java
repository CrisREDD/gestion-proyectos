package uptc.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/** Utilidades de fecha independientes de JavaFX. */
public final class DateUtils {
    private DateUtils() { }

    /** Formatea una fecha de acuerdo con el idioma indicado. */
    public static String format(LocalDate date, Locale locale) {
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(locale == null ? Locale.getDefault() : locale).format(date);
    }

    /** Convierte una fecha ISO o lanza una excepción de dominio localizada. */
    public static LocalDate parseIso(String value) {
        try {
            return LocalDate.parse(value == null ? "" : value.trim());
        } catch (RuntimeException ex) {
            throw new uptc.exception.DominioException("error.proyecto.fecha.invalida");
        }
    }
}
