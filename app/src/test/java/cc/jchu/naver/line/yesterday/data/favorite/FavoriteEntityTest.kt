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

        database.favoriteDao().upsert(sample(sourceType = 1))
        database.favoriteDao().upsert(sample(sourceType = 2))

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

        database.favoriteDao().upsert(expected)

        assertEquals(expected, database.favoriteDao().find(1, "same-id"))
        database.close()
    }

    @Test
    fun findAllOrdersNewestFirstWithDeterministicTieBreakers() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            FavoriteEntityDatabase::class.java,
        ).allowMainThreadQueries().build()

        database.favoriteDao().upsert(sample(sourceType = 2).copy(addedAt = 20L))
        database.favoriteDao().upsert(sample(sourceType = 1).copy(addedAt = 20L))
        database.favoriteDao().upsert(sample(itemId = "older", addedAt = 10L))

        assertEquals(
            listOf(1 to "same-id", 2 to "same-id", 1 to "older"),
            database.favoriteDao().findAll().map { it.sourceType to it.itemId },
        )
        database.close()
    }

    private fun sample(
        sourceType: Int = 1,
        itemId: String = "same-id",
        addedAt: Long = 10L,
        title: String = "title",
        imgUrl: String = "img",
        description: String = "description",
        extraInformation: String = "extra",
    ) = FavoriteEntity(
        sourceType = sourceType,
        itemId = itemId,
        addedAt = addedAt,
        title = title,
        imgUrl = imgUrl,
        description = description,
        extraInformation = extraInformation,
    )
}

@Database(entities = [FavoriteEntity::class], version = 1, exportSchema = false)
abstract class FavoriteEntityDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
}
