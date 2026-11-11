package com.obratrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obratrack.app.data.CategoriaVista
import com.obratrack.app.data.Categorias
import com.obratrack.app.data.Gasto
import com.obratrack.app.data.Obra
import com.obratrack.app.ui.EstadoVacio
import com.obratrack.app.ui.Etiqueta
import com.obratrack.app.ui.Formato
import com.obratrack.app.ui.FotoVisor
import com.obratrack.app.ui.Miniatura
import com.obratrack.app.ui.ObraViewModel
import com.obratrack.app.ui.Rutas
import com.obratrack.app.ui.TarjetaBase
import com.obratrack.app.ui.theme.Paleta

/** RF-07 y RF-08: listado de gastos con su factura fotografiada. */
@Composable
fun TabGastos(vm: ObraViewModel, obra: Obra, navegar: (String) -> Unit) {
    val flujo = remember(obra.id) { vm.gastos(obra.id) }
    val gastos by flujo.collectAsStateWithLifecycle(initialValue = emptyList<Gasto>())
    var facturaVista by remember { mutableStateOf<Gasto?>(null) }
    val propias by vm.categorias.collectAsStateWithLifecycle()
    val opcionesCategoria = Categorias.todas(propias)

    if (gastos.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EstadoVacio(
                icono = Icons.Default.Receipt,
                titulo = "Aún no hay gastos",
                texto = "Toca \"Registrar gasto\" para anotar cada compra o pago y guardar la foto de la factura."
            )
        }
    } else {
        val total = gastos.sumOf { it.monto }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(Paleta.Pizarra, Color(0xFF475569))))
                        .padding(20.dp)
                ) {
                    Column {
                        Text("Total gastado", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
                        Text(Formato.pesos(total), color = Color.White, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "${gastos.size} gasto(s) registrados",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            items(gastos, key = { it.id }) { gasto ->
                TarjetaGasto(
                    gasto = gasto,
                    categoria = Categorias.buscar(gasto.categoria, opcionesCategoria),
                    onClick = { navegar(Rutas.gastoForm(obra.id, gasto.id)) },
                    onFactura = { facturaVista = gasto }
                )
            }
        }
    }

    facturaVista?.let { g ->
        if (g.facturaRuta != null) {
            FotoVisor(
                ruta = g.facturaRuta,
                fecha = null,
                latitud = null,
                longitud = null,
                onCerrar = { facturaVista = null }
            )
        }
    }
}

@Composable
private fun TarjetaGasto(gasto: Gasto, categoria: CategoriaVista, onClick: () -> Unit, onFactura: () -> Unit) {
    val color = Color(categoria.argb)

    TarjetaBase(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(6.dp).fillMaxHeight().background(color))
            Row(
                Modifier.padding(14.dp).weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(gasto.descripcion, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Etiqueta(categoria.etiqueta, color)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            Formato.fecha(gasto.fecha),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(Formato.pesos(gasto.monto), style = MaterialTheme.typography.titleMedium)
                    if (gasto.facturaRuta != null) {
                        Spacer(Modifier.height(6.dp))
                        Miniatura(
                            ruta = gasto.facturaRuta,
                            conUbicacion = false,
                            onClick = onFactura,
                            tamano = 44.dp
                        )
                    }
                }
            }
        }
    }
}
