package dk.skancode.skanmate.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserModel(
    val id: String,
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
        fun empty(): UserModel {
            return UserModel(
                id = "",
                tenantId = "",
                name = "",
                email = "",
                emailVerified = false,
                role = "",
                image = null,
                active = false,
                createdAt = "",
                updatedAt = "",
            )
        }
    }
}

@Serializable
data class UserDTOResponse(val user: UserDTO)
@Serializable
data class UserDTO(
    val id: String,
    val tenantId: String,
    val name: String,
    val email: String,
    val emailVerified: Boolean,
    val role: String,
    val image: String?,
    val active: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class UserSignInDTO(val email: String, val credential: String, val type: UserCredentialType)

typealias UserCredentialType = String
const val PinCredentialType: UserCredentialType = "pin"
const val CredentialCredentialType: UserCredentialType = "credential"

@Serializable
data class SignInResult(
    val user: UserModel?,
    val tenant: TenantModel?,
)

@Serializable
data class SignInUnprocessableEntityDetails(
    val email: List<String>? = null,
    val credential: List<String>? = null,
    val type: List<String>? = null,
)

@Serializable
data class SignInDTO(val token: String)

@Serializable
data class TenantModel(
    val id: String,
    val name: String,
    val slug: String,
    val logo: String?,
    val createdAt: String,
    val updatedAt: String,
) {
    companion object {
        fun empty(): TenantModel {
            return TenantModel(
                id = "",
                name = "",
                slug = "",
                logo = null,
                createdAt = "",
                updatedAt = "",
            )
        }
    }
}

@Serializable
data class TenantDTOResponse(
    val tenant: TenantDTO,
)

@Serializable
data class TenantDTO(
    val id: String,
    val name: String,
    val slug: String,
    val logo: String?,
    val createdAt: String,
    val updatedAt: String,
)