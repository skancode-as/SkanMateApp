package dk.skancode.skanmate.data.store

import dk.skancode.skanmate.data.model.TenantModel
import dk.skancode.skanmate.data.model.UserModel
import dk.skancode.skanmate.data.room.AuthDao
import dk.skancode.skanmate.data.room.SessionEntity
import dk.skancode.skanmate.data.room.TenantEntity
import dk.skancode.skanmate.data.room.UserEntity

class LocalAuthStore(
    val dao: AuthDao = LocalDbStore.authDao
) {
    suspend fun loadUserAndTenant(token: String): Pair<UserModel?, TenantModel?> {
        val session = dao.getSessionByToken(token) ?: return null to null
        val userEntity = dao.getUserById(session.userId) ?: return null to null
        val tenantEntity = dao.getTenantById(session.tenantId) ?: return null to null

        return UserModel(
            id = userEntity.id,
            tenantId = userEntity.tenantId,
            name = userEntity.name,
            email = userEntity.email,
            emailVerified = userEntity.emailVerified,
            role = userEntity.role,
            image = userEntity.image,
            active = userEntity.active,
            createdAt = userEntity.createdAt,
            updatedAt = userEntity.updatedAt,
        ) to TenantModel(
            id = tenantEntity.id,
            name = tenantEntity.name,
            slug = tenantEntity.slug,
            logo = tenantEntity.logo,
            createdAt = tenantEntity.createdAt,
            updatedAt = tenantEntity.updatedAt,
        )
    }

    suspend fun storeUserAndTenant(token: String, userModel: UserModel, tenantModel: TenantModel) {
        dao.insertUser(UserEntity.fromModel(userModel))
        dao.insertTenant(TenantEntity.fromModel(tenantModel))
        dao.insertSession(SessionEntity(
            token = token,
            userId = userModel.id,
            tenantId = tenantModel.id
        ))
    }

    suspend fun invalidateLocalData() {
        dao.deleteSessions()
        dao.deleteUsers()
        dao.deleteTenants()
    }
}