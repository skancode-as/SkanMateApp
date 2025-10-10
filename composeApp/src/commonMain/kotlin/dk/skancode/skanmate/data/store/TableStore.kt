package dk.skancode.skanmate.data.store

import dk.skancode.skanmate.data.model.ErrorResponse
import dk.skancode.skanmate.data.model.FullTableDTO
import dk.skancode.skanmate.data.model.FullTableResponseDTO
import dk.skancode.skanmate.data.model.StoreResponse
import dk.skancode.skanmate.data.model.SuccessResponse
import dk.skancode.skanmate.data.model.TableSummaryDTO
import dk.skancode.skanmate.data.model.TableSummaryModel
import dk.skancode.skanmate.data.model.TableSummaryResponseDTO
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
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
}