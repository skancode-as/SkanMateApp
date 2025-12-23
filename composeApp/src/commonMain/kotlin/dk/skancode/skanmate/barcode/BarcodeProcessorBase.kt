package dk.skancode.skanmate.barcode

interface BarcodeProcessorBase<T> {
    fun onSuccess(result: T, graphicOverlay: GraphicOverlay)
    fun onFailure(exception: Exception)
    fun onCancellation()
}