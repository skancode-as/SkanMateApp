package dk.skancode.skanmate.ui.state

import dk.skancode.skanmate.data.model.ColumnModel
import dk.skancode.skanmate.data.model.ColumnType
import dk.skancode.skanmate.data.model.ColumnConstraint
import dk.skancode.skanmate.data.model.ColumnValue
import dk.skancode.skanmate.data.model.TableModel
import dk.skancode.skanmate.data.model.ConstraintCheckResult
import dk.skancode.skanmate.data.model.check
import dk.skancode.skanmate.util.InternalStringResource
import dk.skancode.skanmate.util.reduceDefault
import skanmate.composeapp.generated.resources.Res
import skanmate.composeapp.generated.resources.constraint_error_invalid_option

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
    val hasFocusedColumn: Boolean
    val status: FetchStatus
    val constraintErrors: Map<String, List<InternalStringResource>>
}

data class MutableTableUiState(
    override val isFetching: Boolean = false,
    override val isSubmitting: Boolean = false,
    override val model: TableModel? = null,
    override val columns: List<ColumnUiState> = emptyList(),
    override val focusedColumnId: String? = columns.firstOrNull()?.id,
    override val status: FetchStatus = FetchStatus.Undetermined,
    override val constraintErrors: Map<String, List<InternalStringResource>> = emptyMap()
) : TableUiState {
    override val hasFocusedColumn: Boolean
        get() = focusedColumnId != null && columns.any { col -> col.id == focusedColumnId }
}

fun TableModel?.toUiState(isFetching: Boolean): MutableTableUiState =
    MutableTableUiState(
        isFetching = isFetching,
        isSubmitting = false,
        model = this,
        columns = this?.columns?.map {
            it.toUiState()
        } ?: emptyList(),
        status = if (this == null) FetchStatus.NotFound else FetchStatus.Success
    )

fun ColumnModel.toUiState(): ColumnUiState = ColumnUiState(
    id = id,
    name = name,
    dbName = dbName,
    value = ColumnValue.fromType(type, listOptions),
    type = type,
    width = width,
    constraints = constraints,
).let { col ->
    if (col.constraints.any { v -> v is ColumnConstraint.ConstantValue }) {
        val v = (col.constraints.first { v -> v is ColumnConstraint.ConstantValue } as ColumnConstraint.ConstantValue).value
        col.copy(
            value = when(col.value) {
                is ColumnValue.Text -> col.value.copy(v)
                is ColumnValue.Numeric -> col.value.copy(v.toIntOrNull() ?: v.toFloatOrNull())
                else -> col.value
            }
        )
    } else if (col.constraints.any { v -> v is ColumnConstraint.DefaultValue }) {
        val v = (col.constraints.first { v -> v is ColumnConstraint.DefaultValue } as ColumnConstraint.DefaultValue).value
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
    val constraints: List<ColumnConstraint>
)

fun columnCheckError(displayName: String, vararg errors: InternalStringResource): ColumnCheckResult {
    return ColumnCheckResult(
        displayName = displayName,
        ok = false,
        errors = listOf(*errors),
    )
}

data class ColumnCheckResult(val displayName: String, val ok: Boolean, val errors: List<InternalStringResource>)
fun ColumnUiState.check(): ColumnCheckResult {
    if (value is ColumnValue.OptionList && !value.selected.isNullOrBlank() && !value.options.contains(value.selected)) {
        return columnCheckError(
            displayName = name,
            InternalStringResource(
                resource = Res.string.constraint_error_invalid_option,
            )
        )
    }

    val results = constraints.check(value)

    return results.reduceDefault(
        default = ColumnCheckResult(
            displayName = name,
            ok = true,
            errors = emptyList(),
        )
    ) { acc, cur ->
        acc.copy(
            ok = acc.ok && cur == ConstraintCheckResult.Ok,
            errors = listOfNotNull(
                *acc.errors.toTypedArray(),
                cur as? ConstraintCheckResult.Error
            )
        )
    }
}