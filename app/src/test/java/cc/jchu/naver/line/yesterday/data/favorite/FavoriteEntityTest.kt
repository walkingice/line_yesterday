package cc.jchu.naver.line.yesterday.data.favorite

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class FavoriteEntityTest {
    @Test
    fun compositeIdentityAllowsSameItemIdForDifferentSources() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            FavoriteEntityDatabase::class.java,
        ).allowMainThreadQueries().build()

        database.favoriteDao().insert(sample(sourceType = 1))
        database.favoriteDao().insert(sample(sourceType = 2))

        assertEquals(2, database.favoriteDao().count())
        database.close()
    }

    @Test
    fun storesAllFavoriteSnapshotFields() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            FavoriteEntityDatabase::class.java,
        ).allowMainThreadQueries().build()
        val expected = sample(
            title = "Title",
            imgUrl = "image",
            description = "Description",
            extraInformation = "Extra",
        )

        database.favoriteDao().insert(expected)

        assertEquals(expected, database.favoriteDao().find(1, "same-id"))
        database.close()
    }

    private fun sample(
        sourceType: Int = 1,
        title: String = "title",
        imgUrl: String = "img",
        description: String = "description",
        extraInformation: String = "extra",
    ) = FavoriteEntity(
        sourceType = sourceType,
        itemId = "same-id",
        addedAt = 10L,
        title = title,
        imgUrl = imgUrl,
        description = description,
        extraInformation = extraInformation,
    )
}

@androidx.room.Dao
interface FavoriteEntityDao {
    @androidx.room.Insert
    suspend fun insert(entity: FavoriteEntity)

    @androidx.room.Query("SELECT COUNT(*) FROM favorite")
    suspend fun count(): Int

    @androidx.room.Query("SELECT * FROM favorite WHERE sourceType = :sourceType AND itemId = :itemId")
    suspend fun find(sourceType: Int, itemId: String): FavoriteEntity?
}

@Database(entities = [FavoriteEntity::class], version = 1, exportSchema = false)
abstract class FavoriteEntityDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteEntityDao
}
