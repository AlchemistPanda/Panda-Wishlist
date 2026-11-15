package com.panda.wishlist.data.sync

import android.util.Base64
import com.panda.wishlist.BuildConfig
import com.panda.wishlist.core.database.entity.WishItemEntity
import com.panda.wishlist.core.database.entity.WishlistEntity
import com.panda.wishlist.core.logging.DebugLogManager
import com.panda.wishlist.core.logging.LogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class PublicWishItemDto(
    val id: String,
    val itemType: String,
    val title: String,
    val noteText: String = "",
    val linkUrl: String = "",
    val storeDomain: String = "",
    val priceText: String = "",
    val priority: Int = 1,
    val imagePath: String = "",
    val remoteImageUrl: String = "",
    val isPicked: Boolean = false,
    val pickedByName: String = "",
    val pickedMessage: String = "",
    val pickedAt: Long = 0L,
    val createdAt: Long = 0L
)

@Serializable
data class PublicWishlistDto(
    val id: String,
    val name: String,
    val description: String = "",
    val emoji: String = "🎁",
    val ownerName: String = "Manu",
    val ntfyTopic: String,
    val updatedAt: Long,
    val items: List<PublicWishItemDto>
)

@Serializable
private data class GitHubContentMeta(
    val sha: String = ""
)

@Serializable
private data class GitHubPutBody(
    val message: String,
    val content: String,
    val branch: String = "gh-pages",
    val sha: String? = null
)

@Serializable
private data class GitHubDeleteBody(
    val message: String,
    val sha: String,
    val branch: String = "gh-pages"
)

@Serializable
data class GitHubUserProfile(
    val login: String = ""
)

sealed interface SyncResult {
    data class Success(val shareUrl: String) : SyncResult
    data class Error(val message: String) : SyncResult
}

@Singleton
class GitHubWishlistSyncService @Inject constructor(
    private val client: OkHttpClient,
    private val tokenStore: GitHubTokenStore,
    private val debugLog: DebugLogManager
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    fun buildPublicShareUrl(wishlistId: String): String {
        return "${BuildConfig.GITHUB_PAGES_BASE}?id=$wishlistId"
    }

    fun buildNtfyTopic(wishlistId: String): String {
        return "panda-wishlist-claim-$wishlistId"
    }

    suspend fun verifyConnection(): String? = withContext(Dispatchers.IO) {
        val token = tokenStore.getEffectiveToken()
        if (token.isBlank()) return@withContext null
        runCatching {
            val req = Request.Builder()
                .url("https://api.github.com/user")
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string().orEmpty()
                json.decodeFromString<GitHubUserProfile>(body).login.ifBlank { null }
            }
        }.getOrNull()
    }

    suspend fun publishWishlist(
        wishlist: WishlistEntity,
        items: List<WishItemEntity>,
        ownerName: String
    ): SyncResult = withContext(Dispatchers.IO) {
        val token = tokenStore.getEffectiveToken()
        if (token.isBlank()) {
            return@withContext SyncResult.Error("GitHub token is missing. Configure it in Settings.")
        }

        val topic = wishlist.ntfyTopic.ifBlank { buildNtfyTopic(wishlist.id) }
        val itemDtos = mutableListOf<PublicWishItemDto>()

        for (item in items) {
            var relativeImagePath = ""
            if (item.localImagePath.isNotBlank()) {
                val file = File(item.localImagePath)
                if (file.exists() && file.length() > 0L) {
                    val repoImgPath = "lists/${wishlist.id}/images/${item.id}.jpg"
                    val uploaded = ensureFileUploaded(
                        token = token,
                        repoPath = repoImgPath,
                        bytes = file.readBytes(),
                        commitMsg = "Upload wish image ${item.id}",
                        skipIfExists = true
                    )
                    if (uploaded) {
                        relativeImagePath = "images/${item.id}.jpg"
                    }
                }
            }

            itemDtos.add(
                PublicWishItemDto(
                    id = item.id,
                    itemType = item.itemType,
                    title = item.title,
                    noteText = item.noteText,
                    linkUrl = item.linkUrl,
                    storeDomain = item.storeDomain,
                    priceText = item.priceText,
                    priority = item.priority,
                    imagePath = relativeImagePath,
                    remoteImageUrl = item.remoteImageUrl,
                    isPicked = item.isPicked,
                    pickedByName = item.pickedByName,
                    pickedMessage = item.pickedMessage,
                    pickedAt = item.pickedAt,
                    createdAt = item.createdAt
                )
            )
        }

        val payload = PublicWishlistDto(
            id = wishlist.id,
            name = wishlist.name,
            description = wishlist.description,
            emoji = wishlist.emoji,
            ownerName = ownerName,
            ntfyTopic = topic,
            updatedAt = System.currentTimeMillis(),
            items = itemDtos
        )

        val jsonText = json.encodeToString(payload)
        val ok = ensureFileUploaded(
            token = token,
            repoPath = "lists/${wishlist.id}/wishlist.json",
            bytes = jsonText.toByteArray(Charsets.UTF_8),
            commitMsg = "Sync wishlist ${wishlist.name} (${items.size} items)",
            skipIfExists = false
        )

        if (ok) {
            val url = buildPublicShareUrl(wishlist.id)
            debugLog.log("GitHubSync", "Published '${wishlist.name}' (${items.size} wishes) -> $url", LogLevel.ACTION)
            SyncResult.Success(url)
        } else {
            debugLog.log("GitHubSync", "Failed to publish '${wishlist.name}'", LogLevel.ALERT)
            SyncResult.Error("Could not sync to GitHub Pages. Check internet or token.")
        }
    }

    suspend fun deleteItemAsset(wishlistId: String, itemId: String) = withContext(Dispatchers.IO) {
        val token = tokenStore.getEffectiveToken()
        if (token.isBlank()) return@withContext
        val path = "lists/$wishlistId/images/$itemId.jpg"
        deleteFileIfExists(token, path, "Remove deleted wish image $itemId")
    }

    suspend fun unpublishWishlist(wishlistId: String, itemIds: List<String>) = withContext(Dispatchers.IO) {
        val token = tokenStore.getEffectiveToken()
        if (token.isBlank()) return@withContext
        itemIds.forEach { itemId ->
            deleteFileIfExists(token, "lists/$wishlistId/images/$itemId.jpg", "Unpublish image $itemId")
        }
        deleteFileIfExists(token, "lists/$wishlistId/wishlist.json", "Unpublish wishlist $wishlistId")
        debugLog.log("GitHubSync", "Unpublished wishlist $wishlistId from GitHub Pages", LogLevel.ACTION)
    }

    private fun ensureFileUploaded(
        token: String,
        repoPath: String,
        bytes: ByteArray,
        commitMsg: String,
        skipIfExists: Boolean
    ): Boolean {
        val apiUrl = "https://api.github.com/repos/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/contents/$repoPath"
        val existingSha = fetchExistingSha(token, apiUrl)
        if (skipIfExists && existingSha != null) {
            return true
        }

        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        val bodyObj = GitHubPutBody(
            message = commitMsg,
            content = b64,
            branch = "gh-pages",
            sha = existingSha
        )

        val attempt1 = executePut(token, apiUrl, json.encodeToString(bodyObj))
        if (attempt1 == 200 || attempt1 == 201) return true

        if (attempt1 == 409) {
            val freshSha = fetchExistingSha(token, apiUrl)
            val retryBody = bodyObj.copy(sha = freshSha)
            val attempt2 = executePut(token, apiUrl, json.encodeToString(retryBody))
            return attempt2 == 200 || attempt2 == 201
        }

        debugLog.log("GitHubSync", "PUT $repoPath returned HTTP $attempt1", LogLevel.WARN)
        return false
    }

    private fun executePut(token: String, apiUrl: String, jsonBody: String): Int {
        return runCatching {
            val req = Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .put(jsonBody.toRequestBody(jsonMedia))
                .build()
            client.newCall(req).execute().use { it.code }
        }.getOrDefault(-1)
    }

    private fun fetchExistingSha(token: String, apiUrl: String): String? {
        return runCatching {
            val req = Request.Builder()
                .url("$apiUrl?ref=gh-pages")
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string().orEmpty()
                json.decodeFromString<GitHubContentMeta>(body).sha.ifBlank { null }
            }
        }.getOrNull()
    }

    private fun deleteFileIfExists(token: String, repoPath: String, message: String) {
        val apiUrl = "https://api.github.com/repos/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/contents/$repoPath"
        val sha = fetchExistingSha(token, apiUrl) ?: return
        val bodyStr = json.encodeToString(GitHubDeleteBody(message = message, sha = sha, branch = "gh-pages"))
        runCatching {
            val req = Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .delete(bodyStr.toRequestBody(jsonMedia))
                .build()
            client.newCall(req).execute().close()
        }
    }
}
