package com.opennotes.widget

import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders

private val pureBlackColors =
    darkColorScheme(
        background = Color.Black,
        onBackground = Color.White,
        surface = Color.Black,
        onSurface = Color.White,
        surfaceVariant = Color.Black,
        onSurfaceVariant = Color.White,
        primary = Color.White,
        onPrimary = Color.Black,
    )

/** Both day and night palettes stay black, including Markdown code surfaces. */
@Composable
fun WidgetTheme(
    pureBlack: Boolean,
    content: @Composable () -> Unit,
) {
    if (pureBlack) {
        GlanceTheme(colors = ColorProviders(light = pureBlackColors, dark = pureBlackColors), content = content)
    } else {
        GlanceTheme(content = content)
    }
}
