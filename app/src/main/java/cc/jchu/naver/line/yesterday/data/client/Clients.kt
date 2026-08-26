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
import kotlinx.coroutines.delay
import java.io.IOException
import java.net.URI

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

class DummyJsonClientReal(
    context: Context,
    private val networkStatusProvider: NetworkStatusProvider
) : DummyJsonClient {

    override suspend fun getProducts(cursor: PageCursor): ClientResult {
        // use this API endpoint to get real JSON file
        // val index = cursor.toDummyPageIndex()
        // val limit = 5
        // val skip = index * limit
        // https://dummyjson.com/products?limit={limit}&select=title,category,thumbnail&skip={skip}
        TODO()
    }

    override suspend fun getProduct(id: String): ClientResult {
        // use this API endpoint to get real JSON file
        // https://dummyjson.com/products/{id}
        TODO()
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

class SpaceFlightClientReal(
    context: Context,
    private val networkStatusProvider: NetworkStatusProvider
) : SpaceFlightClient {
    override suspend fun getArticles(cursor: PageCursor): ClientResult {
        // use this API endpoint to get real JSON file
        // val index = cursor.toSpaceFlightPageIndex()
        // val limit = 10
        // val offset = index * limit
        // https://api.spaceflightnewsapi.net/v4/articles/?format=json&limit={limit}&offset={offset}
        TODO("Not yet implemented")
    }

    override suspend fun getArticle(id: String): ClientResult {
        // use this API endpoint to get real JSON file
        // https://api.spaceflightnewsapi.net/v4/articles/{id}/?format=json
        TODO("Not yet implemented")
    }

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
