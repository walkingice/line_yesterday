package cc.jchu.naver.line.yesterday.data.provider

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.linecorp.lich.component.ComponentFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

interface NetworkStatusProvider {
    fun isOnline(): Boolean
}

interface NetworkStatusController {
    fun setOnline(online: Boolean)

    fun clearOverride()
}

interface TimeProvider {
    fun getCurrentTimeMillis(): Long
}

interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}

class ConnectivityNetworkStatusProvider(
    context: Context,
) : NetworkStatusProvider {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    override fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

class DemoNetworkStatusProvider(
    private val fallback: NetworkStatusProvider,
) : NetworkStatusProvider, NetworkStatusController {
    @Volatile
    private var override: Boolean? = null

    override fun isOnline(): Boolean = override ?: fallback.isOnline()

    override fun setOnline(online: Boolean) {
        override = online
    }

    override fun clearOverride() {
        override = null
    }

    companion object : ComponentFactory<DemoNetworkStatusProvider>() {
        override fun createComponent(context: Context): DemoNetworkStatusProvider =
            DemoNetworkStatusProvider(ConnectivityNetworkStatusProvider(context))
    }
}

class SystemTimeProvider : TimeProvider {
    override fun getCurrentTimeMillis(): Long = System.currentTimeMillis()
}

object DefaultDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
}

class FakeNetworkStatusProvider(
    var online: Boolean = true,
) : NetworkStatusProvider {
    override fun isOnline(): Boolean = online
}

class FakeTimeProvider(
    var now: Long = 0L,
) : TimeProvider {
    override fun getCurrentTimeMillis(): Long = now
}

class FakeDispatcherProvider(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
) : DispatcherProvider {
    override val main: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}
