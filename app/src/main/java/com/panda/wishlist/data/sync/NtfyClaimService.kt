package com.panda.wishlist.data.sync

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
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class NtfyEnvelope(
    val event: String = "",
    val time: Long = 0L,
    val message: String = ""
)

@Serializable
data class WishClaimEvent(
    val action: String = "pick", // "pick" or "unpick"
    val itemId: String = "",
    val pickedByName: String = "",
    val isSecret: Boolean = false,
    val message: String = "",
    val timestamp: Long = 0L
)

@Singleton
class NtfyClaimService @Inject constructor(
    private val client: OkHttpClient,
    private val debugLog: DebugLogManager
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val textMedia = "text/plain; charset=utf-8".toMediaType()

    suspend fun fetchLatestClaims(topic: String): Map<String, WishClaimEvent> = withContext(Dispatchers.IO) {
        if (topic.isBlank()) return@withContext emptyMap()
        val result = mutableMapOf<String, WishClaimEvent>()
        runCatching {
            val req = Request.Builder()
                .url("https://ntfy.sh/$topic/json?poll=1&since=all")
                .build()
            val body = client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyMap()
                resp.body?.string().orEmpty()
            }
            for (line in body.lineSequence()) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue
                runCatching {
                    val env = json.decodeFromString<NtfyEnvelope>(trimmed)
                    if (env.event == "message" && env.message.isNotBlank()) {
                        val claim = json.decodeFromString<WishClaimEvent>(env.message)
                        if (claim.itemId.isNotBlank()) {
                            val ts = if (claim.timestamp > 0L) claim.timestamp else env.time * 1000L
                            val normalized = claim.copy(timestamp = ts)
                            val prev = result[claim.itemId]
                            if (prev == null || normalized.timestamp >= prev.timestamp) {
                                result[claim.itemId] = normalized
                            }
                        }
                    }
                }
            }
        }.onFailure {
            debugLog.log("NtfyClaims", "Poll failed for $topic: ${it.message}", LogLevel.WARN)
        }
        result
    }

    suspend fun postClaimEvent(topic: String, event: WishClaimEvent): Boolean = withContext(Dispatchers.IO) {
        if (topic.isBlank()) return@withContext false
        runCatching {
            val payload = json.encodeToString(event)
            val req = Request.Builder()
                .url("https://ntfy.sh/$topic")
                .post(payload.toRequestBody(textMedia))
                .build()
            client.newCall(req).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }
}
