package cc.jchu.naver.line.yesterday.data.client

import android.content.Context
import android.util.Log
import cc.jchu.naver.line.yesterday.data.domain.ClientResult
import cc.jchu.naver.line.yesterday.data.domain.PageCursor
import cc.jchu.naver.line.yesterday.data.provider.DemoNetworkStatusProvider
import cc.jchu.naver.line.yesterday.data.provider.NetworkStatusProvider
import cc.jchu.naver.line.yesterday.data.settings.ClientSettings
import com.linecorp.lich.component.ComponentFactory
import com.linecorp.lich.component.getComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URI
import java.net.HttpURLConnection
import java.net.URL

interface DummyJsonClient {
    suspend fun getProducts(cursor: PageCursor): ClientResult

    suspend fun getProduct(id: String): ClientResult

    companion object : ComponentFactory<DummyJsonClient>() {
        private const val TAG: String = "DummyJsonClient"
        override fun createComponent(context: Context): DummyJsonClient {
            val networkStatusProvider = context.getComponent(DemoNetworkStatusProvider)
            return createDummyJsonClient(context, networkStatusProvider)
        }
    }
}

interface SpaceFlightClient {
    suspend fun getArticles(cursor: PageCursor): ClientResult

    suspend fun getArticle(id: String): ClientResult

    companion object : ComponentFactory<SpaceFlightClient>() {
        private const val TAG: String = "SpaceFlightClient"
        override fun createComponent(context: Context): SpaceFlightClient {
            val networkStatusProvider = context.getComponent(DemoNetworkStatusProvider)
            return createSpaceFlightClient(context, networkStatusProvider)
        }
    }
}

internal fun createDummyJsonClient(
    context: Context,
    networkStatusProvider: NetworkStatusProvider,
): DummyJsonClient {
    if (ClientSettings(context).useRealClient) {
        Log.d("DummyJsonClient", "Create DummyJsonClient by using REAL implementation")
        return DummyJsonClientReal(context, networkStatusProvider)
    }
    Log.d("DummyJsonClient", "Create DummyJsonClient by using Mock implementation")
    return DummyJsonClientMock(context, networkStatusProvider)
}

internal fun createSpaceFlightClient(
    context: Context,
    networkStatusProvider: NetworkStatusProvider,
): SpaceFlightClient {
    if (ClientSettings(context).useRealClient) {
        Log.d("SpaceFlightClient", "Create SpaceFlightClient by using REAL implementation")
        return SpaceFlightClientReal(context, networkStatusProvider)
    }
    Log.d("SpaceFlightClient", "Create SpaceFlightClient by using Mock implementation")
    return SpaceFlightClientMock(context, networkStatusProvider)
}

fun interface ClientDelayProvider {
    suspend fun await()
}

class NetworkClientDelayProvider(
    private val delayMillis: Long = DEFAULT_CLIENT_DELAY_MILLIS,
) : ClientDelayProvider {
    override suspend fun await() {
        delay(delayMillis)
    }

    private companion object {
        const val DEFAULT_CLIENT_DELAY_MILLIS = 1_000L
    }
}

private class AssetReader(
    context: Context,
) {
    private val assets = context.applicationContext.assets

    fun read(path: String): ClientResult = try {
        ClientResult.Success(assets.open(path).bufferedReader().use { it.readText() })
    } catch (cause: IOException) {
        ClientResult.Failure(cause)
    }
}

class DummyJsonClientMock(
    context: Context,
    private val networkStatusProvider: NetworkStatusProvider,
    private val delayProvider: ClientDelayProvider = NetworkClientDelayProvider(),
) : DummyJsonClient {
    private val assetReader = AssetReader(context)

    override suspend fun getProducts(cursor: PageCursor): ClientResult =
        request("dummy_json/feeds/page_${cursor.toDummyPageIndex()}.json")

    override suspend fun getProduct(id: String): ClientResult =
        request("dummy_json/details/product_$id.json")

    private suspend fun request(path: String): ClientResult {
        delayProvider.await()
        if (!networkStatusProvider.isOnline()) {
            Log.d(TAG, "No asset read: device is offline; path=$path")
            return ClientResult.Offline
        }
        Log.d(TAG, "Reading mock asset: path=$path")
        return assetReader.read(path)
    }

    private companion object {
        const val TAG = "DummyJsonClientMock"
    }
}

class DummyJsonClientReal internal constructor(
    context: Context,
    private val networkStatusProvider: NetworkStatusProvider,
    private val responseFetcher: JsonResponseFetcher,
) : DummyJsonClient {

    constructor(context: Context, networkStatusProvider: NetworkStatusProvider) : this(
        context,
        networkStatusProvider,
        HttpUrlConnectionJsonResponseFetcher,
    )

    override suspend fun getProducts(cursor: PageCursor): ClientResult {
        val skip = cursor.toDummySkip()
            ?: return ClientResult.Failure(IllegalArgumentException("Invalid DummyJSON cursor: ${cursor.value}"))
        return requestRealJson(
            networkStatusProvider = networkStatusProvider,
            responseFetcher = responseFetcher,
            url = "$PRODUCTS_URL?limit=$PAGE_SIZE&select=title,category,thumbnail,meta&skip=$skip",
            tag = TAG,
        )
    }

    override suspend fun getProduct(id: String): ClientResult = requestRealJson(
        networkStatusProvider = networkStatusProvider,
        responseFetcher = responseFetcher,
        url = "$PRODUCTS_URL/$id",
        tag = TAG,
    )

    private companion object {
        const val TAG = "DummyJsonClientReal"
        const val PRODUCTS_URL = "https://dummyjson.com/products"
        const val PAGE_SIZE = 10
    }
}

class SpaceFlightClientMock(
    context: Context,
    private val networkStatusProvider: NetworkStatusProvider,
    private val delayProvider: ClientDelayProvider = NetworkClientDelayProvider(),
) : SpaceFlightClient {
    private val assetReader = AssetReader(context)

    override suspend fun getArticles(cursor: PageCursor): ClientResult =
        request("space_flight/feeds/page_${cursor.toSpaceFlightPageIndex()}.json")

    override suspend fun getArticle(id: String): ClientResult =
        request("space_flight/details/article_$id.json")

    private suspend fun request(path: String): ClientResult {
        delayProvider.await()
        if (!networkStatusProvider.isOnline()) {
            Log.d(TAG, "No asset read: device is offline; path=$path")
            return ClientResult.Offline
        }
        Log.d(TAG, "Reading mock asset: path=$path")
        return assetReader.read(path)
    }

    private companion object {
        const val TAG = "SpaceFlightClientMock"
    }
}

class SpaceFlightClientReal internal constructor(
    context: Context,
    private val networkStatusProvider: NetworkStatusProvider,
    private val responseFetcher: JsonResponseFetcher,
) : SpaceFlightClient {
    constructor(context: Context, networkStatusProvider: NetworkStatusProvider) : this(
        context,
        networkStatusProvider,
        HttpUrlConnectionJsonResponseFetcher,
    )

    override suspend fun getArticles(cursor: PageCursor): ClientResult {
        val offset = cursor.toSpaceFlightOffset()
            ?: return ClientResult.Failure(IllegalArgumentException("Invalid SpaceFlight cursor: ${cursor.value}"))
        return requestRealJson(
            networkStatusProvider = networkStatusProvider,
            responseFetcher = responseFetcher,
            url = "$ARTICLES_URL?format=json&limit=$PAGE_SIZE&offset=$offset",
            tag = TAG,
        )
    }

    override suspend fun getArticle(id: String): ClientResult = requestRealJson(
        networkStatusProvider = networkStatusProvider,
        responseFetcher = responseFetcher,
        url = "$ARTICLES_URL$id/?format=json",
        tag = TAG,
    )

    private companion object {
        const val TAG = "SpaceFlightClientReal"
        const val ARTICLES_URL = "https://api.spaceflightnewsapi.net/v4/articles/"
        const val PAGE_SIZE = 10
    }
}

internal fun interface JsonResponseFetcher {
    suspend fun fetch(url: String): ClientResult
}

internal object HttpUrlConnectionJsonResponseFetcher : JsonResponseFetcher {
    override suspend fun fetch(url: String): ClientResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            if (connection.responseCode !in 200..299) {
                throw IOException("HTTP ${connection.responseCode} for $url")
            }
            ClientResult.Success(connection.inputStream.bufferedReader().use { reader -> reader.readText() })
        } catch (cause: IOException) {
            ClientResult.Failure(cause)
        } finally {
            connection?.disconnect()
        }
    }

    private const val TIMEOUT_MILLIS = 10_000
}

private suspend fun requestRealJson(
    networkStatusProvider: NetworkStatusProvider,
    responseFetcher: JsonResponseFetcher,
    url: String,
    tag: String,
): ClientResult {
    if (!networkStatusProvider.isOnline()) {
        Log.d(tag, "No network request: device is offline; url=$url")
        return ClientResult.Offline
    }
    Log.d(tag, "Fetching real JSON: url=$url")
    return responseFetcher.fetch(url)
}

private fun PageCursor.toDummyPageIndex(): Int? = value.toIntOrNull()
    ?.takeIf { it >= 0 && it % 10 == 0 }
    ?.div(10)

private fun PageCursor.toSpaceFlightPageIndex(): Int? {
    if (value == "0") return 0
    val offset = runCatching { URI(value).query }
        .getOrNull()
        ?.split('&')
        ?.firstOrNull { it.startsWith("offset=") }
        ?.substringAfter("=")
        ?.toIntOrNull()
    return offset?.takeIf { it >= 0 && it % 10 == 0 }?.div(10)
}

private fun PageCursor.toDummySkip(): Int? = value.toIntOrNull()
    ?.takeIf { it >= 0 && it % 10 == 0 }

private fun PageCursor.toSpaceFlightOffset(): Int? = if (value == "0") {
    0
} else {
    runCatching { URI(value).query }
        .getOrNull()
        ?.split('&')
        ?.firstOrNull { it.startsWith("offset=") }
        ?.substringAfter("=")
        ?.toIntOrNull()
        ?.takeIf { it >= 0 && it % 10 == 0 }
}
