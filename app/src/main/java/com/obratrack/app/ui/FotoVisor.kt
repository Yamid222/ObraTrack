package com.obratrack.app.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import java.io.File

/** Foto pequeña; muestra un pin cuando la foto tiene ubicación. */
@Composable
fun Miniatura(
    ruta: String,
    conUbicacion: Boolean,
    onClick: () -> Unit,
    tamano: Dp = 76.dp
) {
    Box(
        Modifier
            .size(tamano)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = File(ruta),
            contentDescription = "Foto guardada",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (conUbicacion) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .background(Color(0xAA000000), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = "Con ubicación", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

/** Foto a pantalla completa con su fecha, hora y ubicación. */
@Composable
fun FotoVisor(
    ruta: String,
    fecha: Long?,
    latitud: Double?,
    longitud: Double?,
    onCerrar: () -> Unit,
    onEliminar: (() -> Unit)? = null
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            // Las acciones van arriba, en una barra fija, para que se vean en cualquier tamaño de pantalla.
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xB3000000))
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCerrar) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                    Spacer(Modifier.weight(1f))
                    if (latitud != null && longitud != null) {
                        TextButton(onClick = {
                            val intento = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$latitud,$longitud"))
                            try {
                                context.startActivity(intento)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No hay una aplicación de mapas instalada", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFFDBA74))
                            Spacer(Modifier.width(4.dp))
                            Text("Mapa", color = Color(0xFFFDBA74), maxLines = 1)
                        }
                    }
                    if (onEliminar != null) {
                        TextButton(onClick = onEliminar) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFCA5A5))
                            Spacer(Modifier.width(4.dp))
                            Text("Eliminar", color = Color(0xFFFCA5A5), maxLines = 1)
                        }
                    }
                }
                AsyncImage(
                    model = File(ruta),
                    contentDescription = "Foto",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xB3000000))
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    if (fecha != null) {
                        Text(
                            Formato.fechaHora(fecha),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        if (latitud != null && longitud != null) "Ubicación: ${Formato.coordenadas(latitud, longitud)}"
                        else "Sin ubicación registrada",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
