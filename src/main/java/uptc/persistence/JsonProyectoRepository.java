package uptc.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uptc.exception.PersistenciaException;
import uptc.model.Actividad;
import uptc.model.Estado;
import uptc.model.Proyecto;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Repositorio JSON de proyectos con reemplazo seguro del archivo. */
public class JsonProyectoRepository implements ProyectoRepository {
    private static final String ARCHIVO_INEXISTENTE = "error.persistencia.archivo.inexistente";
    private static final String ARCHIVO_VACIO = "error.persistencia.archivo.vacio";
    private static final String ARCHIVO_CORRUPTO = "error.persistencia.archivo.corrupto";
    private static final String ERROR_IO = "error.persistencia.io";

    private final Path archivo;
    private final ObjectMapper mapper;

    public JsonProyectoRepository(Path archivo) {
        if (archivo == null) {
            throw new IllegalArgumentException("archivo no puede ser null");
        }
        this.archivo = archivo.toAbsolutePath().normalize();
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public List<Proyecto> cargarTodos() throws PersistenciaException {
        if (!Files.exists(archivo)) {
            throw new PersistenciaException(ARCHIVO_INEXISTENTE);
        }

        try {
            if (Files.size(archivo) == 0) {
                throw new PersistenciaException(ARCHIVO_VACIO);
            }
            JsonNode raiz = mapper.readTree(archivo.toFile());
            if (raiz == null || !raiz.isArray()) {
                throw new PersistenciaException(ARCHIVO_CORRUPTO);
            }
            List<Proyecto> proyectos = new ArrayList<>();
            for (JsonNode nodo : raiz) {
                proyectos.add(desdeJson(nodo));
            }
            return proyectos;
        } catch (PersistenciaException e) {
            throw e;
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new PersistenciaException(ARCHIVO_CORRUPTO, e);
        } catch (IOException e) {
            throw new PersistenciaException(ERROR_IO, e);
        }
    }

    @Override
    public void guardar(Proyecto proyecto) throws PersistenciaException {
        if (proyecto == null) {
            throw new IllegalArgumentException("proyecto no puede ser null");
        }

        List<Proyecto> proyectos;
        if (Files.exists(archivo)) {
            proyectos = cargarTodos();
        } else {
            proyectos = new ArrayList<>();
        }

        boolean reemplazado = false;
        for (int i = 0; i < proyectos.size(); i++) {
            if (proyectos.get(i).getId().equals(proyecto.getId())) {
                proyectos.set(i, proyecto);
                reemplazado = true;
                break;
            }
        }
        if (!reemplazado) {
            proyectos.add(proyecto);
        }
        escribirSeguro(proyectos);
    }

    @Override
    public void eliminar(String idProyecto) throws PersistenciaException {
        if (idProyecto == null || idProyecto.isBlank()) {
            throw new PersistenciaException("error.proyecto.id.vacio");
        }
        List<Proyecto> proyectos = cargarTodos();
        boolean eliminado = proyectos.removeIf(p -> p.getId().equals(idProyecto));
        if (!eliminado) {
            throw new PersistenciaException("error.proyecto.inexistente");
        }
        escribirSeguro(proyectos);
    }

    private void escribirSeguro(List<Proyecto> proyectos) {
        try {
            Path directorio = archivo.getParent();
            if (directorio != null) {
                Files.createDirectories(directorio);
            }
            Path temporal = Files.createTempFile(directorio, archivo.getFileName().toString(), ".tmp");
            try {
                mapper.writerWithDefaultPrettyPrinter().writeValue(temporal.toFile(), aJson(proyectos));
                try {
                    Files.move(temporal, archivo, StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporal);
            }
        } catch (JsonProcessingException e) {
            throw new PersistenciaException(ERROR_IO, e);
        } catch (IOException e) {
            throw new PersistenciaException(ERROR_IO, e);
        }
    }

    private ArrayNode aJson(List<Proyecto> proyectos) {
        ArrayNode raiz = mapper.createArrayNode();
        for (Proyecto proyecto : proyectos) {
            ObjectNode nodo = raiz.addObject();
            nodo.put("id", proyecto.getId());
            nodo.put("nombre", proyecto.getNombre());
            if (proyecto.getDescripcion() == null) nodo.putNull("descripcion");
            else nodo.put("descripcion", proyecto.getDescripcion());
            nodo.put("fechaInicio", proyecto.getFechaInicio().toString());

            ArrayNode actividades = nodo.putArray("actividades");
            for (String id : proyecto.getIds()) {
                Actividad actividad = proyecto.getActividad(id);
                ObjectNode a = actividades.addObject();
                a.put("id", actividad.getId());
                a.put("nombre", actividad.getNombre());
                if (actividad.getDescripcion() == null) a.putNull("descripcion");
                else a.put("descripcion", actividad.getDescripcion());
                a.put("duracionDias", actividad.getDuracionDias());
                if (actividad.getResponsable() == null) a.putNull("responsable");
                else a.put("responsable", actividad.getResponsable());
                a.put("estado", actividad.getEstado().name());
            }

            ArrayNode dependencias = nodo.putArray("dependencias");
            for (String origen : proyecto.getIds()) {
                for (String destino : proyecto.getSucesoras(origen)) {
                    ObjectNode dependencia = dependencias.addObject();
                    dependencia.put("predecesora", origen);
                    dependencia.put("sucesora", destino);
                }
            }
        }
        return raiz;
    }

    private Proyecto desdeJson(JsonNode nodo) {
        try {
            String id = textoObligatorio(nodo, "id");
            String nombre = textoObligatorio(nodo, "nombre");
            LocalDate fecha = LocalDate.parse(textoObligatorio(nodo, "fechaInicio"));
            Proyecto proyecto = new Proyecto(id, nombre, fecha);
            if (!nodo.path("descripcion").isNull()) {
                proyecto.setDescripcion(nodo.path("descripcion").asText());
            }

            for (JsonNode a : arregloObligatorio(nodo, "actividades")) {
                Actividad actividad = new Actividad(
                        textoObligatorio(a, "id"),
                        textoObligatorio(a, "nombre"),
                        enteroObligatorio(a, "duracionDias"));
                if (!a.path("descripcion").isNull()) actividad.setDescripcion(a.path("descripcion").asText());
                if (!a.path("responsable").isNull()) actividad.setResponsable(a.path("responsable").asText());
                if (a.hasNonNull("estado")) actividad.setEstado(Estado.valueOf(a.get("estado").asText()));
                proyecto.agregarActividad(actividad);
            }
            for (JsonNode d : arregloObligatorio(nodo, "dependencias")) {
                proyecto.agregarDependencia(
                        textoObligatorio(d, "predecesora"),
                        textoObligatorio(d, "sucesora"));
            }
            return proyecto;
        } catch (RuntimeException e) {
            if (e instanceof PersistenciaException) throw e;
            throw new PersistenciaException(ARCHIVO_CORRUPTO, e);
        }
    }

    private String textoObligatorio(JsonNode nodo, String campo) {
        if (nodo == null || !nodo.hasNonNull(campo) || !nodo.get(campo).isTextual()
                || nodo.get(campo).asText().isBlank()) {
            throw new IllegalArgumentException("campo inválido: " + campo);
        }
        return nodo.get(campo).asText();
    }

    private int enteroObligatorio(JsonNode nodo, String campo) {
        if (nodo == null || !nodo.has(campo) || !nodo.get(campo).canConvertToInt()) {
            throw new IllegalArgumentException("campo inválido: " + campo);
        }
        return nodo.get(campo).asInt();
    }

    private JsonNode arregloObligatorio(JsonNode nodo, String campo) {
        JsonNode valor = nodo.get(campo);
        if (valor == null || !valor.isArray()) {
            throw new IllegalArgumentException("campo inválido: " + campo);
        }
        return valor;
    }
}
