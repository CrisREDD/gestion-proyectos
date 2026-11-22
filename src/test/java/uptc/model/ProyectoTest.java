package uptc.model;

import org.junit.jupiter.api.Test;
import uptc.exception.DominioException;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProyectoTest {
    private Proyecto nuevo() {
        return new Proyecto("P1", "Proyecto", LocalDate.of(2026, 11, 20));
    }

    @Test
    void gettersDelGrafoNoPermitenMutacionExterna() {
        Proyecto p = nuevo();
        p.agregarActividad(new Actividad("A", "A", 1));
        p.agregarActividad(new Actividad("B", "B", 1));
        p.agregarDependencia("A", "B");

        assertThrows(UnsupportedOperationException.class, () -> p.getIds().remove("A"));
        assertThrows(UnsupportedOperationException.class, () -> p.getSucesoras("A").add("B"));
        assertThrows(UnsupportedOperationException.class, () -> p.getPredecesoras("B").clear());
        assertEquals(Set.of("A", "B"), p.getIds());
        assertEquals(Set.of("B"), p.getSucesoras("A"));
        assertEquals(Set.of("A"), p.getPredecesoras("B"));
    }

    @Test
    void eliminarActividadQuitaActividadYAristasEntrantesYSalientes() {
        Proyecto p = nuevo();
        for (String id : new String[]{"A", "B", "C"})
            p.agregarActividad(new Actividad(id, id, 1));
        p.agregarDependencia("A", "B");
        p.agregarDependencia("B", "C");

        p.eliminarActividad("B");

        assertNull(p.getActividad("B"));
        assertEquals(Set.of("A", "C"), p.getIds());
        assertTrue(p.getSucesoras("A").isEmpty());
        assertTrue(p.getPredecesoras("C").isEmpty());
        assertThrows(DominioException.class, () -> p.eliminarActividad("B"));
    }

    @Test
    void setNombreYFechaInicioRechazanValoresInvalidosSinCambiarEstado() {
        Proyecto p = nuevo();
        assertThrows(DominioException.class, () -> p.setNombre(" "));
        assertThrows(DominioException.class, () -> p.setNombre(null));
        assertThrows(DominioException.class, () -> p.setFechaInicio(null));
        assertEquals("Proyecto", p.getNombre());
        assertEquals(LocalDate.of(2026, 11, 20), p.getFechaInicio());
    }

    @Test
    void idsRechazanEspaciosYSimbolosFueraDelFormato() {
        assertEquals("error.actividad.id.formato", assertThrows(DominioException.class,
                () -> new Actividad("id con espacio", "Actividad", 1)).getClave());
        assertEquals("error.actividad.id.formato", assertThrows(DominioException.class,
                () -> new Actividad("id!", "Actividad", 1)).getClave());
        assertEquals("error.proyecto.id.formato", assertThrows(DominioException.class,
                () -> new Proyecto("id malo", "Proyecto", LocalDate.now())).getClave());
    }
}
