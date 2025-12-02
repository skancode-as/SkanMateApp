package dk.skancode.skanmate

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerColors
import org.ncgroup.kscan.ScannerController
import org.ncgroup.kscan.scannerColors

typealias BarcodeType = String
//typealias Gs1Object = MutableMap<String, String>

sealed class ScanEvent {
    data class Barcode(val barcode: String?, val barcodeType: BarcodeType, val ok: Boolean): ScanEvent()
    //data class Gs1(val ok: Boolean, val isGs1: Boolean, val gs1: Gs1Object, val barcode: String?, val barcodeType: BarcodeType): ScanEvent()
}

fun interface ScanEventHandler {
    fun handle(event: ScanEvent)
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
    @get:Composable
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
fun rememberImageResource(path: String? = null): ImageResource<Painter> {
    return remember(path) {
        object : ImageResource<Painter> {
            private val stateFlow: MutableStateFlow<ImageResourceState<Painter>> = MutableStateFlow(ImageResourceState.Unspecified)

            @get:Composable
            override val state: State<ImageResourceState<Painter>>
                get() = stateFlow.collectAsState()

            @get:Composable
            override val isLoading: State<Boolean>
                get() = rememberUpdatedState(stateFlow.collectAsState().value == ImageResourceState.Loading)

            override fun load() {
                stateFlow.update { ImageResourceState.Loading }
            }

            override fun update(painter: Painter) {
                stateFlow.update { ImageResourceState.Image(painter) }
            }

            override fun error(error: String) {
                stateFlow.update { ImageResourceState.Error(error) }
            }

            override fun reset() {
                stateFlow.update { ImageResourceState.Unspecified }
            }
        }
    }
}

@Composable
expect fun loadImage(imagePath: String?): ImageResource<Painter>

expect suspend fun deleteFile(path: String)

@Composable
expect fun SkanMateScannerView(
    modifier: Modifier = Modifier.fillMaxSize(),
    codeTypes: List<BarcodeFormat>,
    colors: ScannerColors = scannerColors(),
    showUi: Boolean = true,
    scannerController: ScannerController? = null,
    filter: (Barcode) -> Boolean = { true },
    result: (BarcodeResult) -> Unit,
)
