package dk.skancode.skanmate.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "__auth_session")
data class SessionEntity(
    @PrimaryKey val token: String,
    val userId: String,
    val tenantId: String,
)
