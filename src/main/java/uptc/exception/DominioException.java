package uptc.exception;

/** Error de dominio. Lleva una CLAVE de i18n, no un texto en un idioma. */
public class DominioException extends RuntimeException {
    private final String clave;

    public DominioException(String clave) {
        super(clave);
        this.clave = clave;
    }

    public String getClave() {
        return clave;
    }
}
