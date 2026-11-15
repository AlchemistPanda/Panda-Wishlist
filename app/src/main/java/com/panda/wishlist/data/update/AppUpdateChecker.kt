package com.panda.wishlist.data.update

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class CatalogAppEntry(
    val id: String = "",
    val name: String = "",
    val packageName: String = "",
    val versionCode: Int = 0,
    val versionName: String = "",
    val downloadUrl: String = "",
    val releaseNotes: String = ""
)

@Serializable
data class AppStoreCatalog(
    val apps: List<CatalogAppEntry> = emptyList()
)

data class UpdateCheckResult(
    val hasUpdate: Boolean,
    val currentVersion: String,
    val currentBuild: Int,
    val latestVersion: String,
    val latestBuild: Int,
    val releaseNotes: String,
    val downloadUrl: String
)

@Singleton
class AppUpdateChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun checkForUpdates(): UpdateCheckResult? = withContext(Dispatchers.IO) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val currentVersion = packageInfo.versionName ?: "1.0.0"
            @Suppress("DEPRECATION")
            val currentBuild = packageInfo.versionCode

            val request = Request.Builder()
                .url(CATALOG_URL)
                .build()

            val body = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body?.string().orEmpty()
            }

            if (body.isBlank()) return@withContext null

            val catalog = json.decodeFromString<AppStoreCatalog>(body)
            val appEntry = catalog.apps.find {
                it.packageName == context.packageName ||
                    it.name.contains("Panda Wishlist", ignoreCase = true)
            } ?: return@withContext UpdateCheckResult(
                hasUpdate = false,
                currentVersion = currentVersion,
                currentBuild = currentBuild,
                latestVersion = currentVersion,
                latestBuild = currentBuild,
                releaseNotes = "Up to date.",
                downloadUrl = ""
            )

            val hasUpdate = appEntry.versionCode > currentBuild

            UpdateCheckResult(
                hasUpdate = hasUpdate,
                currentVersion = currentVersion,
                currentBuild = currentBuild,
                latestVersion = appEntry.versionName,
                latestBuild = appEntry.versionCode,
                releaseNotes = appEntry.releaseNotes,
                downloadUrl = appEntry.downloadUrl
            )
        } catch (e: Exception) {
            Log.w(TAG, "Update check failed: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "AppUpdateChecker"
        private const val CATALOG_URL =
            "https://raw.githubusercontent.com/AlchemistPanda/Panda-App-store/store/catalog.json"
    }
}
