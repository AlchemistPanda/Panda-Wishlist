package com.panda.wishlist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.panda.wishlist.ui.theme.CyberGlassTokens

@Composable
fun AmbientGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberGlassTokens.BgDeepSpace)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CyberGlassTokens.ElectricCyan.copy(alpha = 0.09f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.2f, size.height * 0.1f),
                        radius = size.width * 0.8f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CyberGlassTokens.IndigoGlow.copy(alpha = 0.07f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.85f),
                        radius = size.width * 0.72f
                    )
                )
            },
        content = content
    )
}
