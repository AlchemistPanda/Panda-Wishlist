package com.panda.wishlist.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.panda.wishlist.ui.theme.CyberGlassTokens

@Composable
fun CasioGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    onClick: (() -> Unit)? = null,
    accentBorderColor: Color? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderBrush = if (accentBorderColor != null) {
        Brush.verticalGradient(
            colors = listOf(
                accentBorderColor.copy(alpha = 0.65f),
                accentBorderColor.copy(alpha = 0.25f),
                Color.White.copy(alpha = 0.06f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.20f),
                CyberGlassTokens.ElectricCyan.copy(alpha = 0.11f),
                Color.White.copy(alpha = 0.04f)
            )
        )
    }

    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            CyberGlassTokens.GlassSlate.copy(alpha = 0.58f),
            CyberGlassTokens.GlassSurface.copy(alpha = 0.76f)
        )
    )

    Surface(
        modifier = modifier
            .clip(shape)
            .border(BorderStroke(1.dp, borderBrush), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgBrush)
                .padding(contentPadding),
            content = content
        )
    }
}
