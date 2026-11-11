package com.obratrack.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obratrack.app.data.ObraResumen
import com.obratrack.app.ui.BarraSuperior
import com.obratrack.app.ui.EstadoVacio
import com.obratrack.app.ui.Formato
import com.obratrack.app.ui.IconoCircular
import com.obratrack.app.ui.ObraViewModel
import com.obratrack.app.ui.TarjetaBase
import com.obratrack.app.ui.theme.Paleta

@Composable
fun ObrasScreen(
    vm: ObraViewModel,
    onNueva: () -> Unit,
    onAbrir: (Long) -> Unit
) {
    val obras by vm.resumenes.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BarraSuperior(
                titulo = "ObraTrack",
                subtitulo = if (obras.isEmpty()) "Control de tus obras" else "${obras.size} obra(s) en seguimiento"
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNueva,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nueva obra") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        if (obras.isEmpty()) {
            Column(Modifier.padding(padding).fillMaxSize(), verticalArrangement = Arrangement.Center) {
                EstadoVacio(
                    icono = Icons.Default.Construction,
                    titulo = "Aún no tienes obras",
                    texto = "Toca \"Nueva obra\" para registrar la primera y empezar a llevar su avance, personal y gastos."
                ) {
                    TextButton(onClick = { vm.cargarEjemplo() }) { Text("Cargar una obra de ejemplo") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(obras, key = { it.obra.id }) { resumen ->
                    TarjetaObra(resumen) { onAbrir(resumen.obra.id) }
                }
            }
        }
    }
}

@Composable
private fun TarjetaObra(resumen: ObraResumen, onClick: () -> Unit) {
    val obra = resumen.obra
    val avance = if (resumen.totalEtapas == 0) 0f else resumen.completadas.toFloat() / resumen.totalEtapas
    val porcentajeGasto = Formato.porcentaje(resumen.gastado, obra.presupuesto)
    val colorGasto = when {
        porcentajeGasto >= 100 -> Paleta.Rojo
        porcentajeGasto >= 80 -> Paleta.Ambar
        else -> Paleta.Verde
    }

    TarjetaBase(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoCircular(Icons.Default.Construction, tamano = 48)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(obra.nombre, style = MaterialTheme.typography.titleLarge, maxLines = 2)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            obra.direccion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            IndicadorProgreso(
                titulo = "Avance de la obra",
                valor = if (resumen.totalEtapas == 0) "Sin etapas" else "${resumen.completadas} de ${resumen.totalEtapas} etapas",
                progreso = avance,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            IndicadorProgreso(
                titulo = "Presupuesto gastado",
                valor = "${Formato.pesos(resumen.gastado)} de ${Formato.pesos(obra.presupuesto)}",
                progreso = (porcentajeGasto / 100f).coerceIn(0f, 1f),
                color = colorGasto
            )
        }
    }
}

@Composable
private fun IndicadorProgreso(
    titulo: String,
    valor: String,
    progreso: Float,
    color: androidx.compose.ui.graphics.Color
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(titulo, style = MaterialTheme.typography.labelLarge)
            Text(valor, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progreso },
            modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(50)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}
