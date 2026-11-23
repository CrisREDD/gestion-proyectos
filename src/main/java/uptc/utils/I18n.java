package uptc.utils;

import java.util.Locale;
import java.util.ResourceBundle;

/** Cargador centralizado de los recursos de traducción. */
public final class I18n {
    private static final String BASE_NAME = "uptc.i18n.messages";

    private I18n() { }

    /** Devuelve el bundle de la aplicación para un idioma. */
    public static ResourceBundle bundle(Locale locale) {
        return ResourceBundle.getBundle(BASE_NAME, locale == null ? Locale.forLanguageTag("es") : locale);
    }
}
