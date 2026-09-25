package io.github.chalexey.cashpet.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = CoinYellowDark,
    onPrimary = SurfaceCream,
    primaryContainer = Color(0xFFFFE7A8),
    onPrimaryContainer = Color(0xFF3B2800),
    secondary = NeedGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8F2E2),
    onSecondaryContainer = Color(0xFF123B24),
    tertiary = WantBlue,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCE7FF),
    onTertiaryContainer = Color(0xFF172E63),
    background = SurfaceCream,
    onBackground = OnLight,
    surface = SurfaceCream,
    onSurface = OnLight,
    surfaceVariant = Color(0xFFF3EBDD),
    onSurfaceVariant = Color(0xFF625A50),
    error = WarningAmber,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFC94D),
    onPrimary = Color(0xFF3A2900),
    primaryContainer = Color(0xFF5B4300),
    onPrimaryContainer = Color(0xFFFFE7A8),
    secondary = Color(0xFF8DD5A7),
    onSecondary = Color(0xFF10351F),
    secondaryContainer = Color(0xFF214C32),
    onSecondaryContainer = Color(0xFFB5EFC6),
    tertiary = Color(0xFFAEC5FF),
    onTertiary = Color(0xFF132E62),
    tertiaryContainer = Color(0xFF304A7A),
    onTertiaryContainer = Color(0xFFDCE7FF),
    background = Color(0xFF14120F),
    onBackground = OnDark,
    surface = Color(0xFF191713),
    onSurface = OnDark,
    surfaceVariant = Color(0xFF2A251F),
    onSurfaceVariant = Color(0xFFD0C6B9),
    error = Color(0xFFFFB4AB),
)

private val CashPetShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
)

@Composable
fun CashPetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = CashPetTypography,
        shapes = CashPetShapes,
        content = content,
    )
}
