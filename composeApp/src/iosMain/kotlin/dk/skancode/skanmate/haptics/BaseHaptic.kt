package dk.skancode.skanmate.haptics

import dk.skancode.skanmate.util.Haptic
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreHaptics.CHHapticEngine
import platform.CoreHaptics.CHHapticEngineFinishedActionStopEngine
import platform.CoreHaptics.CHHapticPattern

abstract class BaseHaptic(
    val engine: CHHapticEngine,
    val pattern: CHHapticPattern,
): Haptic {
    @OptIn(ExperimentalForeignApi::class)
    override fun start() {
        val player = engine.createPlayerWithPattern(pattern, null)

        engine.notifyWhenPlayersFinished { error ->
            println("Finished playing haptic. Error: $error")
            CHHapticEngineFinishedActionStopEngine
        }
        engine.startWithCompletionHandler { error ->
            if (error == null) {
                player?.startAtTime(0.0, null)
            } else {
                println("Failed to start haptic engine Error: $error")
            }
        }
    }
}