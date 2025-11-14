package dk.skancode.skanmate.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import dk.skancode.skanmate.ui.component.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import dk.skancode.skanmate.ui.component.ColumnWithErrorLayout
import dk.skancode.skanmate.ui.component.CustomButtonElevation
import dk.skancode.skanmate.ui.component.FullWidthButton
import dk.skancode.skanmate.ui.component.ImageCaptureAction
import dk.skancode.skanmate.ui.component.ImageCaptureListener
import dk.skancode.skanmate.ui.component.LocalScanModule
import dk.skancode.skanmate.ui.component.LocalUiCameraController
import dk.skancode.skanmate.ui.state.FetchStatus
import dk.skancode.skanmate.ui.state.TableUiState
import dk.skancode.skanmate.ui.viewmodel.TableViewModel
import dk.skancode.skanmate.util.InternalStringResource
import dk.skancode.skanmate.util.darken
import dk.skancode.skanmate.util.find
import dk.skancode.skanmate.util.keyboardVisibleAsState
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import skanmate.composeapp.generated.resources.Res
import skanmate.composeapp.generated.resources.camera
import skanmate.composeapp.generated.resources.cannot_display_image
import skanmate.composeapp.generated.resources.input_placeholder
import skanmate.composeapp.generated.resources.select_placeholder
import skanmate.composeapp.generated.resources.submit
import skanmate.composeapp.generated.resources.table_not_found
import skanmate.composeapp.generated.resources.triangle_alert
import kotlin.math.roundToInt

private val LocalImageResourceMap: ProvidableCompositionLocal<Map<String, ImageResource<Painter>>> = compositionLocalOf { emptyMap() }
private val LocalImageResource: ProvidableCompositionLocal<ImageResource<Painter>?> = compositionLocalOf { null }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(table?.name ?: "Oh no!")
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigateBack,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LocalContentColor.current),
                        elevation = CustomButtonElevation(all = Dp.Unspecified)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        },
    ) { paddingValues ->
        if (table == null || tableUiState.status == FetchStatus.NotFound) {
            TableNotFound(
                modifier = Modifier.padding(paddingValues),
            )
        } else {
            Surface(
                modifier = Modifier.padding(paddingValues).imePadding(),
            ) {
                LaunchedEffect(viewModel) {
                    viewModel.setCurrentTableId(id)
                }

                RegisterScanEventHandler(handler = viewModel)

                TableContent(
                    tableUiState = tableUiState,
                    setFocusedColumn = { id, focused ->
                        if (focused && tableUiState.focusedColumnId != id) {
                            viewModel.setFocusedColumn(id)
                        } else if (!focused && tableUiState.focusedColumnId == id) {
                            viewModel.setFocusedColumn(null)
                        }
                    },
                    submitData = {
                        focusManager.clearFocus(true)
                        viewModel.submitData { ok ->
                            if (ok) {
                                viewModel.resetColumnData()
                            }
                        }
                    },
                    validateColumn = { col, value ->
                        viewModel.validateColumn(col, value)
                    },
                    deleteLocalFile = { path ->
                        println("TableScreen::deleteLocalFile($path)")
                        viewModel.deleteLocalImage(path)
                    }
                ) { columns ->
                    viewModel.updateColumns(columns)
                }
            }
        }
    }
}

@Composable
fun TableContent(
    modifier: Modifier = Modifier,
    tableUiState: TableUiState,
    setFocusedColumn: (String, Boolean) -> Unit = { _, _ -> },
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
            modifier = modifier
                .fillMaxSize()
                .pointerInput(focusManager) {
                    detectTapGestures { focusManager.clearFocus() }
                },
            contentAlignment = Alignment.Center,
            propagateMinConstraints = true,
        ) {
            AnimatedContent(
                targetState = tableUiState.isFetching,
            ) { isFetching ->
                val columns = tableUiState.columns
                if (isFetching) {
                    Box(
                        modifier = modifier
                            .requiredSize(48.dp)
                            .align(Alignment.Center) ,
                        contentAlignment = Alignment.Center,
                        propagateMinConstraints = true,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(4.dp)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                    ) {
                        LazyVerticalGrid(
                            modifier = Modifier.weight(1f, fill = true),
                            columns = GridCells.Fixed(12),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            tableColumns(
                                columns = columns.filter { col -> !col.type.autogenerated && col.constraints.none { v -> v is ColumnConstraint.ConstantValue } },
                                constraintErrors = tableUiState.constraintErrors,
                                updateCol = { col ->
                                    updateColumns(
                                        columns
                                            .map { c -> if (c.id == col.id) col else c }
                                    )
                                },
                                focusedColumnId = tableUiState.focusedColumnId,
                                setFocus = setFocusedColumn,
                                onDone = {
                                    submitData()
                                },
                                deleteFile = deleteLocalFile,
                                validateColumn = validateColumn,
                                enabled = !tableUiState.isSubmitting
                            )
                        }

                        FullWidthButton(
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
                            horizontalArrangement = Arrangement.spacedBy(
                                8.dp,
                                Alignment.CenterHorizontally
                            ),
                            enabled = !tableUiState.isSubmitting && tableUiState.constraintErrors.values.all { list -> list.isEmpty() }
                        ) {
                            Text(stringResource(Res.string.submit))
                            AnimatedVisibility(tableUiState.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = LocalContentColor.current,
                                    trackColor = MaterialTheme.colorScheme.primaryContainer.darken(0.15f),
                                    strokeWidth = 2.dp,
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
    onDone: () -> Unit = {},
    deleteFile: (String) -> Unit,
    enabled: Boolean = true,
) {
    itemsIndexed(
        items = columns,
        key = { _, c -> c.id },
        span = { _, col ->
            GridItemSpan((maxLineSpan * col.width).roundToInt())
        },
        contentType = { _, col -> col.type }
    ) { idx, col ->
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
                        ImeAction.Done -> onDone()
                    }
                },
                validateValue = { value ->
                    validateColumn(col, value)
                }
            ) { newColValue ->
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
    ColumnWithErrorLayout(
        modifier = Modifier.wrapContentHeight(),
        errors = errors,
    ) {
        val enabled = enabled
        val modifier = Modifier.fillMaxWidth()
        val focusRequester = remember { FocusRequester() }
        val imeAction = if (isLast) ImeAction.Done else ImeAction.Next

        if (col.type == ColumnType.Boolean && col.value is ColumnValue.Boolean) {
            TableColumnCheckbox(
                modifier = modifier.focusRequester(focusRequester),
                label = col.name,
                checked = col.value.checked,
                setChecked = { checked ->
                    updateValue(col.value.copy(checked = checked))
                },
                enabled = enabled,
            )
        } else if (col.type == ColumnType.File && col.value is ColumnValue.File) {
            TableColumnFile(
                modifier = Modifier.fillMaxWidth(),
                label = col.name,
                value =
                    if (col.value.localUrl == null) null
                    else ImageData(
                        path = col.value.localUrl,
                        name = col.value.fileName,
                        data = col.value.bytes
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
            )
        } else if (col.type == ColumnType.List && col.value is ColumnValue.OptionList) {
            TableColumnList(
                modifier = Modifier.fillMaxWidth(),
                selectOption = { opt ->
                    updateValue(
                        col.value.copy(selected = opt)
                    )
                },
                option = col.value.selected,
                options = col.value.options,
                label = col.name,
                enabled = enabled,
            )
        } else {
            LaunchedEffect(focusRequester, isFocused) {
                if (isFocused) {
                    focusRequester.requestFocus()
                    //softwareKeyboardController?.hide()
                }
            }
            TableColumnInput(
                modifier = modifier.focusRequester(focusRequester),
                label = col.name,
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
                keyboardType = if (col.constraints.any { it is ColumnConstraint.Email }) KeyboardType.Email else col.type.keyboardType(),
                onKeyboardAction = { onKeyboardAction(imeAction) },
                isError = errors.isNotEmpty()
            )
        }
    }
}

fun ColumnType.keyboardType(): KeyboardType {
    return if (this is ColumnType.Numeric) KeyboardType.Number else KeyboardType.Ascii
}

@Composable
fun TableColumnInput(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    setValue: (String) -> Unit,
    validateValue: (String) -> Boolean,
    isError: Boolean,
    type: ColumnType,
    enabled: Boolean = type.autogenerated,
    imeAction: ImeAction = ImeAction.Next,
    onKeyboardAction: KeyboardActionScope.() -> Unit = {},
    keyboardType: KeyboardType = type.keyboardType(),
    setFocus: (Boolean) -> Unit = {},
    scanModule: ScanModule = LocalScanModule.current,
) {
    val keyboardVisible by keyboardVisibleAsState()

    val keyboardOptions = remember(type, keyboardVisible) {
        when (type) {
            ColumnType.Unknown,
            ColumnType.Boolean,
            ColumnType.File,
            ColumnType.List,
            ColumnType.Id,
            ColumnType.Timestamp,
            ColumnType.User -> KeyboardOptions.Default.copy(imeAction = imeAction)

            ColumnType.Numeric,
            ColumnType.Text -> KeyboardOptions(
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
    }
    val labelComposable: (@Composable () -> Unit) = {
        Text(label)
    }
    val placeholder: (@Composable () -> Unit) = {
        Text(
            text = stringResource(Res.string.input_placeholder, label), //"Input $label...",
            maxLines = 1,
        )
    }
    val onFocusChange: (Boolean) -> Unit = {
        setFocus(it)
        if (!it) {
            setValue(text)
        }
    }
    val keyboardActions = KeyboardActions {
        setValue(text)
        onKeyboardAction()
        defaultKeyboardAction(imeAction = imeAction)
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
            label = labelComposable,
            enabled = enabled,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            placeholder = placeholder,
            onFocusChange = onFocusChange,
            isError = isError,
            colors = colors,
        )
    } else {
        InputField(
            modifier = modifier,
            value = text,
            onValueChange = onValueChange,
            label = labelComposable,
            enabled = enabled,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            placeholder = placeholder,
            onFocusChange = onFocusChange,
            isError = isError,
            colors = colors,
        )
    }
}

@Composable
fun TableColumnFile(
    modifier: Modifier = Modifier,
    label: String,
    value: ImageData?,
    setValue: (data: ImageData?) -> Unit,
    deleteFile: (String?) -> Unit,
    enabled: Boolean = true,
) {
    val imageResource = LocalImageResource.current
    val loading = imageResource?.isLoading?.value ?: true
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

    val size = 56.dp
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label)
        AnimatedContent(value) { targetValue ->
            if (targetValue == null) {
                IconButton(
                    modifier = Modifier.requiredSize(size = size),
                    onClick = {
                        focusManager.clearFocus()
                        uiCameraController.startCamera(listener)
                    },
                    enabled = enabled,
                ) {
                    Icon(
                        modifier = Modifier.minimumInteractiveComponentSize(),
                        imageVector = vectorResource(Res.drawable.camera),
                        contentDescription = null,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .requiredSize(size = size)
                        .shadow(2.dp, RoundedCornerShape(8.dp))
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            uiCameraController.showPreview(targetValue, listener)
                        },
                    contentAlignment = Alignment.Center,
                    propagateMinConstraints = true,
                ) {
                    AnimatedContent(loading) { isLoadingImage ->
                        if (isLoadingImage) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(all = 4.dp)
                                    .fillMaxSize(),
                            )
                        } else {
                            when (painter) {
                                is ImageResourceState.Image<*> ->
                                    Image(
                                        painter = (painter as ImageResourceState.Image<*>).data,
                                        contentDescription = null,
                                        contentScale = ContentScale.FillWidth,
                                    )
                                is ImageResourceState.Error ->
                                    Text(stringResource(Res.string.cannot_display_image))

                                else ->
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .padding(all = 4.dp)
                                            .fillMaxSize(),
                                    )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableColumnList(
    modifier: Modifier = Modifier,
    label: String,
    selectOption: (String) -> Unit,
    option: String?,
    options: List<String>,
    enabled: Boolean = true,
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
            label = { Text(label) },
            placeholder = {
                Text(stringResource(Res.string.select_placeholder, label)) //"Select $label..."
            },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            singleLine = true,
            readOnly = true,
            enabled = enabled,
            onFocusChange = {
                expanded = it
            }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
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
fun TableColumnCheckbox(
    modifier: Modifier = Modifier,
    label: String,
    checked: Boolean,
    setChecked: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label)
        Checkbox(
            checked = checked,
            onCheckedChange = {
                focusManager.clearFocus()
                setChecked(it)
            },
            enabled = enabled,
        )
    }
}

@Composable
fun TableNotFound(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true
    ) {
        Column(
            modifier = Modifier
                .wrapContentSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.triangle_alert),
                contentDescription = null
            )

            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = stringResource(Res.string.table_not_found),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}
