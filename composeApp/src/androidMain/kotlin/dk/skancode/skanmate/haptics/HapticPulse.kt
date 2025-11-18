package dk.skancode.skanmate.haptics

import androidx.annotation.FloatRange
import dk.skancode.skanmate.util.MAX_HAPTIC_AMPLITUDE
import kotlin.math.roundToInt
import kotlin.time.Duration

interface HapticPulse{
    val gap: Duration
    val pulse: Duration
    val amplitude: Int
}

fun HapticPulse(
    gap: Duration,
    pulse: Duration,
    @FloatRange(0.0, 1.0) amplitude: Float,
    maxAmplitude: Int = MAX_HAPTIC_AMPLITUDE
): HapticPulse {
    return object : HapticPulse {
        override val gap: Duration = gap
        override val pulse: Duration = pulse
        override val amplitude: Int = (maxAmplitude * amplitude).roundToInt()
    }
}

typealias HapticPulseArray = MutableList<HapticPulse>

val HapticPulseArray.timings: LongArray
    get() = this.flatMap { pulse ->
        listOf(
            pulse.gap.inWholeMilliseconds,
            pulse.pulse.inWholeMilliseconds
        )
    }.toLongArray()

val HapticPulseArray.amplitudes: IntArray
    get() = this.flatMap { pulse -> listOf(0, pulse.amplitude) }.toIntArray()