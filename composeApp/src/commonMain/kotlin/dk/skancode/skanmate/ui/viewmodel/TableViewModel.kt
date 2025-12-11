package dk.skancode.skanmate.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.skancode.skanmate.ScanEvent
import dk.skancode.skanmate.ScanEventHandler
import dk.skancode.skanmate.data.model.ColumnValue
import dk.skancode.skanmate.data.model.TableSummaryModel
import dk.skancode.skanmate.data.model.ConstraintCheckResult
import dk.skancode.skanmate.data.model.LocalTableData
import dk.skancode.skanmate.data.model.rowDataOf
import dk.skancode.skanmate.data.model.check
import dk.skancode.skanmate.data.model.localRowDataOf
import dk.skancode.skanmate.data.service.ConnectivityService
import dk.skancode.skanmate.data.service.TableService
import dk.skancode.skanmate.deleteFile
import dk.skancode.skanmate.ui.state.ColumnUiState
import dk.skancode.skanmate.ui.state.MutableTableUiState
import dk.skancode.skanmate.ui.state.TableUiState
import dk.skancode.skanmate.ui.state.toUiState
import dk.skancode.skanmate.ui.state.check
import dk.skancode.skanmate.ui.state.prepare
import dk.skancode.skanmate.ui.state.prepareLocal
import dk.skancode.skanmate.util.AudioPlayerInstance
import dk.skancode.skanmate.util.InternalStringResource
import dk.skancode.skanmate.util.assert
import dk.skancode.skanmate.util.snackbar.UserMessageService
import dk.skancode.skanmate.util.snackbar.UserMessageServiceImpl
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import skanmate.composeapp.generated.resources.Res
import skanmate.composeapp.generated.resources.constraint_error_server
import skanmate.composeapp.generated.resources.table_vm_could_not_select_barcode
import skanmate.composeapp.generated.resources.table_vm_could_not_submit_data
import skanmate.composeapp.generated.resources.table_vm_could_not_submit_data_constraint
import skanmate.composeapp.generated.resources.table_vm_could_not_upload_image
import skanmate.composeapp.generated.resources.table_vm_scan_not_possible_for_col

class TableViewModel(
    val tableService: TableService,
    val userMessageService: UserMessageService,
    val connectivityService: ConnectivityService = ConnectivityService.instance,
) : ViewModel(), ScanEventHandler {
    private val _tableFlow = MutableStateFlow<List<TableSummaryModel>>(emptyList())
    val tableFlow: StateFlow<List<TableSummaryModel>>
        get() = _tableFlow

    private val _localDataFlow = MutableStateFlow<List<LocalTableData>>(emptyList())
    val localDataFlow: StateFlow<List<LocalTableData>>
        get() = _localDataFlow

    @OptIn(FlowPreview::class)
    private val _uiState = MutableStateFlow(MutableTableUiState())
    val uiState: StateFlow<TableUiState>
        get() = _uiState

    private val _submitResultChannel = Channel<Boolean>(capacity = Channel.CONFLATED)
    val submitResultChannel: ReceiveChannel<Boolean>
        get() = _submitResultChannel

    var currentUsername: String? by mutableStateOf(null)

    init {
        viewModelScope.launch {
            tableService.tableFlow.collect { tables ->
                _tableFlow.update { tables }
            }
        }
        viewModelScope.launch {
            tableService.localDataFlow.collect { data ->
                _localDataFlow.update { data }
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
        val newState = if (id == null) _uiState.updateAndGet { it.copy(focusedColumnId = null) }
        else _uiState.updateAndGet {
            it.copy(
                focusedColumnId = it.columns.find { col -> col.id == id }?.id,
            )
        }

        println("TableViewModel::setFocusedColumn($id) - new focused column id: ${newState.focusedColumnId}, name: ${newState.columns.find { it.id == newState.focusedColumnId }?.name}")
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

    fun submitData() {
        _uiState.update {
            it.copy(isSubmitting = true)
        }
        viewModelScope.launch {
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
                _submitResultChannel.send(false)
                return@launch
            }

            if (connectivityService.isConnected()) {
                sendDataToServer(state)
            } else {
                storeDataLocally(state)
            }
        }
    }

    private suspend fun storeDataLocally(state: MutableTableUiState) {
        var res = false
        try {
            if (state.model != null) {
                val columns: List<ColumnUiState> = state.columns.map { col -> col.prepareLocal(currentUsername ?: "") }
                println("Locally prepared columnValues: ${columns.joinToString { col -> "${col.name}: ${col.value}" }}")

                val submitRes = tableService.storeRow(
                    tableId = state.model.id,
                    row = localRowDataOf(columns),
                )

                res = submitRes.ok.also { ok ->
                    if (!ok) {
                        println("Local insert of data failed with exception: ${submitRes.exception}")
                        submitRes.exception?.printStackTrace()
                        userMessageService.displayError(
                            message = InternalStringResource(
                                resource = Res.string.table_vm_could_not_submit_data,
                            )
                        )
                    }
                }
            }
        } finally {
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                )
            }
            _submitResultChannel.send(res)
        }
    }

    private suspend fun sendDataToServer(state: MutableTableUiState) {
        var res = false
        var constraintErrors: Map<String, List<InternalStringResource>> = emptyMap()
        try {
            if (state.model != null) {
                val deferred = mutableListOf<Deferred<Unit>>()
                val columns: List<ColumnUiState> = state.columns.map { col ->
                    col.prepare(
                        uploadImage = { fileName, bytes ->
                            val objectUrl = tableService.uploadImage(
                                tableId = state.model.id,
                                filename = fileName,
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
                                return
                            }

                            objectUrl
                        },
                        queueImageDeletion = { localFileUrl ->
                            deferred.add(
                                deleteLocalImage(
                                    path = localFileUrl,
                                    start = CoroutineStart.LAZY
                                )
                            )
                        },
                    )
                }

                val submitRes = tableService.submitRow(
                    tableId = state.model.id,
                    row = rowDataOf(columns),
                )
                val ok = submitRes.ok
                val msg = submitRes.msg
                val errors = submitRes.errors

                val err = errors?.columnErrors?.mapNotNull { (k, v) ->
                    columns.find { col -> col.dbName == k }?.let { col ->
                        println("Errors for col ${col.name}:\n\t${v.joinToString("\n\t")}")
                        col.name to v.map { serverError ->
                            InternalStringResource(
                                Res.string.constraint_error_server,
                                listOf(serverError)
                            )
                        }
                    }
                }?.toTypedArray()
                res = ok.also { ok ->
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
                                    args = listOf(msg)
                                )
                            )
                        }

                        deferred.forEach { it.cancel() }
                    }
                }
            }
        } finally {
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    constraintErrors = constraintErrors,
                )
            }
            _submitResultChannel.send(res)
        }
    }

    /** If [index] == -1, this will clear the scannedBarcodes, without selecting any */
    fun selectBarcode(index: Int) {
        if (index == -1) {
            _uiState.update { it.copy(scannedBarcodes = emptyList()) }
            return
        }

        val selected = _uiState.value.scannedBarcodes.getOrNull(index) ?: run {
            UserMessageServiceImpl.displayError(
                InternalStringResource(
                    Res.string.table_vm_could_not_select_barcode,
                )
            )
            return
        }

        insertBarcodeData(selected)
        _uiState.update { it.copy(scannedBarcodes = emptyList()) }
    }

    fun resetColumnData() {
        _uiState.update {
            assert(it.columns.size == it.model?.columns?.size)
            val resatColumns = it.model?.columns?.mapIndexed { idx, col ->
                var colUiState = col.toUiState()
                if (col.rememberValue) {
                    colUiState = colUiState.copy(value = it.columns[idx].value)
                }

                colUiState
            } ?: emptyList()
            it.copy(
                columns = resatColumns,
                focusedColumnId = resatColumns.firstOrNull { col -> !col.rememberValue }?.id
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
                                is ColumnValue.Text -> {
                                    idx = i
                                    ColumnValue.Text(v)
                                }

                                is ColumnValue.Numeric -> {
                                    idx = i
                                    ColumnValue.Numeric(
                                        v.toIntOrNull() ?: v.toDoubleOrNull()
                                    )
                                }

                                is ColumnValue.Boolean,
                                is ColumnValue.File,
                                is ColumnValue.OptionList,
                                ColumnValue.Null -> {
                                    AudioPlayerInstance.playError()
                                    UserMessageServiceImpl.displayError(
                                        message = InternalStringResource(
                                            Res.string.table_vm_scan_not_possible_for_col,
                                            col.name,
                                        )
                                    )
                                    col.value.clone()
                                }
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

        if (focusedColumn == null || focusedColumnId == null) {
            AudioPlayerInstance.playError()
            return -1
        }

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

    private fun insertBarcodeData(barcode: String) {
        val idx = if (_uiState.value.hasFocusedColumn) {
            updateFocusedColumnValue(v = barcode)
        } else {
            updateNextColumn(v = barcode)
        }
        val columns = _uiState.value.columns
        val displayColumns = _uiState.value.displayColumns

        val isLast =
            displayColumns.isNotEmpty() && columns.getOrNull(idx)?.id == displayColumns.last().id
        val isValid = columns.getOrNull(idx)?.check().let { it != null && it.ok }

        val newFocusIdx = when {
            isLast && isValid -> -1
            isValid -> (idx + 1) % columns.size
            else -> idx
        }

        setFocusedColumn(id = columns.getOrNull(newFocusIdx)?.id)

        if (isLast && isValid) {
            println("submitting data after event handled")
            submitData()
        }
    }

    private fun displayFoundBarcodes(barcodes: List<String>) {
        _uiState.update {
            it.copy(
                scannedBarcodes = barcodes,
            )
        }
    }

    override fun handleEvents(events: List<ScanEvent>) {
        val barcodes = events.mapNotNull { event -> extractBarcode(event) }

        when {
            barcodes.isEmpty() -> {}
            barcodes.size == 1 -> insertBarcodeData(barcodes[0])
            else -> displayFoundBarcodes(barcodes)
        }

        println("Handle event done: $events")
    }
}