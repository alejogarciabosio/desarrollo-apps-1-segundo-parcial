# Informe de corrección · Jornadas del Sur

Se corrigieron los pedidos 41 a 44 sin perder datos de instalaciones previas. La app compila (`./gradlew assembleDebug`) y se probó en el emulador con `adb`.

## Cambios por pedido

**Compilación (previo).** `ServicioLocal` usa la API de OkHttp 4 (`toMediaType`, `toResponseBody`), pero Retrofit 2.11 arrastra OkHttp 3 y no compilaba. Se agregó `okhttp:4.12.0` en `app/build.gradle.kts`.

**41 · Programa sin señal** (`data/Programa.kt`, `AgendaRepository.kt`, `ui/Catalogo.kt`, `ui/ProgramaModel.kt`)
- Nueva tabla Room `edicion` que registra que hubo una descarga exitosa, aunque la edición no tenga charlas.
- Si falla y hay edición previa: se mantienen lista, detalle y favoritas, y aparece «No se pudo actualizar. Se están usando datos guardados.».
- Sin edición previa: «No se pudo consultar el programa» con botón «Reintentar».
- Una v4 exitosa muestra «Sin charlas programadas» y, al reabrir sin señal, sigue siendo edición válida.

**42 · Agenda personal** (`CarteleraDao.reemplazarEdicion`)
- Antes `@Insert(REPLACE)` pisaba `favorita` con `false` y nunca borraba charlas retiradas.
- Ahora, en una sola transacción: se leen las marcas vigentes, se vacía el catálogo y se inserta la instantánea conservando la marca por `id`. Las marcas cambiadas durante la petición se respetan porque se leen al escribir.
- Una charla retirada se borra (su marca se pierde) y, si regresa, entra sin marcar.
- Migración Room 1→2 (`MIGRACION_1_2`) sin borrar datos: crea `edicion` y la marca como descargada si ya había charlas.

**43 · Volver al punto de lectura** (`ui/Portada.kt`, `ui/Catalogo.kt`)
- Día y tema vivían en `remember` / en el ViewModel y se perdían al rotar o al cambiar de pantalla. Se subieron a `Portada` con `rememberSaveable` (State Hoisting), junto con la ficha abierta, así sobreviven a rotación y a la muerte del proceso. Una tarea nueva arranca con `Todos`.

**44 · Control de descargas** (`MainActivity.kt`, `ui/ProgramaModel.kt`, `ui/Catalogo.kt`)
- Se quitaron las cargas de `onStart` y de `LaunchedEffect` (se disparaban al rotar, entrar a la ficha y cambiar de pestaña).
- Nueva clase `JornadasApp` (Application, registrada en el Manifest con `android:name`) con un `AppContainer` que crea `Servicios` (Room, Retrofit, `ServicioLocal` con el contador) y guarda el flag de carga inicial. Así son únicos por proceso: salir con Atrás y reabrir no reinicia el contador ni dispara otra carga automática.
- `cargaInicial()` pide el flag al `AppContainer` (se llama después de leer el extra `servicio_disponible`); `recargar()` es la actualización explícita y se ignora si ya hay una petición en curso (`cargando`), así dos toques suman un intento.
- Al terminar (éxito o falla) `cargando` vuelve a `false`. Los filtros no se bloquean durante la espera.

## Cómo se verifica

Con el emulador abierto, `./gradlew installDebug` y la pestaña Mesa para elegir edición y ver el contador:

| Pedido | Prueba | Resultado |
| --- | --- | --- |
| 41 | `pm clear` + `am start ... --ez servicio_disponible false` | «No se pudo consultar…» + Reintentar |
| 41 | v4 → Actualizar; luego reabrir sin señal | «Sin charlas programadas»; sin señal: datos guardados |
| 42 | Marcar charla 11 en la versión anterior, instalar encima, pasar a v2 | Conserva marca, «Compose en producción avanzada», «Sala Puerto» |
| 42 | v3 | 11 desaparece del catálogo y de Mi agenda; entra la 90 |
| 43 | Día 2 + Datos, abrir ficha, volver, rotar | Dos resultados de Datos |
| 43 | Ficha abierta, Inicio, `adb shell am kill ar.jornadas`, Recientes | Recupera ficha y, al volver, ambos filtros |
| 44 | Rotar, abrir ficha, cambiar pestaña | Contador sin cambios (1 auto + explícitas) |
| 44 | Abrir, Atrás, reabrir sin matar el proceso | El contador sigue igual, no suma carga automática |
| 42 | v1 marcar 11, pasar a v3 (desaparece), volver a v2 | La 11 regresa sin marcar y no está en Mi agenda |
| 44 | Dos toques seguidos en Actualizar | +1 intento |
