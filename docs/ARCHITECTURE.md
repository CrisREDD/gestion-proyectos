# Documentación técnica

## 1. Infraestructura

El proyecto es un módulo Java llamado `uptc`, construido con Maven. `pom.xml`
declara JavaFX 21.0.2, Jackson para JSON, JUnit/Mockito para pruebas, Surefire
para ejecutar tests y JaCoCo para cobertura. `javafx-maven-plugin` configura el
arranque de `uptc.App` mediante `mvn javafx:run`.

La aplicación guarda los datos del usuario en
`~/.gestion-proyectos/proyectos.json`. Ese archivo no pertenece al repositorio.
El DAO escribe primero un temporal y después reemplaza el archivo, reduciendo
el riesgo de dejar un JSON incompleto si el proceso se interrumpe.

## 2. Capas y dependencias

```text
App (composición manual)
 └── viewController ──> controller ──> model
                            └───────> persistence (interfaces)
 utils y exception son transversales
```

### `uptc.model`

Contiene `Proyecto`, `Actividad`, `Estado`, `Cronograma` e
`ImpactoResultado`. Estas clases representan el dominio y no importan
JavaFX ni clases de persistencia.

- `Proyecto` mantiene actividades y las aristas de precedencia.
- `Actividad` contiene identidad, nombre, duración, responsable y estado.
- `Estado` define `TODO`, `IN_PROGRESS` y `DONE`.
- `Cronograma` representa fecha de inicio y fin calculadas.
- `ImpactoResultado` representa fechas antes/después y actividades afectadas.

### `uptc.controller`

`ProyectoService` es la fachada de casos de uso. `ProyectoServiceImpl` mantiene
el mapa de proyectos en memoria y coordina validación de dominio, persistencia y
algoritmos. Sus métodos principales son:

- `crearProyecto`, `actualizarProyecto` y `eliminarProyecto`.
- `agregarActividad`, `agregarDependencia` y `eliminarDependencia`.
- `registrarAvance` con la regla de predecesoras terminadas.
- `ordenEjecucion`, `cronograma` y simulaciones de impacto.

`ProyectoServiceStub` usa un DAO en memoria para el modo `--demo`; no altera el
servicio real ni requiere una base de datos.

### `uptc.controller.algorithm`

`PlanificadorServiceImpl` implementa los algoritmos de grafos:

- `ordenEjecucion`: orden topológico de Kahn.
- `niveles`: agrupa nodos que pueden ejecutarse en paralelo.
- `existeCamino`: búsqueda DFS.
- `cerrariaCiclo`: comprueba si una arista nueva crea un ciclo.
- `calcularFechas`: calcula el inicio por el máximo fin de predecesoras.
- `actividadesBloqueadas`: identifica tareas con predecesoras pendientes.
- `impactoCambioDuracion` e `impactoEliminarDependencia`: calculan escenarios
  sobre copias o mapas de duraciones, sin modificar el proyecto original.

### `uptc.persistence`

`ProyectoRepository` es la abstracción DAO. `JsonProyectoRepository` transforma
el dominio a JSON y viceversa con Jackson. La dependencia se inyecta desde
`App` y desde las pruebas; no se usa Singleton porque un estado global
dificultaría reemplazar el almacenamiento y aislar cada test.

### `uptc.utils`

- `ValidationUtils`: IDs y duraciones positivas.
- `DateUtils`: parseo ISO y formato dependiente del Locale.
- `I18n`: carga centralizada de `ResourceBundle`.

Estas utilidades no dependen de JavaFX y pueden probarse de forma aislada.

### `uptc.viewController`

`MainController` compone la navegación y el estado del proyecto seleccionado.
`DependencyGraphPane`, `GanttPane` y `SummaryPane` son componentes visuales
JavaFX. `ProjectStatistics` calcula indicadores sin JavaFX para mantener la
lógica de estadísticas testeable.

- El grafo coloca actividades por niveles topológicos y dibuja aristas.
- Los nodos se colorean por estado y pueden marcar ciclos.
- Gantt usa `Cronograma.inicio()` y `Cronograma.fin()`.
- El resumen muestra tarjetas, actividades por estado y carga por responsable.

## 3. Variables de estado importantes

- `proyectos`: mapa de proyectos cargados en `ProyectoServiceImpl`.
- `repositorio`: DAO inyectado para leer/escribir JSON.
- `planificador`: servicio de algoritmos usado por casos de uso y vistas.
- `selectorProyecto`: proyecto activo en la UI.
- `cycleIds`: IDs involucrados en el último ciclo detectado, usados para
  resaltado visual.
- `temaOscuro`: estado del tema claro/oscuro.
- `simulacionDuracion` y `simulacionDependencia`: resultados temporales antes
  de aplicar un cambio de impacto.

## 4. Recursos e internacionalización

Las vistas están en `src/main/resources/uptc/view`, el CSS en `css`, y los dos
bundles en `i18n`. Las claves de español e inglés se validan con
`I18nResourcesTest`; ambos archivos deben conservar exactamente el mismo
conjunto de claves.

`resources-data/proyecto-ejemplo.json` es un recurso versionado. Los datos
generados, `target/` y archivos de IDE están excluidos por `.gitignore`.

## 5. Pruebas y cobertura

```bash
mvn clean test
mvn clean verify
```

El reporte se genera en `target/site/jacoco/index.html`. Las clases JavaFX de
presentación se excluyen del umbral porque requieren un entorno gráfico; el
umbral se aplica a modelo, servicios, algoritmos, persistencia e integración.
