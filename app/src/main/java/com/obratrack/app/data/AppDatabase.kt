package com.obratrack.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Obra::class, Etapa::class, Foto::class, Trabajador::class, Asistencia::class, Gasto::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun obraDao(): ObraDao
    abstract fun etapaDao(): EtapaDao
    abstract fun fotoDao(): FotoDao
    abstract fun trabajadorDao(): TrabajadorDao
    abstract fun asistenciaDao(): AsistenciaDao
    abstract fun gastoDao(): GastoDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun obtener(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "obratrack.db"
                )
                    // Etapa de desarrollo: si cambia la estructura, se recrea la base local.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instancia = it }
            }
    }
}
