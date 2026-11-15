package com.panda.wishlist

import com.panda.wishlist.core.database.entity.WishItemType
import com.panda.wishlist.core.share.ShareContentAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareContentAnalyzerTest {

    @Test
    fun `extracts Amazon link and clean title from shared product text`() {
        val raw = "Check this out: Sony WH-1000XM5 Wireless Headphones ₹29,990 https://amzn.in/d/0bXYZ123"
        val parsed = ShareContentAnalyzer.analyzeTextAndImages(
            rawText = raw,
            subject = "",
            imagePaths = emptyList()
        )
        assertEquals(WishItemType.LINK, parsed.itemType)
        assertEquals("Amazon", parsed.storeDomain)
        assertEquals("https://amzn.in/d/0bXYZ123", parsed.linkUrl)
        assertEquals("₹29,990", parsed.priceText)
        assertTrue(parsed.title.contains("Sony WH-1000XM5"))
    }

    @Test
    fun `extracts Flipkart link and subject`() {
        val parsed = ShareContentAnalyzer.analyzeTextAndImages(
            rawText = "Take a look at this Apple iPad on Flipkart\nhttps://dl.flipkart.com/s/test123!",
            subject = "Apple iPad 10th Gen",
            imagePaths = emptyList()
        )
        assertEquals(WishItemType.LINK, parsed.itemType)
        assertEquals("Flipkart", parsed.storeDomain)
        assertEquals("https://dl.flipkart.com/s/test123", parsed.linkUrl)
        assertEquals("Apple iPad 10th Gen", parsed.title)
    }

    @Test
    fun `detects Instagram and Facebook links cleanly`() {
        assertEquals("Instagram", ShareContentAnalyzer.detectStoreName("https://www.instagram.com/reel/ABC123/?igsh=xyz"))
        assertEquals("Facebook", ShareContentAnalyzer.detectStoreName("https://www.facebook.com/share/r/12345/"))
        assertEquals("Myntra", ShareContentAnalyzer.detectStoreName("https://www.myntra.com/shoes/nike/12345/buy"))
    }

    @Test
    fun `classifies plain WhatsApp text as TEXT wish`() {
        val parsed = ShareContentAnalyzer.analyzeTextAndImages(
            rawText = "Buy Nike Pegasus 41 Size UK 9 for Rs. 9,499",
            subject = "",
            imagePaths = emptyList()
        )
        assertEquals(WishItemType.TEXT, parsed.itemType)
        assertEquals("", parsed.linkUrl)
        assertEquals("Rs. 9,499", parsed.priceText)
        assertEquals("Buy Nike Pegasus 41 Size UK 9 for Rs. 9,499", parsed.noteText)
    }

    @Test
    fun `classifies image with caption as IMAGE while preserving link`() {
        val parsed = ShareContentAnalyzer.analyzeTextAndImages(
            rawText = "Birthday jacket idea https://www.ajio.com/item/123",
            subject = "",
            imagePaths = listOf("/data/user/0/com.panda.wishlist/files/wish_images/img1.jpg")
        )
        assertEquals(WishItemType.IMAGE, parsed.itemType)
        assertEquals("Ajio", parsed.storeDomain)
        assertEquals("https://www.ajio.com/item/123", parsed.linkUrl)
        assertEquals(1, parsed.localImagePaths.size)
    }
}
