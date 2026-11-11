package com.obratrack.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obratrack.app.ui.BarraSuperior
import com.obratrack.app.ui.ObraViewModel
import com.obratrack.app.ui.Rutas

private data class SeccionObra(val titulo: String, val icono: ImageVector)

private val secciones = listOf(
    SeccionObra("Avance", Icons.Default.Construction),
    SeccionObra("Personal", Icons.Default.Groups),
    SeccionObra("Asistencia", Icons.Default.CheckCircle),
    SeccionObra("Gastos", Icons.Default.Receipt),
    SeccionObra("Balance", Icons.Default.Assessment)
)

/** Pantalla principal de una obra, con sus cinco secciones en la barra inferior. */
@Composable
fun ObraScreen(
    vm: ObraViewModel,
    obraId: Long,
    onVolver: () -> Unit,
    navegar: (String) -> Unit
) {
    val flujoObra = remember(obraId) { vm.obra(obraId) }
    val obra by flujoObra.collectAsStateWithLifecycle(initialValue = null)
    var seccion by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BarraSuperior(
                titulo = obra?.nombre ?: "Obra",
                subtitulo = obra?.direccion,
                onVolver = onVolver,
                acciones = {
                    IconButton(onClick = { navegar(Rutas.obraForm(obraId)) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar obra")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                secciones.forEachIndexed { indice, s ->
                    NavigationBarItem(
                        selected = seccion == indice,
                        onClick = { seccion = indice },
                        icon = { Icon(s.icono, contentDescription = null) },
                        label = { Text(s.titulo, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            when (seccion) {
                0 -> BotonFlotante("Registrar etapa") { navegar(Rutas.etapaForm(obraId, 0)) }
                1 -> BotonFlotante("Agregar trabajador") { navegar(Rutas.trabajadorForm(obraId, 0)) }
                3 -> BotonFlotante("Registrar gasto") { navegar(Rutas.gastoForm(obraId, 0)) }
                else -> {}
            }
        }
    ) { padding ->
        val actual = obra
        if (actual == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (seccion) {
                    0 -> TabAvance(vm, actual, navegar)
                    1 -> TabPersonal(vm, actual, navegar)
                    2 -> TabAsistencia(vm, actual)
                    3 -> TabGastos(vm, actual, navegar)
                    else -> TabBalance(vm, actual)
                }
            }
        }
    }
}

@Composable
private fun BotonFlotante(texto: String, onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text(texto) },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )
}
