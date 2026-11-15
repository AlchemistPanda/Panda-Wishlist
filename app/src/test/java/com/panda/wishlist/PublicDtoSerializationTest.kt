package com.panda.wishlist

import com.panda.wishlist.data.sync.PublicWishItemDto
import com.panda.wishlist.data.sync.PublicWishlistDto
import com.panda.wishlist.data.sync.WishClaimEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PublicDtoSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `serializes and deserializes PublicWishlistDto cleanly`() {
        val dto = PublicWishlistDto(
            id = "list123",
            name = "Birthday Wishes",
            description = "Ideas for Nov",
            emoji = "🎂",
            ownerName = "Manu",
            ntfyTopic = "panda-wishlist-claim-list123",
            updatedAt = 1794735000000L,
            items = listOf(
                PublicWishItemDto(
                    id = "item1",
                    itemType = "LINK",
                    title = "Casio G-Shock",
                    linkUrl = "https://www.amazon.in/dp/B00000",
                    storeDomain = "Amazon",
                    priceText = "₹7,995",
                    priority = 2
                )
            )
        )
        val encoded = json.encodeToString(dto)
        val decoded = json.decodeFromString<PublicWishlistDto>(encoded)
        assertEquals("Birthday Wishes", decoded.name)
        assertEquals(1, decoded.items.size)
        assertEquals("Amazon", decoded.items.first().storeDomain)
    }

    @Test
    fun `parses secret surprise claim event from browser`() {
        val raw = """{"action":"pick","itemId":"item1","pickedByName":"Secret Surprise 🤫","isSecret":true,"message":"Surprise!","timestamp":1794735600000}"""
        val event = json.decodeFromString<WishClaimEvent>(raw)
        assertEquals("pick", event.action)
        assertEquals("item1", event.itemId)
        assertTrue(event.isSecret)
        assertEquals("Surprise!", event.message)
    }
}
