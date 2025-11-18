package dk.skancode.skanmate.util

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dk.skancode.skanmate.haptics.ErrorHaptic
import dk.skancode.skanmate.haptics.StubHaptic
import dk.skancode.skanmate.haptics.SuccessHaptic

@Composable
actual fun rememberHaptic(kind: HapticKind): Haptic {
    val context = LocalContext.current

    val haptic = remember(kind) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (kind) {
                is HapticKind.Success -> SuccessHaptic(context)
                is HapticKind.Error -> ErrorHaptic(context)
            }
        } else {
            StubHaptic()
        }
    }

    return haptic
}

const val MAX_HAPTIC_AMPLITUDE = 255