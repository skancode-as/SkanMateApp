package dk.skancode.skanmate.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.window.DialogProperties
import dk.skancode.skanmate.ImageResource
import dk.skancode.skanmate.ImageResourceState
import dk.skancode.skanmate.data.model.ColumnType
import dk.skancode.skanmate.data.model.ColumnValue
import dk.skancode.skanmate.data.model.LocalTableData
import dk.skancode.skanmate.loadImage
import dk.skancode.skanmate.ui.component.Badge
import dk.skancode.skanmate.ui.component.ContentDialog
import dk.skancode.skanmate.ui.component.CustomButtonElevation
import dk.skancode.skanmate.ui.component.IconButton
import dk.skancode.skanmate.ui.component.KeyboardAwareScaffold
import dk.skancode.skanmate.ui.component.LocalLabelTextStyle
import dk.skancode.skanmate.ui.component.SizeValues
import dk.skancode.skanmate.ui.component.SkanMateDropdown
import dk.skancode.skanmate.ui.component.SkanMateTopAppBar
import dk.skancode.skanmate.ui.component.SwitchDefaults
import dk.skancode.skanmate.ui.component.Table
import dk.skancode.skanmate.ui.component.TableColors
import dk.skancode.skanmate.ui.component.TableDefaults
import dk.skancode.skanmate.ui.component.TextButton
import dk.skancode.skanmate.ui.component.fab.FloatingActionButton
import dk.skancode.skanmate.ui.viewmodel.GENERAL_ROW_ERROR_NAME
import dk.skancode.skanmate.ui.viewmodel.LocalConnectionState
import dk.skancode.skanmate.ui.viewmodel.SyncViewModel
import dk.skancode.skanmate.util.BorderSide
import dk.skancode.skanmate.util.HapticKind
import dk.skancode.skanmate.util.InternalStringResource
import dk.skancode.skanmate.util.LocalAudioPlayer
import dk.skancode.skanmate.util.animator
import dk.skancode.skanmate.util.composeString
import dk.skancode.skanmate.util.darken
import dk.skancode.skanmate.util.measureText
import dk.skancode.skanmate.util.rememberHaptic
import dk.skancode.skanmate.util.rememberMutableStateOf
import dk.skancode.skanmate.util.singleSideBorder
import dk.skancode.skanmate.util.snackbar.UserMessageServiceImpl
import dk.skancode.skanmate.util.titleTextStyle
import dk.skancode.skanmate.util.toLocalTimeString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import skanmate.composeapp.generated.resources.Res
import skanmate.composeapp.generated.resources.cannot_display_image
import skanmate.composeapp.generated.resources.cloud_upload
import skanmate.composeapp.generated.resources.sync_screen_could_not_delete_local_row
import skanmate.composeapp.generated.resources.sync_screen_could_not_delete_local_table_rows
import skanmate.composeapp.generated.resources.sync_screen_could_not_sync_data
import skanmate.composeapp.generated.resources.sync_screen_data_synchronised
import skanmate.composeapp.generated.resources.sync_screen_delete_row_alert_accept_btn
import skanmate.composeapp.generated.resources.sync_screen_delete_row_alert_desc
import skanmate.composeapp.generated.resources.sync_screen_delete_row_alert_content_text
import skanmate.composeapp.generated.resources.sync_screen_delete_row_alert_dismiss_btn
import skanmate.composeapp.generated.resources.sync_screen_delete_row_alert_title
import skanmate.composeapp.generated.resources.sync_screen_delete_table_alert_content_text
import skanmate.composeapp.generated.resources.sync_screen_delete_table_alert_desc
import skanmate.composeapp.generated.resources.sync_screen_delete_table_alert_title
import skanmate.composeapp.generated.resources.sync_screen_fab_text
import skanmate.composeapp.generated.resources.sync_screen_no_local_data_content
import skanmate.composeapp.generated.resources.sync_screen_no_local_data_title
import skanmate.composeapp.generated.resources.sync_screen_table_description
import skanmate.composeapp.generated.resources.sync_screen_title
import skanmate.composeapp.generated.resources.table_screen_switch_off
import skanmate.composeapp.generated.resources.table_screen_switch_on

private val LocalImageResource: ProvidableCompositionLocal<ImageResource<Painter>?> =
    compositionLocalOf { null }
@Composable
fun SyncScreen(
    viewModel: SyncViewModel,
    navigateBack: () -> Unit,
) {
    val hasConnection by LocalConnectionState.current
    val data by viewModel.localDataFlow.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val audioPlayer = LocalAudioPlayer.current
    val successHaptic = rememberHaptic(HapticKind.Success)
    val errorHaptic = rememberHaptic(HapticKind.Error)

    KeyboardAwareScaffold(
        topBar = {
            SkanMateTopAppBar(
                title = { Text(stringResource(Res.string.sync_screen_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = navigateBack,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LocalContentColor.current),
                        elevation = CustomButtonElevation.None,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            SyncFloatingActionButton(
                onClick = {
                    viewModel.synchroniseLocalData(data) { ok ->
                        if (ok) {
                            UserMessageServiceImpl.displayMessage(
                                message = InternalStringResource(
                                    Res.string.sync_screen_data_synchronised,
                                ),
                            )
                            audioPlayer.playSuccess()
                            successHaptic.start()
                        } else {
                            UserMessageServiceImpl.displayError(
                                message = InternalStringResource(
                                    resource = Res.string.sync_screen_could_not_sync_data
                                )
                            )
                            audioPlayer.playError()
                            errorHaptic.start()
                        }
                    }
                },
                isVisible = hasConnection && data.isNotEmpty(),
                isLoading = uiState.isLoading
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(paddingValues = padding)
                .padding(all = 16.dp),
        ) {
            when {
                data.isEmpty() -> {
                    NoLocalDataFound()
                }
                data.isNotEmpty() -> {
                    LocalDataTables(
                        data = data,
                        onDeleteRow = { localRowId, cb ->
                            viewModel.deleteLocalRow(localRowId, cb)
                        },
                        onDeleteTableData = { tableId, cb ->
                            viewModel.deleteLocalTableRows(tableId, cb)
                        },
                        errorMap = uiState.synchronisationErrors,
                        isLoading = uiState.isLoading,
                    )
                }
            }
        }
    }
}

@Composable
fun NoLocalDataFound(
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.sync_screen_no_local_data_title),
                style = titleTextStyle(),
            )

            Text(
                text = stringResource(Res.string.sync_screen_no_local_data_content),
                style = LocalLabelTextStyle.current,
            )
        }
    }
}

@Composable
fun SyncFloatingActionButton(
    onClick: () -> Unit,
    isVisible: Boolean,
    isLoading: Boolean,
) {
    AnimatedVisibility(isVisible) {
        FloatingActionButton(
            modifier = Modifier.padding(16.dp),
            onClick = onClick,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.darken(.15f),
                disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            enabled = !isLoading,
            icon = {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = LocalContentColor.current,
                        trackColor = MaterialTheme.colorScheme.primaryContainer.darken(0.15f),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = vectorResource(Res.drawable.cloud_upload),
                        contentDescription = null,
                    )
                }
            },
        ) {
            Text(stringResource(Res.string.sync_screen_fab_text))
        }
    }
}

@Composable
fun LocalDataTables(
    modifier: Modifier = Modifier,
    data: List<LocalTableData>,
    onDeleteRow: (localRowId: Long, cb: (Boolean) -> Unit) -> Unit,
    onDeleteTableData: (tableId: String, cb: (Boolean) -> Unit) -> Unit,
    errorMap: Map<Long, Map<String, List<InternalStringResource>>>,
    isLoading: Boolean,
) {
    val expandedStates = remember { mutableStateListOf(*data.map { false }.toTypedArray()) }
    LaunchedEffect(errorMap) {
        if (errorMap.isNotEmpty()) {
            for (i in 0..<expandedStates.size) {
                expandedStates[i] = true
            }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        data.forEachIndexed { i, it ->
            LocalTableDataItem(
                item = it,
                onDeleteRow = onDeleteRow,
                onDeleteTableData = onDeleteTableData,
                expanded = expandedStates[i],
                setExpanded = {
                    expandedStates[i] = it
                },
                errorMap = errorMap,
                isLoading = isLoading,
            )
        }
    }
}

@Composable
fun LocalTableDataItem(
    modifier: Modifier = Modifier,
    item: LocalTableData,
    onDeleteRow: (localRowId: Long, cb: (Boolean) -> Unit) -> Unit,
    onDeleteTableData: (tableId: String, cb: (Boolean) -> Unit) -> Unit,
    expanded: Boolean,
    setExpanded: (Boolean) -> Unit,
    errorMap: Map<Long, Map<String, List<InternalStringResource>>>,
    isLoading: Boolean,
) {
    val imageResourceMap: Map<String, ImageResource<Painter>> = mapOf(
        *item.rows.flatMap { row ->
            if (row.values.none { cell -> cell.value is ColumnValue.File })
                return@flatMap emptyList()

            val pairs = row.cols.entries.mapNotNull { (colName, value) ->
                if (value.value !is ColumnValue.File || value.value.localUrl == null) null
                else "${row.localRowId}_${colName}" to loadImage(value.value.localUrl)
            }

            pairs
        }.toTypedArray()
    )


    var rowIdToDelete: Long? by rememberMutableStateOf(null)
    var tableIdToDelete: String? by rememberMutableStateOf(null)
    val expandedIconAnimator = animator(0f, -90f)

    LaunchedEffect(expanded) {
        expandedIconAnimator.animateTo(if (expanded) -90f else 0f)
    }

    if (rowIdToDelete != null) {
        var isDeleting by rememberMutableStateOf(false)

        DeleteAlert(
            titleText = stringResource(Res.string.sync_screen_delete_row_alert_title),
            descriptionText = stringResource(Res.string.sync_screen_delete_row_alert_desc),
            contentText = stringResource(Res.string.sync_screen_delete_row_alert_content_text),
            onAccept = {
                isDeleting = true
                onDeleteRow(rowIdToDelete!!) { deleted ->
                    if (deleted) {
                        rowIdToDelete = null
                    } else {
                        UserMessageServiceImpl.displayError(
                            message = InternalStringResource(Res.string.sync_screen_could_not_delete_local_row)
                        )
                    }
                    isDeleting = false
                }
            },
            onDismiss = {
                rowIdToDelete = null
            },
            isDeleting = isDeleting,
        )
    }
    if (tableIdToDelete != null) {
        var isDeleting by rememberMutableStateOf(false)

        DeleteAlert(
            titleText = stringResource(Res.string.sync_screen_delete_table_alert_title),
            descriptionText = stringResource(Res.string.sync_screen_delete_table_alert_desc),
            contentText = stringResource(Res.string.sync_screen_delete_table_alert_content_text),
            onAccept = {
                isDeleting = true
                onDeleteTableData(tableIdToDelete!!) { deleted ->
                    if (deleted) {
                        tableIdToDelete = null
                    } else {
                        UserMessageServiceImpl.displayError(
                            message = InternalStringResource(Res.string.sync_screen_could_not_delete_local_table_rows)
                        )
                    }
                    isDeleting = false
                }
            },
            onDismiss = {
                tableIdToDelete = null
            },
            isDeleting = isDeleting,
        )
    }

    ElevatedCard(
        modifier = modifier.animateContentSize(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = item.model.name,
                        style = titleTextStyle(),
                    )
                    Text(
                        text = pluralStringResource(
                            resource = Res.plurals.sync_screen_table_description,
                            quantity = item.rows.size,
                            item.rows.size,
                        ),
                        style = LocalLabelTextStyle.current,
                    )
                }
                IconButton(
                    onClick = {
                        setExpanded(!expanded)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LocalContentColor.current),
                    elevation = CustomButtonElevation.None,
                ) {
                    val rotation by expandedIconAnimator.value
                    Icon(
                        modifier = Modifier.rotate(degrees = rotation),
                        imageVector = Icons.AutoMirrored.Default.KeyboardArrowLeft,
                        contentDescription = null,
                    )
                }
            }

            if (expanded) {
                val headerColors = TableDefaults.headerColors
                val rowColors = TableColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
                val actionPadding = PaddingValues(12.dp)
                val cellPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                val columns = remember(item.model.id) { item.model.columns.filter { col -> col.type != ColumnType.Id } }

                val hasGeneralError =
                    errorMap.isNotEmpty() && errorMap.values.none { rowErrorMap ->
                        rowErrorMap[GENERAL_ROW_ERROR_NAME] == null
                    }

                Table(
                    columnCount = columns.size + 1,
                    rowCount = item.rows.size,
                    headerColors = headerColors,
                    headerCell = { colIdx ->
                        when (colIdx) {
                            0 -> {
                                BaseCell(
                                    containerColor = headerColors.containerColor(),
                                    arrangement = Arrangement.Center,
                                    contentPadding = PaddingValues(0.dp),
                                ) {
                                    if (hasGeneralError) {
                                        ErrorIconButton(
                                            onClick = {},
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = Color.Transparent,
                                                contentColor = Color.Transparent,
                                                disabledContainerColor = Color.Transparent,
                                                disabledContentColor = Color.Transparent,
                                            ),
                                            enabled = false,
                                        )
                                    }

                                    IconButton(
                                        onClick = { tableIdToDelete = item.model.id },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        elevation = CustomButtonElevation.None,
                                        contentPadding = actionPadding,
                                        enabled = !isLoading,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                        )
                                    }
                                }
                            }

                            else -> {
                                val column = columns[colIdx - 1]
                                val containerColor = headerColors.containerColor()

                                HeaderCell(
                                    modifier = Modifier
                                        .singleSideBorder(
                                            width = Dp.Hairline,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = .8f)
                                                .compositeOver(containerColor),
                                            side = BorderSide.Left,
                                        ),
                                    name = column.name,
                                    containerColor = containerColor,
                                    padding = cellPadding,
                                )
                            }
                        }
                    },
                    rowColors = rowColors,
                ) { colIdx, rowIndex ->
                    val row = item.rows[rowIndex]
                    val rowErrors = errorMap[row.localRowId] ?: emptyMap()

                    when (colIdx) {
                        0 -> {
                            val errors = rowErrors[GENERAL_ROW_ERROR_NAME] ?: emptyList()

                            BaseCell(
                                containerColor = rowColors.containerColor(),
                                arrangement = Arrangement.Center,
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                if (errors.isNotEmpty()) {
                                    CellErrorIcon(errors = errors)
                                } else if (hasGeneralError) {
                                    ErrorIconButton(
                                        onClick = {},
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color.Transparent,
                                            contentColor = Color.Transparent,
                                            disabledContainerColor = Color.Transparent,
                                            disabledContentColor = Color.Transparent,
                                        ),
                                        enabled = false,
                                    )
                                }

                                TableActions(
                                    onEdit = {},
                                    onDelete = {
                                        rowIdToDelete = item.rows[rowIndex].localRowId
                                    },
                                    padding = actionPadding,
                                    enabled = !isLoading,
                                )
                            }
                        }

                        else -> {
                            val columnDef = columns[colIdx - 1]
                            val cell = row[columnDef.dbName]
                                ?: error("Could not find element at col: $colIdx, row: $rowIndex")
                            val containerColor = rowColors.containerColor()
                            val errors = rowErrors[columnDef.dbName] ?: emptyList()

                            CompositionLocalProvider(LocalImageResource provides imageResourceMap["${row.localRowId}_${columnDef.dbName}"]) {
                                BaseCell(
                                    modifier = Modifier
                                        .singleSideBorder(
                                            width = Dp.Hairline,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = .8f)
                                                .compositeOver(containerColor),
                                            side = BorderSide.Left,
                                        ),
                                    containerColor = containerColor,
                                    contentPadding = cellPadding,
                                ) {
                                    DataRowContent(
                                        type = cell.type,
                                        value = cell.value,
                                        errors = errors,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteAlert(
    titleText: String,
    descriptionText: String,
    contentText: String,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    isDeleting: Boolean,
    padding: PaddingValues = PaddingValues(16.dp),
) {
    val enabled = !isDeleting

    val description = @Composable {
        Text(
            text = descriptionText,
            style = MaterialTheme.typography.bodyLarge,
        )
    }

    val content: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier.padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = contentText,
                style = MaterialTheme.typography.bodyLarge,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(
                    enabled = enabled,
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                ) {
                    Text(
                        text = stringResource(Res.string.sync_screen_delete_row_alert_dismiss_btn),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                TextButton(
                    enabled = enabled,
                    onClick = onAccept,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    )
                ) {
                    Text(
                        text = stringResource(Res.string.sync_screen_delete_row_alert_accept_btn),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }

    ContentDialog(
        closable = enabled,
        properties = DialogProperties(
            dismissOnBackPress = enabled,
            dismissOnClickOutside = enabled
        ),
        onDismissRequest = onDismiss,
        contentPadding = padding,
        title = {
            Text(
                text = titleText,
                style = titleTextStyle(),
            )
        },
        description = description,
        content = content,
    )
}

@Composable
fun TableActions(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    padding: PaddingValues,
    enabled: Boolean = true,
) {
    SkanMateDropdown(
        buttonSizeValues = SizeValues(
            minHeight = DefaultBaseCellHeight,
            maxHeight = DefaultBaseCellHeight
        ),
        enabled = enabled,
        contentPadding = padding,
        aspectRatio = 1f,
    ) { collapse ->
        DropdownMenuItem(
            text = {
                Text(
                    text = "Edit",
                )
            },
            onClick = {
                onEdit()
                collapse()
            }
        )
        DropdownMenuItem(
            text = {
                Text(
                    text = "Delete",
                    color = MaterialTheme.colorScheme.error,
                )
            },
            onClick = {
                onDelete()
                collapse()
            }
        )
    }
}

@Composable
fun DataRowContent(
    type: ColumnType,
    value: ColumnValue,
    errors: List<InternalStringResource>,
) {
    val imageResource = LocalImageResource.current
    when (value) {
        is ColumnValue.File if imageResource != null -> {
            FileDataRowContent(imageResource)
        }
        is ColumnValue.Boolean -> {
            BooleanDataRowContent(value)
        }
        else -> {
            val text = when (value) {
                is ColumnValue.Text -> when(type) {
                    is ColumnType.Timestamp -> value.text.toLocalTimeString()
                    else -> value.text
                }
                is ColumnValue.Numeric -> value.num?.toString() ?: ""
                is ColumnValue.OptionList -> value.selected ?: ""

                is ColumnValue.Boolean -> ""
                is ColumnValue.File -> ""
                ColumnValue.Null -> ""
            }

            Text(
                text = text,
                style = LocalLabelTextStyle.current,
            )
        }
    }

    if (errors.isNotEmpty()) {
        Spacer(modifier = Modifier.width(8.dp))
        CellErrorIcon(errors = errors)
    }
}

@Composable
fun BooleanDataRowContent(
    value: ColumnValue.Boolean,
) {
    val switchColors = SwitchDefaults.colors()
    val badgeTextStyle = MaterialTheme.typography.labelLarge
    val text = when(value.checked) {
        true -> stringResource(Res.string.table_screen_switch_on)
        false -> stringResource(Res.string.table_screen_switch_off)
    }

    Badge(
        color = switchColors.containerColor(value.checked, enabled = true),
        contentStyle = badgeTextStyle,
        contentPadding = PaddingValues(4.dp, 3.dp)
    ) {
        Text(
            modifier = Modifier,
            text = text,
        )
    }
}

@Composable
fun FileDataRowContent(
    imageResource: ImageResource<Painter>
) {
    val image by imageResource.state

    Box(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
            ),
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true,
    ) {
        AnimatedContent(image) { targetImage ->
            when (targetImage) {
                is ImageResourceState.Image<*> -> Image(
                    painter = (targetImage as ImageResourceState.Image<*>).data,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                )

                is ImageResourceState.Error -> Text(stringResource(Res.string.cannot_display_image))

                else -> CircularProgressIndicator(
                    modifier = Modifier.padding(all = 4.dp).fillMaxSize(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CellErrorIcon(
    errors: List<InternalStringResource>,
) {
    val tooltipState = rememberTooltipState(
        isPersistent = true,
    )
    val scope = CoroutineScope(Dispatchers.IO)

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Above
        ),
        tooltip = {
            PlainTooltip(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                shadowElevation = 4.dp,
            ) {
                errors.forEach { res ->
                    Text(
                        text = res.composeString()
                    )
                }
            }
        },
        state = tooltipState,
    ) {
        ErrorIconButton(
            onClick = {
                scope.launch { tooltipState.show() }
            },
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        )
    }
}

@Composable
fun ErrorIconButton(
    onClick: () -> Unit,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(
        contentColor = MaterialTheme.colorScheme.error,
    ),
    enabled: Boolean = true,
) {

    IconButton(
        onClick = onClick,
        colors = colors,
        elevation = CustomButtonElevation.None,
        enabled = enabled,
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "CellErrorIcon"
        )
    }
}

@Composable
fun HeaderCell(
    modifier: Modifier = Modifier,
    name: String,
    containerColor: Color,
    padding: PaddingValues = PaddingValues(8.dp),
) {
    BaseCell(
        modifier = modifier,
        containerColor = containerColor,
        contentPadding = padding,
    ) {
        Text(
            text = name,
            style = LocalLabelTextStyle.current,
        )
    }
}

@Composable
fun BaseCell(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = LocalContentColor.current,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    height: Dp = DefaultBaseCellHeight,
    arrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .background(color = containerColor, shape = shape)
            .padding(paddingValues = contentPadding)
            .requiredHeight(height = height),
        horizontalArrangement = arrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

private val DefaultBaseCellHeight = 48.dp
