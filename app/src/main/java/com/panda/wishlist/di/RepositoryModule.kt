package com.panda.wishlist.di

import com.panda.wishlist.data.repository.WishlistRepository
import com.panda.wishlist.data.repository.WishlistRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWishlistRepository(
        impl: WishlistRepositoryImpl
    ): WishlistRepository
}
