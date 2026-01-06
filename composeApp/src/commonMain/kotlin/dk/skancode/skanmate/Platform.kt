package dk.skancode.skanmate

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import com.russhwolf.settings.Settings
import dk.skancode.skanmate.ui.component.barcode.BarcodeFormat
import dk.skancode.skanmate.ui.component.barcode.BarcodeResult

typealias BarcodeType = String
//typealias Gs1Object = MutableMap<String, String>

sealed class ScanEvent {
    data class Barcode(val barcode: String?, val barcodeType: BarcodeType, val ok: Boolean): ScanEvent()
    //data class Gs1(val ok: Boolean, val isGs1: Boolean, val gs1: Gs1Object, val barcode: String?, val barcodeType: BarcodeType): ScanEvent()
}

fun interface ScanEventHandler {
    fun handleEvents(events: List<ScanEvent>)
}

interface ScanModule {
    fun isHardwareScanner(): Boolean
    fun registerListener(handler: ScanEventHandler)
    fun unregisterListener(handler: ScanEventHandler)
    fun enableScan()
    fun disableScan()
//    fun enableGs1()
//    fun disableGs1()
}

@Composable
expect fun rememberScanModule(): ScanModule

expect val platformSettingsFactory: Settings.Factory

@Composable
expect fun CameraView(
    modifier: Modifier = Modifier,
    cameraUi: @Composable BoxScope.(CameraController) -> Unit,
)

interface ImageResource<T: Painter> {
    @get:Composable
    val isLoading: State<Boolean>
    val state: State<ImageResourceState<T>>

    fun load()
    fun update(painter: T)
    fun error(error: String)
    fun reset()

}
sealed class ImageResourceState<T: Painter>() {
    data object Unspecified: ImageResourceState<Painter>()
    data object Loading: ImageResourceState<Painter>()
    data class Image<T: Painter>(val data: T): ImageResourceState<T>()
    data class Error(val error: String): ImageResourceState<Painter>()
}

@Composable
fun rememberImageResource(path: String?): ImageResource<Painter> {
    val resource = remember(path) {
        object : ImageResource<Painter> {
            private val internalState = mutableStateOf<ImageResourceState<Painter>>(ImageResourceState.Unspecified)

            override val state: State<ImageResourceState<Painter>>
                get() = internalState

            @get:Composable
            override val isLoading: State<Boolean>
                get() = rememberUpdatedState(internalState.value == ImageResourceState.Loading)

            override fun load() {
                internalState.value = ImageResourceState.Loading
            }

            override fun update(painter: Painter) {
                internalState.value = ImageResourceState.Image(painter)
            }

            override fun error(error: String) {
                internalState.value = ImageResourceState.Error(error)
            }

            override fun reset() {
                internalState.value = ImageResourceState.Unspecified
            }
        }
    }

    println("rememberImageResource($path) - resource: $resource")

    DisposableEffect(resource) {
        onDispose {
            println("Resetting resource onDispose $resource")
            resource.reset()
        }
    }

    return resource
}

expect suspend fun loadLocalImage(imagePath: String): ImageData

@Composable
expect fun loadImage(imagePath: String?): ImageResource<Painter>

expect suspend fun deleteFile(path: String)

@Composable
expect fun SkanMateScannerView(
    modifier: Modifier = Modifier.fillMaxSize(),
    codeTypes: List<BarcodeFormat>,
    scannerController: ScannerController? = null,
    result: (BarcodeResult) -> Unit,
)

expect class ScannerController() {
    val torchEnabled: Boolean
    val zoomRatio: Float
    val maxZoomRatio: Float

    fun setTorch(enabled: Boolean)

    fun setZoom(ratio: Float)
}
