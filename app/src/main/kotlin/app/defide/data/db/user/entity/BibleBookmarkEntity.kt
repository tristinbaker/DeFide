package app.defide.data.db.user.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bible_bookmarks")
data class BibleBookmarkEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "translation_id") val translationId: String,
    @ColumnInfo(name = "book_number") val bookNumber: Int,
    val chapter: Int,
    val verse: Int,
    val note: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
