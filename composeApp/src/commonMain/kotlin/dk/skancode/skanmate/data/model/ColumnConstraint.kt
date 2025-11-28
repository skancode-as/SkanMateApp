package dk.skancode.skanmate.data.model

import dk.skancode.skanmate.util.InternalStringResource
import dk.skancode.skanmate.util.equal
import dk.skancode.skanmate.util.unreachable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SealedSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.jetbrains.compose.resources.StringResource
import skanmate.composeapp.generated.resources.Res
import skanmate.composeapp.generated.resources.column_type_boolean
import skanmate.composeapp.generated.resources.column_type_file
import skanmate.composeapp.generated.resources.column_type_list
import skanmate.composeapp.generated.resources.column_type_null
import skanmate.composeapp.generated.resources.column_type_numeric
import skanmate.composeapp.generated.resources.constraint_error_constant_value
import skanmate.composeapp.generated.resources.constraint_error_email
import skanmate.composeapp.generated.resources.constraint_error_ends_with
import skanmate.composeapp.generated.resources.constraint_error_invalid_type
import skanmate.composeapp.generated.resources.constraint_error_max_length
import skanmate.composeapp.generated.resources.constraint_error_min_length
import skanmate.composeapp.generated.resources.constraint_error_max_value
import skanmate.composeapp.generated.resources.constraint_error_min_value
import skanmate.composeapp.generated.resources.constraint_error_regex
import skanmate.composeapp.generated.resources.constraint_error_required
import skanmate.composeapp.generated.resources.constraint_error_starts_with
import kotlin.math.roundToInt

@OptIn(SealedSerializationApi::class)
private class ColumnConstraintValueSerialDescriptor(
    override val serialName: String,
    override val isInline: Boolean = true,
    override val isNullable: Boolean = true,
) : SerialDescriptor {
    override val kind: SerialKind
        get() = SerialKind.CONTEXTUAL
    override val elementsCount: Int
        get() = 0

    override fun getElementName(index: Int): String = error()
    override fun getElementIndex(name: String): Int = error()
    override fun getElementAnnotations(index: Int): List<Annotation> = error()
    override fun getElementDescriptor(index: Int): SerialDescriptor = error()
    override fun isElementOptional(index: Int): Boolean = error()
    private fun error(): Nothing =
        throw IllegalStateException("ColumnConstraintValueDescriptor does not have elements")
}

private object ColumnConstraintSerializer : KSerializer<ColumnConstraint> {
    private val nameDescriptor =
        PrimitiveSerialDescriptor("dk.skanmate.ColumnConstraint.name", PrimitiveKind.STRING)
    private val valueDescriptor =
        ColumnConstraintValueSerialDescriptor("dk.skanmate.ColumnConstraint.value")

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("dk.skanmate.ColumnConstraint") {
            element("name", nameDescriptor)
            element("value", valueDescriptor, isOptional = true)
        }

    override fun serialize(
        encoder: Encoder,
        value: ColumnConstraint
    ) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: error("ColumnConstraintSerializer can only be used with JSON")

        jsonEncoder.encodeJsonElement(
            buildJsonObject {
                put("name", value.name)
                when (value) {
                    is ColumnConstraint.MaxLength     -> put("value", JsonPrimitive(value.value))
                    is ColumnConstraint.MaxValue      -> put("value", JsonPrimitive(value.value))
                    is ColumnConstraint.MinLength     -> put("value", JsonPrimitive(value.value))
                    is ColumnConstraint.MinValue      -> put("value", JsonPrimitive(value.value))
                    is ColumnConstraint.Pattern       -> put("value", JsonPrimitive(value.value))
                    is ColumnConstraint.DefaultValue  -> put("value", JsonPrimitive(value.value))
                    is ColumnConstraint.ConstantValue -> put("value", JsonPrimitive(value.value))
                    is ColumnConstraint.Prefix        -> put("value", JsonPrimitive(value.value))
                    is ColumnConstraint.Suffix        -> put("value", JsonPrimitive(value.value))
                    is ColumnConstraint.StartsWith    -> put("value", JsonPrimitive(value.value))
                    is ColumnConstraint.EndsWith      -> put("value", JsonPrimitive(value.value))

                    ColumnConstraint.Email, ColumnConstraint.Required, ColumnConstraint.Unique -> {}
                }
            }
        )

    }

    override fun deserialize(decoder: Decoder): ColumnConstraint {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("ColumnConstraintSerializer can only be used with JSON")

        val obj = jsonDecoder.decodeJsonElement().jsonObject

        val name = obj["name"]?.jsonPrimitive?.contentOrNull
            ?: error("Missing 'name' in ColumnConstraint")
        val value = obj["value"]

        return when (name) {
            MinLengthConstraintName -> ColumnConstraint.MinLength(
                value?.jsonPrimitive?.floatOrNull?.roundToInt()
                    ?: error("Invalid value $value for name: $name")
            )

            MaxLengthConstraintName -> ColumnConstraint.MaxLength(
                value?.jsonPrimitive?.floatOrNull?.roundToInt()
                    ?: error("Invalid value $value for name: $name")
            )

            MinValueConstraintName -> ColumnConstraint.MinValue(
                value?.jsonPrimitive?.floatOrNull ?: error("Invalid value $value for name: $name")
            )

            MaxValueConstraintName -> ColumnConstraint.MaxValue(
                value?.jsonPrimitive?.floatOrNull ?: error("Invalid value $value for name: $name")
            )

            PatternConstraintName -> ColumnConstraint.Pattern(
                value?.jsonPrimitive?.contentOrNull ?: error("Invalid value $value for name: $name")
            )

            DefaultValueConstraintName -> ColumnConstraint.DefaultValue(
                value?.jsonPrimitive?.contentOrNull ?: error("Invalid value $value for name: $name")
            )

            ConstantValueConstraintName -> ColumnConstraint.ConstantValue(
                value?.jsonPrimitive?.contentOrNull ?: error("Invalid value $value for name: $name")
            )

            EmailConstraintName -> ColumnConstraint.Email
            RequiredConstraintName -> ColumnConstraint.Required
            UniqueConstraintName -> ColumnConstraint.Unique
            PrefixConstraintName -> ColumnConstraint.Prefix(
                value?.jsonPrimitive?.contentOrNull ?: error("Invalid value $value for name: $name")
            )

            SuffixConstraintName -> ColumnConstraint.Suffix(
                value?.jsonPrimitive?.contentOrNull ?: error("Invalid value $value for name: $name")
            )

            StartsWithConstraintName -> ColumnConstraint.StartsWith(
                value?.jsonPrimitive?.contentOrNull ?: error("Invalid value $value for name: $name")
            )

            EndsWithConstraintName -> ColumnConstraint.EndsWith(
                value?.jsonPrimitive?.contentOrNull ?: error("Invalid value $value for name: $name")
            )

            else -> unreachable("Unknown constraint name found: $name")
        }
    }
}

sealed class ConstraintCheckResult {
    data object Ok : ConstraintCheckResult()
    data class Error(
        override val resource: StringResource,
        override val args: List<Any> = emptyList()
    ) : ConstraintCheckResult(), InternalStringResource
}

@Serializable(with = ColumnConstraintSerializer::class)
sealed class ColumnConstraint(val name: String) {
    abstract fun check(value: ColumnValue): ConstraintCheckResult

    data class MinLength(val value: Int) : ColumnConstraint(MinLengthConstraintName) {
        override fun check(value: ColumnValue): ConstraintCheckResult {
            return when (value) {
                is ColumnValue.Text -> {
                    if (value.text.length >= this.value) {
                        ConstraintCheckResult.Ok
                    } else {
                        ConstraintCheckResult.Error(
                            resource = Res.string.constraint_error_min_length,
                            args = listOf(this.value)
                        )
                    }
                }

                else -> unreachable("Min length constraint is only allowed on Text value")
            }
        }
    }

    data class MaxLength(val value: Int) : ColumnConstraint(MaxLengthConstraintName) {
        override fun check(value: ColumnValue): ConstraintCheckResult {
            return when (value) {
                is ColumnValue.Text -> {
                    if (value.text.length <= this.value) {
                        ConstraintCheckResult.Ok
                    } else {
                        ConstraintCheckResult.Error(
                            resource = Res.string.constraint_error_max_length,
                            args = listOf(this.value)
                        )
                    }
                }

                else -> unreachable("Max length constraint is only allowed on Text value")
            }
        }
    }

    data class MinValue(val value: Float) : ColumnConstraint(MinValueConstraintName) {
        override fun check(value: ColumnValue): ConstraintCheckResult {
            return when (value) {
                is ColumnValue.Numeric -> {
                    if (value.num != null && value.num.toFloat() >= this.value) {
                        ConstraintCheckResult.Ok
                    } else {
                        ConstraintCheckResult.Error(
                            resource = Res.string.constraint_error_min_value,
                            args = listOf(this.value)
                        )
                    }
                }

                else -> unreachable("Min value constraint is only allowed on Text value")
            }
        }
    }

    data class MaxValue(val value: Float) : ColumnConstraint(MaxValueConstraintName) {
        override fun check(value: ColumnValue): ConstraintCheckResult {
            return when (value) {
                is ColumnValue.Numeric -> {
                    if (value.num != null && value.num.toFloat() <= this.value) {
                        ConstraintCheckResult.Ok
                    } else {
                        ConstraintCheckResult.Error(
                            resource = Res.string.constraint_error_max_value,
                            args = listOf(this.value)
                        )
                    }
                }

                else -> unreachable("Max value constraint is only allowed on Text value")
            }
        }
    }

    data class Pattern(val value: String) : ColumnConstraint(PatternConstraintName) {
        val regex = Regex(value)
        override fun check(value: ColumnValue): ConstraintCheckResult {
            return when (value) {
                is ColumnValue.Text -> {
                    if (regex.matches(value.text)) {
                        ConstraintCheckResult.Ok
                    } else {
                        ConstraintCheckResult.Error(
                            resource = Res.string.constraint_error_regex,
                            args = listOf(this.value)
                        )
                    }
                }

                else -> unreachable("Pattern constraint is only allowed on Text value")
            }
        }
    }

    data class DefaultValue(val value: String) : ColumnConstraint(DefaultValueConstraintName) {
        override fun check(value: ColumnValue): ConstraintCheckResult {
            return ConstraintCheckResult.Ok
        }
    }

    data class ConstantValue(val value: String) : ColumnConstraint(ConstantValueConstraintName) {
        override fun check(value: ColumnValue): ConstraintCheckResult {
            val ok = when (value) {
                is ColumnValue.Text -> value.text == this.value
                is ColumnValue.Numeric -> value.num != null && value.num equal this.value.toFloatOrNull()
                else -> return ConstraintCheckResult.Error(
                    resource = Res.string.constraint_error_invalid_type,
                    args = listOf(
                        when (value) {
                            is ColumnValue.File -> Res.string.column_type_file
                            is ColumnValue.OptionList -> Res.string.column_type_list
                            is ColumnValue.Boolean -> Res.string.column_type_boolean
                            is ColumnValue.Null -> Res.string.column_type_null

                            is ColumnValue.Numeric,
                            is ColumnValue.Text -> unreachable()
                        }
                    )
                )

            }
            return if (ok) {
                ConstraintCheckResult.Ok
            } else {
                ConstraintCheckResult.Error(
                    resource = Res.string.constraint_error_constant_value,
                    args = listOf(this.value)
                )
            }
        }
    }

    data object Email : ColumnConstraint(EmailConstraintName) {
        val regex = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        override fun check(value: ColumnValue): ConstraintCheckResult {
            return when (value) {
                is ColumnValue.Text -> {
                    if (regex.matches(value.text)) {
                        ConstraintCheckResult.Ok
                    } else {
                        ConstraintCheckResult.Error(
                            resource = Res.string.constraint_error_email,
                        )
                    }
                }

                else -> unreachable("Email constraint is only allowed on Text value")
            }
        }
    }

    data object Required : ColumnConstraint(RequiredConstraintName) {
        override fun check(value: ColumnValue): ConstraintCheckResult {
            val ok = when (value) {
                is ColumnValue.File -> !value.fileName.isNullOrBlank()
                is ColumnValue.Numeric -> value.num != null
                is ColumnValue.Text -> value.text.isNotBlank()
                is ColumnValue.OptionList -> !value.selected.isNullOrBlank() || value.options.contains(
                    value.selected
                )

                is ColumnValue.Boolean -> true
                ColumnValue.Null -> return ConstraintCheckResult.Error(
                    resource = Res.string.constraint_error_invalid_type,
                    args = listOf(Res.string.column_type_null)
                )
            }
            return if (ok) {
                ConstraintCheckResult.Ok
            } else {
                ConstraintCheckResult.Error(
                    resource = Res.string.constraint_error_required,
                )
            }
        }
    }

    data object Unique : ColumnConstraint(UniqueConstraintName) {
        override fun check(value: ColumnValue): ConstraintCheckResult {
            return ConstraintCheckResult.Ok
        }
    }

    data class Prefix(val value: String) : ColumnConstraint(PrefixConstraintName) {
        override fun check(value: ColumnValue): ConstraintCheckResult {
            return ConstraintCheckResult.Ok
        }
    }

    data class Suffix(val value: String) : ColumnConstraint(SuffixConstraintName) {
        override fun check(value: ColumnValue): ConstraintCheckResult {
            return ConstraintCheckResult.Ok
        }
    }

    data class StartsWith(val value: String) : ColumnConstraint(StartsWithConstraintName) {
        override fun check(value: ColumnValue): ConstraintCheckResult {
            val ok = when (value) {
                is ColumnValue.Text -> value.text.startsWith(prefix = this.value)
                is ColumnValue.File,
                is ColumnValue.Numeric,
                is ColumnValue.OptionList,
                is ColumnValue.Boolean,
                ColumnValue.Null -> return ConstraintCheckResult.Error(
                    resource = Res.string.constraint_error_invalid_type,
                    args = listOf(
                        Res.string.column_type_null,
                        Res.string.column_type_file,
                        Res.string.column_type_numeric,
                        Res.string.column_type_list,
                        Res.string.column_type_boolean
                    )
                )
            }
            return if (ok) {
                ConstraintCheckResult.Ok
            } else {
                ConstraintCheckResult.Error(
                    resource = Res.string.constraint_error_starts_with,
                    args = listOf(this.value),
                )
            }
        }
    }

    data class EndsWith(val value: String) : ColumnConstraint(EndsWithConstraintName) {
        override fun check(value: ColumnValue): ConstraintCheckResult {
            val ok = when (value) {
                is ColumnValue.Text -> value.text.endsWith(suffix = this.value)
                is ColumnValue.File,
                is ColumnValue.Numeric,
                is ColumnValue.OptionList,
                is ColumnValue.Boolean,
                ColumnValue.Null -> return ConstraintCheckResult.Error(
                    resource = Res.string.constraint_error_invalid_type,
                    args = listOf(
                        Res.string.column_type_null,
                        Res.string.column_type_file,
                        Res.string.column_type_numeric,
                        Res.string.column_type_list,
                        Res.string.column_type_boolean
                    )
                )
            }
            return if (ok) {
                ConstraintCheckResult.Ok
            } else {
                ConstraintCheckResult.Error(
                    resource = Res.string.constraint_error_ends_with,
                    args = listOf(this.value),
                )
            }
        }
    }
}

const val MinLengthConstraintName = "minLength"
const val MaxLengthConstraintName = "maxLength"
const val MinValueConstraintName = "minValue"
const val MaxValueConstraintName = "maxValue"
const val DefaultValueConstraintName = "defaultValue"
const val ConstantValueConstraintName = "constantValue"
const val PatternConstraintName = "pattern"
const val EmailConstraintName = "email"
const val RequiredConstraintName = "required"
const val UniqueConstraintName = "unique"
const val PrefixConstraintName = "prefix"
const val SuffixConstraintName = "suffix"
const val StartsWithConstraintName = "startsWith"
const val EndsWithConstraintName = "endsWith"

fun List<ColumnConstraint>.check(value: ColumnValue): List<ConstraintCheckResult> {
    return this.map { v -> v.check(value) }
}
