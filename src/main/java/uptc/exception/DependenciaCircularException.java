package uptc.exception;

import java.util.List;

/** Se lanza cuando las dependencias forman un ciclo (RF05). */
public class DependenciaCircularException extends DominioException {
    private final List<String> idsInvolucrados;

    public DependenciaCircularException(List<String> ids) {
        super("error.dependencia.circular");
        this.idsInvolucrados = List.copyOf(ids);
    }

    public List<String> getIdsInvolucrados() {
        return idsInvolucrados;
    }
}
