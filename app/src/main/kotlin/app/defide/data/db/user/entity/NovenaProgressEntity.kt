package app.defide.data.db.user.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "novena_progress")
data class NovenaProgressEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "novena_id") val novenaId: String,
    @ColumnInfo(name = "start_date") val startDate: String,       // ISO 8601
    @ColumnInfo(name = "last_completed_day") val lastCompletedDay: Int = 0,
    val completed: Boolean = false,
    @ColumnInfo(name = "notifications_enabled") val notificationsEnabled: Boolean = false,
    @ColumnInfo(name = "notification_time") val notificationTime: String?,  // HH:MM
)
