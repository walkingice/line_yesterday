package cc.jchu.naver.line.yesterday.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import cc.jchu.naver.line.yesterday.data.cache.JsonCacheDao
import cc.jchu.naver.line.yesterday.data.cache.JsonCacheEntity
import cc.jchu.naver.line.yesterday.data.favorite.FavoriteDao
import cc.jchu.naver.line.yesterday.data.favorite.FavoriteEntity

@Database(
    entities = [JsonCacheEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jsonCacheDao(): JsonCacheDao

    abstract fun favoriteDao(): FavoriteDao
}
