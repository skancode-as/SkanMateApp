package dk.skancode.skanmate.data.store

import androidx.compose.ui.text.intl.Locale
import dk.skancode.skanmate.data.model.ErrorResponse
import dk.skancode.skanmate.data.model.StoreResponse
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
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
            val body: ErrorResponse = when (res.headers[HttpHeaders.ContentType]) {
                ContentType.Application.Json.toString() -> res.body()
                else -> ErrorResponse(
                    requestId = "Unknown",
                    code = serverErrorCodeFromStatus(res.status),
                    error = res.bodyAsText(),
                    details = null,
                )
            }
            println(body)

            StoreResponse(
                ok = false,
                data = null,
                msg = body.error,
                details = body.details,
                errorCode = body.code,
            )
        }
    }
}

fun serverErrorCodeFromStatus(status: HttpStatusCode): String {
    return when(status) {
        HttpStatusCode.BadRequest -> ServerErrorCodes.BAD_REQUEST
        HttpStatusCode.Unauthorized -> ServerErrorCodes.UNAUTHORIZED
        HttpStatusCode.Forbidden -> ServerErrorCodes.FORBIDDEN
        HttpStatusCode.NotFound -> ServerErrorCodes.NOT_FOUND
        HttpStatusCode.UnsupportedMediaType -> ServerErrorCodes.UNSUPPORTED_MEDIA_TYPE
        HttpStatusCode.UnprocessableEntity -> ServerErrorCodes.UNPROCESSABLE_ENTITY
        HttpStatusCode.InternalServerError -> ServerErrorCodes.INTERNAL_SERVER_ERROR
        else -> "UNKNOWN"
    }
}

object ServerErrorCodes {
    const val BAD_REQUEST = "BAD_REQUEST"
    const val UNAUTHORIZED = "UNAUTHORIZED"
    const val FORBIDDEN = "FORBIDDEN"
    const val NOT_FOUND = "NOT_FOUND"
    const val UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE"
    const val UNPROCESSABLE_ENTITY = "UNPROCESSABLE_ENTITY"
    const val INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR"
}

fun HeadersBuilder.acceptLanguage(lang: String = Locale.current.language) {
    set(HttpHeaders.AcceptLanguage, lang)
}