package dk.skancode.skanmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.skancode.skanmate.ScanEvent
import dk.skancode.skanmate.ScanEventHandler
import dk.skancode.skanmate.data.model.ColumnValue
import dk.skancode.skanmate.data.model.TableSummaryModel
import dk.skancode.skanmate.data.model.ConstraintCheckResult
import dk.skancode.skanmate.data.model.rowDataOf
import dk.skancode.skanmate.data.model.check
import dk.skancode.skanmate.data.service.TableService
import dk.skancode.skanmate.deleteFile
import dk.skancode.skanmate.ui.state.ColumnUiState
import dk.skancode.skanmate.ui.state.MutableTableUiState
import dk.skancode.skanmate.ui.state.TableUiState
import dk.skancode.skanmate.ui.state.toUiState
import dk.skancode.skanmate.ui.state.check
import dk.skancode.skanmate.util.InternalStringResource
import dk.skancode.skanmate.util.snackbar.UserMessageService
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import skanmate.composeapp.generated.resources.Res
import skanmate.composeapp.generated.resources.constraint_error_server
import skanmate.composeapp.generated.resources.table_vm_could_not_submit_data
import skanmate.composeapp.generated.resources.table_vm_could_not_submit_data_constraint
import skanmate.composeapp.generated.resources.table_vm_could_not_upload_image

class TableViewModel(
    val tableService: TableService,
    val userMessageService: UserMessageService,
) : ViewModel(), ScanEventHandler {
    private val _tableFlow = MutableStateFlow<List<TableSummaryModel>>(emptyList())
    val tableFlow: StateFlow<List<TableSummaryModel>>
        get() = _tableFlow

    @OptIn(FlowPreview::class)
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
        else _uiState.update {
            it.copy(
                focusedColumnId = it.columns.find { col -> col.id == id }?.id,
            )
        }
    }

    fun deleteLocalImage(
        path: String,
        start: CoroutineStart = CoroutineStart.DEFAULT
    ): Deferred<Unit> = viewModelScope.async(start = start) {
        println("TableViewModel::deleteLocalImage")
        deleteFile(path)
    }

    fun validateColumn(column: ColumnUiState, value: ColumnValue): Boolean {
        val constraintResults = column.constraints.check(value)

        _uiState.update {
            val errors = it.constraintErrors.toMutableMap()
            errors[column.name] =
                constraintResults.mapNotNull { v -> v as? ConstraintCheckResult.Error }
            it.copy(
                constraintErrors = errors
            )
        }

        return constraintResults.all { it == ConstraintCheckResult.Ok }
    }

    fun submitData(cb: (Boolean) -> Unit) {
        _uiState.update {
            it.copy(isSubmitting = true)
        }

        val state = _uiState.value
        val checkResults = state.columns.map { col -> col.check() }
        if (checkResults.any { v -> !v.ok }) {
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    constraintErrors = mapOf(
                        *checkResults.map { v -> v.displayName to v.errors }.toTypedArray()
                    )
                )
            }
            userMessageService.displayError(
                message = InternalStringResource(
                    resource = Res.string.table_vm_could_not_submit_data_constraint,
                )
            )
            return
        }

        viewModelScope.launch {
            var res = false
            var constraintErrors: Map<String, List<InternalStringResource>> = emptyMap()
            try {
                if (state.model != null) {
                    val deferred = mutableListOf<Deferred<Unit>>()
                    val columns: List<ColumnUiState> = state.columns.map { col ->
                        when {
                            col.value is ColumnValue.File && col.value.fileName != null && col.value.bytes != null && !col.value.isUploaded -> {
                                val bytes = col.value.bytes

                                val objectUrl = tableService.uploadImage(
                                    tableId = state.model.id,
                                    filename = col.value.fileName,
                                    data = bytes,
                                )
                                if (objectUrl == null) {
                                    println("Could not upload image")
                                    userMessageService.displayError(
                                        message = InternalStringResource(
                                            resource = Res.string.table_vm_could_not_upload_image,
                                        )
                                    )
                                    res = false
                                    return@launch
                                } else {
                                    println("Image uploaded to $objectUrl")
                                    if (col.value.localUrl != null) {
                                        deferred.add(
                                            deleteLocalImage(
                                                col.value.localUrl,
                                                CoroutineStart.LAZY
                                            )
                                        )
                                    }
                                }

                                col.copy(
                                    value = col.value.copy(objectUrl = objectUrl, isUploaded = true)
                                )
                            }

                            col.value is ColumnValue.File && !col.value.isUploaded -> {
                                col.copy(
                                    value = col.value.copy(objectUrl = "")
                                )
                            }

                            col.value is ColumnValue.File && col.value.isUploaded -> {
                                if (col.value.localUrl != null) {
                                    deferred.add(
                                        deleteLocalImage(
                                            col.value.localUrl,
                                            CoroutineStart.LAZY
                                        )
                                    )
                                }
                                col
                            }

                            else -> col
                        }
                    }

                    val (ok, errors) = tableService.submitRow(
                        tableId = state.model.id,
                        row = rowDataOf(columns),
                    )
                    val err = errors?.columnErrors?.mapNotNull { (k, v) ->
                        columns.find { col -> col.dbName == k }?.let { col ->
                            println("Errors for col ${col.name}:\n\t${v.joinToString("\n\t")}}")
                            col.name to v.map { serverError ->
                                InternalStringResource(
                                    Res.string.constraint_error_server,
                                    listOf(serverError)
                                )
                            }
                        }
                    }?.toTypedArray()
                    if (ok) {
                        deferred.forEach { it.await() }
                    } else {
                        if (err != null && err.isNotEmpty()) {
                            constraintErrors = mapOf(*err)
                            userMessageService.displayError(
                                message = InternalStringResource(
                                    resource = Res.string.table_vm_could_not_submit_data_constraint,
                                )
                            )
                        } else {
                            userMessageService.displayError(
                                message = InternalStringResource(
                                    resource = Res.string.table_vm_could_not_submit_data,
                                )
                            )
                        }

                        deferred.forEach { it.cancel() }
                    }
                    res = ok
                }
            } finally {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        constraintErrors = constraintErrors,
                    )
                }
                cb(res)
            }
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

    private fun updateColumn(id: String, v: String): Int {
        var idx = -1
        _uiState.update {
            it.copy(
                columns = it.columns.mapIndexed { i, col ->
                    if (col.id != id) col
                    else {
                        idx = i
                        col.copy(
                            value = when (col.value) {
                                is ColumnValue.Text -> ColumnValue.Text(v)
                                is ColumnValue.Numeric -> ColumnValue.Numeric(
                                    v.toIntOrNull() ?: v.toDoubleOrNull()
                                )

                                is ColumnValue.Boolean,
                                is ColumnValue.File,
                                is ColumnValue.OptionList,
                                ColumnValue.Null -> col.value.clone()
                            }
                        )
                    }
                }.toMutableList()
            )
        }
        return idx
    }

    private fun updateFocusedColumnValue(v: String): Int {
        val columns = _uiState.value.columns
        val focusedColumnId = _uiState.value.focusedColumnId
        val focusedColumn = columns.find { col -> col.id == focusedColumnId }

        if (focusedColumn == null || focusedColumnId == null) return -1

        return updateColumn(focusedColumnId, v)
    }

    private fun updateNextColumn(v: String): Int {
        val columns = _uiState.value.columns
        val nextColId = (columns.firstOrNull { col ->
            when (col.value) {
                is ColumnValue.Text -> col.value.text.isBlank()
                is ColumnValue.Numeric -> col.value.num == null

                else -> false
            }
        } ?: columns[0]).id

        return updateColumn(nextColId, v)
    }

    override fun handle(event: ScanEvent) {
        val barcode = extractBarcode(event) ?: return

        val idx = if (_uiState.value.hasFocusedColumn) {
            updateFocusedColumnValue(v = barcode)
        } else {
            updateNextColumn(v = barcode)
        }

        val columns = _uiState.value.columns
        val newFocusIdx = if (columns.getOrNull(idx)?.check().let { it != null && it.ok }) {
            (idx + 1) % columns.size
        } else {
            idx
        }

        _uiState.update {
            it.copy(
                focusedColumnId = columns.getOrNull(newFocusIdx)?.id
            )
        }
        println("Handle event done: $event")
    }
}