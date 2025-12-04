package dk.skancode.skanmate.camera.barcode

import dk.skancode.skanmate.ui.component.barcode.BarcodeData
import dk.skancode.skanmate.ui.component.barcode.BarcodeFormat

data class AndroidBarcodeSettings(
    val codeTypes: List<BarcodeFormat> = emptyList(),
    val onSuccess: ((List<BarcodeData>) -> Unit)? = null,
    val onFailed: ((Exception) -> Unit)? = null,
    val onCanceled: (() -> Unit)? = null,
) {
    val enabled: Boolean
        get() = codeTypes.isNotEmpty() && (onSuccess != null || onFailed != null || onCanceled != null)
}
