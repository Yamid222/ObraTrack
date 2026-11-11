package com.obratrack.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.obratrack.app.data.Obra
import com.obratrack.app.ui.BarraSuperior
import com.obratrack.app.ui.BotonPrincipal
import com.obratrack.app.ui.Formato
import com.obratrack.app.ui.ObraViewModel
import com.obratrack.app.ui.SelectorFecha

/** RF-01: crear o editar una obra (obraId = 0 significa obra nueva). */
@Composable
fun ObraFormScreen(vm: ObraViewModel, obraId: Long, onVolver: () -> Unit) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var direccion by rememberSaveable { mutableStateOf("") }
    var presupuesto by rememberSaveable { mutableStateOf("") }
    var descripcion by rememberSaveable { mutableStateOf("") }
    var fechaInicio by rememberSaveable { mutableStateOf(Formato.hoy()) }
    var intento by remember { mutableStateOf(false) }

    LaunchedEffect(obraId) {
        if (obraId != 0L) {
            vm.cargarObra(obraId)?.let {
                nombre = it.nombre
                direccion = it.direccion
                presupuesto = it.presupuesto.toString()
                descripcion = it.descripcion
                fechaInicio = it.fechaInicio
            }
        }
    }

    val presupuestoNumero = presupuesto.toLongOrNull()
    val presupuestoValido = presupuestoNumero != null && presupuestoNumero > 0
    val valido = nombre.isNotBlank() && direccion.isNotBlank() && presupuestoValido

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BarraSuperior(
                titulo = if (obraId == 0L) "Nueva obra" else "Editar obra",
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
                label = { Text("Nombre de la obra *") },
                isError = intento && nombre.isBlank(),
                supportingText = { if (intento && nombre.isBlank()) Text("Campo obligatorio") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = direccion,
                onValueChange = { direccion = it },
                label = { Text("Dirección *") },
                isError = intento && direccion.isBlank(),
                supportingText = { if (intento && direccion.isBlank()) Text("Campo obligatorio") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            SelectorFecha(etiqueta = "Fecha de inicio *", valor = fechaInicio, onCambio = { fechaInicio = it })
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = presupuesto,
                onValueChange = { nuevo -> presupuesto = nuevo.filter { it.isDigit() }.take(13) },
                label = { Text("Presupuesto estimado *") },
                prefix = { Text("$ ") },
                isError = intento && !presupuestoValido,
                supportingText = {
                    when {
                        intento && !presupuestoValido -> Text("Ingresa un valor mayor a cero")
                        presupuestoNumero != null -> Text(Formato.pesos(presupuestoNumero))
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción general (opcional)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
            BotonPrincipal(texto = "Guardar obra", onClick = {
                intento = true
                if (valido) {
                    vm.guardarObra(
                        Obra(
                            id = obraId,
                            nombre = nombre.trim(),
                            direccion = direccion.trim(),
                            fechaInicio = fechaInicio,
                            presupuesto = presupuestoNumero ?: 0L,
                            descripcion = descripcion.trim()
                        ),
                        onVolver
                    )
                }
            })
        }
    }
}
