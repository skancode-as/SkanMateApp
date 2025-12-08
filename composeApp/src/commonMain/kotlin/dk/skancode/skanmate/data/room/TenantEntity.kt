package dk.skancode.skanmate.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import dk.skancode.skanmate.data.model.TenantModel

@Entity(tableName = "__auth_tenant")
data class TenantEntity(
    @PrimaryKey val id: String,
    val name: String,
    val slug: String,
    val logo: String?,
    val createdAt: String,
    val updatedAt: String,
) {
    companion object {
        fun fromModel(model: TenantModel): TenantEntity {
            return TenantEntity(
                id = model.id,
                name = model.name,
                slug = model.slug,
                logo = model.logo,
                createdAt = model.createdAt,
                updatedAt = model.updatedAt,
            )
        }
    }
}
