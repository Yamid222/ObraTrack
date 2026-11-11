package com.obratrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obratrack.app.data.Categoria
import com.obratrack.app.data.Categorias
import com.obratrack.app.ui.ObraViewModel
import com.obratrack.app.ui.mensajeEnUso
import com.obratrack.app.ui.theme.Paleta

/**
 * Lista de las categorías creadas por el usuario, para renombrarlas o eliminarlas.
 * - Renombrar: si está en uso se avisa en qué obras, pero se deja cambiar.
 * - Eliminar: solo si no está en uso; si lo está se muestra un mensaje y no se borra.
 */
@Composable
fun DialogoCategorias(
    vm: ObraViewModel,
    onCerrar: () -> Unit,
    onEliminada: (clave: String) -> Unit
) {
    val propias by vm.categorias.collectAsStateWithLifecycle()

    var renombrando by remember { mutableStateOf<Categoria?>(null) }
    var obrasEnUso by remember { mutableStateOf<List<String>>(emptyList()) }
    var texto by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var porEliminar by remember { mutableStateOf<Categoria?>(null) }
    var aviso by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Mis categorías") },
        text = {
            if (propias.isEmpty()) {
                Text("Todavía no has creado categorías. Usa el botón + junto a \"Categoría\".")
            } else {
                Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                    propias.forEach { cat ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(12.dp).background(Color(cat.argb), CircleShape))
                            Spacer(Modifier.width(10.dp))
                            Text(cat.nombre, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                vm.consultarUso(cat) { obras ->
                                    obrasEnUso = obras
                                    texto = cat.nombre
                                    error = null
                                    renombrando = cat
                                }
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Cambiar nombre de ${cat.nombre}")
                            }
                            IconButton(onClick = {
                                vm.consultarUso(cat) { obras ->
                                    if (obras.isEmpty()) porEliminar = cat
                                    else aviso = mensajeEnUso(cat.nombre, obras)
                                }
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar ${cat.nombre}",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } }
    )

    // ---------- Cambiar nombre ----------
    renombrando?.let { cat ->
        AlertDialog(
            onDismissRequest = { renombrando = null },
            title = { Text("Cambiar nombre") },
            text = {
                Column {
                    if (obrasEnUso.isNotEmpty()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Paleta.Ambar.copy(alpha = 0.12f))
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Paleta.Ambar)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Esta categoría está en uso en: ${obrasEnUso.joinToString(", ")}. " +
                                    "Si cambias el nombre, esos gastos se verán con el nombre nuevo.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Paleta.Ambar
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { texto = it.take(30); error = null },
                        label = { Text("Nombre") },
                        isError = error != null,
                        supportingText = { error?.let { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = texto.isNotBlank(),
                    onClick = {
                        vm.renombrarCategoria(cat, texto) { problema ->
                            if (problema == null) renombrando = null else error = problema
                        }
                    }
                ) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { renombrando = null }) { Text("Cancelar") } }
        )
    }

    // ---------- Confirmar eliminación (solo llega aquí si no está en uso) ----------
    porEliminar?.let { cat ->
        AlertDialog(
            onDismissRequest = { porEliminar = null },
            title = { Text("¿Eliminar \"${cat.nombre}\"?") },
            text = { Text("No está en uso en ninguna obra. Ya no aparecerá al registrar gastos.") },
            confirmButton = {
                TextButton(onClick = {
                    porEliminar = null
                    vm.eliminarCategoria(cat) { problema ->
                        if (problema == null) onEliminada(Categorias.clave(cat)) else aviso = problema
                    }
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { porEliminar = null }) { Text("Cancelar") } }
        )
    }

    // ---------- Mensaje: no se puede eliminar porque está en uso ----------
    aviso?.let { mensaje ->
        AlertDialog(
            onDismissRequest = { aviso = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Paleta.Ambar) },
            title = { Text("Categoría en uso") },
            text = { Text(mensaje) },
            confirmButton = { TextButton(onClick = { aviso = null }) { Text("Entendido") } }
        )
    }
}
