package com.obratrack.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.obratrack.app.data.AcumuladoTrabajador
import com.obratrack.app.data.AppDatabase
import com.obratrack.app.data.Asistencia
import com.obratrack.app.data.CategoriaGasto
import com.obratrack.app.data.EstadoEtapa
import com.obratrack.app.data.Etapa
import com.obratrack.app.data.Foto
import com.obratrack.app.data.Gasto
import com.obratrack.app.data.Obra
import com.obratrack.app.data.ObraResumen
import com.obratrack.app.data.ResumenFecha
import com.obratrack.app.data.TipoPago
import com.obratrack.app.data.Trabajador
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ObraViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.obtener(application)
    private val obraDao = db.obraDao()
    private val etapaDao = db.etapaDao()
    private val fotoDao = db.fotoDao()
    private val trabajadorDao = db.trabajadorDao()
    private val asistenciaDao = db.asistenciaDao()
    private val gastoDao = db.gastoDao()

    // ---------- Obras (RF-01) ----------
    val resumenes: StateFlow<List<ObraResumen>> = obraDao.observarResumenes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun obra(id: Long): Flow<Obra?> = obraDao.observarObra(id)
    suspend fun cargarObra(id: Long): Obra? = obraDao.obtenerObra(id)

    fun guardarObra(obra: Obra, alTerminar: () -> Unit) {
        viewModelScope.launch {
            if (obra.id == 0L) obraDao.insertar(obra) else obraDao.actualizar(obra)
            alTerminar()
        }
    }

    /** Elimina la obra con todo lo asociado (etapas, fotos, personal, asistencia y gastos). */
    fun eliminarObra(obraId: Long, alTerminar: () -> Unit) {
        viewModelScope.launch {
            val obra = obraDao.obtenerObra(obraId) ?: return@launch alTerminar()
            val archivos = fotoDao.rutasDeObra(obraId) + gastoDao.facturasDeObra(obraId)
            obraDao.eliminar(obra) // las tablas hijas se borran en cascada
            archivos.forEach { Archivos.borrar(it) }
            alTerminar()
        }
    }

    // ---------- Etapas y fotos (RF-02, RF-03, RF-10) ----------
    fun etapas(obraId: Long): Flow<List<Etapa>> = etapaDao.observarEtapas(obraId)
    fun fotos(obraId: Long): Flow<List<Foto>> = fotoDao.observarFotosObra(obraId)

    suspend fun cargarEtapa(id: Long): Etapa? = etapaDao.obtenerEtapa(id)
    suspend fun cargarFotosEtapa(etapaId: Long): List<Foto> = fotoDao.obtenerFotosEtapa(etapaId)

    fun guardarEtapa(
        etapa: Etapa,
        fotosNuevas: List<Foto>,
        fotosEliminadas: List<Foto>,
        alTerminar: () -> Unit
    ) {
        viewModelScope.launch {
            val etapaId = if (etapa.id == 0L) {
                etapaDao.insertar(etapa)
            } else {
                etapaDao.actualizar(etapa)
                etapa.id
            }
            fotosNuevas.forEach { fotoDao.insertar(it.copy(etapaId = etapaId)) }
            fotosEliminadas.forEach {
                fotoDao.eliminar(it)
                Archivos.borrar(it.ruta)
            }
            alTerminar()
        }
    }

    fun eliminarEtapa(etapa: Etapa, alTerminar: () -> Unit) {
        viewModelScope.launch {
            val fotos = fotoDao.obtenerFotosEtapa(etapa.id)
            etapaDao.eliminar(etapa)
            fotos.forEach { Archivos.borrar(it.ruta) }
            alTerminar()
        }
    }

    // ---------- Personal (RF-04) ----------
    fun trabajadores(obraId: Long): Flow<List<Trabajador>> = trabajadorDao.observarTrabajadores(obraId)
    fun acumulados(obraId: Long): Flow<List<AcumuladoTrabajador>> = trabajadorDao.observarAcumulados(obraId)
    suspend fun cargarTrabajador(id: Long): Trabajador? = trabajadorDao.obtenerTrabajador(id)

    fun guardarTrabajador(trabajador: Trabajador, alTerminar: () -> Unit) {
        viewModelScope.launch {
            if (trabajador.id == 0L) trabajadorDao.insertar(trabajador) else trabajadorDao.actualizar(trabajador)
            alTerminar()
        }
    }

    fun eliminarTrabajador(trabajador: Trabajador, alTerminar: () -> Unit) {
        viewModelScope.launch {
            trabajadorDao.eliminar(trabajador)
            alTerminar()
        }
    }

    // ---------- Asistencia (RF-05) ----------
    fun asistenciaDelDia(obraId: Long, fecha: Long): Flow<List<Asistencia>> =
        asistenciaDao.observarDelDia(obraId, fecha)

    fun asistenciaPorFecha(obraId: Long): Flow<List<ResumenFecha>> =
        asistenciaDao.observarPorFecha(obraId)

    fun asistencias(obraId: Long): Flow<List<Asistencia>> = asistenciaDao.observarTodas(obraId)

    fun guardarAsistencia(
        fecha: Long,
        presentes: List<Asistencia>,
        ausentes: List<Long>,
        alTerminar: () -> Unit
    ) {
        viewModelScope.launch {
            db.withTransaction {
                presentes.forEach { asistenciaDao.guardar(it) }
                ausentes.forEach { asistenciaDao.borrar(it, fecha) }
            }
            alTerminar()
        }
    }

    // ---------- Gastos y facturas (RF-07, RF-08, RF-09) ----------
    fun gastos(obraId: Long): Flow<List<Gasto>> = gastoDao.observarGastos(obraId)
    suspend fun cargarGasto(id: Long): Gasto? = gastoDao.obtenerGasto(id)

    fun guardarGasto(gasto: Gasto, rutaParaBorrar: String?, alTerminar: () -> Unit) {
        viewModelScope.launch {
            if (gasto.id == 0L) gastoDao.insertar(gasto) else gastoDao.actualizar(gasto)
            Archivos.borrar(rutaParaBorrar)
            alTerminar()
        }
    }

    fun eliminarGasto(gasto: Gasto, alTerminar: () -> Unit) {
        viewModelScope.launch {
            gastoDao.eliminar(gasto)
            Archivos.borrar(gasto.facturaRuta)
            alTerminar()
        }
    }

    /** Registra como gasto de mano de obra lo calculado por asistencia que aún no se ha sumado. */
    fun sumarManoDeObra(obraId: Long, monto: Long, alTerminar: () -> Unit) {
        if (monto <= 0L) return
        viewModelScope.launch {
            val hoy = Formato.hoy()
            gastoDao.insertar(
                Gasto(
                    obraId = obraId,
                    descripcion = "Mano de obra según asistencia (hasta ${Formato.fecha(hoy)})",
                    monto = monto,
                    fecha = hoy,
                    categoria = CategoriaGasto.MANO_DE_OBRA.name,
                    deAsistencia = true
                )
            )
            alTerminar()
        }
    }

    // ---------- Datos de ejemplo para probar la app ----------
    fun cargarEjemplo() {
        viewModelScope.launch {
            val hoy = Formato.hoy()
            val dia = 86_400_000L
            val obraId = obraDao.insertar(
                Obra(
                    nombre = "Casa familiar Rodríguez",
                    direccion = "Vereda El Rosal, Tunja",
                    fechaInicio = hoy - 45 * dia,
                    presupuesto = 85_000_000L,
                    descripcion = "Vivienda de dos pisos con local en el primer nivel."
                )
            )
            etapaDao.insertar(Etapa(obraId = obraId, nombre = "Cimentación", descripcion = "Zapatas y vigas de amarre terminadas.", estado = EstadoEtapa.COMPLETADO.name, fecha = hoy - 40 * dia))
            etapaDao.insertar(Etapa(obraId = obraId, nombre = "Estructura", descripcion = "Columnas y placa del primer piso.", estado = EstadoEtapa.COMPLETADO.name, fecha = hoy - 25 * dia))
            etapaDao.insertar(Etapa(obraId = obraId, nombre = "Mampostería", descripcion = "Levante de muros del primer piso.", estado = EstadoEtapa.EN_PROCESO.name, fecha = hoy - 6 * dia))
            etapaDao.insertar(Etapa(obraId = obraId, nombre = "Acabados", descripcion = "Pañete, pisos y pintura.", estado = EstadoEtapa.PENDIENTE.name, fecha = hoy + 10 * dia))

            val pedro = trabajadorDao.insertar(Trabajador(obraId = obraId, nombre = "Pedro Gómez", cargo = "Oficial de obra", tipoPago = TipoPago.JORNAL.name, valor = 90_000L))
            trabajadorDao.insertar(Trabajador(obraId = obraId, nombre = "Jorge Ruiz", cargo = "Maestro de obra", tipoPago = TipoPago.SEMANA.name, valor = 600_000L, horasDia = 9.0, diasSemana = 6))
            val luis = trabajadorDao.insertar(Trabajador(obraId = obraId, nombre = "Luis Martínez", cargo = "Ayudante", tipoPago = TipoPago.JORNAL.name, valor = 60_000L))
            val carlos = trabajadorDao.insertar(Trabajador(obraId = obraId, nombre = "Carlos Pérez", cargo = "Instalador de pisos", tipoPago = TipoPago.DESTAJO.name, valor = 18_000L))
            for (i in 0..2) {
                val fecha = hoy - i * dia
                asistenciaDao.guardar(Asistencia(obraId = obraId, trabajadorId = pedro, fecha = fecha, cantidad = 8.0))
                asistenciaDao.guardar(Asistencia(obraId = obraId, trabajadorId = luis, fecha = fecha, cantidad = 8.0))
            }
            asistenciaDao.guardar(Asistencia(obraId = obraId, trabajadorId = carlos, fecha = hoy, cantidad = 12.0))

            gastoDao.insertar(Gasto(obraId = obraId, descripcion = "Cemento gris x 40 bultos", monto = 1_480_000L, fecha = hoy - 30 * dia, categoria = CategoriaGasto.MATERIALES.name))
            gastoDao.insertar(Gasto(obraId = obraId, descripcion = "Hierro y varilla corrugada", monto = 9_200_000L, fecha = hoy - 28 * dia, categoria = CategoriaGasto.MATERIALES.name))
            gastoDao.insertar(Gasto(obraId = obraId, descripcion = "Flete de arena y triturado", monto = 650_000L, fecha = hoy - 20 * dia, categoria = CategoriaGasto.TRANSPORTE.name))
            gastoDao.insertar(Gasto(obraId = obraId, descripcion = "Pago semanal de obreros", monto = 4_800_000L, fecha = hoy - 7 * dia, categoria = CategoriaGasto.MANO_DE_OBRA.name))
            gastoDao.insertar(Gasto(obraId = obraId, descripcion = "Ladrillo y bloque", monto = 5_400_000L, fecha = hoy - 5 * dia, categoria = CategoriaGasto.MATERIALES.name))
            gastoDao.insertar(Gasto(obraId = obraId, descripcion = "Permiso y papelería", monto = 380_000L, fecha = hoy - 3 * dia, categoria = CategoriaGasto.OTROS.name))
        }
    }
}
