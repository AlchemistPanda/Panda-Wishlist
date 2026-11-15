package com.panda.wishlist.ui.screens.shared

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.panda.wishlist.core.database.entity.WishItemEntity
import com.panda.wishlist.core.database.entity.WishlistEntity
import com.panda.wishlist.core.database.entity.WishlistWithStats
import com.panda.wishlist.ui.components.CasioGlassCard
import com.panda.wishlist.ui.theme.CyberGlassTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SharedHubScreen(
    wishlists: List<WishlistWithStats>,
    pickedItems: List<WishItemEntity>,
    syncingWishlistId: String?,
    onOpenWishlist: (String) -> Unit,
    onSyncAndShare: (WishlistEntity) -> Unit,
    onShowQr: (WishlistEntity) -> Unit,
    onUnpublish: (String) -> Unit,
    onRefreshAllClaims: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    val publicLists = wishlists.filter { it.wishlist.isPublicSynced }
    val listNameMap = wishlists.associate { it.wishlist.id to "${it.wishlist.emoji} ${it.wishlist.name}" }
    val timeFmt = SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Public Shared Hub",
                        color = CyberGlassTokens.TextPrimary,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Manage live web links & see wishes picked by friends",
                        color = CyberGlassTokens.TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onRefreshAllClaims,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberGlassTokens.MintEmerald.copy(alpha = 0.16f))
                        .border(1.dp, CyberGlassTokens.MintEmerald.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Check Friend Picks", tint = CyberGlassTokens.MintEmerald, modifier = Modifier.size(18.dp))
                }
            }
        }

        item {
            Text(
                text = "ACTIVE PUBLIC WISHLISTS (${publicLists.size})",
                color = CyberGlassTokens.ElectricCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )
        }

        if (publicLists.isEmpty()) {
            item {
                CasioGlassCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🌐🎁", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No wishlists published yet",
                            color = CyberGlassTokens.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Publish & Share' on any wishlist folder to get a public GitHub Pages web link anyone can open.",
                            color = CyberGlassTokens.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(publicLists, key = { it.wishlist.id }) { stats ->
                val wl = stats.wishlist
                val shareUrl = wl.publicShareUrl.ifBlank {
                    "https://alchemistpanda.github.io/Panda-Wishlist/?id=${wl.id}"
                }
                val isSyncing = syncingWishlistId == wl.id

                CasioGlassCard(
                    accentBorderColor = CyberGlassTokens.MintEmerald,
                    onClick = { onOpenWishlist(wl.id) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(wl.emoji, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = wl.name,
                                color = CyberGlassTokens.TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${stats.itemCount} wishes • ${stats.pickedCount} picked" +
                                    if (wl.lastSyncedAt > 0) " • Synced ${timeFmt.format(Date(wl.lastSyncedAt))}" else "",
                                color = CyberGlassTokens.TextSecondary,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = shareUrl,
                        color = CyberGlassTokens.ElectricCyan,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmallHubAction(
                            icon = Icons.Default.Share,
                            label = if (isSyncing) "Syncing..." else "Sync & Share",
                            tint = CyberGlassTokens.ElectricCyan,
                            onClick = { if (!isSyncing) onSyncAndShare(wl) },
                            loading = isSyncing
                        )
                        SmallHubAction(
                            icon = Icons.Default.ContentCopy,
                            label = "Copy Link",
                            tint = CyberGlassTokens.TextPrimary,
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Wishlist Link", shareUrl))
                                onShowToast("Public link copied!")
                            }
                        )
                        SmallHubAction(
                            icon = Icons.Default.QrCode2,
                            label = "QR",
                            tint = CyberGlassTokens.TextPrimary,
                            onClick = { onShowQr(wl) }
                        )
                        SmallHubAction(
                            icon = Icons.Default.OpenInBrowser,
                            label = "Open Web",
                            tint = CyberGlassTokens.MintEmerald,
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(shareUrl)))
                                }
                            }
                        )
                        SmallHubAction(
                            icon = Icons.Default.CloudOff,
                            label = "Unpublish",
                            tint = CyberGlassTokens.NeonRose,
                            onClick = { onUnpublish(wl.id) }
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "PICKED BY FRIENDS (${pickedItems.size})",
                color = CyberGlassTokens.MintEmerald,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )
        }

        if (pickedItems.isEmpty()) {
            item {
                CasioGlassCard {
                    Text(
                        text = "No wishes have been picked yet. When friends open your link and tap 'Pick this Wish 🎁', they appear here!",
                        color = CyberGlassTokens.TextSecondary,
                        fontSize = 12.5.sp
                    )
                }
            }
        } else {
            items(pickedItems, key = { it.id }) { item ->
                CasioGlassCard(
                    accentBorderColor = CyberGlassTokens.MintEmerald,
                    contentPadding = 13.dp,
                    onClick = { onOpenWishlist(item.wishlistId) }
                ) {
                    Text(
                        text = listNameMap[item.wishlistId] ?: "Wishlist",
                        color = CyberGlassTokens.ElectricCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.title,
                        color = CyberGlassTokens.TextPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "🎁 Picked by ${item.pickedByName.ifBlank { "Secret Surprise 🤫" }}",
                        color = CyberGlassTokens.MintEmerald,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (item.pickedMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "\"${item.pickedMessage}\"",
                            color = CyberGlassTokens.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallHubAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    loading: Boolean = false
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.13f))
            .border(BorderStroke(1.dp, tint.copy(alpha = 0.36f)), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(13.dp), strokeWidth = 2.dp, color = tint)
            } else {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.width(5.dp))
            Text(label, color = tint, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}
