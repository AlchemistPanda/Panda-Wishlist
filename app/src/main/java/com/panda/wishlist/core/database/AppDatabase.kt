package com.panda.wishlist.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.panda.wishlist.core.database.dao.WishlistDao
import com.panda.wishlist.core.database.entity.WishItemEntity
import com.panda.wishlist.core.database.entity.WishlistEntity

@Database(
    entities = [
        WishlistEntity::class,
        WishItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wishlistDao(): WishlistDao
}
