package uptc;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import uptc.controller.algorithm.PlanificadorServiceImpl;
import uptc.exception.DominioException;
import uptc.exception.PersistenciaException;
import uptc.persistence.JsonProyectoRepository;
import uptc.controller.ProyectoService;
import uptc.controller.ProyectoServiceImpl;
import uptc.controller.ProyectoServiceStub;
import uptc.viewController.MainController;

import java.nio.file.Path;
import java.util.Locale;
import java.util.ResourceBundle;

/** Punto de entrada JavaFX y ensamblado de dependencias. */
public class App extends Application {
    private Stage stage;
    private ProyectoService proyectoService;
    private Locale locale = Locale.forLanguageTag("es");

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        var icon = App.class.getResourceAsStream("/uptc/images/logo.svg");
        if (icon != null) stage.getIcons().add(new Image(icon));
        try {
            boolean demo = getParameters().getRaw().contains("--demo")
                    || Boolean.getBoolean("uptc.demo");
            if (demo) {
                proyectoService = new ProyectoServiceStub();
            } else {
                Path archivo = Path.of(System.getProperty("user.home"), ".gestion-proyectos", "proyectos.json");
                proyectoService = new ProyectoServiceImpl(new JsonProyectoRepository(archivo),
                        new PlanificadorServiceImpl());
            }
            mostrarVentana();
        } catch (RuntimeException e) {
            mostrarError(e, locale);
            stage.close();
        }
    }

    private void mostrarVentana() throws Exception {
        ResourceBundle bundle = ResourceBundle.getBundle("uptc.i18n.messages", locale);
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/uptc/view/main.fxml"), bundle);
        Parent root = loader.load();
        root.getStylesheets().add(App.class.getResource("/uptc/css/styles.css").toExternalForm());
        MainController controller = loader.getController();
        controller.configure(proyectoService, bundle, this::cambiarIdioma);
        stage.setTitle(bundle.getString("app.title"));
        stage.setMinWidth(900);
        stage.setMinHeight(620);
        stage.setScene(new Scene(root, 1180, 760));
        stage.show();
    }

    private void cambiarIdioma(Locale nuevo) {
        locale = nuevo;
        try {
            mostrarVentana();
        } catch (Exception e) {
            mostrarError(e, locale);
        }
    }

    private static void mostrarError(Throwable error, Locale idioma) {
        String clave = error instanceof DominioException de ? de.getClave()
                : error instanceof PersistenciaException pe ? pe.getClave() : "error.general";
        ResourceBundle bundle = ResourceBundle.getBundle("uptc.i18n.messages", idioma);
        String mensaje = bundle.containsKey(clave) ? bundle.getString(clave) : bundle.getString("error.general");
        javafx.scene.control.Alert alerta = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
        alerta.setTitle(bundle.getString("error.title"));
        alerta.setHeaderText(bundle.getString("error.header"));
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
