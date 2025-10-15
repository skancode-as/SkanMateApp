package dk.skancode.skanmate.data.store

import dk.skancode.skanmate.data.model.ErrorResponse
import dk.skancode.skanmate.data.model.StoreResponse
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode

suspend inline fun<reified T> tryCatch(action: suspend () -> StoreResponse<T>): StoreResponse<T> {
    return try {
        action()
    } catch (e: Exception) {
        println(e.message ?: "Unknown error occurred")
        StoreResponse(
            ok = false,
            data = null,
            msg = e.message ?: "Unknown error occurred"
        )
    }
}

suspend inline fun<reified T> handleResponse(res: HttpResponse, successCode: HttpStatusCode = HttpStatusCode.OK, successAction: (HttpResponse) -> StoreResponse<T>): StoreResponse<T> {
    return when (res.status) {
        successCode -> {
            successAction(res)
        }

        else -> {
            val body: ErrorResponse = res.body()
            println(body)

            StoreResponse(
                ok = false,
                data = null,
                msg = body.error,
                details = body.details,
            )
        }
    }
}