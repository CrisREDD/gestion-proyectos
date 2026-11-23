package uptc.viewController;

import uptc.Demo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.geometry.Insets;
import javafx.util.StringConverter;
import uptc.exception.DominioException;
import uptc.exception.DependenciaCircularException;
import uptc.exception.PersistenciaException;
import uptc.controller.algorithm.PlanificadorServiceImpl;
import uptc.model.*;
import uptc.controller.ProyectoService;
import uptc.utils.DateUtils;
import uptc.utils.ValidationUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

/** Controlador de las vistas de proyectos, actividades y planificación. */
public class MainController {
    @FXML private BorderPane root;

    private ProyectoService service;
    private ResourceBundle textos;
    private Consumer<Locale> cambiarIdioma;
    private ComboBox<Proyecto> selectorProyecto;
    private TabPane tabs;
    private TableView<Actividad> tablaActividades;
    private TableView<Actividad> tablaEstados;
    private TableView<Proyecto> tablaProyectos;
    private Label impactoResultado;
    private ImpactoResultado simulacionDuracion;
    private ImpactoResultado simulacionDependencia;
    private String idSimulado;
    private Integer duracionSimulada;
    private String predSimulada;
    private String sucSimulada;
    private boolean temaOscuro;
    private Set<String> cycleIds = Set.of();
    private final PlanificadorServiceImpl planificador = new PlanificadorServiceImpl();
    private DependencyGraphPane graphPane;
    private GanttPane ganttPane;
    private SummaryPane summaryPane;

    public void configure(ProyectoService service, ResourceBundle textos, Consumer<Locale> cambiarIdioma) {
        this.service = Objects.requireNonNull(service);
        this.textos = Objects.requireNonNull(textos);
        this.cambiarIdioma = Objects.requireNonNull(cambiarIdioma);
        construirInterfaz();
    }

    private void construirInterfaz() {
        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(16));
        layout.setTop(construirCabecera());
        layout.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.N) { crearActividad(); event.consume(); }
            else if (event.getCode() == KeyCode.DELETE) {
                eliminarProyecto(tablaProyectos == null ? null : tablaProyectos.getSelectionModel().getSelectedItem());
                event.consume();
            } else if (event.isControlDown() && event.getCode() == KeyCode.L) {
                cambiarIdioma.accept(textos.getLocale().getLanguage().equals("en")
                        ? Locale.forLanguageTag("es") : Locale.forLanguageTag("en"));
                event.consume();
            }
        });
        tabs = new TabPane();
        tabs.getTabs().setAll(tabProyectos(), tabActividades(), tabDependencias(),
                tabPlanificacion(), tabEstados(), tabImpacto(), tabResumen(), tabGrafo(), tabGantt());
        layout.setCenter(tabs);
        root.setCenter(layout);
        refrescarProyectos(null);
    }

    private Node construirCabecera() {
        Label title = new Label(tr("app.title"));
        title.getStyleClass().add("title");
        selectorProyecto = new ComboBox<>();
        selectorProyecto.setPromptText(tr("project.select"));
        selectorProyecto.setMaxWidth(Double.MAX_VALUE);
        selectorProyecto.setConverter(new StringConverter<>() {
            @Override public String toString(Proyecto p) {
                return p == null ? "" : p.getNombre() + " (" + p.getId() + ")";
            }
            @Override public Proyecto fromString(String s) { return null; }
        });
        selectorProyecto.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Proyecto p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null : p.getNombre() + " (" + p.getId() + ")");
            }
        });
        selectorProyecto.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Proyecto p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? tr("project.select") : p.getNombre() + " (" + p.getId() + ")");
            }
        });
        selectorProyecto.setOnAction(e -> refrescarVistas());

        ComboBox<String> idioma = new ComboBox<>(FXCollections.observableArrayList("Español", "English"));
        idioma.setValue(textos.getLocale().getLanguage().equals("en") ? "English" : "Español");
        idioma.setOnAction(e -> cambiarIdioma.accept(idioma.getValue().equals("English")
                ? Locale.forLanguageTag("en") : Locale.forLanguageTag("es")));
        Button nuevoProyecto = button("project.new", e -> crearProyecto());
        Button cargarEjemplo = button("project.loadExample", e -> cargarEjemplo());
        Button tema = button("theme.toggle", e -> alternarTema());
        HBox fila = new HBox(12, title, selectorProyecto, nuevoProyecto, cargarEjemplo, idioma, tema);
        fila.setPadding(new Insets(0, 0, 14, 0));
        HBox.setHgrow(selectorProyecto, Priority.ALWAYS);
        return fila;
    }

    private void alternarTema() {
        temaOscuro = !temaOscuro;
        if (temaOscuro) root.getStyleClass().add("dark-theme");
        else root.getStyleClass().remove("dark-theme");
    }

    private Tab tabProyectos() {
        tablaProyectos = new TableView<>();
        tablaProyectos.getColumns().setAll(
                columna("project.id", "id", Proyecto::getId),
                columna("project.name", "name", Proyecto::getNombre),
                columna("project.start", "start", p -> formatearFecha(p.getFechaInicio())));
        tablaProyectos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaProyectos.setPlaceholder(new Label(tr("project.empty")));
        Button add = button("project.new", e -> crearProyecto());
        Button edit = button("project.edit", e -> editarProyecto(tablaProyectos.getSelectionModel().getSelectedItem()));
        Button delete = button("project.delete", e -> eliminarProyecto(tablaProyectos.getSelectionModel().getSelectedItem()));
        tablaProyectos.setOnMouseClicked(e -> {
            Proyecto p = tablaProyectos.getSelectionModel().getSelectedItem();
            if (p != null) selectorProyecto.getSelectionModel().select(p);
        });
        VBox box = new VBox(10, tablaProyectos, new HBox(8, add, edit, delete));
        VBox.setVgrow(tablaProyectos, Priority.ALWAYS);
        return new Tab(tr("tab.projects"), box);
    }

    private Tab tabActividades() {
        tablaActividades = crearTablaActividades();
        Button add = button("activity.new", e -> crearActividad());
        Button state = button("activity.changeState", e -> cambiarEstado(tablaActividades.getSelectionModel().getSelectedItem()));
        VBox box = new VBox(10, new Label(tr("activity.help")), tablaActividades, new HBox(8, add, state));
        VBox.setVgrow(tablaActividades, Priority.ALWAYS);
        return new Tab(tr("tab.activities"), box);
    }

    private Tab tabDependencias() {
        ComboBox<Actividad> pred = actividadSelector();
        ComboBox<Actividad> suc = actividadSelector();
        TableView<DependenciaFila> tabla = new TableView<>();
        tabla.getColumns().setAll(
                columna("dependency.predecessor", "pred", d -> d.pred().getId() + " · " + d.pred().getNombre()),
                columna("dependency.successor", "suc", d -> d.suc().getId() + " · " + d.suc().getNombre()));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        Button add = button("dependency.add", e -> envolver(() -> {
            Proyecto p = proyectoActual();
            String predId = id(pred.getValue()), sucId = id(suc.getValue());
            Proyecto copia = p.copiar();
            copia.agregarDependencia(predId, sucId);
            try {
                new PlanificadorServiceImpl().ordenEjecucion(copia);
            } catch (DependenciaCircularException ciclo) {
                cycleIds = Set.copyOf(ciclo.getIdsInvolucrados());
                mostrarError(tr("error.dependencia.circular") + "\n"
                        + tr("dependency.cyclePath") + ": " + String.join(" → ", ciclo.getIdsInvolucrados()));
                return;
            }
            service.agregarDependencia(p.getId(), predId, sucId);
            refrescarVistas();
        }));
        Button remove = button("dependency.remove", e -> {
            DependenciaFila fila = tabla.getSelectionModel().getSelectedItem();
            if (fila == null) { aviso("validation.selectDependency"); return; }
            confirmar(tr("dependency.remove.confirm"), () -> envolver(() -> {
                service.eliminarDependencia(proyectoActual().getId(), fila.pred().getId(), fila.suc().getId());
                refrescarVistas();
            }));
        });
        VBox box = new VBox(10,
                new Label(tr("dependency.help")), new HBox(8, pred, suc, add, remove), tabla);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        Tab tab = new Tab(tr("tab.dependencies"), box);
        tab.setOnSelectionChanged(e -> { if (tab.isSelected()) { actualizarSelector(pred); actualizarSelector(suc); tabla.refresh(); } });
        tab.setUserData(tabla);
        return tab;
    }

    private Tab tabPlanificacion() {
        ListView<String> orden = new ListView<>();
        TableView<CronogramaFila> fechas = new TableView<>();
        fechas.getColumns().setAll(
                columna("activity.id", "id", f -> f.id()),
                columna("activity.name", "name", f -> f.nombre()),
                columna("schedule.start", "start", f -> formatearFecha(f.cronograma().inicio())),
                columna("schedule.finish", "finish", f -> formatearFecha(f.cronograma().fin())));
        fechas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        Label fin = new Label();
        Button calcular = button("planning.calculate", e -> envolver(() -> {
            Proyecto p = proyectoActual();
            orden.getItems().setAll(service.ordenEjecucion(p.getId()).stream()
                    .map(a -> a.getId() + " · " + a.getNombre()).toList());
            Map<String, Cronograma> cronograma = service.cronograma(p.getId());
            fechas.getItems().setAll(p.getIds().stream().map(id -> new CronogramaFila(id,
                    p.getActividad(id).getNombre(), cronograma.get(id))).toList());
            fin.setText(tr("schedule.projectFinish") + ": " + cronograma.values().stream()
                    .map(Cronograma::fin).max(Comparator.naturalOrder()).map(this::formatearFecha).orElse(formatearFecha(p.getFechaInicio())));
        }));
        VBox box = new VBox(10, new HBox(8, calcular), new Label(tr("planning.order")), orden,
                new Label(tr("planning.schedule")), fechas, fin);
        VBox.setVgrow(orden, Priority.ALWAYS);
        VBox.setVgrow(fechas, Priority.ALWAYS);
        return new Tab(tr("tab.planning"), box);
    }

    private Tab tabEstados() {
        ComboBox<String> filtro = new ComboBox<>(FXCollections.observableArrayList(
                tr("filter.all"), estado(Estado.TODO), estado(Estado.IN_PROGRESS), estado(Estado.DONE), tr("filter.blocked")));
        filtro.setValue(tr("filter.all"));
        tablaEstados = crearTablaActividades();
        filtro.setOnAction(e -> refrescarEstados(filtro.getValue()));
        Button cambiar = button("activity.changeState", e -> cambiarEstado(tablaEstados.getSelectionModel().getSelectedItem()));
        VBox box = new VBox(10, new Label(tr("status.help")), new HBox(8, new Label(tr("status.filter")), filtro),
                tablaEstados, cambiar);
        VBox.setVgrow(tablaEstados, Priority.ALWAYS);
        Tab tab = new Tab(tr("tab.status"), box);
        tab.setOnSelectionChanged(e -> { if (tab.isSelected()) refrescarEstados(filtro.getValue()); });
        return tab;
    }

    private Tab tabImpacto() {
        ComboBox<Actividad> actividad = actividadSelector();
        TextField nuevaDuracion = new TextField(); nuevaDuracion.setPromptText(tr("activity.duration"));
        Button simularDuracion = button("impact.simulateDuration", e -> envolver(() -> {
            idSimulado = id(actividad.getValue());
            duracionSimulada = enteroPositivo(nuevaDuracion.getText());
            simulacionDuracion = service.simularCambioDuracion(proyectoActual().getId(), idSimulado, duracionSimulada);
            mostrarImpacto(simulacionDuracion);
        }));
        Button aplicarDuracion = button("impact.applyDuration", e -> {
            if (simulacionDuracion == null || duracionSimulada == null) { aviso("impact.simulateFirst"); return; }
            confirmar(tr("impact.confirmApply"), () -> envolver(() -> {
                proyectoActual().getActividad(idSimulado).setDuracionDias(duracionSimulada);
                service.actualizarProyecto(proyectoActual());
                simulacionDuracion = null;
                refrescarVistas();
            }));
        });

        ComboBox<Actividad> pred = actividadSelector();
        ComboBox<Actividad> suc = actividadSelector();
        Button simularEliminar = button("impact.simulateDependency", e -> envolver(() -> {
            predSimulada = id(pred.getValue()); sucSimulada = id(suc.getValue());
            simulacionDependencia = service.simularEliminarDependencia(proyectoActual().getId(), predSimulada, sucSimulada);
            mostrarImpacto(simulacionDependencia);
        }));
        Button aplicarEliminar = button("impact.applyDependency", e -> {
            if (simulacionDependencia == null) { aviso("impact.simulateFirst"); return; }
            confirmar(tr("impact.confirmApply"), () -> envolver(() -> {
                service.eliminarDependencia(proyectoActual().getId(), predSimulada, sucSimulada);
                simulacionDependencia = null;
                refrescarVistas();
            }));
        });

        impactoResultado = new Label(tr("impact.resultPlaceholder"));
        impactoResultado.setWrapText(true);
        VBox box = new VBox(12,
                new Label(tr("impact.durationTitle")), new HBox(8, actividad, nuevaDuracion, simularDuracion, aplicarDuracion),
                new Separator(), new Label(tr("impact.dependencyTitle")), new HBox(8, pred, suc, simularEliminar, aplicarEliminar),
                new Separator(), impactoResultado);
        Tab tab = new Tab(tr("tab.impact"), box);
        tab.setOnSelectionChanged(e -> { if (tab.isSelected()) { actualizarSelector(actividad); actualizarSelector(pred); actualizarSelector(suc); } });
        return tab;
    }

    private Tab tabResumen() {
        summaryPane = new SummaryPane(textos);
        Tab tab = new Tab(tr("tab.summary"), summaryPane);
        tab.setOnSelectionChanged(e -> { if (tab.isSelected()) actualizarResumen(); });
        return tab;
    }

    private Tab tabGrafo() {
        graphPane = new DependencyGraphPane(planificador);
        Tab tab = new Tab(tr("tab.graph"), graphPane);
        tab.setOnSelectionChanged(e -> { if (tab.isSelected()) actualizarGrafo(); });
        return tab;
    }

    private Tab tabGantt() {
        ganttPane = new GanttPane(textos);
        Tab tab = new Tab(tr("tab.gantt"), ganttPane);
        tab.setOnSelectionChanged(e -> { if (tab.isSelected()) actualizarGantt(); });
        return tab;
    }

    private void actualizarResumen() {
        Proyecto project = selectorProyecto == null ? null : selectorProyecto.getValue();
        if (project == null) { if (summaryPane != null) summaryPane.render(null, null); return; }
        Map<String, Cronograma> schedule = service.cronograma(project.getId());
        summaryPane.render(project, planificador.fechaFin(project, schedule));
    }

    private void actualizarGrafo() {
        Proyecto project = selectorProyecto == null ? null : selectorProyecto.getValue();
        if (graphPane != null) graphPane.render(project, a -> mostrarDetalles(a), cycleIds);
    }

    private void actualizarGantt() {
        Proyecto project = selectorProyecto == null ? null : selectorProyecto.getValue();
        if (ganttPane != null && project != null) ganttPane.render(project, service.cronograma(project.getId()));
        else if (ganttPane != null) ganttPane.render(null, null);
    }

    private void mostrarDetalles(Actividad activity) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(activity.getId());
        alert.setHeaderText(activity.getNombre());
        Proyecto project = selectorProyecto.getValue();
        alert.setContentText(tr("graph.duration") + ": " + activity.getDuracionDias()
                + "\n" + tr("graph.owner") + ": " + Objects.toString(activity.getResponsable(), "-")
                + "\n" + tr("graph.predecessors") + ": " + project.getPredecesoras(activity.getId())
                + "\n" + tr("graph.successors") + ": " + project.getSucesoras(activity.getId()));
        alert.showAndWait();
    }

    private TableView<Actividad> crearTablaActividades() {
        TableView<Actividad> tabla = new TableView<>();
        tabla.getColumns().setAll(
                columna("activity.id", "id", Actividad::getId),
                columna("activity.name", "name", Actividad::getNombre),
                columna("activity.duration", "duration", a -> Integer.toString(a.getDuracionDias())),
                columna("activity.owner", "owner", a -> Objects.toString(a.getResponsable(), "")),
                columna("activity.state", "state", a -> estado(a.getEstado())));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label(tr("activity.empty")));
        return tabla;
    }

    private <T> TableColumn<T, String> columna(String key, String id, java.util.function.Function<T, String> valor) {
        TableColumn<T, String> col = new TableColumn<>(tr(key));
        col.setId(id);
        col.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(valor.apply(data.getValue())));
        return col;
    }

    private ComboBox<Actividad> actividadSelector() {
        ComboBox<Actividad> combo = new ComboBox<>();
        combo.setPromptText(tr("activity.select"));
        combo.setConverter(new StringConverter<>() {
            @Override public String toString(Actividad a) { return a == null ? "" : a.getId() + " · " + a.getNombre(); }
            @Override public Actividad fromString(String s) { return null; }
        });
        combo.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Actividad a, boolean empty) {
                super.updateItem(a, empty);
                setText(empty || a == null ? null : a.getId() + " · " + a.getNombre());
            }
        });
        return combo;
    }

    private Button button(String key, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Button button = new Button(tr(key)); button.setOnAction(action); return button;
    }

    private void crearProyecto() {
        List<String> valores = formulario(tr("project.new"), List.of(
                new Campo(tr("project.name"), ""), new Campo(tr("project.description"), "", false),
                new Campo(tr("project.start"), LocalDate.now().toString())));
        if (valores == null) return;
        envolver(() -> {
            LocalDate fecha = parseDate(valores.get(2));
            Proyecto creado = service.crearProyecto(valores.get(0), valores.get(1), fecha);
            refrescarProyectos(creado.getId());
        });
    }

    private void editarProyecto(Proyecto p) {
        if (p == null) { aviso("validation.selectProject"); return; }
        List<String> valores = formulario(tr("project.edit"), List.of(
                new Campo(tr("project.name"), p.getNombre()), new Campo(tr("project.description"), Objects.toString(p.getDescripcion(), ""), false),
                new Campo(tr("project.start"), p.getFechaInicio().toString())));
        if (valores == null) return;
        envolver(() -> {
            p.setNombre(valores.get(0)); p.setDescripcion(valores.get(1)); p.setFechaInicio(parseDate(valores.get(2)));
            service.actualizarProyecto(p); refrescarProyectos(p.getId());
        });
    }

    private void eliminarProyecto(Proyecto p) {
        if (p == null) { aviso("validation.selectProject"); return; }
        confirmar(tr("project.delete.confirm"), () -> envolver(() -> {
            service.eliminarProyecto(p.getId()); refrescarProyectos(null);
        }));
    }

    private void crearActividad() {
        Proyecto p = proyectoActual(); if (p == null) return;
        List<String> valores = formulario(tr("activity.new"), List.of(
                new Campo(tr("activity.id"), ""), new Campo(tr("activity.name"), ""),
                new Campo(tr("activity.description"), "", false), new Campo(tr("activity.duration"), "1"),
                new Campo(tr("activity.owner"), "", false)));
        if (valores == null) return;
        envolver(() -> {
            int dias = enteroPositivo(valores.get(3));
            Actividad actividad = new Actividad(valores.get(0).trim(), valores.get(1).trim(), dias);
            actividad.setDescripcion(valores.get(2));
            if (!valores.get(4).isBlank()) actividad.setResponsable(valores.get(4).trim());
            service.agregarActividad(p.getId(), actividad); refrescarVistas();
        });
    }

    private void cambiarEstado(Actividad a) {
        if (a == null) { aviso("validation.selectActivity"); return; }
        List<String> estados = Arrays.stream(Estado.values()).map(this::estado).toList();
        ChoiceDialog<String> dialog = new ChoiceDialog<>(estado(a.getEstado()), estados);
        dialog.setTitle(tr("activity.changeState")); dialog.setHeaderText(tr("activity.chooseState"));
        dialog.setContentText(tr("activity.state"));
        dialog.showAndWait().ifPresent(etiqueta -> envolver(() -> {
            Estado nuevo = Arrays.stream(Estado.values()).filter(e -> estado(e).equals(etiqueta)).findFirst().orElseThrow();
            service.registrarAvance(proyectoActual().getId(), a.getId(), nuevo); refrescarVistas();
        }));
    }

    private List<String> formulario(String titulo, List<Campo> campos) {
        Dialog<List<String>> dialog = new Dialog<>();
        dialog.setTitle(titulo); dialog.setHeaderText(titulo);
        ButtonType guardar = new ButtonType(tr("common.save"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(guardar, ButtonType.CANCEL);
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(8); grid.setPadding(new Insets(10));
        List<TextField> inputs = new ArrayList<>();
        for (int i = 0; i < campos.size(); i++) {
            TextField input = new TextField(campos.get(i).valor());
            grid.addRow(i, new Label(campos.get(i).nombre()), input); inputs.add(input);
        }
        dialog.getDialogPane().setContent(grid);
        Node ok = dialog.getDialogPane().lookupButton(guardar);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            boolean faltanObligatorios = java.util.stream.IntStream.range(0, inputs.size())
                    .anyMatch(i -> campos.get(i).obligatorio() && (inputs.get(i).getText() == null
                            || inputs.get(i).getText().isBlank()));
            if (faltanObligatorios) {
                mostrarError(tr("validation.required")); event.consume();
            }
        });
        dialog.setResultConverter(b -> b == guardar ? inputs.stream().map(TextField::getText).toList() : null);
        return dialog.showAndWait().orElse(null);
    }

    private void confirmar(String mensaje, Runnable accion) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.YES, ButtonType.NO);
        alert.setTitle(tr("common.confirm")); alert.setHeaderText(null);
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) accion.run();
    }

    private void refrescarProyectos(String seleccionarId) {
        List<Proyecto> proyectos = service.listarProyectos();
        selectorProyecto.getItems().setAll(proyectos);
        if (tablaProyectos != null) tablaProyectos.getItems().setAll(proyectos);
        Proyecto seleccionado = proyectos.stream().filter(p -> Objects.equals(p.getId(), seleccionarId)).findFirst()
                .orElseGet(() -> proyectos.isEmpty() ? null : proyectos.get(0));
        selectorProyecto.getSelectionModel().select(seleccionado);
        refrescarVistas();
    }

    private void refrescarVistas() {
        Proyecto p = selectorProyecto == null ? null : selectorProyecto.getValue();
        if (p == null) {
            if (tablaActividades != null) tablaActividades.getItems().clear();
            if (tablaEstados != null) tablaEstados.getItems().clear();
            return;
        }
        if (tablaActividades != null) tablaActividades.getItems().setAll(p.getIds().stream().map(p::getActividad).toList());
        if (tablaEstados != null) refrescarEstados(tr("filter.all"));
        if (tablaProyectos != null) tablaProyectos.getItems().setAll(service.listarProyectos());
        for (Tab tab : tabs.getTabs()) {
            if (tab.getUserData() instanceof TableView<?> t && tab.getText().equals(tr("tab.dependencies"))) {
                @SuppressWarnings("unchecked") TableView<DependenciaFila> tabla = (TableView<DependenciaFila>) t;
                List<DependenciaFila> aristas = new ArrayList<>();
                for (String a : p.getIds()) for (String b : p.getSucesoras(a))
                    aristas.add(new DependenciaFila(p.getActividad(a), p.getActividad(b)));
                tabla.getItems().setAll(aristas);
            }
        }
        for (Node node : root.lookupAll("ComboBox")) {
            if (node instanceof ComboBox<?> combo && combo.getPromptText() != null
                    && (combo.getPromptText().equals(tr("activity.select")))) {
                @SuppressWarnings("unchecked") ComboBox<Actividad> actividades = (ComboBox<Actividad>) combo;
                actualizarSelector(actividades);
            }
        }
        actualizarResumen();
        actualizarGrafo();
        actualizarGantt();
    }

    private void refrescarEstados(String filtro) {
        Proyecto p = proyectoActual(); if (p == null || tablaEstados == null) return;
        List<Actividad> todas = p.getIds().stream().map(p::getActividad).toList();
        if (tr("filter.blocked").equals(filtro)) {
            tablaEstados.getItems().setAll(todas.stream().filter(this::bloqueada).toList());
        } else if (!tr("filter.all").equals(filtro)) {
            Estado estado = Arrays.stream(Estado.values()).filter(e -> estado(e).equals(filtro)).findFirst().orElse(null);
            tablaEstados.getItems().setAll(todas.stream().filter(a -> a.getEstado() == estado).toList());
        } else tablaEstados.getItems().setAll(todas);
    }

    private boolean bloqueada(Actividad a) {
        Proyecto p = proyectoActual();
        return a.getEstado() != Estado.DONE && p.getPredecesoras(a.getId()).stream()
                .anyMatch(pred -> p.getActividad(pred).getEstado() != Estado.DONE);
    }

    private void actualizarSelector(ComboBox<Actividad> combo) {
        Proyecto p = proyectoActual();
        String idActual = combo.getValue() == null ? null : combo.getValue().getId();
        combo.getItems().setAll(p == null ? List.of() : p.getIds().stream().map(p::getActividad).toList());
        if (idActual != null) combo.getItems().stream().filter(a -> a.getId().equals(idActual)).findFirst()
                .ifPresent(a -> combo.getSelectionModel().select(a));
    }

    private void mostrarImpacto(ImpactoResultado resultado) {
        impactoResultado.setText(tr("impact.before") + ": " + formatearFecha(resultado.finAntes()) + "\n"
                + tr("impact.after") + ": " + formatearFecha(resultado.finDespues()) + "\n"
                + tr("impact.delta") + ": " + resultado.diasDeDiferencia() + "\n"
                + tr("impact.affected") + ": " + String.join(", ", resultado.actividadesAfectadas()));
    }

    private int enteroPositivo(String texto) {
        return ValidationUtils.positiveDuration(texto);
    }

    private void cargarEjemplo() {
        envolver(() -> {
            Proyecto sample = Demo.crearEjemplo();
            Proyecto created = service.crearProyecto(sample.getNombre(), sample.getDescripcion(), sample.getFechaInicio());
            for (String id : sample.getIds()) {
                Actividad original = sample.getActividad(id);
                Actividad copy = new Actividad(original.getId(), original.getNombre(), original.getDuracionDias());
                copy.setDescripcion(original.getDescripcion());
                copy.setResponsable(original.getResponsable());
                copy.setEstado(original.getEstado());
                service.agregarActividad(created.getId(), copy);
            }
            for (String from : sample.getIds()) {
                for (String to : sample.getSucesoras(from)) service.agregarDependencia(created.getId(), from, to);
            }
            refrescarProyectos(created.getId());
        });
    }

    private LocalDate parseDate(String texto) {
        return DateUtils.parseIso(texto);
    }

    private Proyecto proyectoActual() {
        Proyecto p = selectorProyecto == null ? null : selectorProyecto.getValue();
        if (p == null) aviso("validation.selectProject");
        return p;
    }

    private String id(Actividad a) {
        if (a == null) throw new DominioException("error.actividad.inexistente");
        return a.getId();
    }

    private String estado(Estado e) { return tr("estado." + e.name()); }
    private String formatearFecha(LocalDate fecha) {
        return DateUtils.format(fecha, textos.getLocale());
    }
    private String tr(String key) { return textos.getString(key); }
    private void aviso(String key) { mostrarError(tr(key)); }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle(tr("error.title")); alerta.setHeaderText(tr("error.header")); alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private void envolver(Runnable accion) {
        try { accion.run(); }
        catch (RuntimeException e) {
            String clave = e instanceof DominioException de ? de.getClave()
                    : e instanceof PersistenciaException pe ? pe.getClave() : "error.general";
            mostrarError(textos.containsKey(clave) ? tr(clave) : tr("error.general"));
        }
    }

    private record Campo(String nombre, String valor, boolean obligatorio) {
        private Campo(String nombre, String valor) { this(nombre, valor, true); }
    }
    private record DependenciaFila(Actividad pred, Actividad suc) { }
    private record CronogramaFila(String id, String nombre, Cronograma cronograma) { }
}
