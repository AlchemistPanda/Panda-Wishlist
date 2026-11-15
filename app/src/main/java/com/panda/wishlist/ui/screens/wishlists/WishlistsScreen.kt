package com.panda.wishlist.ui.screens.wishlists

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.panda.wishlist.R
import com.panda.wishlist.core.database.entity.WishlistEntity
import com.panda.wishlist.core.database.entity.WishlistWithStats
import com.panda.wishlist.ui.components.CasioGlassCard
import com.panda.wishlist.ui.components.CreateWishlistDialog
import com.panda.wishlist.ui.components.glassTextFieldColors
import com.panda.wishlist.ui.theme.CyberGlassTokens

@Composable
fun WishlistsScreen(
    wishlists: List<WishlistWithStats>,
    syncingWishlistId: String?,
    onOpenWishlist: (String) -> Unit,
    onCreateWishlist: (name: String, desc: String, emoji: String) -> Unit,
    onSyncAndShare: (WishlistEntity) -> Unit,
    onShowQr: (WishlistEntity) -> Unit,
    onRefreshClaims: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val filtered = remember(wishlists, searchQuery) {
        if (searchQuery.isBlank()) wishlists
        else wishlists.filter {
            it.wishlist.name.contains(searchQuery, ignoreCase = true) ||
                it.wishlist.description.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalWishes = wishlists.sumOf { it.itemCount }
    val totalPicked = wishlists.sumOf { it.pickedCount }
    val sharedFolders = wishlists.count { it.wishlist.isPublicSynced }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.panda_gift_hero),
                contentDescription = "Panda Wishlist",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, CyberGlassTokens.ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Panda Wishlist",
                    color = CyberGlassTokens.TextPrimary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Share links, photos & ideas cleanly",
                    color = CyberGlassTokens.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = onRefreshClaims,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberGlassTokens.GlassSlate.copy(alpha = 0.7f))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh Picks",
                    tint = CyberGlassTokens.ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(CyberGlassTokens.DeepCyan, CyberGlassTokens.ElectricCyan)
                        )
                    )
                    .clickable { showCreateDialog = true }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = "New Folder", tint = CyberGlassTokens.BgDeepSpace, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New", color = CyberGlassTokens.BgDeepSpace, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3-Column Summary Metrics Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryMetricTile(
                value = totalWishes.toString(),
                label = "WISHES",
                accent = CyberGlassTokens.ElectricCyan,
                modifier = Modifier.weight(1f)
            )
            SummaryMetricTile(
                value = sharedFolders.toString(),
                label = "PUBLIC",
                accent = CyberGlassTokens.IndigoGlow,
                modifier = Modifier.weight(1f)
            )
            SummaryMetricTile(
                value = totalPicked.toString(),
                label = "PICKED",
                accent = CyberGlassTokens.MintEmerald,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search wishlist folders...", fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = CyberGlassTokens.TextTertiary, modifier = Modifier.size(18.dp))
            },
            singleLine = true,
            colors = glassTextFieldColors(),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            CasioGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🐼🎁", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (wishlists.isEmpty()) "No wishlists yet" else "No matching folders",
                        color = CyberGlassTokens.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap '+ New' to create a wishlist folder, or share anything from WhatsApp, Amazon, Instagram, or Gallery!",
                        color = CyberGlassTokens.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.wishlist.id }) { stats ->
                    WishlistFolderCard(
                        stats = stats,
                        isSyncing = syncingWishlistId == stats.wishlist.id,
                        onOpen = { onOpenWishlist(stats.wishlist.id) },
                        onSyncAndShare = { onSyncAndShare(stats.wishlist) },
                        onShowQr = { onShowQr(stats.wishlist) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateWishlistDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, desc, emoji ->
                showCreateDialog = false
                onCreateWishlist(name, desc, emoji)
            }
        )
    }
}

@Composable
private fun SummaryMetricTile(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    CasioGlassCard(
        modifier = modifier,
        contentPadding = 10.dp,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = accent,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = CyberGlassTokens.TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WishlistFolderCard(
    stats: WishlistWithStats,
    isSyncing: Boolean,
    onOpen: () -> Unit,
    onSyncAndShare: () -> Unit,
    onShowQr: () -> Unit
) {
    val wishlist = stats.wishlist
    CasioGlassCard(
        accentBorderColor = if (wishlist.isPublicSynced) CyberGlassTokens.MintEmerald else null,
        onClick = onOpen
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(CyberGlassTokens.ElectricCyan.copy(alpha = 0.14f))
                    .border(1.dp, CyberGlassTokens.ElectricCyan.copy(alpha = 0.35f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = wishlist.emoji, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = wishlist.name,
                        color = CyberGlassTokens.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (wishlist.isPublicSynced) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(CyberGlassTokens.MintEmerald.copy(alpha = 0.16f))
                                .border(1.dp, CyberGlassTokens.MintEmerald.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudDone, contentDescription = null, tint = CyberGlassTokens.MintEmerald, modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("LIVE", color = CyberGlassTokens.MintEmerald, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }
                if (wishlist.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = wishlist.description,
                        color = CyberGlassTokens.TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${stats.itemCount} wish${if (stats.itemCount == 1) "" else "es"} • ${stats.pickedCount} picked",
                    color = CyberGlassTokens.ElectricCyan,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberGlassTokens.ElectricCyan.copy(alpha = 0.16f))
                    .border(BorderStroke(1.dp, CyberGlassTokens.ElectricCyan.copy(alpha = 0.45f)), RoundedCornerShape(10.dp))
                    .clickable(enabled = !isSyncing, onClick = onSyncAndShare),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = CyberGlassTokens.ElectricCyan
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Syncing...", color = CyberGlassTokens.ElectricCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            if (wishlist.isPublicSynced) Icons.Default.Share else Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = CyberGlassTokens.ElectricCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (wishlist.isPublicSynced) "Sync & Share Link" else "Publish & Share",
                            color = CyberGlassTokens.ElectricCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            if (wishlist.isPublicSynced) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberGlassTokens.GlassSlate.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onShowQr),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = "QR Code", tint = CyberGlassTokens.TextPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
