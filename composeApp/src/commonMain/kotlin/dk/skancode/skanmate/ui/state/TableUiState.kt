package dk.skancode.skanmate.ui.state

import dk.skancode.skanmate.data.model.ColumnModel
import dk.skancode.skanmate.data.model.ColumnType
import dk.skancode.skanmate.data.model.TableModel

sealed class FetchStatus {
    data object Undetermined : FetchStatus()
    data object Success : FetchStatus()
    data object NotFound : FetchStatus()
}

interface TableUiState {
    val isFetching: Boolean
    val isSubmitting: Boolean
    val model: TableModel?
    val columns: List<ColumnUiState>
    val focusedColumnId: String?
    val status: FetchStatus
}

data class MutableTableUiState(
    override val isFetching: Boolean = false,
    override val isSubmitting: Boolean = false,
    override val model: TableModel? = null,
    override val columns: List<ColumnUiState> = emptyList(),
    override val focusedColumnId: String? = null,
    override val status: FetchStatus = FetchStatus.Undetermined,
) : TableUiState

fun TableModel?.toUiState(isFetching: Boolean): MutableTableUiState =
    MutableTableUiState(
        isFetching = isFetching,
        isSubmitting = false,
        model = this,
        columns = this?.columns?.map {
            it.toUiState()
        } ?: emptyList(),
        focusedColumnId = null,
        status = if (this == null) FetchStatus.NotFound else FetchStatus.Success
    )

fun ColumnModel.toUiState(): ColumnUiState = ColumnUiState(
    id,
    name,
    ColumnValue.fromType(type),
    type,
    width,
)

data class ColumnUiState(
    val id: String,
    val name: String,
    val value: ColumnValue,
    val type: ColumnType,
    val width: Float,
)

sealed class ColumnValue {
    data class Boolean(val checked: kotlin.Boolean = false) : ColumnValue() {
        override fun clone(): ColumnValue = this.copy()
    }
    data class Text(val text: String = "") : ColumnValue() {
        override fun clone(): ColumnValue = this.copy()
    }
    data class Numeric(val num: Number? = null) : ColumnValue() {
        override fun clone(): ColumnValue = this.copy()
    }

    data object Null : ColumnValue() {
        override fun clone(): ColumnValue = this
    }

    abstract fun clone(): ColumnValue

    companion object {
        fun fromType(t: ColumnType): ColumnValue = when (t) {
            ColumnType.Boolean -> Boolean()
            ColumnType.Numeric -> Numeric()
            ColumnType.Text -> Text()
            ColumnType.Timestamp -> Text()
            ColumnType.User -> Text()
            ColumnType.Unknown -> Null
            ColumnType.Id -> Null
        }
    }
}