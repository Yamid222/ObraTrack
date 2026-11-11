package com.obratrack.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.obratrack.app.data.AcumuladoTrabajador
import com.obratrack.app.data.AppDatabase
import com.obratrack.app.data.Asistencia
import com.obratrack.app.data.Categoria
import com.obratrack.app.data.CategoriaGasto
import com.obratrack.app.data.Categorias
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
    private val categoriaDao = db.categoriaDao()

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
            if (trabajador.id == 0L) {
                trabajadorDao.insertar(trabajador)
            } else {
                val antes = trabajadorDao.obtenerTrabajador(trabajador.id)
                db.withTransaction {
                    trabajadorDao.actualizar(trabajador)
                    // Si cambiaron las horas del día, los días completos ya registrados pasan a las horas nuevas
                    // (así la asistencia muestra el valor nuevo y un día completo se sigue pagando completo).
                    // Los días con horas parciales se dejan como estaban.
                    if (antes != null &&
                        TipoPago.desde(antes.tipoPago).porHoras &&
                        TipoPago.desde(trabajador.tipoPago).porHoras &&
                        antes.horasDia != trabajador.horasDia
                    ) {
                        asistenciaDao.cambiarHorasDia(trabajador.id, antes.horasDia, trabajador.horasDia)
                    }
                }
            }
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

    /** Corrige la asistencia de un trabajador en un día: cantidad null = no asistió. */
    fun corregirAsistencia(obraId: Long, trabajadorId: Long, fecha: Long, cantidad: Double?, alTerminar: () -> Unit) {
        viewModelScope.launch {
            if (cantidad == null) asistenciaDao.borrar(trabajadorId, fecha)
            else asistenciaDao.guardar(Asistencia(obraId = obraId, trabajadorId = trabajadorId, fecha = fecha, cantidad = cantidad))
            alTerminar()
        }
    }

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

    // ---------- Categorías de gasto (compartidas por todas las obras) ----------
    val categorias: StateFlow<List<Categoria>> = categoriaDao.observar()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Crea una categoría nueva y devuelve su clave para dejarla seleccionada.
     * Si ya existe una con el mismo nombre (fija o propia), devuelve esa en vez de repetirla.
     */
    fun crearCategoria(nombre: String, alTerminar: (String) -> Unit) {
        val limpio = nombre.trim().replace(Regex("\\s+"), " ")
        if (limpio.isEmpty()) return
        viewModelScope.launch {
            val propias = categoriaDao.obtenerTodas()
            val existente = Categorias.todas(propias).firstOrNull { it.etiqueta.equals(limpio, ignoreCase = true) }
            if (existente != null) {
                alTerminar(existente.clave)
            } else {
                val id = categoriaDao.insertar(Categoria(nombre = limpio, argb = Categorias.colorPara(propias.size)))
                alTerminar("C$id")
            }
        }
    }

    /** Devuelve los nombres de las obras donde se usa la categoría (vacío = no está en uso). */
    fun consultarUso(categoria: Categoria, resultado: (List<String>) -> Unit) {
        viewModelScope.launch { resultado(gastoDao.obrasQueUsan(Categorias.clave(categoria))) }
    }

    /** Cambia el nombre; devuelve un mensaje de error o null si todo salió bien. */
    fun renombrarCategoria(categoria: Categoria, nombre: String, resultado: (String?) -> Unit) {
        val limpio = nombre.trim().replace(Regex("\\s+"), " ")
        viewModelScope.launch {
            val repetida = Categorias.todas(categoriaDao.obtenerTodas())
                .any { it.clave != Categorias.clave(categoria) && it.etiqueta.equals(limpio, ignoreCase = true) }
            when {
                limpio.isEmpty() -> resultado("Escribe un nombre.")
                repetida -> resultado("Ya existe una categoría llamada \"$limpio\".")
                else -> {
                    // Los gastos guardan la clave (C + id), así que todos muestran el nombre nuevo.
                    categoriaDao.actualizar(categoria.copy(nombre = limpio))
                    resultado(null)
                }
            }
        }
    }

    /** Elimina la categoría solo si ningún gasto la usa; si está en uso devuelve el mensaje para el usuario. */
    fun eliminarCategoria(categoria: Categoria, resultado: (String?) -> Unit) {
        viewModelScope.launch {
            val obras = gastoDao.obrasQueUsan(Categorias.clave(categoria))
            if (obras.isNotEmpty()) {
                resultado(mensajeEnUso(categoria.nombre, obras))
            } else {
                categoriaDao.eliminar(categoria)
                resultado(null)
            }
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

    /** Quita los gastos creados con "Sumar al presupuesto", para que se puedan volver a sumar. */
    fun restarManoDeObra(obraId: Long, alTerminar: () -> Unit) {
        viewModelScope.launch {
            gastoDao.eliminarDeAsistencia(obraId)
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

/** Mensaje que se muestra cuando no se puede eliminar una categoría porque tiene gastos. */
fun mensajeEnUso(nombre: String, obras: List<String>): String =
    "No se puede eliminar \"$nombre\" porque está en uso en: ${obras.joinToString(", ")}. " +
        "Primero cambia la categoría de esos gastos."
