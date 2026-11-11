package com.obratrack.app.ui

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Formato {
    private val colombia = Locale("es", "CO")

    /** Fecha de un día (guardada como medianoche UTC). */
    fun fecha(millis: Long): String {
        val f = SimpleDateFormat("dd/MM/yyyy", colombia)
        f.timeZone = TimeZone.getTimeZone("UTC")
        return f.format(Date(millis))
    }

    /** Fecha y hora exacta de una captura, en la hora del celular. */
    fun fechaHora(millis: Long): String {
        val f = SimpleDateFormat("dd/MM/yyyy hh:mm a", colombia)
        return f.format(Date(millis))
    }

    fun pesos(valor: Long): String {
        val f = NumberFormat.getCurrencyInstance(colombia)
        f.maximumFractionDigits = 0
        return f.format(valor)
    }

    /** 8.0 -> "8"; 7.5 -> "7,5". */
    fun numero(valor: Double): String =
        if (valor % 1.0 == 0.0) valor.toLong().toString()
        else String.format(colombia, "%.1f", valor)

    fun coordenadas(latitud: Double, longitud: Double): String =
        String.format(Locale.US, "%.5f, %.5f", latitud, longitud)

    fun porcentaje(parte: Long, total: Long): Int =
        if (total <= 0L) 0 else ((parte * 100.0) / total).toInt()

    /** Fecha de hoy a medianoche UTC (el formato que usa el selector de fecha). */
    fun hoy(): Long =
        LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
