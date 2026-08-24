package cc.jchu.naver.line.yesterday.data.dto

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ApiDtosTest {
    private val gson = Gson()

    @Test
    fun parsesDummyJsonFeedFixture() {
        val dto = gson.fromJson(resource("dummy_json_feeds.json"), DummyJsonFeedDto::class.java)

        assertEquals(10, dto.products?.size)
        assertEquals(1, dto.products?.first()?.id)
        assertEquals("beauty", dto.products?.first()?.category)
        assertEquals(194, dto.total)
        assertEquals(0, dto.skip)
    }

    @Test
    fun parsesDummyJsonDetailFixture() {
        val dto = gson.fromJson(resource("dummy_json_product.json"), DummyJsonProductDto::class.java)

        assertEquals("Product with id '0' not found", dto.message)
        assertNull(dto.id)
    }

    @Test
    fun parsesSpaceFlightFeedFixture() {
        val dto = gson.fromJson(resource("space_flight_feeds.json"), SpaceFlightFeedDto::class.java)

        assertEquals(10, dto.results?.size)
        assertEquals(39646, dto.results?.first()?.id)
        assertEquals(
            "https://api.spaceflightnewsapi.net/v4/articles/?format=json&limit=10&offset=10",
            dto.next,
        )
        assertEquals("World Space Week Association", dto.results?.first()?.authors?.first()?.name)
    }

    @Test
    fun parsesSpaceFlightDetailFixture() {
        val dto = gson.fromJson(resource("space_flight_article.json"), SpaceFlightArticleDto::class.java)

        assertNotNull(dto)
        assertEquals(39615, dto.id)
        assertEquals("Arstechnica", dto.newsSite)
        assertEquals("Eric Berger", dto.authors?.first()?.name)
        assertEquals("https://cdn.arstechnica.net/wp-content/uploads/2025/03/GlYHDKoWQAAxQiG.jpg", dto.imageUrl)
    }

    private fun resource(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource(name)).readText()
}
