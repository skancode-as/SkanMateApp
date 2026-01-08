package dk.skancode.skanmate

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import androidx.camera.core.impl.utils.Exif
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.core.database.getStringOrNull
import com.russhwolf.settings.Settings
import dk.skancode.skanmate.ui.component.LocalCameraScanManager
import dk.skancode.barcodescannermodule.compose.LocalScannerModule
import androidx.core.net.toUri
import dk.skancode.skanmate.barcode.BarcodeProcessorBase
import dk.skancode.skanmate.barcode.BarcodeProcessorImpl
import dk.skancode.skanmate.barcode.BoundingBoxGraphicOverlay
import dk.skancode.skanmate.camera.AndroidCameraView
import dk.skancode.skanmate.camera.CameraPermissionAlert
import dk.skancode.skanmate.camera.barcode.AndroidScannerView
import dk.skancode.skanmate.ui.component.LocalUiCameraController
import dk.skancode.skanmate.ui.component.barcode.BarcodeData
import dk.skancode.skanmate.ui.component.barcode.BarcodeFormat
import dk.skancode.skanmate.ui.component.barcode.BarcodeResult
import dk.skancode.skanmate.util.clamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

@Composable
actual fun rememberScanModule(): ScanModule {
    val localModule = LocalScannerModule.current
    val localCameraScanManager = LocalCameraScanManager.current

    return remember(localModule) {
        AndroidScanModuleImpl(localModule, localCameraScanManager)
    }
}

actual val platformSettingsFactory: Settings.Factory = SkanMateApplication.settingsFactory

@Composable
actual fun CameraView(
    modifier: Modifier,
    cameraUi: @Composable BoxScope.(CameraController) -> Unit,
) {
    val uiCameraController = LocalUiCameraController.current
    AndroidCameraView(modifier = modifier, cameraUi = cameraUi)

    CameraPermissionAlert(
        onDismissRequest = {
            uiCameraController.stopCamera()
        }
    )
}

private object LocalImageLoader {
    private val map = ConcurrentHashMap<String, Bitmap>()

    @SuppressLint("RestrictedApi")
    suspend fun loadBitmapFromUri(uri: Uri, context: Context): Bitmap? {
        if (map.containsKey(uri.toString())) {
            return map.getValue(uri.toString())
        }

        var inputStream = context.contentResolver.openInputStream(uri)

        val rotation = inputStream?.use { inputStream ->
            val exif = Exif.createFromInputStream(inputStream)
            exif.rotation
        } ?: return null

        inputStream = context.contentResolver.openInputStream(uri)

        return inputStream?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)?.rotate(rotation)
        }.also { bitmap ->
            if (bitmap != null) {
                map[uri.toString()] = bitmap
            }
        }
    }

}

actual suspend fun loadLocalImage(imagePath: String): ImageData {
    val context = SkanMateApplication.applicationContext

    val uri: Uri = imagePath.toUri()
    val name: String? = uri.scheme?.let { scheme ->
        when (scheme) {
            "content" -> {
                context.contentResolver.query(
                    uri,
                    arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    println("RowCount: ${cursor.count}, ColumnCount: ${cursor.columnCount}, ColumnNames: ${cursor.columnNames.joinToString()}")
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                        cursor.getStringOrNull(index)
                    } else null
                }
            }

            else -> uri.path
        }
    }

    println("Image name ($name) for path: $imagePath")

    val data: ByteArray? = context.contentResolver.openInputStream(uri)?.readBytes()

    println("Image name: $name, byteCount: ${data?.size}")

    return ImageData(
        path = imagePath,
        name = name,
        data = data,
    )
}

@Composable
actual fun loadImage(imagePath: String?): ImageResource<Painter> {
    val resource = rememberImageResource(imagePath)

    val context = LocalContext.current
    LaunchedEffect(context, resource, imagePath) {
        println("loadImage::LaunchedEffect($context, $resource)")

        if (imagePath == null) return@LaunchedEffect
        launch(Dispatchers.IO) {
            val bitmap: Bitmap =
                LocalImageLoader.loadBitmapFromUri(uri = imagePath.toUri(), context = context) ?: run {
                    println("Could not open input stream at imagePath: $imagePath")
                    resource.error("Could not open input stream at imagePath: $imagePath")
                    return@launch
                }

            resource.update(
                painter = BitmapPainter(bitmap.asImageBitmap())
            )
        }
    }

    return resource
}

fun Bitmap.rotate(degrees: Number): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    val rotatedBitmap =
        Bitmap.createBitmap(this.copy(config!!, isMutable), 0, 0, width, height, matrix, true)
    this.recycle()

    return rotatedBitmap
}

actual suspend fun deleteFile(path: String) {
    SkanMateApplication.deleteLocalFile(path)
}

@Composable
actual fun SkanMateScannerView(
    modifier: Modifier,
    codeTypes: List<BarcodeFormat>,
    scannerController: ScannerController?,
    result: (BarcodeResult) -> Unit
) {
    val scannerController = scannerController ?: remember { ScannerController() }

    val textMeasurer = rememberTextMeasurer()
    val processor: BarcodeProcessorBase<List<BarcodeData>> = remember(result) { BarcodeProcessorImpl(onResult = result, textMeasurer = textMeasurer) }

    val barcodeOverlay = remember { BoundingBoxGraphicOverlay() }

    AndroidScannerView(
        modifier = modifier,
        codeTypes = codeTypes,
        scannerController = scannerController,
        processor = processor,
        barcodeOverlay = barcodeOverlay,
    )
}

actual class ScannerController {
    private var _torchEnabled by mutableStateOf(false)
    actual val torchEnabled: Boolean
        get() = _torchEnabled

    private var _zoomRatio by mutableStateOf(1f)
    actual val zoomRatio: Float
        get() = _zoomRatio

    private var _maxZoomRatio by mutableStateOf(1f)
    actual val maxZoomRatio: Float
        get() = _maxZoomRatio

    fun setMaxZoom(ratio: Float) {
        _maxZoomRatio = ratio
    }

    var onTorchChange: (Boolean) -> Unit = {}
    var onZoomChange: (Float) -> Unit = {}

    actual fun setTorch(enabled: Boolean) {
        _torchEnabled = enabled
        onTorchChange(enabled)
    }

    actual fun setZoom(ratio: Float) {
        ratio.clamp(1f, maxZoomRatio).also { zoom ->
            _zoomRatio = zoom
            onZoomChange(zoom)
        }
    }
}