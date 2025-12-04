package dk.skancode.skanmate.ui.component.barcode

sealed interface BarcodeResult {
    data class OnSuccess(val barcodes: List<BarcodeData>) : BarcodeResult

    data class OnFailed(val exception: Exception) : BarcodeResult

    data object OnCanceled : BarcodeResult
}
