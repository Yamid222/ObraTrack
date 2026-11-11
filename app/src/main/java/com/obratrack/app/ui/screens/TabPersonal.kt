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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obratrack.app.data.Obra
import com.obratrack.app.data.TipoPago
import com.obratrack.app.data.Trabajador
import com.obratrack.app.ui.EstadoVacio
import com.obratrack.app.ui.Etiqueta
import com.obratrack.app.ui.Formato
import com.obratrack.app.ui.ObraViewModel
import com.obratrack.app.ui.Rutas
import com.obratrack.app.ui.TarjetaBase
import com.obratrack.app.ui.theme.Paleta

/** RF-04: listado de trabajadores de la obra. */
@Composable
fun TabPersonal(vm: ObraViewModel, obra: Obra, navegar: (String) -> Unit) {
    val flujo = remember(obra.id) { vm.trabajadores(obra.id) }
    val trabajadores by flujo.collectAsStateWithLifecycle(initialValue = emptyList<Trabajador>())

    if (trabajadores.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EstadoVacio(
                icono = Icons.Default.Groups,
                titulo = "Aún no hay personal",
                texto = "Toca \"Agregar trabajador\" para registrar a los obreros con su cargo y lo que se acordó pagarles."
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(trabajadores, key = { it.id }) { trabajador ->
                TarjetaTrabajador(trabajador) { navegar(Rutas.trabajadorForm(obra.id, trabajador.id)) }
            }
        }
    }
}

@Composable
private fun TarjetaTrabajador(trabajador: Trabajador, onClick: () -> Unit) {
    val tipo = TipoPago.desde(trabajador.tipoPago)
    val color = when (tipo) {
        TipoPago.JORNAL -> Paleta.Naranja
        TipoPago.SEMANA -> Color(0xFF16A34A)
        TipoPago.DESTAJO -> Color(0xFF2563EB)
    }

    TarjetaBase(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).background(color.copy(alpha = 0.13f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    trabajador.nombre.firstOrNull()?.uppercase() ?: "?",
                    color = color,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(trabajador.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    trabajador.cargo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Etiqueta(tipo.etiqueta, color)
                Text(
                    "${Formato.pesos(trabajador.valor)} ${tipo.valorPor}" +
                        if (tipo == TipoPago.SEMANA) " (${trabajador.diasSemana} días)" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
