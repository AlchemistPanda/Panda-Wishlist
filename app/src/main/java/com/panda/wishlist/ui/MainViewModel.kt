package com.panda.wishlist.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panda.wishlist.core.database.entity.WishItemEntity
import com.panda.wishlist.core.database.entity.WishItemType
import com.panda.wishlist.core.database.entity.WishlistEntity
import com.panda.wishlist.core.database.entity.WishlistWithStats
import com.panda.wishlist.core.logging.DebugLogEntry
import com.panda.wishlist.core.logging.DebugLogManager
import com.panda.wishlist.core.logging.LogLevel
import com.panda.wishlist.core.share.LinkMetadataExtractor
import com.panda.wishlist.core.share.ParsedSharePayload
import com.panda.wishlist.core.share.ShareContentAnalyzer
import com.panda.wishlist.core.share.ShareIntentParser
import com.panda.wishlist.data.repository.WishlistRepository
import com.panda.wishlist.data.sync.AppSettings
import com.panda.wishlist.data.sync.SyncResult
import com.panda.wishlist.data.update.AppUpdateChecker
import com.panda.wishlist.data.update.UpdateCheckResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class PendingIncomingShare(
    val payload: ParsedSharePayload,
    val remoteImageUrl: String = "",
    val isFetchingMeta: Boolean = false,
    /** Identifies one incoming share; metadata updates keep it, a new share gets a new one. */
    val shareId: Long = System.nanoTime()
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: WishlistRepository,
    private val shareIntentParser: ShareIntentParser,
    private val linkMetadataExtractor: LinkMetadataExtractor,
    private val updateChecker: AppUpdateChecker,
    private val debugLog: DebugLogManager,
    private val okHttpClient: OkHttpClient
) : ViewModel() {

    val wishlistsWithStats: StateFlow<List<WishlistWithStats>> =
        repository.observeWishlistsWithStats()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWishlists: StateFlow<List<WishlistEntity>> =
        repository.observeAllWishlists()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pickedItems: StateFlow<List<WishItemEntity>> =
        repository.observeAllPickedItems()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> =
        repository.settings
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val debugLogs: StateFlow<List<DebugLogEntry>> = debugLog.logs

    private val _selectedWishlistId = MutableStateFlow<String?>(null)
    val selectedWishlistId: StateFlow<String?> = _selectedWishlistId.asStateFlow()

    private val _pendingShare = MutableStateFlow<PendingIncomingShare?>(null)
    val pendingShare: StateFlow<PendingIncomingShare?> = _pendingShare.asStateFlow()

    private val _syncingWishlistId = MutableStateFlow<String?>(null)
    val syncingWishlistId: StateFlow<String?> = _syncingWishlistId.asStateFlow()

    private val _qrDialogWishlist = MutableStateFlow<WishlistEntity?>(null)
    val qrDialogWishlist: StateFlow<WishlistEntity?> = _qrDialogWishlist.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _connectedGithubUser = MutableStateFlow<String?>(null)
    val connectedGithubUser: StateFlow<String?> = _connectedGithubUser.asStateFlow()

    private val _startupUpdate = MutableStateFlow<UpdateCheckResult?>(null)
    val startupUpdate: StateFlow<UpdateCheckResult?> = _startupUpdate.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultsSeeded()
            verifyGitHub()
            repository.syncFriendPicksAndRepublish()
            val s = repository.settings.first()
            if (s.autoUpdateCheckEnabled) {
                val res = updateChecker.checkForUpdates()
                if (res?.hasUpdate == true) {
                    _startupUpdate.value = res
                }
            }
        }
    }

    fun observeWishlist(id: String) = repository.observeWishlist(id)
    fun observeItemsForWishlist(id: String) = repository.observeItemsForWishlist(id)

    fun openWishlistDetail(id: String) {
        _selectedWishlistId.value = id
        viewModelScope.launch {
            repository.refreshLiveClaimsForWishlist(id)
        }
    }

    fun closeWishlistDetail() {
        _selectedWishlistId.value = null
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun dismissUpdateBanner() {
        _startupUpdate.value = null
    }

    fun openQrDialog(wishlist: WishlistEntity) {
        _qrDialogWishlist.value = wishlist
    }

    fun closeQrDialog() {
        _qrDialogWishlist.value = null
    }

    fun handleIncomingIntent(intent: Intent?) {
        if (!shareIntentParser.isShareIntent(intent)) return
        viewModelScope.launch {
            repository.ensureDefaultsSeeded()
            val parsed = withContext(Dispatchers.IO) {
                shareIntentParser.parseIntent(intent)
            } ?: return@launch

            debugLog.log(
                "ShareIn",
                "Received ${parsed.itemType.name}: '${parsed.title}' (${parsed.localImagePaths.size} img)",
                LogLevel.ACTION
            )

            val shouldFetchMeta = parsed.linkUrl.isNotBlank()
            // A new share replaces one still on screen; clean up the replaced one's photos.
            _pendingShare.value?.payload?.localImagePaths?.let { stale ->
                withContext(Dispatchers.IO) { stale.forEach { runCatching { File(it).delete() } } }
            }
            _pendingShare.value = PendingIncomingShare(
                payload = parsed,
                isFetchingMeta = shouldFetchMeta
            )

            if (shouldFetchMeta) {
                val meta = linkMetadataExtractor.fetchMetadata(parsed.linkUrl)
                val current = _pendingShare.value ?: return@launch
                // Another share may have replaced this one while the page was loading.
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
        // The shared photos were already copied into app storage; drop them if not saved.
        viewModelScope.launch(Dispatchers.IO) {
            dismissed.payload.localImagePaths.forEach { runCatching { File(it).delete() } }
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
        onShareReady: (wishlistName: String, url: String) -> Unit
    ) {
        val pending = _pendingShare.value ?: return
        _pendingShare.value = null
        viewModelScope.launch {
            val cleanUrl = linkUrl.trim()
            val store = ShareContentAnalyzer.detectStoreName(cleanUrl)
            val effectiveType = when {
                pending.payload.localImagePaths.isNotEmpty() -> WishItemType.IMAGE
                cleanUrl.isNotBlank() -> WishItemType.LINK
                else -> WishItemType.TEXT
            }
            if (publishAndShare) _syncingWishlistId.value = wishlistId
            val res = repository.addWishItems(
                wishlistId = wishlistId,
                itemType = effectiveType,
                title = title,
                noteText = noteText,
                linkUrl = cleanUrl,
                storeDomain = store,
                priceText = priceText,
                priority = priority,
                localImagePaths = pending.payload.localImagePaths,
                remoteImageUrl = pending.remoteImageUrl,
                publishAfterSave = publishAndShare
            )
            _syncingWishlistId.value = null
            _selectedWishlistId.value = wishlistId

            if (publishAndShare && res is SyncResult.Success) {
                val listName = allWishlists.value.find { it.id == wishlistId }?.name ?: "My Wishlist"
                onShareReady(listName, res.shareUrl)
            } else if (res is SyncResult.Error) {
                _toastMessage.value = "Saved locally. Sync note: ${res.message}"
            } else {
                _toastMessage.value = "Added to wishlist!"
            }
        }
    }

    fun createWishlist(name: String, description: String, emoji: String, accentHex: Long = 0xFF38BDF8L) {
        viewModelScope.launch {
            val created = repository.createWishlist(name, description, emoji, accentHex)
            _toastMessage.value = "Created folder '${created.name}'"
        }
    }

    fun updateWishlistInfo(id: String, name: String, description: String, emoji: String) {
        viewModelScope.launch {
            repository.updateWishlistInfo(id, name, description, emoji)
            _toastMessage.value = "Folder updated"
        }
    }

    fun deleteWishlist(id: String) {
        viewModelScope.launch {
            if (_selectedWishlistId.value == id) _selectedWishlistId.value = null
            repository.deleteWishlist(id)
            _toastMessage.value = "Wishlist deleted"
        }
    }

    fun addManualWish(
        wishlistId: String,
        itemType: WishItemType,
        title: String,
        noteText: String,
        linkUrl: String,
        priceText: String,
        priority: Int,
        localImageUris: List<Uri>,
        remoteImageUrl: String,
        publishAndShare: Boolean,
        onShareReady: (wishlistName: String, url: String) -> Unit
    ) {
        viewModelScope.launch {
            val copiedPaths = withContext(Dispatchers.IO) {
                localImageUris.mapNotNull { shareIntentParser.copyUriToLocalImage(it) }
            }
            val cleanUrl = linkUrl.trim()
            val store = ShareContentAnalyzer.detectStoreName(cleanUrl)
            if (publishAndShare) _syncingWishlistId.value = wishlistId

            val res = repository.addWishItems(
                wishlistId = wishlistId,
                itemType = if (copiedPaths.isNotEmpty()) WishItemType.IMAGE else itemType,
                title = title,
                noteText = noteText,
                linkUrl = cleanUrl,
                storeDomain = store,
                priceText = priceText,
                priority = priority,
                localImagePaths = copiedPaths,
                remoteImageUrl = remoteImageUrl,
                publishAfterSave = publishAndShare
            )
            _syncingWishlistId.value = null

            if (publishAndShare && res is SyncResult.Success) {
                val listName = allWishlists.value.find { it.id == wishlistId }?.name ?: "My Wishlist"
                onShareReady(listName, res.shareUrl)
            } else if (res is SyncResult.Error) {
                _toastMessage.value = "Saved locally. Sync error: ${res.message}"
            } else {
                _toastMessage.value = "Wish saved!"
            }
        }
    }

    suspend fun fetchMetadataForInput(url: String) = linkMetadataExtractor.fetchMetadata(url)

    fun updateWishItem(item: WishItemEntity) {
        viewModelScope.launch {
            repository.updateWishItem(item)
            _toastMessage.value = "Wish updated"
        }
    }

    fun deleteWishItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteWishItem(itemId)
            _toastMessage.value = "Removed from wishlist"
        }
    }

    fun moveWishItem(itemId: String, targetWishlistId: String) {
        viewModelScope.launch {
            repository.moveWishItem(itemId, targetWishlistId)
            _toastMessage.value = "Moved to folder"
        }
    }

    fun setWishPicked(
        itemId: String,
        isPicked: Boolean,
        pickedByName: String,
        isSecret: Boolean,
        message: String
    ) {
        viewModelScope.launch {
            repository.setWishPicked(itemId, isPicked, pickedByName, isSecret, message)
            _toastMessage.value = if (isPicked) "Wish marked as picked 🎁" else "Wish marked as available"
        }
    }

    fun syncWishlist(
        wishlistId: String,
        onShareReady: ((wishlistName: String, url: String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _syncingWishlistId.value = wishlistId
            when (val res = repository.syncWishlistToGitHub(wishlistId)) {
                is SyncResult.Success -> {
                    val list = allWishlists.value.find { it.id == wishlistId }
                    _toastMessage.value = "Synced live to GitHub Pages!"
                    if (onShareReady != null && list != null) {
                        onShareReady(list.name, res.shareUrl)
                    }
                }
                is SyncResult.Error -> {
                    _toastMessage.value = res.message
                }
            }
            _syncingWishlistId.value = null
        }
    }

    fun unpublishWishlist(wishlistId: String) {
        viewModelScope.launch {
            _syncingWishlistId.value = wishlistId
            repository.unpublishWishlist(wishlistId)
            _syncingWishlistId.value = null
            _toastMessage.value = "Unpublished from public web"
        }
    }

    fun refreshAllClaims() {
        viewModelScope.launch {
            val count = repository.syncFriendPicksAndRepublish()
            _toastMessage.value = if (count > 0) {
                "Updated $count picked wish(es) from friends 🎁"
            } else {
                "All wishes are up to date"
            }
        }
    }

    fun saveWishImageToGallery(context: Context, item: WishItemEntity) {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) {
                saveImageFileToGallery(context, item)
            }
            _toastMessage.value = if (saved) {
                "Saved image to Pictures/PandaWishlist"
            } else {
                "Could not save image to gallery"
            }
        }
    }

    private fun saveImageFileToGallery(context: Context, item: WishItemEntity): Boolean {
        return runCatching {
            val bitmap: Bitmap = when {
                item.localImagePath.isNotBlank() && File(item.localImagePath).exists() ->
                    BitmapFactory.decodeFile(item.localImagePath)
                item.remoteImageUrl.isNotBlank() -> {
                    val req = Request.Builder().url(item.remoteImageUrl).build()
                    val bytes = okHttpClient.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) null else resp.body?.bytes()
                    } ?: return false
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                else -> return false
            }

            val filename = "PandaWish_${item.id}_${System.currentTimeMillis()}.jpg"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/PandaWishlist")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return false
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            } else {
                @Suppress("DEPRECATION")
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "PandaWishlist"
                ).apply { mkdirs() }
                val outFile = File(dir, filename)
                FileOutputStream(outFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                MediaScannerConnection.scanFile(context, arrayOf(outFile.absolutePath), arrayOf("image/jpeg"), null)
            }
            bitmap.recycle()
            true
        }.getOrDefault(false)
    }

    fun verifyGitHub() {
        viewModelScope.launch {
            _connectedGithubUser.value = repository.verifyGitHubConnection()
        }
    }

    fun saveCustomToken(token: String) {
        viewModelScope.launch {
            repository.setCustomGitHubToken(token)
            verifyGitHub()
            _toastMessage.value = "GitHub token updated"
        }
    }

    fun updateOwnerName(name: String) {
        viewModelScope.launch {
            repository.setOwnerDisplayName(name)
            _toastMessage.value = "Display name saved"
        }
    }

    fun setAutoSync(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoSyncOnChange(enabled) }
    }

    fun setAutoUpdateCheck(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoUpdateCheckEnabled(enabled) }
    }

    fun checkForUpdatesManually() {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val res = updateChecker.checkForUpdates()
            _isCheckingUpdate.value = false
            if (res == null) {
                _toastMessage.value = "Could not reach Panda App Store"
            } else if (res.hasUpdate) {
                _startupUpdate.value = res
                _toastMessage.value = "Update v${res.latestVersion} available!"
            } else {
                _toastMessage.value = "You're on the latest build (v${res.currentVersion})"
            }
        }
    }

    fun clearDebugLogs() = debugLog.clear()
    fun exportDebugLogs(): String = debugLog.exportAsText()
}
