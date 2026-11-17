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
    isModal: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderBrush = if (accentBorderColor != null) {
        Brush.verticalGradient(
            colors = listOf(
                accentBorderColor.copy(alpha = 0.78f),
                accentBorderColor.copy(alpha = 0.36f),
                Color.White.copy(alpha = 0.12f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.26f),
                CyberGlassTokens.ElectricCyan.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.10f)
            )
        )
    }

    val topAlpha = if (isModal) 0.96f else 0.88f
    val bottomAlpha = if (isModal) 0.98f else 0.95f

    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            CyberGlassTokens.GlassSlate.copy(alpha = topAlpha),
            CyberGlassTokens.GlassSurface.copy(alpha = bottomAlpha)
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
