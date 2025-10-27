package dk.skancode.skanmate.util

import org.ncgroup.kscan.Barcode

fun interface CameraScanListener {
    fun handle(barcode: String, barcodeFormat: String)
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

    fun send(barcode: Barcode) {
        this.listeners.forEach { listener -> listener.handle(barcode.data, barcode.format) }
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