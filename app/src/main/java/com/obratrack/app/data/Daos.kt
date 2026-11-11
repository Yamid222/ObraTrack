package com.obratrack.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ObraDao {
    @Query(
        """
        SELECT o.*,
          (SELECT COUNT(*) FROM etapas WHERE obraId = o.id) AS totalEtapas,
          (SELECT COUNT(*) FROM etapas WHERE obraId = o.id AND estado = 'COMPLETADO') AS completadas,
          (SELECT COALESCE(SUM(monto), 0) FROM gastos WHERE obraId = o.id) AS gastado
        FROM obras o
        ORDER BY o.id DESC
        """
    )
    fun observarResumenes(): Flow<List<ObraResumen>>

    @Query("SELECT * FROM obras WHERE id = :id")
    fun observarObra(id: Long): Flow<Obra?>

    @Query("SELECT * FROM obras WHERE id = :id")
    suspend fun obtenerObra(id: Long): Obra?

    @Insert
    suspend fun insertar(obra: Obra): Long

    @Update
    suspend fun actualizar(obra: Obra)

    @Delete
    suspend fun eliminar(obra: Obra)
}

@Dao
interface EtapaDao {
    @Query("SELECT * FROM etapas WHERE obraId = :obraId ORDER BY fecha ASC, id ASC")
    fun observarEtapas(obraId: Long): Flow<List<Etapa>>

    @Query("SELECT * FROM etapas WHERE id = :id")
    suspend fun obtenerEtapa(id: Long): Etapa?

    @Insert
    suspend fun insertar(etapa: Etapa): Long

    @Update
    suspend fun actualizar(etapa: Etapa)

    @Delete
    suspend fun eliminar(etapa: Etapa)
}

@Dao
interface FotoDao {
    @Query("SELECT * FROM fotos WHERE obraId = :obraId ORDER BY fecha ASC")
    fun observarFotosObra(obraId: Long): Flow<List<Foto>>

    @Query("SELECT ruta FROM fotos WHERE obraId = :obraId")
    suspend fun rutasDeObra(obraId: Long): List<String>

    @Query("SELECT * FROM fotos WHERE etapaId = :etapaId ORDER BY fecha ASC")
    suspend fun obtenerFotosEtapa(etapaId: Long): List<Foto>

    @Insert
    suspend fun insertar(foto: Foto): Long

    @Delete
    suspend fun eliminar(foto: Foto)
}

@Dao
interface TrabajadorDao {
    @Query("SELECT * FROM trabajadores WHERE obraId = :obraId ORDER BY nombre ASC")
    fun observarTrabajadores(obraId: Long): Flow<List<Trabajador>>

    @Query(
        """
        SELECT t.*, COUNT(a.id) AS dias, COALESCE(SUM(a.cantidad), 0.0) AS total
        FROM trabajadores t
        LEFT JOIN asistencias a ON a.trabajadorId = t.id
        WHERE t.obraId = :obraId
        GROUP BY t.id
        ORDER BY t.nombre ASC
        """
    )
    fun observarAcumulados(obraId: Long): Flow<List<AcumuladoTrabajador>>

    @Query("SELECT * FROM trabajadores WHERE id = :id")
    suspend fun obtenerTrabajador(id: Long): Trabajador?

    @Insert
    suspend fun insertar(trabajador: Trabajador): Long

    @Update
    suspend fun actualizar(trabajador: Trabajador)

    @Delete
    suspend fun eliminar(trabajador: Trabajador)
}

@Dao
interface AsistenciaDao {
    @Query("SELECT * FROM asistencias WHERE obraId = :obraId AND fecha = :fecha")
    fun observarDelDia(obraId: Long, fecha: Long): Flow<List<Asistencia>>

    @Query(
        """
        SELECT fecha, COUNT(*) AS asistentes
        FROM asistencias
        WHERE obraId = :obraId
        GROUP BY fecha
        ORDER BY fecha DESC
        """
    )
    fun observarPorFecha(obraId: Long): Flow<List<ResumenFecha>>

    @Query("SELECT * FROM asistencias WHERE obraId = :obraId")
    fun observarTodas(obraId: Long): Flow<List<Asistencia>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(asistencia: Asistencia)

    @Query("DELETE FROM asistencias WHERE trabajadorId = :trabajadorId AND fecha = :fecha")
    suspend fun borrar(trabajadorId: Long, fecha: Long)
}

@Dao
interface GastoDao {
    @Query("SELECT * FROM gastos WHERE obraId = :obraId ORDER BY fecha DESC, id DESC")
    fun observarGastos(obraId: Long): Flow<List<Gasto>>

    @Query("SELECT facturaRuta FROM gastos WHERE obraId = :obraId AND facturaRuta IS NOT NULL")
    suspend fun facturasDeObra(obraId: Long): List<String>

    @Query("SELECT * FROM gastos WHERE id = :id")
    suspend fun obtenerGasto(id: Long): Gasto?

    @Insert
    suspend fun insertar(gasto: Gasto): Long

    @Update
    suspend fun actualizar(gasto: Gasto)

    @Delete
    suspend fun eliminar(gasto: Gasto)
}
