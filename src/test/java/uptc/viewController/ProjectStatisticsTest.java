package uptc.viewController;

import org.junit.jupiter.api.Test;
import uptc.model.Actividad;
import uptc.model.Estado;
import uptc.model.Proyecto;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Pruebas de los indicadores sin levantar JavaFX. */
class ProjectStatisticsTest {
    @Test
    void calculatesStatesWorkloadAndProgress() {
        Proyecto project = new Proyecto("P1", "Demo", LocalDate.of(2026, 1, 1));
        Actividad done = new Actividad("A", "Análisis", 2); done.setEstado(Estado.DONE); done.setResponsable("Ana");
        Actividad pending = new Actividad("B", "Construcción", 4); pending.setResponsable("Ana");
        project.agregarActividad(done); project.agregarActividad(pending); project.agregarDependencia("A", "B");

        ProjectStatistics statistics = new ProjectStatistics();
        assertEquals(1L, statistics.byState(project).get(Estado.DONE));
        assertEquals(6, statistics.workloadByOwner(project).get("Ana"));
        assertEquals(50.0, statistics.completionPercent(project));
        assertEquals(0, statistics.blocked(project));
    }
}
