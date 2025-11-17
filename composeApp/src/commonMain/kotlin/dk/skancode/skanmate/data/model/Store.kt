package dk.skancode.skanmate.data.model

import kotlinx.serialization.json.JsonElement

data class StoreResponse<T>(
    val ok: Boolean,
    val data: T?,
    val msg: String,
    val details: JsonElement? = null,
    val errorCode: String? = null,
)