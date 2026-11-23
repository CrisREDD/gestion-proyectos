package uptc.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uptc.controller.algorithm.PlanificadorServiceImpl;
import uptc.exception.DominioException;
import uptc.model.Actividad;
import uptc.model.Cronograma;
import uptc.model.Proyecto;
import uptc.persistence.JsonProyectoRepository;
import uptc.controller.ProyectoServiceImpl;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProyectoIntegracionTest {
    @TempDir
    Path carpetaTemporal;

    @Test
    void cicloCompletoDeCreacionCalculoGuardadoYRecarga() {
        Path archivo = carpetaTemporal.resolve("proyectos.json");
        JsonProyectoRepository repositorio = new JsonProyectoRepository(archivo);
        PlanificadorServiceImpl planificador = new PlanificadorServiceImpl();
        ProyectoServiceImpl servicio = new ProyectoServiceImpl(repositorio, planificador);

        Proyecto proyecto = servicio.crearProyecto("Lanzar app", "Integración", LocalDate.of(2026, 11, 2));
        servicio.agregarActividad(proyecto.getId(), new Actividad("A", "Requisitos", 3));
        servicio.agregarActividad(proyecto.getId(), new Actividad("B", "Backend", 2));
        servicio.agregarActividad(proyecto.getId(), new Actividad("C", "Pruebas", 1));
        servicio.agregarDependencia(proyecto.getId(), "A", "B");
        servicio.agregarDependencia(proyecto.getId(), "B", "C");

        DominioException ciclo = assertThrows(DominioException.class,
                () -> servicio.agregarDependencia(proyecto.getId(), "C", "A"));
        assertEquals("error.dependencia.circular", ciclo.getClave());

        Map<String, Cronograma> cronograma = servicio.cronograma(proyecto.getId());
        assertEquals(LocalDate.of(2026, 11, 2), cronograma.get("A").inicio());
        assertEquals(LocalDate.of(2026, 11, 8), cronograma.get("C").fin());

        ProyectoServiceImpl servicioRecargado = new ProyectoServiceImpl(
                new JsonProyectoRepository(archivo), new PlanificadorServiceImpl());

        assertEquals(1, servicioRecargado.listarProyectos().size());
        Proyecto recuperado = servicioRecargado.listarProyectos().get(0);
        assertEquals("Lanzar app", recuperado.getNombre());
        assertEquals(3, recuperado.cantidadActividades());
        assertEquals(1, recuperado.getPredecesoras("C").size());
        assertEquals(LocalDate.of(2026, 11, 8), servicioRecargado.cronograma(recuperado.getId()).get("C").fin());
    }
}
