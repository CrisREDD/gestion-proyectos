package uptc.viewController;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import uptc.model.Cronograma;
import uptc.model.Proyecto;

import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.ResourceBundle;

/** Vista de cronograma tipo Gantt construida con controles JavaFX estándar. */
public final class GanttPane extends VBox {
    private ResourceBundle texts;
    /** Construye una vista vacía de Gantt. */
    public GanttPane(ResourceBundle texts) { this.texts = texts; setSpacing(6); setPadding(new Insets(8)); }

    /** Dibuja barras proporcionales a la duración del cronograma. */
    public void render(Proyecto project, Map<String, Cronograma> schedule) {
        getChildren().clear();
        if (project == null || schedule == null || schedule.isEmpty()) {
            getChildren().add(new Label(texts.getString("gantt.empty"))); return;
        }
        long total = Math.max(1, schedule.values().stream().mapToLong(c -> ChronoUnit.DAYS.between(c.inicio(), c.fin())).sum());
        for (String id : project.getIds()) {
            Cronograma c = schedule.get(id); if (c == null) continue;
            long days = Math.max(1, ChronoUnit.DAYS.between(c.inicio(), c.fin()));
            Label name = new Label(id + "  " + project.getActividad(id).getNombre()); name.setPrefWidth(190);
            Rectangle bar = new Rectangle(Math.max(30, 520.0 * days / total), 22, Color.web("#2563eb"));
            Label dates = new Label(c.inicio() + " → " + c.fin());
            HBox row = new HBox(8, name, bar, dates); row.setPadding(new Insets(2));
            getChildren().add(row);
        }
    }
}
