package com.panda.wishlist.core.share

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

data class LinkMetadata(
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val priceText: String = ""
)

@Singleton
class LinkMetadataExtractor @Inject constructor(
    private val client: OkHttpClient
) {
    suspend fun fetchMetadata(url: String): LinkMetadata = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext LinkMetadata()
        runCatching {
            val req = Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                )
                .header("Accept-Language", "en-IN,en-US;q=0.9,en;q=0.8")
                .build()

            val html = client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext LinkMetadata()
                resp.body?.string().orEmpty()
            }
            if (html.isBlank()) return@withContext LinkMetadata()

            val doc = Jsoup.parse(html, url)
            val ogTitle = doc.selectFirst("meta[property=og:title]")?.attr("content").orEmpty().trim()
            val twitterTitle = doc.selectFirst("meta[name=twitter:title]")?.attr("content").orEmpty().trim()
            val pageTitle = doc.title().trim()
            val title = listOf(ogTitle, twitterTitle, pageTitle).firstOrNull {
                it.isNotBlank() && !it.equals("Robot Check", ignoreCase = true)
            }.orEmpty()

            val ogDesc = doc.selectFirst("meta[property=og:description]")?.attr("content").orEmpty().trim()
            val metaDesc = doc.selectFirst("meta[name=description]")?.attr("content").orEmpty().trim()
            val desc = ogDesc.ifBlank { metaDesc }

            val ogImg = doc.selectFirst("meta[property=og:image]")?.attr("abs:content").orEmpty().trim()
            val twImg = doc.selectFirst("meta[name=twitter:image]")?.attr("abs:content").orEmpty().trim()
            val img = ogImg.ifBlank { twImg }

            val priceMeta = doc.selectFirst("meta[property=product:price:amount]")?.attr("content").orEmpty().trim()
            val currencyMeta = doc.selectFirst("meta[property=product:price:currency]")?.attr("content").orEmpty().trim()
            val formattedMetaPrice = if (priceMeta.isNotBlank()) {
                val sym = if (currencyMeta == "INR" || currencyMeta.isBlank()) "₹" else "$currencyMeta "
                "$sym$priceMeta"
            } else ""

            val price = formattedMetaPrice
                .ifBlank { ShareContentAnalyzer.extractPrice(title) }
                .ifBlank { ShareContentAnalyzer.extractPrice(desc) }

            LinkMetadata(
                title = title.take(140),
                description = desc.take(260),
                imageUrl = img,
                priceText = price
            )
        }.getOrDefault(LinkMetadata())
    }
}
