package com.panda.wishlist

import com.panda.wishlist.data.update.AppStoreCatalog
import com.panda.wishlist.data.update.findLatestRelease
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateCatalogTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // Same shape as Panda App Store's live catalog.json: versions live in `releases`,
    // never on the app entry itself.
    private val catalogText = """
        {"apps":[
          {"packageName":"com.other.app","releases":[{"versionName":"9.0.0","versionCode":99,"apkUrl":"https://x/o.apk"}]},
          {"packageName":"com.panda.wishlist","name":"Panda Wishlist","iconUrl":"https://x/i.png",
           "releases":[
             {"versionName":"1.0.0","versionCode":1,"apkUrl":"https://x/b1.apk","notes":"old","size":10,"sha256":"aa"},
             {"versionName":"1.0.1","versionCode":2,"apkUrl":"https://x/b2.apk","notes":"Fixes","size":20}
           ],
           "history":[{"versionName":"1.0.0","versionCode":1}]}
        ]}
    """.trimIndent()

    @Test
    fun picksHighestBuildForOwnPackage() {
        val catalog = json.decodeFromString<AppStoreCatalog>(catalogText)
        val latest = findLatestRelease(catalog, "com.panda.wishlist")!!
        assertEquals(2, latest.versionCode)
        assertEquals("1.0.1", latest.versionName)
        assertEquals("https://x/b2.apk", latest.apkUrl)
        assertEquals("Fixes", latest.notes)
    }

    @Test
    fun unknownPackageHasNoRelease() {
        val catalog = json.decodeFromString<AppStoreCatalog>(catalogText)
        // The debug build's package must not pick up the release build's entry.
        assertNull(findLatestRelease(catalog, "com.panda.wishlist.debug"))
    }

    @Test
    fun releasesWithoutApkUrlAreIgnored() {
        val catalog = json.decodeFromString<AppStoreCatalog>(
            """{"apps":[{"packageName":"p","releases":[{"versionName":"2","versionCode":5}]}]}"""
        )
        assertNull(findLatestRelease(catalog, "p"))
    }
}
