package cc.jchu.naver.line.yesterday.data.client

import cc.jchu.naver.line.yesterday.data.domain.ClientResult
import cc.jchu.naver.line.yesterday.data.domain.PageCursor
import cc.jchu.naver.line.yesterday.data.provider.NetworkStatusProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ClientsTest {
    private val context = RuntimeEnvironment.getApplication()
    private val events = mutableListOf<String>()
    private val network = RecordingNetworkStatusProvider(events)
    private val delay = RecordingDelayProvider(events)
    private val dummyJson = DummyJsonClientMock(context, network, delay)
    private val spaceFlight = SpaceFlightClientMock(context, network, delay)

    @Test
    fun dummyJsonReturnsRawFeedAndDetailFixturesForEveryPreparedMapping() = runBlocking {
        assertRawAsset(dummyJson.getProducts(PageCursor("0")), "dummy_json/feeds/page_0.json")
        assertRawAsset(dummyJson.getProducts(PageCursor("10")), "dummy_json/feeds/page_1.json")
        assertRawAsset(dummyJson.getProducts(PageCursor("20")), "dummy_json/feeds/page_2.json")
        assertRawAsset(dummyJson.getProducts(PageCursor("30")), "dummy_json/feeds/page_3.json")
        assertRawAsset(dummyJson.getProducts(PageCursor("40")), "dummy_json/feeds/page_4.json")
        assertRawAsset(dummyJson.getProducts(PageCursor("50")), "dummy_json/feeds/page_5.json")
        (0..49).forEach { id ->
            assertRawAsset(
                dummyJson.getProduct(id.toString()),
                "dummy_json/details/product_$id.json",
            )
        }
    }

    @Test
    fun spaceFlightReturnsRawFeedAndDetailFixturesForEveryPreparedMapping() = runBlocking {
        val first = "https://api.spaceflightnewsapi.net/v4/articles/?format=json&limit=10&offset="
        assertRawAsset(spaceFlight.getArticles(PageCursor("0")), "space_flight/feeds/page_0.json")
        assertRawAsset(spaceFlight.getArticles(PageCursor(first + "10")), "space_flight/feeds/page_1.json")
        assertRawAsset(spaceFlight.getArticles(PageCursor(first + "20")), "space_flight/feeds/page_2.json")
        context.assets.list("space_flight/details")!!.forEach { filename ->
            val id = filename.removePrefix("article_").removeSuffix(".json")
            assertRawAsset(
                spaceFlight.getArticle(id),
                "space_flight/details/$filename",
            )
        }
    }

    @Test
    fun everyRequestRunsDelayBeforeNetworkCheck() = runBlocking {
        dummyJson.getProduct("1")

        assertEquals(listOf("delay", "network"), events)
    }

    @Test
    fun offlineRequestReturnsOfflineWithoutReadingAsset() = runBlocking {
        network.online = false

        val result = dummyJson.getProduct("missing")

        assertEquals(ClientResult.Offline, result)
        assertEquals(1, delay.calls)
        assertEquals(1, network.calls)
    }

    @Test
    fun missingFixtureReturnsFailure() = runBlocking {
        val result = spaceFlight.getArticle("missing")

        assertTrue(result is ClientResult.Failure)
    }

    @Test
    fun unsupportedCursorReturnsFailure() = runBlocking {
        val result = dummyJson.getProducts(PageCursor("15"))

        assertTrue(result is ClientResult.Failure)
    }

    private fun assertRawAsset(result: ClientResult, path: String) {
        val expected = context.assets.open(path).bufferedReader().use { it.readText() }
        assertEquals(ClientResult.Success(expected), result)
    }

    private class RecordingDelayProvider(
        private val events: MutableList<String>,
    ) : ClientDelayProvider {
        var calls = 0

        override suspend fun await() {
            calls++
            events += "delay"
        }
    }

    private class RecordingNetworkStatusProvider(
        private val events: MutableList<String>,
    ) : NetworkStatusProvider {
        var online = true
        var calls = 0

        override fun isOnline(): Boolean {
            calls++
            events += "network"
            return online
        }
    }
}
