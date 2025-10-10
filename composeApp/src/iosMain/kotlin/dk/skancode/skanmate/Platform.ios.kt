package dk.skancode.skanmate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import dk.skancode.skanmate.util.CameraScanListener
import dk.skancode.skanmate.util.CameraScanManager
import dk.skancode.skanmate.util.LocalCameraScanManager

class IosScanModule(val cameraScanManager: CameraScanManager): ScanModule {
    private val cameraListeners: MutableMap<ScanEventHandler, CameraScanListener> = HashMap()

    override fun isHardwareScanner(): Boolean {
        return false
    }

    override fun registerListener(handler: ScanEventHandler) {
        val cameraScanListener = CameraScanListener { barcode, format ->
            handler.handle(ScanEvent.Barcode(
                barcode = barcode,
                barcodeType = format,
                ok = true,
            ))
        }

        cameraListeners[handler] = cameraScanListener
        cameraScanManager.registerListener(cameraScanListener)
    }

    override fun unregisterListener(handler: ScanEventHandler) {
        val cameraScanListener = cameraListeners.remove(handler) ?: return

        cameraScanManager.unregisterListener(cameraScanListener)
    }

    override fun enableScan() {
        cameraScanManager.startScanning()
    }

    override fun disableScan() {
        cameraScanManager.stopScanning()
    }

//    override fun enableGs1() { }
//
//    override fun disableGs1() { }

}

@Composable
actual fun rememberScanModule(): ScanModule {
    val cameraScanManager = LocalCameraScanManager.current

    return remember { IosScanModule(cameraScanManager) }
}

actual val platformSettingsFactory: Settings.Factory = NSUserDefaultsSettings.Factory()