package cc.jchu.naver.line.yesterday.data.favorite

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import cc.jchu.naver.line.yesterday.data.domain.FeedSource
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class FavoriteStoreTest {
    private lateinit var database: FavoriteStoreDatabase
    private lateinit var store: FavoriteStore

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            FavoriteStoreDatabase::class.java,
        ).allowMainThreadQueries().build()
        store = RoomFavoriteStore(database.favoriteDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveGetListAndDeleteUseSourceAndItemIdentity() = runBlocking {
        val dummy = entry(FeedSource.DUMMY_JSON)
        val spaceFlight = entry(FeedSource.SPACE_FLIGHT)

        store.save(dummy)
        store.save(spaceFlight)

        assertEquals(dummy, store.get(FeedSource.DUMMY_JSON, "item"))
        assertEquals(listOf(dummy, spaceFlight), store.getAll())

        store.delete(FeedSource.DUMMY_JSON, "item")

        assertNull(store.get(FeedSource.DUMMY_JSON, "item"))
        assertEquals(listOf(spaceFlight), store.getAll())
    }

    @Test
    fun savingExistingFavoriteUpdatesSnapshotWithoutChangingAddedAt() = runBlocking {
        store.save(entry(addedAt = 100L, title = "old"))
        store.save(entry(addedAt = 200L, title = "new"))

        assertEquals(
            entry(addedAt = 100L, title = "new"),
            store.get(FeedSource.DUMMY_JSON, "item"),
        )
    }

    private fun entry(
        source: FeedSource = FeedSource.DUMMY_JSON,
        addedAt: Long = 10L,
        title: String = "title",
    ) = FavoriteEntry(
        source = source,
        itemId = "item",
        addedAt = addedAt,
        title = title,
        imgUrl = "image",
        description = "description",
        extraInformation = "extra",
    )
}

@Database(entities = [FavoriteEntity::class], version = 1, exportSchema = false)
abstract class FavoriteStoreDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
}
