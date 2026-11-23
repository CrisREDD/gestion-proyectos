package uptc;

import javafx.application.Application;

/** Punto de entrada compatible con la ejecución del JAR sombreado en classpath. */
public final class Launcher {
    private Launcher() { }

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
