package cc.jchu.naver.line.yesterday.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cc.jchu.naver.line.yesterday.data.cache.JsonCacheDao
import cc.jchu.naver.line.yesterday.data.cache.JsonCacheEntity
import cc.jchu.naver.line.yesterday.data.favorite.FavoriteDao
import cc.jchu.naver.line.yesterday.data.favorite.FavoriteEntity

@Database(
    entities = [JsonCacheEntity::class, FavoriteEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jsonCacheDao(): JsonCacheDao

    abstract fun favoriteDao(): FavoriteDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE favorite ADD COLUMN time TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
