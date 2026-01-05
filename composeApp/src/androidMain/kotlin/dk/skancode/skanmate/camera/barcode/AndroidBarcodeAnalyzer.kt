package dk.skancode.skanmate.camera.barcode

import android.graphics.Matrix
import android.graphics.Point
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dk.skancode.skanmate.barcode.BarcodeProcessorBase
import dk.skancode.skanmate.barcode.GraphicOverlay
import dk.skancode.skanmate.ui.component.barcode.BarcodeBoundingBox
import dk.skancode.skanmate.ui.component.barcode.BarcodeData
import dk.skancode.skanmate.ui.component.barcode.BarcodeFormat
import dk.skancode.skanmate.ui.component.barcode.BarcodeInfo

class AndroidBarcodeAnalyzer(
    private val scanner: BarcodeScanner,
    private val graphicOverlay: GraphicOverlay,
    private val processor: BarcodeProcessorBase<List<BarcodeData>>,
    private val coordinateSystem: Int = ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
    private val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
): ImageAnalysis.Analyzer {
    override fun getTargetCoordinateSystem(): Int {
        return coordinateSystem
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        updateGraphicOverlay(imageProxy)

        imageProxy.image?.let { mediaImage ->
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(inputImage)
                .addOnSuccessListener { mlKitBarcodes ->
                    //println("AndroidBarcodeAnalyzer::process() - onSuccessListener: ${mlKitBarcodes.size} barcodes found!")
                    graphicOverlay.clear()
                    val result = processFoundBarcodes(
                        mlKitBarcodes = mlKitBarcodes,
                        imageWidth = mediaImage.width.toFloat(),
                        imageHeight = inputImage.height.toFloat(),
                    )
                    processor.onSuccess(result, graphicOverlay)
                }
                .addOnFailureListener {
                    println("AndroidBarcodeAnalyzer::process() - onFailureListener: $it exception thrown")
                    it.printStackTrace()
                    processor.onFailure(it)
                }
                .addOnCanceledListener {
                    println("AndroidBarcodeAnalyzer::process() - onCanceledListener: image processing cancelled")
                    processor.onCancellation()
                }
                .addOnCompleteListener { imageProxy.close() }
        } ?: imageProxy.close()
    }

    private fun updateGraphicOverlay(imageProxy: ImageProxy) {
        val isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT
        graphicOverlay.setImageSourceInfo(
            imageProxy.height, imageProxy.width, isImageFlipped
        )
//        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
//        if (rotationDegrees == 0 || rotationDegrees == 180) {
//        graphicOverlay.setImageSourceInfo(
//            imageProxy.width, imageProxy.height, !isImageFlipped
//        )
//        } else {
//        }
    }

    private fun processFoundBarcodes(
        mlKitBarcodes: List<Barcode>,
        imageWidth: Float,
        imageHeight: Float,
    ): List<BarcodeData> {
        return mlKitBarcodes.mapNotNull { mlKitBarcode ->
            val displayValue = mlKitBarcode.displayValue ?: return@mapNotNull null
            val corners = mlKitBarcode.cornerPoints?.toMutableList() ?: return@mapNotNull null
            val rect = mlKitBarcode.boundingBox ?: return@mapNotNull null
            val rawBytes = mlKitBarcode.rawBytes ?: displayValue.encodeToByteArray()

            val appSpecificFormat = mlKitFormatToAppFormat(mlKitBarcode.format)

            println("AndroidBarcodeAnalyzer::processFoundBarcodes - barcode: $displayValue, format: $appSpecificFormat, corners: $corners, imageSize: (x: $imageWidth, y: $imageHeight)")

            fun Point.toOffset(): Offset {
                return Offset(x.toFloat(), y.toFloat())
            }

            val data = BarcodeData(
                info = BarcodeInfo(
                    value = displayValue,
                    format = appSpecificFormat.toString(),
                    rawBytes = rawBytes,
                ),
                box = BarcodeBoundingBox(
                    topLeft = corners[0].toOffset(),
                    topRight = corners[1].toOffset(),
                    botRight = corners[2].toOffset(),
                    botLeft = corners[3].toOffset(),
                ),
                rect = Rect(
                    left = rect.left.toFloat(),
                    right = rect.right.toFloat(),
                    top = rect.top.toFloat(),
                    bottom = rect.bottom.toFloat(),
                ),
                corners = corners.map { it.toOffset() }
            )
            data
        }
    }

    companion object {
        private val APP_TO_MLKIT_FORMAT_MAP: Map<BarcodeFormat, Int> =
            mapOf(
                BarcodeFormat.FORMAT_QR_CODE to Barcode.FORMAT_QR_CODE,
                BarcodeFormat.FORMAT_CODE_128 to Barcode.FORMAT_CODE_128,
                BarcodeFormat.FORMAT_CODE_39 to Barcode.FORMAT_CODE_39,
                BarcodeFormat.FORMAT_CODE_93 to Barcode.FORMAT_CODE_93,
                BarcodeFormat.FORMAT_CODABAR to Barcode.FORMAT_CODABAR,
                BarcodeFormat.FORMAT_DATA_MATRIX to Barcode.FORMAT_DATA_MATRIX,
                BarcodeFormat.FORMAT_EAN_13 to Barcode.FORMAT_EAN_13,
                BarcodeFormat.FORMAT_EAN_8 to Barcode.FORMAT_EAN_8,
                BarcodeFormat.FORMAT_ITF to Barcode.FORMAT_ITF,
                BarcodeFormat.FORMAT_UPC_A to Barcode.FORMAT_UPC_A,
                BarcodeFormat.FORMAT_UPC_E to Barcode.FORMAT_UPC_E,
                BarcodeFormat.FORMAT_PDF417 to Barcode.FORMAT_PDF417,
                BarcodeFormat.FORMAT_AZTEC to Barcode.FORMAT_AZTEC,
            )

        private val MLKIT_TO_APP_FORMAT_MAP: Map<Int, BarcodeFormat> =
            APP_TO_MLKIT_FORMAT_MAP.entries.associateBy({ it.value }) { it.key }
                .plus(Barcode.FORMAT_UNKNOWN to BarcodeFormat.FORMAT_UNKNOWN)

        fun getMLKitBarcodeFormats(appFormats: List<BarcodeFormat>): Int {
            if (appFormats.isEmpty() || appFormats.contains(BarcodeFormat.FORMAT_ALL_FORMATS)) {
                return Barcode.FORMAT_ALL_FORMATS
            }

            return appFormats
                .mapNotNull { APP_TO_MLKIT_FORMAT_MAP[it] }
                .distinct()
                .fold(0) { acc, formatInt -> acc or formatInt }
                .let { if (it == 0) Barcode.FORMAT_ALL_FORMATS else it }
        }

        fun mlKitFormatToAppFormat(mlKitFormat: Int): BarcodeFormat {
            return MLKIT_TO_APP_FORMAT_MAP[mlKitFormat] ?: BarcodeFormat.FORMAT_UNKNOWN
        }
    }
}
