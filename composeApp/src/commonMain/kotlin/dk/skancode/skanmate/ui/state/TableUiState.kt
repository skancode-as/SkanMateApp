package dk.skancode.skanmate.ui.state

import dk.skancode.skanmate.data.model.ColumnModel
import dk.skancode.skanmate.data.model.ColumnType
import dk.skancode.skanmate.data.model.ColumnValidation
import dk.skancode.skanmate.data.model.ColumnValue
import dk.skancode.skanmate.data.model.TableModel
import dk.skancode.skanmate.data.model.ValidationResult
import dk.skancode.skanmate.data.model.validateWithErrors
import dk.skancode.skanmate.util.InternalStringResource
import dk.skancode.skanmate.util.reduceDefault

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
    val validationErrors: Map<String, List<InternalStringResource>>
}

data class MutableTableUiState(
    override val isFetching: Boolean = false,
    override val isSubmitting: Boolean = false,
    override val model: TableModel? = null,
    override val columns: List<ColumnUiState> = emptyList(),
    override val focusedColumnId: String? = null,
    override val status: FetchStatus = FetchStatus.Undetermined,
    override val validationErrors: Map<String, List<InternalStringResource>> = emptyMap()
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
    id = id,
    name = name,
    dbName = dbName,
    value = ColumnValue.fromType(type),
    type = type,
    width = width,
    validations = validations,
).let { col ->
    if (col.validations.any { v -> v is ColumnValidation.ConstantValue }) {
        val v = (col.validations.first { v -> v is ColumnValidation.ConstantValue } as ColumnValidation.ConstantValue).value
        col.copy(
            value = when(col.value) {
                is ColumnValue.Text -> col.value.copy(v)
                is ColumnValue.Numeric -> col.value.copy(v.toIntOrNull() ?: v.toFloatOrNull())
                else -> col.value
            }
        )
    } else if (col.validations.any { v -> v is ColumnValidation.DefaultValue }) {
        val v = (col.validations.first { v -> v is ColumnValidation.DefaultValue } as ColumnValidation.DefaultValue).value
        col.copy(
            value = when(col.value) {
                is ColumnValue.Text -> col.value.copy(v)
                is ColumnValue.Numeric -> col.value.copy(v.toFloatOrNull())
                else -> col.value
            }
        )
    } else {
        col
    }
}

data class ColumnUiState(
    val id: String,
    val name: String,
    val dbName: String,
    val value: ColumnValue,
    val type: ColumnType,
    val width: Float,
    val validations: List<ColumnValidation>
)

data class ColumnValidationResult(val displayName: String, val ok: Boolean, val errors: List<InternalStringResource>)
fun ColumnUiState.validate(): ColumnValidationResult {
    val results = validations.validateWithErrors(value)

    return results.reduceDefault(
        default = ColumnValidationResult(
            displayName = name,
            ok = true,
            errors = emptyList(),
        )
    ) { acc, cur ->
        acc.copy(
            ok = acc.ok && cur == ValidationResult.Ok,
            errors = listOfNotNull(
                *acc.errors.toTypedArray(),
                cur as? ValidationResult.Error
            )
        )
    }
}