package dk.skancode.skanmate.data.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface TableSummaryModel {
    val id: String
    val name: String
    val description: String?
    fun availableOffline(): Boolean
}

fun TableSummaryModel(
    id: String,
    name: String,
    description: String?,
): TableSummaryModel = OnlineTableSummaryModel(
    id = id,
    name = name,
    description = description,
)

@Serializable
data class OnlineTableSummaryModel(
    override val id: String,
    override val name: String,
    override val description: String?
): TableSummaryModel {
    override fun availableOffline(): Boolean = true
}

@Serializable
data class OfflineTableSummaryModel(
    override val id: String,
    override val name: String,
    override val description: String?,
    var isAvailableOffline: Boolean = true,
): TableSummaryModel {

    override fun availableOffline(): Boolean = isAvailableOffline
}

@Serializable
data class TableSummaryResponseDTO(
    val tables: List<TableSummaryDTO>
)

@Serializable
data class TableSummaryDTO(
    val id: String,
    val displayName: String,
    val displayDescription: String?,
) {
    fun toModel(): TableSummaryModel = TableSummaryModel(
        id = id,
        name = displayName,
        description = displayDescription,
    )
}

@Serializable
data class TableModel(
    val id: String,
    val databaseName: String,
    val name: String,
    val description: String?,
    val columns: List<ColumnModel>,
)

@Serializable
data class ColumnModel(
    val id: String,
    override val dbName: String,
    override val name: String,
    override val width: Float,
    override val type: ColumnType,
    override val constraints: List<ColumnConstraint>,
    val listOptions: List<String>,
    override val rememberValue: Boolean,
): ColumnLike

data class SubmitRowResponse(
    val ok: Boolean,
    val msg: String,
    val errors: TableRowErrors? = null,
)
data class StoreRowResponse(
    val ok: Boolean,
    val exception: Exception?,
)

@Serializable
data class FullTableResponseDTO(
    val table: FullTableDTO
)

@Serializable
data class FullTableDTO(
    val id: String,
    val databaseName: String,
    val displayName: String,
    val displayDescription: String?,
    val columns: List<TableColumnDTO>,
) {
    fun toModel(): TableModel = TableModel(
        id = id,
        databaseName = databaseName,
        name = displayName,
        description = displayDescription,
        columns = columns.map { it.toModel() },
    )
}

@Serializable
data class TableColumnDTO(
    val id: String,
    val databaseName: String,
    val displayName: String,
    val width: Float,
    val type: ColumnType,
    val constraints: List<ColumnConstraint>,
    val listOptions: List<String> = emptyList(),
    val rememberValue: Boolean = false,
) {
    fun toModel(): ColumnModel = ColumnModel(
        id = id,
        dbName = databaseName,
        name = displayName,
        width = width,
        type = type,
        constraints = constraints,
        listOptions = listOptions,
        rememberValue = rememberValue,
    )
}

@Serializable
data class TableImageFilenameDTO(val filename: String)

@Serializable
data class TableImagePresignedURL(val data: PresignedUrlDTO)

@Serializable
data class PresignedUrlDTO(val presignedUrl: String, val objectUrl: String)