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
data class CatalogRelease(
    val versionName: String = "",
    val versionCode: Int = 0,
    val apkUrl: String = "",
    val notes: String = "",
    val size: Long = 0L
)

@Serializable
data class CatalogAppEntry(
    val packageName: String = "",
    val name: String = "",
    val releases: List<CatalogRelease> = emptyList()
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

/**
 * The newest release of [packageName] in Panda App Store's catalog, or null when the
 * app or a usable release is missing. Releases are compared by build number
 * (versionCode), never by the version string. Pure, so it is unit-tested.
 */
fun findLatestRelease(catalog: AppStoreCatalog, packageName: String): CatalogRelease? =
    catalog.apps.firstOrNull { it.packageName == packageName }
        ?.releases
        ?.filter { it.versionCode > 0 && it.apkUrl.isNotBlank() }
        ?.maxByOrNull { it.versionCode }

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
            val latest = findLatestRelease(catalog, context.packageName)

            UpdateCheckResult(
                hasUpdate = latest != null && latest.versionCode > currentBuild,
                currentVersion = currentVersion,
                currentBuild = currentBuild,
                latestVersion = latest?.versionName ?: currentVersion,
                latestBuild = latest?.versionCode ?: currentBuild,
                releaseNotes = latest?.notes.orEmpty(),
                downloadUrl = latest?.apkUrl.orEmpty()
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
