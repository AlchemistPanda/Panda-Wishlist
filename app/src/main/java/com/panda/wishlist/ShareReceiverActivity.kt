package com.panda.wishlist

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.panda.wishlist.ui.ShareReceiverViewModel
import com.panda.wishlist.ui.components.CreateWishlistDialog
import com.panda.wishlist.ui.components.IncomingShareSheet
import com.panda.wishlist.ui.theme.PandaWishlistTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Translucent floating share pop-up activity so sharing from WhatsApp, Chrome, Amazon, or Gallery
 * saves directly to the chosen wishlist without opening the full app.
 */
@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {

    private val viewModel: ShareReceiverViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            viewModel.handleIncomingIntent(intent) { finish() }
        }
        setContent {
            PandaWishlistTheme(configureSystemBars = false) {
                val allWishlists by viewModel.allWishlists.collectAsState()
                val settings by viewModel.settings.collectAsState()
                val pendingShare by viewModel.pendingShare.collectAsState()
                val isSaving by viewModel.isSaving.collectAsState()
                var showInlineCreateFolder by remember { mutableStateOf(false) }

                val resolvedDefaultWishlistId = remember(settings, allWishlists) {
                    settings.resolveDefaultWishlistId(allWishlists)
                }

                BackHandler {
                    viewModel.dismissPendingShare()
                    finish()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isSaving) {
                                viewModel.dismissPendingShare()
                                finish()
                            }
                        }
                        .systemBarsPadding()
                        .imePadding(),
                    contentAlignment = Alignment.Center
                ) {
                    pendingShare?.let { incoming ->
                        key(incoming.shareId) {
                            Box(
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {}
                                )
                            ) {
                                IncomingShareSheet(
                                    pending = incoming,
                                    wishlists = allWishlists,
                                    defaultWishlistId = resolvedDefaultWishlistId,
                                    autoSaveEnabled = settings.autoSaveShareEnabled,
                                    useDialog = false,
                                    isSaving = isSaving,
                                    onDismiss = {
                                        viewModel.dismissPendingShare()
                                        finish()
                                    },
                                    onCreateNewFolder = { showInlineCreateFolder = true },
                                    onSave = { wishlistId, title, note, url, price, priority, pub ->
                                        viewModel.savePendingShare(
                                            wishlistId = wishlistId,
                                            title = title,
                                            noteText = note,
                                            linkUrl = url,
                                            priceText = price,
                                            priority = priority,
                                            publishAndShare = pub,
                                            onSavedDone = { finish() },
                                            onShareReady = { name, shareUrl ->
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(
                                                        Intent.EXTRA_TEXT,
                                                        "Check out my '$name' wishlist on Panda Wishlist 🐼🎁\n$shareUrl"
                                                    )
                                                }
                                                startActivity(Intent.createChooser(shareIntent, "Share Public Wishlist"))
                                                finish()
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }

                    if (showInlineCreateFolder) {
                        CreateWishlistDialog(
                            onDismiss = { showInlineCreateFolder = false },
                            onConfirm = { name, desc, emoji ->
                                showInlineCreateFolder = false
                                viewModel.createWishlist(name, desc, emoji)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.handleIncomingIntent(intent) { finish() }
    }
}
