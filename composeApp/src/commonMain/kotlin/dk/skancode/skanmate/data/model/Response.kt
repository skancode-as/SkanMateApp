package dk.skancode.skanmate.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SuccessResponse<T>(val data: T)

@Serializable
data class ErrorResponse(val requestId: String, val code: String, val error: String)