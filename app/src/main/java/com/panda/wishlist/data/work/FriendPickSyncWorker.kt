package com.panda.wishlist.data.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.panda.wishlist.core.logging.DebugLogManager
import com.panda.wishlist.core.logging.LogLevel
import com.panda.wishlist.data.repository.WishlistRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/**
 * Periodically pulls friends' picks from ntfy and writes them into the published
 * wishlist.json. ntfy.sh forgets messages after about 12 hours, so without this a pick
 * made on the web page vanishes for everyone if the owner does not open the app in time,
 * and two friends can end up buying the same gift.
 */
class FriendPickSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun repository(): WishlistRepository
        fun debugLog(): DebugLogManager
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Deps::class.java)
        return runCatching { deps.repository().syncFriendPicksAndRepublish() }
            .fold(
                onSuccess = { count ->
                    if (count > 0) {
                        deps.debugLog().log("BackgroundSync", "Synced $count friend pick(s) in background", LogLevel.ACTION)
                    }
                    Result.success()
                },
                onFailure = { Result.retry() }
            )
    }

    companion object {
        private const val UNIQUE_NAME = "friend-pick-sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<FriendPickSyncWorker>(3, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
