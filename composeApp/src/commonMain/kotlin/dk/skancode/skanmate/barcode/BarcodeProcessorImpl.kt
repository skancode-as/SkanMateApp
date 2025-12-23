package dk.skancode.skanmate.barcode

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextMeasurer
import dk.skancode.skanmate.ui.component.barcode.BarcodeData
import dk.skancode.skanmate.ui.component.barcode.BarcodeResult
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalAtomicApi::class)
class BarcodeProcessorImpl(
    private val onResult: (BarcodeResult) -> Unit,
    private val textMeasurer: TextMeasurer? = null,
    private val successThreshold: Int = 5,
    private val barcodeMinCount: Int = 2,
): BarcodeProcessorBase<List<BarcodeData>> {
    private val isSuccess = AtomicBoolean(false)
    private val registeredBarcodeCounts: MutableMap<String, Int> = mutableMapOf()
    private val registeredBarcodes: MutableMap<String, BarcodeData> = mutableMapOf()
    private val boundingBoxRectFactory = { width: Float, height: Float ->
        val center = Offset(
            x = width / 2f,
            y = height / 2f,
        )
        val boxWidth = min(width * 0.8f, height * 0.8f)
        val size = Size(
            width = boxWidth,
            height = boxWidth,
        )

        Rect(
            offset = Offset(
                x = center.x - size.width / 2f,
                y = center.y - size.height / 2f,
            ),
            size = size,
        )
    }

    override fun onSuccess(
        result: List<BarcodeData>,
        graphicOverlay: GraphicOverlay
    ) {
        val boundingBoxGraphic = (graphicOverlay as? BoundingBoxGraphicOverlay)?.boundingBoxGraphic ?: BoundingBoxGraphic(graphicOverlay, boundingBoxRectFactory)

        registeredBarcodeCounts.mapValuesTo(registeredBarcodeCounts) { (_, value) -> max(value - 1, 0)}

        result.forEach { data ->
            val containerColor = when {
                boundingBoxGraphic.contains(data.corners) -> {
                    registeredBarcodes[data.info.value] = data
                    val foundCount = registeredBarcodeCounts[data.info.value] ?: -1
                    registeredBarcodeCounts[data.info.value] = foundCount + 2

                    Color.Green
                }
                else -> Color.Red
            }

            graphicOverlay.addGraphic(
                graphic = BarcodeGraphic(
                    overlay = graphicOverlay,
                    barcode = data,
                    textMeasurer = textMeasurer,
                    containerColor = containerColor,
                )
            )
        }

        graphicOverlay.addGraphic(boundingBoxGraphic)
        if (!isSuccess.load() && registeredBarcodeCounts.values.any { it >= successThreshold }) {
            val barcodes = registeredBarcodeCounts
                .filter { (_, count) -> count >= barcodeMinCount }
                .mapNotNull { (barcode, _) -> barcode }
                .mapNotNull { registeredBarcodes[it] }
            if (barcodes.isNotEmpty()) {
                onResult(BarcodeResult.OnSuccess(barcodes))

                isSuccess.compareAndSet(expectedValue = false, newValue = true)
            }
        }
    }

    override fun onFailure(exception: Exception) {
        onResult(BarcodeResult.OnFailed(exception = exception))
    }

    override fun onCancellation() {
        onResult(BarcodeResult.OnCanceled)
    }
}