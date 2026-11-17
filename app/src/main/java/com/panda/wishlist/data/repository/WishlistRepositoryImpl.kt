package com.panda.wishlist.data.repository

import com.panda.wishlist.core.database.dao.WishlistDao
import com.panda.wishlist.core.database.entity.WishItemEntity
import com.panda.wishlist.core.database.entity.WishItemType
import com.panda.wishlist.core.database.entity.WishlistEntity
import com.panda.wishlist.core.database.entity.WishlistWithStats
import com.panda.wishlist.core.logging.DebugLogManager
import com.panda.wishlist.core.logging.LogLevel
import com.panda.wishlist.data.sync.AppSettings
import com.panda.wishlist.data.sync.GitHubTokenStore
import com.panda.wishlist.data.sync.GitHubWishlistSyncService
import com.panda.wishlist.data.sync.NtfyClaimService
import com.panda.wishlist.data.sync.SyncResult
import com.panda.wishlist.data.sync.WishClaimEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WishlistRepositoryImpl @Inject constructor(
    private val dao: WishlistDao,
    private val tokenStore: GitHubTokenStore,
    private val gitHubSync: GitHubWishlistSyncService,
    private val ntfyClaims: NtfyClaimService,
    private val debugLog: DebugLogManager
) : WishlistRepository {

    override val settings: Flow<AppSettings> = tokenStore.settingsFlow

    override fun observeWishlistsWithStats(): Flow<List<WishlistWithStats>> =
        dao.observeAllWishlistsWithStats()

    override fun observeAllWishlists(): Flow<List<WishlistEntity>> =
        dao.observeAllWishlists()

    override fun observeWishlist(id: String): Flow<WishlistEntity?> =
        dao.observeWishlist(id)

    override fun observeItemsForWishlist(wishlistId: String): Flow<List<WishItemEntity>> =
        dao.observeItemsForWishlist(wishlistId)

    override fun observeAllPickedItems(): Flow<List<WishItemEntity>> =
        dao.observeAllPickedItems()

    override suspend fun ensureDefaultsSeeded() {
        val currentSettings = tokenStore.settingsFlow.first()
        if (currentSettings.hasSeededDefaults || dao.countWishlists() > 0) {
            if (!currentSettings.hasSeededDefaults) tokenStore.markSeededDefaults()
            return
        }
        val now = System.currentTimeMillis()
        val starter = listOf(
            WishlistEntity(
                id = generateShortId(),
                name = "My Wishlist",
                description = "Everyday finds, links, and gift ideas",
                emoji = "🎁",
                accentHex = 0xFF38BDF8L,
                createdAt = now
            ),
            WishlistEntity(
                id = generateShortId(),
                name = "Tech & Gadgets",
                description = "Accessories, gear, and upgrades",
                emoji = "⚡",
                accentHex = 0xFF2DD4BFL,
                createdAt = now - 1000L
            ),
            WishlistEntity(
                id = generateShortId(),
                name = "Birthday Wishes",
                description = "Shareable celebration wishlist",
                emoji = "🎂",
                accentHex = 0xFFA855F7L,
                createdAt = now - 2000L
            )
        )
        starter.forEach { dao.insertWishlist(it) }
        tokenStore.setLastUsedWishlistId(starter.first().id)
        tokenStore.markSeededDefaults()
        debugLog.log("Repository", "Seeded ${starter.size} starter wishlist folders", LogLevel.INFO)
    }

    override suspend fun createWishlist(
        name: String,
        description: String,
        emoji: String,
        accentHex: Long
    ): WishlistEntity {
        val id = generateShortId()
        val entity = WishlistEntity(
            id = id,
            name = name.trim().ifBlank { "New Wishlist" },
            description = description.trim(),
            emoji = emoji.ifBlank { "🎁" },
            accentHex = accentHex,
            ntfyTopic = gitHubSync.buildNtfyTopic(id),
            publicShareUrl = gitHubSync.buildPublicShareUrl(id),
            createdAt = System.currentTimeMillis()
        )
        dao.insertWishlist(entity)
        tokenStore.setLastUsedWishlistId(id)
        debugLog.log("Repository", "Created wishlist '${entity.name}' ($id)", LogLevel.ACTION)
        return entity
    }

    override suspend fun updateWishlistInfo(id: String, name: String, description: String, emoji: String) {
        val existing = dao.getWishlistById(id) ?: return
        val updated = existing.copy(
            name = name.trim().ifBlank { existing.name },
            description = description.trim(),
            emoji = emoji.ifBlank { existing.emoji }
        )
        dao.updateWishlist(updated)
        if (updated.isPublicSynced && tokenStore.settingsFlow.first().autoSyncOnChange) {
            syncWishlistToGitHub(id)
        }
    }

    override suspend fun deleteWishlist(id: String) {
        val existing = dao.getWishlistById(id) ?: return
        val items = dao.getItemsForWishlistOnce(id)
        if (existing.isPublicSynced) {
            gitHubSync.unpublishWishlist(id, items.map { it.id })
        }
        items.forEach { item ->
            if (item.localImagePath.isNotBlank()) {
                runCatching { File(item.localImagePath).delete() }
            }
        }
        dao.deleteWishlistById(id)
        val currentSettings = tokenStore.settingsFlow.first()
        if (currentSettings.defaultWishlistId == id) {
            tokenStore.setDefaultWishlistId("")
        }
        if (currentSettings.lastUsedWishlistId == id) {
            tokenStore.setLastUsedWishlistId("")
        }
        debugLog.log("Repository", "Deleted wishlist '${existing.name}'", LogLevel.ACTION)
    }

    override suspend fun addWishItems(
        wishlistId: String,
        itemType: WishItemType,
        title: String,
        noteText: String,
        linkUrl: String,
        storeDomain: String,
        priceText: String,
        priority: Int,
        localImagePaths: List<String>,
        remoteImageUrl: String,
        publishAfterSave: Boolean
    ): SyncResult? {
        val now = System.currentTimeMillis()
        if (localImagePaths.isEmpty()) {
            val item = WishItemEntity(
                id = generateShortId(),
                wishlistId = wishlistId,
                itemType = itemType.name,
                title = title.trim().ifBlank { "Saved Wish" },
                noteText = noteText.trim(),
                linkUrl = linkUrl.trim(),
                storeDomain = storeDomain.trim(),
                priceText = priceText.trim(),
                priority = priority.coerceIn(0, 2),
                localImagePath = "",
                remoteImageUrl = remoteImageUrl.trim(),
                createdAt = now
            )
            dao.insertItem(item)
        } else {
            localImagePaths.forEachIndexed { idx, imgPath ->
                val itemTitle = if (localImagePaths.size > 1) {
                    "${title.trim().ifBlank { "Saved Photo" }} (${idx + 1}/${localImagePaths.size})"
                } else {
                    title.trim().ifBlank { "Saved Photo" }
                }
                val item = WishItemEntity(
                    id = generateShortId(),
                    wishlistId = wishlistId,
                    itemType = WishItemType.IMAGE.name,
                    title = itemTitle,
                    noteText = noteText.trim(),
                    linkUrl = linkUrl.trim(),
                    storeDomain = storeDomain.trim(),
                    priceText = priceText.trim(),
                    priority = priority.coerceIn(0, 2),
                    localImagePath = imgPath,
                    remoteImageUrl = remoteImageUrl.trim(),
                    createdAt = now - idx
                )
                dao.insertItem(item)
            }
        }

        tokenStore.setLastUsedWishlistId(wishlistId)
        debugLog.log("Wishlist", "Added wish '$title' to folder $wishlistId", LogLevel.ACTION)

        val wishlist = dao.getWishlistById(wishlistId) ?: return null
        val autoSync = tokenStore.settingsFlow.first().autoSyncOnChange
        return if (publishAfterSave || (wishlist.isPublicSynced && autoSync)) {
            syncWishlistToGitHub(wishlistId)
        } else null
    }

    override suspend fun updateWishItem(item: WishItemEntity) {
        dao.updateItem(item)
        val wishlist = dao.getWishlistById(item.wishlistId)
        if (wishlist?.isPublicSynced == true && tokenStore.settingsFlow.first().autoSyncOnChange) {
            syncWishlistToGitHub(item.wishlistId)
        }
    }

    override suspend fun deleteWishItem(itemId: String) {
        val item = dao.getItemById(itemId) ?: return
        if (item.localImagePath.isNotBlank()) {
            runCatching { File(item.localImagePath).delete() }
        }
        dao.deleteItemById(itemId)
        debugLog.log("Wishlist", "Removed wish '${item.title}'", LogLevel.ACTION)

        val wishlist = dao.getWishlistById(item.wishlistId)
        if (wishlist?.isPublicSynced == true) {
            gitHubSync.deleteItemAsset(item.wishlistId, item.id)
            if (tokenStore.settingsFlow.first().autoSyncOnChange) {
                syncWishlistToGitHub(item.wishlistId)
            }
        }
    }

    override suspend fun moveWishItem(itemId: String, targetWishlistId: String) {
        val item = dao.getItemById(itemId) ?: return
        val sourceId = item.wishlistId
        if (sourceId == targetWishlistId) return
        dao.moveItemToWishlist(itemId, targetWishlistId)
        debugLog.log("Wishlist", "Moved '${item.title}' -> $targetWishlistId", LogLevel.ACTION)

        val autoSync = tokenStore.settingsFlow.first().autoSyncOnChange
        if (autoSync) {
            if (dao.getWishlistById(sourceId)?.isPublicSynced == true) {
                gitHubSync.deleteItemAsset(sourceId, itemId)
                syncWishlistToGitHub(sourceId)
            }
            if (dao.getWishlistById(targetWishlistId)?.isPublicSynced == true) {
                syncWishlistToGitHub(targetWishlistId)
            }
        }
    }

    override suspend fun setWishPicked(
        itemId: String,
        isPicked: Boolean,
        pickedByName: String,
        isSecret: Boolean,
        message: String
    ) {
        val item = dao.getItemById(itemId) ?: return
        val now = System.currentTimeMillis()
        val displayPicker = when {
            !isPicked -> ""
            isSecret -> "Secret Surprise 🤫"
            else -> pickedByName.trim().ifBlank { "Friend" }
        }
        val cleanMsg = if (isPicked) message.trim() else ""
        dao.updateItemPickState(
            itemId = itemId,
            isPicked = isPicked,
            pickedByName = displayPicker,
            pickedMessage = cleanMsg,
            // pickedAt records the last pick/unpick, not only picks: resetting it to 0 on unpick
            // let any older friend "pick" still on ntfy win again on the next claim refresh.
            pickedAt = now
        )

        val wishlist = dao.getWishlistById(item.wishlistId)
        if (wishlist?.isPublicSynced == true) {
            val topic = wishlist.ntfyTopic.ifBlank { gitHubSync.buildNtfyTopic(wishlist.id) }
            ntfyClaims.postClaimEvent(
                topic,
                WishClaimEvent(
                    action = if (isPicked) "pick" else "unpick",
                    itemId = itemId,
                    pickedByName = displayPicker,
                    isSecret = isSecret,
                    message = cleanMsg,
                    timestamp = now
                )
            )
            if (tokenStore.settingsFlow.first().autoSyncOnChange) {
                syncWishlistToGitHub(wishlist.id)
            }
        }
    }

    override suspend fun syncWishlistToGitHub(wishlistId: String): SyncResult {
        val wishlist = dao.getWishlistById(wishlistId)
            ?: return SyncResult.Error("Wishlist not found")

        // Pull any recent browser picks first so we don't overwrite a friend's pick
        refreshLiveClaimsForWishlist(wishlistId)

        val items = dao.getItemsForWishlistOnce(wishlistId)
        val ownerName = tokenStore.settingsFlow.first().ownerDisplayName
        val res = gitHubSync.publishWishlist(wishlist, items, ownerName)
        if (res is SyncResult.Success) {
            val topic = wishlist.ntfyTopic.ifBlank { gitHubSync.buildNtfyTopic(wishlistId) }
            dao.updateWishlist(
                wishlist.copy(
                    isPublicSynced = true,
                    publicShareUrl = res.shareUrl,
                    ntfyTopic = topic,
                    lastSyncedAt = System.currentTimeMillis()
                )
            )
        }
        return res
    }

    override suspend fun unpublishWishlist(wishlistId: String) {
        val wishlist = dao.getWishlistById(wishlistId) ?: return
        val items = dao.getItemsForWishlistOnce(wishlistId)
        gitHubSync.unpublishWishlist(wishlistId, items.map { it.id })
        dao.updateWishlist(
            wishlist.copy(
                isPublicSynced = false,
                lastSyncedAt = 0L
            )
        )
    }

    override suspend fun refreshLiveClaimsForWishlist(wishlistId: String): Int {
        val wishlist = dao.getWishlistById(wishlistId) ?: return 0
        if (!wishlist.isPublicSynced && wishlist.ntfyTopic.isBlank()) return 0
        val topic = wishlist.ntfyTopic.ifBlank { gitHubSync.buildNtfyTopic(wishlistId) }
        val claims = ntfyClaims.fetchLatestClaims(topic)
        if (claims.isEmpty()) return 0

        val items = dao.getItemsForWishlistOnce(wishlistId)
        var updatedCount = 0
        for (item in items) {
            val claim = claims[item.id] ?: continue
            if (claim.timestamp > item.pickedAt) {
                val shouldBePicked = claim.action != "unpick"
                val pickerLabel = when {
                    !shouldBePicked -> ""
                    claim.isSecret -> "Secret Surprise 🤫"
                    else -> claim.pickedByName.ifBlank { "A Friend" }
                }
                if (item.isPicked != shouldBePicked ||
                    item.pickedByName != pickerLabel ||
                    item.pickedMessage != claim.message
                ) {
                    dao.updateItemPickState(
                        itemId = item.id,
                        isPicked = shouldBePicked,
                        pickedByName = pickerLabel,
                        pickedMessage = if (shouldBePicked) claim.message else "",
                        pickedAt = claim.timestamp
                    )
                    updatedCount++
                }
            }
        }
        if (updatedCount > 0) {
            debugLog.log("Claims", "Synced $updatedCount friend pick(s) for '${wishlist.name}'", LogLevel.ACTION)
        }
        return updatedCount
    }

    override suspend fun refreshAllLiveClaims(): Int {
        val lists = dao.getAllWishlistsOnce().filter { it.isPublicSynced }
        var total = 0
        for (list in lists) {
            total += refreshLiveClaimsForWishlist(list.id)
        }
        return total
    }

    override suspend fun syncFriendPicksAndRepublish(): Int {
        val autoSync = tokenStore.settingsFlow.first().autoSyncOnChange
        var total = 0
        for (list in dao.getAllWishlistsOnce().filter { it.isPublicSynced }) {
            val changed = refreshLiveClaimsForWishlist(list.id)
            total += changed
            // ntfy.sh only keeps messages for ~12 hours, and the web page reads picks from
            // there plus wishlist.json. Writing new picks into wishlist.json is what keeps a
            // friend's pick visible to everyone after ntfy forgets it.
            if (changed > 0 && autoSync) syncWishlistToGitHub(list.id)
        }
        return total
    }

    override suspend fun verifyGitHubConnection(): String? = gitHubSync.verifyConnection()

    override suspend fun setCustomGitHubToken(token: String) = tokenStore.setCustomToken(token)

    override suspend fun setOwnerDisplayName(name: String) = tokenStore.setOwnerDisplayName(name)

    override suspend fun setDefaultWishlistId(id: String) = tokenStore.setDefaultWishlistId(id)

    override suspend fun setAutoSaveShareEnabled(enabled: Boolean) = tokenStore.setAutoSaveShareEnabled(enabled)

    override suspend fun setAutoSyncOnChange(enabled: Boolean) = tokenStore.setAutoSyncOnChange(enabled)

    override suspend fun setAutoUpdateCheckEnabled(enabled: Boolean) = tokenStore.setAutoUpdateCheckEnabled(enabled)

    private fun generateShortId(): String = UUID.randomUUID().toString().replace("-", "").take(10)
}
