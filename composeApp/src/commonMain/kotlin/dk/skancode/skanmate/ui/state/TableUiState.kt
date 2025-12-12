package dk.skancode.skanmate.ui.state

import dk.skancode.skanmate.data.model.ColumnModel
import dk.skancode.skanmate.data.model.ColumnType
import dk.skancode.skanmate.data.model.ColumnConstraint
import dk.skancode.skanmate.data.model.ColumnLike
import dk.skancode.skanmate.data.model.ColumnValue
import dk.skancode.skanmate.data.model.TableModel
import dk.skancode.skanmate.data.model.ConstraintCheckResult
import dk.skancode.skanmate.data.model.check
import dk.skancode.skanmate.data.model.isAvailableOffline
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
    val displayColumns: List<ColumnUiState>
    val focusedColumnId: String?
    val hasFocusedColumn: Boolean
    val status: FetchStatus
    val constraintErrors: Map<String, List<InternalStringResource>>
    val scannedBarcodes: List<String>
    val isAvailableOffline: Boolean
}

data class MutableTableUiState(
    override val isFetching: Boolean = false,
    override val isSubmitting: Boolean = false,
    override val model: TableModel? = null,
    override val columns: List<ColumnUiState> = emptyList(),
    override val focusedColumnId: String? = columns.firstOrNull()?.id,
    override val status: FetchStatus = FetchStatus.Undetermined,
    override val constraintErrors: Map<String, List<InternalStringResource>> = emptyMap(),
    override val scannedBarcodes: List<String> = emptyList(),
) : TableUiState {
    override val hasFocusedColumn: Boolean
        get() = focusedColumnId != null && columns.any { col -> col.id == focusedColumnId }
    override val displayColumns: List<ColumnUiState>
        get() = columns.filter { col -> col.isDisplayColumn() }

    override val isAvailableOffline: Boolean
        get() = columns.isAvailableOffline()
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
    rememberValue = rememberValue,
).let { col ->
    if (col.hasConstantValue) {
        val v = col.constantValue
        col.copy(
            value = when(col.value) {
                is ColumnValue.Text -> col.value.copy(text = v)
                is ColumnValue.Numeric -> col.value.copy(num = v.toIntOrNull() ?: v.toFloatOrNull())
                else -> col.value
            }
        )
    } else if (col.hasDefaultValue) {
        val v = col.defaultValue
        col.copy(
            value = when(col.value) {
                is ColumnValue.Text -> col.value.copy(text = v)
                is ColumnValue.Numeric -> col.value.copy(num = v.toFloatOrNull())
                else -> col.value
            }
        )
    } else {
        col
    }
}

data class ColumnUiState(
    val id: String,
    override val name: String,
    override val dbName: String,
    val value: ColumnValue,
    override val type: ColumnType,
    override val width: Float,
    override val constraints: List<ColumnConstraint>,
    override val rememberValue: Boolean,
): ColumnLike {
    val hasConstantValue: Boolean
        get() = hasConstraint<ColumnConstraint.ConstantValue>()
    val constantValue: String
        get() = constraint<ColumnConstraint.ConstantValue>().value

    val hasDefaultValue: Boolean
        get() = hasConstraint<ColumnConstraint.DefaultValue>()
    val defaultValue: String
        get() = constraint<ColumnConstraint.DefaultValue>().value

    val hasPrefix: Boolean
        get() = hasConstraint<ColumnConstraint.Prefix>()
    val prefix: String
        get() = constraint<ColumnConstraint.Prefix>().value

    val hasSuffix: Boolean
        get() = hasConstraint<ColumnConstraint.Suffix>()
    val suffix: String
        get() = constraint<ColumnConstraint.Suffix>().value

    inline fun <reified T: ColumnConstraint>hasConstraint(): Boolean = constraints.any { v -> v is T }
    inline fun <reified T: ColumnConstraint>constraint(): T = constraints.first { v -> v is T } as T
}

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

fun ColumnUiState.isDisplayColumn(): Boolean {
    val col = this
    return !col.type.autogenerated && !col.hasConstantValue
}

inline fun ColumnUiState.prepare(
    uploadImage: (fileName: String, bytes: ByteArray) -> String,
    queueImageDeletion: (localFileUrl: String) -> Unit,
): ColumnUiState =
    when (value) {
        is ColumnValue.File if value.fileName != null && value.bytes != null && !value.isUploaded -> {
            val fileName = value.fileName
            val bytes = value.bytes

            val objectUrl = uploadImage(
                fileName,
                bytes
            )
            if (value.localUrl != null) queueImageDeletion(value.localUrl)

            copy(
                value = value.copy(objectUrl = objectUrl, isUploaded = true)
            )
        }

        is ColumnValue.File if !value.isUploaded -> {
            copy(
                value = value.copy(objectUrl = "")
            )
        }

        is ColumnValue.File if value.isUploaded -> {
            if (value.localUrl != null) {
                queueImageDeletion(
                    value.localUrl,
                )
            }
            this
        }

        is ColumnValue.Text if hasPrefix -> {
            copy(
                value = value.copy(text = this.prefix + value.text)
            )
        }
        is ColumnValue.Text if hasSuffix -> {
            copy(
                value = value.copy(text = value.text + this.suffix)
            )
        }

        else -> this
    }

fun ColumnUiState.prepareLocal(username: String): ColumnUiState =
    when {
        value is ColumnValue.Text && hasPrefix -> {
            copy(
                value = value.copy(text = this.prefix + value.text)
            )
        }
        value is ColumnValue.Text && hasSuffix -> {
            copy(
                value = value.copy(text = value.text + this.suffix)
            )
        }
        type is ColumnType.User -> {
            copy(
                value = ColumnValue.Text(text = username)
            )
        }

        else -> this
    }
