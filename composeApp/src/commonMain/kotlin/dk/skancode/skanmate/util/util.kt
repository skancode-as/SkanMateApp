package dk.skancode.skanmate.util

fun assert(condition: Boolean, msg: String? = null) {
    if (!condition) {
        throw AssertionError(msg ?: "Assertion failed")
    }
}