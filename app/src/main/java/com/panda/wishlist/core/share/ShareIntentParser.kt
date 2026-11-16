package com.panda.wishlist.core.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import com.panda.wishlist.core.database.entity.WishItemType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedSharePayload(
    val itemType: WishItemType,
    val title: String,
    val noteText: String,
    val linkUrl: String,
    val storeDomain: String,
    val priceText: String,
    val localImagePaths: List<String>
)

object ShareContentAnalyzer {
    private val urlRegex = Regex("""https?://[^\s<>"'\)\]]+""", RegexOption.IGNORE_CASE)
    private val priceRegex = Regex("""(₹\s?[\d,]+(?:\.\d{1,2})?|Rs\.?\s?[\d,]+(?:\.\d{1,2})?|\$\s?[\d,]+(?:\.\d{2})?|INR\s?[\d,]+)""", RegexOption.IGNORE_CASE)

    fun extractFirstUrl(text: String): String {
        val match = urlRegex.find(text)?.value ?: return ""
        return match.trimEnd('.', ',', ';', ')', ']', '!', '?')
    }

    fun extractPrice(text: String): String {
        return priceRegex.find(text)?.value?.trim().orEmpty()
    }

    fun detectStoreName(url: String): String {
        if (url.isBlank()) return ""
        val host = runCatching { URI(url).host?.lowercase().orEmpty() }.getOrDefault("")
            .removePrefix("www.")
            .removePrefix("m.")
            .removePrefix("dl.")
        return when {
            host.contains("amazon.") || host.contains("amzn.") -> "Amazon"
            host.contains("flipkart.") || host.contains("fkrt.") -> "Flipkart"
            host.contains("myntra.") -> "Myntra"
            host.contains("ajio.") -> "Ajio"
            host.contains("meesho.") -> "Meesho"
            host.contains("nykaa.") -> "Nykaa"
            host.contains("instagram.") || host.contains("instagr.am") -> "Instagram"
            host.contains("facebook.") || host.contains("fb.watch") || host.contains("fb.com") -> "Facebook"
            host.contains("youtube.") || host.contains("youtu.be") -> "YouTube"
            host.contains("whatsapp.") -> "WhatsApp"
            host.contains("pinterest.") || host.contains("pin.it") -> "Pinterest"
            host.contains("twitter.") || host == "x.com" -> "X"
            host.contains("reddit.") -> "Reddit"
            host.contains("etsy.") -> "Etsy"
            host.contains("ebay.") -> "eBay"
            host.contains("croma.") -> "Croma"
            host.contains("reliancedigital.") -> "Reliance Digital"
            host.contains("tatacliq.") -> "Tata CLiQ"
            host.contains("firstcry.") -> "FirstCry"
            host.isNotBlank() -> host.substringBefore('.').replaceFirstChar { it.uppercase() }
            else -> "Web"
        }
    }

    fun analyzeTextAndImages(
        rawText: String,
        subject: String,
        imagePaths: List<String>
    ): ParsedSharePayload {
        val trimmedText = rawText.trim()
        val trimmedSubject = subject.trim()
        val url = extractFirstUrl(trimmedText).ifBlank { extractFirstUrl(trimmedSubject) }
        val store = detectStoreName(url)
        val price = extractPrice("$trimmedSubject\n$trimmedText")

        val textWithoutUrl = if (url.isNotBlank()) {
            trimmedText.replace(url, "").replace(Regex("""\n{3,}"""), "\n\n").trim()
        } else {
            trimmedText
        }

        val itemType = when {
            imagePaths.isNotEmpty() -> WishItemType.IMAGE
            url.isNotBlank() -> WishItemType.LINK
            else -> WishItemType.TEXT
        }

        val inferredTitle = when {
            trimmedSubject.isNotBlank() && !trimmedSubject.startsWith("http", ignoreCase = true) ->
                trimmedSubject.take(100)
            textWithoutUrl.isNotBlank() ->
                textWithoutUrl.lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.take(100)
                    ?: "Shared Wish"
            url.isNotBlank() ->
                if (store.isNotBlank()) "$store Wish" else "Saved Link"
            imagePaths.size > 1 -> "Shared Photos (${imagePaths.size})"
            imagePaths.size == 1 -> "Saved Photo"
            else -> "New Wish"
        }

        val cleanNote = when {
            itemType == WishItemType.TEXT -> trimmedText
            textWithoutUrl.isNotBlank() && textWithoutUrl != inferredTitle -> textWithoutUrl
            else -> ""
        }

        return ParsedSharePayload(
            itemType = itemType,
            title = inferredTitle,
            noteText = cleanNote,
            linkUrl = url,
            storeDomain = store,
            priceText = price,
            localImagePaths = imagePaths
        )
    }
}

@Singleton
class ShareIntentParser @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun isShareIntent(intent: Intent?): Boolean {
        val action = intent?.action ?: return false
        return action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE
    }

    fun parseIntent(intent: Intent?): ParsedSharePayload? {
        if (!isShareIntent(intent)) return null
        val nonNullIntent = intent ?: return null

        val extraText = nonNullIntent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        val extraSubject = nonNullIntent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()

        val clipText = buildString {
            val cd = nonNullIntent.clipData
            if (cd != null && extraText.isBlank()) {
                for (i in 0 until cd.itemCount) {
                    val itemText = cd.getItemAt(i).text?.toString().orEmpty()
                    if (itemText.isNotBlank()) {
                        if (isNotEmpty()) append('\n')
                        append(itemText)
                    }
                }
            }
        }

        val combinedText = extraText.ifBlank { clipText }
        val uris = extractUris(nonNullIntent)
        val savedPaths = uris.mapNotNull { copyUriToLocalImage(it) }

        if (combinedText.isBlank() && extraSubject.isBlank() && savedPaths.isEmpty()) {
            return null
        }

        return ShareContentAnalyzer.analyzeTextAndImages(
            rawText = combinedText,
            subject = extraSubject,
            imagePaths = savedPaths
        )
    }

    fun copyUriToLocalImage(uri: Uri): String? {
        return runCatching {
            val dir = File(context.filesDir, "wish_images").apply { mkdirs() }
            val outFile = File(dir, "wish_${UUID.randomUUID()}.jpg")

            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val maxDim = 1600
            var sampleSize = 1
            val w = boundsOptions.outWidth
            val h = boundsOptions.outHeight
            if (w > maxDim || h > maxDim) {
                while (w / sampleSize > maxDim * 2 || h / sampleSize > maxDim * 2) {
                    sampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            val scaled = scaleDownIfNeeded(bitmap, maxDim)
            if (scaled !== bitmap) bitmap.recycle()
            // BitmapFactory ignores EXIF orientation, so camera photos came out sideways.
            val upright = applyExifOrientation(scaled, readExifOrientation(uri))
            if (upright !== scaled) scaled.recycle()
            FileOutputStream(outFile).use { fos ->
                upright.compress(Bitmap.CompressFormat.JPEG, 86, fos)
            }
            upright.recycle()
            outFile.absolutePath
        }.getOrNull()
    }

    private fun readExifOrientation(uri: Uri): Int = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

    private fun applyExifOrientation(bmp: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
            else -> return bmp
        }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
    }

    private fun scaleDownIfNeeded(bmp: Bitmap, maxDim: Int): Bitmap {
        val w = bmp.width
        val h = bmp.height
        if (w <= maxDim && h <= maxDim) return bmp
        val ratio = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
        val nw = (w * ratio).toInt().coerceAtLeast(1)
        val nh = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bmp, nw, nh, true)
    }

    private fun extractUris(intent: Intent): List<Uri> {
        val result = linkedSetOf<Uri>()
        when (intent.action) {
            Intent.ACTION_SEND -> {
                getParcelableUri(intent, Intent.EXTRA_STREAM)?.let { result.add(it) }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                getParcelableUriList(intent, Intent.EXTRA_STREAM).forEach { result.add(it) }
            }
        }
        val cd = intent.clipData
        if (cd != null) {
            for (i in 0 until cd.itemCount) {
                cd.getItemAt(i).uri?.let { result.add(it) }
            }
        }
        return result.toList()
    }

    @Suppress("DEPRECATION")
    private fun getParcelableUri(intent: Intent, key: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(key, Uri::class.java)
        } else {
            intent.getParcelableExtra(key)
        }
    }

    @Suppress("DEPRECATION")
    private fun getParcelableUriList(intent: Intent, key: String): List<Uri> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(key, Uri::class.java) ?: emptyList()
        } else {
            intent.getParcelableArrayListExtra<Uri>(key) ?: emptyList()
        }
    }
}
