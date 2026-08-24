package cc.jchu.naver.line.yesterday.data.favorite

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

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
