package com.panda.wishlist.data.repository

import com.panda.wishlist.core.database.entity.WishItemEntity
import com.panda.wishlist.core.database.entity.WishItemType
import com.panda.wishlist.core.database.entity.WishlistEntity
import com.panda.wishlist.core.database.entity.WishlistWithStats
import com.panda.wishlist.data.sync.AppSettings
import com.panda.wishlist.data.sync.SyncResult
import kotlinx.coroutines.flow.Flow

interface WishlistRepository {
    val settings: Flow<AppSettings>
    fun observeWishlistsWithStats(): Flow<List<WishlistWithStats>>
    fun observeAllWishlists(): Flow<List<WishlistEntity>>
    fun observeWishlist(id: String): Flow<WishlistEntity?>
    fun observeItemsForWishlist(wishlistId: String): Flow<List<WishItemEntity>>
    fun observeAllPickedItems(): Flow<List<WishItemEntity>>

    suspend fun ensureDefaultsSeeded()
    suspend fun createWishlist(name: String, description: String, emoji: String, accentHex: Long = 0xFF38BDF8L): WishlistEntity
    suspend fun updateWishlistInfo(id: String, name: String, description: String, emoji: String)
    suspend fun deleteWishlist(id: String)

    suspend fun addWishItems(
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
        publishAfterSave: Boolean = false
    ): SyncResult?

    suspend fun updateWishItem(item: WishItemEntity)
    suspend fun deleteWishItem(itemId: String)
    suspend fun moveWishItem(itemId: String, targetWishlistId: String)

    suspend fun setWishPicked(
        itemId: String,
        isPicked: Boolean,
        pickedByName: String,
        isSecret: Boolean,
        message: String
    )

    suspend fun syncWishlistToGitHub(wishlistId: String): SyncResult
    suspend fun unpublishWishlist(wishlistId: String)
    suspend fun refreshLiveClaimsForWishlist(wishlistId: String): Int
    suspend fun refreshAllLiveClaims(): Int

    suspend fun verifyGitHubConnection(): String?
    suspend fun setCustomGitHubToken(token: String)
    suspend fun setOwnerDisplayName(name: String)
    suspend fun setAutoSyncOnChange(enabled: Boolean)
    suspend fun setAutoUpdateCheckEnabled(enabled: Boolean)
}
