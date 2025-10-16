package dk.skancode.skanmate

interface CameraController {
    fun takePicture(cb: (TakePictureResponse) -> Unit)
}

data class TakePictureResponse(
    val ok: Boolean,
    /** When ok == false expect this to be null */
    val filePath: String?,
    /** When ok == true expect this to be null */
    val error: String?,
)