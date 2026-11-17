package com.panda.wishlist.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panda.wishlist.core.database.entity.WishItemType
import com.panda.wishlist.core.database.entity.WishlistEntity
import com.panda.wishlist.core.logging.DebugLogManager
import com.panda.wishlist.core.logging.LogLevel
import com.panda.wishlist.core.share.LinkMetadataExtractor
import com.panda.wishlist.core.share.ParsedSharePayload
import com.panda.wishlist.core.share.ShareContentAnalyzer
import com.panda.wishlist.core.share.ShareIntentParser
import com.panda.wishlist.data.repository.WishlistRepository
import com.panda.wishlist.data.sync.AppSettings
import com.panda.wishlist.data.sync.SyncResult
import com.panda.wishlist.di.ApplicationScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class PendingIncomingShare(
    val payload: ParsedSharePayload,
    val remoteImageUrl: String = "",
    val isFetchingMeta: Boolean = false,
    /** Identifies one incoming share; metadata updates keep it, a new share gets a new one. */
    val shareId: Long = System.nanoTime()
)

@HiltViewModel
class ShareReceiverViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    @ApplicationScope private val appScope: CoroutineScope,
    private val repository: WishlistRepository,
    private val shareIntentParser: ShareIntentParser,
    private val linkMetadataExtractor: LinkMetadataExtractor,
    private val debugLog: DebugLogManager
) : ViewModel() {

    val allWishlists: StateFlow<List<WishlistEntity>> =
        repository.observeAllWishlists()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> =
        repository.settings
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _pendingShare = MutableStateFlow<PendingIncomingShare?>(null)
    val pendingShare: StateFlow<PendingIncomingShare?> = _pendingShare.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultsSeeded()
        }
    }

    fun handleIncomingIntent(intent: Intent?, onEmptyShare: () -> Unit) {
        if (!shareIntentParser.isShareIntent(intent)) {
            onEmptyShare()
            return
        }
        viewModelScope.launch {
            repository.ensureDefaultsSeeded()
            val parsed = withContext(Dispatchers.IO) {
                shareIntentParser.parseIntent(intent)
            }
            if (parsed == null) {
                Toast.makeText(appContext, "Nothing shareable found", Toast.LENGTH_SHORT).show()
                onEmptyShare()
                return@launch
            }

            debugLog.log(
                "ShareIn",
                "Received ${parsed.itemType.name}: '${parsed.title}' (${parsed.localImagePaths.size} img)",
                LogLevel.ACTION
            )

            val shouldFetchMeta = parsed.linkUrl.isNotBlank()
            _pendingShare.value?.payload?.localImagePaths?.let { stale ->
                appScope.launch { stale.forEach { runCatching { File(it).delete() } } }
            }
            _pendingShare.value = PendingIncomingShare(
                payload = parsed,
                isFetchingMeta = shouldFetchMeta
            )

            if (shouldFetchMeta) {
                val meta = linkMetadataExtractor.fetchMetadata(parsed.linkUrl)
                val current = _pendingShare.value ?: return@launch
                if (current.payload.linkUrl != parsed.linkUrl ||
                    current.payload.localImagePaths != parsed.localImagePaths
                ) return@launch

                val upgradedTitle = when {
                    meta.title.isNotBlank() && (
                        current.payload.title.endsWith("Wish") ||
                            current.payload.title == "Saved Link" ||
                            current.payload.title.length < 18
                        ) -> meta.title
                    else -> current.payload.title
                }
                val upgradedPrice = current.payload.priceText.ifBlank { meta.priceText }
                val upgradedNote = current.payload.noteText.ifBlank {
                    if (meta.description.isNotBlank() && meta.description != upgradedTitle) meta.description else ""
                }
                _pendingShare.value = current.copy(
                    payload = current.payload.copy(
                        title = upgradedTitle,
                        priceText = upgradedPrice,
                        noteText = upgradedNote
                    ),
                    remoteImageUrl = meta.imageUrl,
                    isFetchingMeta = false
                )
            }
        }
    }

    fun dismissPendingShare() {
        val dismissed = _pendingShare.value ?: return
        _pendingShare.value = null
        appScope.launch {
            dismissed.payload.localImagePaths.forEach { runCatching { File(it).delete() } }
        }
    }

    fun createWishlist(name: String, description: String, emoji: String) {
        viewModelScope.launch {
            val created = repository.createWishlist(name, description, emoji)
            Toast.makeText(appContext, "Created folder '${created.name}'", Toast.LENGTH_SHORT).show()
        }
    }

    fun savePendingShare(
        wishlistId: String,
        title: String,
        noteText: String,
        linkUrl: String,
        priceText: String,
        priority: Int,
        publishAndShare: Boolean,
        onSavedDone: () -> Unit,
        onShareReady: (wishlistName: String, url: String) -> Unit
    ) {
        val pending = _pendingShare.value ?: return
        if (!publishAndShare) {
            _pendingShare.value = null
        }
        viewModelScope.launch {
            if (publishAndShare) _isSaving.value = true
            val cleanUrl = linkUrl.trim()
            val store = ShareContentAnalyzer.detectStoreName(cleanUrl)
            val effectiveType = when {
                pending.payload.localImagePaths.isNotEmpty() -> WishItemType.IMAGE
                cleanUrl.isNotBlank() -> WishItemType.LINK
                else -> WishItemType.TEXT
            }
            val resolvedTitle = ShareContentAnalyzer.resolveFallbackTitle(
                title = title,
                itemType = effectiveType,
                storeName = store,
                hasImages = pending.payload.localImagePaths.isNotEmpty()
            )
            val folder = allWishlists.value.find { it.id == wishlistId }
            val folderLabel = folder?.let { "${it.emoji} ${it.name}" } ?: "wishlist"

            val res = repository.addWishItems(
                wishlistId = wishlistId,
                itemType = effectiveType,
                title = resolvedTitle,
                noteText = noteText,
                linkUrl = cleanUrl,
                storeDomain = store,
                priceText = priceText,
                priority = priority,
                localImagePaths = pending.payload.localImagePaths,
                remoteImageUrl = pending.remoteImageUrl,
                publishAfterSave = publishAndShare,
                awaitSync = publishAndShare
            )
            _isSaving.value = false
            _pendingShare.value = null

            if (publishAndShare && res is SyncResult.Success) {
                onShareReady(folder?.name ?: "My Wishlist", res.shareUrl)
            } else if (res is SyncResult.Error) {
                Toast.makeText(appContext, "Saved to $folderLabel (sync note: ${res.message})", Toast.LENGTH_SHORT).show()
                onSavedDone()
            } else {
                Toast.makeText(appContext, "Saved to $folderLabel!", Toast.LENGTH_SHORT).show()
                onSavedDone()
            }
        }
    }
}
