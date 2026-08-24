package cc.jchu.naver.line.yesterday.data.dto

import cc.jchu.naver.line.yesterday.data.domain.FeedSource
import com.google.gson.JsonSyntaxException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ApiDtoParserMapperTest {
    private val parser = ApiDtoParser()

    @Test
    fun parsesAndMapsFeedFixtures() {
        val dummy = parser.parseDummyJsonFeed(resource("dummy_json_feeds.json"))
            .let { ApiDtoMappers.run { it.toDomainItems() } }
        val spaceFlight = parser.parseSpaceFlightFeed(resource("space_flight_feeds.json"))
            .let { ApiDtoMappers.run { it.toDomainItems() } }

        assertEquals("1", dummy.first().id)
        assertEquals("39646", spaceFlight.first().id)
        assertEquals("Essence Mascara Lash Princess", dummy.first().title)
        assertEquals("Tory Bruno Named Honorary Chair Of World Space Week 2027", spaceFlight.first().title)
    }

    @Test
    fun mapsSpaceFlightDetailFixtureToDomainDetail() {
        val detail = parser.parseSpaceFlightArticle(resource("space_flight_article.json"))
            .let { ApiDtoMappers.run { it.toDetail() } }

        assertEquals("39615", detail.id)
        assertEquals(FeedSource.SPACE_FLIGHT, detail.source)
        assertEquals("Arstechnica", detail.extraInformation)
    }

    @Test
    fun emptyFeedMapsToEmptyDomainList() {
        val dto = DummyJsonFeedDto(products = emptyList(), total = 0, skip = 0, limit = 10)

        assertEquals(emptyList<Any>(), ApiDtoMappers.run { dto.toDomainItems() })
    }

    @Test
    fun malformedJsonIsRejectedByParser() {
        assertThrows(JsonSyntaxException::class.java) {
            parser.parseDummyJsonFeed("{not-json}")
        }
    }

    @Test
    fun missingRequiredFieldIsRejectedByMapper() {
        val dto = DummyJsonProductDto(
            id = 1,
            title = "Title",
            description = "Description",
            category = "beauty",
            price = null,
            discountPercentage = null,
            rating = null,
            stock = null,
            tags = null,
            brand = null,
            sku = null,
            weight = null,
            dimensions = null,
            warrantyInformation = null,
            shippingInformation = null,
            availabilityStatus = null,
            reviews = null,
            returnPolicy = null,
            minimumOrderQuantity = null,
            meta = null,
            images = null,
            thumbnail = null,
            message = null,
        )

        assertThrows(IllegalArgumentException::class.java) {
            ApiDtoMappers.run { dto.toDomainItem() }
        }
    }

    @Test
    fun sameIdFromDifferentSourcesRemainsDistinct() {
        val dummy = DummyJsonItemFactory.create(id = 1)
        val spaceFlight = SpaceFlightArticleDto(
            id = 1,
            title = "Article",
            authors = null,
            url = null,
            imageUrl = "image",
            newsSite = "site",
            summary = "summary",
            publishedAt = null,
            updatedAt = null,
            featured = null,
            launches = null,
            events = null,
        ).let { ApiDtoMappers.run { it.toDomainItem() } }

        assertEquals(dummy.id, spaceFlight.id)
        assertNotEquals(dummy.source to dummy.id, spaceFlight.source to spaceFlight.id)
    }

    @Test
    fun mapperPreservesDuplicateIdsForLaterSequenceDeduplication() {
        val dto = DummyJsonFeedDto(
            products = listOf(
                DummyJsonItemFactory.dto(id = 1),
                DummyJsonItemFactory.dto(id = 1),
            ),
            total = 2,
            skip = 0,
            limit = 2,
        )
        val items = ApiDtoMappers.run { dto.toDomainItems() }

        assertEquals(listOf("1", "1"), items.map { it.id })
    }

    private fun resource(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource(name)).readText()
}

private object DummyJsonItemFactory {
    fun dto(id: Int) = DummyJsonProductDto(
        id = id,
        title = "Product",
        description = "Description",
        category = "category",
        price = null,
        discountPercentage = null,
        rating = null,
        stock = null,
        tags = null,
        brand = null,
        sku = null,
        weight = null,
        dimensions = null,
        warrantyInformation = null,
        shippingInformation = null,
        availabilityStatus = null,
        reviews = null,
        returnPolicy = null,
        minimumOrderQuantity = null,
        meta = null,
        images = null,
        thumbnail = "image",
        message = null,
    )

    fun create(id: Int) = dto(id).let { ApiDtoMappers.run { it.toDomainItem() } }
}
