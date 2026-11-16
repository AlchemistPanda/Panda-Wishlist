package com.panda.wishlist

import android.app.Application
import com.panda.wishlist.data.work.FriendPickSyncWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PandaWishlistApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FriendPickSyncWorker.schedule(this)
    }
}
