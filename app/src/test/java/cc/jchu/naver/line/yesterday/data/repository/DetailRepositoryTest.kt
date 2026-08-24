package cc.jchu.naver.line.yesterday.data.repository

import cc.jchu.naver.line.yesterday.data.cache.CacheType
import cc.jchu.naver.line.yesterday.data.cache.JsonCacheEntry
import cc.jchu.naver.line.yesterday.data.cache.JsonCacheStore
import cc.jchu.naver.line.yesterday.data.client.DummyJsonClient
import cc.jchu.naver.line.yesterday.data.client.SpaceFlightClient
import cc.jchu.naver.line.yesterday.data.domain.ClientResult
import cc.jchu.naver.line.yesterday.data.domain.DetailLoadEvent
import cc.jchu.naver.line.yesterday.data.domain.FeedSource
import cc.jchu.naver.line.yesterday.data.domain.PageCursor
import cc.jchu.naver.line.yesterday.data.provider.FakeTimeProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

class DetailRepositoryTest {
    @Test
    fun routesBySourceWithoutPassingApiObjects() = runBlocking {
        val repository = DetailRepository(
            DummyJsonRepository(DummyClient(), EmptyCacheStore(), FakeTimeProvider()),
            SpaceFlightRepository(SpaceClient(), EmptyCacheStore(), FakeTimeProvider()),
        )

        val dummy = repository.getDetail(FeedSource.DUMMY_JSON, "1").first()
        val space = repository.getDetail(FeedSource.SPACE_FLIGHT, "2").first()

        assertEquals(FeedSource.DUMMY_JSON, (dummy as DetailLoadEvent.Updated).detail.source)
        assertEquals(FeedSource.SPACE_FLIGHT, (space as DetailLoadEvent.Updated).detail.source)
    }

    private class DummyClient : DummyJsonClient {
        override suspend fun getProducts(cursor: PageCursor) = ClientResult.Failure(Exception())
        override suspend fun getProduct(id: String) = ClientResult.Success(dummyDetail)
    }

    private class SpaceClient : SpaceFlightClient {
        override suspend fun getArticles(cursor: PageCursor) = ClientResult.Failure(Exception())
        override suspend fun getArticle(id: String) = ClientResult.Success(spaceDetail)
    }

    private class EmptyCacheStore : JsonCacheStore {
        override suspend fun get(type: CacheType, key: String): JsonCacheEntry? = null
        override suspend fun put(type: CacheType, key: String, rawJson: String, timestamp: Long) = Unit
        override suspend fun delete(type: CacheType, key: String) = Unit
        override suspend fun clearAll() = Unit
        override suspend fun replaceFeedPages(type: CacheType, firstPageKey: String, rawJson: String, timestamp: Long) = Unit
    }

    companion object {
        private const val dummyDetail = """{"id":1,"title":"Product","thumbnail":"image","category":"cat","description":"desc"}"""
        private const val spaceDetail = """{"id":2,"title":"Article","image_url":"image","news_site":"NASA","summary":"summary"}"""
    }
}
