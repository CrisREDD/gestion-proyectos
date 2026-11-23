package uptc;

import uptc.controller.algorithm.PlanificadorService;
import uptc.controller.algorithm.PlanificadorServiceImpl;
import uptc.model.*;

import java.time.LocalDate;

/** Ejemplo "Lanzar una app". Ejecutar con: java -cp target/classes uptc.Demo */
public class Demo {

    public static Proyecto crearEjemplo() {
        Proyecto p = new Proyecto("P1", "Lanzar app", LocalDate.of(2026, 11, 2));

        p.agregarActividad(new Actividad("A", "Requisitos", 3));
        p.agregarActividad(new Actividad("B", "Diseño BD", 2));
        p.agregarActividad(new Actividad("C", "Diseño UI", 4));
        p.agregarActividad(new Actividad("D", "Backend", 5));
        p.agregarActividad(new Actividad("E", "Frontend", 4));
        p.agregarActividad(new Actividad("F", "Pruebas", 3));
        p.agregarActividad(new Actividad("G", "Documentación", 2));

        p.agregarDependencia("A", "B");
        p.agregarDependencia("A", "C");
        p.agregarDependencia("A", "G");
        p.agregarDependencia("B", "D");
        p.agregarDependencia("C", "E");
        p.agregarDependencia("D", "F");
        p.agregarDependencia("E", "F");
        return p;
    }

    public static void main(String[] args) {
        Proyecto p = crearEjemplo();
        PlanificadorService plan = new PlanificadorServiceImpl();

        System.out.println("== Orden de ejecución ==");
        plan.ordenEjecucion(p).forEach(a -> System.out.println("  " + a.getId() + " " + a.getNombre()));

        System.out.println("== Niveles (cada nivel puede ir a la vez) ==");
        plan.niveles(p).forEach(n ->
                System.out.println("  " + n.stream().map(Actividad::getId).toList()));

        System.out.println("== Cronograma ==");
        var cronograma = plan.calcularFechas(p);
        cronograma.forEach((id, c) ->
                System.out.println("  " + id + ": " + c.inicio() + " -> " + c.fin()));
        System.out.println("  Fin del proyecto: " + plan.fechaFin(p, cronograma));

        System.out.println("== Simultaneidad ==");
        System.out.println("  C y D simultáneas: " + plan.sonSimultaneas(p, "C", "D"));
        System.out.println("  B y D simultáneas: " + plan.sonSimultaneas(p, "B", "D"));
        System.out.println("  G y F simultáneas: " + plan.sonSimultaneas(p, "G", "F"));

        System.out.println("== Ciclo ==");
        System.out.println("  F -> A cerraría ciclo: " + plan.cerrariaCiclo(p, "F", "A"));

        System.out.println("== Bloqueadas (solo A terminada) ==");
        p.getActividad("A").setEstado(Estado.DONE);
        System.out.println("  " + plan.actividadesBloqueadas(p).stream().map(Actividad::getId).toList());

        System.out.println("== Impacto ==");
        ImpactoResultado i1 = plan.impactoCambioDuracion(p, "D", 7);
        System.out.println("  D=7 -> fin " + i1.finDespues() + " (" + i1.diasDeDiferencia()
                + " día(s)), afectadas " + i1.actividadesAfectadas());
        ImpactoResultado i2 = plan.impactoCambioDuracion(p, "E", 3);
        System.out.println("  E=3 -> fin " + i2.finDespues() + " (" + i2.diasDeDiferencia()
                + " día(s)), afectadas " + i2.actividadesAfectadas());
        ImpactoResultado i3 = plan.impactoEliminarDependencia(p, "E", "F");
        System.out.println("  sin E->F -> fin " + i3.finDespues() + " (" + i3.diasDeDiferencia()
                + " día(s)), afectadas " + i3.actividadesAfectadas());
    }
}
