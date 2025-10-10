package dk.skancode.skanmate.data.model

data class StoreResponse<T>(val ok: Boolean, val data: T?, val msg: String)