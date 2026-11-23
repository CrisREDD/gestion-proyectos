package uptc.viewController;

import uptc.model.Actividad;
import uptc.model.Cronograma;
import uptc.model.Estado;
import uptc.model.Proyecto;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Estadísticas puras del proyecto; no depende de JavaFX y es fácilmente testeable. */
public final class ProjectStatistics {
    /** Calcula el número de actividades por estado. */
    public Map<Estado, Long> byState(Proyecto project) {
        Map<Estado, Long> result = new EnumMap<>(Estado.class);
        for (Estado state : Estado.values()) result.put(state, 0L);
        project.getIds().stream().map(project::getActividad)
                .forEach(activity -> result.merge(activity.getEstado(), 1L, Long::sum));
        return result;
    }

    /** Calcula la carga total de días por responsable. */
    public Map<String, Integer> workloadByOwner(Proyecto project) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String id : project.getIds()) {
            Actividad activity = project.getActividad(id);
            result.merge(activity.getResponsable() == null || activity.getResponsable().isBlank()
                    ? "Sin responsable" : activity.getResponsable(), activity.getDuracionDias(), Integer::sum);
        }
        return result;
    }

    /** Calcula el porcentaje de actividades terminadas. */
    public double completionPercent(Proyecto project) {
        if (project.cantidadActividades() == 0) return 0;
        return 100.0 * byState(project).getOrDefault(Estado.DONE, 0L) / project.cantidadActividades();
    }

    /** Cuenta las actividades bloqueadas con base en las predecesoras. */
    public long blocked(Proyecto project) {
        return project.getIds().stream().map(project::getActividad)
                .filter(a -> a.getEstado() != Estado.DONE)
                .filter(a -> project.getPredecesoras(a.getId()).stream()
                        .anyMatch(id -> project.getActividad(id).getEstado() != Estado.DONE))
                .count();
    }

    /** Calcula la fecha final a partir del cronograma. */
    public LocalDate finish(Map<String, Cronograma> schedule, LocalDate fallback) {
        return schedule.values().stream().map(Cronograma::fin).max(LocalDate::compareTo).orElse(fallback);
    }
}
