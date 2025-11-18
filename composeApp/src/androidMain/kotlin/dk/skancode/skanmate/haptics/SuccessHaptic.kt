package dk.skancode.skanmate.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import androidx.annotation.RequiresApi
import kotlin.time.Duration.Companion.milliseconds

@RequiresApi(Build.VERSION_CODES.O)
class SuccessHaptic(
    context: Context,
) : BaseHaptic(
    context = context,
    numPulses = 3
) {
    init {
        val pulses: HapticPulseArray = ArrayList(numPulses)
        pulses.add(
            HapticPulse(
                gap = 75.milliseconds,
                pulse = 75.milliseconds,
                amplitude = 0.4f,
            )
        )
        pulses.add(
            HapticPulse(
                gap = 125.milliseconds,
                pulse = 125.milliseconds,
                amplitude = 0.75f,
            )
        )

        effect = VibrationEffect.createWaveform(pulses.timings, pulses.amplitudes, -1)
    }
}