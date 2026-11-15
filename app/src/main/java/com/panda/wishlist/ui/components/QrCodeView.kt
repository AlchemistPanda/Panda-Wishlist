package com.panda.wishlist.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.panda.wishlist.ui.theme.CyberGlassTokens

@Composable
fun QrCodeView(
    content: String,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp
) {
    val matrix = remember(content) {
        runCatching {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1
            )
            QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
        }.getOrNull()
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(2.dp, CyberGlassTokens.ElectricCyan.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        if (matrix != null) {
            Canvas(modifier = Modifier.size(size - 20.dp)) {
                val cellW = this.size.width / matrix.width
                val cellH = this.size.height / matrix.height
                for (x in 0 until matrix.width) {
                    for (y in 0 until matrix.height) {
                        if (matrix.get(x, y)) {
                            drawRect(
                                color = Color(0xFF030712),
                                topLeft = Offset(x * cellW, y * cellH),
                                size = Size(cellW + 0.5f, cellH + 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
