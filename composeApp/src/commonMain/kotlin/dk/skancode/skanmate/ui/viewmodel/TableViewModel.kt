package dk.skancode.skanmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.skancode.skanmate.data.model.TableSummaryModel
import dk.skancode.skanmate.data.service.TableService
import dk.skancode.skanmate.ui.state.ColumnUiState
import dk.skancode.skanmate.ui.state.FetchStatus
import dk.skancode.skanmate.ui.state.MutableTableUiState
import dk.skancode.skanmate.ui.state.TableUiState
import dk.skancode.skanmate.ui.state.toUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TableViewModel(
    val tableService: TableService,
): ViewModel() {
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

    fun resetUiState() {
        _uiState.update { MutableTableUiState() }
    }
}