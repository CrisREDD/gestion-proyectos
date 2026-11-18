package uptc.exception;

/** Error de lectura o escritura de datos persistidos. */
public class PersistenciaException extends RuntimeException {
    private final String clave;

    public PersistenciaException(String clave) {
        super(clave);
        this.clave = clave;
    }

    public PersistenciaException(String clave, Throwable causa) {
        super(clave, causa);
        this.clave = clave;
    }

    public String getClave() {
        return clave;
    }
}
