package dk.skancode.skanmate

import dk.skancode.skanmate.util.AudioPlayer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFile
import platform.AVFAudio.AVAudioPlayerNode
import platform.Foundation.NSURL
import skanmate.composeapp.generated.resources.Res

class IosAudioPlayer(
    successAudioUri: String = Res.getUri("files/success.mp3"),
    errorAudioUri: String = Res.getUri("files/error.mp3"),
): AudioPlayer {
    private val successAudioURL = NSURL.URLWithString(URLString = successAudioUri) ?: throw IllegalStateException("Could not load success audio")
    private val errorAudioURL = NSURL.URLWithString(URLString = errorAudioUri) ?: throw IllegalStateException("Could not load error audio")

    private val audioEngine = AVAudioEngine()
    private val audioPlayerNode = AVAudioPlayerNode()
    private var isReleased: Boolean = false

    companion object {
        var instance: IosAudioPlayer? = null
    }

    init {
        audioEngine.attachNode(audioPlayerNode)
        audioEngine.connect(
            audioPlayerNode,
            audioEngine.outputNode,
            null,
        )
        if (instance?.isReleased == false) {
            instance?.release()
            instance = null
        }

        instance = this
    }

    override fun playSuccess() {
        playAudio(url = successAudioURL)
    }

    override fun playError() {
        playAudio(url = errorAudioURL)
    }

    override fun release() {
        audioPlayerNode.stop()
        audioEngine.stop()
        audioEngine.disconnectNodeOutput(audioPlayerNode)
        audioEngine.detachNode(audioPlayerNode)
        isReleased = true
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun playAudio(url: NSURL) {
        val audioFile = AVAudioFile(forReading = url, error = null)

        audioPlayerNode.scheduleFile(audioFile, atTime = null) {
            audioFile.close()
        }

        audioEngine.startAndReturnError(null)
        audioPlayerNode.play()
    }
}