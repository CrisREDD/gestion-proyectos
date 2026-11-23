# Gestión de proyectos

Aplicación JavaFX 21 para gestionar proyectos, actividades y dependencias como
un grafo dirigido. Incluye orden topológico, detección de ciclos, cronograma,
análisis de impacto, grafo visual, Gantt y resumen estadístico.

## Requisitos y ejecución

- JDK 17 o superior (el proyecto compila con `maven.compiler.release=17`).
- Maven 3.9 o superior.
- JavaFX 21.0.2, descargado por Maven.

```bash
mvn clean test
mvn clean verify
mvn javafx:run
```

El modo de demostración conserva el proyecto de ejemplo:

```bash
mvn javafx:run -Djavafx.run.args=--demo
```

## Arquitectura

La explicación detallada de infraestructura, métodos, variables, persistencia
y decisiones de diseño está en [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

```text
uptc.model                 Dominio sin dependencias de UI
uptc.controller            Casos de uso y servicios de aplicación
uptc.controller.algorithm  Algoritmos de grafos y planificación
uptc.persistence           DAO JSON reemplazable en pruebas
uptc.exception             Excepciones de dominio y persistencia
uptc.utils                 Validación, fechas e i18n transversal
uptc.viewController        Controladores y componentes JavaFX
src/main/resources/uptc/view  Vistas FXML
src/main/resources/uptc/css   Tema visual
src/main/resources/uptc/i18n  Traducciones español/inglés
resources-data              JSON versionable de ejemplo
```

`App` realiza la composición manual de dependencias. La persistencia se inyecta
por `ProyectoRepository`, por lo que las pruebas usan repositorios en memoria
sin Singleton global.

## Interfaz

La aplicación cuenta con pestañas para proyectos, actividades, dependencias,
planificación, estados, impacto, resumen, grafo y Gantt. El grafo organiza las
actividades por niveles topológicos y colorea sus estados; el Gantt usa el
cronograma calculado por el algoritmo existente. Los indicadores se calculan
en `ProjectStatistics`, una clase sin JavaFX que se puede probar aisladamente.

## Datos y cobertura

Los datos de la aplicación se guardan fuera del repositorio, en
`.gestion-proyectos/proyectos.json`. El archivo `resources-data/proyecto-ejemplo.json`
es un recurso versionable para pruebas y demostraciones. `target/`, archivos de
IDE y datos locales están excluidos mediante `.gitignore` para evitar conflictos.

El reporte de cobertura se genera en `target/site/jacoco/index.html`. Las vistas
JavaFX se mantienen ligeras y la lógica estadística se prueba sin levantar una
ventana; la cobertura exigida se concentra en dominio, controladores,
algoritmos y persistencia.
