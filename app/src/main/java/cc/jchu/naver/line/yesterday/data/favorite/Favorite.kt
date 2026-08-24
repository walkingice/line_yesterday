package cc.jchu.naver.line.yesterday.data.favorite

import androidx.room.Entity

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
