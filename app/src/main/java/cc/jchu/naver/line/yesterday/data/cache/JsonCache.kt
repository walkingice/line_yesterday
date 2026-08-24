package cc.jchu.naver.line.yesterday.data.cache

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

enum class CacheType(val databaseValue: Int) {
    DUMMY_JSON_FEED(1),
    SPACE_FLIGHT_FEED(2),
    DUMMY_JSON_DETAIL(3),
    SPACE_FLIGHT_DETAIL(4),
}

data class JsonCacheEntry(
    val rawJson: String,
    val timestamp: Long,
)

@Entity(
    tableName = "json_cache",
    indices = [Index(value = ["type", "cacheKey"], unique = true)],
)
data class JsonCacheEntity(
    @androidx.room.PrimaryKey(autoGenerate = true)
    val _id: Long = 0,
    val timestamp: Long,
    val type: Int,
    val cacheKey: String,
    val jsonString: String,
)

@Dao
interface JsonCacheDao {
    @Query("SELECT * FROM json_cache WHERE type = :type AND cacheKey = :key LIMIT 1")
    suspend fun get(type: Int, key: String): JsonCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: JsonCacheEntity)

    @Query("DELETE FROM json_cache WHERE type = :type AND cacheKey = :key")
    suspend fun delete(type: Int, key: String)

    @Query("DELETE FROM json_cache")
    suspend fun clearAll()

    @Query("DELETE FROM json_cache WHERE type = :type AND cacheKey != :firstPageKey")
    suspend fun deleteOtherFeedPages(type: Int, firstPageKey: String)

    @Transaction
    suspend fun replaceFeedPages(
        type: Int,
        firstPageKey: String,
        rawJson: String,
        timestamp: Long,
    ) {
        upsert(
            JsonCacheEntity(
                timestamp = timestamp,
                type = type,
                cacheKey = firstPageKey,
                jsonString = rawJson,
            ),
        )
        deleteOtherFeedPages(type, firstPageKey)
    }
}

interface JsonCacheStore {
    suspend fun get(type: CacheType, key: String): JsonCacheEntry?

    suspend fun put(
        type: CacheType,
        key: String,
        rawJson: String,
        timestamp: Long,
    )

    suspend fun delete(type: CacheType, key: String)
    suspend fun clearAll()

    suspend fun replaceFeedPages(
        type: CacheType,
        firstPageKey: String,
        rawJson: String,
        timestamp: Long,
    )
}

class RoomJsonCacheStore(
    private val dao: JsonCacheDao,
) : JsonCacheStore {
    override suspend fun get(type: CacheType, key: String): JsonCacheEntry? {
        return dao.get(type.databaseValue, key)?.let { entity ->
            JsonCacheEntry(rawJson = entity.jsonString, timestamp = entity.timestamp)
        }
    }

    override suspend fun put(type: CacheType, key: String, rawJson: String, timestamp: Long) {
        dao.upsert(
            JsonCacheEntity(
                timestamp = timestamp,
                type = type.databaseValue,
                cacheKey = key,
                jsonString = rawJson,
            ),
        )
    }

    override suspend fun delete(type: CacheType, key: String) {
        dao.delete(type.databaseValue, key)
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }

    override suspend fun replaceFeedPages(
        type: CacheType,
        firstPageKey: String,
        rawJson: String,
        timestamp: Long,
    ) {
        dao.replaceFeedPages(type.databaseValue, firstPageKey, rawJson, timestamp)
    }
}
