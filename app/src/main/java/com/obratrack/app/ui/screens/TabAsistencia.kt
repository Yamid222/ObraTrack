package com.obratrack.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obratrack.app.data.AcumuladoTrabajador
import com.obratrack.app.data.Asistencia
import com.obratrack.app.data.Obra
import com.obratrack.app.data.ResumenFecha
import com.obratrack.app.data.TipoPago
import com.obratrack.app.data.Trabajador
import com.obratrack.app.ui.BotonPrincipal
import com.obratrack.app.ui.EstadoVacio
import com.obratrack.app.ui.Formato
import com.obratrack.app.ui.IconoCircular
import com.obratrack.app.ui.ObraViewModel
import com.obratrack.app.ui.SelectorFecha
import com.obratrack.app.ui.TarjetaBase
import com.obratrack.app.ui.theme.Paleta

private fun leerCantidad(texto: String): Double? = texto.replace(',', '.').toDoubleOrNull()

/** RF-05: registro diario de asistencia y consolidado por trabajador y por fecha. */
@Composable
fun TabAsistencia(vm: ObraViewModel, obra: Obra) {
    var subtab by rememberSaveable { mutableStateOf(0) }
    var fecha by rememberSaveable { mutableStateOf(Formato.hoy()) }
    val flujoTrabajadores = remember(obra.id) { vm.trabajadores(obra.id) }
    val trabajadores by flujoTrabajadores.collectAsStateWithLifecycle(initialValue = emptyList<Trabajador>())

    Column(Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = subtab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            listOf("Registrar", "Por trabajador", "Por fecha").forEachIndexed { i, titulo ->
                Tab(selected = subtab == i, onClick = { subtab = i }, text = { Text(titulo, maxLines = 1) })
            }
        }
        when (subtab) {
            0 -> RegistroAsistencia(vm, obra, trabajadores, fecha) { fecha = it }
            1 -> AsistenciaPorTrabajador(vm, obra)
            else -> AsistenciaPorFecha(vm, obra) { elegida ->
                fecha = elegida
                subtab = 0
            }
        }
    }
}

@Composable
private fun RegistroAsistencia(
    vm: ObraViewModel,
    obra: Obra,
    trabajadores: List<Trabajador>,
    fecha: Long,
    onFecha: (Long) -> Unit
) {
    val context = LocalContext.current
    val flujoDia = remember(obra.id, fecha) { vm.asistenciaDelDia(obra.id, fecha) }
    val delDia by flujoDia.collectAsStateWithLifecycle(initialValue = emptyList<Asistencia>())
    val cantidades = remember { mutableStateMapOf<Long, String>() }
    var intento by remember { mutableStateOf(false) }

    // Al cambiar de fecha (o al llegar los datos guardados) se cargan las marcas de ese día.
    LaunchedEffect(fecha, delDia) {
        cantidades.clear()
        delDia.forEach { cantidades[it.trabajadorId] = Formato.numero(it.cantidad) }
        intento = false
    }

    if (trabajadores.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EstadoVacio(
                icono = Icons.Default.Groups,
                titulo = "Primero registra personal",
                texto = "Ve a la pestaña Personal y agrega a los trabajadores para poder tomar la asistencia."
            )
        }
        return
    }

    val hayInvalidos = trabajadores.any { t ->
        val texto = cantidades[t.id]
        texto != null && (leerCantidad(texto) ?: 0.0) <= 0.0
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { SelectorFecha(etiqueta = "Fecha de asistencia", valor = fecha, onCambio = onFecha) }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${cantidades.size} de ${trabajadores.size} asistieron",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    trabajadores.forEach { t ->
                        if (cantidades[t.id] == null) {
                            cantidades[t.id] = if (TipoPago.desde(t.tipoPago) == TipoPago.JORNAL) "8" else ""
                        }
                    }
                }) { Text("Marcar todos") }
            }
        }
        items(trabajadores, key = { it.id }) { trabajador ->
            val tipo = TipoPago.desde(trabajador.tipoPago)
            val texto = cantidades[trabajador.id]
            TarjetaBase(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = texto != null,
                        onCheckedChange = { marcado ->
                            if (marcado) cantidades[trabajador.id] = if (tipo == TipoPago.JORNAL) "8" else ""
                            else cantidades.remove(trabajador.id)
                        }
                    )
                    Column(Modifier.weight(1f)) {
                        Text(trabajador.nombre, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${trabajador.cargo} · ${tipo.etiqueta}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (texto != null) {
                        OutlinedTextField(
                            value = texto,
                            onValueChange = { nuevo -> cantidades[trabajador.id] = nuevo.filter { it.isDigit() || it == '.' || it == ',' }.take(6) },
                            label = { Text(tipo.unidad.replaceFirstChar { it.uppercase() }) },
                            isError = intento && (leerCantidad(texto) ?: 0.0) <= 0.0,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.width(120.dp).padding(end = 6.dp)
                        )
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(6.dp))
            if (intento && hayInvalidos) {
                Text(
                    "Escribe las horas o unidades de cada trabajador que asistió.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            BotonPrincipal(texto = "Guardar asistencia del día", onClick = {
                intento = true
                if (!hayInvalidos) {
                    val presentes = trabajadores.mapNotNull { t ->
                        val cantidad = cantidades[t.id]?.let { leerCantidad(it) }
                        if (cantidad == null) null
                        else Asistencia(obraId = obra.id, trabajadorId = t.id, fecha = fecha, cantidad = cantidad)
                    }
                    val ausentes = trabajadores.filter { cantidades[it.id] == null }.map { it.id }
                    vm.guardarAsistencia(fecha, presentes, ausentes) {
                        Toast.makeText(context, "Asistencia guardada", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }
}

@Composable
private fun AsistenciaPorTrabajador(vm: ObraViewModel, obra: Obra) {
    val flujo = remember(obra.id) { vm.acumulados(obra.id) }
    val acumulados by flujo.collectAsStateWithLifecycle(initialValue = emptyList<AcumuladoTrabajador>())

    if (acumulados.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EstadoVacio(
                icono = Icons.Default.CheckCircle,
                titulo = "Sin asistencia registrada",
                texto = "Cuando guardes la asistencia de los días, aquí verás el acumulado de cada trabajador."
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(acumulados, key = { it.trabajador.id }) { fila ->
            val tipo = TipoPago.desde(fila.trabajador.tipoPago)
            TarjetaBase(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(fila.trabajador.nombre, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${fila.trabajador.cargo} · ${tipo.etiqueta}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${Formato.numero(fila.total)} ${tipo.unidad}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Paleta.Naranja
                        )
                        Text(
                            "${fila.dias} día(s) asistidos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AsistenciaPorFecha(vm: ObraViewModel, obra: Obra, onElegir: (Long) -> Unit) {
    val flujo = remember(obra.id) { vm.asistenciaPorFecha(obra.id) }
    val fechas by flujo.collectAsStateWithLifecycle(initialValue = emptyList<ResumenFecha>())

    if (fechas.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EstadoVacio(
                icono = Icons.Default.DateRange,
                titulo = "Sin días registrados",
                texto = "Aquí aparecerá cada día con cuántos trabajadores asistieron."
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(fechas, key = { it.fecha }) { fila ->
            TarjetaBase(Modifier.fillMaxWidth(), onClick = { onElegir(fila.fecha) }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconoCircular(Icons.Default.DateRange, Color(0xFF2563EB))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(Formato.fecha(fila.fecha), style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${fila.asistentes} trabajador(es) asistieron",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("Ver", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
