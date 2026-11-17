package com.panda.wishlist.ui.screens.add

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.panda.wishlist.core.database.entity.WishItemType
import com.panda.wishlist.core.database.entity.WishlistEntity
import com.panda.wishlist.core.share.LinkMetadata
import com.panda.wishlist.core.share.ShareContentAnalyzer
import com.panda.wishlist.ui.components.CasioGlassCard
import com.panda.wishlist.ui.components.CasioGlassFilterChip
import com.panda.wishlist.ui.components.glassPrimaryButtonColors
import com.panda.wishlist.ui.components.glassSecondaryButtonColors
import com.panda.wishlist.ui.components.glassTextFieldColors
import com.panda.wishlist.ui.theme.CyberGlassTokens
import kotlinx.coroutines.launch

@Composable
fun QuickAddScreen(
    wishlists: List<WishlistEntity>,
    defaultWishlistId: String,
    onFetchMetadata: suspend (String) -> LinkMetadata,
    onSaveWish: (
        wishlistId: String,
        itemType: WishItemType,
        title: String,
        noteText: String,
        linkUrl: String,
        priceText: String,
        priority: Int,
        imageUris: List<Uri>,
        remoteImageUrl: String,
        publishAndShare: Boolean
    ) -> Unit,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedType by remember { mutableStateOf(WishItemType.LINK) }
    var selectedWishlistId by remember(defaultWishlistId, wishlists) {
        mutableStateOf(
            wishlists.find { it.id == defaultWishlistId }?.id
                ?: wishlists.firstOrNull()?.id.orEmpty()
        )
    }
    var title by remember { mutableStateOf("") }
    var linkUrl by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var priority by remember { mutableIntStateOf(1) }
    var remoteImageUrl by remember { mutableStateOf("") }
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isFetchingMeta by remember { mutableStateOf(false) }

    LaunchedEffect(wishlists) {
        if (selectedWishlistId.isBlank() && wishlists.isNotEmpty()) {
            selectedWishlistId = wishlists.first().id
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 8)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris = uris
            if (title.isBlank()) {
                title = if (uris.size > 1) "Wish Photos (${uris.size})" else "Wish Photo"
            }
        }
    }

    fun fetchLinkDetails(rawInput: String) {
        val clean = ShareContentAnalyzer.extractFirstUrl(rawInput).ifBlank { rawInput.trim() }
        if (clean.isBlank()) {
            onShowToast("Paste a product or web link first")
            return
        }
        linkUrl = clean
        val inlinePrice = ShareContentAnalyzer.extractPrice(rawInput)
        if (priceText.isBlank() && inlinePrice.isNotBlank()) priceText = inlinePrice

        scope.launch {
            isFetchingMeta = true
            val meta = onFetchMetadata(clean)
            isFetchingMeta = false
            if (meta.title.isNotBlank() && title.isBlank()) title = meta.title
            if (meta.description.isNotBlank() && noteText.isBlank()) noteText = meta.description
            if (meta.priceText.isNotBlank() && priceText.isBlank()) priceText = meta.priceText
            if (meta.imageUrl.isNotBlank()) remoteImageUrl = meta.imageUrl
            if (meta.title.isNotBlank() || meta.imageUrl.isNotBlank()) {
                onShowToast("Fetched product preview!")
            } else {
                onShowToast("Link ready! Enter a title if you like.")
            }
        }
    }

    fun resetForm() {
        title = ""
        linkUrl = ""
        noteText = ""
        priceText = ""
        remoteImageUrl = ""
        selectedUris = emptyList()
        priority = 1
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Add a New Wish",
            color = CyberGlassTokens.TextPrimary,
            fontSize = 21.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Paste links (Amazon, Flipkart, etc.), pick photos, or write notes — or share directly from any app!",
            color = CyberGlassTokens.TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Type Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CasioGlassFilterChip(
                text = "🔗 Link",
                selected = selectedType == WishItemType.LINK,
                onClick = { selectedType = WishItemType.LINK },
                modifier = Modifier.weight(1f)
            )
            CasioGlassFilterChip(
                text = "🖼️ Photos",
                selected = selectedType == WishItemType.IMAGE,
                onClick = { selectedType = WishItemType.IMAGE },
                modifier = Modifier.weight(1f)
            )
            CasioGlassFilterChip(
                text = "📝 Text",
                selected = selectedType == WishItemType.TEXT,
                onClick = { selectedType = WishItemType.TEXT },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        CasioGlassCard {
            // Folder picker
            Text(
                text = "Target Wishlist Folder",
                color = CyberGlassTokens.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                wishlists.forEach { folder ->
                    CasioGlassFilterChip(
                        text = "${folder.emoji} ${folder.name}",
                        selected = selectedWishlistId == folder.id,
                        onClick = { selectedWishlistId = folder.id }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedType == WishItemType.LINK) {
                OutlinedTextField(
                    value = linkUrl,
                    onValueChange = { linkUrl = it },
                    label = { Text("Product or Post Link") },
                    placeholder = { Text("https://amzn.in/... or flipkart.com/...") },
                    maxLines = 2,
                    colors = glassTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val pasted = cm.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                            if (pasted.isNotBlank()) {
                                fetchLinkDetails(pasted)
                            } else {
                                onShowToast("Clipboard is empty")
                            }
                        },
                        colors = glassSecondaryButtonColors(),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste Link", fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                    val canFetch = !isFetchingMeta && linkUrl.isNotBlank()
                    OutlinedButton(
                        onClick = { fetchLinkDetails(linkUrl) },
                        enabled = canFetch,
                        colors = glassSecondaryButtonColors(),
                        border = BorderStroke(
                            1.dp,
                            if (canFetch) CyberGlassTokens.ElectricCyan.copy(alpha = 0.65f)
                            else Color.White.copy(alpha = 0.16f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isFetchingMeta) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = CyberGlassTokens.ElectricCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fetching...", maxLines = 1)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Auto-Fetch", fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }

                if (remoteImageUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    AsyncImage(
                        model = remoteImageUrl,
                        contentDescription = "Fetched preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (selectedType == WishItemType.IMAGE) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CyberGlassTokens.ElectricCyan.copy(alpha = 0.12f))
                        .border(1.dp, CyberGlassTokens.ElectricCyan.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = CyberGlassTokens.ElectricCyan, modifier = Modifier.size(30.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (selectedUris.isEmpty()) "Tap to Pick Photos from Gallery" else "${selectedUris.size} Photo(s) Selected — Tap to Change",
                            color = CyberGlassTokens.ElectricCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (selectedUris.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedUris.forEach { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(if (selectedType == WishItemType.TEXT) "Wish Title" else "Title (optional)") },
                placeholder = { Text("e.g. Sony Headphones, Birthday Cake Idea") },
                maxLines = 2,
                colors = glassTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("Estimated Price (optional)") },
                placeholder = { Text("₹2,999") },
                singleLine = true,
                colors = glassTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text(if (selectedType == WishItemType.TEXT) "Wish Text / Details" else "Note (color, size, remarks)") },
                minLines = if (selectedType == WishItemType.TEXT) 3 else 2,
                maxLines = 5,
                colors = glassTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))
            Text("Priority", color = CyberGlassTokens.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CasioGlassFilterChip(text = "Nice", selected = priority == 0, onClick = { priority = 0 }, modifier = Modifier.weight(1f))
                CasioGlassFilterChip(text = "Love It", selected = priority == 1, onClick = { priority = 1 }, modifier = Modifier.weight(1f))
                CasioGlassFilterChip(text = "⭐ Must", selected = priority == 2, selectedColor = CyberGlassTokens.NeonRose, onClick = { priority = 2 }, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            val canSave = selectedWishlistId.isNotBlank() && (
                (selectedType == WishItemType.LINK && linkUrl.isNotBlank()) ||
                    (selectedType == WishItemType.IMAGE && selectedUris.isNotEmpty()) ||
                    (selectedType == WishItemType.TEXT && (title.isNotBlank() || noteText.isNotBlank()))
                )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val finalTitle = title.ifBlank {
                            if (selectedType == WishItemType.LINK) "${ShareContentAnalyzer.detectStoreName(linkUrl)} Wish" else "Saved Wish"
                        }
                        onSaveWish(
                            selectedWishlistId,
                            selectedType,
                            finalTitle,
                            noteText,
                            linkUrl,
                            priceText,
                            priority,
                            selectedUris,
                            remoteImageUrl,
                            false
                        )
                        resetForm()
                    },
                    enabled = canSave,
                    colors = glassSecondaryButtonColors(),
                    border = BorderStroke(
                        1.dp,
                        if (canSave) CyberGlassTokens.ElectricCyan.copy(alpha = 0.65f)
                        else Color.White.copy(alpha = 0.16f)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save", fontWeight = FontWeight.Bold, maxLines = 1)
                }

                Button(
                    onClick = {
                        val finalTitle = title.ifBlank {
                            if (selectedType == WishItemType.LINK) "${ShareContentAnalyzer.detectStoreName(linkUrl)} Wish" else "Saved Wish"
                        }
                        onSaveWish(
                            selectedWishlistId,
                            selectedType,
                            finalTitle,
                            noteText,
                            linkUrl,
                            priceText,
                            priority,
                            selectedUris,
                            remoteImageUrl,
                            true
                        )
                        resetForm()
                    },
                    enabled = canSave,
                    colors = glassPrimaryButtonColors(),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & Share", fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
