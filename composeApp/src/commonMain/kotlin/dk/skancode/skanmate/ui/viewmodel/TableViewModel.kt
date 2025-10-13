package dk.skancode.skanmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.skancode.skanmate.ScanEvent
import dk.skancode.skanmate.ScanEventHandler
import dk.skancode.skanmate.data.model.TableSummaryModel
import dk.skancode.skanmate.data.service.TableService
import dk.skancode.skanmate.ui.state.ColumnUiState
import dk.skancode.skanmate.ui.state.ColumnValue
import dk.skancode.skanmate.ui.state.MutableTableUiState
import dk.skancode.skanmate.ui.state.TableUiState
import dk.skancode.skanmate.ui.state.toUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class TableViewModel(
    val tableService: TableService,
) : ViewModel(), ScanEventHandler {
    private val _tableFlow = MutableStateFlow<List<TableSummaryModel>>(emptyList())
    val tableFlow: StateFlow<List<TableSummaryModel>>
        get() = _tableFlow

    private val _uiState = MutableStateFlow(MutableTableUiState())
    val uiState: StateFlow<TableUiState>
        get() = _uiState

    init {
        viewModelScope.launch {
            tableService.tableFlow.collect { tables ->
                _tableFlow.update { tables }
            }
        }
    }

    fun updateTableFlow(cb: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            cb(tableService.updateTableFlow())
        }
    }

    fun setCurrentTableId(id: String) {
        _uiState.update { it.copy(isFetching = true) }
        viewModelScope.launch {
            val tableModel = tableService.fetchTable(id)

            _uiState.update {
                tableModel.toUiState(isFetching = false)
            }
        }
    }

    fun updateColumns(cols: List<ColumnUiState>) {
        _uiState.update { it.copy(columns = cols) }
    }

    fun setFocusedColumn(id: String?) {
        if (id == null) _uiState.update { it.copy(focusedColumnId = null) }

        _uiState.update {
            it.copy(
                focusedColumnId = it.columns.find { col -> col.id == id }?.id,
            )
        }
    }

    fun submitData(cb: (Boolean) -> Unit) {
        _uiState.update {
            it.copy(isSubmitting = true)
        }
        viewModelScope.launch {
            delay(5.seconds)

            _uiState.update {
                it.copy(isSubmitting = false)
            }
            cb(true)
        }
    }

    fun resetColumnData() {
        _uiState.update {
            it.copy(
                columns = it.model?.columns?.map { col -> col.toUiState() } ?: emptyList(),
            )
        }
    }

    fun resetUiState() {
        _uiState.update { MutableTableUiState() }
    }

    private fun extractBarcode(e: ScanEvent): String? {
        return when (e) {
            is ScanEvent.Barcode -> {
                if (!e.ok || e.barcode == null) null
                else e.barcode
            }
        }
    }

    private fun updateFocusedColumnValue(v: String) {
        val columns = _uiState.value.columns
        val focusedColumnId = _uiState.value.focusedColumnId
        val focusedColumn = columns.find { col -> col.id == focusedColumnId }

        if (focusedColumn == null) return

        _uiState.update {
            it.copy(
                columns = it.columns.map { col ->
                    if (col.id != focusedColumnId) col
                    else focusedColumn.copy(
                        value = when (focusedColumn.value) {
                            is ColumnValue.Text -> ColumnValue.Text(v)
                            is ColumnValue.Boolean,
                            is ColumnValue.Numeric,
                            ColumnValue.Null -> focusedColumn.value.clone()
                        }
                    )
                }.toMutableList()
            )
        }
    }

    override fun handle(event: ScanEvent) {
        val barcode = extractBarcode(event) ?: return
        updateFocusedColumnValue(barcode)
    }
}