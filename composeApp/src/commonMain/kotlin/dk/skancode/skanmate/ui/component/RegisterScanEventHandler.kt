package dk.skancode.skanmate.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import dk.skancode.skanmate.ScanEventHandler
import dk.skancode.skanmate.ScanModule

@Composable
fun RegisterScanEventHandler(
    scanModule: ScanModule = LocalScanModule.current,
    handler: ScanEventHandler,
) {
    DisposableEffect(handler, scanModule) {
        scanModule.registerListener(handler)

        if (scanModule.isHardwareScanner()) {
            scanModule.enableScan()
        }

        onDispose {
            if (scanModule.isHardwareScanner()) {
                scanModule.disableScan()
            }

            scanModule.unregisterListener(handler)
        }
    }
}
