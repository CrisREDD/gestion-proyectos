package uptc.viewController;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import uptc.controller.algorithm.PlanificadorService;
import uptc.model.Actividad;
import uptc.model.Estado;
import uptc.model.Proyecto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Vista ligera del grafo de dependencias, organizada por niveles topológicos. */
public final class DependencyGraphPane extends Pane {
    private final PlanificadorService planner;
    private final Map<String, VBox> nodes = new HashMap<>();

    /** Construye la vista con el planificador de la aplicación. */
    public DependencyGraphPane(PlanificadorService planner) {
        this.planner = planner;
        setMinSize(700, 420);
        setStyle("-fx-background-color: #f8fafc;");
    }

    /** Dibuja el proyecto y devuelve el nodo seleccionado mediante el listener indicado. */
    public void render(Proyecto project, java.util.function.Consumer<Actividad> onSelected,
                       Set<String> cycleIds) {
        getChildren().clear(); nodes.clear();
        if (project == null || project.cantidadActividades() == 0) return;
        List<List<Actividad>> levels = planner.niveles(project);
        double columnWidth = Math.max(170, getWidth() / Math.max(1, levels.size()));
        if (columnWidth == 170) columnWidth = 190;
        Map<String, double[]> centers = new HashMap<>();
        for (int column = 0; column < levels.size(); column++) {
            List<Actividad> level = levels.get(column);
            for (int row = 0; row < level.size(); row++) {
                Actividad activity = level.get(row);
                double x = 20 + column * columnWidth;
                double y = 30 + row * 90;
                VBox node = node(activity, onSelected, cycleIds != null && cycleIds.contains(activity.getId()));
                node.setLayoutX(x); node.setLayoutY(y); nodes.put(activity.getId(), node);
                getChildren().add(node);
                centers.put(activity.getId(), new double[]{x + 70, y + 22});
            }
        }
        for (String from : project.getIds()) {
            for (String to : project.getSucesoras(from)) {
                double[] a = centers.get(from), b = centers.get(to);
                if (a == null || b == null) continue;
                Line line = new Line(a[0], a[1], b[0], b[1]);
                line.setStroke(Color.web("#94a3b8")); line.setStrokeWidth(1.5);
                Polygon arrow = new Polygon(b[0], b[1], b[0] - 8, b[1] - 4, b[0] - 8, b[1] + 4);
                arrow.setFill(Color.web("#64748b"));
                getChildren().addAll(0, List.of(line, arrow));
            }
        }
    }

    private VBox node(Actividad activity, java.util.function.Consumer<Actividad> listener, boolean cycle) {
        Label title = new Label(activity.getId() + " · " + activity.getNombre());
        title.setStyle("-fx-font-weight: bold;");
        Label state = new Label(activity.getEstado().name());
        VBox box = new VBox(3, title, state);
        box.setPadding(new Insets(8)); box.setPrefWidth(140); box.setPrefHeight(45);
        box.setStyle("-fx-background-color: " + color(activity.getEstado())
                + "; -fx-border-color: " + (cycle ? "#dc2626" : "#64748b")
                + "; -fx-border-width: " + (cycle ? "3" : "1") + "; -fx-border-radius: 5; -fx-background-radius: 5;");
        box.setOnMouseClicked(e -> listener.accept(activity));
        return box;
    }

    private String color(Estado state) {
        return switch (state) { case TODO -> "#e2e8f0"; case IN_PROGRESS -> "#fef3c7"; case DONE -> "#bbf7d0"; };
    }
}
