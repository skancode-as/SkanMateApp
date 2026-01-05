package dk.skancode.skanmate.ui.component.barcode

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

data class BarcodeData(
    val info: BarcodeInfo,
    val box: BarcodeBoundingBox,
    val rect: Rect,
    val corners: List<Offset>
)

data class BarcodeInfo(
    val value: String,
    val format: String,
    val rawBytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BarcodeInfo

        if (value != other.value) return false
        if (format != other.format) return false
        if (!rawBytes.contentEquals(other.rawBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + format.hashCode()
        result = 31 * result + rawBytes.contentHashCode()
        return result
    }
}

data class BarcodeBoundingBox(
    /*x and y in the range 0 to 1*/
    val topLeft: Offset,
    /*x and y in the range 0 to 1*/
    val topRight: Offset,
    /*x and y in the range 0 to 1*/
    val botLeft: Offset,
    /*x and y in the range 0 to 1*/
    val botRight: Offset,
)

