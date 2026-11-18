package uptc.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uptc.Demo;
import uptc.exception.PersistenciaException;
import uptc.model.Proyecto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonProyectoRepositoryTest {
    @TempDir
    Path carpetaTemporal;

    @Test
    void guardaYRecargaProyectoCompleto() {
        Path archivo = carpetaTemporal.resolve("proyectos.json");
        JsonProyectoRepository repositorio = new JsonProyectoRepository(archivo);
        Proyecto original = Demo.crearEjemplo();
        original.setDescripcion("Proyecto de prueba");

        repositorio.guardar(original);

        List<Proyecto> cargados = repositorio.cargarTodos();
        assertEquals(1, cargados.size());
        Proyecto copia = cargados.get(0);
        assertEquals(original.getId(), copia.getId());
        assertEquals(original.getNombre(), copia.getNombre());
        assertEquals(original.getDescripcion(), copia.getDescripcion());
        assertEquals(original.getFechaInicio(), copia.getFechaInicio());
        assertEquals(original.cantidadActividades(), copia.cantidadActividades());
        assertEquals(original.getSucesoras("A"), copia.getSucesoras("A"));
        assertEquals(original.getActividad("F").getDuracionDias(), copia.getActividad("F").getDuracionDias());
    }

    @Test
    void guardarMismoIdReemplazaYNoDuplica() {
        Path archivo = carpetaTemporal.resolve("proyectos.json");
        JsonProyectoRepository repositorio = new JsonProyectoRepository(archivo);
        Proyecto proyecto = Demo.crearEjemplo();
        repositorio.guardar(proyecto);

        proyecto.setNombre("Nombre actualizado");
        repositorio.guardar(proyecto);

        assertEquals(1, repositorio.cargarTodos().size());
        assertEquals("Nombre actualizado", repositorio.cargarTodos().get(0).getNombre());
    }

    @Test
    void eliminaProyecto() {
        Path archivo = carpetaTemporal.resolve("proyectos.json");
        JsonProyectoRepository repositorio = new JsonProyectoRepository(archivo);
        repositorio.guardar(Demo.crearEjemplo());

        repositorio.eliminar("P1");

        assertTrue(repositorio.cargarTodos().isEmpty());
    }

    @Test
    void archivoInexistenteTieneClaveDeError() {
        JsonProyectoRepository repositorio = new JsonProyectoRepository(carpetaTemporal.resolve("no-existe.json"));

        PersistenciaException ex = assertThrows(PersistenciaException.class, repositorio::cargarTodos);

        assertEquals("error.persistencia.archivo.inexistente", ex.getClave());
    }

    @Test
    void archivoVacioTieneClaveDeError() throws Exception {
        Path archivo = carpetaTemporal.resolve("vacio.json");
        Files.createFile(archivo);
        JsonProyectoRepository repositorio = new JsonProyectoRepository(archivo);

        PersistenciaException ex = assertThrows(PersistenciaException.class, repositorio::cargarTodos);

        assertEquals("error.persistencia.archivo.vacio", ex.getClave());
    }

    @Test
    void archivoCorruptoTieneClaveDeError() throws Exception {
        Path archivo = carpetaTemporal.resolve("corrupto.json");
        Files.writeString(archivo, "{ no es json }");
        JsonProyectoRepository repositorio = new JsonProyectoRepository(archivo);

        PersistenciaException ex = assertThrows(PersistenciaException.class, repositorio::cargarTodos);

        assertEquals("error.persistencia.archivo.corrupto", ex.getClave());
    }
}
