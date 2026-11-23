package uptc.controller;

import uptc.model.Actividad;
import uptc.model.Cronograma;
import uptc.model.Estado;
import uptc.model.ImpactoResultado;
import uptc.model.Proyecto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Casos de uso de proyectos consumidos por la interfaz de usuario. */
public interface ProyectoService {
    Proyecto crearProyecto(String nombre, String descripcion, LocalDate inicio);

    List<Proyecto> listarProyectos();

    void actualizarProyecto(Proyecto proyecto);

    void eliminarProyecto(String id);

    Actividad agregarActividad(String idProyecto, Actividad actividad);

    void agregarDependencia(String idProyecto, String idPred, String idSuc);

    void eliminarDependencia(String idProyecto, String idPred, String idSuc);

    void registrarAvance(String idProyecto, String idAct, Estado nuevoEstado);

    List<Actividad> ordenEjecucion(String idProyecto);

    Map<String, Cronograma> cronograma(String idProyecto);

    ImpactoResultado simularCambioDuracion(String idProyecto, String idAct, int dias);

    ImpactoResultado simularEliminarDependencia(String idProyecto, String idPred, String idSuc);
}
