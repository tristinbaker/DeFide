package com.tristinbaker.defide.data.db.user.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prayer_log")
data class PrayerLogEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "prayer_id") val prayerId: String,
    @ColumnInfo(name = "prayed_at") val prayedAt: Long,
)
