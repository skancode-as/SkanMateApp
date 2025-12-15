package dk.skancode.skanmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.skancode.skanmate.ImageData
import dk.skancode.skanmate.data.model.ColumnValue
import dk.skancode.skanmate.data.model.LocalColumnValue
import dk.skancode.skanmate.data.model.LocalTableData
import dk.skancode.skanmate.data.model.RowData
import dk.skancode.skanmate.data.service.FileService
import dk.skancode.skanmate.data.service.TableService
import dk.skancode.skanmate.ui.state.MutableSyncUiState
import dk.skancode.skanmate.ui.state.SyncUiState
import dk.skancode.skanmate.util.InternalStringResource
import dk.skancode.skanmate.util.snackbar.UserMessageServiceImpl
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import skanmate.composeapp.generated.resources.Res
import skanmate.composeapp.generated.resources.constraint_error_server
import skanmate.composeapp.generated.resources.sync_vm_synchronisation_already_running

const val GENERAL_ROW_ERROR_NAME = "general_error"

class SyncViewModel(
    private val tableService: TableService,
    private val fileService: FileService = FileService.instance,
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

    fun synchroniseLocalData(data: List<LocalTableData>, cb: (Boolean) -> Unit = {}) {
        if (mutex.isLocked) {
            UserMessageServiceImpl.displayError(
                message = InternalStringResource(
                    resource = Res.string.sync_vm_synchronisation_already_running,
                )
            )
            cb(false)
            return
        }

        _uiState.update { it.copy(isLoading = true, synchronisationErrors = emptyMap()) }
        viewModelScope.launch {
            val synchronisationErrors = mutex.withLock {
                syncDataWithServer(data)
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    synchronisationErrors = synchronisationErrors,
                )
            }
            cb(synchronisationErrors.isEmpty())
        }
    }

    private suspend fun syncDataWithServer(data: List<LocalTableData>): Map<Long, Map<String, List<InternalStringResource>>> {
        val errorMap = mutableMapOf<Long, Map<String, List<InternalStringResource>>>()

        val deferred = data.map { tableData ->
            viewModelScope.async {
                val tableId = tableData.model.id

                val deferredRows = tableData.rows.map { localRow ->
                    viewModelScope.async(Dispatchers.IO) {
                        val deferred = mutableListOf<Deferred<Unit>>()
                        val row: RowData = mapOf(
                            *localRow.entries
                                .map { (key, value) ->
                                    key to value.prepare(
                                        fetchAndUploadFile = { localUrl ->
                                            val imageData = fileService.loadLocalFile(localFilePath = localUrl)
                                            if (imageData.data == null || imageData.name == null) return@prepare null

                                            val objectUrl = tableService.uploadImage(tableId = tableId, filename = imageData.name, data = imageData.data)
                                            if (objectUrl == null) null
                                            else FetchAndUploadFileResponse(imageData, objectUrl)
                                        },
                                        queueImageDeletion = { localFileUrl ->
                                            deferred.add(
                                                fileService.deleteLocalFileDeferred(
                                                    localFilePath = localFileUrl,
                                                    start = CoroutineStart.LAZY,
                                                    scope = viewModelScope,
                                                )
                                            )
                                        }
                                    )
                                }.map { (key, value) ->
                                    key to value.value
                                }.toTypedArray()
                        )

                        val res = tableService.submitRow(
                            tableId = tableId,
                            row = row,
                        )
                        if (res.ok) {
                            if (!tableService.deleteLocalRow(localRow.localRowId)) {
                                println("Local data was uploaded but not deleted from local db. Row: $localRow")
                            }

                            if (deferred.isNotEmpty()) {
                                deferred.forEach { d ->
                                    d.await()
                                }
                            }

                        } else if (res.errors != null) {
                            val errors = res.errors.columnErrors.mapNotNull { (k, v) ->
                                k to v.map { serverError ->
                                    InternalStringResource(
                                        Res.string.constraint_error_server,
                                        listOf(serverError)
                                    )
                                }
                            }.toTypedArray()
                            errorMap[localRow.localRowId] = mapOf(*errors)
                        } else {
                            errorMap[localRow.localRowId] = mapOf(GENERAL_ROW_ERROR_NAME to listOf(
                                InternalStringResource(
                                    Res.string.constraint_error_server,
                                    listOf(res.msg)
                                )
                            ))
//                            UserMessageServiceImpl.displayError(
//                                message = InternalStringResource(
//                                    resource = Res.string.sync_vm_could_not_sync_row,
//                                    args = listOf(res.msg, localRow.localRowId)
//                                )
//                            )
                        }
                    }
                }

                if (deferredRows.isNotEmpty()) {
                    deferredRows.forEach { it.await() }
                }
            }
        }

        if (deferred.isNotEmpty()) {
            deferred.forEach { it.await() }
        }

        tableService.updateLocalDataFlow()
        return errorMap
    }
}

private data class FetchAndUploadFileResponse(val imageData: ImageData, val objectUrl: String)
private inline fun LocalColumnValue.prepare(
    fetchAndUploadFile: (localUrl: String) -> FetchAndUploadFileResponse?,
    queueImageDeletion: (localFileUrl: String) -> Unit,
): LocalColumnValue {
    return when(value) {
        is ColumnValue.File if value.localUrl != null && !value.isUploaded -> {
            val res = fetchAndUploadFile(value.localUrl)

            println("fetchAndUploadFile res: $res")

            val isUploaded = res != null
            if (isUploaded) {
                println("Queueing image deletion")
                queueImageDeletion(value.localUrl)
            }

            this.copy(
                value = value.copy(
                    fileName = res?.imageData?.name,
                    bytes = res?.imageData?.data,
                    objectUrl = res?.objectUrl,
                    isUploaded = isUploaded,
                )
            )
        }

        is ColumnValue.File if value.localUrl != null && value.isUploaded -> {
            queueImageDeletion(value.localUrl)

            this
        }

        is ColumnValue.File if !value.isUploaded -> {
            this.copy(
                value = value.copy(objectUrl = "")
            )
        }

        else -> this
    }
}