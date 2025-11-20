package dk.skancode.skanmate.util

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

interface AudioPlayer {
    fun playSuccess()
    fun playError()
    fun release()
}

expect val AudioPlayerInstance: AudioPlayer

val LocalAudioPlayer: ProvidableCompositionLocal<AudioPlayer> = compositionLocalOf {
    object : AudioPlayer {
        override fun playSuccess() { }
        override fun playError() { }
        override fun release() { }
    }
}
