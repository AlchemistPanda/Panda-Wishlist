package com.panda.wishlist.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.panda.wishlist.core.database.entity.WishItemEntity
import com.panda.wishlist.core.database.entity.WishlistEntity
import com.panda.wishlist.ui.theme.CyberGlassTokens
import java.io.File

@Composable
fun glassTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyberGlassTokens.ElectricCyan,
    unfocusedBorderColor = Color.White.copy(alpha = 0.16f),
    focusedTextColor = CyberGlassTokens.TextPrimary,
    unfocusedTextColor = CyberGlassTokens.TextPrimary,
    focusedLabelColor = CyberGlassTokens.ElectricCyan,
    unfocusedLabelColor = CyberGlassTokens.TextSecondary,
    cursorColor = CyberGlassTokens.ElectricCyan
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateWishlistDialog(
    initialName: String = "",
    initialDesc: String = "",
    initialEmoji: String = "🎁",
    titleText: String = "New Wishlist Folder",
    confirmLabel: String = "Create Folder",
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, emoji: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var desc by remember { mutableStateOf(initialDesc) }
    var emoji by remember { mutableStateOf(initialEmoji) }
    val emojis = listOf("🎁", "⚡", "🎂", "🛍️", "🏡", "🎮", "👕", "📚", "✈️", "🐼")

    Dialog(onDismissRequest = onDismiss) {
        CasioGlassCard(accentBorderColor = CyberGlassTokens.ElectricCyan) {
            Text(
                text = titleText,
                color = CyberGlassTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Folder Icon",
                color = CyberGlassTokens.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                emojis.forEach { e ->
                    val selected = e == emoji
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) CyberGlassTokens.ElectricCyan.copy(alpha = 0.25f)
                                else CyberGlassTokens.GlassSlate.copy(alpha = 0.55f)
                            )
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (selected) CyberGlassTokens.ElectricCyan else Color.White.copy(alpha = 0.12f)
                                ),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { emoji = e },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = e, fontSize = 18.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Wishlist Name") },
                placeholder = { Text("e.g. Birthday Wishes, Home Setup") },
                singleLine = true,
                colors = glassTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                label = { Text("Short Description (optional)") },
                maxLines = 2,
                colors = glassTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", color = CyberGlassTokens.TextSecondary)
                }
                Button(
                    onClick = {
                        if (name.isNotBlank()) onConfirm(name.trim(), desc.trim(), emoji)
                    },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberGlassTokens.ElectricCyan,
                        contentColor = CyberGlassTokens.BgDeepSpace
                    ),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text(confirmLabel, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun PickWishDialog(
    wishTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (pickedByName: String, isSecret: Boolean, message: String) -> Unit
) {
    var isSecret by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        CasioGlassCard(accentBorderColor = CyberGlassTokens.MintEmerald) {
            Text(
                text = "🎁 Pick this Wish",
                color = CyberGlassTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = wishTitle,
                color = CyberGlassTokens.ElectricCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CasioGlassFilterChip(
                    text = "🙋 With Name",
                    selected = !isSecret,
                    onClick = { isSecret = false },
                    modifier = Modifier.weight(1f)
                )
                CasioGlassFilterChip(
                    text = "🤫 Secret Surprise",
                    selected = isSecret,
                    selectedColor = CyberGlassTokens.MintEmerald,
                    onClick = { isSecret = true },
                    modifier = Modifier.weight(1f)
                )
            }

            if (!isSecret) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Picker's Name") },
                    placeholder = { Text("e.g. Rahul") },
                    singleLine = true,
                    colors = glassTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Optional Note") },
                placeholder = { Text("e.g. Ordered! Arriving Friday") },
                maxLines = 2,
                colors = glassTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", color = CyberGlassTokens.TextSecondary)
                }
                Button(
                    onClick = {
                        onConfirm(
                            if (isSecret) "Secret Surprise 🤫" else name.trim(),
                            isSecret,
                            message.trim()
                        )
                    },
                    enabled = isSecret || name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberGlassTokens.MintEmerald,
                        contentColor = CyberGlassTokens.BgDeepSpace
                    ),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text("Confirm Pick", fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun ShareQrDialog(
    wishlist: WishlistEntity,
    onDismiss: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    val shareUrl = wishlist.publicShareUrl.ifBlank {
        "https://alchemistpanda.github.io/Panda-Wishlist/?id=${wishlist.id}"
    }

    Dialog(onDismissRequest = onDismiss) {
        CasioGlassCard(accentBorderColor = CyberGlassTokens.ElectricCyan) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${wishlist.emoji} Share Public Link",
                        color = CyberGlassTokens.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = CyberGlassTokens.TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Anyone can open this in a browser — no app or login needed!",
                    color = CyberGlassTokens.TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                QrCodeView(content = shareUrl, size = 188.dp)

                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberGlassTokens.BgDeepSpace.copy(alpha = 0.7f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = shareUrl,
                        color = CyberGlassTokens.ElectricCyan,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Wishlist Link", shareUrl))
                            onShowToast("Public link copied!")
                        },
                        border = BorderStroke(1.dp, CyberGlassTokens.ElectricCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyberGlassTokens.ElectricCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy", color = CyberGlassTokens.ElectricCyan, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(shareUrl)))
                            }
                        },
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = CyberGlassTokens.TextPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open", color = CyberGlassTokens.TextPrimary, maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Check out my ${wishlist.emoji} ${wishlist.name} on Panda Wishlist:\n$shareUrl"
                            )
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Wishlist Link"))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberGlassTokens.ElectricCyan,
                        contentColor = CyberGlassTokens.BgDeepSpace
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share via WhatsApp / Apps", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MoveItemDialog(
    wishTitle: String,
    currentWishlistId: String,
    wishlists: List<WishlistEntity>,
    onDismiss: () -> Unit,
    onSelectTarget: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        CasioGlassCard(accentBorderColor = CyberGlassTokens.ElectricCyan) {
            Text(
                text = "Move Wish to Folder",
                color = CyberGlassTokens.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = wishTitle,
                color = CyberGlassTokens.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                wishlists.filter { it.id != currentWishlistId }.forEach { folder ->
                    CasioGlassCard(
                        contentPadding = 12.dp,
                        onClick = { onSelectTarget(folder.id) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = folder.emoji, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folder.name,
                                    color = CyberGlassTokens.TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                if (folder.isPublicSynced) {
                                    Text(
                                        text = "Live Public Folder",
                                        color = CyberGlassTokens.MintEmerald,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel", color = CyberGlassTokens.TextSecondary)
            }
        }
    }
}

@Composable
fun FullScreenImageDialog(
    item: WishItemEntity,
    onDismiss: () -> Unit,
    onSaveToGallery: () -> Unit
) {
    val model: Any? = remember(item.id, item.localImagePath, item.remoteImageUrl) {
        when {
            item.localImagePath.isNotBlank() && File(item.localImagePath).exists() -> File(item.localImagePath)
            item.remoteImageUrl.isNotBlank() -> item.remoteImageUrl
            else -> null
        }
    }
    var zoomed by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberGlassTokens.BgDeepSpace.copy(alpha = 0.96f))
        ) {
            if (model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = item.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 72.dp, horizontal = 12.dp)
                        .graphicsLayer {
                            val s = if (zoomed) 1.85f else 1f
                            scaleX = s
                            scaleY = s
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { zoomed = !zoomed },
                                onTap = { zoomed = !zoomed }
                            )
                        }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    color = CyberGlassTokens.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberGlassTokens.GlassSlate.copy(alpha = 0.8f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = CyberGlassTokens.TextPrimary)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { zoomed = !zoomed },
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (zoomed) "Reset Zoom" else "Tap to Zoom", color = CyberGlassTokens.TextPrimary)
                }
                Button(
                    onClick = onSaveToGallery,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberGlassTokens.ElectricCyan,
                        contentColor = CyberGlassTokens.BgDeepSpace
                    ),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to Gallery", fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}
