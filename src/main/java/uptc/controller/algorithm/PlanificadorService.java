package uptc.controller.algorithm;

import uptc.model.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Contrato del núcleo algorítmico (lo implementa la Persona A). */
public interface PlanificadorService {
    List<Actividad> ordenEjecucion(Proyecto p);                                      // RF06
    List<List<Actividad>> niveles(Proyecto p);                                       // RF06 (paralelismo)
    boolean existeCamino(Proyecto p, String desde, String hasta);
    boolean cerrariaCiclo(Proyecto p, String idA, String idB);                       // RF05
    boolean sonSimultaneas(Proyecto p, String x, String y);
    Map<String, Cronograma> calcularFechas(Proyecto p);                              // RF07
    LocalDate fechaFin(Proyecto p, Map<String, Cronograma> cronograma);
    List<Actividad> actividadesBloqueadas(Proyecto p);                               // RF04, RF09
    ImpactoResultado impactoCambioDuracion(Proyecto p, String idAct, int dias);      // RF08
    ImpactoResultado impactoEliminarDependencia(Proyecto p, String idA, String idB); // RF08
}
