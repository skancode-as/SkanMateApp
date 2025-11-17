package dk.skancode.skanmate.data.store

import dk.skancode.skanmate.data.model.SignInDTO
import dk.skancode.skanmate.data.model.StoreResponse
import dk.skancode.skanmate.data.model.SuccessResponse
import dk.skancode.skanmate.data.model.TenantDTO
import dk.skancode.skanmate.data.model.TenantDTOResponse
import dk.skancode.skanmate.data.model.UserDTO
import dk.skancode.skanmate.data.model.UserDTOResponse
import dk.skancode.skanmate.data.model.UserSignInDTO
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class AuthStore(
    val baseUrl: String,
    val client: HttpClient,
) {
    suspend fun signIn(data: UserSignInDTO): StoreResponse<SignInDTO> {
        return tryCatch {
            handleResponse(
                res = client.post("$baseUrl/auth/token") {
                    headers {
                        acceptLanguage()
                        contentType(ContentType.Application.Json)
                        set("X-Platform", "app")
                    }
                    setBody(data)
                },
                successCode = HttpStatusCode.Created,
            ) { res ->
                val body: SuccessResponse<SignInDTO> = res.body()

                return StoreResponse(
                    ok = true,
                    data = body.data,
                    msg = "Success"
                )
            }
        }
    }

    suspend fun getUserInfo(
        token: String,
    ): StoreResponse<UserDTO> {
        return tryCatch {
            handleResponse(
                res = client.get("$baseUrl/auth/user") {
                    headers {
                        acceptLanguage()
                        bearerAuth(token)
                    }
                },
                successCode = HttpStatusCode.OK,
            ) { res ->
                val body: SuccessResponse<UserDTOResponse> = res.body()

                return StoreResponse(
                    ok = true,
                    data = body.data.user,
                    msg = "Success"
                )
            }
        }
    }

    suspend fun getTenantInfo(
        token: String,
    ): StoreResponse<TenantDTO> {
        return tryCatch {
            handleResponse(
                res = client.get("$baseUrl/auth/tenant") {
                    headers {
                        acceptLanguage()
                        bearerAuth(token)
                    }
                },
                successCode = HttpStatusCode.OK,
            ) { res ->
                val body: SuccessResponse<TenantDTOResponse> = res.body()

                return StoreResponse(
                    ok = true,
                    data = body.data.tenant,
                    msg = "Success"
                )
            }
        }
    }
}