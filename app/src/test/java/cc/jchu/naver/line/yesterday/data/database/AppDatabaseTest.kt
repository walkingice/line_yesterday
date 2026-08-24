package cc.jchu.naver.line.yesterday.data.database

import androidx.room.Room
import cc.jchu.naver.line.yesterday.data.cache.CacheType
import cc.jchu.naver.line.yesterday.data.cache.RoomJsonCacheStore
import cc.jchu.naver.line.yesterday.data.domain.FeedSource
import cc.jchu.naver.line.yesterday.data.favorite.FavoriteEntry
import cc.jchu.naver.line.yesterday.data.favorite.RoomFavoriteStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class AppDatabaseTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun providesBothDaosAndKeepsStoresIndependent() = runBlocking {
        val cacheStore = RoomJsonCacheStore(database.jsonCacheDao())
        val favoriteStore = RoomFavoriteStore(database.favoriteDao())
        val favorite = FavoriteEntry(
            source = FeedSource.DUMMY_JSON,
            itemId = "6",
            addedAt = 10L,
            title = "title",
            imgUrl = "image",
            description = "description",
            extraInformation = "extra",
        )

        cacheStore.put(CacheType.DUMMY_JSON_DETAIL, "6", "{}", 20L)
        favoriteStore.save(favorite)
        cacheStore.clearAll()

        val cachedEntry = database.jsonCacheDao().get(
            CacheType.DUMMY_JSON_DETAIL.databaseValue,
            "6",
        )

        assertEquals(null, cachedEntry)
        assertEquals(listOf(favorite), favoriteStore.getAll())
    }

    @Test
    fun containsOnlyApplicationTables() {
        val tables = database.openHelper.writableDatabase.query(
            """
            SELECT name FROM sqlite_master
            WHERE type = 'table'
              AND name NOT LIKE 'room_%'
              AND name NOT IN ('android_metadata', 'sqlite_sequence')
            """.trimIndent(),
        ).use { cursor ->
            buildList {
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) add(cursor.getString(nameIndex))
            }
        }

        assertEquals(setOf("json_cache", "favorite"), tables.toSet())
        assertNotNull(database.jsonCacheDao())
        assertNotNull(database.favoriteDao())
    }
}
