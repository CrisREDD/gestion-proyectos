package uptc.ui;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class I18nResourcesTest {
    @Test
    void spanishAndEnglishBundlesHaveTheSameKeys() {
        ResourceBundle es = ResourceBundle.getBundle("uptc.i18n.messages", Locale.forLanguageTag("es"));
        ResourceBundle en = ResourceBundle.getBundle("uptc.i18n.messages", Locale.forLanguageTag("en"));
        assertEquals(es.keySet(), en.keySet());
    }

    @Test
    void allDomainExceptionKeysAreTranslatedInBothLanguages() {
        ResourceBundle es = ResourceBundle.getBundle("uptc.i18n.messages", Locale.forLanguageTag("es"));
        ResourceBundle en = ResourceBundle.getBundle("uptc.i18n.messages", Locale.forLanguageTag("en"));
        Set<String> exceptionKeys = Set.of(
                "error.actividad.duplicada", "error.actividad.duracion.invalida", "error.actividad.id.formato",
                "error.actividad.id.vacio", "error.actividad.inexistente", "error.actividad.nombre.vacio",
                "error.actividad.predecesora.pendiente", "error.dependencia.autoreferencia",
                "error.dependencia.circular", "error.dependencia.duplicada", "error.dependencia.inexistente",
                "error.estado.invalido", "error.persistencia.archivo.corrupto", "error.persistencia.archivo.inexistente",
                "error.persistencia.archivo.vacio", "error.persistencia.io", "error.proyecto.fecha.invalida",
                "error.proyecto.fecha.vacia", "error.proyecto.id.formato", "error.proyecto.id.vacio",
                "error.proyecto.inexistente", "error.proyecto.nombre.vacio");
        assertTrue(es.keySet().containsAll(exceptionKeys));
        assertTrue(en.keySet().containsAll(exceptionKeys));
        for (String key : exceptionKeys) {
            assertTrue(!es.getString(key).isBlank(), "Clave vacía en español: " + key);
            assertTrue(!en.getString(key).isBlank(), "Empty English translation: " + key);
        }
    }
}
