package com.obratrack.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.roundToLong

/** Cálculo del costo de mano de obra a partir de la asistencia. */
object ManoDeObra {

    /** Lo que se le debe a un trabajador por un día de asistencia (horas o unidades). */
    fun costoDia(t: Trabajador, cantidad: Double): Long {
        val horasDia = if (t.horasDia > 0) t.horasDia else 8.0
        val dias = if (t.diasSemana > 0) t.diasSemana else 6
        val valor = when (TipoPago.desde(t.tipoPago)) {
            TipoPago.JORNAL -> t.valor * (cantidad / horasDia)
            TipoPago.SEMANA -> t.valor.toDouble() / dias * (cantidad / horasDia)
            TipoPago.DESTAJO -> t.valor * cantidad
        }
        return valor.roundToLong()
    }

    /** Costo total de una lista de asistencias. */
    fun costo(asistencias: List<Asistencia>, trabajadores: List<Trabajador>): Long {
        val porId = trabajadores.associateBy { it.id }
        return asistencias.sumOf { a -> porId[a.trabajadorId]?.let { costoDia(it, a.cantidad) } ?: 0L }
    }

    /** Lunes de la semana que contiene [fecha] (medianoche UTC, igual que las fechas guardadas). */
    fun inicioSemana(fecha: Long): Long {
        val dia = java.time.Instant.ofEpochMilli(fecha).atZone(ZoneOffset.UTC).toLocalDate()
        return dia.with(DayOfWeek.MONDAY).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    fun inicioSemanaActual(): Long =
        inicioSemana(LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())

    const val DIA = 86_400_000L
}
