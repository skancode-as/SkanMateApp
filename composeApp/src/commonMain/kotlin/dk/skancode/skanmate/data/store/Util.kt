package dk.skancode.skanmate.data.store

import dk.skancode.skanmate.data.model.StoreResponse

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
