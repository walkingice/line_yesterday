package cc.jchu.naver.line.yesterday.data.repository

import cc.jchu.naver.line.yesterday.data.cache.CacheType
import cc.jchu.naver.line.yesterday.data.cache.JsonCacheEntry
import cc.jchu.naver.line.yesterday.data.cache.JsonCacheStore
import cc.jchu.naver.line.yesterday.data.client.DummyJsonClient
import cc.jchu.naver.line.yesterday.data.domain.ClientResult
import cc.jchu.naver.line.yesterday.data.domain.DataError
import cc.jchu.naver.line.yesterday.data.domain.DetailLoadEvent
import cc.jchu.naver.line.yesterday.data.domain.PageCursor
import cc.jchu.naver.line.yesterday.data.provider.FakeTimeProvider
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DummyJsonRepositoryTest {
    private val page = """
        {"products":[{"id":1,"title":"One","thumbnail":"image","category":"cat"}],"total":2,"skip":0,"limit":1}
    """.trimIndent()
    private val detail = """
        {"id":1,"title":"One","thumbnail":"image","category":"cat","description":"Description"}
    """.trimIndent()
    private val time = FakeTimeProvider(400_000L)

    @Test
    fun cacheMissLoadsAndStoresParsedFeed() = runBlocking {
        val store = FakeCacheStore()
        val repository = repository(store, ClientResult.Success(page))

        val result = repository.getFeedPage(PageCursor("0"))

        assertEquals("1", result.items.single().id)
        assertEquals(page, store.entries[CacheType.DUMMY_JSON_FEED to "0"]?.rawJson)
    }

    @Test
    fun freshCacheAvoidsClient() = runBlocking {
        val store = FakeCacheStore()
        store.entries[CacheType.DUMMY_JSON_FEED to "0"] = JsonCacheEntry(page, 399_500L)
        val client = RecordingClient(ClientResult.Failure(AssertionError("not called")))
        val repository = DummyJsonRepository(client, store, time)

        val result = repository.getFeedPage(PageCursor("0"))

        assertEquals("1", result.items.single().id)
        assertEquals(0, client.feedCalls)
    }

    @Test
    fun staleCacheIsDisplayedWhenClientIsOffline(): Unit = runBlocking {
        val store = FakeCacheStore()
        store.entries[CacheType.DUMMY_JSON_FEED to "0"] = JsonCacheEntry(page, 0L)
        val repository = repository(store, ClientResult.Offline)

        val result = repository.getFeedPage(PageCursor("0"))

        assertEquals("1", result.items.single().id)
        assertTrue(result.isStale)
        assertIs<DataError.Offline>(result.loadFailure)
    }

    @Test
    fun forcedRefreshReplacesPagesAndDoesNotReadOldCache() = runBlocking {
        val store = FakeCacheStore()
        store.entries[CacheType.DUMMY_JSON_FEED to "0"] = JsonCacheEntry("old", 399_500L)
        val client = RecordingClient(ClientResult.Success(page))
        val repository = DummyJsonRepository(client, store, time)

        val result = repository.getFeedPage(PageCursor("0"), forceRefresh = true)

        assertEquals("1", result.items.single().id)
        assertEquals(0, store.getCalls)
        assertEquals(page, store.entries[CacheType.DUMMY_JSON_FEED to "0"]?.rawJson)
    }

    @Test
    fun malformedCacheFallsBackToClient() = runBlocking {
        val store = FakeCacheStore()
        store.entries[CacheType.DUMMY_JSON_FEED to "0"] = JsonCacheEntry("bad", 399_500L)
        val repository = repository(store, ClientResult.Success(page))

        val result = repository.getFeedPage(PageCursor("0"))

        assertEquals("1", result.items.single().id)
    }

    @Test
    fun malformedClientJsonReturnsParseFailureWithoutDeletingCache() = runBlocking {
        val store = FakeCacheStore()
        val repository = repository(store, ClientResult.Success("bad"))

        val result = repository.getFeedPage(PageCursor("0"))

        assertIs<DataError.Parse>(result.loadFailure)
        assertTrue(result.items.isEmpty())
    }

    @Test
    fun cacheWriteFailureKeepsNewDataAndReportsWarning() = runBlocking {
        val store = FakeCacheStore(writeFailure = IllegalStateException("write"))
        val repository = repository(store, ClientResult.Success(page))

        val result = repository.getFeedPage(PageCursor("0"))

        assertEquals("1", result.items.single().id)
        assertIs<DataError.Storage>(result.cacheWarning)
        assertEquals(0, store.entries.size)
    }

    @Test
    fun recoverySkipsPageCacheUntilSuccessfulRefreshTransaction() = runBlocking {
        val store = FakeCacheStore(writeFailure = IllegalStateException("write"))
        val client = RecordingClient(ClientResult.Success(page))
        val repository = DummyJsonRepository(client, store, time)

        repository.getFeedPage(PageCursor("0"), forceRefresh = true)
        store.writeFailure = null
        repository.getFeedPage(PageCursor("0"))
        assertEquals(0, store.entries.size)

        repository.getFeedPage(PageCursor("0"), forceRefresh = true)
        assertEquals(page, store.entries[CacheType.DUMMY_JSON_FEED to "0"]?.rawJson)
    }

    @Test
    fun detailEmitsCachedThenUpdatedForStaleEntry(): Unit = runBlocking {
        val store = FakeCacheStore()
        store.entries[CacheType.DUMMY_JSON_DETAIL to "1"] = JsonCacheEntry(detail, 0L)
        val repository = repository(store, ClientResult.Success(detail))

        val events = repository.getDetail("1").toList()

        assertEquals(2, events.size)
        assertIs<DetailLoadEvent.Cached>(events[0])
        assertIs<DetailLoadEvent.Updated>(events[1])
    }

    private fun repository(store: FakeCacheStore, result: ClientResult): DummyJsonRepository =
        DummyJsonRepository(RecordingClient(result), store, time)

    private class RecordingClient(
        private val result: ClientResult,
    ) : DummyJsonClient {
        var feedCalls = 0

        override suspend fun getProducts(cursor: PageCursor): ClientResult {
            feedCalls++
            return result
        }

        override suspend fun getProduct(id: String): ClientResult = result
    }

    private class FakeCacheStore(
        var writeFailure: Throwable? = null,
    ) : JsonCacheStore {
        val entries = mutableMapOf<Pair<CacheType, String>, JsonCacheEntry>()
        var getCalls = 0

        override suspend fun get(type: CacheType, key: String): JsonCacheEntry? {
            getCalls++
            return entries[type to key]
        }

        override suspend fun put(type: CacheType, key: String, rawJson: String, timestamp: Long) {
            writeFailure?.let { throw it }
            entries[type to key] = JsonCacheEntry(rawJson, timestamp)
        }

        override suspend fun delete(type: CacheType, key: String) {
            entries.remove(type to key)
        }

        override suspend fun clearAll() = entries.clear()

        override suspend fun replaceFeedPages(
            type: CacheType,
            firstPageKey: String,
            rawJson: String,
            timestamp: Long,
        ) {
            put(type, firstPageKey, rawJson, timestamp)
            entries.keys.removeAll { it.first == type && it.second != firstPageKey }
        }
    }
}
