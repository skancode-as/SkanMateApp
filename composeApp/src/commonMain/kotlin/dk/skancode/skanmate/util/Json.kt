package dk.skancode.skanmate.util

import kotlinx.serialization.json.Json

val jsonSerializer = Json {
    ignoreUnknownKeys = true
}