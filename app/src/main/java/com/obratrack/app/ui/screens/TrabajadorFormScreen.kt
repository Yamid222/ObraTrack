package com.obratrack.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.obratrack.app.data.TipoPago
import com.obratrack.app.data.Trabajador
import com.obratrack.app.ui.BarraSuperior
import com.obratrack.app.ui.BotonPrincipal
import com.obratrack.app.ui.BotonSecundario
import com.obratrack.app.ui.Formato
import com.obratrack.app.ui.ObraViewModel
import com.obratrack.app.ui.TituloSeccion

/** RF-04: registrar o editar un trabajador (trabajadorId = 0 es trabajador nuevo). */
@Composable
fun TrabajadorFormScreen(
    vm: ObraViewModel,
    obraId: Long,
    trabajadorId: Long,
    onVolver: () -> Unit
) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var cargo by rememberSaveable { mutableStateOf("") }
    var tipo by rememberSaveable { mutableStateOf(TipoPago.JORNAL.name) }
    var valor by rememberSaveable { mutableStateOf("") }
    var intento by remember { mutableStateOf(false) }
    var confirmarEliminar by remember { mutableStateOf(false) }
    var original by remember { mutableStateOf<Trabajador?>(null) }

    LaunchedEffect(trabajadorId) {
        if (trabajadorId != 0L) {
            vm.cargarTrabajador(trabajadorId)?.let {
                original = it
                nombre = it.nombre
                cargo = it.cargo
                tipo = it.tipoPago
                valor = it.valor.toString()
            }
        }
    }

    val valorNumero = valor.toLongOrNull()
    val valorValido = valorNumero != null && valorNumero > 0
    val tipoActual = TipoPago.desde(tipo)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BarraSuperior(
                titulo = if (trabajadorId == 0L) "Nuevo trabajador" else "Editar trabajador",
                onVolver = onVolver
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
                label = { Text("Nombre del trabajador *") },
                isError = intento && nombre.isBlank(),
                supportingText = { if (intento && nombre.isBlank()) Text("Campo obligatorio") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = cargo,
                onValueChange = { cargo = it },
                label = { Text("Cargo * (oficial, ayudante, maestro…)") },
                isError = intento && cargo.isBlank(),
                supportingText = { if (intento && cargo.isBlank()) Text("Campo obligatorio") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            TituloSeccion("Tipo de pago")
            TipoPago.entries.forEach { opcion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = tipo == opcion.name,
                            onClick = { tipo = opcion.name },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = tipo == opcion.name, onClick = null)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(opcion.etiqueta, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (opcion == TipoPago.JORNAL) "Se paga por día trabajado" else "Se paga por cada unidad de trabajo hecha",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = valor,
                onValueChange = { nuevo -> valor = nuevo.filter { it.isDigit() }.take(10) },
                label = { Text(if (tipoActual == TipoPago.JORNAL) "Valor del jornal *" else "Valor por unidad *") },
                prefix = { Text("$ ") },
                isError = intento && !valorValido,
                supportingText = {
                    when {
                        intento && !valorValido -> Text("Ingresa un valor mayor a cero")
                        valorNumero != null -> Text(Formato.pesos(valorNumero))
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
            BotonPrincipal(texto = "Guardar trabajador", onClick = {
                intento = true
                if (nombre.isNotBlank() && cargo.isNotBlank() && valorValido) {
                    vm.guardarTrabajador(
                        Trabajador(
                            id = trabajadorId,
                            obraId = obraId,
                            nombre = nombre.trim(),
                            cargo = cargo.trim(),
                            tipoPago = tipo,
                            valor = valorNumero ?: 0L
                        ),
                        onVolver
                    )
                }
            })
            if (trabajadorId != 0L) {
                Spacer(Modifier.height(12.dp))
                BotonSecundario(
                    texto = "Eliminar trabajador",
                    onClick = { confirmarEliminar = true },
                    icono = Icons.Default.Delete,
                    peligro = true
                )
            }
        }
    }

    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            title = { Text("¿Eliminar este trabajador?") },
            text = { Text("También se eliminará su historial de asistencia. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarEliminar = false
                    original?.let { vm.eliminarTrabajador(it, onVolver) }
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarEliminar = false }) { Text("Cancelar") }
            }
        )
    }
}
