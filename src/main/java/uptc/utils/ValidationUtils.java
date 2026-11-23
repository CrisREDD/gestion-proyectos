package uptc.utils;

import uptc.exception.DominioException;

/** Validaciones reutilizables para entradas del dominio y de la interfaz. */
public final class ValidationUtils {
    private ValidationUtils() { }

    /** Valida un identificador de entidad. */
    public static String requiredId(String value, String errorKey) {
        if (value == null || value.isBlank() || !value.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new DominioException(errorKey);
        }
        return value.trim();
    }

    /** Valida una duración positiva. */
    public static int positiveDuration(String value) {
        try {
            int duration = Integer.parseInt(value == null ? "" : value.trim());
            if (duration < 1 || duration > 36500) throw new NumberFormatException();
            return duration;
        } catch (NumberFormatException ex) {
            throw new DominioException("error.actividad.duracion.invalida");
        }
    }
}
