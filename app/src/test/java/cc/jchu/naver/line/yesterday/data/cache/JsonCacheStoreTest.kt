package cc.jchu.naver.line.yesterday.data.cache

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class JsonCacheStoreTest {
    private var database: TestJsonCacheDatabase? = null
    private lateinit var store: JsonCacheStore

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, TestJsonCacheDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = RoomJsonCacheStore(database!!.jsonCacheDao())
    }

    @After
    fun tearDown() {
        database?.close()
    }

    @Test
    fun putAndGetPreservesRawJsonAndUsesExplicitTypeValue() = runBlocking {
        val rawJson = "{\"unparsed\":true,\"value\":\"  exact  \"}"

        store.put(CacheType.SPACE_FLIGHT_DETAIL, "39639", rawJson, 123L)

        assertEquals(JsonCacheEntry(rawJson, 123L), store.get(CacheType.SPACE_FLIGHT_DETAIL, "39639"))
        assertEquals(4, CacheType.SPACE_FLIGHT_DETAIL.databaseValue)
    }

    @Test
    fun putReplacesOnlyTheSameTypeAndKey() = runBlocking {
        store.put(CacheType.DUMMY_JSON_FEED, "0", "old", 1L)
        store.put(CacheType.DUMMY_JSON_FEED, "0", "new", 2L)
        store.put(CacheType.SPACE_FLIGHT_FEED, "0", "other-source", 3L)

        assertEquals(JsonCacheEntry("new", 2L), store.get(CacheType.DUMMY_JSON_FEED, "0"))
        assertEquals(JsonCacheEntry("other-source", 3L), store.get(CacheType.SPACE_FLIGHT_FEED, "0"))
    }

    @Test
    fun replaceFeedPagesKeepsFirstPageAndDeletesOtherCursorsForThatSource() = runBlocking {
        store.put(CacheType.DUMMY_JSON_FEED, "0", "old-first", 1L)
        store.put(CacheType.DUMMY_JSON_FEED, "5", "old-second", 1L)
        store.put(CacheType.SPACE_FLIGHT_FEED, "0", "other-source", 1L)

        store.replaceFeedPages(CacheType.DUMMY_JSON_FEED, "0", "new-first", 2L)

        assertEquals(JsonCacheEntry("new-first", 2L), store.get(CacheType.DUMMY_JSON_FEED, "0"))
        assertNull(store.get(CacheType.DUMMY_JSON_FEED, "5"))
        assertEquals(JsonCacheEntry("other-source", 1L), store.get(CacheType.SPACE_FLIGHT_FEED, "0"))
    }

    @Test
    fun deleteAndClearAllRemoveOnlyApiCacheEntries() = runBlocking {
        store.put(CacheType.DUMMY_JSON_DETAIL, "6", "detail", 1L)
        store.put(CacheType.SPACE_FLIGHT_FEED, "0", "feed", 1L)

        store.delete(CacheType.DUMMY_JSON_DETAIL, "6")
        assertNull(store.get(CacheType.DUMMY_JSON_DETAIL, "6"))
        assertEquals(JsonCacheEntry("feed", 1L), store.get(CacheType.SPACE_FLIGHT_FEED, "0"))

        store.clearAll()
        assertNull(store.get(CacheType.SPACE_FLIGHT_FEED, "0"))
    }
}

@Database(entities = [JsonCacheEntity::class], version = 1, exportSchema = false)
abstract class TestJsonCacheDatabase : RoomDatabase() {
    abstract fun jsonCacheDao(): JsonCacheDao
}
