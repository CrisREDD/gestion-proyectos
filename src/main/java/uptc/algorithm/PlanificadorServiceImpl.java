package uptc.algorithm;

import uptc.exception.DependenciaCircularException;
import uptc.exception.DominioException;
import uptc.model.*;

import java.time.LocalDate;
import java.util.*;

public class PlanificadorServiceImpl implements PlanificadorService {

    /** Kahn: orden topológico. Lanza excepción si hay ciclo (RF05, RF06). */
    @Override
    public List<Actividad> ordenEjecucion(Proyecto p) {
        Map<String, Integer> grado = gradosDeEntrada(p);

        Deque<String> cola = new ArrayDeque<>();
        grado.forEach((id, g) -> { if (g == 0) cola.add(id); });

        List<Actividad> orden = new ArrayList<>();
        while (!cola.isEmpty()) {
            String id = cola.poll();
            orden.add(p.getActividad(id));
            for (String suc : p.getSucesoras(id)) {
                if (grado.merge(suc, -1, Integer::sum) == 0) cola.add(suc);
            }
        }

        if (orden.size() < p.cantidadActividades())
            throw new DependenciaCircularException(cicloExacto(p));
        return orden;
    }

    /** Kahn por niveles: cada nivel agrupa actividades que pueden ir a la vez. */
    @Override
    public List<List<Actividad>> niveles(Proyecto p) {
        Map<String, Integer> grado = gradosDeEntrada(p);

        List<String> actual = new ArrayList<>();
        grado.forEach((id, g) -> { if (g == 0) actual.add(id); });

        List<List<Actividad>> niveles = new ArrayList<>();
        int procesadas = 0;
        List<String> ronda = actual;

        while (!ronda.isEmpty()) {
            List<Actividad> nivel = new ArrayList<>();
            List<String> siguiente = new ArrayList<>();
            for (String id : ronda) {
                nivel.add(p.getActividad(id));
                procesadas++;
                for (String suc : p.getSucesoras(id)) {
                    if (grado.merge(suc, -1, Integer::sum) == 0) siguiente.add(suc);
                }
            }
            niveles.add(nivel);
            ronda = siguiente;
        }

        if (procesadas < p.cantidadActividades())
            throw new DependenciaCircularException(cicloExacto(p));
        return niveles;
    }

    /** ¿Hay camino de "desde" a "hasta"? DFS iterativo. */
    @Override
    public boolean existeCamino(Proyecto p, String desde, String hasta) {
        if (p.getActividad(desde) == null || p.getActividad(hasta) == null)
            throw new DominioException("error.actividad.inexistente");
        Set<String> visitados = new HashSet<>();
        Deque<String> pila = new ArrayDeque<>();
        pila.push(desde);
        while (!pila.isEmpty()) {
            String id = pila.pop();
            if (id.equals(hasta)) return true;
            if (visitados.add(id)) {
                for (String suc : p.getSucesoras(id)) pila.push(suc);
            }
        }
        return false;
    }

    /** La arista A -> B cerraría un ciclo si ya existe un camino B ... A. */
    @Override
    public boolean cerrariaCiclo(Proyecto p, String idA, String idB) {
        return idA.equals(idB) || existeCamino(p, idB, idA);
    }

    /** Simultáneas (exacto): no hay camino en ninguna dirección. */
    @Override
    public boolean sonSimultaneas(Proyecto p, String x, String y) {
        return !x.equals(y) && !existeCamino(p, x, y) && !existeCamino(p, y, x);
    }

    @Override
    public Map<String, Cronograma> calcularFechas(Proyecto p) {
        return calcularFechas(p, Map.of());
    }

    /** duraciones: sustituye la duración de ciertas actividades solo para este cálculo. */
    Map<String, Cronograma> calcularFechas(Proyecto p, Map<String, Integer> duraciones) {
        Map<String, Cronograma> res = new LinkedHashMap<>();
        for (Actividad a : ordenEjecucion(p)) {
            int dur = duraciones.getOrDefault(a.getId(), a.getDuracionDias());
            LocalDate inicio = p.getFechaInicio();
            for (String pred : p.getPredecesoras(a.getId())) {
                LocalDate finPred = res.get(pred).fin();
                if (finPred.isAfter(inicio)) inicio = finPred;
            }
            res.put(a.getId(), new Cronograma(inicio, inicio.plusDays(dur)));
        }
        return res;
    }

    /** Fecha de fin del proyecto = la fin más tardía. */
    @Override
    public LocalDate fechaFin(Proyecto p, Map<String, Cronograma> cronograma) {
        return cronograma.values().stream()
                .map(Cronograma::fin)
                .max(Comparator.naturalOrder())
                .orElse(p.getFechaInicio());
    }

    /** Bloqueada: no está terminada y alguna predecesora no está terminada (RF04, RF09). */
    @Override
    public List<Actividad> actividadesBloqueadas(Proyecto p) {
        List<Actividad> res = new ArrayList<>();
        for (String id : p.getIds()) {
            Actividad a = p.getActividad(id);
            if (a.getEstado() == Estado.DONE) continue;
            boolean bloqueada = p.getPredecesoras(id).stream()
                    .anyMatch(pr -> p.getActividad(pr).getEstado() != Estado.DONE);
            if (bloqueada) res.add(a);
        }
        return res;
    }

    /** RF08: qué pasa si cambio la duración de una actividad. No modifica el proyecto. */
    @Override
    public ImpactoResultado impactoCambioDuracion(Proyecto p, String idAct, int nuevaDuracion) {
        if (p.getActividad(idAct) == null)
            throw new DominioException("error.actividad.inexistente");
        if (nuevaDuracion <= 0)
            throw new DominioException("error.actividad.duracion.invalida");
        Map<String, Cronograma> antes = calcularFechas(p);
        Map<String, Cronograma> despues = calcularFechas(p, Map.of(idAct, nuevaDuracion));
        return comparar(p, antes, despues);
    }

    /** RF08: qué pasa si elimino una dependencia. Trabaja sobre una copia. */
    @Override
    public ImpactoResultado impactoEliminarDependencia(Proyecto p, String idA, String idB) {
        Map<String, Cronograma> antes = calcularFechas(p);
        Proyecto copia = p.copiar();
        copia.eliminarDependencia(idA, idB);
        return comparar(p, antes, calcularFechas(copia));
    }

    // ---------- auxiliares ----------

    private Map<String, Integer> gradosDeEntrada(Proyecto p) {
        Map<String, Integer> grado = new LinkedHashMap<>();
        for (String id : p.getIds()) grado.put(id, p.getPredecesoras(id).size());
        return grado;
    }

    /** DFS de colores: devuelve solo los nodos de un ciclo, sin arrastrar sus descendientes. */
    private List<String> cicloExacto(Proyecto p) {
        Map<String, Integer> color = new HashMap<>(); // 0 blanco, 1 gris, 2 negro
        List<String> camino = new ArrayList<>();
        for (String id : p.getIds()) {
            List<String> ciclo = buscarCiclo(p, id, color, camino);
            if (ciclo != null) return ciclo;
        }
        return List.of();
    }

    private List<String> buscarCiclo(Proyecto p, String id, Map<String, Integer> color,
                                     List<String> camino) {
        int estado = color.getOrDefault(id, 0);
        if (estado == 2) return null;
        if (estado == 1) {
            int inicio = camino.indexOf(id);
            return new ArrayList<>(camino.subList(inicio, camino.size()));
        }
        color.put(id, 1);
        camino.add(id);
        for (String suc : p.getSucesoras(id)) {
            List<String> ciclo = buscarCiclo(p, suc, color, camino);
            if (ciclo != null) return ciclo;
        }
        camino.remove(camino.size() - 1);
        color.put(id, 2);
        return null;
    }

    private ImpactoResultado comparar(Proyecto p,
                                      Map<String, Cronograma> antes,
                                      Map<String, Cronograma> despues) {
        List<String> afectadas = new ArrayList<>();
        antes.forEach((id, c) -> { if (!c.equals(despues.get(id))) afectadas.add(id); });
        return new ImpactoResultado(fechaFin(p, antes), fechaFin(p, despues), afectadas);
    }
}
