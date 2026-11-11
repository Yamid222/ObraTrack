package com.obratrack.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obratrack.app.data.CategoriaGasto
import com.obratrack.app.data.Categorias
import com.obratrack.app.data.Gasto
import com.obratrack.app.ui.Archivos
import com.obratrack.app.ui.BarraSuperior
import com.obratrack.app.ui.BotonPrincipal
import com.obratrack.app.ui.BotonSecundario
import com.obratrack.app.ui.Formato
import com.obratrack.app.ui.FotoVisor
import com.obratrack.app.ui.Miniatura
import com.obratrack.app.ui.ObraViewModel
import com.obratrack.app.ui.SelectorFecha
import com.obratrack.app.ui.TituloSeccion
import com.obratrack.app.ui.rememberCamara

/** RF-07 y RF-08: registrar o editar un gasto con la foto de su factura (gastoId = 0 es gasto nuevo). */
@Composable
fun GastoFormScreen(
    vm: ObraViewModel,
    obraId: Long,
    gastoId: Long,
    onVolver: () -> Unit
) {
    var descripcion by rememberSaveable { mutableStateOf("") }
    var monto by rememberSaveable { mutableStateOf("") }
    var fecha by rememberSaveable { mutableStateOf(Formato.hoy()) }
    var categoria by rememberSaveable { mutableStateOf(CategoriaGasto.MATERIALES.name) }
    var facturaRuta by rememberSaveable { mutableStateOf<String?>(null) }
    var rutaOriginal by rememberSaveable { mutableStateOf<String?>(null) }
    var intento by remember { mutableStateOf(false) }
    var confirmarEliminar by remember { mutableStateOf(false) }
    var verFactura by remember { mutableStateOf(false) }
    var original by remember { mutableStateOf<Gasto?>(null) }
    var nuevaCategoria by remember { mutableStateOf<String?>(null) } // texto del diálogo; null = cerrado
    var verCategorias by remember { mutableStateOf(false) }
    val propias by vm.categorias.collectAsStateWithLifecycle()
    val opcionesCategoria = Categorias.todas(propias)

    LaunchedEffect(gastoId) {
        if (gastoId != 0L) {
            vm.cargarGasto(gastoId)?.let {
                original = it
                descripcion = it.descripcion
                monto = it.monto.toString()
                fecha = it.fecha
                categoria = it.categoria
                facturaRuta = it.facturaRuta
                rutaOriginal = it.facturaRuta
            }
        }
    }

    val montoNumero = monto.toLongOrNull()
    val montoValido = montoNumero != null && montoNumero > 0

    // Si se toma una foto nueva y ya había una sin guardar, se borra la anterior.
    val tomarFoto = rememberCamara(prefijo = "factura", conUbicacion = false) { captura ->
        if (facturaRuta != null && facturaRuta != rutaOriginal) Archivos.borrar(facturaRuta)
        facturaRuta = captura.ruta
    }

    fun cancelar() {
        if (facturaRuta != null && facturaRuta != rutaOriginal) Archivos.borrar(facturaRuta)
        onVolver()
    }
    BackHandler { cancelar() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BarraSuperior(
                titulo = if (gastoId == 0L) "Nuevo gasto" else "Editar gasto",
                onVolver = { cancelar() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción del gasto *") },
                isError = intento && descripcion.isBlank(),
                supportingText = { if (intento && descripcion.isBlank()) Text("Campo obligatorio") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = monto,
                onValueChange = { nuevo -> monto = nuevo.filter { it.isDigit() }.take(12) },
                label = { Text("Monto *") },
                prefix = { Text("$ ") },
                isError = intento && !montoValido,
                supportingText = {
                    when {
                        intento && !montoValido -> Text("Ingresa un valor mayor a cero")
                        montoNumero != null -> Text(Formato.pesos(montoNumero))
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            SelectorFecha(etiqueta = "Fecha", valor = fecha, onCambio = { fecha = it })

            Row(verticalAlignment = Alignment.CenterVertically) {
                TituloSeccion("Categoría", Modifier.weight(1f))
                if (propias.isNotEmpty()) {
                    IconButton(onClick = { verCategorias = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Renombrar o eliminar categorías")
                    }
                }
                IconButton(onClick = { nuevaCategoria = "" }) {
                    Icon(
                        Icons.Default.AddCircle,
                        contentDescription = "Crear categoría",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            opcionesCategoria.chunked(2).forEach { fila ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    fila.forEach { opcion ->
                        FilterChip(
                            selected = categoria == opcion.clave,
                            onClick = { categoria = opcion.clave },
                            label = { Text(opcion.etiqueta, maxLines = 1) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Si la fila queda con una sola categoría, se completa para que no se estire.
                    if (fila.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
            }

            TituloSeccion("Foto de la factura")
            val ruta = facturaRuta
            if (ruta != null) {
                Miniatura(ruta = ruta, conUbicacion = false, onClick = { verFactura = true }, tamano = 140.dp)
                Spacer(Modifier.height(12.dp))
                BotonSecundario(texto = "Cambiar foto", onClick = tomarFoto, icono = Icons.Default.AddAPhoto)
                Spacer(Modifier.height(8.dp))
                BotonSecundario(
                    texto = "Quitar foto",
                    onClick = {
                        if (ruta != rutaOriginal) Archivos.borrar(ruta)
                        facturaRuta = null
                    },
                    icono = Icons.Default.Delete,
                    peligro = true
                )
            } else {
                Text(
                    "Fotografía el recibo o la factura para dejar la evidencia del gasto.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                BotonSecundario(texto = "Tomar foto de la factura", onClick = tomarFoto, icono = Icons.Default.AddAPhoto)
            }

            Spacer(Modifier.height(28.dp))
            BotonPrincipal(texto = "Guardar gasto", onClick = {
                intento = true
                if (descripcion.isNotBlank() && montoValido) {
                    // Si la factura original fue cambiada o quitada, su archivo ya no se necesita.
                    val sobrante = if (rutaOriginal != null && rutaOriginal != facturaRuta) rutaOriginal else null
                    vm.guardarGasto(
                        gasto = Gasto(
                            id = gastoId,
                            obraId = obraId,
                            descripcion = descripcion.trim(),
                            monto = montoNumero ?: 0L,
                            fecha = fecha,
                            categoria = categoria,
                            facturaRuta = facturaRuta,
                            // Conserva la marca si el gasto vino de "Sumar al presupuesto".
                            deAsistencia = original?.deAsistencia ?: false
                        ),
                        rutaParaBorrar = sobrante,
                        alTerminar = onVolver
                    )
                }
            })
            if (gastoId != 0L) {
                Spacer(Modifier.height(12.dp))
                BotonSecundario(
                    texto = "Eliminar gasto",
                    onClick = { confirmarEliminar = true },
                    icono = Icons.Default.Delete,
                    peligro = true
                )
            }
        }
    }

    if (verFactura && facturaRuta != null) {
        FotoVisor(
            ruta = facturaRuta!!,
            fecha = null,
            latitud = null,
            longitud = null,
            onCerrar = { verFactura = false }
        )
    }

    if (verCategorias) {
        DialogoCategorias(
            vm = vm,
            onCerrar = { verCategorias = false },
            // Si se borra la categoría que estaba elegida en este gasto, se vuelve a Materiales.
            onEliminada = { clave -> if (categoria == clave) categoria = CategoriaGasto.MATERIALES.name }
        )
    }

    nuevaCategoria?.let { texto ->
        AlertDialog(
            onDismissRequest = { nuevaCategoria = null },
            title = { Text("Nueva categoría") },
            text = {
                Column {
                    Text(
                        "Quedará disponible para los gastos de todas las obras.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { nuevaCategoria = it.take(30) },
                        label = { Text("Nombre (ej: Herramientas)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = texto.isNotBlank(),
                    onClick = {
                        vm.crearCategoria(texto) { clave -> categoria = clave }
                        nuevaCategoria = null
                    }
                ) { Text("Crear") }
            },
            dismissButton = {
                TextButton(onClick = { nuevaCategoria = null }) { Text("Cancelar") }
            }
        )
    }

    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            title = { Text("¿Eliminar este gasto?") },
            text = { Text("También se eliminará la foto de la factura. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarEliminar = false
                    if (facturaRuta != null && facturaRuta != rutaOriginal) Archivos.borrar(facturaRuta)
                    original?.let { vm.eliminarGasto(it, onVolver) }
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarEliminar = false }) { Text("Cancelar") }
            }
        )
    }
}
