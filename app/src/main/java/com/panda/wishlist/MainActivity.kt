package com.panda.wishlist

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.panda.wishlist.ui.MainViewModel
import com.panda.wishlist.ui.components.AmbientGlassBackground
import com.panda.wishlist.ui.components.CreateWishlistDialog
import com.panda.wishlist.ui.components.IncomingShareSheet
import com.panda.wishlist.ui.components.ShareQrDialog
import com.panda.wishlist.ui.screens.add.QuickAddScreen
import com.panda.wishlist.ui.screens.settings.SettingsScreen
import com.panda.wishlist.ui.screens.shared.SharedHubScreen
import com.panda.wishlist.ui.screens.wishlists.WishlistDetailScreen
import com.panda.wishlist.ui.screens.wishlists.WishlistsScreen
import com.panda.wishlist.ui.theme.CyberGlassTokens
import com.panda.wishlist.ui.theme.PandaWishlistTheme
import dagger.hilt.android.AndroidEntryPoint

enum class WishlistTab(val title: String, val icon: ImageVector) {
    WISHLISTS("Wishlists", Icons.Default.CardGiftcard),
    ADD("Add Wish", Icons.Default.AddCircle),
    SHARED("Shared", Icons.Default.Public),
    SETTINGS("Settings", Icons.Default.Tune)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            viewModel.handleIncomingIntent(intent)
        }
        setContent {
            PandaWishlistTheme {
                PandaWishlistScaffold(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.handleIncomingIntent(intent)
    }
}

@Composable
private fun PandaWishlistScaffold(viewModel: MainViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(WishlistTab.WISHLISTS) }
    var showInlineCreateFolder by remember { mutableStateOf(false) }

    val wishlistsWithStats by viewModel.wishlistsWithStats.collectAsState()
    val allWishlists by viewModel.allWishlists.collectAsState()
    val pickedItems by viewModel.pickedItems.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val selectedWishlistId by viewModel.selectedWishlistId.collectAsState()
    val pendingShare by viewModel.pendingShare.collectAsState()
    val syncingId by viewModel.syncingWishlistId.collectAsState()
    val qrDialogWishlist by viewModel.qrDialogWishlist.collectAsState()
    val toastMsg by viewModel.toastMessage.collectAsState()
    val connectedUser by viewModel.connectedGithubUser.collectAsState()
    val startupUpdate by viewModel.startupUpdate.collectAsState()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
    val debugLogs by viewModel.debugLogs.collectAsState()

    LaunchedEffect(toastMsg) {
        toastMsg?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    fun launchSystemShare(wishlistName: String, url: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Check out my '$wishlistName' wishlist on Panda Wishlist 🐼🎁\n$url"
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Public Wishlist"))
    }

    AmbientGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (startupUpdate != null && settings.autoUpdateCheckEnabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberGlassTokens.MintEmerald.copy(alpha = 0.18f))
                            .border(
                                BorderStroke(1.dp, CyberGlassTokens.MintEmerald.copy(alpha = 0.6f)),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                val url = startupUpdate?.downloadUrl.orEmpty()
                                if (url.isNotBlank()) {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Update available: v${startupUpdate!!.latestVersion} — Tap to download",
                                color = CyberGlassTokens.MintEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = CyberGlassTokens.MintEmerald,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { viewModel.dismissUpdateBanner() }
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.statusBarsPadding())
                }
            },
            bottomBar = {
                if (selectedWishlistId == null || currentTab != WishlistTab.WISHLISTS) {
                    CyberGlassBottomBar(
                        currentTab = currentTab,
                        onTabSelected = { tab ->
                            currentTab = tab
                            if (tab != WishlistTab.WISHLISTS) {
                                viewModel.closeWishlistDetail()
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            // System Back on Add / Shared / Settings returns to Wishlists instead of closing the app.
            BackHandler(enabled = currentTab != WishlistTab.WISHLISTS) {
                currentTab = WishlistTab.WISHLISTS
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    WishlistTab.WISHLISTS -> {
                        val detailId = selectedWishlistId
                        if (detailId != null) key(detailId) {
                            // Flows are remembered per wishlist: building them inline restarted the
                            // queries on every recomposition. The first frame is always null while
                            // Room loads, so that must not be read as "wishlist deleted" (deleting
                            // a wishlist already clears the selection in the view model).
                            val activeWishlist by remember { viewModel.observeWishlist(detailId) }
                                .collectAsState(initial = null)
                            val detailItems by remember { viewModel.observeItemsForWishlist(detailId) }
                                .collectAsState(initial = emptyList())
                            val wishlistNow = activeWishlist
                            if (wishlistNow != null) {
                                val activeWishlist = wishlistNow
                                WishlistDetailScreen(
                                    wishlist = activeWishlist,
                                    items = detailItems,
                                    allWishlists = allWishlists,
                                    isSyncing = syncingId == activeWishlist.id,
                                    onBack = { viewModel.closeWishlistDetail() },
                                    onSyncAndShare = {
                                        viewModel.syncWishlist(activeWishlist.id) { name, url ->
                                            launchSystemShare(name, url)
                                        }
                                    },
                                    onShowQr = { viewModel.openQrDialog(activeWishlist) },
                                    onRefreshClaims = { viewModel.refreshAllClaims() },
                                    onEditWishlist = { name, desc, emoji ->
                                        viewModel.updateWishlistInfo(activeWishlist.id, name, desc, emoji)
                                    },
                                    onDeleteWishlist = { viewModel.deleteWishlist(activeWishlist.id) },
                                    onDeleteItem = { viewModel.deleteWishItem(it) },
                                    onMoveItem = { itemId, targetId -> viewModel.moveWishItem(itemId, targetId) },
                                    onSetItemPicked = { itemId, picked, byName, secret, msg ->
                                        viewModel.setWishPicked(itemId, picked, byName, secret, msg)
                                    },
                                    onSaveImageToGallery = { viewModel.saveWishImageToGallery(context, it) },
                                    onUpdateItem = { viewModel.updateWishItem(it) },
                                    onShowToast = { viewModel.showToast(it) }
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = CyberGlassTokens.ElectricCyan)
                                }
                            }
                        } else {
                            WishlistsScreen(
                                wishlists = wishlistsWithStats,
                                syncingWishlistId = syncingId,
                                onOpenWishlist = { viewModel.openWishlistDetail(it) },
                                onCreateWishlist = { name, desc, emoji ->
                                    viewModel.createWishlist(name, desc, emoji)
                                },
                                onSyncAndShare = { wl ->
                                    viewModel.syncWishlist(wl.id) { name, url ->
                                        launchSystemShare(name, url)
                                    }
                                },
                                onShowQr = { viewModel.openQrDialog(it) },
                                onRefreshClaims = { viewModel.refreshAllClaims() }
                            )
                        }
                    }

                    WishlistTab.ADD -> {
                        QuickAddScreen(
                            wishlists = allWishlists,
                            defaultWishlistId = settings.lastUsedWishlistId,
                            onFetchMetadata = { url -> viewModel.fetchMetadataForInput(url) },
                            onSaveWish = { wishlistId, type, title, note, url, price, prio, uris, remoteImg, pub ->
                                viewModel.addManualWish(
                                    wishlistId = wishlistId,
                                    itemType = type,
                                    title = title,
                                    noteText = note,
                                    linkUrl = url,
                                    priceText = price,
                                    priority = prio,
                                    localImageUris = uris,
                                    remoteImageUrl = remoteImg,
                                    publishAndShare = pub
                                ) { name, shareUrl ->
                                    launchSystemShare(name, shareUrl)
                                }
                                currentTab = WishlistTab.WISHLISTS
                                viewModel.openWishlistDetail(wishlistId)
                            },
                            onShowToast = { viewModel.showToast(it) }
                        )
                    }

                    WishlistTab.SHARED -> {
                        SharedHubScreen(
                            wishlists = wishlistsWithStats,
                            pickedItems = pickedItems,
                            syncingWishlistId = syncingId,
                            onOpenWishlist = { id ->
                                currentTab = WishlistTab.WISHLISTS
                                viewModel.openWishlistDetail(id)
                            },
                            onSyncAndShare = { wl ->
                                viewModel.syncWishlist(wl.id) { name, url ->
                                    launchSystemShare(name, url)
                                }
                            },
                            onShowQr = { viewModel.openQrDialog(it) },
                            onUnpublish = { viewModel.unpublishWishlist(it) },
                            onRefreshAllClaims = { viewModel.refreshAllClaims() },
                            onShowToast = { viewModel.showToast(it) }
                        )
                    }

                    WishlistTab.SETTINGS -> {
                        SettingsScreen(
                            settings = settings,
                            connectedGithubUser = connectedUser,
                            isCheckingUpdate = isCheckingUpdate,
                            debugLogs = debugLogs,
                            onSaveOwnerName = { viewModel.updateOwnerName(it) },
                            onSaveCustomToken = { viewModel.saveCustomToken(it) },
                            onVerifyGitHub = { viewModel.verifyGitHub() },
                            onSetAutoSync = { viewModel.setAutoSync(it) },
                            onSetAutoUpdateCheck = { viewModel.setAutoUpdateCheck(it) },
                            onCheckUpdatesNow = { viewModel.checkForUpdatesManually() },
                            onClearLogs = { viewModel.clearDebugLogs() },
                            onExportLogs = { viewModel.exportDebugLogs() },
                            onShowToast = { viewModel.showToast(it) }
                        )
                    }
                }
            }
        }
    }

    pendingShare?.let { incoming -> key(incoming.shareId) {
        IncomingShareSheet(
            pending = incoming,
            wishlists = allWishlists,
            defaultWishlistId = settings.lastUsedWishlistId,
            onDismiss = { viewModel.dismissPendingShare() },
            onCreateNewFolder = { showInlineCreateFolder = true },
            onSave = { wishlistId, title, note, url, price, priority, pub ->
                currentTab = WishlistTab.WISHLISTS
                viewModel.savePendingShare(
                    wishlistId = wishlistId,
                    title = title,
                    noteText = note,
                    linkUrl = url,
                    priceText = price,
                    priority = priority,
                    publishAndShare = pub
                ) { name, shareUrl ->
                    launchSystemShare(name, shareUrl)
                }
            }
        )
    } }

    if (showInlineCreateFolder) {
        CreateWishlistDialog(
            onDismiss = { showInlineCreateFolder = false },
            onConfirm = { name, desc, emoji ->
                showInlineCreateFolder = false
                viewModel.createWishlist(name, desc, emoji)
            }
        )
    }

    qrDialogWishlist?.let { wl ->
        ShareQrDialog(
            wishlist = wl,
            onDismiss = { viewModel.closeQrDialog() },
            onShowToast = { viewModel.showToast(it) }
        )
    }
}

@Composable
private fun CyberGlassBottomBar(
    currentTab: WishlistTab,
    onTabSelected: (WishlistTab) -> Unit
) {
    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.18f),
            CyberGlassTokens.ElectricCyan.copy(alpha = 0.08f),
            Color.Transparent
        )
    )
    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            CyberGlassTokens.GlassSlate.copy(alpha = 0.92f),
            CyberGlassTokens.GlassSurface.copy(alpha = 0.98f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(BorderStroke(1.dp, borderBrush), RoundedCornerShape(20.dp))
            .background(bgBrush)
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WishlistTab.entries.forEach { tab ->
                val isSelected = currentTab == tab
                val itemBg = if (isSelected) {
                    Brush.verticalGradient(
                        listOf(
                            CyberGlassTokens.DeepCyan.copy(alpha = 0.35f),
                            CyberGlassTokens.ElectricCyan.copy(alpha = 0.12f)
                        )
                    )
                } else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (isSelected) Modifier.border(
                                BorderStroke(1.dp, CyberGlassTokens.ElectricCyan.copy(alpha = 0.5f)),
                                RoundedCornerShape(12.dp)
                            ) else Modifier
                        )
                        .background(itemBg)
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = if (isSelected) CyberGlassTokens.ElectricCyan else CyberGlassTokens.TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.title,
                            color = if (isSelected) CyberGlassTokens.ElectricCyan else CyberGlassTokens.TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
