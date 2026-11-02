package uptc.model;

import uptc.exception.DominioException;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Proyecto = actividades + grafo dirigido de precedencias.
 * Arista A -> B significa: B necesita que A termine.
 * Se usa LinkedHashMap/LinkedHashSet para que el orden sea determinista.
 */
public class Proyecto {
    private final String id;
    private String nombre;
    private String descripcion;
    private LocalDate fechaInicio;

    private final Map<String, Actividad> actividades = new LinkedHashMap<>();
    private final Map<String, Set<String>> sucesoras = new LinkedHashMap<>();
    private final Map<String, Set<String>> predecesoras = new LinkedHashMap<>();

    public Proyecto(String id, String nombre, LocalDate fechaInicio) {
        if (id == null || id.isBlank())
            throw new DominioException("error.proyecto.id.vacio");
        if (nombre == null || nombre.isBlank())
            throw new DominioException("error.proyecto.nombre.vacio");
        if (fechaInicio == null)
            throw new DominioException("error.proyecto.fecha.vacia");
        this.id = id;
        this.nombre = nombre;
        this.fechaInicio = fechaInicio;
    }

    public void agregarActividad(Actividad a) {
        if (actividades.containsKey(a.getId()))
            throw new DominioException("error.actividad.duplicada");
        actividades.put(a.getId(), a);
        sucesoras.put(a.getId(), new LinkedHashSet<>());
        predecesoras.put(a.getId(), new LinkedHashSet<>());
    }

    /** idA -> idB significa: B necesita que A termine. */
    public void agregarDependencia(String idA, String idB) {
        if (!actividades.containsKey(idA) || !actividades.containsKey(idB))
            throw new DominioException("error.actividad.inexistente");
        if (idA.equals(idB))
            throw new DominioException("error.dependencia.autoreferencia");
        if (!sucesoras.get(idA).add(idB))
            throw new DominioException("error.dependencia.duplicada");
        predecesoras.get(idB).add(idA);
    }

    public void eliminarDependencia(String idA, String idB) {
        if (!actividades.containsKey(idA) || !actividades.containsKey(idB)
                || !sucesoras.get(idA).remove(idB))
            throw new DominioException("error.dependencia.inexistente");
        predecesoras.get(idB).remove(idA);
    }

    /** Copia lógica: las simulaciones trabajan sobre esto y no alteran el original. */
    public Proyecto copiar() {
        Proyecto c = new Proyecto(id, nombre, fechaInicio);
        c.descripcion = descripcion;
        actividades.values().forEach(a -> c.agregarActividad(a.copiar()));
        sucesoras.forEach((a, sucs) -> sucs.forEach(b -> c.agregarDependencia(a, b)));
        return c;
    }

    public String getId()                          { return id; }
    public String getNombre()                      { return nombre; }
    public String getDescripcion()                 { return descripcion; }
    public LocalDate getFechaInicio()              { return fechaInicio; }
    public void setNombre(String nombre)           { this.nombre = nombre; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setFechaInicio(LocalDate f)        { this.fechaInicio = f; }

    public Set<String> getIds()                    { return actividades.keySet(); }
    public Actividad getActividad(String id)       { return actividades.get(id); }
    public Set<String> getSucesoras(String id)     { return sucesoras.get(id); }
    public Set<String> getPredecesoras(String id)  { return predecesoras.get(id); }
    public int cantidadActividades()               { return actividades.size(); }
}
