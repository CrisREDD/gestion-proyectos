package uptc.model;

/**
 * Estado almacenado de una actividad.
 * BLOQUEADA no existe aquí a propósito: se calcula a partir de las predecesoras.
 * El texto visible (Pendiente, En curso, Terminada) lo traduce la interfaz con i18n.
 */
public enum Estado {
    TODO,
    IN_PROGRESS,
    DONE
}
