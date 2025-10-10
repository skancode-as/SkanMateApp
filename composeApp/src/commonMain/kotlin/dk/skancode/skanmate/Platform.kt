package dk.skancode.skanmate

import androidx.compose.runtime.Composable
import com.russhwolf.settings.Settings

typealias BarcodeType = String
typealias Gs1Object = MutableMap<String, String>

sealed class ScanEvent {
    data class Barcode(val barcode: String?, val barcodeType: BarcodeType, val ok: Boolean): ScanEvent()
    //data class Gs1(val ok: Boolean, val isGs1: Boolean, val gs1: Gs1Object, val barcode: String?, val barcodeType: BarcodeType): ScanEvent()
}

fun interface ScanEventHandler {
    fun handle(event: ScanEvent): Unit
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