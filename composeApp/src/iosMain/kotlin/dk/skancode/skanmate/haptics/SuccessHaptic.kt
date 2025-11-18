package dk.skancode.skanmate.haptics

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreHaptics.CHHapticEngine
import platform.CoreHaptics.CHHapticEventParameterIDHapticIntensity
import platform.CoreHaptics.CHHapticEventTypeHapticTransient
import platform.CoreHaptics.CHHapticPattern
import platform.CoreHaptics.CHHapticPatternKeyEvent
import platform.CoreHaptics.CHHapticPatternKeyEventDuration
import platform.CoreHaptics.CHHapticPatternKeyEventParameters
import platform.CoreHaptics.CHHapticPatternKeyEventType
import platform.CoreHaptics.CHHapticPatternKeyParameterID
import platform.CoreHaptics.CHHapticPatternKeyParameterValue
import platform.CoreHaptics.CHHapticPatternKeyPattern
import platform.CoreHaptics.CHHapticPatternKeyTime
import platform.CoreHaptics.CHHapticTimeImmediate
import kotlin.Any

private val hapticDictionary = mapOf<Any?, Any?>(
    CHHapticPatternKeyPattern to mapOf<Any?, Any?>(
        CHHapticPatternKeyEvent to mapOf<Any?, Any?>(
            CHHapticPatternKeyEventType to CHHapticEventTypeHapticTransient,
            CHHapticPatternKeyTime to CHHapticTimeImmediate,
            CHHapticPatternKeyEventDuration to 1.0,
            CHHapticPatternKeyEventParameters to mapOf<Any?, Any?>(
                CHHapticPatternKeyParameterID to CHHapticEventParameterIDHapticIntensity,
                CHHapticPatternKeyParameterValue to 0.8,
            ),
        ),
        CHHapticPatternKeyEvent to mapOf<Any?, Any?>(
            CHHapticPatternKeyEventType to CHHapticEventTypeHapticTransient,
            CHHapticPatternKeyTime to 1.0,
            CHHapticPatternKeyEventDuration to 0.25,
            CHHapticPatternKeyEventParameters to mapOf<Any?, Any?>(
                CHHapticPatternKeyParameterID to CHHapticEventParameterIDHapticIntensity,
                CHHapticPatternKeyParameterValue to 0.0,
            ),
        ),
        CHHapticPatternKeyEvent to mapOf<Any?, Any?>(
            CHHapticPatternKeyEventType to CHHapticEventTypeHapticTransient,
            CHHapticPatternKeyTime to CHHapticTimeImmediate,
            CHHapticPatternKeyEventDuration to 1.0,
            CHHapticPatternKeyEventParameters to mapOf<Any?, Any?>(
                CHHapticPatternKeyParameterID to CHHapticEventParameterIDHapticIntensity,
                CHHapticPatternKeyParameterValue to 0.8,
            ),
        ),
    )
)

@OptIn(ExperimentalForeignApi::class)
class SuccessHaptic(
    engine: CHHapticEngine,
): BaseHaptic(
    engine = engine,
    pattern = CHHapticPattern(hapticDictionary, null)
)
