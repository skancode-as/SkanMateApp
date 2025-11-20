package dk.skancode.skanmate.util

import dk.skancode.skanmate.AndroidAudioPlayer

actual val AudioPlayerInstance: AudioPlayer
    get() = AndroidAudioPlayer.instance!!