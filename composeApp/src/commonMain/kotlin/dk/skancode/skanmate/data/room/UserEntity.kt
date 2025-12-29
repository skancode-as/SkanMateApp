package dk.skancode.skanmate.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import dk.skancode.skanmate.data.model.UserModel

@Entity(tableName = "__auth_user")
data class UserEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val name: String,
    val email: String,
    val emailVerified: Boolean,
    val role: String,
    val image: String?,
    val active: Boolean,
    val createdAt: String,
    val updatedAt: String,
) {
    companion object {
        fun fromModel(model: UserModel): UserEntity {
            return UserEntity(
                id = model.id,
                tenantId = model.tenantId,
                name = model.name,
                email = model.email,
                emailVerified = model.emailVerified,
                role = model.role,
                image = model.image,
                active = model.active,
                createdAt = model.createdAt,
                updatedAt = model.updatedAt
            )
        }
    }
}
