package ar.jornadas.ui

import android.app.Application
import androidx.lifecycle.*
import ar.jornadas.AppContainer
import ar.jornadas.JornadasApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProgramaModel(app: Application) : AndroidViewModel(app) {
    private val contenedor: AppContainer = (app as JornadasApp).container
    private val servicios = contenedor.servicios
    val mesa = servicios.mesa
    private val repo = servicios.agenda
    val charlas = repo.programa.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    /** null = todavía leyendo el archivo local. */
    val hayEdicion: StateFlow<Boolean?> =
        repo.hayEdicion.map<Boolean, Boolean?> { it }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val fallo = MutableStateFlow(false)
    val cargando = MutableStateFlow(false)

    /** Una sola carga automática por proceso: el flag vive en el AppContainer, no en el ViewModel. */
    fun cargaInicial() {
        if (contenedor.reclamarCargaInicial()) recargar()
    }

    /** Actualización explícita. Se ignora si ya hay una petición en curso. */
    fun recargar() {
        if (cargando.value) return
        cargando.value = true
        fallo.value = false
        viewModelScope.launch {
            try {
                repo.sincronizar()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fallo.value = true
            } finally {
                cargando.value = false
            }
        }
    }

    fun marcar(id: String) { viewModelScope.launch { repo.cambiarFavorita(id) } }
}
