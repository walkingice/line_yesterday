package cc.jchu.naver.line.yesterday.data.repository

import cc.jchu.naver.line.yesterday.data.cache.CacheType
import cc.jchu.naver.line.yesterday.data.cache.JsonCacheEntry
import cc.jchu.naver.line.yesterday.data.cache.JsonCacheStore
import cc.jchu.naver.line.yesterday.data.client.SpaceFlightClient
import cc.jchu.naver.line.yesterday.data.domain.ClientResult
import cc.jchu.naver.line.yesterday.data.domain.DataError
import cc.jchu.naver.line.yesterday.data.domain.PageCursor
import cc.jchu.naver.line.yesterday.data.provider.FakeTimeProvider
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SpaceFlightRepositoryTest {
    private val page = """
        {"count":1,"next":null,"results":[{"id":9,"title":"Article","image_url":"image","news_site":"NASA","summary":"Summary"}]}
    """.trimIndent()

    @Test
    fun lastPageIsExhaustedAndOfflineUsesStaleCache(): Unit = runBlocking {
        val store = FakeStore()
        store.entries[CacheType.SPACE_FLIGHT_FEED to "0"] = JsonCacheEntry(page, 0L)
        val repository = SpaceFlightRepository(
            FakeClient(ClientResult.Offline),
            store,
            FakeTimeProvider(400_000L),
        )

        val result = repository.getFeedPage(PageCursor("0"))

        assertEquals(true, result.isStale)
        assertEquals(true, result.isExhausted)
        assertIs<DataError.Offline>(result.loadFailure)
    }

    @Test
    fun successfulPageIsCachedAsRawJson(): Unit = runBlocking {
        val store = FakeStore()
        val repository = SpaceFlightRepository(
            FakeClient(ClientResult.Success(page)),
            store,
            FakeTimeProvider(400_000L),
        )

        val result = repository.getFeedPage(PageCursor("0"))

        assertEquals("9", result.items.single().id)
        assertEquals(page, store.entries[CacheType.SPACE_FLIGHT_FEED to "0"]?.rawJson)
    }

    private class FakeClient(private val result: ClientResult) : SpaceFlightClient {
        override suspend fun getArticles(cursor: PageCursor): ClientResult = result
        override suspend fun getArticle(id: String): ClientResult = result
    }

    private class FakeStore : JsonCacheStore {
        val entries = mutableMapOf<Pair<CacheType, String>, JsonCacheEntry>()

        override suspend fun get(type: CacheType, key: String) = entries[type to key]
        override suspend fun put(type: CacheType, key: String, rawJson: String, timestamp: Long) {
            entries[type to key] = JsonCacheEntry(rawJson, timestamp)
        }
        override suspend fun delete(type: CacheType, key: String) { entries.remove(type to key) }
        override suspend fun clearAll() { entries.clear() }
        override suspend fun replaceFeedPages(type: CacheType, firstPageKey: String, rawJson: String, timestamp: Long) {
            put(type, firstPageKey, rawJson, timestamp)
            entries.keys.removeAll { it.first == type && it.second != firstPageKey }
        }
    }
}
