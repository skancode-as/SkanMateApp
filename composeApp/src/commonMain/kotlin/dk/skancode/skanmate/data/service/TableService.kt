package dk.skancode.skanmate.data.service

import dk.skancode.skanmate.data.model.LocalRowData
import dk.skancode.skanmate.data.model.LocalTableData
import dk.skancode.skanmate.data.model.OfflineTableSummaryModel
import dk.skancode.skanmate.data.model.RowData
import dk.skancode.skanmate.data.model.StoreRowResponse
import dk.skancode.skanmate.data.model.SubmitRowResponse
import dk.skancode.skanmate.data.model.TableModel
import dk.skancode.skanmate.data.model.TableRowErrors
import dk.skancode.skanmate.data.model.TableSummaryModel
import dk.skancode.skanmate.data.model.TenantModel
import dk.skancode.skanmate.data.model.isAvailableOffline
import dk.skancode.skanmate.data.store.LocalTableStore
import dk.skancode.skanmate.data.store.TableStore
import dk.skancode.skanmate.util.InternalStringResource
import dk.skancode.skanmate.util.string
import io.ktor.http.ContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import skanmate.composeapp.generated.resources.Res
import skanmate.composeapp.generated.resources.signed_out

interface TableService {
    val tableFlow: SharedFlow<List<TableSummaryModel>>
    val localDataFlow: SharedFlow<List<LocalTableData>>
    suspend fun fetchTable(id: String): TableModel?
    suspend fun updateTableFlow(): Boolean
    suspend fun uploadImage(tableId: String, filename: String, data: ByteArray): String?
    suspend fun submitRow(tableId: String, row: RowData): SubmitRowResponse
    suspend fun storeRow(tableId: String, row: LocalRowData): StoreRowResponse
    suspend fun deleteLocalRow(rowId: Long): Boolean
    suspend fun deleteLocalTableRows(tableId: String): Boolean
    suspend fun updateLocalDataFlow()
}

@OptIn(ExperimentalCoroutinesApi::class)
class TableServiceImpl(
    val tableStore: TableStore,
    val localTableStore: LocalTableStore,
    tokenFlow: SharedFlow<String?>,
    tenantFlow: SharedFlow<TenantModel?>,
    val externalScope: CoroutineScope,
    val connectivityService: ConnectivityService = ConnectivityService.instance,
) : TableService {
    private val _tableFlow = MutableSharedFlow<List<TableSummaryModel>>(1)
    override val tableFlow: SharedFlow<List<TableSummaryModel>>
        get() = _tableFlow

    private val _localDataFlow = MutableSharedFlow<List<LocalTableData>>(1)
    override val localDataFlow: SharedFlow<List<LocalTableData>>
        get() = _localDataFlow

    private var _token: String? = null
    private var _tenant: TenantModel? = null
    private suspend fun hasConnection(): Boolean = connectivityService.isConnected()

    init {
        externalScope.launch {
            updateLocalDataFlow()
        }
        externalScope.launch {
            tenantFlow.collect { tenantModel ->
                _tenant = tenantModel
                if (tenantModel != null) {
                    updateDataFlows()
                }
            }
        }
        externalScope.launch {
            tokenFlow.collect { token ->
                _token = token
                if (token == null) {
                    resetDataFlows()
                } else {
                    updateDataFlows()
                }
            }
        }
    }

    private suspend fun resetDataFlows() {
        _tableFlow.resetReplayCache()
        _tableFlow.emit(emptyList())

        _localDataFlow.resetReplayCache()
        _localDataFlow.emit(emptyList())
    }

    private suspend fun updateDataFlows() {
        updateTableFlow()
        updateLocalDataFlow()
    }

    override suspend fun fetchTable(id: String): TableModel? {
        if (!hasConnection()) {
            return localTableStore.loadTableModel(id)
        }

        val token = _token ?: return null

        val res = tableStore.fetchFullTable(id, token)
        if (!res.ok || res.data == null) return null

        return res.data.toModel()
    }

    override suspend fun updateTableFlow(): Boolean {
        val token = _token ?: return false
        val tenantId: String = _tenant?.id ?: return false

        val res = fetchTables(token = token, tenantId = tenantId)
        _tableFlow.emit(res)

        return res.isNotEmpty()
    }

    override suspend fun uploadImage(
        tableId: String,
        filename: String,
        data: ByteArray,
    ): String? {
        println("TableServiceImpl::uploadImage($tableId, $filename, byteCount: ${data.size})")
        val token = _token ?: return null

        println("TableServiceImpl::uploadImage() - Getting presignedURL")
        val urlRes = tableStore.getPresignedURL(
            tableId,
            filename,
            token
        )

        if (!urlRes.ok || urlRes.data == null) return null

        println("TableServiceImpl::uploadImage() - uploading to presignedURL: ${urlRes.data.presignedUrl}")
        val uploadRes = tableStore.uploadImage(
            presignedUrl = urlRes.data.presignedUrl,
            data = data,
            imageType = ContentType.Image.JPEG,
        )

        return when {
            uploadRes.ok -> {
                println("TableServiceImpl::uploadImage() - image uploaded! ObjectUrl: ${urlRes.data.objectUrl}")
                urlRes.data.objectUrl
            }
            else -> {
                println(uploadRes.msg)
                null
            }
        }
    }

    override suspend fun submitRow(tableId: String, row: RowData): SubmitRowResponse {
        val token = _token ?: return SubmitRowResponse(
            ok = false,
            msg = InternalStringResource(Res.string.signed_out).string()
        )

        val res = tableStore.submitTableData(
            tableId = tableId,
            data = listOf(row),
            token = token
        )
        val errors = if (!res.ok) {
            TableRowErrors.decode(res.details)
        } else null

        return SubmitRowResponse(
            ok = res.ok,
            msg = res.msg,
            errors = errors,
        )
    }

    override suspend fun storeRow(
        tableId: String,
        row: LocalRowData
    ): StoreRowResponse {
        try {
            val tenantId: String = _tenant?.id ?: return StoreRowResponse(ok = false,
                exception = Exception("No tenant available")
            )

            localTableStore.storeRowData(tableId = tableId, tenantId = tenantId, rowData = row)
        } catch (e: Exception) {
            return StoreRowResponse(
                ok = false,
                exception = e,
            )
        }

        externalScope.launch { updateLocalDataFlow() }

        return StoreRowResponse(
            ok = true,
            exception = null,
        )
    }

    override suspend fun deleteLocalRow(rowId: Long): Boolean {
        try {
            val rowsDeleted = localTableStore.deleteRowData(rowId)
            return rowsDeleted > 0
        } catch (_: Exception) {
            return false
        }
    }

    override suspend fun deleteLocalTableRows(tableId: String): Boolean {
        try {
            val rowsDeleted = localTableStore.deleteTableRows(tableId = tableId)
            return rowsDeleted > 0
        } catch (_: Exception) {
            return false
        }
    }

    override suspend fun updateLocalDataFlow() {
        val tenantId: String = _tenant?.id ?: return

        val localData = localTableStore.loadRowData(tenantId = tenantId)
        _localDataFlow.emit(localData)
    }

    private suspend fun fetchTables(token: String, tenantId: String): List<TableSummaryModel> {
        if (!hasConnection()) {
            return localTableStore.loadTableSummaries(tenantId = tenantId)
        }

        val res = tableStore.fetchTableSummaries(token)
        if (!res.ok || res.data == null){
            return emptyList()
        }

        val summaries = res.data.map { it.toModel() }
        val models = summaries.mapNotNull { summary ->
            fetchTable(summary.id)
        }
        localTableStore.storeTableModels(models = models, tenantId = tenantId)

        return models.map { model ->
            OfflineTableSummaryModel(
                id = model.id,
                name = model.name,
                description = model.description,
                isAvailableOffline = model.columns.isAvailableOffline()
            )
        }
    }
}