package dk.skancode.skanmate.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dk.skancode.skanmate.haptics.ErrorHaptic
import dk.skancode.skanmate.haptics.StubHaptic
import dk.skancode.skanmate.haptics.SuccessHaptic
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreHaptics.CHHapticEngine

private val supportsHaptics = false && CHHapticEngine.capabilitiesForHardware().supportsHaptics
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberHaptic(kind: HapticKind): Haptic {
    val haptic = remember(kind) {
        if (supportsHaptics) {
            val hapticEngine = CHHapticEngine()

            when (kind) {
                is HapticKind.Success -> SuccessHaptic(hapticEngine)
                is HapticKind.Error -> ErrorHaptic(hapticEngine)
            }
        } else {
            StubHaptic()
        }
    }

    return haptic
}