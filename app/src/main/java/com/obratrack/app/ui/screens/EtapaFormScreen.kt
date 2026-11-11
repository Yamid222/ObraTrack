package com.obratrack.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.obratrack.app.data.EstadoEtapa
import com.obratrack.app.data.Etapa
import com.obratrack.app.data.Foto
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

private val ETAPAS_SUGERIDAS = listOf(
    "Cimentación", "Estructura", "Mampostería", "Cubierta",
    "Instalaciones", "Pañete", "Pisos", "Acabados"
)

// Las fotos nuevas (aún sin guardar) se conservan como texto para sobrevivir a un cambio de pantalla.
private fun codificar(fotos: List<Foto>): String =
    fotos.joinToString("\n") { "${it.ruta};${it.fecha};${it.latitud ?: ""};${it.longitud ?: ""}" }

private fun decodificar(texto: String, obraId: Long): List<Foto> =
    if (texto.isBlank()) emptyList()
    else texto.split("\n").mapNotNull { linea ->
        val partes = linea.split(";")
        if (partes.size < 4) null
        else Foto(
            obraId = obraId,
            etapaId = 0,
            ruta = partes[0],
            fecha = partes[1].toLongOrNull() ?: System.currentTimeMillis(),
            latitud = partes[2].toDoubleOrNull(),
            longitud = partes[3].toDoubleOrNull()
        )
    }

/** RF-02 y RF-03: registrar o editar una etapa, con fotos de evidencia (etapaId = 0 es etapa nueva). */
@Composable
fun EtapaFormScreen(
    vm: ObraViewModel,
    obraId: Long,
    etapaId: Long,
    onVolver: () -> Unit
) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var descripcion by rememberSaveable { mutableStateOf("") }
    var estado by rememberSaveable { mutableStateOf(EstadoEtapa.PENDIENTE.name) }
    var fecha by rememberSaveable { mutableStateOf(Formato.hoy()) }
    var intento by remember { mutableStateOf(false) }
    var confirmarEliminar by remember { mutableStateOf(false) }
    var etapaOriginal by remember { mutableStateOf<Etapa?>(null) }

    val existentes = remember { mutableStateListOf<Foto>() }
    var nuevasTexto by rememberSaveable { mutableStateOf("") }
    var eliminadasIds by rememberSaveable { mutableStateOf("") }
    var fotoVista by remember { mutableStateOf<Foto?>(null) }

    val nuevas = decodificar(nuevasTexto, obraId)
    val idsEliminados = eliminadasIds.split(",").mapNotNull { it.toLongOrNull() }.toSet()
    val visibles = existentes.filter { it.id !in idsEliminados } + nuevas

    LaunchedEffect(etapaId) {
        if (etapaId != 0L) {
            vm.cargarEtapa(etapaId)?.let {
                etapaOriginal = it
                nombre = it.nombre
                descripcion = it.descripcion
                estado = it.estado
                fecha = it.fecha
            }
            existentes.clear()
            existentes.addAll(vm.cargarFotosEtapa(etapaId))
        }
    }

    val tomarFoto = rememberCamara(prefijo = "etapa", conUbicacion = true) { captura ->
        val actuales = decodificar(nuevasTexto, obraId)
        nuevasTexto = codificar(
            actuales + Foto(
                obraId = obraId, etapaId = 0, ruta = captura.ruta, fecha = captura.fecha,
                latitud = captura.latitud, longitud = captura.longitud
            )
        )
    }

    // Al salir sin guardar se borran los archivos de las fotos nuevas.
    fun cancelar() {
        nuevas.forEach { Archivos.borrar(it.ruta) }
        onVolver()
    }
    BackHandler { cancelar() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BarraSuperior(
                titulo = if (etapaId == 0L) "Nueva etapa" else "Editar etapa",
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
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre de la etapa *") },
                isError = intento && nombre.isBlank(),
                supportingText = { if (intento && nombre.isBlank()) Text("Campo obligatorio") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                items(ETAPAS_SUGERIDAS) { sugerida ->
                    AssistChip(onClick = { nombre = sugerida }, label = { Text(sugerida) })
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción del avance") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            TituloSeccion("Estado")
            EstadoEtapa.entries.forEach { opcion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .selectable(
                            selected = estado == opcion.name,
                            onClick = { estado = opcion.name },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = estado == opcion.name, onClick = null)
                    Spacer(Modifier.width(12.dp))
                    Text(opcion.etiqueta, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.height(8.dp))
            SelectorFecha(etiqueta = "Fecha", valor = fecha, onCambio = { fecha = it })

            TituloSeccion("Evidencia fotográfica")
            Text(
                "Cada foto se guarda con la hora y la ubicación GPS del momento en que se toma.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            if (visibles.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    visibles.forEach { foto ->
                        Miniatura(
                            ruta = foto.ruta,
                            conUbicacion = foto.latitud != null,
                            onClick = { fotoVista = foto },
                            tamano = 96.dp
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            BotonSecundario(texto = "Tomar foto", onClick = tomarFoto, icono = Icons.Default.AddAPhoto)

            Spacer(Modifier.height(28.dp))
            BotonPrincipal(texto = "Guardar etapa", onClick = {
                intento = true
                if (nombre.isNotBlank()) {
                    vm.guardarEtapa(
                        etapa = Etapa(
                            id = etapaId,
                            obraId = obraId,
                            nombre = nombre.trim(),
                            descripcion = descripcion.trim(),
                            estado = estado,
                            fecha = fecha
                        ),
                        fotosNuevas = nuevas,
                        fotosEliminadas = existentes.filter { it.id in idsEliminados },
                        alTerminar = onVolver
                    )
                }
            })
            if (etapaId != 0L) {
                Spacer(Modifier.height(12.dp))
                BotonSecundario(
                    texto = "Eliminar etapa",
                    onClick = { confirmarEliminar = true },
                    icono = Icons.Default.Delete,
                    peligro = true
                )
            }
        }
    }

    fotoVista?.let { f ->
        FotoVisor(
            ruta = f.ruta,
            fecha = f.fecha,
            latitud = f.latitud,
            longitud = f.longitud,
            onCerrar = { fotoVista = null },
            onEliminar = {
                if (f.id == 0L) {
                    // Foto nueva: se quita de la lista y se borra el archivo.
                    nuevasTexto = codificar(nuevas.filter { it.ruta != f.ruta })
                    Archivos.borrar(f.ruta)
                } else {
                    eliminadasIds = (idsEliminados + f.id).joinToString(",")
                }
                fotoVista = null
            }
        )
    }

    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            title = { Text("¿Eliminar esta etapa?") },
            text = { Text("También se eliminarán sus fotos. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarEliminar = false
                    nuevas.forEach { Archivos.borrar(it.ruta) }
                    etapaOriginal?.let { vm.eliminarEtapa(it, onVolver) }
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarEliminar = false }) { Text("Cancelar") }
            }
        )
    }
}
