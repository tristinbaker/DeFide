package app.defide.data.db.user.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rosary_sessions")
data class RosarySessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "mystery_id") val mysteryId: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "completed_at") val completedAt: Long?,
    val completed: Boolean = false,
)
