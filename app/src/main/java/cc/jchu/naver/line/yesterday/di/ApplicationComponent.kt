package cc.jchu.naver.line.yesterday.di

import android.content.Context
import androidx.room.Room
import com.linecorp.lich.component.ComponentFactory
import com.linecorp.lich.component.getComponent
import cc.jchu.naver.line.yesterday.data.cache.RoomJsonCacheStore
import cc.jchu.naver.line.yesterday.data.client.DummyJsonClientMock
import cc.jchu.naver.line.yesterday.data.client.SpaceFlightClientMock
import cc.jchu.naver.line.yesterday.data.database.AppDatabase
import cc.jchu.naver.line.yesterday.data.favorite.RoomFavoriteStore
import cc.jchu.naver.line.yesterday.data.provider.ConnectivityNetworkStatusProvider
import cc.jchu.naver.line.yesterday.data.provider.NetworkStatusProvider
import cc.jchu.naver.line.yesterday.data.provider.SystemTimeProvider
import cc.jchu.naver.line.yesterday.data.provider.TimeProvider
import cc.jchu.naver.line.yesterday.data.repository.DetailRepository
import cc.jchu.naver.line.yesterday.data.repository.DummyJsonRepository
import cc.jchu.naver.line.yesterday.data.repository.FavoritesRepository
import cc.jchu.naver.line.yesterday.data.repository.SpaceFlightRepository

class ApplicationComponent private constructor(
    val applicationContext: Context,
    val database: AppDatabase,
    val timeProvider: TimeProvider,
    val networkStatusProvider: NetworkStatusProvider,
    val dummyJsonRepository: DummyJsonRepository,
    val spaceFlightRepository: SpaceFlightRepository,
    val detailRepository: DetailRepository,
    val favoritesRepository: FavoritesRepository,
    val feedViewModelFactory: FeedViewModelFactory,
    val favoritesViewModelFactory: FavoritesViewModelFactory,
) {
    fun detailViewModelFactory(arguments: cc.jchu.naver.line.yesterday.detail.DetailArguments?) =
        DetailViewModelFactory(arguments, detailRepository, favoritesRepository)

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
            val networkStatusProvider = ConnectivityNetworkStatusProvider(applicationContext)
            val dummyJsonRepository = DummyJsonRepository(
                client = DummyJsonClientMock(applicationContext, networkStatusProvider),
                cacheStore = jsonCacheStore,
                timeProvider = timeProvider,
            )
            val spaceFlightRepository = SpaceFlightRepository(
                client = SpaceFlightClientMock(applicationContext, networkStatusProvider),
                cacheStore = jsonCacheStore,
                timeProvider = timeProvider,
            )
            val detailRepository = DetailRepository(dummyJsonRepository, spaceFlightRepository)
            val favoritesRepository = FavoritesRepository(favoriteStore, timeProvider)
            return ApplicationComponent(
                applicationContext = applicationContext,
                database = database,
                timeProvider = timeProvider,
                networkStatusProvider = networkStatusProvider,
                dummyJsonRepository = dummyJsonRepository,
                spaceFlightRepository = spaceFlightRepository,
                detailRepository = detailRepository,
                favoritesRepository = favoritesRepository,
                feedViewModelFactory = FeedViewModelFactory(
                    dummyJsonRepository,
                    spaceFlightRepository,
                ),
                favoritesViewModelFactory = FavoritesViewModelFactory(favoritesRepository),
            )
        }

        private const val DATABASE_NAME = "line_yesterday.db"
    }
}

fun Context.applicationComponent(): ApplicationComponent =
    getComponent(ApplicationComponent)
