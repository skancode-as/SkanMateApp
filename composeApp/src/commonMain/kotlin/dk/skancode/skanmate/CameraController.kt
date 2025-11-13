package dk.skancode.skanmate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State


interface CameraController {
    val minZoom: Float
    val maxZoom: Float

    var zoom: Float
    @get:Composable
    val zoomState: State<Float>

    val flashState: Boolean

    val canSwitchCamera: State<Boolean>
    val canTakePicture: Boolean

    // Attempts to set the flash state of the camera. Returns true if configuration success
    fun setFlashState(v: Boolean): Boolean
    fun takePicture(cb: (TakePictureResponse) -> Unit)
    fun switchCamera()
}

data class ImageData(
    val path: String?,
    val name: String?,
    val data: ByteArray?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ImageData

        if (path != other.path) return false
        if (name != other.name) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path?.hashCode() ?: 0
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (data?.contentHashCode() ?: 0)
        return result
    }
}

data class TakePictureResponse(
    val ok: Boolean,
    /** When ok == false expect this to be null */
    val data: ImageData?,
    /** When ok == true expect this to be null */
    val error: String?,
)