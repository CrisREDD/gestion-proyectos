# Manual de usuario

## Iniciar la aplicación

La aplicación requiere Java 17 o posterior. Para abrirla, ejecuta la clase `uptc.App` desde el IDE con las dependencias Maven cargadas. `mvn clean test` compila el proyecto y ejecuta las pruebas. La aplicación guarda los proyectos en `.gestion-proyectos/proyectos.json` dentro de la carpeta del usuario. La primera ejecución muestra la lista vacía.

Para abrir el ejemplo de demostración en memoria, inicia con el argumento `--demo`. Ese modo carga el proyecto **Lanzar app** y no guarda cambios al disco.

## Proyectos

En la pestaña **Proyectos**, crea, edita o elimina proyectos. La fecha se escribe con el formato `AAAA-MM-DD`. Usa el selector de la parte superior para elegir el proyecto activo; sus actividades y dependencias aparecen en las otras pestañas. La eliminación solicita confirmación.

## Actividades y dependencias

En **Actividades**, crea cada actividad con un ID único, nombre y duración en días calendario. Descripción y responsable son opcionales. Selecciona una actividad y pulsa **Cambiar estado** para registrar si está pendiente, en curso o terminada.

En **Dependencias**, elige una predecesora y una sucesora y agrega la relación. Si la relación crea un ciclo, la aplicación muestra el error correspondiente. Para borrar una relación, selecciónala y confirma.

## Planificación y avance

En **Orden y cronograma**, pulsa **Calcular planificación** para ver el orden de ejecución y las fechas estimadas de inicio y fin.

En **Estado y avance**, filtra todas las actividades por su estado o muestra las bloqueadas. Una actividad queda bloqueada cuando alguna predecesora todavía no está terminada.

## Simular y aplicar cambios

En **Impacto**, simula el cambio de duración de una actividad o la eliminación de una dependencia. Revisa la fecha final anterior y la nueva, los días de diferencia y las actividades afectadas. Pulsa el botón de aplicar y confirma para guardar el cambio.

## Idioma

El selector de la esquina superior derecha cambia entre español e inglés y vuelve a cargar las etiquetas de toda la interfaz. Los datos del proyecto se conservan al cambiar el idioma.

## Mensajes de error

Los formularios indican los campos obligatorios y validan la fecha y la duración. Los errores de dominio y persistencia se muestran en el idioma elegido.
