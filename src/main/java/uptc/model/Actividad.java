package uptc.model;

import uptc.exception.DominioException;

import java.util.Objects;

public class Actividad {
    private final String id;
    private String nombre;
    private String descripcion;
    private int duracionDias;
    private String responsable;
    private Estado estado;

    public Actividad(String id, String nombre, int duracionDias) {
        if (id == null || id.isBlank())
            throw new DominioException("error.actividad.id.vacio");
        if (!id.matches("[A-Za-z0-9_-]{1,64}"))
            throw new DominioException("error.actividad.id.formato");
        if (nombre == null || nombre.isBlank())
            throw new DominioException("error.actividad.nombre.vacio");
        if (duracionDias <= 0)
            throw new DominioException("error.actividad.duracion.invalida");
        this.id = id;
        this.nombre = nombre;
        this.duracionDias = duracionDias;
        this.estado = Estado.TODO;
    }

    /** Copia independiente, para simulaciones que no deben tocar los datos reales. */
    public Actividad copiar() {
        Actividad c = new Actividad(id, nombre, duracionDias);
        c.descripcion = descripcion;
        c.responsable = responsable;
        c.estado = estado;
        return c;
    }

    public String getId()          { return id; }
    public String getNombre()      { return nombre; }
    public String getDescripcion() { return descripcion; }
    public int getDuracionDias()   { return duracionDias; }
    public String getResponsable() { return responsable; }
    public Estado getEstado()      { return estado; }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.isBlank())
            throw new DominioException("error.actividad.nombre.vacio");
        this.nombre = nombre;
    }

    public void setDuracionDias(int duracionDias) {
        if (duracionDias <= 0)
            throw new DominioException("error.actividad.duracion.invalida");
        this.duracionDias = duracionDias;
    }

    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setResponsable(String responsable) { this.responsable = responsable; }
    public void setEstado(Estado estado)           { this.estado = Objects.requireNonNull(estado); }

    @Override
    public boolean equals(Object o) {
        return o instanceof Actividad a && id.equals(a.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
