package dk.skancode.skanmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.skancode.skanmate.data.model.LocalTableData
import dk.skancode.skanmate.data.model.RowData
import dk.skancode.skanmate.data.service.TableService
import dk.skancode.skanmate.ui.state.MutableSyncUiState
import dk.skancode.skanmate.ui.state.SyncUiState
import dk.skancode.skanmate.util.InternalStringResource
import dk.skancode.skanmate.util.snackbar.UserMessageServiceImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import skanmate.composeapp.generated.resources.Res
import skanmate.composeapp.generated.resources.constraint_error_server
import skanmate.composeapp.generated.resources.sync_vm_could_not_sync_row
import skanmate.composeapp.generated.resources.sync_vm_synchronisation_already_running

class SyncViewModel(
    private val tableService: TableService,
): ViewModel() {
    private val _localDataFlow = MutableStateFlow<List<LocalTableData>>(emptyList())
    val localDataFlow: StateFlow<List<LocalTableData>>
        get() = _localDataFlow

    private val _uiState = MutableStateFlow(MutableSyncUiState())
    val uiState: StateFlow<SyncUiState>
        get() = _uiState

    private val mutex = Mutex()

    init {
        viewModelScope.launch {
            tableService.localDataFlow.collect { localTableData ->
                _localDataFlow.update { localTableData }
            }
        }
    }

    fun deleteLocalRow(id: Long, cb: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = tableService.deleteLocalRow(rowId = id)
            tableService.updateLocalDataFlow()

            cb(res)
        }
    }

    fun deleteLocalTableRows(tableId: String, cb: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = tableService.deleteLocalTableRows(tableId = tableId)
            tableService.updateLocalDataFlow()

            cb(res)
        }
    }

    fun synchroniseLocalData(data: List<LocalTableData>) {
        if (mutex.isLocked) {
            UserMessageServiceImpl.displayError(
                message = InternalStringResource(
                    resource = Res.string.sync_vm_synchronisation_already_running,
                )
            )
            return
        }

        _uiState.update { it.copy(isLoading = true, synchronisationErrors = emptyMap()) }
        viewModelScope.launch {
            mutex.withLock {
                syncDataWithServer(data)
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun syncDataWithServer(data: List<LocalTableData>) {
        val errorMap = mutableMapOf<Long, Map<String, List<InternalStringResource>>>()

        data.forEach { tableData ->
            val tableId = tableData.model.id

            tableData.rows.forEach { localRow ->
                val columns = localRow.values.toList()
                val row: RowData = mapOf(
                    *localRow.entries.map { (key, value) -> key to value.value }.toTypedArray()
                )
                val res = tableService.submitRow(
                    tableId = tableId,
                    row = row,
                )
                if (res.ok) {
                    if (!tableService.deleteLocalRow(localRow.localRowId)) {
                        println("Local data was uploaded but not deleted from local db. Row: $localRow")
                    }
                } else if (res.errors != null) {
                    val errors = res.errors.columnErrors.mapNotNull { (k, v) ->
                        columns.find { col -> col.dbName == k }?.let { col ->
                            println("Errors for col ${col.name}:\n\t${v.joinToString("\n\t")}")
                            col.name to v.map { serverError ->
                                InternalStringResource(
                                    Res.string.constraint_error_server,
                                    listOf(serverError)
                                )
                            }
                        }
                    }.toTypedArray()
                    errorMap[localRow.localRowId] = mapOf(*errors)
                } else {
                    UserMessageServiceImpl.displayError(
                        message = InternalStringResource(
                            resource = Res.string.sync_vm_could_not_sync_row,
                            args = listOf(res.msg, localRow.localRowId)
                        )
                    )
                }
            }
        }

        tableService.updateLocalDataFlow()

        _uiState.update {
            it.copy(
                synchronisationErrors = errorMap,
            )
        }
    }
}