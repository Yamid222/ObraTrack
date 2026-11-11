package com.obratrack.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obratrack.app.data.AcumuladoTrabajador
import com.obratrack.app.data.ManoDeObra
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
import com.obratrack.app.ui.TituloSeccion
import com.obratrack.app.ui.theme.Paleta

private fun leerCantidad(texto: String): Double? = texto.replace(',', '.').toDoubleOrNull()

/** Valor que se propone al marcar asistencia: las horas del día del trabajador, o vacío si es destajo. */
private fun cantidadPorDefecto(t: Trabajador): String =
    if (TipoPago.desde(t.tipoPago).porHoras) Formato.numero(t.horasDia) else ""

/** RF-05: registro diario de asistencia y consolidado por trabajador y por fecha. */
@Composable
fun TabAsistencia(vm: ObraViewModel, obra: Obra) {
    var subtab by rememberSaveable { mutableStateOf(0) }
    var fecha by rememberSaveable { mutableStateOf(Formato.hoy()) }
    val flujoTrabajadores = remember(obra.id) { vm.trabajadores(obra.id) }
    val trabajadores by flujoTrabajadores.collectAsStateWithLifecycle(initialValue = emptyList<Trabajador>())

    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = subtab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 8.dp
        ) {
            listOf("Registrar", "Por trabajador", "Por fecha", "Pagos").forEachIndexed { i, titulo ->
                Tab(selected = subtab == i, onClick = { subtab = i }, text = { Text(titulo, maxLines = 1) })
            }
        }
        when (subtab) {
            0 -> RegistroAsistencia(vm, obra, trabajadores, fecha) { fecha = it }
            1 -> AsistenciaPorTrabajador(vm, obra)
            2 -> AsistenciaPorFecha(vm, obra) { elegida ->
                fecha = elegida
                subtab = 0
            }
            else -> PagosSemana(vm, obra, trabajadores)
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

    // Un día que ya tiene asistencia guardada no se vuelve a registrar; se corrige desde Pagos.
    val yaRegistrado = delDia.isNotEmpty()

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
        if (yaRegistrado) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Paleta.Ambar.copy(alpha = 0.12f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Paleta.Ambar)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "La asistencia del ${Formato.fecha(fecha)} ya está registrada. " +
                            "Para corregirla, ve a Pagos y toca el nombre del trabajador.",
                        color = Paleta.Ambar,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${cantidades.size} de ${trabajadores.size} asistieron",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (!yaRegistrado) {
                    TextButton(onClick = {
                        trabajadores.forEach { t ->
                            if (cantidades[t.id] == null) {
                                cantidades[t.id] = cantidadPorDefecto(t)
                            }
                        }
                    }) { Text("Marcar todos") }
                }
            }
        }
        items(trabajadores, key = { it.id }) { trabajador ->
            val tipo = TipoPago.desde(trabajador.tipoPago)
            val texto = cantidades[trabajador.id]
            TarjetaBase(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = texto != null,
                        enabled = !yaRegistrado,
                        onCheckedChange = { marcado ->
                            if (marcado) cantidades[trabajador.id] = cantidadPorDefecto(trabajador)
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
                            enabled = !yaRegistrado,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.width(120.dp).padding(end = 6.dp)
                        )
                    }
                }
            }
        }
        if (!yaRegistrado) item {
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

/** Resumen de lo que se debe pagar en la semana actual y el total histórico de mano de obra. */
@Composable
private fun PagosSemana(vm: ObraViewModel, obra: Obra, trabajadores: List<Trabajador>) {
    val flujo = remember(obra.id) { vm.asistencias(obra.id) }
    val asistencias by flujo.collectAsStateWithLifecycle(initialValue = emptyList<Asistencia>())
    var verTotal by rememberSaveable { mutableStateOf(false) }
    var trabajadorSemana by remember { mutableStateOf<Trabajador?>(null) }

    if (trabajadores.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EstadoVacio(
                icono = Icons.Default.Payments,
                titulo = "Primero registra personal",
                texto = "Cuando tengas trabajadores y tomes la asistencia, aquí verás cuánto se debe pagar cada semana."
            )
        }
        return
    }

    val inicio = ManoDeObra.inicioSemanaActual()
    val fin = inicio + 6 * ManoDeObra.DIA
    val deLaSemana = asistencias.filter { it.fecha in inicio..fin }
    val totalSemana = ManoDeObra.costo(deLaSemana, trabajadores)
    val totalObra = ManoDeObra.costo(asistencias, trabajadores)
    // Total de cada semana, de la más reciente a la más antigua.
    val porSemana = asistencias
        .groupBy { ManoDeObra.inicioSemana(it.fecha) }
        .map { (lunes, lista) -> lunes to ManoDeObra.costo(lista, trabajadores) }
        .sortedByDescending { it.first }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            TarjetaBase(Modifier.fillMaxWidth(), onClick = { verTotal = !verTotal }) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconoCircular(Icons.Default.Payments, Paleta.Verde)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Semana actual", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${Formato.fecha(inicio)} al ${Formato.fecha(fin)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            if (verTotal) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (verTotal) "Ocultar total" else "Ver total de mano de obra"
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        Formato.pesos(totalSemana),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Paleta.Verde
                    )
                    Text(
                        "Mano de obra de esta semana",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (verTotal) {
                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Total mano de obra de la obra",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(Formato.pesos(totalObra), style = MaterialTheme.typography.titleMedium, color = Paleta.Naranja)
                        }
                        porSemana.forEach { (lunes, monto) ->
                            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                                Text(
                                    "Semana del ${Formato.fecha(lunes)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(Formato.pesos(monto), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    } else {
                        Text(
                            "Toca para ver el total de mano de obra",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
        item { TituloSeccion("Trabajadores esta semana") }
        items(trabajadores, key = { it.id }) { t ->
            val tipo = TipoPago.desde(t.tipoPago)
            val suyas = deLaSemana.filter { it.trabajadorId == t.id }
            val pago = suyas.sumOf { ManoDeObra.costoDia(t, it.cantidad) }
            TarjetaBase(Modifier.fillMaxWidth(), onClick = { trabajadorSemana = t }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t.nombre, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${t.cargo} · ${tipo.etiqueta}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${suyas.size} de ${t.diasSemana} días",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (suyas.size >= t.diasSemana) Paleta.Verde else MaterialTheme.colorScheme.onSurface
                        )
                        Text(Formato.pesos(pago), style = MaterialTheme.typography.bodyMedium, color = Paleta.Naranja)
                    }
                }
            }
        }
    }

    trabajadorSemana?.let { t ->
        DialogoSemana(
            vm = vm,
            obra = obra,
            trabajador = t,
            asistencias = asistencias.filter { it.trabajadorId == t.id },
            onCerrar = { trabajadorSemana = null }
        )
    }
}

private val nombresDias = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

/** Semana de un trabajador: cada día con su fecha, en verde los que asistió; tocar un día permite corregirlo. */
@Composable
private fun DialogoSemana(
    vm: ObraViewModel,
    obra: Obra,
    trabajador: Trabajador,
    asistencias: List<Asistencia>,
    onCerrar: () -> Unit
) {
    val tipo = TipoPago.desde(trabajador.tipoPago)
    var inicio by remember { mutableStateOf(ManoDeObra.inicioSemanaActual()) }
    var editando by remember { mutableStateOf<Long?>(null) } // fecha del día que se corrige
    val porFecha = asistencias.associateBy { it.fecha }
    val fechas = (0..6).map { inicio + it * ManoDeObra.DIA }
    val asistidos = fechas.mapNotNull { porFecha[it] }
    val pago = asistidos.sumOf { ManoDeObra.costoDia(trabajador, it.cantidad) }
    val hoy = Formato.hoy()

    AlertDialog(
        onDismissRequest = onCerrar,
        title = {
            Column {
                Text(trabajador.nombre)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { inicio -= 7 * ManoDeObra.DIA }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Semana anterior")
                    }
                    Text(
                        "${Formato.fecha(fechas.first())} al ${Formato.fecha(fechas.last())}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    IconButton(onClick = { inicio += 7 * ManoDeObra.DIA }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Semana siguiente")
                    }
                }
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                fechas.forEachIndexed { i, dia ->
                    val registro = porFecha[dia]
                    val fondo = if (registro != null) Paleta.Verde.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(fondo)
                            .clickable { editando = dia }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                nombresDias[i] + if (dia == hoy) " (hoy)" else "",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (registro != null) Paleta.Verde else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                Formato.fecha(dia),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (registro != null) {
                            Text(
                                "${Formato.numero(registro.cantidad)} ${tipo.unidad}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Paleta.Verde
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Default.CheckCircle, contentDescription = "Asistió", tint = Paleta.Verde)
                        } else {
                            Text(
                                "No asistió",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "${asistidos.size} de ${trabajador.diasSemana} días · ${Formato.pesos(pago)}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Toca un día para corregir la asistencia.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } }
    )

    editando?.let { dia ->
        DialogoCorregirDia(
            trabajador = trabajador,
            fecha = dia,
            actual = porFecha[dia]?.cantidad,
            onCancelar = { editando = null },
            onGuardar = { cantidad ->
                vm.corregirAsistencia(obra.id, trabajador.id, dia, cantidad) { editando = null }
            }
        )
    }
}

/** Corrige un día: marcar o quitar la asistencia y cambiar las horas o unidades. */
@Composable
private fun DialogoCorregirDia(
    trabajador: Trabajador,
    fecha: Long,
    actual: Double?,
    onCancelar: () -> Unit,
    onGuardar: (Double?) -> Unit
) {
    val tipo = TipoPago.desde(trabajador.tipoPago)
    var asistio by remember { mutableStateOf(actual != null) }
    var texto by remember { mutableStateOf(actual?.let { Formato.numero(it) } ?: cantidadPorDefecto(trabajador)) }
    val cantidad = leerCantidad(texto)
    val valido = !asistio || (cantidad != null && cantidad > 0.0)

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Corregir ${Formato.fecha(fecha)}") },
        text = {
            Column {
                Row(
                    Modifier.fillMaxWidth().clickable { asistio = !asistio },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = asistio, onCheckedChange = { asistio = it })
                    Text("${trabajador.nombre} asistió", style = MaterialTheme.typography.bodyLarge)
                }
                if (asistio) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { nuevo -> texto = nuevo.filter { it.isDigit() || it == '.' || it == ',' }.take(6) },
                        label = { Text(tipo.unidad.replaceFirstChar { it.uppercase() }) },
                        isError = !valido,
                        supportingText = { if (!valido) Text("Escribe un valor mayor a cero") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valido, onClick = { onGuardar(if (asistio) cantidad else null) }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}
