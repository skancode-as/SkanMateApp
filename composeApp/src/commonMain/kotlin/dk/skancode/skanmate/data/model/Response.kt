package dk.skancode.skanmate.data.model

import dk.skancode.skanmate.util.jsonSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

@Serializable
data class SuccessResponse<T>(val data: T)

@Serializable
data class ErrorResponse(val requestId: String, val code: String, val error: String, val details: JsonElement?)

@Serializable
data class TableRowErrors(
    @SerialName("fieldErrors")
    val columnErrors: Map<String, List<String>>
) {
    companion object {
        fun decode(jsonElement: JsonElement?): TableRowErrors? {
            if (jsonElement == null) return null

            return try {
                jsonSerializer.decodeFromJsonElement(jsonElement)
            } catch (_: Exception) {
                null
            }
        }
    }
}
