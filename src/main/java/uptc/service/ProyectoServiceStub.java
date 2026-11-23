package uptc.service;

import uptc.Demo;
import uptc.algorithm.PlanificadorServiceImpl;
import uptc.exception.PersistenciaException;
import uptc.model.Proyecto;
import uptc.persistence.ProyectoRepository;

import java.util.ArrayList;
import java.util.List;

/** Servicio de demostración en memoria, inicializado con el ejemplo "Lanzar una app". */
public final class ProyectoServiceStub extends ProyectoServiceImpl {
    public ProyectoServiceStub() {
        super(repositorioDemo(), new PlanificadorServiceImpl());
    }

    private static ProyectoRepository repositorioDemo() {
        return new RepositorioMemoria(Demo.crearEjemplo());
    }

    private static final class RepositorioMemoria implements ProyectoRepository {
        private final List<Proyecto> proyectos = new ArrayList<>();

        private RepositorioMemoria(Proyecto inicial) {
            proyectos.add(inicial);
        }

        @Override
        public List<Proyecto> cargarTodos() {
            return List.copyOf(proyectos);
        }

        @Override
        public void guardar(Proyecto proyecto) {
            proyectos.removeIf(p -> p.getId().equals(proyecto.getId()));
            proyectos.add(proyecto);
        }

        @Override
        public void eliminar(String idProyecto) {
            boolean eliminado = proyectos.removeIf(p -> p.getId().equals(idProyecto));
            if (!eliminado) throw new PersistenciaException("error.proyecto.inexistente");
        }
    }
}
