package dk.skancode.skanmate.data.store

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
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class TableStore(val client: HttpClient, val baseUrl: String) {
    suspend fun fetchTableSummaries(token: String): StoreResponse<List<TableSummaryDTO>> {
        return tryCatch {
            handleResponse(
                res = client.get("$baseUrl/tables") {
                    headers {
                        acceptLanguage()
                        bearerAuth(token)
                    }
                },
                successCode = HttpStatusCode.OK,
            ) { res ->
                val body: SuccessResponse<TableSummaryResponseDTO> = res.body()

                return StoreResponse(
                    ok = true,
                    data = body.data.tables,
                    msg = "Success"
                )
            }
        }
    }

    suspend fun fetchFullTable(tableId: String, token: String): StoreResponse<FullTableDTO> {
        return tryCatch {
            handleResponse(
                res = client.get("$baseUrl/tables/$tableId") {
                    headers {
                        acceptLanguage()
                        bearerAuth(token)
                    }
                },
                successCode = HttpStatusCode.OK,
            ) { res ->
                val body: SuccessResponse<FullTableResponseDTO> = res.body()

                return StoreResponse(
                    ok = true,
                    data = body.data.table,
                    msg = "Success"
                )
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
                        acceptLanguage()
                        bearerAuth(token)
                        contentType(type = ContentType.Application.Json)
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
            handleResponse(
                res = client.put(presignedUrl) {
                    headers {
                        contentType(type = imageType)
                    }
                    setBody(data)
                },
                successCode = HttpStatusCode.OK,
            ) {
                StoreResponse(
                    ok = true,
                    Unit,
                    "Success",
                )
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
                        acceptLanguage()
                        bearerAuth(token)
                        contentType(type = ContentType.Application.Json)
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