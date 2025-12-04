package dk.skancode.skanmate.camera.barcode

import android.content.Context
import android.graphics.Point
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import dev.icerock.moko.permissions.PermissionState
import dk.skancode.skanmate.LocalPermissionsViewModel
import dk.skancode.skanmate.ScannerController
import dk.skancode.skanmate.ui.component.barcode.BarcodeBoundingBox
import dk.skancode.skanmate.ui.component.barcode.BarcodeData
import dk.skancode.skanmate.ui.component.barcode.BarcodeFormat
import dk.skancode.skanmate.ui.component.barcode.BarcodeInfo
import dk.skancode.skanmate.ui.component.barcode.BarcodeResult
import kotlin.collections.emptyList

@Composable
fun AndroidScannerView(
    modifier: Modifier,
    scannerController: ScannerController,
    codeTypes: List<BarcodeFormat>,
    result: (BarcodeResult) -> Unit
) {
    val permissionsViewModel = LocalPermissionsViewModel.current
    if (permissionsViewModel?.cameraState != PermissionState.Granted) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background),
        )
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val backgroundColor = MaterialTheme.colorScheme.background.value.toInt()

    val cameraController = remember(context, lifecycleOwner) {
        val cameraController = LifecycleCameraController(context)
        scannerController.onTorchChange = { cameraController.enableTorch(it) }
        scannerController.onZoomChange = { cameraController.setZoomRatio(it) }
        cameraController.zoomState.observe(lifecycleOwner) { state ->
            scannerController.setMaxZoom(state.maxZoomRatio)
        }

        cameraController
    }

    val previewViewFactory = remember {
        barcodePreviewFactory(
            lifecycleOwner = lifecycleOwner,
            backgroundColor = backgroundColor,
            cameraController = cameraController,
            codeTypes = codeTypes,
            onResult = result,
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize(),
        propagateMinConstraints = true,
    ) {
        AndroidView(
            modifier = Modifier.align(Alignment.Center),
            factory = { ctx -> previewViewFactory(ctx) },
        )
    }
}

fun barcodePreviewFactory(
    lifecycleOwner: LifecycleOwner,
    backgroundColor: Int,
    cameraController: LifecycleCameraController,
    codeTypes: List<BarcodeFormat>,
    onResult: (BarcodeResult) -> Unit,
): (Context) -> PreviewView {
    return { ctx ->
        PreviewView(ctx).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
            setBackgroundColor(backgroundColor)

            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    AndroidBarcodeAnalyzer.getMLKitBarcodeFormats(codeTypes)
                )
                .build()

            // Initialize the barcode scanner client with the configured options
            val barcodeScanner = BarcodeScanning.getClient(options)

            cameraController.imageAnalysisResolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(1280, 720),
                        ResolutionStrategy.FALLBACK_RULE_NONE,
                    )
                )
                .build()

            cameraController.setImageAnalysisAnalyzer(
                ContextCompat.getMainExecutor(ctx),
                AndroidBarcodeAnalyzer(
                    scanner = barcodeScanner,
                    onSuccess = { if (it.isNotEmpty()) onResult(BarcodeResult.OnSuccess(it)) },
                    onFailed = { onResult(BarcodeResult.OnFailed(it)) },
                    onCanceled = { onResult(BarcodeResult.OnCanceled) },
                )
            )

            cameraController.bindToLifecycle(lifecycleOwner)

            this.controller = cameraController
        }
    }
}

fun barcodeAnalyzer(
    ctx: Context,
    barcodeScanner: BarcodeScanner,
    onBarcodeData: (List<BarcodeData>) -> Unit,
): MlKitAnalyzer = MlKitAnalyzer(
    listOf(barcodeScanner),
    ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
    ContextCompat.getMainExecutor(ctx)
) { result ->
    val barcodeResults = result.getValue(barcodeScanner)
    if (!barcodeResults.isNullOrEmpty()) {
        onBarcodeData(barcodeResults.mapNotNull { mlKitBarcode ->
            val displayValue = mlKitBarcode.displayValue ?: return@mapNotNull null
            val corners =
                mlKitBarcode.cornerPoints?.toMutableList()
                    ?: return@mapNotNull null
            val rawBytes = mlKitBarcode.rawBytes ?: displayValue.encodeToByteArray()

            val appSpecificFormat =
                AndroidBarcodeAnalyzer.mlKitFormatToAppFormat(mlKitBarcode.format)

            println("AndroidBarcodeAnalyzer::processFoundBarcodes - barcode: $displayValue, format: $appSpecificFormat, corners: $corners")

            fun Point.toOffset(): Offset {
                return Offset((x.toFloat() / 1280f), (y.toFloat() / 720f))
            }

            BarcodeData(
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
                )
            )
        })
    } else {
        onBarcodeData(emptyList())
    }
}