package dk.skancode.skanmate.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import dk.skancode.skanmate.ui.component.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.window.DialogProperties
import dk.skancode.skanmate.ImageData
import dk.skancode.skanmate.ImageResource
import dk.skancode.skanmate.ImageResourceState
import dk.skancode.skanmate.ScanModule
import dk.skancode.skanmate.data.model.ColumnType
import dk.skancode.skanmate.data.model.ColumnConstraint
import dk.skancode.skanmate.ui.component.InputField
import dk.skancode.skanmate.ui.component.RegisterScanEventHandler
import dk.skancode.skanmate.ui.component.ScanableInputField
import dk.skancode.skanmate.ui.state.ColumnUiState
import dk.skancode.skanmate.data.model.ColumnValue
import dk.skancode.skanmate.loadImage
import dk.skancode.skanmate.ui.component.Badge
import dk.skancode.skanmate.ui.component.ColumnWithErrorLayout
import dk.skancode.skanmate.ui.component.CustomButtonElevation
import dk.skancode.skanmate.ui.component.ImageCaptureAction
import dk.skancode.skanmate.ui.component.ImageCaptureListener
import dk.skancode.skanmate.ui.component.KeyboardAwareScaffold
import dk.skancode.skanmate.ui.component.LocalLabelTextStyle
import dk.skancode.skanmate.ui.component.LocalScanModule
import dk.skancode.skanmate.ui.component.LocalUiCameraController
import dk.skancode.skanmate.ui.component.PanelButton
import dk.skancode.skanmate.ui.component.AutoSizeText
import dk.skancode.skanmate.ui.component.ContentDialog
import dk.skancode.skanmate.ui.component.LocalAuthUser
import dk.skancode.skanmate.ui.component.SizeValues
import dk.skancode.skanmate.ui.component.SkanMateTopAppBar
import dk.skancode.skanmate.ui.component.Switch
import dk.skancode.skanmate.ui.component.SwitchDefaults
import dk.skancode.skanmate.ui.component.TextButton
import dk.skancode.skanmate.ui.component.fab.FloatingActionButton
import dk.skancode.skanmate.ui.state.FetchStatus
import dk.skancode.skanmate.ui.state.TableUiState
import dk.skancode.skanmate.ui.viewmodel.LocalConnectionState
import dk.skancode.skanmate.ui.viewmodel.TableViewModel
import dk.skancode.skanmate.util.HapticKind
import dk.skancode.skanmate.util.InternalStringResource
import dk.skancode.skanmate.util.LocalAudioPlayer
import dk.skancode.skanmate.util.Success
import dk.skancode.skanmate.util.darken
import dk.skancode.skanmate.util.find
import dk.skancode.skanmate.util.keyboardVisibleAsState
import dk.skancode.skanmate.util.measureText
import dk.skancode.skanmate.util.rememberHaptic
import dk.skancode.skanmate.util.snackbar.UserMessageServiceImpl
import dk.skancode.skanmate.util.titleTextStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import skanmate.composeapp.generated.resources.Res
import skanmate.composeapp.generated.resources.camera
import skanmate.composeapp.generated.resources.cannot_display_image
import skanmate.composeapp.generated.resources.input_placeholder
import skanmate.composeapp.generated.resources.save
import skanmate.composeapp.generated.resources.select_placeholder
import skanmate.composeapp.generated.resources.submit
import skanmate.composeapp.generated.resources.table_not_found
import skanmate.composeapp.generated.resources.table_screen_data_remembered_tooltip
import skanmate.composeapp.generated.resources.table_screen_data_submitted
import skanmate.composeapp.generated.resources.table_screen_multiple_barcodes
import skanmate.composeapp.generated.resources.table_screen_multiple_barcodes_desc
import skanmate.composeapp.generated.resources.table_screen_retake_picture
import skanmate.composeapp.generated.resources.table_screen_switch_off
import skanmate.composeapp.generated.resources.table_screen_switch_on
import skanmate.composeapp.generated.resources.table_screen_take_picture
import skanmate.composeapp.generated.resources.table_screen_connection_lost_title
import skanmate.composeapp.generated.resources.table_screen_connection_lost_content_text_1
import skanmate.composeapp.generated.resources.table_screen_connection_lost_content_text_2
import skanmate.composeapp.generated.resources.table_screen_connection_lost_content_text_3
import skanmate.composeapp.generated.resources.table_screen_connection_lost_exit_btn
import skanmate.composeapp.generated.resources.triangle_alert
import kotlin.math.roundToInt

private val LocalImageResourceMap: ProvidableCompositionLocal<Map<String, ImageResource<Painter>>> =
    compositionLocalOf { emptyMap() }
private val LocalImageResource: ProvidableCompositionLocal<ImageResource<Painter>?> =
    compositionLocalOf { null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableScreen(
    id: String,
    viewModel: TableViewModel,
    navigateBack: () -> Unit,
) {
    val table = viewModel.tableFlow.find { it.id == id }
    val tableUiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val audioPlayer = LocalAudioPlayer.current
    val successHaptic = rememberHaptic(HapticKind.Success)
    val errorHaptic = rememberHaptic(HapticKind.Error)
    val user = LocalAuthUser.current

    LaunchedEffect(user) {
        viewModel.currentUsername = user.name
    }

    val submitData = {
        focusManager.clearFocus(true)
        viewModel.submitData()
    }

    LaunchedEffect(viewModel.submitResultChannel, audioPlayer, successHaptic, errorHaptic) {
        for (ok in viewModel.submitResultChannel) {
            if (ok) {
                viewModel.resetColumnData()
                UserMessageServiceImpl.displayMessage(
                    message = InternalStringResource(
                        Res.string.table_screen_data_submitted
                    ),
                )
                audioPlayer.playSuccess()
                successHaptic.start()
            } else {
                audioPlayer.playError()
                errorHaptic.start()
            }
        }
    }

    if (tableUiState.scannedBarcodes.isNotEmpty()) {
        SelectScannedBarcodeDialog(
            scannedBarcodes = tableUiState.scannedBarcodes,
            selectBarcode = { viewModel.selectBarcode(it) },
            dismiss = { viewModel.selectBarcode(-1) }
        )
    }

    TableNotAvailableWhileOfflineDialog(
        tableUiState = tableUiState,
        navigateBack = navigateBack,
    )

    KeyboardAwareScaffold(
        topBar = {
            SkanMateTopAppBar(
                title = {
                    Text(table?.name ?: "Oh no!")
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigateBack,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LocalContentColor.current),
                        elevation = CustomButtonElevation.None,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = LocalContentColor.current,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            TableFloatingActionButton(
                isVisible = tableUiState.status == FetchStatus.Success,
                submitData = submitData,
                tableUiState = tableUiState,
            )
        },
    ) { paddingValues ->
        if (table == null || tableUiState.status == FetchStatus.NotFound) {
            TableNotFound(
                modifier = Modifier.padding(paddingValues),
            )
        } else {
            Surface(
                modifier = Modifier.padding(paddingValues),
            ) {
                LaunchedEffect(viewModel) {
                    viewModel.setCurrentTableId(id)
                }

                RegisterScanEventHandler(handler = viewModel)

                TableContent(
                    tableUiState = tableUiState,
                    getUpdatedColumns = {
                        viewModel.uiState.value.columns
                    },
                    setFocusedColumn = { id, focused ->
                        if (focused && tableUiState.focusedColumnId != id) {
                            viewModel.setFocusedColumn(id)
                        } else if (!focused && tableUiState.focusedColumnId == id) {
                            viewModel.setFocusedColumn(null)
                        }
                    },
                    focusNextColumn = {
                        val nextId =
                            tableUiState.columns.indexOfFirst { col -> col.id == tableUiState.focusedColumnId }
                                .let { idx ->
                                    tableUiState.columns.getOrNull((idx + 1) % tableUiState.columns.size)?.id
                                }

                        viewModel.setFocusedColumn(nextId)
                    },
                    submitData = submitData,
                    validateColumn = { col, value ->
                        viewModel.validateColumn(col, value)
                    },
                    deleteLocalFile = { path ->
                        println("TableScreen::deleteLocalFile($path)")
                        viewModel.deleteLocalImage(path)
                    },
                ) { columns ->
                    viewModel.updateColumns(columns)
                }
            }
        }
    }
}

@Composable
fun TableNotAvailableWhileOfflineDialog(
    tableUiState: TableUiState,
    navigateBack: () -> Unit,
    hasConnectionState: State<Boolean> = LocalConnectionState.current,
) {
    val hasConnection by hasConnectionState

    if (!(hasConnection || tableUiState.isAvailableOffline)) {
        val contentPadding = PaddingValues(16.dp)

        ContentDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            closable = false,
            contentPadding = contentPadding,
            title = {
                Text(
                    text = stringResource(Res.string.table_screen_connection_lost_title),
                    style = titleTextStyle(),
                )
            }
        ) {
            Column(
                modifier = Modifier.padding(paddingValues = contentPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(Res.string.table_screen_connection_lost_content_text_1),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(Res.string.table_screen_connection_lost_content_text_2),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(Res.string.table_screen_connection_lost_content_text_3),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = navigateBack,
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        )
                    ) {
                        Text(
                            text = stringResource(Res.string.table_screen_connection_lost_exit_btn),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SelectScannedBarcodeDialog(
    scannedBarcodes: List<String>,
    selectBarcode: (idx: Int) -> Unit,
    dismiss: () -> Unit,
) {
    ContentDialog(
        onDismissRequest = dismiss,
        title = {
            Text(
                text = stringResource(Res.string.table_screen_multiple_barcodes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        description = {
            Text(
                text = stringResource(Res.string.table_screen_multiple_barcodes_desc),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState()),
        ) {
            repeat(times = scannedBarcodes.size) { idx ->
                if (idx != 0) {
                    HorizontalDivider()
                }
                Row(
                    modifier = Modifier
                        .clickable { selectBarcode(idx) }
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                ) {
                    Text(
                        text = scannedBarcodes[idx],
                        softWrap = false,
                        overflow = TextOverflow.MiddleEllipsis,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun TableFloatingActionButton(
    submitData: () -> Unit,
    isVisible: Boolean,
    tableUiState: TableUiState,
) {
    val noErrors = tableUiState.constraintErrors.values.all { list -> list.isEmpty() }

    AnimatedVisibility(
        visible = isVisible,
    ) {
        FloatingActionButton(
            modifier = Modifier.padding(16.dp),
            onClick = submitData,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.darken(
                    .15f
                ),
                disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            enabled = !tableUiState.isSubmitting && noErrors,
            expanded = noErrors,
            icon = {
                AnimatedContent(targetState = tableUiState.isSubmitting) { submitting ->
                    if (submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = LocalContentColor.current,
                            trackColor = MaterialTheme.colorScheme.primaryContainer.darken(0.15f),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Check, contentDescription = "Submit icon"
                        )
                    }
                }
            },
        ) {
            Text(stringResource(Res.string.submit))
        }
    }
}

@Composable
fun TableContent(
    modifier: Modifier = Modifier,
    tableUiState: TableUiState,
    getUpdatedColumns: () -> List<ColumnUiState>,
    setFocusedColumn: (String, Boolean) -> Unit = { _, _ -> },
    focusNextColumn: () -> Unit,
    submitData: () -> Unit = {},
    deleteLocalFile: (path: String) -> Unit,
    validateColumn: (ColumnUiState, ColumnValue) -> Boolean,
    updateColumns: (List<ColumnUiState>) -> Unit,
) {
    val imageResourceMap = mapOf(
        *tableUiState.columns.mapNotNull { col ->
            if (col.type is ColumnType.File && col.value is ColumnValue.File) {
                col.name to loadImage(col.value.localUrl)
            } else {
                null
            }
        }.toTypedArray()
    )
    val focusManager = LocalFocusManager.current

    CompositionLocalProvider(LocalImageResourceMap provides imageResourceMap) {
        Box(
            modifier = modifier.fillMaxSize().pointerInput(focusManager, tableUiState) {
                detectTapGestures {
                    focusManager.clearFocus()
                    tableUiState.focusedColumnId?.also { focusedColumnId ->
                        setFocusedColumn(focusedColumnId, false)
                    }
                }
            },
            contentAlignment = Alignment.Center,
            propagateMinConstraints = true,
        ) {
            AnimatedContent(
                targetState = tableUiState.isFetching,
            ) { isFetching ->
                if (isFetching) {
                    Box(
                        modifier = modifier.requiredSize(48.dp).align(Alignment.Center),
                        contentAlignment = Alignment.Center,
                        propagateMinConstraints = true,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyVerticalGrid(
                            modifier = Modifier.weight(1f, fill = true),
                            columns = GridCells.Fixed(12),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            tableColumns(
                                columns = tableUiState.displayColumns,
                                constraintErrors = tableUiState.constraintErrors,
                                updateCol = { col ->
                                    updateColumns(
                                        getUpdatedColumns().map { c -> if (c.id == col.id) col else c })
                                },
                                focusedColumnId = tableUiState.focusedColumnId,
                                setFocus = setFocusedColumn,
                                onNext = {
                                    focusNextColumn()
                                },
                                onDone = {
                                    submitData()
                                },
                                deleteFile = deleteLocalFile,
                                validateColumn = validateColumn,
                                enabled = !tableUiState.isSubmitting
                            )

                            item(
                                span = { GridItemSpan(maxCurrentLineSpan) }) {
                                val lineHeightDp =
                                    with(LocalDensity.current) { LocalLabelTextStyle.current.lineHeight.toDp() }
                                Spacer(
                                    modifier = Modifier.height(lineHeightDp + 56.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun LazyGridScope.tableColumns(
    columns: List<ColumnUiState>,
    validateColumn: (ColumnUiState, ColumnValue) -> Boolean,
    constraintErrors: Map<String, List<InternalStringResource>>,
    updateCol: (ColumnUiState) -> Unit,
    focusedColumnId: String?,
    setFocus: (String, Boolean) -> Unit = { _, _ -> },
    onNext: () -> Unit = {},
    onDone: () -> Unit = {},
    deleteFile: (String) -> Unit,
    enabled: Boolean = true,
) {
    itemsIndexed(items = columns, key = { _, c -> c.id }, span = { _, col ->
        GridItemSpan((maxLineSpan * col.width).roundToInt())
    }, contentType = { _, col -> col.type }) { idx, col ->
        CompositionLocalProvider(LocalImageResource provides LocalImageResourceMap.current[col.name]) {
            TableColumn(
                col = col,
                errors = constraintErrors[col.name] ?: emptyList(),
                enabled = enabled,
                isFocused = col.id == focusedColumnId,
                setFocus = setFocus,
                isLast = idx == columns.size - 1,
                deleteFile = deleteFile,
                onKeyboardAction = { action ->
                    when (action) {
                        ImeAction.Next -> onNext()
                        ImeAction.Done -> onDone()
                    }
                },
                validateValue = { value ->
                    validateColumn(col, value)
                }) { newColValue ->
                updateCol(col.copy(value = newColValue))
            }
        }
    }
}

@Composable
fun TableColumn(
    col: ColumnUiState,
    errors: List<InternalStringResource>,
    enabled: Boolean = true,
    isLast: Boolean = false,
    onKeyboardAction: (ImeAction) -> Unit = {},
    isFocused: Boolean,
    setFocus: (String, Boolean) -> Unit = { _, _ -> },
    deleteFile: (String) -> Unit,
    validateValue: (ColumnValue) -> Boolean,
    updateValue: (ColumnValue) -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val label = @Composable {
        Row {
            Text(text = col.name, style = LocalLabelTextStyle.current)
            if (col.isRequired) {
                Spacer(Modifier.width(2.dp))
                Text(text = "*", style = LocalLabelTextStyle.current, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    val inputHeight = 56.dp

    ColumnWithErrorLayout(
        modifier = Modifier.wrapContentHeight(),
        errors = errors,
    ) {
        val enabled = enabled
        val modifier = Modifier.fillMaxWidth()
        val imeAction = if (isLast) ImeAction.Done else ImeAction.Next

        when (col.type) {
            ColumnType.Boolean if col.value is ColumnValue.Boolean -> {
                LaunchedEffect(focusManager, isFocused) {
                    if (isFocused) {
                        focusManager.clearFocus()
                    }
                }

                TableColumnBoolean(
                    modifier = modifier,
                    label = label,
                    checked = col.value.checked,
                    setChecked = { checked ->
                        updateValue(col.value.copy(checked = checked))
                    },
                    enabled = enabled,
                    isFocused = isFocused,
                    height = inputHeight,
                )
            }
            ColumnType.File if col.value is ColumnValue.File -> {
                LaunchedEffect(focusManager, isFocused) {
                    if (isFocused) {
                        focusManager.clearFocus()
                    }
                }

                TableColumnFile(
                    modifier = modifier,
                    label = label,
                    value = if (col.value.localUrl == null) null
                    else ImageData(
                        path = col.value.localUrl, name = col.value.fileName, data = col.value.bytes
                    ),
                    deleteFile = { path ->
                        println("TableColumn::deleteFile($path)")
                        if (path != null) deleteFile(path)
                    },
                    setValue = { data ->
                        updateValue(
                            col.value.copy(
                                localUrl = data?.path,
                                fileName = data?.name,
                                bytes = data?.data,
                                isUploaded = col.value.isUploaded && data != null
                            )
                        )
                    },
                    enabled = enabled,
                    isFocused = isFocused,
                    buttonHeight = inputHeight,
                )
            }
            ColumnType.List if col.value is ColumnValue.OptionList -> {
                LaunchedEffect(focusRequester, isFocused) {
                    if (isFocused) {
                        focusRequester.requestFocus()
                    }
                }

                TableColumnList(
                    modifier = modifier,
                    selectOption = { opt ->
                        updateValue(
                            col.value.copy(selected = opt)
                        )
                    },
                    option = col.value.selected,
                    options = col.value.options,
                    label = label,
                    placeholder = {
                        Text(stringResource(Res.string.select_placeholder, col.name)) //"Select $label..."
                    },
                    setFocus = {
                        setFocus(col.id, it)
                    },
                    enabled = enabled,
                    focusRequester = focusRequester,
                    inputHeight = inputHeight,
                )
            }
            else -> {
                LaunchedEffect(focusRequester, isFocused) {
                    if (isFocused) {
                        focusRequester.requestFocus()
                    }
                }
                val prefixSuffix = { value: String ->
                    (@Composable {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(4.dp),
                                )
                                .padding(horizontal = 4.dp),
                        ) {
                            Text(value)
                        }
                    })
                }
                val placeholder: (@Composable () -> Unit) = {
                    Text(
                        text = stringResource(Res.string.input_placeholder, col.name), //"Input $label...",
                        maxLines = 1,
                    )
                }

                TableColumnInput(
                    modifier = modifier
                        .heightIn(max = inputHeight),
                    borderColor =
                        if (col.rememberValue)
                            if (col.value.isNotEmpty()) Color.Success else MaterialTheme.colorScheme.error
                        else Color.Unspecified,
                    label = when (col.rememberValue) {
                        true -> (@Composable {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                label()
                                TableRememberValueBadge()
                            }
                        })
                        false -> label
                    },
                    placeholder = placeholder,
                    value = when (col.value) {
                        is ColumnValue.Text -> col.value.text
                        is ColumnValue.Numeric -> col.value.num?.toString() ?: ""
                        else -> ""
                    },
                    setValue = {
                        val newValue = when (col.value) {
                            is ColumnValue.Text -> col.value.copy(text = it)
                            is ColumnValue.Numeric -> col.value.copy(
                                num = it.toIntOrNull() ?: it.toDoubleOrNull()
                            )

                            else -> col.value
                        }
                        updateValue(newValue)
                    },
                    validateValue = {
                        if (it.isNotEmpty()) {
                            when (col.value) {
                                is ColumnValue.Text -> validateValue(col.value.copy(text = it))
                                is ColumnValue.Numeric -> validateValue(
                                    col.value.copy(
                                        num = it.toIntOrNull() ?: it.toDoubleOrNull()
                                    )
                                )

                                else -> false
                            }
                        } else {
                            true
                        }
                    },
                    type = col.type,
                    enabled = enabled,
                    setFocus = { setFocus(col.id, it) },
                    imeAction = imeAction,
                    keyboardType = if (col.hasConstraint<ColumnConstraint.Email>()) KeyboardType.Email else col.type.keyboardType(),
                    onKeyboardAction = { onKeyboardAction(imeAction) },
                    isError = errors.isNotEmpty(),
                    focusRequester = focusRequester,
                    prefix = if (!col.hasPrefix) null else prefixSuffix(col.prefix),
                    suffix = if (!col.hasSuffix) null else prefixSuffix(col.suffix),
                )
            }
        }
    }
}

fun ColumnType.keyboardType(): KeyboardType {
    return if (this is ColumnType.Numeric) KeyboardType.Number else KeyboardType.Ascii
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableRememberValueBadge() {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = CoroutineScope(Dispatchers.IO)
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            PlainTooltip(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 4.dp,
            ) {
                Text(text = stringResource(Res.string.table_screen_data_remembered_tooltip))
            }
        },
        state = tooltipState,
    ) {
        Badge(
            modifier = Modifier
                .clickable {
                    scope.launch {
                        tooltipState.show()
                    }
                },
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            contentStyle = MaterialTheme.typography.labelMedium,
            contentPadding = PaddingValues(6.dp),
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = vectorResource(Res.drawable.save),
                contentDescription = "ColumnSavedIcon",
            )
        }
    }
}

@Composable
fun TableColumnInput(
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
    placeholder: @Composable () -> Unit,
    value: String,
    setValue: (String) -> Unit,
    validateValue: (String) -> Boolean,
    isError: Boolean,
    type: ColumnType,
    enabled: Boolean = type.autogenerated,
    imeAction: ImeAction = ImeAction.Next,
    onKeyboardAction: (KeyboardActionScope.() -> Unit)? = null,
    keyboardType: KeyboardType = type.keyboardType(),
    setFocus: (Boolean) -> Unit = {},
    scanModule: ScanModule = LocalScanModule.current,
    borderColor: Color = Color.Unspecified,
    shape: Shape = RoundedCornerShape(4.dp),
    focusRequester: FocusRequester,
    prefix: (@Composable () -> Unit)?,
    suffix: (@Composable () -> Unit)?,
) {
    val keyboardVisible by keyboardVisibleAsState()

    val keyboardOptions = remember(type, keyboardVisible) {
        when (type) {
            ColumnType.Unknown, ColumnType.Boolean, ColumnType.File, ColumnType.List, ColumnType.Id, ColumnType.Timestamp, ColumnType.User -> KeyboardOptions.Default.copy(
                imeAction = imeAction
            )

            ColumnType.Numeric, ColumnType.Text -> KeyboardOptions(
                capitalization = if (keyboardType == KeyboardType.Ascii) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
                keyboardType = keyboardType,
                showKeyboardOnFocus = keyboardVisible || !scanModule.isHardwareScanner(),
                imeAction = imeAction,
            )
        }
    }

    var text by remember(value) { mutableStateOf(value) }
    val onValueChange: (String) -> Unit = {
        text = it
        validateValue(it)
        if (it.isEmpty() && value.isNotEmpty()) setValue(it)
    }
    val onFocusChange: (Boolean) -> Unit = {
        setFocus(it)
        if (!it) {
            setValue(text)
        }
    }
    val keyboardActions = KeyboardActions {
        setValue(text)
        onKeyboardAction?.invoke(this) ?: defaultKeyboardAction(imeAction = imeAction)
    }
    val colors = TextFieldDefaults.colors(
        errorContainerColor = MaterialTheme.colorScheme.errorContainer,
        errorTextColor = MaterialTheme.colorScheme.onErrorContainer
    )

    if (type is ColumnType.Text || type is ColumnType.Numeric) {
        ScanableInputField(
            modifier = modifier,
            value = text,
            onValueChange = onValueChange,
            scanIconOnClick = { setFocus(true) },
            label = label,
            prefix = prefix,
            suffix = suffix,
            enabled = enabled,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            placeholder = placeholder,
            onFocusChange = onFocusChange,
            isError = isError,
            borderColor = borderColor,
            colors = colors,
            shape = shape,
            focusRequester = focusRequester,
        )
    } else {
        InputField(
            modifier = modifier,
            value = text,
            onValueChange = onValueChange,
            label = label,
            prefix = prefix,
            suffix = suffix,
            enabled = enabled,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            placeholder = placeholder,
            onFocusChange = onFocusChange,
            isError = isError,
            borderColor = borderColor,
            colors = colors,
            shape = shape,
            focusRequester = focusRequester,
        )
    }
}

@Composable
fun TableColumnFile(
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
    value: ImageData?,
    setValue: (data: ImageData?) -> Unit,
    deleteFile: (String?) -> Unit,
    enabled: Boolean = true,
    isFocused: Boolean,
    buttonHeight: Dp = 56.dp,
) {
    val imageResource = LocalImageResource.current
    val painter = imageResource?.state?.value

    val uiCameraController = LocalUiCameraController.current
    val focusManager = LocalFocusManager.current

    val listener = remember(setValue) {
        ImageCaptureListener { res ->
            when (res) {
                is ImageCaptureAction.Accept -> {
                    setValue(res.data)
                    uiCameraController.stopCamera()
                }

                is ImageCaptureAction.Discard -> {
                    deleteFile(res.data.path)
                    setValue(null)
                }
            }
        }
    }

    val hasImage = value != null
    val buttonSize = buttonHeight
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        label()

        val buttonShape = RoundedCornerShape(4.dp)
        val buttonModifier = if (isFocused) {
            Modifier.fillMaxWidth().border(1.dp, color = Color.Black, shape = buttonShape)
        } else {
            Modifier.fillMaxWidth()
        }
        PanelButton(
            modifier = buttonModifier,
            shape = buttonShape,
            onClick = {
                if (!hasImage) {
                    focusManager.clearFocus()
                    uiCameraController.startCamera(listener)
                } else {
                    uiCameraController.showPreview(value, listener)
                }
            },
            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Medium),
            heightValues = SizeValues(minHeight = buttonSize, maxHeight = buttonSize),
            contentPadding = PaddingValues(0.dp),
            enabled = enabled,
            leftPanel = {
                AnimatedContent(hasImage) { targetValue ->
                    if (!targetValue) {
                        Icon(
                            modifier = Modifier.padding(12.dp).minimumInteractiveComponentSize(),
                            imageVector = vectorResource(Res.drawable.camera),
                            contentDescription = null,
                        )
                    } else {
                        Box(
                            modifier = Modifier.aspectRatio(1f).background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ),
                            contentAlignment = Alignment.Center,
                            propagateMinConstraints = true,
                        ) {
                            AnimatedContent(painter) { targetPainter ->
                                when (targetPainter) {
                                    is ImageResourceState.Image<*> -> Image(
                                        painter = (targetPainter as ImageResourceState.Image<*>).data,
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
                }
            }) {
            val text = if (!hasImage) {
                stringResource(Res.string.table_screen_take_picture)
            } else {
                stringResource(Res.string.table_screen_retake_picture)
            }
            AutoSizeText(
                modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 12.dp),
                text = text,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableColumnList(
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
    placeholder: @Composable () -> Unit,
    selectOption: (String) -> Unit,
    setFocus: (Boolean) -> Unit,
    option: String?,
    options: List<String>,
    enabled: Boolean = true,
    inputHeight: Dp = 56.dp,
    focusRequester: FocusRequester,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        var selected = remember(option) { option ?: "" }
        InputField(
            value = selected,
            onValueChange = { selected = it },
            label = label,
            placeholder = placeholder,
            modifier = Modifier
                .heightIn(max = inputHeight)
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            singleLine = true,
            readOnly = true,
            enabled = enabled,
            onFocusChange = {
                expanded = it
                setFocus(it)
            },
            focusRequester = focusRequester,
        )
        ExposedDropdownMenu(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            expanded = expanded, onDismissRequest = { expanded = false }) {
            repeat(options.size) { idx ->
                val option = options[idx]
                DropdownMenuItem(
                    text = {
                        Text(option)
                    },
                    onClick = {
                        selected = option
                        selectOption(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
fun TableColumnBoolean(
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
    checked: Boolean,
    setChecked: (Boolean) -> Unit,
    enabled: Boolean = true,
    isFocused: Boolean,
    height: Dp = 56.dp,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        label()
        Row(
            modifier = Modifier.height(height).border(
                width = if (isFocused) 1.dp else Dp.Unspecified,
                color = Color.Black,
                shape = RoundedCornerShape(4.dp)
            ).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val switchColors = SwitchDefaults.colors()
            Switch(
                checked = checked,
                onCheckedChange = {
                    focusManager.clearFocus()
                    setChecked(it)
                },
                enabled = enabled,
                colors = switchColors,
            )

            val badgeTextStyle = MaterialTheme.typography.labelLarge
            val onText = stringResource(Res.string.table_screen_switch_on)
            val offText = stringResource(Res.string.table_screen_switch_off)
            val requiredTextWidth = max(
                a = measureText(onText, badgeTextStyle).width,
                b = measureText(offText, badgeTextStyle).width,
            )
            Badge(
                color = switchColors.containerColor(checked, enabled = true),
                contentStyle = badgeTextStyle,
                contentPadding = PaddingValues(4.dp, 3.dp)
            ) {
                Text(
                    modifier = Modifier.requiredWidthIn(min = requiredTextWidth + 8.dp),
                    text = if (checked) onText else offText,
                )
            }
        }
    }
}

@Composable
fun TableNotFound(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true
    ) {
        Column(
            modifier = Modifier.wrapContentSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.triangle_alert), contentDescription = null
            )

            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = stringResource(Res.string.table_not_found),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}
