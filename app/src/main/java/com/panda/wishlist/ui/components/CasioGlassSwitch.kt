package com.panda.wishlist.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.panda.wishlist.ui.theme.CyberGlassTokens

@Composable
fun CasioGlassSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }

    val trackWidth = 52.dp
    val trackHeight = 30.dp
    val thumbSize = 24.dp
    val thumbPadding = 3.dp

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - thumbPadding else thumbPadding,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "thumbOffset"
    )

    val activeTrackBrush = Brush.horizontalGradient(
        colors = listOf(
            CyberGlassTokens.DeepCyan,
            CyberGlassTokens.ElectricCyan
        )
    )

    val inactiveTrackBrush = Brush.verticalGradient(
        colors = listOf(
            CyberGlassTokens.GlassSlate.copy(alpha = 0.65f),
            CyberGlassTokens.GlassSurface.copy(alpha = 0.85f)
        )
    )

    val trackBorder = if (checked) {
        BorderStroke(1.dp, CyberGlassTokens.ElectricCyan.copy(alpha = 0.80f))
    } else {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    }

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clip(RoundedCornerShape(15.dp))
            .border(trackBorder, RoundedCornerShape(15.dp))
            .background(if (checked) activeTrackBrush else inactiveTrackBrush)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = { onCheckedChange?.invoke(!checked) }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8FAFC),
                            Color(0xFFCBD5E1)
                        )
                    )
                )
                .border(
                    BorderStroke(0.8.dp, Color.White.copy(alpha = 0.7f)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (checked) CyberGlassTokens.DeepCyan else CyberGlassTokens.TextTertiary
                    )
            )
        }
    }
}
