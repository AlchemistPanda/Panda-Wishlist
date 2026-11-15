package com.panda.wishlist.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CyberGlassTokens.ElectricCyan,
    onPrimary = CyberGlassTokens.BgDeepSpace,
    primaryContainer = CyberGlassTokens.DeepCyan,
    onPrimaryContainer = CyberGlassTokens.TextPrimary,
    secondary = CyberGlassTokens.MintEmerald,
    onSecondary = CyberGlassTokens.BgDeepSpace,
    background = CyberGlassTokens.BgDeepSpace,
    onBackground = CyberGlassTokens.TextPrimary,
    surface = CyberGlassTokens.GlassSurface,
    onSurface = CyberGlassTokens.TextPrimary,
    surfaceVariant = CyberGlassTokens.GlassSlate,
    onSurfaceVariant = CyberGlassTokens.TextSecondary,
    error = CyberGlassTokens.NeonRose,
    onError = CyberGlassTokens.TextPrimary
)

@Composable
fun PandaWishlistTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = CyberGlassTokens.BgDeepSpace.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = CyberGlassTokens.BgDeepSpace.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
