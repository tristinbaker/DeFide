package com.tristinbaker.defide.data.db.user.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "bible_chapter_read",
    primaryKeys = ["book_number", "chapter"],
)
data class BibleChapterReadEntity(
    @ColumnInfo(name = "book_number") val bookNumber: Int,
    val chapter: Int,
    @ColumnInfo(name = "read_at") val readAt: Long = System.currentTimeMillis(),
)
