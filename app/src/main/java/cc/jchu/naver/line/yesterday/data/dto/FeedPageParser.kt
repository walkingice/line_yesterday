package cc.jchu.naver.line.yesterday.data.dto

import cc.jchu.naver.line.yesterday.data.domain.FeedPageResult
import cc.jchu.naver.line.yesterday.data.domain.PageCursor

class FeedPageParser(
    private val dtoParser: ApiDtoParser = ApiDtoParser(),
) {
    fun parseDummyJson(rawJson: String): FeedPageResult {
        val dto = dtoParser.parseDummyJsonFeed(rawJson)
        val total = requireNotNull(dto.total) { "DummyJson feed total is required" }
        val skip = requireNotNull(dto.skip) { "DummyJson feed skip is required" }
        val limit = requireNotNull(dto.limit) { "DummyJson feed limit is required" }
        require(limit > 0) { "DummyJson feed limit must be positive" }

        val isExhausted = skip + limit >= total
        return FeedPageResult(
            items = ApiDtoMappers.run { dto.toDomainItems() },
            nextCursor = if (isExhausted) null else PageCursor((skip + limit).toString()),
            isExhausted = isExhausted,
        )
    }

    fun parseSpaceFlight(rawJson: String): FeedPageResult {
        val dto = dtoParser.parseSpaceFlightFeed(rawJson)
        val next = dto.next
        return FeedPageResult(
            items = ApiDtoMappers.run { dto.toDomainItems() },
            nextCursor = next?.let(::PageCursor),
            isExhausted = next == null,
        )
    }
}
