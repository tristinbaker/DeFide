package com.tristinbaker.defide.data.db.user.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_saints")
data class FavoriteSaintEntity(
    @PrimaryKey
    @ColumnInfo(name = "saint_id")
    val saintId: String,
)
