package com.panda.wishlist.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.panda.wishlist.core.database.entity.WishItemEntity
import com.panda.wishlist.core.database.entity.WishlistEntity
import com.panda.wishlist.core.database.entity.WishlistWithStats
import kotlinx.coroutines.flow.Flow

@Dao
interface WishlistDao {

    @Query("""
        SELECT w.*,
               COUNT(i.id) AS itemCount,
               COALESCE(SUM(CASE WHEN i.isPicked = 1 THEN 1 ELSE 0 END), 0) AS pickedCount
        FROM wishlists w
        LEFT JOIN wish_items i ON w.id = i.wishlistId
        GROUP BY w.id
        ORDER BY w.isPublicSynced DESC, w.createdAt DESC
    """)
    fun observeAllWishlistsWithStats(): Flow<List<WishlistWithStats>>

    @Query("SELECT * FROM wishlists ORDER BY createdAt DESC")
    fun observeAllWishlists(): Flow<List<WishlistEntity>>

    @Query("SELECT * FROM wishlists ORDER BY createdAt DESC")
    suspend fun getAllWishlistsOnce(): List<WishlistEntity>

    @Query("SELECT * FROM wishlists WHERE id = :id LIMIT 1")
    fun observeWishlist(id: String): Flow<WishlistEntity?>

    @Query("SELECT * FROM wishlists WHERE id = :id LIMIT 1")
    suspend fun getWishlistById(id: String): WishlistEntity?

    @Query("SELECT COUNT(*) FROM wishlists")
    suspend fun countWishlists(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishlist(wishlist: WishlistEntity)

    @Update
    suspend fun updateWishlist(wishlist: WishlistEntity)

    @Query("DELETE FROM wishlists WHERE id = :id")
    suspend fun deleteWishlistById(id: String)

    @Query("SELECT * FROM wish_items WHERE wishlistId = :wishlistId ORDER BY isPicked ASC, priority DESC, createdAt DESC")
    fun observeItemsForWishlist(wishlistId: String): Flow<List<WishItemEntity>>

    @Query("SELECT * FROM wish_items WHERE wishlistId = :wishlistId ORDER BY createdAt DESC")
    suspend fun getItemsForWishlistOnce(wishlistId: String): List<WishItemEntity>

    @Query("SELECT * FROM wish_items WHERE isPicked = 1 ORDER BY pickedAt DESC")
    fun observeAllPickedItems(): Flow<List<WishItemEntity>>

    @Query("SELECT * FROM wish_items ORDER BY createdAt DESC")
    fun observeAllItems(): Flow<List<WishItemEntity>>

    @Query("SELECT * FROM wish_items WHERE id = :itemId LIMIT 1")
    suspend fun getItemById(itemId: String): WishItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: WishItemEntity)

    @Update
    suspend fun updateItem(item: WishItemEntity)

    @Query("DELETE FROM wish_items WHERE id = :itemId")
    suspend fun deleteItemById(itemId: String)

    @Query("UPDATE wish_items SET wishlistId = :targetWishlistId WHERE id = :itemId")
    suspend fun moveItemToWishlist(itemId: String, targetWishlistId: String)

    @Query("UPDATE wish_items SET isPicked = :isPicked, pickedByName = :pickedByName, pickedMessage = :pickedMessage, pickedAt = :pickedAt WHERE id = :itemId")
    suspend fun updateItemPickState(
        itemId: String,
        isPicked: Boolean,
        pickedByName: String,
        pickedMessage: String,
        pickedAt: Long
    )
}
