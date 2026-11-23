package uptc.viewController;

import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import uptc.model.Estado;
import uptc.model.Proyecto;

import java.util.ResourceBundle;

/** Panel de indicadores y gráficos del proyecto seleccionado. */
public final class SummaryPane extends VBox {
    private final ProjectStatistics statistics = new ProjectStatistics();
    private final GridPane cards = new GridPane();
    private final PieChart states = new PieChart();
    private final BarChart<String, Number> workload = new BarChart<>(new CategoryAxis(), new NumberAxis());
    private final ResourceBundle texts;

    /** Construye el panel de resumen. */
    public SummaryPane(ResourceBundle texts) { this.texts = texts; setSpacing(12); cards.setHgap(12); cards.setVgap(8); getChildren().addAll(cards, states, workload); }

    /** Actualiza las tarjetas y gráficos sin alterar el dominio. */
    public void render(Proyecto project, java.time.LocalDate finish) {
        cards.getChildren().clear(); states.getData().clear(); workload.getData().clear();
        if (project == null) { getChildren().removeIf(node -> node != cards); return; }
        getChildren().clear(); getChildren().addAll(cards, states, workload);
        var byState = statistics.byState(project);
        for (Estado state : Estado.values()) states.getData().add(new PieChart.Data(state.name(), byState.get(state)));
        var series = new BarChart.Data<String, Number>("Días", 0);
        var data = new javafx.scene.chart.XYChart.Series<String, Number>(); data.setName("Carga");
        statistics.workloadByOwner(project).forEach((owner, days) -> data.getData().add(new javafx.scene.chart.XYChart.Data<>(owner, days)));
        workload.getData().add(data);
        addCard(texts.getString("summary.activities"), Integer.toString(project.cantidadActividades()), 0, 0);
        addCard(texts.getString("summary.progress"), String.format("%.0f%%", statistics.completionPercent(project)), 1, 0);
        addCard(texts.getString("summary.finish"), finish == null ? "-" : finish.toString(), 2, 0);
        addCard(texts.getString("summary.blocked"), Long.toString(statistics.blocked(project)), 3, 0);
    }

    private void addCard(String title, String value, int col, int row) {
        VBox card = new VBox(3, new Label(title), new Label(value));
        card.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-color: #cbd5e1; -fx-border-radius: 5;");
        cards.add(card, col, row);
    }
}
