package dk.skancode.skanmate.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import dk.skancode.skanmate.data.model.ColumnType
import dk.skancode.skanmate.ui.component.Button
import dk.skancode.skanmate.ui.component.InputField
import dk.skancode.skanmate.ui.component.RegisterScanEventHandler
import dk.skancode.skanmate.ui.component.ScanableInputField
import dk.skancode.skanmate.ui.state.ColumnUiState
import dk.skancode.skanmate.data.model.ColumnValue
import dk.skancode.skanmate.ui.state.FetchStatus
import dk.skancode.skanmate.ui.state.TableUiState
import dk.skancode.skanmate.ui.viewmodel.TableViewModel
import dk.skancode.skanmate.util.darken
import dk.skancode.skanmate.util.find
import org.jetbrains.compose.resources.vectorResource
import skanmate.composeapp.generated.resources.Res
import skanmate.composeapp.generated.resources.triangle_alert
import kotlin.math.roundToInt

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
                    IconButton(onClick = navigateBack) {
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
    updateColumns: (List<ColumnUiState>) -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true,
    ) {
        AnimatedContent(
            targetState = tableUiState.isFetching,
        ) { isFetching ->
            val columns = tableUiState.columns
            if (isFetching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).align(Alignment.Center)
                )
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
                            columns = columns.filter { col -> !col.type.autogenerated },
                            updateCol = { col ->
                                updateColumns(
                                    columns
                                        .map { c -> if (c.id == col.id) col else c }
                                )
                            },
                            setFocus = setFocusedColumn,
                            onDone = {
                                submitData()
                            },
                            enabled = !tableUiState.isSubmitting
                        )
                    }

                    Button(
                        modifier = Modifier.padding(16.dp),
                        onClick = submitData,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.darken(.1f),
                            disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        enabled = !tableUiState.isSubmitting
                    ) {
                        Text("Submit")
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

fun LazyGridScope.tableColumns(
    columns: List<ColumnUiState>,
    updateCol: (ColumnUiState) -> Unit,
    setFocus: (String, Boolean) -> Unit = {_,_ ->},
    onDone: () -> Unit = {},
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
        TableColumn(
            col = col,
            enabled = enabled,
            setFocus = setFocus,
            isLast = idx == columns.size,
            onKeyboardAction = { action ->
                when (action) {
                    ImeAction.Done -> onDone()
                }
            }
        ) { newColValue ->
            updateCol(col.copy(value = newColValue))
        }
    }
}

@Composable
fun TableColumn(
    col: ColumnUiState,
    enabled: Boolean = true,
    isLast: Boolean = false,
    onKeyboardAction: (ImeAction) -> Unit = {},
    setFocus: (String, Boolean) -> Unit = {_,_ ->},
    updateValue: (ColumnValue) -> Unit = {},
) {
    val modifier = Modifier.fillMaxWidth()
    val focusRequester = remember { FocusRequester() }

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
    } else {
        val imeAction = if (isLast) ImeAction.Done else ImeAction.Next

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
            type = col.type,
            enabled = enabled,
            setFocus = { setFocus(col.id, it) },
            imeAction = imeAction,
            onKeyboardAction = { onKeyboardAction(imeAction) }
        )
    }
}

@Composable
fun TableColumnInput(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    setValue: (String) -> Unit,
    type: ColumnType,
    enabled: Boolean = type.autogenerated,
    imeAction: ImeAction = ImeAction.Next,
    onKeyboardAction: KeyboardActionScope.() -> Unit = {},
    setFocus: (Boolean) -> Unit = {},
) {
    val keyboardOptions = remember(type) {
        when (type) {
            ColumnType.Unknown,
            ColumnType.Boolean,
            ColumnType.Id,
            ColumnType.Timestamp,
            ColumnType.User -> KeyboardOptions.Default.copy(imeAction = imeAction)

            ColumnType.Numeric -> KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Number,
                showKeyboardOnFocus = true,
                imeAction = imeAction,
            )

            ColumnType.Text -> KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Ascii,
                showKeyboardOnFocus = true,
                imeAction = imeAction,
            )
        }
    }

    var selection by remember { mutableStateOf(TextRange(value.length)) }
    var text by remember(value) { mutableStateOf(TextFieldValue(value, selection)) }
    val onValueChange: (TextFieldValue) -> Unit = {
        text = it
        selection = it.selection
    }
    val labelComposable: (@Composable () -> Unit) = {
        Text(label)
    }
    val placeholder: (@Composable () -> Unit) = {
        Text(
            text = "Input $label...",
            maxLines = 1,
        )
    }
    val onFocusChange: (Boolean) -> Unit = {
        setFocus(it)
        if (!it) setValue(text.text)
    }
    val keyboardActions = KeyboardActions {
        setValue(text.text)
        onKeyboardAction()
        defaultKeyboardAction(imeAction = imeAction)
    }

    if (type is ColumnType.Text) {
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
        )
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
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label)
        Checkbox(
            checked = checked,
            onCheckedChange = setChecked,
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
                text = "That table could no longer be found. Please go back and try again.",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}
