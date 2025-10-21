package dk.skancode.skanmate


interface CameraController {
    val flashState: Boolean

    // Attempts to set the flash state of the camera. Returns true if configuration success
    fun setFlashState(v: Boolean): Boolean
    fun takePicture(cb: (TakePictureResponse) -> Unit)
    // TODO: add function to pause/unpause camera preview
}

data class TakePictureResponse(
    val ok: Boolean,
    /** When ok == false expect this to be null */
    val filePath: String?,
    /** When ok == false expect this to be null */
    val filename: String?,
    /** When ok == false expect this to be null */
    val fileData: ByteArray?,
    /** When ok == true expect this to be null */
    val error: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as TakePictureResponse

        if (ok != other.ok) return false
        if (filePath != other.filePath) return false
        if (filename != other.filename) return false
        if (!fileData.contentEquals(other.fileData)) return false
        if (error != other.error) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ok.hashCode()
        result = 31 * result + (filePath?.hashCode() ?: 0)
        result = 31 * result + (filename?.hashCode() ?: 0)
        result = 31 * result + (fileData?.contentHashCode() ?: 0)
        result = 31 * result + (error?.hashCode() ?: 0)
        return result
    }
}