package app.androfile.manager.ui

import androidx.compose.ui.graphics.Color

data class AppPalette(
    val background: Color,
    val backgroundShade: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val accentSoft: Color
)

fun paletteFor(appearance: AppAppearance): AppPalette {
    val background = when (appearance.background) {
        "graphite" -> Color(0xFF17191F)
        "ocean" -> Color(0xFF0C1A29)
        "plum" -> Color(0xFF1B1526)
        else -> Color(0xFF10151E)
    }
    val accent = when (appearance.accent) {
        "cyan" -> Color(0xFF69C9F0)
        "violet" -> Color(0xFFB18BF3)
        "amber" -> Color(0xFFE8B76E)
        else -> Color(0xFF70DDB5)
    }
    return AppPalette(
        background = background,
        backgroundShade = background.copy(red = background.red * 0.82f, green = background.green * 0.86f, blue = background.blue * 0.92f),
        surface = Color(0xFF1B2532),
        surfaceRaised = Color(0xFF243143),
        text = Color(0xFFF2F5F8),
        muted = Color(0xFF9EADBF),
        accent = accent,
        accentSoft = accent.copy(alpha = 0.13f)
    )
}
