package cc.jchu.naver.line.yesterday.data.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedPageParserTest {
    private val parser = FeedPageParser()

    @Test
    fun parsesDummyJsonNextCursorWithoutIncrementingOpaqueValue() {
        val result = parser.parseDummyJson(resource("dummy_json_feeds.json"))

        assertEquals("10", result.nextCursor?.value)
        assertFalse(result.isExhausted)
        assertEquals(10, result.items.size)
    }

    @Test
    fun marksDummyJsonLastPageAsExhausted() {
        val result = parser.parseDummyJson(
            """
            {
              "products": [],
              "total": 10,
              "skip": 10,
              "limit": 10
            }
            """.trimIndent(),
        )

        assertNull(result.nextCursor)
        assertTrue(result.isExhausted)
    }

    @Test
    fun parsesSpaceFlightNextUrlAsOpaqueCursor() {
        val result = parser.parseSpaceFlight(resource("space_flight_feeds.json"))

        assertEquals(
            "https://api.spaceflightnewsapi.net/v4/articles/?format=json&limit=10&offset=10",
            result.nextCursor?.value,
        )
        assertFalse(result.isExhausted)
    }

    @Test
    fun marksSpaceFlightEmptyLastPageAsExhausted() {
        val result = parser.parseSpaceFlight(
            """
            {
              "count": 10,
              "next": null,
              "previous": "previous",
              "results": []
            }
            """.trimIndent(),
        )

        assertTrue(result.items.isEmpty())
        assertNull(result.nextCursor)
        assertTrue(result.isExhausted)
    }

    private fun resource(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource(name)).readText()
}
