package dk.skancode.skanmate.util

import dk.skancode.skanmate.ui.component.barcode.BarcodeData

fun interface CameraScanListener {
    fun handle(barcodes: List<Pair<String, String>>)
}

fun interface CameraPowerListener {
    fun handle(enable: Boolean)
}

interface CameraScanManager {
    fun registerListener(listener: CameraScanListener)
    fun unregisterListener(listener: CameraScanListener)

    fun startScanning()
    fun stopScanning()
}

class CameraScanManagerImpl: CameraScanManager {
    private val listeners: MutableSet<CameraScanListener> = HashSet()
    private val powerListeners: MutableSet<CameraPowerListener> = HashSet()

    fun send(barcodes: List<BarcodeData>) {
        val events = barcodes.map { barcode -> barcode.info.value to barcode.info.format }
        this.listeners.forEach { listener -> listener.handle(barcodes = events) }
    }

    override fun registerListener(listener: CameraScanListener) {
        listeners.add(listener)
    }

    override fun unregisterListener(listener: CameraScanListener) {
        listeners.remove(listener)
    }

    fun registerScanListener(listener: CameraPowerListener) {
        powerListeners.add(listener)
    }
    fun unregisterScanListener(listener: CameraPowerListener) {
        powerListeners.remove(listener)
    }

    override fun startScanning() {
        powerListeners.forEach { listener -> listener.handle(true) }
    }

    override fun stopScanning() {
        powerListeners.forEach { listener -> listener.handle(false) }
    }
}