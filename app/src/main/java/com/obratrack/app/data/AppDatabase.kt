package com.obratrack.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Obra::class, Etapa::class, Foto::class, Trabajador::class, Asistencia::class, Gasto::class],
    version = 3,
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
        /** v3: horas por día y días por semana del trabajador; marca de gastos creados desde la asistencia. */
        private val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trabajadores ADD COLUMN horasDia REAL NOT NULL DEFAULT 8")
                db.execSQL("ALTER TABLE trabajadores ADD COLUMN diasSemana INTEGER NOT NULL DEFAULT 6")
                db.execSQL("ALTER TABLE gastos ADD COLUMN deAsistencia INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var instancia: AppDatabase? = null

        fun obtener(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "obratrack.db"
                )
                    .addMigrations(MIGRACION_2_3)
                    // Etapa de desarrollo: si cambia la estructura, se recrea la base local.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instancia = it }
            }
    }
}
