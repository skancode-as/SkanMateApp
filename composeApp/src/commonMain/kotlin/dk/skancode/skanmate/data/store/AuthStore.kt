package dk.skancode.skanmate.data.store

import dk.skancode.skanmate.data.model.ErrorResponse
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
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

class AuthStore(
    val baseUrl: String,
    val client: HttpClient,
) {
    suspend fun signIn(data: UserSignInDTO): StoreResponse<SignInDTO> {
        return tryCatch {
            val res = client.post("$baseUrl/auth/token") {
                headers {
                    set(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    set("X-Platform", "app")
                }
                setBody(data)
            }

            when (res.status) {
                HttpStatusCode.Created -> {
                    val body: SuccessResponse<SignInDTO> = res.body()

                    return StoreResponse(
                        ok = true,
                        data = body.data,
                        msg = "Success"
                    )
                }

                else -> {
                    val body: ErrorResponse = res.body()

                    println(body.error)

                    return StoreResponse(
                        ok = false,
                        data = null,
                        msg = body.error,
                    )
                }
            }
        }
    }

    suspend fun getUserInfo(
        token: String,
    ): StoreResponse<UserDTO> {
        println(token)
        return tryCatch {
            val res = client.get("$baseUrl/auth/user") {
                headers {
                    set(HttpHeaders.Authorization, "bearer $token")
                }
            }

            when (res.status) {
                HttpStatusCode.OK -> {
                    val body: SuccessResponse<UserDTOResponse> = res.body()

                    return StoreResponse(
                        ok = true,
                        data = body.data.user,
                        msg = "Success"
                    )
                }
                else -> {
                    val body: ErrorResponse = res.body()

                    println(body.error)

                    return StoreResponse(
                        ok = false,
                        data = null,
                        msg = body.error,
                    )
                }
            }
        }
    }

    suspend fun getTenantInfo(
        token: String,
    ): StoreResponse<TenantDTO> {
        return tryCatch {
            val res = client.get("$baseUrl/auth/tenant") {
                headers {
                    set(HttpHeaders.Authorization, "bearer $token")
                }
            }

            when (res.status) {
                HttpStatusCode.OK -> {
                    val body: SuccessResponse<TenantDTOResponse> = res.body()

                    return StoreResponse(
                        ok = true,
                        data = body.data.tenant,
                        msg = "Success"
                    )
                }
                else -> {
                    val body: ErrorResponse = res.body()

                    println(body.error)

                    return StoreResponse(
                        ok = false,
                        data = null,
                        msg = body.error,
                    )
                }
            }
        }
    }

}