package dk.skancode.skanmate.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AuthDao {
    @Insert
    suspend fun insertUser(userEntity: UserEntity)
    @Insert
    suspend fun insertTenant(tenantEntity: TenantEntity)
    @Insert
    suspend fun insertSession(sessionEntity: SessionEntity)

    @Query("DELETE FROM __auth_user")
    suspend fun deleteUsers()
    @Query("DELETE FROM __auth_tenant")
    suspend fun deleteTenants()
    @Query("DELETE FROM __auth_session")
    suspend fun deleteSessions()

    @Query("SELECT * FROM __auth_session WHERE token = :token LIMIT 1")
    suspend fun getSessionByToken(token: String): SessionEntity?

    @Query("SELECT * FROM __auth_user WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM __auth_tenant WHERE id = :id LIMIT 1")
    suspend fun getTenantById(id: String): TenantEntity?
}