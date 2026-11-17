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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import kotlinx.coroutines.delay
import java.io.File

private const val AUTO_SAVE_DURATION_MS = 3000

@Composable
fun IncomingShareSheet(
    pending: PendingIncomingShare,
    wishlists: List<WishlistEntity>,
    defaultWishlistId: String,
    autoSaveEnabled: Boolean = true,
    useDialog: Boolean = true,
    isSaving: Boolean = false,
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
    var userPickedFolder by remember { mutableStateOf(false) }
    var selectedWishlistId by remember {
        mutableStateOf(
            wishlists.find { it.id == defaultWishlistId }?.id
                ?: wishlists.firstOrNull()?.id.orEmpty()
        )
    }
    LaunchedEffect(wishlists, defaultWishlistId) {
        if (!userPickedFolder || wishlists.none { it.id == selectedWishlistId }) {
            selectedWishlistId = wishlists.find { it.id == defaultWishlistId }?.id
                ?: wishlists.firstOrNull()?.id.orEmpty()
        }
    }

    // Fields start from the share and pick up fetched page details (title, price, note)
    // when they arrive, but never overwrite something the user has already typed.
    var title by remember { mutableStateOf(pending.payload.title) }
    var noteText by remember { mutableStateOf(pending.payload.noteText) }
    var linkUrl by remember { mutableStateOf(pending.payload.linkUrl) }
    var priceText by remember { mutableStateOf(pending.payload.priceText) }
    var titleEdited by remember { mutableStateOf(false) }
    var noteEdited by remember { mutableStateOf(false) }
    var priceEdited by remember { mutableStateOf(false) }
    LaunchedEffect(pending.payload) {
        if (!titleEdited) title = pending.payload.title
        if (!noteEdited) noteText = pending.payload.noteText
        if (!priceEdited) priceText = pending.payload.priceText
    }
    var priority by remember { mutableIntStateOf(1) }

    var autoSaveCancelled by remember { mutableStateOf(false) }
    var autoSaveTriggered by remember { mutableStateOf(false) }
    var remainingMs by remember { mutableIntStateOf(AUTO_SAVE_DURATION_MS) }
    var countdownResetTrigger by remember { mutableIntStateOf(0) }

    val currentWishlistId by rememberUpdatedState(selectedWishlistId)
    val currentTitle by rememberUpdatedState(title)
    val currentNoteText by rememberUpdatedState(noteText)
    val currentLinkUrl by rememberUpdatedState(linkUrl)
    val currentPriceText by rememberUpdatedState(priceText)
    val currentPriority by rememberUpdatedState(priority)
    val currentOnSave by rememberUpdatedState(onSave)

    fun cancelAutoSave() {
        autoSaveCancelled = true
    }

    val canAutoSave = autoSaveEnabled && !autoSaveCancelled && !autoSaveTriggered && selectedWishlistId.isNotBlank()

    LaunchedEffect(
        canAutoSave,
        pending.isFetchingMeta,
        countdownResetTrigger
    ) {
        if (canAutoSave && !pending.isFetchingMeta) {
            remainingMs = AUTO_SAVE_DURATION_MS
            while (remainingMs > 0) {
                delay(50L)
                remainingMs = (remainingMs - 50).coerceAtLeast(0)
            }
            if (!autoSaveCancelled && !autoSaveTriggered && currentWishlistId.isNotBlank()) {
                autoSaveTriggered = true
                currentOnSave(
                    currentWishlistId,
                    currentTitle,
                    currentNoteText,
                    currentLinkUrl,
                    currentPriceText,
                    currentPriority,
                    false
                )
            }
        }
    }

    val selectedFolderName = remember(selectedWishlistId, wishlists) {
        wishlists.find { it.id == selectedWishlistId }?.let { "${it.emoji} ${it.name}" } ?: "Wishlist"
    }

    val cardContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            CasioGlassCard(
                accentBorderColor = CyberGlassTokens.ElectricCyan,
                isModal = true
            ) {
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
                        IconButton(
                            onClick = {
                                cancelAutoSave()
                                onDismiss()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = CyberGlassTokens.TextSecondary)
                        }
                    }

                    if (canAutoSave) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val secondsLeft = ((remainingMs + 999) / 1000).coerceIn(1, 3)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyberGlassTokens.BgDeepSpace.copy(alpha = 0.88f))
                                .border(1.dp, CyberGlassTokens.ElectricCyan.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (pending.isFetchingMeta) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = CyberGlassTokens.ElectricCyan
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = if (pending.isFetchingMeta) {
                                            "Fetching preview… auto-save ready after"
                                        } else {
                                            "Auto-saving to $selectedFolderName in ${secondsLeft}s"
                                        },
                                        color = CyberGlassTokens.TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CyberGlassTokens.NeonRose.copy(alpha = 0.22f))
                                        .border(1.dp, CyberGlassTokens.NeonRose.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                        .clickable { cancelAutoSave() }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "Cancel",
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (pending.isFetchingMeta) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = CyberGlassTokens.ElectricCyan,
                                    trackColor = Color.White.copy(alpha = 0.14f)
                                )
                            } else {
                                LinearProgressIndicator(
                                    progress = { (remainingMs.toFloat() / AUTO_SAVE_DURATION_MS.toFloat()).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = CyberGlassTokens.ElectricCyan,
                                    trackColor = Color.White.copy(alpha = 0.14f)
                                )
                            }
                        }
                    } else if (pending.isFetchingMeta) {
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
                                onClick = {
                                    userPickedFolder = true
                                    selectedWishlistId = folder.id
                                    cancelAutoSave()
                                }
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyberGlassTokens.MintEmerald.copy(alpha = 0.16f))
                                .border(1.dp, CyberGlassTokens.MintEmerald.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable {
                                    cancelAutoSave()
                                    onCreateNewFolder()
                                }
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
                        onValueChange = {
                            title = it
                            titleEdited = true
                            cancelAutoSave()
                        },
                        label = { Text("Wish Title") },
                        maxLines = 2,
                        trailingIcon = if (title.isNotEmpty()) {
                            {
                                IconButton(
                                    onClick = {
                                        title = ""
                                        titleEdited = true
                                        cancelAutoSave()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear title",
                                        tint = CyberGlassTokens.TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else null,
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
                            onValueChange = {
                                priceText = it
                                priceEdited = true
                                cancelAutoSave()
                            },
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
                            onValueChange = {
                                linkUrl = it
                                cancelAutoSave()
                            },
                            label = { Text("Link URL") },
                            singleLine = true,
                            colors = glassTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = {
                                noteText = it
                                noteEdited = true
                                cancelAutoSave()
                            },
                            label = { Text("Note / Text") },
                            maxLines = 3,
                            colors = glassTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                        if (noteText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    noteText = ""
                                    noteEdited = true
                                    if (autoSaveEnabled && !autoSaveCancelled && !autoSaveTriggered) {
                                        countdownResetTrigger++
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CyberGlassTokens.GlassSurface.copy(alpha = 0.92f))
                                    .border(1.dp, Color.White.copy(alpha = 0.24f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear shared text",
                                    tint = CyberGlassTokens.TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

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
                            onClick = {
                                priority = 0
                                cancelAutoSave()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CasioGlassFilterChip(
                            text = "Love It",
                            selected = priority == 1,
                            onClick = {
                                priority = 1
                                cancelAutoSave()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CasioGlassFilterChip(
                            text = "⭐ Must",
                            selected = priority == 2,
                            selectedColor = CyberGlassTokens.NeonRose,
                            onClick = {
                                priority = 2
                                cancelAutoSave()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val canSave = selectedWishlistId.isNotBlank() && !isSaving
                        OutlinedButton(
                            onClick = {
                                if (canSave) {
                                    cancelAutoSave()
                                    onSave(selectedWishlistId, title, noteText, linkUrl, priceText, priority, false)
                                }
                            },
                            enabled = canSave,
                            colors = glassSecondaryButtonColors(),
                            border = BorderStroke(
                                1.dp,
                                if (canSave) CyberGlassTokens.ElectricCyan.copy(alpha = 0.7f)
                                else Color.White.copy(alpha = 0.16f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        Button(
                            onClick = {
                                if (canSave) {
                                    cancelAutoSave()
                                    onSave(selectedWishlistId, title, noteText, linkUrl, priceText, priority, true)
                                }
                            },
                            enabled = canSave,
                            colors = glassPrimaryButtonColors(),
                            modifier = Modifier.weight(1.35f)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sharing...", fontWeight = FontWeight.Bold, maxLines = 1)
                            } else {
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

    if (useDialog) {
        Dialog(
            onDismissRequest = {
                cancelAutoSave()
                onDismiss()
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            cardContent()
        }
    } else {
        cardContent()
    }
}
