package dk.skancode.skanmate.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dk.skancode.skanmate.ScanEvent
import dk.skancode.skanmate.ScanEventHandler
import dk.skancode.skanmate.data.model.ColumnType
import dk.skancode.skanmate.ui.component.RegisterScanEventHandler
import dk.skancode.skanmate.ui.component.ScanableInputField
import dk.skancode.skanmate.ui.state.ColumnUiState
import dk.skancode.skanmate.ui.state.ColumnValue
import dk.skancode.skanmate.ui.state.FetchStatus
import dk.skancode.skanmate.ui.state.TableUiState
import dk.skancode.skanmate.ui.viewmodel.TableViewModel
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
        }
    ) { paddingValues ->
        if (table == null || tableUiState.status == FetchStatus.NotFound) {
            TableNotFound(
                modifier = Modifier.padding(paddingValues),
            )
        } else {
            LaunchedEffect(viewModel) {
                viewModel.setCurrentTableId(id)
            }

            RegisterScanEventHandler { e ->
                when (e) {
                    is ScanEvent.Barcode -> {
                        if (e.ok && e.barcode != null) {
                            println(e.barcode)
                        }
                    }
                }
            }

            TableContent(
                modifier = Modifier.padding(paddingValues),
                tableUiState = tableUiState,
            ) { columns ->
                viewModel.updateColumns(columns)
            }
        }
    }
}

@Composable
fun TableContent(
    modifier: Modifier = Modifier,
    tableUiState: TableUiState,
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
                LazyVerticalGrid(
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
                        }
                    )
                }
            }
        }
    }
}

fun LazyGridScope.tableColumns(
    columns: List<ColumnUiState>,
    updateCol: (ColumnUiState) -> Unit,
    enabled: Boolean = true,
) {
    itemsIndexed(
        items = columns,
        key = { _, c -> c.id},
        span = { _, col ->
            GridItemSpan((maxLineSpan * col.width).roundToInt())
        },
        contentType = {_, col -> col.type}
    ) { idx, col ->
        TableColumn(
            col,
            enabled,
        ) { newCol ->
            updateCol(col.copy(value = newCol))
        }
    }
}

@Composable
fun TableColumn(
    col: ColumnUiState,
    enabled: Boolean = true,
    updateValue: (ColumnValue) -> Unit = {},
) {
    val modifier = Modifier.fillMaxWidth()

    if (col.type == ColumnType.Boolean && col.value is ColumnValue.Boolean) {
        TableColumnCheckbox(
            modifier = modifier,
            label = col.name,
            checked = col.value.checked,
            setChecked = { checked ->
                updateValue(col.value.copy(checked = checked))
            },
            enabled = enabled,
        )
    } else {
        TableColumnInput(
            modifier = modifier,
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

    var text by remember { mutableStateOf(value) }

    ScanableInputField(
        modifier = modifier,
        value = text,
        onValueChange = { text = it},
        label = {
            Text(label)
        },
        enabled = enabled,
        keyboardOptions = keyboardOptions,
        keyboardActions = KeyboardActions {
            setValue(text)
            onKeyboardAction()
            defaultKeyboardAction(imeAction = imeAction)
        },
        placeholder = {
            Text(
                text = "Input $label...",
                maxLines = 1,
            )
        }
    )
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
