package com.panda.wishlist.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.panda.wishlist.core.database.entity.WishlistEntity
import com.panda.wishlist.ui.PendingIncomingShare
import com.panda.wishlist.ui.theme.CyberGlassTokens
import java.io.File

@Composable
fun IncomingShareSheet(
    pending: PendingIncomingShare,
    wishlists: List<WishlistEntity>,
    defaultWishlistId: String,
    onDismiss: () -> Unit,
    onCreateNewFolder: () -> Unit,
    onSave: (
        wishlistId: String,
        title: String,
        noteText: String,
        linkUrl: String,
        priceText: String,
        priority: Int,
        publishAndShare: Boolean
    ) -> Unit
) {
    var selectedWishlistId by remember(defaultWishlistId, wishlists) {
        mutableStateOf(
            wishlists.find { it.id == defaultWishlistId }?.id
                ?: wishlists.firstOrNull()?.id.orEmpty()
        )
    }
    var title by remember(pending.payload.title) { mutableStateOf(pending.payload.title) }
    var noteText by remember(pending.payload.noteText) { mutableStateOf(pending.payload.noteText) }
    var linkUrl by remember(pending.payload.linkUrl) { mutableStateOf(pending.payload.linkUrl) }
    var priceText by remember(pending.payload.priceText) { mutableStateOf(pending.payload.priceText) }
    var priority by remember { mutableIntStateOf(1) }

    LaunchedEffect(wishlists) {
        if (selectedWishlistId.isBlank() && wishlists.isNotEmpty()) {
            selectedWishlistId = wishlists.first().id
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            CasioGlassCard(accentBorderColor = CyberGlassTokens.ElectricCyan) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🎁 Save to Panda Wishlist",
                                color = CyberGlassTokens.TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = buildString {
                                    append("Shared ")
                                    append(pending.payload.itemType.name.lowercase())
                                    if (pending.payload.storeDomain.isNotBlank()) {
                                        append(" from ${pending.payload.storeDomain}")
                                    }
                                },
                                color = CyberGlassTokens.ElectricCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = CyberGlassTokens.TextSecondary)
                        }
                    }

                    if (pending.isFetchingMeta) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = CyberGlassTokens.ElectricCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Fetching product preview & price...",
                                color = CyberGlassTokens.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    val previewModel: Any? = remember(pending.payload.localImagePaths, pending.remoteImageUrl) {
                        val firstLocal = pending.payload.localImagePaths.firstOrNull()
                        when {
                            firstLocal != null && File(firstLocal).exists() -> File(firstLocal)
                            pending.remoteImageUrl.isNotBlank() -> pending.remoteImageUrl
                            else -> null
                        }
                    }
                    if (previewModel != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyberGlassTokens.BgDeepSpace)
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = previewModel,
                                contentDescription = "Shared preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(130.dp)
                            )
                            if (pending.payload.localImagePaths.size > 1) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CyberGlassTokens.BgDeepSpace.copy(alpha = 0.85f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${pending.payload.localImagePaths.size} photos",
                                        color = CyberGlassTokens.ElectricCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Choose Wishlist Folder",
                        color = CyberGlassTokens.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        wishlists.forEach { folder ->
                            CasioGlassFilterChip(
                                text = "${folder.emoji} ${folder.name}",
                                selected = selectedWishlistId == folder.id,
                                onClick = { selectedWishlistId = folder.id }
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyberGlassTokens.MintEmerald.copy(alpha = 0.16f))
                                .border(1.dp, CyberGlassTokens.MintEmerald.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable(onClick = onCreateNewFolder)
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = CyberGlassTokens.MintEmerald, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New", color = CyberGlassTokens.MintEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Wish Title") },
                        maxLines = 2,
                        colors = glassTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text("Price (optional)") },
                            placeholder = { Text("₹1,999") },
                            singleLine = true,
                            colors = glassTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (linkUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = linkUrl,
                            onValueChange = { linkUrl = it },
                            label = { Text("Link URL") },
                            singleLine = true,
                            colors = glassTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Note / Text") },
                        maxLines = 3,
                        colors = glassTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Priority",
                        color = CyberGlassTokens.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CasioGlassFilterChip(
                            text = "Nice",
                            selected = priority == 0,
                            onClick = { priority = 0 },
                            modifier = Modifier.weight(1f)
                        )
                        CasioGlassFilterChip(
                            text = "Love It",
                            selected = priority == 1,
                            onClick = { priority = 1 },
                            modifier = Modifier.weight(1f)
                        )
                        CasioGlassFilterChip(
                            text = "⭐ Must",
                            selected = priority == 2,
                            selectedColor = CyberGlassTokens.NeonRose,
                            onClick = { priority = 2 },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (selectedWishlistId.isNotBlank()) {
                                    onSave(selectedWishlistId, title, noteText, linkUrl, priceText, priority, false)
                                }
                            },
                            enabled = selectedWishlistId.isNotBlank(),
                            border = BorderStroke(1.dp, CyberGlassTokens.ElectricCyan.copy(alpha = 0.6f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save", color = CyberGlassTokens.ElectricCyan, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        Button(
                            onClick = {
                                if (selectedWishlistId.isNotBlank()) {
                                    onSave(selectedWishlistId, title, noteText, linkUrl, priceText, priority, true)
                                }
                            },
                            enabled = selectedWishlistId.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberGlassTokens.ElectricCyan,
                                contentColor = CyberGlassTokens.BgDeepSpace
                            ),
                            modifier = Modifier.weight(1.35f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save & Share", fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
