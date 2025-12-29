package dk.skancode.skanmate.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.layout

@Composable
fun Table(
    modifier: Modifier = Modifier,
    rowModifier: Modifier = Modifier,
    verticalLazyListState: LazyListState = rememberLazyListState(),
    horizontalScrollState: ScrollState = rememberScrollState(),
    columnCount: Int,
    rowCount: Int,
    headerColors: TableColors = TableDefaults.headerColors,
    headerShape: Shape = RectangleShape,
    headerCell: (@Composable (columnIndex: Int) -> Unit)? = null,
    rowColors: TableColors = TableDefaults.rowColors,
    rowShape: Shape = RectangleShape,
    cellContent: @Composable (columnIndex: Int, rowIndex: Int) -> Unit
) {
    val columnWidths = remember { mutableStateMapOf<Int, Int>() }

    Box(modifier = modifier.then(Modifier.horizontalScroll(horizontalScrollState))) {
        LazyColumn(state = verticalLazyListState) {
            stickyHeader {
                if (headerCell != null) {
                    TableRow(
                        modifier = rowModifier,
                        columnCount = columnCount,
                        columnWidths = columnWidths,
                        colors = headerColors,
                        shape = headerShape,
                    ) { columnIndex ->
                        headerCell(columnIndex)
                    }
                }
            }
            items(rowCount) { rowIndex ->
                Column {
                    TableRow(
                        modifier = rowModifier,
                        columnCount = columnCount,
                        columnWidths = columnWidths,
                        colors = rowColors,
                        shape = rowShape,
                    ) { columnIndex ->
                        cellContent(columnIndex, rowIndex)
                    }
                }
            }
        }
    }
}

@Composable
private fun TableRow(
    modifier: Modifier,
    columnCount: Int,
    columnWidths: SnapshotStateMap<Int, Int>,
    colors: TableColors,
    shape: Shape = RectangleShape,
    cellContent: @Composable (columnIndex: Int) -> Unit,
) {
    val containerColor = colors.containerColor()
    val contentColor = colors.contentColor()

    val modifier = Modifier
        .background(color = containerColor, shape = shape)
        .then(modifier)

    Row(modifier = modifier) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            (0 until columnCount).forEach { columnIndex ->
                Box(
                    modifier = Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)

                        val existingWidth = columnWidths[columnIndex] ?: 0
                        val maxWidth = maxOf(existingWidth, placeable.width)

                        if (maxWidth > existingWidth) {
                            columnWidths[columnIndex] = maxWidth
                        }

                        layout(width = maxWidth, height = placeable.height) {
                            placeable.placeRelative(0, 0)
                        }
                    },
                ) {
                    cellContent(columnIndex)
                }
            }
        }
    }
}

data class TableColors(
    private val containerColor: Color = Color.Unspecified,
    private val contentColor: Color = Color.Unspecified,
) {
    @Composable
    fun containerColor(): Color = containerColor.takeOrElse { TableDefaults.defaultTableRowContainerColor }
    @Composable
    fun contentColor(): Color = contentColor.takeOrElse { TableDefaults.defaultTableRowContentColor }
}

object TableDefaults {
    @get:Composable
    val defaultTableRowContainerColor: Color
        get() = MaterialTheme.colorScheme.surface

    @get:Composable
    val defaultTableRowContentColor: Color
        get() = MaterialTheme.colorScheme.onSurface
    @get:Composable
    val defaultTableHeaderContainerColor: Color
        get() = MaterialTheme.colorScheme.primary

    @get:Composable
    val defaultTableHeaderContentColor: Color
        get() = MaterialTheme.colorScheme.onPrimary

    @get:Composable
    val rowColors: TableColors
        get() = TableColors(
            containerColor = defaultTableRowContainerColor,
            contentColor = defaultTableRowContentColor
        )

    @get:Composable
    val headerColors: TableColors
        get() = TableColors(
            containerColor = defaultTableHeaderContainerColor,
            contentColor = defaultTableHeaderContentColor,
        )
}

