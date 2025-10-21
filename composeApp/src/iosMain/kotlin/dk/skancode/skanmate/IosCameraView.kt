package dk.skancode.skanmate

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import dk.skancode.skanmate.util.currentDateTimeUTC
import dk.skancode.skanmate.util.format
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDeviceInput.Companion.deviceInputWithDevice
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCapturePhoto
import platform.AVFoundation.AVCapturePhotoCaptureDelegateProtocol
import platform.AVFoundation.AVCapturePhotoOutput
import platform.AVFoundation.AVCapturePhotoSettings
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetPhoto
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVVideoCodecJPEG
import platform.AVFoundation.AVVideoCodecKey
import platform.AVFoundation.fileDataRepresentation
import platform.AVFoundation.position
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.writeToFile
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIView
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
fun IosCameraView(
    cameraUi: @Composable BoxScope.(CameraController) -> Unit,
) {
    val device: AVCaptureDevice? = AVCaptureDevice
        .devicesWithMediaType(AVMediaTypeVideo)
        .firstOrNull { device ->
            (device as AVCaptureDevice).position == AVCaptureDevicePositionBack
        } as AVCaptureDevice?
    if (device == null) {
        println("Back camera not available")
        return
    }

    val output = remember { AVCapturePhotoOutput() }

    val session = remember {
        AVCaptureSession().also { session ->
            session.sessionPreset = AVCaptureSessionPresetPhoto
            val input: AVCaptureDeviceInput = deviceInputWithDevice(device = device, error = null)!!
            session.addInput(input)
            session.addOutput(output)
        }
    }

    val controller = remember { IosCameraController(output, session) }

    val cameraPreviewLayer = remember { AVCaptureVideoPreviewLayer(session = session) }

    Box(
        modifier = Modifier.fillMaxSize(),
        propagateMinConstraints = true,
    ) {
        UIKitView(
            factory = {
                val container = object : UIView(frame = CGRectZero.readValue()) {
                    override fun layoutSubviews() {
                        CATransaction.begin()
                        CATransaction.setValue(true, kCATransactionDisableActions)
                        layer.setFrame(frame)
                        cameraPreviewLayer.setFrame(frame)
                        CATransaction.commit()
                    }
                }
                container.layer.addSublayer(cameraPreviewLayer)
                cameraPreviewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
                controller.onCreate()
                container
            },
            update = { view ->
                println("UIKitView::update($view)")
            },
            onRelease = {
                controller.onRelease()
            },
            modifier = Modifier.sizeIn(minWidth = 300.dp, minHeight = 300.dp).fillMaxSize(),
        )

        cameraUi(controller)
    }
}

class IosCameraController(
    private val output: AVCapturePhotoOutput,
    private val session: AVCaptureSession,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) : CameraController {
    fun onCreate() {
        externalScope.launch {
            session.startRunning()
        }
    }

    fun onRelease() {
        if (session.running) {
            session.stopRunning()
        }
    }

    override fun takePicture(cb: (TakePictureResponse) -> Unit) {
        val settings =
            AVCapturePhotoSettings.photoSettingsWithFormat(format = mapOf(AVVideoCodecKey to AVVideoCodecJPEG))
        output.capturePhotoWithSettings(settings, OutputCapturer(cb))
    }

    private class OutputCapturer(val cb: (TakePictureResponse) -> Unit) : NSObject(),
        AVCapturePhotoCaptureDelegateProtocol {
        override fun captureOutput(
            output: AVCapturePhotoOutput,
            didFinishProcessingPhoto: AVCapturePhoto,
            error: NSError?
        ) {
            println("IosCameraController::OutputCapturer::captureOutput()")
            if (error != null) {
                cb(
                    TakePictureResponse(
                        ok = false,
                        filePath = null,
                        filename = null,
                        fileData = null,
                        error = error.description ?: "Unknown error occurred"
                    )
                )
            }

            val photoData = didFinishProcessingPhoto.fileDataRepresentation()
            val cbRes = if (photoData != null) {
                println("Received photoData!!")
                val dirPath = NSSearchPathForDirectoriesInDomains(
                    NSDocumentDirectory,
                    NSUserDomainMask,
                    true
                )[0] as String
                val fileName = "${currentDateTimeUTC().format("yyyy-MM-dd-HH-mm-ss")}.jpg"
                val filePath = "$dirPath/$fileName"

                println("Attempting to write image data to path: $filePath")
                if (photoData.writeToFile(path = filePath, atomically = false)) {
                    println("Photo written to path $filePath")
                    TakePictureResponse(
                        ok = true,
                        filePath = fileName,
                        filename = fileName,
                        fileData = photoData.toByteArray(),
                        error = null,
                    )
                } else {
                    println("Photo was not written to disk")
                    TakePictureResponse(
                        ok = false,
                        filePath = null,
                        filename = null,
                        fileData = null,
                        error = "No photo data was received"
                    )
                }
            } else {
                TakePictureResponse(
                    ok = false,
                    filePath = null,
                    filename = null,
                    fileData = null,
                    error = "No photo data was received"
                )
            }
            cb(cbRes)
        }
    }
}