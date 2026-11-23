package app.androfile.manager.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun AndroFileTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = Color(0xFF70DDB5),
        onPrimary = Color(0xFF06251A),
        secondary = Color(0xFF73B8F4),
        background = Color(0xFF10141D),
        surface = Color(0xFF171F2B),
        surfaceVariant = Color(0xFF222D3B),
        onBackground = Color(0xFFF2F5F8),
        onSurface = Color(0xFFF2F5F8),
        onSurfaceVariant = Color(0xFFA6B1C0)
    )
    MaterialTheme(colorScheme = colors, content = content)
}
