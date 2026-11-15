package com.panda.wishlist.ui.screens.wishlists

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.panda.wishlist.core.database.entity.WishItemEntity
import com.panda.wishlist.core.database.entity.WishItemType
import com.panda.wishlist.core.database.entity.WishlistEntity
import com.panda.wishlist.ui.components.CasioGlassCard
import com.panda.wishlist.ui.components.CasioGlassFilterChip
import com.panda.wishlist.ui.components.CreateWishlistDialog
import com.panda.wishlist.ui.components.FullScreenImageDialog
import com.panda.wishlist.ui.components.MoveItemDialog
import com.panda.wishlist.ui.components.PickWishDialog
import com.panda.wishlist.ui.theme.CyberGlassTokens
import java.io.File

private enum class DetailFilter(val label: String) {
    ALL("All"),
    AVAILABLE("Available"),
    PICKED("Picked 🎁"),
    LINK("Links 🔗"),
    IMAGE("Images 🖼️"),
    TEXT("Text 📝"),
    MUST("Must ⭐")
}

@Composable
fun WishlistDetailScreen(
    wishlist: WishlistEntity,
    items: List<WishItemEntity>,
    allWishlists: List<WishlistEntity>,
    isSyncing: Boolean,
    onBack: () -> Unit,
    onSyncAndShare: () -> Unit,
    onShowQr: () -> Unit,
    onRefreshClaims: () -> Unit,
    onEditWishlist: (name: String, desc: String, emoji: String) -> Unit,
    onDeleteWishlist: () -> Unit,
    onDeleteItem: (String) -> Unit,
    onMoveItem: (itemId: String, targetWishlistId: String) -> Unit,
    onSetItemPicked: (itemId: String, isPicked: Boolean, pickedByName: String, isSecret: Boolean, message: String) -> Unit,
    onSaveImageToGallery: (WishItemEntity) -> Unit,
    onShowToast: (String) -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    var activeFilter by remember { mutableStateOf(DetailFilter.ALL) }
    var fullScreenImageItem by remember { mutableStateOf<WishItemEntity?>(null) }
    var pickDialogItem by remember { mutableStateOf<WishItemEntity?>(null) }
    var moveDialogItem by remember { mutableStateOf<WishItemEntity?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val filtered = remember(items, activeFilter) {
        when (activeFilter) {
            DetailFilter.ALL -> items
            DetailFilter.AVAILABLE -> items.filter { !it.isPicked }
            DetailFilter.PICKED -> items.filter { it.isPicked }
            DetailFilter.LINK -> items.filter { it.linkUrl.isNotBlank() }
            DetailFilter.IMAGE -> items.filter { it.localImagePath.isNotBlank() || it.remoteImageUrl.isNotBlank() }
            DetailFilter.TEXT -> items.filter { it.itemType == WishItemType.TEXT.name }
            DetailFilter.MUST -> items.filter { it.priority >= 2 }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberGlassTokens.GlassSlate.copy(alpha = 0.7f))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CyberGlassTokens.TextPrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${wishlist.emoji} ${wishlist.name}",
                    color = CyberGlassTokens.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (wishlist.isPublicSynced) "Live Public Link Active" else "Private Folder (Tap Share to publish)",
                    color = if (wishlist.isPublicSynced) CyberGlassTokens.MintEmerald else CyberGlassTokens.TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(onClick = { showEditDialog = true }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Folder", tint = CyberGlassTokens.TextSecondary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Folder", tint = CyberGlassTokens.NeonRose, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sync & Share Action Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberGlassTokens.ElectricCyan.copy(alpha = 0.18f))
                    .border(1.dp, CyberGlassTokens.ElectricCyan.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                    .clickable(enabled = !isSyncing, onClick = onSyncAndShare),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp, color = CyberGlassTokens.ElectricCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Syncing to GitHub Pages...", color = CyberGlassTokens.ElectricCyan, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Share, contentDescription = null, tint = CyberGlassTokens.ElectricCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (wishlist.isPublicSynced) "Sync & Share Link" else "Publish & Share Publicly",
                            color = CyberGlassTokens.ElectricCyan,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            if (wishlist.isPublicSynced) {
                IconButton(
                    onClick = onShowQr,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberGlassTokens.GlassSlate.copy(alpha = 0.7f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = "QR Code", tint = CyberGlassTokens.TextPrimary, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = onRefreshClaims,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberGlassTokens.GlassSlate.copy(alpha = 0.7f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Sync Friend Picks", tint = CyberGlassTokens.MintEmerald, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DetailFilter.entries.forEach { f ->
                CasioGlassFilterChip(
                    text = f.label,
                    selected = activeFilter == f,
                    onClick = { activeFilter = f }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filtered.isEmpty()) {
            CasioGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🐼✨", fontSize = 34.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (items.isEmpty()) "This wishlist is empty" else "No wishes match this filter",
                        color = CyberGlassTokens.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Share links, photos, or text from WhatsApp, Amazon, Flipkart, Instagram, Facebook, or Gallery straight into Panda Wishlist!",
                        color = CyberGlassTokens.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.id }) { item ->
                    WishItemCard(
                        item = item,
                        canMove = allWishlists.size > 1,
                        onOpenLink = {
                            if (item.linkUrl.isNotBlank()) {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.linkUrl)))
                                }.onFailure { onShowToast("Could not open link") }
                            }
                        },
                        onCopyText = {
                            val clipText = item.noteText.ifBlank { item.linkUrl.ifBlank { item.title } }
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Wish Content", clipText))
                            onShowToast("Copied to clipboard!")
                        },
                        onViewImage = { fullScreenImageItem = item },
                        onSaveImage = { onSaveImageToGallery(item) },
                        onPickOrUnpick = {
                            if (item.isPicked) {
                                onSetItemPicked(item.id, false, "", false, "")
                            } else {
                                pickDialogItem = item
                            }
                        },
                        onMove = { moveDialogItem = item },
                        onDelete = { onDeleteItem(item.id) }
                    )
                }
            }
        }
    }

    fullScreenImageItem?.let { item ->
        FullScreenImageDialog(
            item = item,
            onDismiss = { fullScreenImageItem = null },
            onSaveToGallery = { onSaveImageToGallery(item) }
        )
    }

    pickDialogItem?.let { item ->
        PickWishDialog(
            wishTitle = item.title,
            onDismiss = { pickDialogItem = null },
            onConfirm = { name, isSecret, msg ->
                pickDialogItem = null
                onSetItemPicked(item.id, true, name, isSecret, msg)
            }
        )
    }

    moveDialogItem?.let { item ->
        MoveItemDialog(
            wishTitle = item.title,
            currentWishlistId = wishlist.id,
            wishlists = allWishlists,
            onDismiss = { moveDialogItem = null },
            onSelectTarget = { targetId ->
                moveDialogItem = null
                onMoveItem(item.id, targetId)
            }
        )
    }

    if (showEditDialog) {
        CreateWishlistDialog(
            initialName = wishlist.name,
            initialDesc = wishlist.description,
            initialEmoji = wishlist.emoji,
            titleText = "Edit Wishlist",
            confirmLabel = "Save",
            onDismiss = { showEditDialog = false },
            onConfirm = { name, desc, emoji ->
                showEditDialog = false
                onEditWishlist(name, desc, emoji)
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = CyberGlassTokens.GlassSurface,
            title = { Text("Delete '${wishlist.name}'?", color = CyberGlassTokens.TextPrimary) },
            text = {
                Text(
                    "This deletes all wishes in this folder and unpublishes its public web link.",
                    color = CyberGlassTokens.TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteWishlist()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = CyberGlassTokens.NeonRose)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = CyberGlassTokens.TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun WishItemCard(
    item: WishItemEntity,
    canMove: Boolean,
    onOpenLink: () -> Unit,
    onCopyText: () -> Unit,
    onViewImage: () -> Unit,
    onSaveImage: () -> Unit,
    onPickOrUnpick: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    val imageModel: Any? = remember(item.id, item.localImagePath, item.remoteImageUrl) {
        when {
            item.localImagePath.isNotBlank() && File(item.localImagePath).exists() -> File(item.localImagePath)
            item.remoteImageUrl.isNotBlank() -> item.remoteImageUrl
            else -> null
        }
    }

    CasioGlassCard(
        accentBorderColor = if (item.isPicked) CyberGlassTokens.MintEmerald else null,
        contentPadding = 14.dp
    ) {
        if (imageModel != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberGlassTokens.BgDeepSpace)
                    .clickable(onClick = onViewImage)
            ) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(CyberGlassTokens.BgDeepSpace.copy(alpha = 0.8f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Tap for full screen", color = CyberGlassTokens.TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Badges row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val typeTag = when (item.itemType) {
                WishItemType.IMAGE.name -> "🖼️ Photo"
                WishItemType.LINK.name -> "🔗 Link"
                else -> "📝 Note"
            }
            SmallTagPill(text = typeTag, color = CyberGlassTokens.IndigoGlow)
            if (item.storeDomain.isNotBlank()) {
                SmallTagPill(text = item.storeDomain, color = CyberGlassTokens.ElectricCyan)
            }
            if (item.priceText.isNotBlank()) {
                SmallTagPill(text = item.priceText, color = CyberGlassTokens.WarningAmber)
            }
            if (item.priority >= 2) {
                SmallTagPill(text = "⭐ Must", color = CyberGlassTokens.NeonRose)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.title,
            color = CyberGlassTokens.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (item.noteText.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberGlassTokens.BgDeepSpace.copy(alpha = 0.45f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = item.noteText,
                    color = CyberGlassTokens.TextSecondary,
                    fontSize = 12.5.sp,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (item.isPicked) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberGlassTokens.MintEmerald.copy(alpha = 0.14f))
                    .border(1.dp, CyberGlassTokens.MintEmerald.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Column {
                    Text(
                        text = "✅ Picked by ${item.pickedByName.ifBlank { "Secret Surprise 🤫" }}",
                        color = CyberGlassTokens.MintEmerald,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (item.pickedMessage.isNotBlank()) {
                        Text(
                            text = "\"${item.pickedMessage}\"",
                            color = CyberGlassTokens.TextSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (item.linkUrl.isNotBlank()) {
                ActionPillButton(
                    icon = Icons.Default.OpenInBrowser,
                    label = "Open",
                    tint = CyberGlassTokens.ElectricCyan,
                    onClick = onOpenLink
                )
            }
            ActionPillButton(
                icon = Icons.Default.ContentCopy,
                label = if (item.linkUrl.isNotBlank() && item.noteText.isBlank()) "Copy Link" else "Copy",
                tint = CyberGlassTokens.TextPrimary,
                onClick = onCopyText
            )
            if (imageModel != null) {
                ActionPillButton(
                    icon = Icons.Default.Download,
                    label = "Save",
                    tint = CyberGlassTokens.TextPrimary,
                    onClick = onSaveImage
                )
            }
            ActionPillButton(
                icon = Icons.Default.CardGiftcard,
                label = if (item.isPicked) "Unpick" else "Pick",
                tint = if (item.isPicked) CyberGlassTokens.WarningAmber else CyberGlassTokens.MintEmerald,
                onClick = onPickOrUnpick
            )
            if (canMove) {
                ActionPillButton(
                    icon = Icons.Default.DriveFileMove,
                    label = "Move",
                    tint = CyberGlassTokens.TextSecondary,
                    onClick = onMove
                )
            }
            ActionPillButton(
                icon = Icons.Default.Delete,
                label = "Remove",
                tint = CyberGlassTokens.NeonRose,
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun SmallTagPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.42f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = text, color = color, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun ActionPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.12f))
            .border(BorderStroke(1.dp, tint.copy(alpha = 0.35f)), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(text = label, color = tint, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}
