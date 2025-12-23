package dk.skancode.skanmate.camera.barcode

import android.content.Context
import android.graphics.Rect
import android.util.Size
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import dev.icerock.moko.permissions.PermissionState
import dk.skancode.skanmate.LocalPermissionsViewModel
import dk.skancode.skanmate.ScannerController
import dk.skancode.skanmate.barcode.BarcodeProcessorBase
import dk.skancode.skanmate.barcode.BaseGraphicOverlay
import dk.skancode.skanmate.barcode.GraphicOverlay
import dk.skancode.skanmate.ui.component.barcode.BarcodeData
import dk.skancode.skanmate.ui.component.barcode.BarcodeFormat
import dk.skancode.skanmate.util.rememberCameraViewSize

@Composable
fun AndroidScannerView(
    modifier: Modifier,
    scannerController: ScannerController,
    codeTypes: List<BarcodeFormat>,
    processor: BarcodeProcessorBase<List<BarcodeData>>,
    barcodeOverlay: BaseGraphicOverlay,
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
            onPreviewLayoutChange = {},
            barcodeOverlay = barcodeOverlay,
            processor = processor,
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize(),
        propagateMinConstraints = true,
    ) {
        val cameraViewSize by rememberCameraViewSize()

        AndroidView(
            modifier = Modifier
                .size(cameraViewSize)
                .align(Alignment.Center),
            factory = { ctx ->
                previewViewFactory(ctx)
            },
        )
        GraphicOverlay(
            modifier = Modifier
                .size(cameraViewSize)
                .align(Alignment.Center),
            overlay = barcodeOverlay,
        )
//        AndroidView(
//            modifier = Modifier
//                .size(cameraViewSize)
//                .align(Alignment.Center),
//            factory = { barcodeOverlay },
//        )
    }
}

fun barcodePreviewFactory(
    lifecycleOwner: LifecycleOwner,
    backgroundColor: Int,
    cameraController: LifecycleCameraController,
    codeTypes: List<BarcodeFormat>,
    onPreviewLayoutChange: (newRect: Rect) -> Unit,
    barcodeOverlay: GraphicOverlay,
    processor: BarcodeProcessorBase<List<BarcodeData>>
): (Context) -> PreviewView {
    return { ctx ->
        PreviewView(ctx).apply {
            setBackgroundColor(backgroundColor)

            addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                println("AndroidScannerView::barcodePreviewFactory.addOnLayoutChangeListener() - view: ${v.id}, left: $left, top: $top, right: $right, bottom: $bottom, oldLeft: $oldLeft, oldTop: $oldTop, oldRight: $oldRight, oldBottom: $oldBottom")

                if (left == oldLeft && top == oldTop && right == oldRight && bottom == oldBottom) return@addOnLayoutChangeListener

                onPreviewLayoutChange(
                    Rect(
                        left,
                        top,
                        right,
                        bottom,
                    )
                )
            }

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
                    graphicOverlay = barcodeOverlay,
                    processor = processor,
                )
            )

            cameraController.bindToLifecycle(lifecycleOwner)

            this.controller = cameraController
        }
    }
}