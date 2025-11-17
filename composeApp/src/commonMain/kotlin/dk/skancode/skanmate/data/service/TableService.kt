package dk.skancode.skanmate.data.service

import dk.skancode.skanmate.data.model.RowData
import dk.skancode.skanmate.data.model.SubmitRowResponse
import dk.skancode.skanmate.data.model.TableModel
import dk.skancode.skanmate.data.model.TableRowErrors
import dk.skancode.skanmate.data.model.TableSummaryModel
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

    suspend fun fetchTable(id: String): TableModel?
    suspend fun updateTableFlow(): Boolean
    suspend fun uploadImage(tableId: String, filename: String, data: ByteArray): String?
    suspend fun submitRow(tableId: String, row: RowData): SubmitRowResponse
}

@OptIn(ExperimentalCoroutinesApi::class)
class TableServiceImpl(
    val tableStore: TableStore,
    tokenFlow: SharedFlow<String?>,
    externalScope: CoroutineScope,
) : TableService {
    private val _tableFlow = MutableSharedFlow<List<TableSummaryModel>>(1)
    override val tableFlow: SharedFlow<List<TableSummaryModel>>
        get() = _tableFlow
    private var _token: String? = null

    init {
        externalScope.launch {
            tokenFlow.collect { token ->
                _token = token
                if (token == null) {
                    _tableFlow.resetReplayCache()
                } else {
                    _tableFlow.emit(fetchTables(token))
                }
            }
        }
    }

    override suspend fun fetchTable(id: String): TableModel? {
        if (_token == null) return null

        val res = tableStore.fetchFullTable(id, _token!!)
        if (!res.ok || res.data == null) return null

        return res.data.toModel()
    }

    override suspend fun updateTableFlow(): Boolean {
        val token = _token
        if (token == null) return false

        val res = fetchTables(token)
        _tableFlow.emit(res)

        return res.isNotEmpty()
    }

    override suspend fun uploadImage(
        tableId: String,
        filename: String,
        data: ByteArray,
    ): String? {
        val token = _token
        if (token == null) return null

        val urlRes = tableStore.getPresignedURL(
            tableId,
            filename,
            token
        )

        if (!urlRes.ok || urlRes.data == null) return null

        val uploadRes = tableStore.uploadImage(
            presignedUrl = urlRes.data.presignedUrl,
            data = data,
            imageType = ContentType.Image.JPEG,
        )

        return when {
            uploadRes.ok -> {
                urlRes.data.objectUrl
            }
            else -> {
                println(uploadRes.msg)
                null
            }
        }
    }

    override suspend fun submitRow(tableId: String, row: RowData): SubmitRowResponse {
        val token = _token
        if (token == null) return SubmitRowResponse(ok = false, msg = InternalStringResource(Res.string.signed_out).string())
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


    private suspend fun fetchTables(token: String): List<TableSummaryModel> {
        val res = tableStore.fetchTableSummaries(token)

        if (!res.ok || res.data == null){
            return emptyList()
        }

        return res.data.map { it.toModel() }
    }
}