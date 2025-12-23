package dk.skancode.skanmate.barcode

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@Composable
fun GraphicOverlay(
    modifier: Modifier = Modifier,
    overlay: BaseGraphicOverlay,
) {
    Canvas(
        modifier = modifier
    ) {
        val invalidateToken by overlay.invalidateToken

        overlay.drawOverlay(scope = this, invalidateToken)
    }
}