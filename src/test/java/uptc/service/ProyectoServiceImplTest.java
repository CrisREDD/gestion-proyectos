package uptc.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import uptc.Demo;
import uptc.algorithm.PlanificadorService;
import uptc.exception.DominioException;
import uptc.model.Actividad;
import uptc.model.Cronograma;
import uptc.model.Estado;
import uptc.model.ImpactoResultado;
import uptc.model.Proyecto;
import uptc.persistence.ProyectoRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProyectoServiceImplTest {
    @Mock
    private ProyectoRepository repositorio;
    @Mock
    private PlanificadorService planificador;

    private ProyectoServiceImpl servicio;

    @BeforeEach
    void preparar() {
        when(repositorio.cargarTodos()).thenReturn(new ArrayList<>());
        servicio = new ProyectoServiceImpl(repositorio, planificador);
    }

    @Test
    void crearProyectoLoGuardaYLoLista() {
        Proyecto proyecto = servicio.crearProyecto("App", "Descripción", LocalDate.of(2026, 11, 2));

        assertEquals("P1", proyecto.getId());
        assertEquals(List.of(proyecto), servicio.listarProyectos());
        verify(repositorio).guardar(proyecto);
    }

    @Test
    void agregaActividadYDependenciaValida() {
        Proyecto proyecto = servicio.crearProyecto("App", null, LocalDate.of(2026, 11, 2));
        Actividad a = new Actividad("A", "A", 1);
        Actividad b = new Actividad("B", "B", 1);
        servicio.agregarActividad(proyecto.getId(), a);
        servicio.agregarActividad(proyecto.getId(), b);

        when(planificador.cerrariaCiclo(proyecto, "A", "B")).thenReturn(false);
        servicio.agregarDependencia(proyecto.getId(), "A", "B");

        assertTrue(proyecto.getSucesoras("A").contains("B"));
        verify(repositorio, atLeast(3)).guardar(proyecto);
    }

    @Test
    void rechazaDependenciaQueCreaCiclo() {
        Proyecto proyecto = servicio.crearProyecto("App", null, LocalDate.of(2026, 11, 2));
        servicio.agregarActividad(proyecto.getId(), new Actividad("A", "A", 1));
        servicio.agregarActividad(proyecto.getId(), new Actividad("B", "B", 1));
        when(planificador.cerrariaCiclo(proyecto, "B", "A")).thenReturn(true);

        DominioException ex = assertThrows(DominioException.class,
                () -> servicio.agregarDependencia(proyecto.getId(), "B", "A"));

        assertEquals("error.dependencia.circular", ex.getClave());
        assertTrue(proyecto.getSucesoras("B").isEmpty());
    }

    @Test
    void noPermiteTerminarActividadBloqueada() {
        Proyecto proyecto = servicio.crearProyecto("App", null, LocalDate.of(2026, 11, 2));
        servicio.agregarActividad(proyecto.getId(), new Actividad("A", "A", 1));
        servicio.agregarActividad(proyecto.getId(), new Actividad("B", "B", 1));
        when(planificador.cerrariaCiclo(proyecto, "A", "B")).thenReturn(false);
        servicio.agregarDependencia(proyecto.getId(), "A", "B");

        DominioException ex = assertThrows(DominioException.class,
                () -> servicio.registrarAvance(proyecto.getId(), "B", Estado.DONE));

        assertEquals("error.actividad.predecesora.pendiente", ex.getClave());
    }

    @Test
    void permiteTerminarActividadCuandoPredecesorasEstanListas() {
        Proyecto proyecto = servicio.crearProyecto("App", null, LocalDate.of(2026, 11, 2));
        servicio.agregarActividad(proyecto.getId(), new Actividad("A", "A", 1));
        servicio.agregarActividad(proyecto.getId(), new Actividad("B", "B", 1));
        when(planificador.cerrariaCiclo(proyecto, "A", "B")).thenReturn(false);
        servicio.agregarDependencia(proyecto.getId(), "A", "B");
        servicio.registrarAvance(proyecto.getId(), "A", Estado.DONE);
        servicio.registrarAvance(proyecto.getId(), "B", Estado.DONE);

        assertEquals(Estado.DONE, proyecto.getActividad("B").getEstado());
    }

    @Test
    void actualizaYEliminaProyecto() {
        Proyecto proyecto = servicio.crearProyecto("App", null, LocalDate.of(2026, 11, 2));
        proyecto.setNombre("App actualizada");
        servicio.actualizarProyecto(proyecto);
        assertEquals("App actualizada", servicio.listarProyectos().get(0).getNombre());

        servicio.eliminarProyecto(proyecto.getId());

        assertTrue(servicio.listarProyectos().isEmpty());
        verify(repositorio).eliminar(proyecto.getId());
    }

    @Test
    void consultasDeleganEnPlanificador() {
        Proyecto proyecto = servicio.crearProyecto("App", null, LocalDate.of(2026, 11, 2));
        Map<String, Cronograma> cronograma = Map.of();
        ImpactoResultado impacto = new ImpactoResultado(
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 2), List.of());
        when(planificador.ordenEjecucion(proyecto)).thenReturn(List.of());
        when(planificador.calcularFechas(proyecto)).thenReturn(cronograma);
        when(planificador.impactoCambioDuracion(proyecto, "A", 2)).thenReturn(impacto);
        when(planificador.impactoEliminarDependencia(proyecto, "A", "B")).thenReturn(impacto);

        assertEquals(List.of(), servicio.ordenEjecucion(proyecto.getId()));
        assertSame(cronograma, servicio.cronograma(proyecto.getId()));
        assertSame(impacto, servicio.simularCambioDuracion(proyecto.getId(), "A", 2));
        assertSame(impacto, servicio.simularEliminarDependencia(proyecto.getId(), "A", "B"));
        verify(planificador).ordenEjecucion(proyecto);
        verify(planificador).calcularFechas(proyecto);
    }

    @Test
    void iniciarConArchivoInexistenteEmpiezaVacio() {
        when(repositorio.cargarTodos()).thenThrow(
                new uptc.exception.PersistenciaException("error.persistencia.archivo.inexistente"));

        ProyectoService nuevo = new ProyectoServiceImpl(repositorio, planificador);

        assertTrue(nuevo.listarProyectos().isEmpty());
    }

    @Test
    void propagaErroresDeCargaDistintosAlArchivoInexistente() {
        when(repositorio.cargarTodos()).thenThrow(
                new uptc.exception.PersistenciaException("error.persistencia.archivo.corrupto"));

        assertThrows(uptc.exception.PersistenciaException.class,
                () -> new ProyectoServiceImpl(repositorio, planificador));
    }
}
