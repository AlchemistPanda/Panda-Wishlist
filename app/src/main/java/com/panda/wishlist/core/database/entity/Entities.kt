package com.panda.wishlist.core.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class WishItemType {
    LINK,
    IMAGE,
    TEXT
}

@Entity(tableName = "wishlists")
data class WishlistEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String = "",
    val emoji: String = "🎁",
    val accentHex: Long = 0xFF38BDF8L,
    val isPublicSynced: Boolean = false,
    val publicShareUrl: String = "",
    val ntfyTopic: String = "",
    val lastSyncedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "wish_items",
    foreignKeys = [
        ForeignKey(
            entity = WishlistEntity::class,
            parentColumns = ["id"],
            childColumns = ["wishlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("wishlistId")]
)
data class WishItemEntity(
    @PrimaryKey
    val id: String,
    val wishlistId: String,
    val itemType: String = WishItemType.LINK.name,
    val title: String,
    val noteText: String = "",
    val linkUrl: String = "",
    val storeDomain: String = "",
    val priceText: String = "",
    val priority: Int = 1, // 0 = Nice to Have, 1 = Love It, 2 = Must-Have
    val localImagePath: String = "",
    val remoteImageUrl: String = "",
    val isPicked: Boolean = false,
    val pickedByName: String = "",
    val pickedMessage: String = "",
    val pickedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

data class WishlistWithStats(
    @Embedded val wishlist: WishlistEntity,
    val itemCount: Int,
    val pickedCount: Int
)
