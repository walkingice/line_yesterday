package cc.jchu.naver.line.yesterday.di

import android.content.Context
import androidx.room.Room
import cc.jchu.naver.line.yesterday.data.cache.RoomJsonCacheStore
import cc.jchu.naver.line.yesterday.data.client.DummyJsonClient
import cc.jchu.naver.line.yesterday.data.client.SpaceFlightClient
import cc.jchu.naver.line.yesterday.data.database.AppDatabase
import cc.jchu.naver.line.yesterday.data.favorite.RoomFavoriteStore
import cc.jchu.naver.line.yesterday.data.provider.DemoNetworkStatusProvider
import cc.jchu.naver.line.yesterday.data.provider.SystemTimeProvider
import cc.jchu.naver.line.yesterday.data.provider.TimeProvider
import cc.jchu.naver.line.yesterday.data.repository.DetailRepository
import cc.jchu.naver.line.yesterday.data.repository.DummyJsonRepository
import cc.jchu.naver.line.yesterday.data.repository.FavoritesRepository
import cc.jchu.naver.line.yesterday.data.repository.SpaceFlightRepository
import com.linecorp.lich.component.ComponentFactory
import com.linecorp.lich.component.getComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ApplicationComponent private constructor(
    val applicationContext: Context,
    val database: AppDatabase,
    val timeProvider: TimeProvider,
    val networkStatusProvider: DemoNetworkStatusProvider,
    val dummyJsonRepository: DummyJsonRepository,
    val spaceFlightRepository: SpaceFlightRepository,
    val detailRepository: DetailRepository,
    val favoritesRepository: FavoritesRepository,
    val feedViewModelFactory: FeedViewModelFactory,
    val favoritesViewModelFactory: FavoritesViewModelFactory,
    private val cacheClearScope: CoroutineScope,
) {
    fun detailViewModelFactory(arguments: cc.jchu.naver.line.yesterday.detail.DetailArguments?) =
        DetailViewModelFactory(arguments, detailRepository, favoritesRepository)

    fun clearJsonCache() {
        cacheClearScope.launch { database.jsonCacheDao().clearAll() }
    }

    companion object : ComponentFactory<ApplicationComponent>() {
        override fun createComponent(context: Context): ApplicationComponent {
            val applicationContext = context.applicationContext
            val database = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME,
            ).build()
            val jsonCacheStore = RoomJsonCacheStore(database.jsonCacheDao())
            val favoriteStore = RoomFavoriteStore(database.favoriteDao())
            val timeProvider = SystemTimeProvider()
            val dummyJsonRepository = DummyJsonRepository(
                client = context.getComponent(DummyJsonClient),
                cacheStore = jsonCacheStore,
                timeProvider = timeProvider,
            )
            val spaceFlightRepository = SpaceFlightRepository(
                client = context.getComponent(SpaceFlightClient),
                cacheStore = jsonCacheStore,
                timeProvider = timeProvider,
            )
            val detailRepository = DetailRepository(dummyJsonRepository, spaceFlightRepository)
            val favoritesRepository = FavoritesRepository(favoriteStore, timeProvider)
            return ApplicationComponent(
                applicationContext = applicationContext,
                database = database,
                timeProvider = timeProvider,
                networkStatusProvider = context.getComponent(DemoNetworkStatusProvider),
                dummyJsonRepository = dummyJsonRepository,
                spaceFlightRepository = spaceFlightRepository,
                detailRepository = detailRepository,
                favoritesRepository = favoritesRepository,
                feedViewModelFactory = FeedViewModelFactory(
                    dummyJsonRepository,
                    spaceFlightRepository,
                ),
                favoritesViewModelFactory = FavoritesViewModelFactory(favoritesRepository),
                cacheClearScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            )
        }

        private const val DATABASE_NAME = "line_yesterday.db"
    }
}

fun Context.applicationComponent(): ApplicationComponent =
    getComponent(ApplicationComponent)
