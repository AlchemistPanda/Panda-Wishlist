package com.panda.wishlist.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.panda.wishlist.ui.theme.CyberGlassTokens

@Composable
fun CasioGlassFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = CyberGlassTokens.ElectricCyan,
    badgeCount: Int? = null,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) selectedColor else Color.White.copy(alpha = 0.12f),
        animationSpec = tween(180),
        label = "chipBorder"
    )

    val textColor by animateColorAsState(
        targetValue = if (selected) CyberGlassTokens.TextPrimary else CyberGlassTokens.TextSecondary,
        animationSpec = tween(180),
        label = "chipText"
    )

    val shape = RoundedCornerShape(12.dp)

    val backgroundBrush = if (selected) {
        Brush.verticalGradient(
            colors = listOf(
                selectedColor.copy(alpha = 0.34f),
                selectedColor.copy(alpha = 0.14f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                CyberGlassTokens.GlassSlate.copy(alpha = 0.50f),
                CyberGlassTokens.GlassSurface.copy(alpha = 0.70f)
            )
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .border(BorderStroke(1.dp, borderColor), shape)
            .background(backgroundBrush)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )
            if (badgeCount != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (selected) selectedColor else CyberGlassTokens.GlassSlate
                        )
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = badgeCount.toString(),
                        color = if (selected) CyberGlassTokens.BgDeepSpace else CyberGlassTokens.TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
