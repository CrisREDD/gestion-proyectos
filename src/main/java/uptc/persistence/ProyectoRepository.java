package uptc.persistence;

import uptc.exception.PersistenciaException;
import uptc.model.Proyecto;

import java.util.List;

/** Contrato de almacenamiento de proyectos. */
public interface ProyectoRepository {
    List<Proyecto> cargarTodos() throws PersistenciaException;

    void guardar(Proyecto proyecto) throws PersistenciaException;

    void eliminar(String idProyecto) throws PersistenciaException;
}
