package ar.jornadas

import android.app.Application
import java.util.concurrent.atomic.AtomicBoolean

/** AppContainer: dependencias únicas por proceso (sobreviven a que se cierre la Activity). */
class AppContainer(app: Application) {
    val servicios = Servicios(app)
    private val cargaInicial = AtomicBoolean(false)

    /** true solo la primera vez en este proceso. */
    fun reclamarCargaInicial(): Boolean = cargaInicial.compareAndSet(false, true)
}

class JornadasApp : Application() {
    val container by lazy { AppContainer(this) }
}
