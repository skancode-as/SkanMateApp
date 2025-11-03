package dk.skancode.skanmate.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import dk.skancode.skanmate.util.InternalStringResource
import dk.skancode.skanmate.util.composeString


@Composable
fun ColumnWithErrorLayout(
    modifier: Modifier = Modifier,
    errors: List<InternalStringResource>,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        measurePolicy = ColumnWithErrorMeasurePolicy(),
        content = {
            Box(
                modifier = Modifier
                    .layoutId(ColumnWithErrorMeasurePolicy.CONTENT_ID)
                    .wrapContentSize()
            ) {
                content()
            }

            if (errors.isNotEmpty()) {
                val errorMessages = errors.map { it.composeString() }
                Box(
                    modifier = Modifier
                        .layoutId(ColumnWithErrorMeasurePolicy.ERROR_ID)
                        .verticalScroll(rememberScrollState())
                        .requiredHeightIn(min = 24.dp, max = 48.dp)
                        .wrapContentHeight()
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        text = errorMessages
                            .joinToString("\n"),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    )
}

private class ColumnWithErrorMeasurePolicy() : MeasurePolicy {
    companion object {
        const val CONTENT_ID = "content"
        const val ERROR_ID = "error"
    }

    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        val contentMeasurable = measurables.find { it.layoutId == CONTENT_ID }
            ?: throw IllegalStateException("No content registered")
        val contentHeight = contentMeasurable.minIntrinsicHeight(constraints.maxWidth)
        val contentPlaceable =
            contentMeasurable.measure(constraints.copy(minHeight = contentHeight, maxHeight = contentHeight))

        val errorMeasurable = measurables.find { it.layoutId == ERROR_ID }
        val errorHeight = errorMeasurable?.minIntrinsicHeight(constraints.maxWidth) ?: 0
        val errorPlaceable =
            errorMeasurable?.measure(
                constraints = constraints.copy(
                    minHeight = 0,
                    maxHeight = errorHeight,
                )
            )

        return layout(width = constraints.maxWidth, height = contentHeight + errorHeight) {
            contentPlaceable.placeRelative(0, 0)

            errorPlaceable?.placeRelative(0, contentPlaceable.height)
        }
    }
}
