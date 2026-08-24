package cc.jchu.naver.line.yesterday.data.repository

import cc.jchu.naver.line.yesterday.data.domain.Detail
import cc.jchu.naver.line.yesterday.data.domain.DummyJsonItem
import cc.jchu.naver.line.yesterday.data.domain.FeedItem
import cc.jchu.naver.line.yesterday.data.domain.FeedSource
import cc.jchu.naver.line.yesterday.data.domain.SpaceFlightItem
import cc.jchu.naver.line.yesterday.data.favorite.FavoriteEntry
import cc.jchu.naver.line.yesterday.data.favorite.FavoriteStore
import cc.jchu.naver.line.yesterday.data.provider.TimeProvider

interface FavoriteReader {
    suspend fun isFavorite(source: FeedSource, id: String): Boolean
    suspend fun toggle(detail: Detail): Boolean
    suspend fun updateSnapshot(detail: Detail)
}

class FavoritesRepository(
    private val favoriteStore: FavoriteStore,
    private val timeProvider: TimeProvider,
) : FavoriteReader {
    override suspend fun isFavorite(source: FeedSource, id: String): Boolean =
        favoriteStore.get(source, id) != null

    suspend fun add(item: FeedItem) {
        add(item, timeProvider.getCurrentTimeMillis())
    }

    suspend fun add(item: FeedItem, addedAt: Long) {
        favoriteStore.save(item.toEntry(addedAt))
    }

    suspend fun remove(source: FeedSource, id: String) {
        favoriteStore.delete(source, id)
    }

    suspend fun toggle(item: FeedItem): Boolean {
        return if (isFavorite(item.source, item.id)) {
            remove(item.source, item.id)
            false
        } else {
            add(item)
            true
        }
    }

    override suspend fun toggle(detail: Detail): Boolean {
        return if (isFavorite(detail.source, detail.id)) {
            remove(detail.source, detail.id)
            false
        } else {
            add(detail.toFeedItem())
            true
        }
    }

    suspend fun getPage(offset: Int, limit: Int = FAVORITES_PAGE_SIZE): List<FeedItem> {
        require(offset >= 0) { "Offset must not be negative" }
        require(limit > 0) { "Limit must be positive" }
        return favoriteStore.getAll().drop(offset).take(limit).map { it.toItem() }
    }

    suspend fun getAll(): List<FeedItem> = favoriteStore.getAll().map { it.toItem() }

    suspend fun count(): Int = favoriteStore.getAll().size

    override suspend fun updateSnapshot(detail: Detail) {
        val current = favoriteStore.get(detail.source, detail.id) ?: return
        favoriteStore.save(
            current.copy(
                title = detail.title,
                imgUrl = detail.imgUrl,
                description = detail.description,
                extraInformation = detail.extraInformation,
            ),
        )
    }

    private fun Detail.toFeedItem(): FeedItem = when (source) {
        FeedSource.DUMMY_JSON -> DummyJsonItem(id, title, imgUrl, extraInformation)
        FeedSource.SPACE_FLIGHT -> SpaceFlightItem(id, title, imgUrl, description)
    }

    private fun FeedItem.toEntry(addedAt: Long): FavoriteEntry = FavoriteEntry(
        source = source,
        itemId = id,
        addedAt = addedAt,
        title = title,
        imgUrl = imgUrl,
        description = when (this) {
            is DummyJsonItem -> ""
            is SpaceFlightItem -> description
        },
        extraInformation = when (this) {
            is DummyJsonItem -> category
            is SpaceFlightItem -> description
        },
    )

    private fun FavoriteEntry.toItem(): FeedItem = when (source) {
        FeedSource.DUMMY_JSON -> DummyJsonItem(itemId, title, imgUrl, extraInformation)
        FeedSource.SPACE_FLIGHT -> SpaceFlightItem(itemId, title, imgUrl, description)
    }

    companion object {
        const val FAVORITES_PAGE_SIZE = 5
    }
}
