package dk.skancode.skanmate.util

import androidx.compose.runtime.Composable

interface Haptic {
    fun start()
}

sealed class HapticKind {
    data object Success: HapticKind()
    data object Error: HapticKind()
}

@Composable
expect fun rememberHaptic(kind: HapticKind): Haptic