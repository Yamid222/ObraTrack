package com.obratrack.app.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** RF-01: Obra / proyecto que gestiona el maestro. */
@Entity(tableName = "obras")
data class Obra(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val direccion: String,
    val fechaInicio: Long,   // milisegundos (fecha en UTC)
    val presupuesto: Long,   // pesos colombianos
    val descripcion: String = ""
)

/** RF-02: Etapa de avance de una obra. */
@Entity(
    tableName = "etapas",
    foreignKeys = [
        ForeignKey(entity = Obra::class, parentColumns = ["id"], childColumns = ["obraId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("obraId")]
)
data class Etapa(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val obraId: Long,
    val nombre: String,
    val descripcion: String = "",
    val estado: String = EstadoEtapa.PENDIENTE.name,
    val fecha: Long
)

enum class EstadoEtapa(val etiqueta: String, val argb: Long) {
    PENDIENTE("Pendiente", 0xFF94A3B8),
    EN_PROCESO("En proceso", 0xFF2563EB),
    COMPLETADO("Completado", 0xFF16A34A);

    companion object {
        fun desde(nombre: String): EstadoEtapa =
            entries.firstOrNull { it.name == nombre } ?: PENDIENTE
    }
}

/** RF-03: Foto de evidencia con hora y ubicación GPS, asociada a una etapa. */
@Entity(
    tableName = "fotos",
    foreignKeys = [
        ForeignKey(entity = Obra::class, parentColumns = ["id"], childColumns = ["obraId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Etapa::class, parentColumns = ["id"], childColumns = ["etapaId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("obraId"), Index("etapaId")]
)
data class Foto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val obraId: Long,
    val etapaId: Long,
    val ruta: String,          // archivo guardado en el dispositivo
    val fecha: Long,           // fecha y hora exacta de la captura
    val latitud: Double? = null,
    val longitud: Double? = null
)

/** RF-04: Trabajador de la obra. */
@Entity(
    tableName = "trabajadores",
    foreignKeys = [
        ForeignKey(entity = Obra::class, parentColumns = ["id"], childColumns = ["obraId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("obraId")]
)
data class Trabajador(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val obraId: Long,
    val nombre: String,
    val cargo: String,
    val tipoPago: String = TipoPago.JORNAL.name,
    val valor: Long,
    /** Horas de un día normal de trabajo (jornal y semana); es el valor por defecto en la asistencia. */
    @ColumnInfo(defaultValue = "8") val horasDia: Double = 8.0,
    /** Días que tiene la semana de trabajo acordada (5 o 6). */
    @ColumnInfo(defaultValue = "6") val diasSemana: Int = 6
)

enum class TipoPago(val etiqueta: String, val unidad: String, val valorPor: String) {
    JORNAL("Jornal", "horas", "por jornal"),
    SEMANA("Por semana", "horas", "por semana"),
    DESTAJO("Destajo", "unidades", "por unidad");

    /** Jornal y semana se registran en horas; destajo en unidades. */
    val porHoras: Boolean get() = this != DESTAJO

    companion object {
        fun desde(nombre: String): TipoPago =
            entries.firstOrNull { it.name == nombre } ?: JORNAL
    }
}

/** RF-05: Asistencia diaria de un trabajador (horas o unidades de destajo). */
@Entity(
    tableName = "asistencias",
    foreignKeys = [
        ForeignKey(entity = Obra::class, parentColumns = ["id"], childColumns = ["obraId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Trabajador::class, parentColumns = ["id"], childColumns = ["trabajadorId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("obraId"), Index(value = ["trabajadorId", "fecha"], unique = true)]
)
data class Asistencia(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val obraId: Long,
    val trabajadorId: Long,
    val fecha: Long,
    val cantidad: Double
)

/** RF-07 y RF-08: Gasto de la obra con foto opcional de la factura. */
@Entity(
    tableName = "gastos",
    foreignKeys = [
        ForeignKey(entity = Obra::class, parentColumns = ["id"], childColumns = ["obraId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("obraId")]
)
data class Gasto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val obraId: Long,
    val descripcion: String,
    val monto: Long,
    val fecha: Long,
    val categoria: String = CategoriaGasto.MATERIALES.name,
    val facturaRuta: String? = null,
    /** true si el gasto se creó con "Sumar al presupuesto" a partir de la asistencia. */
    @ColumnInfo(defaultValue = "0") val deAsistencia: Boolean = false
)

enum class CategoriaGasto(val etiqueta: String, val argb: Long) {
    MATERIALES("Materiales", 0xFFEA580C),
    TRANSPORTE("Transporte", 0xFF2563EB),
    MANO_DE_OBRA("Mano de obra", 0xFF16A34A),
    OTROS("Otros", 0xFF7C3AED);

    companion object {
        fun desde(nombre: String): CategoriaGasto =
            entries.firstOrNull { it.name == nombre } ?: OTROS
    }
}

/** Obra con su avance y lo gastado, para mostrar en el listado. */
data class ObraResumen(
    @Embedded val obra: Obra,
    val totalEtapas: Int,
    val completadas: Int,
    val gastado: Long
)

/** Acumulado de asistencia por trabajador. */
data class AcumuladoTrabajador(
    @Embedded val trabajador: Trabajador,
    val dias: Int,
    val total: Double
)

/** Asistencia consolidada por fecha. */
data class ResumenFecha(
    val fecha: Long,
    val asistentes: Int
)
