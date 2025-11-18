package dk.skancode.skanmate.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import androidx.annotation.RequiresApi
import java.util.ArrayList
import kotlin.time.Duration.Companion.milliseconds

@RequiresApi(Build.VERSION_CODES.O)
class ErrorHaptic(
    context: Context,
) : BaseHaptic(
    context = context,
    numPulses = 2,
) {
    init {
        val pulses: HapticPulseArray = ArrayList(numPulses)

        pulses.add(
            HapticPulse(
                gap = 0.milliseconds,
                pulse = 350.milliseconds,
                amplitude = 1f,
            )
        )

        effect = VibrationEffect.createWaveform(pulses.timings, pulses.amplitudes, -1)
    }
}