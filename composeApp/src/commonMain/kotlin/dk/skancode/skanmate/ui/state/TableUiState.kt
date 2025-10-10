package dk.skancode.skanmate.ui.state

import dk.skancode.skanmate.data.model.ColumnModel
import dk.skancode.skanmate.data.model.ColumnType
import dk.skancode.skanmate.data.model.TableModel
import io.ktor.util.date.GMTDate

sealed class FetchStatus {
    data object Undetermined : FetchStatus()
    data object Success : FetchStatus()
    data object NotFound : FetchStatus()
}

interface TableUiState {
    val isFetching: Boolean
    val model: TableModel?
    val columns: List<ColumnUiState>
    val status: FetchStatus
}

data class MutableTableUiState(
    override val isFetching: Boolean = false,
    override val model: TableModel? = null,
    override val columns: List<ColumnUiState> = emptyList(),
    override val status: FetchStatus = FetchStatus.Undetermined,
) : TableUiState

fun TableModel?.toUiState(isFetching: Boolean): MutableTableUiState =
    MutableTableUiState(
        isFetching = isFetching,
        model = this,
        columns = this?.columns?.map {
            ColumnUiState(
                it.id,
                it.name,
                ColumnValue.fromType(it.type),
                it.type,
                it.width,
            )
        } ?: emptyList(),
        status = if (this == null) FetchStatus.NotFound else FetchStatus.Success
    )

data class ColumnUiState(
    val id: String,
    val name: String,
    val value: ColumnValue,
    val type: ColumnType,
    val width: Float,
)

sealed class ColumnValue {
    data class Boolean(val checked: kotlin.Boolean = false) : ColumnValue()
    data class Text(val text: String = "") : ColumnValue()
    data class Numeric(val num: Number? = null) : ColumnValue()
    data object Null : ColumnValue()

    companion object {
        fun fromType(t: ColumnType): ColumnValue = when (t) {
            ColumnType.Boolean -> Boolean()
            ColumnType.Id -> Null
            ColumnType.Numeric -> Numeric()
            ColumnType.Text -> Text()
            ColumnType.Timestamp -> Text()
            ColumnType.Unknown -> Null
            ColumnType.User -> Text()
        }
    }
}