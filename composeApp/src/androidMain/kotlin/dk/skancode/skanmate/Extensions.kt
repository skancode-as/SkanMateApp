package dk.skancode.skanmate

import dk.skancode.barcodescannermodule.event.TypedEvent

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

