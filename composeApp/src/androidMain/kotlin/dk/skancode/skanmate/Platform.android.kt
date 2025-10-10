package dk.skancode.skanmate

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import dk.skancode.skanmate.util.CameraScanListener
import dk.skancode.skanmate.util.CameraScanManager
import dk.skancode.skanmate.util.LocalCameraScanManager
import dk.skancode.barcodescannermodule.Enabler
import dk.skancode.barcodescannermodule.IScannerModule
import dk.skancode.barcodescannermodule.compose.LocalScannerModule
import dk.skancode.barcodescannermodule.event.TypedEvent
import dk.skancode.barcodescannermodule.event.TypedEventHandler
import dk.skancode.barcodescannermodule.gs1.Gs1Config

fun TypedEvent.toScanEvent(): ScanEvent? {
    return when (this) {
        is TypedEvent.Gs1Event -> ScanEvent.Barcode(
            ok = ok,
            barcode = barcode,
            barcodeType = barcodeType.name,
        )
        is TypedEvent.BarcodeEvent -> ScanEvent.Barcode(
            barcode = barcode1,
            barcodeType = barcodeType.name,
            ok = ok
        )
        is TypedEvent.NfcEvent -> null
    }
}

class AndroidScanModuleImpl(val scanModule: IScannerModule, val cameraManager: CameraScanManager): ScanModule {
    private val listeners: MutableMap<ScanEventHandler, TypedEventHandler> = HashMap()
    private val cameraListeners: MutableMap<ScanEventHandler, CameraScanListener> = HashMap()

    override fun isHardwareScanner(): Boolean {
        return scanModule.scannerAvailable()
    }

    override fun registerListener(handler: ScanEventHandler) {
        if (scanModule.scannerAvailable()) {
            val typedEventHandler = TypedEventHandler { e ->
                val scanEvent = e.toScanEvent()

                if (scanEvent != null) {
                    handler.handle(scanEvent)
                }
            }

            listeners[handler] = typedEventHandler
            scanModule.registerTypedEventHandler(typedEventHandler)
        } else {
            val cameraScanListener = CameraScanListener { barcode, format ->
                handler.handle(
                    ScanEvent.Barcode(
                        barcode = barcode,
                        barcodeType = format,
                        ok = true,
                    )
                )
            }

            cameraListeners[handler] = cameraScanListener
            cameraManager.registerListener(cameraScanListener)
        }
    }

    override fun unregisterListener(handler: ScanEventHandler) {
        if (scanModule.scannerAvailable()) {
            val typedEventHandler = listeners.remove(handler) ?: return

            scanModule.unregisterTypedEventHandler(typedEventHandler)
        } else {
            val cameraScanListener = cameraListeners.remove(handler) ?: return

            cameraManager.unregisterListener(cameraScanListener)
        }
    }

    override fun enableScan() {
        if (scanModule.scannerAvailable()) {
            scanModule.setScannerState(Enabler.ON)
        } else {
            cameraManager.startScanning()
        }
    }

    override fun disableScan() {
        if (scanModule.scannerAvailable()) {
            scanModule.setScannerState(Enabler.OFF)
        } else {
            cameraManager.stopScanning()
        }
    }

//    override fun enableGs1() {
//        if (scanModule.scannerAvailable()) {
//            scanModule.setGs1Config(Gs1Config(enabled = Enabler.ON))
//        }
//    }
//
//    override fun disableGs1() {
//        if (scanModule.scannerAvailable()) {
//            scanModule.setGs1Config(Gs1Config(enabled = Enabler.OFF))
//        }
//    }
}

@Composable
actual fun rememberScanModule(): ScanModule {
    val localModule = LocalScannerModule.current
    val localCameraScanManager = LocalCameraScanManager.current

    return remember(localModule) {
        AndroidScanModuleImpl(localModule, localCameraScanManager)
    }
}

actual val platformSettingsFactory: Settings.Factory = SkanMateApplication.settingsFactory