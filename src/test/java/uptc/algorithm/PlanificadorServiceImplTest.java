package uptc.algorithm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uptc.Demo;
import uptc.exception.DependenciaCircularException;
import uptc.exception.DominioException;
import uptc.model.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas sobre el ejemplo "Lanzar una app" (fixture con paralelismo). */
class PlanificadorServiceImplTest {

    private PlanificadorService plan;
    private Proyecto p;

    @BeforeEach
    void preparar() {
        plan = new PlanificadorServiceImpl();
        p = Demo.crearEjemplo();
    }

    private static List<String> ids(List<Actividad> as) {
        return as.stream().map(Actividad::getId).toList();
    }

    // ---------- orden y niveles ----------

    @Test
    void ordenRespetaCadaDependencia() {
        List<String> orden = ids(plan.ordenEjecucion(p));
        assertEquals(7, orden.size());
        for (String id : p.getIds()) {
            for (String suc : p.getSucesoras(id)) {
                assertTrue(orden.indexOf(id) < orden.indexOf(suc),
                        id + " debe ir antes que " + suc);
            }
        }
    }

    @Test
    void ordenEsDeterminista() {
        assertEquals(List.of("A", "B", "C", "G", "D", "E", "F"), ids(plan.ordenEjecucion(p)));
    }

    @Test
    void nivelesAgrupanActividadesParalelas() {
        List<List<String>> niveles = plan.niveles(p).stream().map(PlanificadorServiceImplTest::ids).toList();
        assertEquals(List.of(List.of("A"), List.of("B", "C", "G"), List.of("D", "E"), List.of("F")), niveles);
    }

    // ---------- simultaneidad ----------

    @Test
    void simultaneasSinCaminoEntreEllas() {
        assertTrue(plan.sonSimultaneas(p, "B", "C"));
        assertTrue(plan.sonSimultaneas(p, "C", "D")); // niveles distintos, igual simultáneas
        assertTrue(plan.sonSimultaneas(p, "G", "F"));
    }

    @Test
    void noSimultaneasSiHayCamino() {
        assertFalse(plan.sonSimultaneas(p, "B", "D"));
        assertFalse(plan.sonSimultaneas(p, "A", "F"));
        assertFalse(plan.sonSimultaneas(p, "A", "A"));
    }

    // ---------- ciclos ----------

    @Test
    void cerrariaCicloDetectaAristaQueCierraCiclo() {
        assertTrue(plan.cerrariaCiclo(p, "F", "A"));
        assertTrue(plan.cerrariaCiclo(p, "D", "B"));
        assertTrue(plan.cerrariaCiclo(p, "A", "A"));
        assertFalse(plan.cerrariaCiclo(p, "G", "F"));
    }

    @Test
    void cicloEnOrdenLanzaExcepcionConIdsInvolucrados() {
        Proyecto c = new Proyecto("C", "Ciclo", LocalDate.of(2026, 11, 2));
        c.agregarActividad(new Actividad("A", "a", 1));
        c.agregarActividad(new Actividad("B", "b", 1));
        c.agregarActividad(new Actividad("C", "c", 1));
        c.agregarDependencia("A", "B");
        c.agregarDependencia("B", "C");
        c.agregarDependencia("C", "A");

        DependenciaCircularException ex =
                assertThrows(DependenciaCircularException.class, () -> plan.ordenEjecucion(c));
        assertEquals("error.dependencia.circular", ex.getClave());
        assertEquals(List.of("A", "B", "C"), ex.getIdsInvolucrados());
        assertThrows(DependenciaCircularException.class, () -> plan.niveles(c));
        assertThrows(DependenciaCircularException.class, () -> plan.calcularFechas(c));
    }

    @Test
    void cicloReportaSoloNodosDelCicloYNoSusDescendientes() {
        Proyecto c = new Proyecto("C", "Ciclo", LocalDate.of(2026, 11, 2));
        for (String id : List.of("A", "B", "C", "D"))
            c.agregarActividad(new Actividad(id, id, 1));
        c.agregarDependencia("A", "B");
        c.agregarDependencia("B", "C");
        c.agregarDependencia("C", "A");
        c.agregarDependencia("C", "D"); // D no forma parte del ciclo

        DependenciaCircularException ex = assertThrows(DependenciaCircularException.class,
                () -> plan.ordenEjecucion(c));
        assertEquals(List.of("A", "B", "C"), ex.getIdsInvolucrados());
    }


    @Test
    void existeCaminoConActividadInexistenteLanzaExcepcion() {
        DominioException ex = assertThrows(DominioException.class, () -> plan.existeCamino(p, "A", "Z"));
        assertEquals("error.actividad.inexistente", ex.getClave());
    }

    // ---------- fechas ----------

    @Test
    void cronogramaDelEjemplo() {
        Map<String, Cronograma> c = plan.calcularFechas(p);
        assertEquals(new Cronograma(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 5)), c.get("A"));
        assertEquals(new Cronograma(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 7)), c.get("B"));
        assertEquals(new Cronograma(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 9)), c.get("C"));
        assertEquals(new Cronograma(LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 12)), c.get("D"));
        assertEquals(new Cronograma(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 13)), c.get("E"));
        assertEquals(new Cronograma(LocalDate.of(2026, 11, 13), LocalDate.of(2026, 11, 16)), c.get("F"));
        assertEquals(new Cronograma(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 7)), c.get("G"));
        assertEquals(LocalDate.of(2026, 11, 16), plan.fechaFin(p, c));
    }

    @Test
    void proyectoVacioTerminaEnLaFechaInicial() {
        Proyecto vacio = new Proyecto("V", "Vacío", LocalDate.of(2026, 11, 2));
        assertTrue(plan.ordenEjecucion(vacio).isEmpty());
        assertEquals(LocalDate.of(2026, 11, 2), plan.fechaFin(vacio, plan.calcularFechas(vacio)));
    }

    // ---------- bloqueadas ----------

    @Test
    void bloqueadasDependenDelEstadoDeLasPredecesoras() {
        assertEquals(List.of("B", "C", "D", "E", "F", "G"), ids(plan.actividadesBloqueadas(p)));

        p.getActividad("A").setEstado(Estado.DONE);
        assertEquals(List.of("D", "E", "F"), ids(plan.actividadesBloqueadas(p)));

        p.getActividad("B").setEstado(Estado.DONE);
        p.getActividad("C").setEstado(Estado.DONE);
        assertEquals(List.of("F"), ids(plan.actividadesBloqueadas(p)));
    }

    // ---------- impacto ----------

    @Test
    void aumentarDuracionDeActividadCriticaRetrasaElProyecto() {
        ImpactoResultado r = plan.impactoCambioDuracion(p, "D", 7);
        assertEquals(LocalDate.of(2026, 11, 16), r.finAntes());
        assertEquals(LocalDate.of(2026, 11, 17), r.finDespues());
        assertEquals(1, r.diasDeDiferencia());
        assertEquals(List.of("D", "F"), r.actividadesAfectadas());
    }

    @Test
    void reducirDuracionAdelantaElProyecto() {
        ImpactoResultado r = plan.impactoCambioDuracion(p, "E", 3);
        assertEquals(LocalDate.of(2026, 11, 15), r.finDespues());
        assertEquals(-1, r.diasDeDiferencia());
        assertEquals(List.of("E", "F"), r.actividadesAfectadas());
    }

    @Test
    void impactoNoModificaElProyectoReal() {
        plan.impactoCambioDuracion(p, "D", 7);
        plan.impactoEliminarDependencia(p, "E", "F");
        assertEquals(5, p.getActividad("D").getDuracionDias());
        assertTrue(p.getSucesoras("E").contains("F"));
        assertEquals(LocalDate.of(2026, 11, 16), plan.fechaFin(p, plan.calcularFechas(p)));
    }

    @Test
    void eliminarDependenciaMuestraSuImpacto() {
        ImpactoResultado r = plan.impactoEliminarDependencia(p, "E", "F");
        assertEquals(LocalDate.of(2026, 11, 15), r.finDespues());
        assertEquals(List.of("F"), r.actividadesAfectadas());
    }

    @Test
    void impactoValidaEntradas() {
        assertEquals("error.actividad.inexistente",
                assertThrows(DominioException.class, () -> plan.impactoCambioDuracion(p, "Z", 3)).getClave());
        assertEquals("error.actividad.duracion.invalida",
                assertThrows(DominioException.class, () -> plan.impactoCambioDuracion(p, "A", 0)).getClave());
        assertEquals("error.dependencia.inexistente",
                assertThrows(DominioException.class, () -> plan.impactoEliminarDependencia(p, "A", "F")).getClave());
    }

    // ---------- validaciones del modelo ----------

    @Test
    void modeloRechazaDatosInvalidos() {
        assertEquals("error.actividad.duplicada",
                assertThrows(DominioException.class, () -> p.agregarActividad(new Actividad("A", "x", 1))).getClave());
        assertEquals("error.dependencia.autoreferencia",
                assertThrows(DominioException.class, () -> p.agregarDependencia("A", "A")).getClave());
        assertEquals("error.dependencia.duplicada",
                assertThrows(DominioException.class, () -> p.agregarDependencia("A", "B")).getClave());
        assertEquals("error.actividad.inexistente",
                assertThrows(DominioException.class, () -> p.agregarDependencia("A", "Z")).getClave());
        assertEquals("error.actividad.duracion.invalida",
                assertThrows(DominioException.class, () -> new Actividad("X", "x", 0)).getClave());
        assertEquals("error.actividad.nombre.vacio",
                assertThrows(DominioException.class, () -> new Actividad("X", " ", 1)).getClave());
        assertEquals("error.actividad.id.vacio",
                assertThrows(DominioException.class, () -> new Actividad("", "x", 1)).getClave());
        assertEquals("error.proyecto.fecha.vacia",
                assertThrows(DominioException.class, () -> new Proyecto("P", "n", null)).getClave());
    }

    @Test
    void copiaDelProyectoEsIndependiente() {
        Proyecto copia = p.copiar();
        copia.getActividad("A").setDuracionDias(10);
        copia.eliminarDependencia("A", "B");
        assertEquals(3, p.getActividad("A").getDuracionDias());
        assertTrue(p.getSucesoras("A").contains("B"));
        assertEquals(p.cantidadActividades(), copia.cantidadActividades());
    }
}
