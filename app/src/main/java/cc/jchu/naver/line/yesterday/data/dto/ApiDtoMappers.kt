package cc.jchu.naver.line.yesterday.data.dto

import cc.jchu.naver.line.yesterday.data.domain.Detail
import cc.jchu.naver.line.yesterday.data.domain.DummyJsonItem
import cc.jchu.naver.line.yesterday.data.domain.FeedItem
import cc.jchu.naver.line.yesterday.data.domain.FeedSource
import cc.jchu.naver.line.yesterday.data.domain.SpaceFlightItem

object ApiDtoMappers {
    fun DummyJsonFeedDto.toDomainItems(): List<FeedItem> =
        products.orEmpty().map { it.toDomainItem() }

    fun DummyJsonProductDto.toDomainItem(): DummyJsonItem = DummyJsonItem(
        id = requireNotNull(id) { "DummyJson product id is required" }.toString(),
        title = requireNotNull(title) { "DummyJson product title is required" },
        imgUrl = requireNotNull(thumbnail) { "DummyJson product thumbnail is required" },
        category = requireNotNull(category) { "DummyJson product category is required" },
    )

    fun DummyJsonProductDto.toDetail(): Detail = Detail(
        id = requireNotNull(id) { "DummyJson product id is required" }.toString(),
        source = FeedSource.DUMMY_JSON,
        title = requireNotNull(title) { "DummyJson product title is required" },
        imgUrl = requireNotNull(thumbnail) { "DummyJson product thumbnail is required" },
        description = requireNotNull(description) { "DummyJson product description is required" },
        extraInformation = requireNotNull(category) { "DummyJson product category is required" },
    )

    fun SpaceFlightFeedDto.toDomainItems(): List<FeedItem> =
        results.orEmpty().map { it.toDomainItem() }

    fun SpaceFlightArticleDto.toDomainItem(): SpaceFlightItem = SpaceFlightItem(
        id = requireNotNull(id) { "SpaceFlight article id is required" }.toString(),
        title = requireNotNull(title) { "SpaceFlight article title is required" },
        imgUrl = requireNotNull(imageUrl) { "SpaceFlight article image URL is required" },
        description = requireNotNull(summary) { "SpaceFlight article summary is required" },
    )

    fun SpaceFlightArticleDto.toDetail(): Detail = Detail(
        id = requireNotNull(id) { "SpaceFlight article id is required" }.toString(),
        source = FeedSource.SPACE_FLIGHT,
        title = requireNotNull(title) { "SpaceFlight article title is required" },
        imgUrl = requireNotNull(imageUrl) { "SpaceFlight article image URL is required" },
        description = requireNotNull(summary) { "SpaceFlight article summary is required" },
        extraInformation = requireNotNull(newsSite) { "SpaceFlight article news site is required" },
    )
}
