package cc.jchu.naver.line.yesterday.data.domain

enum class FeedSource {
    DUMMY_JSON,
    SPACE_FLIGHT,
}

@JvmInline
value class PageCursor(val value: String)

sealed interface FeedItem {
    val id: String
    val source: FeedSource
    val title: String
    val imgUrl: String
}

data class DummyJsonItem(
    override val id: String,
    override val title: String,
    override val imgUrl: String,
    val category: String,
) : FeedItem {
    override val source: FeedSource = FeedSource.DUMMY_JSON
}

data class SpaceFlightItem(
    override val id: String,
    override val title: String,
    override val imgUrl: String,
    val description: String,
) : FeedItem {
    override val source: FeedSource = FeedSource.SPACE_FLIGHT
}

data class Detail(
    val id: String,
    val source: FeedSource,
    val title: String,
    val imgUrl: String,
    val description: String,
    val extraInformation: String,
)
