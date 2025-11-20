package dk.skancode.skanmate.util

import dk.skancode.skanmate.IosAudioPlayer

actual val AudioPlayerInstance: AudioPlayer
    get() = IosAudioPlayer.instance!!