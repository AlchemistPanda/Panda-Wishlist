package com.panda.wishlist.data.sync

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.panda.wishlist.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.wishlistDataStore by preferencesDataStore(name = "panda_wishlist_prefs")

data class AppSettings(
    val ownerDisplayName: String = "Manu",
    val lastUsedWishlistId: String = "",
    val autoSyncOnChange: Boolean = true,
    val autoUpdateCheckEnabled: Boolean = true,
    val customGithubToken: String = "",
    val hasSeededDefaults: Boolean = false
)

@Singleton
class GitHubTokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val OWNER_NAME = stringPreferencesKey("owner_display_name")
        val LAST_WISHLIST_ID = stringPreferencesKey("last_wishlist_id")
        val AUTO_SYNC = booleanPreferencesKey("auto_sync_on_change")
        val AUTO_UPDATE_CHECK = booleanPreferencesKey("auto_update_check")
        val CUSTOM_GH_TOKEN = stringPreferencesKey("custom_gh_token")
        val HAS_SEEDED = booleanPreferencesKey("has_seeded_defaults")
    }

    val settingsFlow: Flow<AppSettings> = context.wishlistDataStore.data.map { prefs ->
        AppSettings(
            ownerDisplayName = prefs[Keys.OWNER_NAME] ?: "Manu",
            lastUsedWishlistId = prefs[Keys.LAST_WISHLIST_ID] ?: "",
            autoSyncOnChange = prefs[Keys.AUTO_SYNC] ?: true,
            autoUpdateCheckEnabled = prefs[Keys.AUTO_UPDATE_CHECK] ?: true,
            customGithubToken = prefs[Keys.CUSTOM_GH_TOKEN] ?: "",
            hasSeededDefaults = prefs[Keys.HAS_SEEDED] ?: false
        )
    }

    fun getDefaultBuiltInToken(): String {
        val bytes = BuildConfig.DEFAULT_GH_BYTES
        val key = BuildConfig.DEFAULT_GH_KEY
        if (bytes.isEmpty()) return ""
        return bytes.map { (it xor key).toByte() }.toByteArray().toString(Charsets.UTF_8)
    }

    suspend fun getEffectiveToken(): String {
        val custom = context.wishlistDataStore.data.first()[Keys.CUSTOM_GH_TOKEN].orEmpty().trim()
        return custom.ifBlank { getDefaultBuiltInToken() }
    }

    suspend fun isUsingBuiltInToken(): Boolean {
        val custom = context.wishlistDataStore.data.first()[Keys.CUSTOM_GH_TOKEN].orEmpty().trim()
        return custom.isBlank() && getDefaultBuiltInToken().isNotBlank()
    }

    suspend fun setCustomToken(token: String) {
        context.wishlistDataStore.edit { it[Keys.CUSTOM_GH_TOKEN] = token.trim() }
    }

    suspend fun setOwnerDisplayName(name: String) {
        context.wishlistDataStore.edit { it[Keys.OWNER_NAME] = name.trim().ifBlank { "Manu" } }
    }

    suspend fun setLastUsedWishlistId(id: String) {
        context.wishlistDataStore.edit { it[Keys.LAST_WISHLIST_ID] = id }
    }

    suspend fun setAutoSyncOnChange(enabled: Boolean) {
        context.wishlistDataStore.edit { it[Keys.AUTO_SYNC] = enabled }
    }

    suspend fun setAutoUpdateCheckEnabled(enabled: Boolean) {
        context.wishlistDataStore.edit { it[Keys.AUTO_UPDATE_CHECK] = enabled }
    }

    suspend fun markSeededDefaults() {
        context.wishlistDataStore.edit { it[Keys.HAS_SEEDED] = true }
    }
}
