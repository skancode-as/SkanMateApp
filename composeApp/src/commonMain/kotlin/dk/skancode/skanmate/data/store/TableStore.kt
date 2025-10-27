package dk.skancode.skanmate.data.store

import dk.skancode.skanmate.data.model.ErrorResponse
import dk.skancode.skanmate.data.model.FullTableDTO
import dk.skancode.skanmate.data.model.FullTableResponseDTO
import dk.skancode.skanmate.data.model.PresignedUrlDTO
import dk.skancode.skanmate.data.model.RowData
import dk.skancode.skanmate.data.model.StoreResponse
import dk.skancode.skanmate.data.model.SuccessResponse
import dk.skancode.skanmate.data.model.TableImageFilenameDTO
import dk.skancode.skanmate.data.model.TableImagePresignedURL
import dk.skancode.skanmate.data.model.TableSummaryDTO
import dk.skancode.skanmate.data.model.TableSummaryResponseDTO
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

class TableStore(val client: HttpClient, val baseUrl: String) {
    suspend fun fetchTableSummaries(token: String): StoreResponse<List<TableSummaryDTO>> {
        return tryCatch {
            val res = client.get("$baseUrl/tables") {
                headers {
                    set(HttpHeaders.Authorization, "bearer $token")
                }
            }

            when (res.status) {
                HttpStatusCode.OK -> {
                    val body: SuccessResponse<TableSummaryResponseDTO> = res.body()

                    return StoreResponse(
                        ok = true,
                        data = body.data.tables,
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

    suspend fun fetchFullTable(tableId: String, token: String): StoreResponse<FullTableDTO> {
        return tryCatch {
            val res = client.get("$baseUrl/tables/$tableId") {
                headers {
                    set(HttpHeaders.Authorization, "bearer $token")
                }
            }

            when (res.status) {
                HttpStatusCode.OK -> {
                    val body: SuccessResponse<FullTableResponseDTO> = res.body()

                    return StoreResponse(
                        ok = true,
                        data = body.data.table,
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

    suspend fun getPresignedURL(
        tableId: String,
        filename: String,
        token: String,
    ): StoreResponse<PresignedUrlDTO> {
        return tryCatch {
            handleResponse(
                res = client.post("$baseUrl/tables/$tableId/upload") {
                    headers {
                        set(HttpHeaders.Authorization, "bearer $token")
                        set(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }
                    setBody(TableImageFilenameDTO(filename = filename))
                },
                successCode = HttpStatusCode.Created,
            ) { res ->
                val body: TableImagePresignedURL = res.body()

                StoreResponse(
                    ok = true,
                    data = body.data,
                    msg = "Success"
                )
            }
        }
    }

    suspend fun uploadImage(
        presignedUrl: String,
        data: ByteArray,
        imageType: ContentType,
    ): StoreResponse<Unit> {
        return tryCatch {
            val res = client.put(presignedUrl) {
                headers {
                    set(HttpHeaders.ContentType, imageType.toString())
                }
                setBody(data)
            }

            when (res.status) {
                HttpStatusCode.OK -> {
                    StoreResponse(
                        ok= true,
                        Unit,
                        "Success",
                    )
                }

                else -> {
                    val body = res.bodyAsText()
                    println(body)

                    StoreResponse(
                        ok = false,
                        data = null,
                        msg = "Image upload failed with response code ${res.status}"
                    )
                }
            }
        }
    }

    suspend fun submitTableData(
        tableId: String,
        data: List<RowData>,
        token: String,
    ): StoreResponse<Unit> {
        return tryCatch {
            return handleResponse(
                res = client.post("$baseUrl/tables/$tableId") {
                    headers {
                        set(HttpHeaders.Authorization, "bearer $token")
                        set(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }
                    setBody(data)
                },
                successCode = HttpStatusCode.NoContent,
            ) {
                StoreResponse(
                    ok = true,
                    data = null,
                    msg = "Success",
                )
            }
        }
    }
}