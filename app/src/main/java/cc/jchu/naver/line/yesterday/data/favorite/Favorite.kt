package cc.jchu.naver.line.yesterday.data.favorite

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cc.jchu.naver.line.yesterday.data.domain.FeedSource

@Entity(
    tableName = "favorite",
    primaryKeys = ["sourceType", "itemId"],
)
data class FavoriteEntity(
    val sourceType: Int,
    val itemId: String,
    val addedAt: Long,
    val title: String,
    val imgUrl: String,
    val description: String,
    val extraInformation: String,
    val time: String = "",
)

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite WHERE sourceType = :sourceType AND itemId = :itemId LIMIT 1")
    suspend fun find(sourceType: Int, itemId: String): FavoriteEntity?

    @Query("SELECT * FROM favorite ORDER BY addedAt DESC, sourceType ASC, itemId ASC")
    suspend fun findAll(): List<FavoriteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FavoriteEntity)

    @Query("DELETE FROM favorite WHERE sourceType = :sourceType AND itemId = :itemId")
    suspend fun delete(sourceType: Int, itemId: String)

    @Query("DELETE FROM favorite")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM favorite")
    suspend fun count(): Int
}

data class FavoriteEntry(
    val source: FeedSource,
    val itemId: String,
    val addedAt: Long,
    val title: String,
    val imgUrl: String,
    val description: String,
    val extraInformation: String,
    val time: String = "",
)

interface FavoriteStore {
    suspend fun get(source: FeedSource, itemId: String): FavoriteEntry?
    suspend fun getAll(): List<FavoriteEntry>
    suspend fun save(entry: FavoriteEntry)
    suspend fun delete(source: FeedSource, itemId: String)
}

class RoomFavoriteStore(
    private val dao: FavoriteDao,
) : FavoriteStore {
    override suspend fun get(source: FeedSource, itemId: String): FavoriteEntry? {
        return dao.find(source.databaseValue(), itemId)?.toEntry()
    }

    override suspend fun getAll(): List<FavoriteEntry> {
        return dao.findAll().map(FavoriteEntity::toEntry)
    }

    override suspend fun save(entry: FavoriteEntry) {
        val existing = dao.find(entry.source.databaseValue(), entry.itemId)
        dao.upsert(entry.toEntity(existing?.addedAt ?: entry.addedAt))
    }

    override suspend fun delete(source: FeedSource, itemId: String) {
        dao.delete(source.databaseValue(), itemId)
    }
}

private fun FavoriteEntry.toEntity(addedAt: Long) = FavoriteEntity(
    sourceType = source.databaseValue(),
    itemId = itemId,
    addedAt = addedAt,
    title = title,
    imgUrl = imgUrl,
    description = description,
    extraInformation = extraInformation,
    time = time,
)

private fun FavoriteEntity.toEntry() = FavoriteEntry(
    source = sourceType.toFeedSource(),
    itemId = itemId,
    addedAt = addedAt,
    title = title,
    imgUrl = imgUrl,
    description = description,
    extraInformation = extraInformation,
    time = time,
)

private fun FeedSource.databaseValue(): Int = when (this) {
    FeedSource.DUMMY_JSON -> 1
    FeedSource.SPACE_FLIGHT -> 2
}

private fun Int.toFeedSource(): FeedSource = when (this) {
    1 -> FeedSource.DUMMY_JSON
    2 -> FeedSource.SPACE_FLIGHT
    else -> error("Unsupported Favorite source type: $this")
}
