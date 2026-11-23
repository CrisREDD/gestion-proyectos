package uptc.controller;

import uptc.controller.algorithm.PlanificadorService;
import uptc.exception.DominioException;
import uptc.exception.PersistenciaException;
import uptc.model.Actividad;
import uptc.model.Cronograma;
import uptc.model.Estado;
import uptc.model.ImpactoResultado;
import uptc.model.Proyecto;
import uptc.persistence.ProyectoRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Implementación de los casos de uso de proyectos. */
public class ProyectoServiceImpl implements ProyectoService {
    private final ProyectoRepository repositorio;
    private final PlanificadorService planificador;
    private final Map<String, Proyecto> proyectos = new LinkedHashMap<>();

    public ProyectoServiceImpl(ProyectoRepository repositorio, PlanificadorService planificador) {
        if (repositorio == null || planificador == null) {
            throw new IllegalArgumentException("repositorio y planificador son obligatorios");
        }
        this.repositorio = repositorio;
        this.planificador = planificador;
        cargarDesdeRepositorio();
    }

    @Override
    public Proyecto crearProyecto(String nombre, String descripcion, LocalDate inicio) {
        if (nombre == null || nombre.isBlank()) {
            throw new DominioException("error.proyecto.nombre.vacio");
        }
        if (inicio == null) {
            throw new DominioException("error.proyecto.fecha.vacia");
        }
        String id = siguienteId();
        Proyecto proyecto = new Proyecto(id, nombre, inicio);
        proyecto.setDescripcion(descripcion);
        proyectos.put(id, proyecto);
        repositorio.guardar(proyecto);
        return proyecto;
    }

    @Override
    public List<Proyecto> listarProyectos() {
        return List.copyOf(proyectos.values());
    }

    @Override
    public void actualizarProyecto(Proyecto proyecto) {
        if (proyecto == null || proyecto.getId() == null || proyecto.getId().isBlank()) {
            throw new DominioException("error.proyecto.id.vacio");
        }
        exigirProyecto(proyecto.getId());
        proyectos.put(proyecto.getId(), proyecto);
        repositorio.guardar(proyecto);
    }

    @Override
    public void eliminarProyecto(String id) {
        exigirProyecto(id);
        proyectos.remove(id);
        repositorio.eliminar(id);
    }

    @Override
    public Actividad agregarActividad(String idProyecto, Actividad actividad) {
        Proyecto proyecto = exigirProyecto(idProyecto);
        if (actividad == null) {
            throw new DominioException("error.actividad.inexistente");
        }
        proyecto.agregarActividad(actividad);
        repositorio.guardar(proyecto);
        return actividad;
    }

    @Override
    public void agregarDependencia(String idProyecto, String idPred, String idSuc) {
        Proyecto proyecto = exigirProyecto(idProyecto);
        exigirActividad(proyecto, idPred);
        exigirActividad(proyecto, idSuc);
        if (idPred.equals(idSuc)) {
            throw new DominioException("error.dependencia.autoreferencia");
        }
        if (proyecto.getSucesoras(idPred).contains(idSuc)) {
            throw new DominioException("error.dependencia.duplicada");
        }
        if (planificador.cerrariaCiclo(proyecto, idPred, idSuc)) {
            throw new DominioException("error.dependencia.circular");
        }
        proyecto.agregarDependencia(idPred, idSuc);
        repositorio.guardar(proyecto);
    }

    @Override
    public void eliminarDependencia(String idProyecto, String idPred, String idSuc) {
        Proyecto proyecto = exigirProyecto(idProyecto);
        proyecto.eliminarDependencia(idPred, idSuc);
        repositorio.guardar(proyecto);
    }

    @Override
    public void registrarAvance(String idProyecto, String idAct, Estado nuevoEstado) {
        Proyecto proyecto = exigirProyecto(idProyecto);
        Actividad actividad = exigirActividad(proyecto, idAct);
        if (nuevoEstado == null) {
            throw new DominioException("error.estado.invalido");
        }
        if (nuevoEstado == Estado.DONE && proyecto.getPredecesoras(idAct).stream()
                .anyMatch(pred -> proyecto.getActividad(pred).getEstado() != Estado.DONE)) {
            throw new DominioException("error.actividad.predecesora.pendiente");
        }
        actividad.setEstado(nuevoEstado);
        repositorio.guardar(proyecto);
    }

    @Override
    public List<Actividad> ordenEjecucion(String idProyecto) {
        return planificador.ordenEjecucion(exigirProyecto(idProyecto));
    }

    @Override
    public Map<String, Cronograma> cronograma(String idProyecto) {
        return planificador.calcularFechas(exigirProyecto(idProyecto));
    }

    @Override
    public ImpactoResultado simularCambioDuracion(String idProyecto, String idAct, int dias) {
        return planificador.impactoCambioDuracion(exigirProyecto(idProyecto), idAct, dias);
    }

    @Override
    public ImpactoResultado simularEliminarDependencia(String idProyecto, String idPred, String idSuc) {
        return planificador.impactoEliminarDependencia(exigirProyecto(idProyecto), idPred, idSuc);
    }

    private void cargarDesdeRepositorio() {
        try {
            for (Proyecto proyecto : repositorio.cargarTodos()) {
                if (proyecto == null || proyecto.getId() == null || proyecto.getId().isBlank()) {
                    throw new PersistenciaException("error.persistencia.archivo.corrupto");
                }
                proyectos.put(proyecto.getId(), proyecto);
            }
        } catch (PersistenciaException e) {
            if (!"error.persistencia.archivo.inexistente".equals(e.getClave())) {
                throw e;
            }
        }
    }

    private String siguienteId() {
        int siguiente = 1;
        while (proyectos.containsKey("P" + siguiente)) {
            siguiente++;
        }
        return "P" + siguiente;
    }

    private Proyecto exigirProyecto(String id) {
        Proyecto proyecto = proyectos.get(id);
        if (proyecto == null) {
            throw new DominioException("error.proyecto.inexistente");
        }
        return proyecto;
    }

    private Actividad exigirActividad(Proyecto proyecto, String id) {
        Actividad actividad = proyecto.getActividad(id);
        if (actividad == null) {
            throw new DominioException("error.actividad.inexistente");
        }
        return actividad;
    }
}
