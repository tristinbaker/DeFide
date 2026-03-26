package com.tristinbaker.defide.data.db.user.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "bible_chapter_read",
    primaryKeys = ["translation_id", "book_number", "chapter"],
)
data class BibleChapterReadEntity(
    @ColumnInfo(name = "translation_id") val translationId: String,
    @ColumnInfo(name = "book_number") val bookNumber: Int,
    val chapter: Int,
    @ColumnInfo(name = "read_at") val readAt: Long = System.currentTimeMillis(),
)
