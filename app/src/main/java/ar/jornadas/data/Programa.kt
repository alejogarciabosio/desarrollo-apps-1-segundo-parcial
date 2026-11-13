package ar.jornadas.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import retrofit2.http.GET
import kotlinx.coroutines.flow.Flow

data class FichaRemota(val id: String, val title: String, val track: String, val day: Int,
    val time: String, val room: String, val speaker: String, val summary: String)

interface ProgramaApi {
    @GET("programa") suspend fun consultar(): List<FichaRemota>
}

@Entity(tableName = "charlas")
data class Charla(@PrimaryKey val id: String, val titulo: String, val tema: String, val dia: Int,
    val hora: String, val sala: String, val ponente: String, val resumen: String,
    val favorita: Boolean = false)

/** Marca de que ya se descargó al menos una edición completa (aunque no tenga charlas). */
@Entity(tableName = "edicion")
data class Edicion(@PrimaryKey val id: Int = 1)

@Dao
abstract class CarteleraDao {
    @Query("SELECT * FROM charlas ORDER BY dia, hora, sala")
    abstract fun observar(): Flow<List<Charla>>

    @Query("SELECT COUNT(*) FROM edicion")
    abstract fun observarEdicion(): Flow<Int>

    @Query("SELECT id FROM charlas WHERE favorita = 1")
    protected abstract suspend fun idsFavoritas(): List<String>

    @Query("DELETE FROM charlas")
    protected abstract suspend fun vaciar()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertar(fichas: List<Charla>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun registrarEdicion(edicion: Edicion)

    /**
     * Reemplaza el catálogo por la instantánea completa. Las marcas se leen dentro de la
     * misma transacción, así que respetan lo que el usuario cambió mientras llegaba la respuesta.
     * Las charlas que ya no están se borran (y si vuelven, vuelven sin marcar).
     */
    @Transaction
    open suspend fun reemplazarEdicion(fichas: List<Charla>) {
        val favoritas = idsFavoritas().toSet()
        vaciar()
        insertar(fichas.map { it.copy(favorita = it.id in favoritas) })
        registrarEdicion(Edicion())
    }

    @Query("UPDATE charlas SET favorita = NOT favorita WHERE id = :id")
    abstract suspend fun alternar(id: String)
}

@Database(entities = [Charla::class, Edicion::class], version = 2, exportSchema = false)
abstract class Archivo : RoomDatabase() {
    abstract fun cartelera(): CarteleraDao
}

/** v1 -> v2: agrega la marca de edición y conserva charlas y favoritas ya instaladas. */
val MIGRACION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `edicion` (`id` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("INSERT INTO edicion (id) SELECT 1 WHERE EXISTS (SELECT 1 FROM charlas)")
    }
}
