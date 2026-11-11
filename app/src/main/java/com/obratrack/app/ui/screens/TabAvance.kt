package com.obratrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obratrack.app.data.EstadoEtapa
import com.obratrack.app.data.Etapa
import com.obratrack.app.data.Foto
import com.obratrack.app.data.Gasto
import com.obratrack.app.data.Obra
import com.obratrack.app.ui.EstadoVacio
import com.obratrack.app.ui.Etiqueta
import com.obratrack.app.ui.FotoVisor
import com.obratrack.app.ui.Formato
import com.obratrack.app.ui.Miniatura
import com.obratrack.app.ui.ObraViewModel
import com.obratrack.app.ui.Rutas
import com.obratrack.app.ui.TarjetaBase
import com.obratrack.app.ui.TituloSeccion
import com.obratrack.app.ui.theme.Paleta

/** Avance de la obra: resumen, datos y línea de tiempo con fotos (RF-02, RF-03, RF-10). */
@Composable
fun TabAvance(vm: ObraViewModel, obra: Obra, navegar: (String) -> Unit) {
    val flujoEtapas = remember(obra.id) { vm.etapas(obra.id) }
    val flujoFotos = remember(obra.id) { vm.fotos(obra.id) }
    val flujoGastos = remember(obra.id) { vm.gastos(obra.id) }
    val etapas by flujoEtapas.collectAsStateWithLifecycle(initialValue = emptyList<Etapa>())
    val fotos by flujoFotos.collectAsStateWithLifecycle(initialValue = emptyList<Foto>())
    val gastos by flujoGastos.collectAsStateWithLifecycle(initialValue = emptyList<Gasto>())
    var fotoVista by remember { mutableStateOf<Foto?>(null) }

    val fotosPorEtapa = fotos.groupBy { it.etapaId }
    val gastado = gastos.sumOf { it.monto }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 100.dp)
    ) {
        item { EncabezadoAvance(obra, etapas, gastado) }
        item { DatosObra(obra) }
        item { TituloSeccion("Línea de tiempo") }
        if (etapas.isEmpty()) {
            item {
                EstadoVacio(
                    icono = Icons.Default.Construction,
                    titulo = "Sin etapas todavía",
                    texto = "Toca \"Registrar etapa\" para agregar la primera, por ejemplo la cimentación, y adjuntar fotos del avance."
                )
            }
        }
        itemsIndexed(etapas, key = { _, e -> e.id }) { indice, etapa ->
            ItemEtapa(
                etapa = etapa,
                fotos = fotosPorEtapa[etapa.id].orEmpty(),
                esUltima = indice == etapas.lastIndex,
                onClick = { navegar(Rutas.etapaForm(obra.id, etapa.id)) },
                onFoto = { fotoVista = it }
            )
        }
    }

    fotoVista?.let { f ->
        FotoVisor(
            ruta = f.ruta,
            fecha = f.fecha,
            latitud = f.latitud,
            longitud = f.longitud,
            onCerrar = { fotoVista = null }
        )
    }
}

@Composable
private fun EncabezadoAvance(obra: Obra, etapas: List<Etapa>, gastado: Long) {
    val completadas = etapas.count { it.estado == EstadoEtapa.COMPLETADO.name }
    val avance = if (etapas.isEmpty()) 0f else completadas.toFloat() / etapas.size
    val porcentajeGasto = Formato.porcentaje(gastado, obra.presupuesto)

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Paleta.Naranja, Paleta.NaranjaClaro)))
            .padding(20.dp)
    ) {
        Column {
            Text("Avance de la obra", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
            Text(
                "${(avance * 100).toInt()}%",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { avance },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DatoEncabezado("Etapas", if (etapas.isEmpty()) "—" else "$completadas de ${etapas.size}")
                DatoEncabezado("Gastado", "$porcentajeGasto%")
                DatoEncabezado("Presupuesto", Formato.pesos(obra.presupuesto))
            }
        }
    }
}

@Composable
private fun DatoEncabezado(titulo: String, valor: String) {
    Column {
        Text(titulo, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
        Text(valor, color = Color.White, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun DatosObra(obra: Obra) {
    Spacer(Modifier.height(14.dp))
    TarjetaBase(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Inicio: ${Formato.fecha(obra.fechaInicio)}", style = MaterialTheme.typography.bodyLarge)
            }
            if (obra.descripcion.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(obra.descripcion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ItemEtapa(
    etapa: Etapa,
    fotos: List<Foto>,
    esUltima: Boolean,
    onClick: () -> Unit,
    onFoto: (Foto) -> Unit
) {
    val estado = EstadoEtapa.desde(etapa.estado)
    val color = Color(estado.argb)

    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Punto y línea vertical de la línea de tiempo
        Column(
            modifier = Modifier.width(32.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.padding(top = 20.dp).size(16.dp).background(color, CircleShape))
            if (!esUltima) {
                Box(
                    Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
        TarjetaBase(
            modifier = Modifier.weight(1f).padding(bottom = 12.dp),
            onClick = onClick
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        etapa.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Etiqueta(estado.etiqueta, color)
                }
                Text(
                    Formato.fecha(etapa.fecha),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (etapa.descripcion.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(etapa.descripcion, style = MaterialTheme.typography.bodyMedium)
                }
                if (fotos.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fotos.forEach { foto ->
                            Miniatura(
                                ruta = foto.ruta,
                                conUbicacion = foto.latitud != null,
                                onClick = { onFoto(foto) }
                            )
                        }
                    }
                    val conUbicacion = fotos.count { it.latitud != null }
                    if (conUbicacion > 0) {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "$conUbicacion de ${fotos.size} foto(s) con ubicación GPS",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
