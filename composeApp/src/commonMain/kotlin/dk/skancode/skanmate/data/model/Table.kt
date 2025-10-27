package dk.skancode.skanmate.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TableSummaryModel(
    val id: String,
    val name: String,
    val description: String?,
)

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
    val dbName: String,
    val name: String,
    val width: Float,
    val type: ColumnType,
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
) {
    fun toModel(): ColumnModel = ColumnModel(
        id = id,
        dbName = databaseName,
        name = displayName,
        width = width,
        type = type,
    )
}

@Serializable
data class TableImageFilenameDTO(val filename: String)

@Serializable
data class TableImagePresignedURL(val data: PresignedUrlDTO)

@Serializable
data class PresignedUrlDTO(val presignedUrl: String, val objectUrl: String)