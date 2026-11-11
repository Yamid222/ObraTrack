package com.obratrack.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obratrack.app.data.CategoriaGasto
import com.obratrack.app.data.Gasto
import com.obratrack.app.data.Obra
import com.obratrack.app.ui.EstadoVacio
import com.obratrack.app.ui.Formato
import com.obratrack.app.ui.ObraViewModel
import com.obratrack.app.ui.TarjetaBase
import com.obratrack.app.ui.TituloSeccion
import com.obratrack.app.ui.theme.Paleta

/** RF-09: balance automático de gastos frente al presupuesto, por categoría y en total. */
@Composable
fun TabBalance(vm: ObraViewModel, obra: Obra) {
    val flujo = remember(obra.id) { vm.gastos(obra.id) }
    val gastos by flujo.collectAsStateWithLifecycle(initialValue = emptyList<Gasto>())

    val gastado = gastos.sumOf { it.monto }
    val disponible = obra.presupuesto - gastado
    val porcentaje = Formato.porcentaje(gastado, obra.presupuesto)
    val color = when {
        porcentaje >= 100 -> Paleta.Rojo
        porcentaje >= 80 -> Paleta.Ambar
        else -> Paleta.Verde
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TarjetaBase(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Presupuesto ejecutado", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { (porcentaje / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.size(150.dp),
                            strokeWidth = 14.dp,
                            color = color,
                            trackColor = color.copy(alpha = 0.15f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$porcentaje%", style = MaterialTheme.typography.headlineMedium, color = color)
                            Text("gastado", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Cifra("Presupuesto", Formato.pesos(obra.presupuesto), MaterialTheme.colorScheme.onSurface)
                        Cifra("Gastado", Formato.pesos(gastado), color)
                        Cifra(
                            if (disponible >= 0) "Disponible" else "Excedido",
                            Formato.pesos(if (disponible >= 0) disponible else -disponible),
                            if (disponible >= 0) Paleta.Verde else Paleta.Rojo
                        )
                    }
                }
            }
        }

        if (porcentaje >= 80) {
            item {
                val mensaje = if (porcentaje >= 100) "Superaste el presupuesto en ${Formato.pesos(-disponible)}."
                else "Ya usaste el $porcentaje% del presupuesto. Quedan ${Formato.pesos(disponible)}."
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(color.copy(alpha = 0.12f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = color)
                    Spacer(Modifier.width(10.dp))
                    Text(mensaje, color = color, style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        item { TituloSeccion("Gasto por categoría") }

        if (gastos.isEmpty()) {
            item {
                EstadoVacio(
                    icono = Icons.Default.Assessment,
                    titulo = "Sin gastos registrados",
                    texto = "Cuando registres gastos, aquí verás cuánto llevas en cada categoría frente al presupuesto."
                )
            }
        } else {
            val porCategoria = CategoriaGasto.entries.map { cat ->
                val delaCategoria = gastos.filter { it.categoria == cat.name }
                Triple(cat, delaCategoria.sumOf { it.monto }, delaCategoria.size)
            }
            items(porCategoria.size) { i ->
                val (categoria, monto, cantidad) = porCategoria[i]
                FilaCategoria(categoria, monto, cantidad, obra.presupuesto)
            }
        }
    }
}

@Composable
private fun Cifra(titulo: String, valor: String, color: Color) {
    Column {
        Text(titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
private fun FilaCategoria(categoria: CategoriaGasto, monto: Long, cantidad: Int, presupuesto: Long) {
    val color = Color(categoria.argb)
    val porcentaje = Formato.porcentaje(monto, presupuesto)

    TarjetaBase(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).background(color, CircleShape))
                Spacer(Modifier.width(10.dp))
                Text(categoria.etiqueta, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(Formato.pesos(monto), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (porcentaje / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(50)),
                color = color,
                trackColor = color.copy(alpha = 0.15f)
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "$cantidad gasto(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "$porcentaje% del presupuesto",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}
