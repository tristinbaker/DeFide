package app.defide.data.db.user.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bible_highlights")
data class BibleHighlightEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "verse_id") val verseId: Int,
    val color: String = "yellow",
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
