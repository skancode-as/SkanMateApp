package dk.skancode.skanmate

import dk.skancode.barcodescannermodule.Enabler
import dk.skancode.barcodescannermodule.IScannerModule
import dk.skancode.barcodescannermodule.event.TypedEventHandler
import dk.skancode.skanmate.util.CameraScanListener
import dk.skancode.skanmate.util.CameraScanManager

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
                    handler.handleEvents(listOf(scanEvent))
                }
            }

            listeners[handler] = typedEventHandler
            scanModule.registerTypedEventHandler(typedEventHandler)
        } else {
            val cameraScanListener = CameraScanListener { events ->
                handler.handleEvents(
                    events.map { (barcode, format) ->
                        ScanEvent.Barcode(
                            barcode = barcode,
                            barcodeType = format,
                            ok = true,
                        )
                    }
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