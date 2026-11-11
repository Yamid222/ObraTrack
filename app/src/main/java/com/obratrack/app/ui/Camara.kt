package com.obratrack.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.coroutines.resume

/** Resultado de tomar una foto: archivo, hora exacta y coordenadas (si hubo GPS). */
data class Captura(
    val ruta: String,
    val fecha: Long,
    val latitud: Double?,
    val longitud: Double?
)

object Archivos {
    fun nuevaFoto(context: Context, prefijo: String): File {
        val carpeta = File(context.filesDir, "fotos").apply { mkdirs() }
        return File(carpeta, "${prefijo}_${System.currentTimeMillis()}.jpg").apply { createNewFile() }
    }

    fun uri(context: Context, archivo: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)

    fun borrar(ruta: String?) {
        if (ruta != null) runCatching { File(ruta).delete() }
    }
}

/** Ubicación con el GPS nativo del celular (funciona sin internet). */
object Ubicacion {
    fun tienePermiso(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun obtener(context: Context): Location? {
        if (!tienePermiso(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val activos = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).filter {
            runCatching { lm.isProviderEnabled(it) }.getOrDefault(false)
        }
        for (proveedor in activos) {
            val espera = if (proveedor == LocationManager.GPS_PROVIDER) 8_000L else 4_000L
            val ubicacion = try {
                withTimeoutOrNull(espera) { actual(context, lm, proveedor) }
            } catch (e: SecurityException) {
                null
            }
            if (ubicacion != null) return ubicacion
        }
        // Si no hubo lectura nueva, se usa la última ubicación conocida.
        return runCatching {
            lm.getProviders(true).mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time }
        }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    private suspend fun actual(context: Context, lm: LocationManager, proveedor: String): Location? =
        suspendCancellableCoroutine { cont ->
            val senal = CancellationSignal()
            cont.invokeOnCancellation { senal.cancel() }
            LocationManagerCompat.getCurrentLocation(
                lm, proveedor, senal, ContextCompat.getMainExecutor(context)
            ) { ubicacion ->
                if (cont.isActive) cont.resume(ubicacion)
            }
        }
}

/**
 * Devuelve una función que abre la cámara. Al terminar entrega la foto con la hora del sistema
 * y, si [conUbicacion] es verdadero, las coordenadas GPS (pide el permiso la primera vez).
 */
@Composable
fun rememberCamara(
    prefijo: String,
    conUbicacion: Boolean,
    alCapturar: (Captura) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var rutaPendiente by rememberSaveable { mutableStateOf<String?>(null) }
    var buscandoUbicacion by remember { mutableStateOf(false) }
    val entregar by rememberUpdatedState(alCapturar)

    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        val ruta = rutaPendiente
        rutaPendiente = null
        if (!exito || ruta == null) {
            Archivos.borrar(ruta)
        } else {
            val fecha = System.currentTimeMillis()
            if (conUbicacion) {
                buscandoUbicacion = true
                scope.launch {
                    val ubicacion = Ubicacion.obtener(context)
                    buscandoUbicacion = false
                    entregar(Captura(ruta, fecha, ubicacion?.latitude, ubicacion?.longitude))
                }
            } else {
                entregar(Captura(ruta, fecha, null, null))
            }
        }
    }

    fun abrirCamara() {
        val archivo = Archivos.nuevaFoto(context, prefijo)
        rutaPendiente = archivo.absolutePath
        camara.launch(Archivos.uri(context, archivo))
    }

    val permisos = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        // Con o sin permiso se abre la cámara; sin permiso la foto queda sin ubicación.
        abrirCamara()
    }

    if (buscandoUbicacion) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Guardando la foto") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                    Spacer(Modifier.width(16.dp))
                    Text("Obteniendo la ubicación del GPS…")
                }
            }
        )
    }

    return {
        if (conUbicacion && !Ubicacion.tienePermiso(context)) {
            permisos.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        } else {
            abrirCamara()
        }
    }
}
