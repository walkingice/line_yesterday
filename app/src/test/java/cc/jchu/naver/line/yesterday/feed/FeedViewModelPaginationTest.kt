package cc.jchu.naver.line.yesterday.feed

import cc.jchu.naver.line.yesterday.data.cache.CacheType
import cc.jchu.naver.line.yesterday.data.cache.JsonCacheEntry
import cc.jchu.naver.line.yesterday.data.cache.JsonCacheStore
import cc.jchu.naver.line.yesterday.data.client.DummyJsonClient
import cc.jchu.naver.line.yesterday.data.client.SpaceFlightClient
import cc.jchu.naver.line.yesterday.data.domain.ClientResult
import cc.jchu.naver.line.yesterday.data.domain.PageCursor
import cc.jchu.naver.line.yesterday.data.provider.FakeDispatcherProvider
import cc.jchu.naver.line.yesterday.data.provider.FakeTimeProvider
import cc.jchu.naver.line.yesterday.data.repository.DummyJsonRepository
import cc.jchu.naver.line.yesterday.data.repository.SpaceFlightRepository
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class FeedViewModelPaginationTest {
    @Test
    fun loadMoreRequestsTheNextCursorForEachSource() = runBlocking {
        val dummyClient = RecordingDummyClient()
        val spaceClient = RecordingSpaceClient()
        val viewModel = FeedViewModel(
            DummyJsonRepository(dummyClient, CacheStore(), FakeTimeProvider()),
            SpaceFlightRepository(spaceClient, CacheStore(), FakeTimeProvider()),
            FakeDispatcherProvider(),
        )

        viewModel.loadMoreItems()

        assertEquals(listOf("0", "10"), dummyClient.cursors)
        assertEquals(listOf("0", SPACE_NEXT_CURSOR), spaceClient.cursors)
    }

    private class RecordingDummyClient : DummyJsonClient {
        val cursors = mutableListOf<String>()

        override suspend fun getProducts(cursor: PageCursor): ClientResult {
            cursors += cursor.value
            return asset("dummy_json/feeds/page_${cursor.value.toInt() / 10}.json")
        }

        override suspend fun getProduct(id: String): ClientResult = ClientResult.Offline
    }

    private class RecordingSpaceClient : SpaceFlightClient {
        val cursors = mutableListOf<String>()

        override suspend fun getArticles(cursor: PageCursor): ClientResult {
            cursors += cursor.value
            val page = if (cursor.value == "0") 0 else 1
            return asset("space_flight/feeds/page_$page.json")
        }

        override suspend fun getArticle(id: String): ClientResult = ClientResult.Offline
    }

    private class CacheStore : JsonCacheStore {
        private val entries = mutableMapOf<Pair<CacheType, String>, JsonCacheEntry>()

        override suspend fun get(type: CacheType, key: String) = entries[type to key]
        override suspend fun put(type: CacheType, key: String, rawJson: String, timestamp: Long) {
            entries[type to key] = JsonCacheEntry(rawJson, timestamp)
        }
        override suspend fun delete(type: CacheType, key: String) { entries.remove(type to key) }
        override suspend fun clearAll() { entries.clear() }
        override suspend fun replaceFeedPages(
            type: CacheType, firstPageKey: String, rawJson: String, timestamp: Long,
        ) = put(type, firstPageKey, rawJson, timestamp)
    }

    private companion object {
        const val SPACE_NEXT_CURSOR =
            "https://api.spaceflightnewsapi.net/v4/articles/?format=json&limit=10&offset=10"

        fun asset(path: String): ClientResult.Success = ClientResult.Success(
            RuntimeEnvironment.getApplication().assets.open(path).bufferedReader().use { it.readText() },
        )
    }
}
