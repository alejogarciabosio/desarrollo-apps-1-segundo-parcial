package ar.jornadas.data

import kotlinx.coroutines.flow.map

class AgendaRepository(private val api: ProgramaApi, private val dao: CarteleraDao) {
    val programa = dao.observar()
    /** null mientras no se sabe; true si ya hubo una edición descargada con éxito. */
    val hayEdicion = dao.observarEdicion().map { it > 0 }

    suspend fun sincronizar() {
        val fichas = api.consultar().map {
            Charla(it.id, it.title, it.track, it.day, it.time, it.room, it.speaker, it.summary)
        }
        dao.reemplazarEdicion(fichas)
    }

    suspend fun cambiarFavorita(id: String) = dao.alternar(id)
}
