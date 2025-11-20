package dk.skancode.skanmate

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player.COMMAND_PREPARE
import androidx.media3.exoplayer.ExoPlayer
import dk.skancode.skanmate.util.AudioPlayer
import skanmate.composeapp.generated.resources.Res

class AndroidAudioPlayer(
    context: Context,
    private val mediaPlayer: ExoPlayer = ExoPlayer.Builder(context).build(),
    successAudioUri: String = Res.getUri("files/success.mp3"),
    errorAudioUri: String = Res.getUri("files/error.mp3"),
): AudioPlayer {
    private val successMediaItem: MediaItem = MediaItem.fromUri(successAudioUri)
    private val errorMediaItem: MediaItem = MediaItem.fromUri(errorAudioUri)
    private var isReleased: Boolean = false

    companion object {
        var instance: AndroidAudioPlayer? = null
    }

    init {
        if (mediaPlayer.isCommandAvailable(COMMAND_PREPARE)) {
            mediaPlayer.prepare()
            println("AndroidAudioPlayer::init - media player prepared")
        } else {
            println("AndroidAudioPlayer::init - COMMAND_PREPARE not available for media player instance")
        }

        if (instance?.isReleased == false) {
            instance?.release()
            instance = null
        }
        instance = this
    }

    override fun playSuccess() {
        playAudio(successMediaItem)
    }

    override fun playError() {
        playAudio(errorMediaItem)
    }

    override fun release() {
        mediaPlayer.release()
        isReleased = true
    }

    private fun playAudio(item: MediaItem) {
        mediaPlayer.setMediaItem(item)
        mediaPlayer.play()
    }
}