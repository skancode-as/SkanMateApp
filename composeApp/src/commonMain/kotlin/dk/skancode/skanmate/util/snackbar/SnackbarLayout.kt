package dk.skancode.skanmate.util.snackbar

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy

@Composable
fun SnackbarLayout(
    snackbar: @Composable () -> Unit,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable () -> Unit,
) {
    SubcomposeLayout { constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight

        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        val snackbarPlaceables =
            subcompose(SnackbarLayoutContent.Snackbar, snackbar).fastMap {
                // respect only bottom and horizontal for snackbar and fab
                val leftInset = contentWindowInsets.getLeft(this@SubcomposeLayout, layoutDirection)
                val rightInset =
                    contentWindowInsets.getRight(this@SubcomposeLayout, layoutDirection)
                val bottomInset = contentWindowInsets.getBottom(this@SubcomposeLayout)
                // offset the snackbar constraints by the insets values
                it.measure(looseConstraints.offset(-leftInset - rightInset, -bottomInset))
            }
        val snackbarHeight = snackbarPlaceables.fastMaxBy { it.height }?.height ?: 0
        val snackbarWidth = snackbarPlaceables.fastMaxBy { it.width }?.width ?: 0

        val snackbarOffsetFromBottom =
            if (snackbarHeight != 0) {
                snackbarHeight + contentWindowInsets.getBottom(this@SubcomposeLayout)
            } else {
                0
            }

        val bodyContentPlaceables =
            subcompose(SnackbarLayoutContent.MainContent) {
                content()
            }
                .fastMap { it.measure(looseConstraints) }

        layout(layoutWidth, layoutHeight) {
            bodyContentPlaceables.fastForEach { it.place(0, 0) }
            snackbarPlaceables.fastForEach {
                it.place(
                    (layoutWidth - snackbarWidth) / 2 +
                            contentWindowInsets.getLeft(this@SubcomposeLayout, layoutDirection),
                    layoutHeight - snackbarOffsetFromBottom
                )
            }
        }
    }
}

enum class SnackbarLayoutContent {
    Snackbar,
    MainContent,
}