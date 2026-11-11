package com.obratrack.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Paleta {
    val Naranja = Color(0xFFC2410C)
    val NaranjaClaro = Color(0xFFF59E0B)
    val Crema = Color(0xFFF8F6F2)
    val Pizarra = Color(0xFF1E293B)
    val Verde = Color(0xFF16A34A)
    val Rojo = Color(0xFFDC2626)
    val Ambar = Color(0xFFD97706)
}

private val Colores = lightColorScheme(
    primary = Paleta.Naranja,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEDD5),
    onPrimaryContainer = Color(0xFF7C2D12),
    secondary = Color(0xFF475569),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Pizarra,
    tertiary = Paleta.Verde,
    background = Paleta.Crema,
    onBackground = Pizarra,
    surface = Color.White,
    onSurface = Pizarra,
    surfaceVariant = Color(0xFFF1EDE6),
    onSurfaceVariant = Color(0xFF57534E),
    outline = Color(0xFFA8A29E),
    outlineVariant = Color(0xFFE7E1D8),
    error = Paleta.Rojo
)

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val Tipografia = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp)
)

@Composable
fun ObraTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colores,
        shapes = Formas,
        typography = Tipografia,
        content = content
    )
}
