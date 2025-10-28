package dk.skancode.skanmate.data.model

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
import kotlin.math.roundToInt

@OptIn(SealedSerializationApi::class)
private class ColumnValidationValueSerialDescriptor(
    override val serialName: String,
    override val isInline: Boolean = true,
    override val isNullable: Boolean = true,
): SerialDescriptor {
    override val kind: SerialKind
        get() = SerialKind.CONTEXTUAL
    override val elementsCount: Int
        get() = 0

    override fun getElementName(index: Int): String = error()
    override fun getElementIndex(name: String): Int = error()
    override fun getElementAnnotations(index: Int): List<Annotation> = error()
    override fun getElementDescriptor(index: Int): SerialDescriptor = error()
    override fun isElementOptional(index: Int): Boolean = error()
    private fun error(): Nothing = throw IllegalStateException("ColumnValueDescriptor does not have elements")
}

private object ColumnValidationSerializer: KSerializer<ColumnValidation> {
    private val nameDescriptor = PrimitiveSerialDescriptor("dk.skanmate.ColumnValidation.name", PrimitiveKind.STRING)
    private val valueDescriptor = ColumnValidationValueSerialDescriptor("dk.skanmate.ColumnValidation.value")

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("dk.skanmate.ColumnValidation") {
        element("name", nameDescriptor)
        element("value", valueDescriptor, isOptional = true)
    }

    override fun serialize(
        encoder: Encoder,
        value: ColumnValidation
    ) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: error("ColumnValidationSerializer can only be used with JSON")

        jsonEncoder.encodeJsonElement(
            buildJsonObject {
                put("name", value.name)
                when(value) {
                    is ColumnValidation.MaxLength     -> put("value", JsonPrimitive(value.value))
                    is ColumnValidation.MaxValue      -> put("value", JsonPrimitive(value.value))
                    is ColumnValidation.MinLength     -> put("value", JsonPrimitive(value.value))
                    is ColumnValidation.MinValue      -> put("value", JsonPrimitive(value.value))
                    is ColumnValidation.Pattern       -> put("value", JsonPrimitive(value.value))
                    is ColumnValidation.DefaultValue  -> put("value", JsonPrimitive(value.value))
                    is ColumnValidation.ConstantValue -> put("value", JsonPrimitive(value.value))

                    ColumnValidation.Email, ColumnValidation.Required, ColumnValidation.Unique -> {}
                }
            }
        )

    }

    override fun deserialize(decoder: Decoder): ColumnValidation {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("ColumnValidationSerializer can only be used with JSON")

        val obj = jsonDecoder.decodeJsonElement().jsonObject

        val name = obj["name"]?.jsonPrimitive?.contentOrNull
            ?: error("Missing 'name' in ColumnValidation")
        val value = obj["value"]

        return when (name) {
            MinLengthValidationName -> ColumnValidation.MinLength(value?.jsonPrimitive?.floatOrNull?.roundToInt() ?: error("Invalid value $value for name: $name"))
            MaxLengthValidationName -> ColumnValidation.MaxLength(value?.jsonPrimitive?.floatOrNull?.roundToInt() ?: error("Invalid value $value for name: $name"))
            MinValueValidationName -> ColumnValidation.MinValue(value?.jsonPrimitive?.floatOrNull ?: error("Invalid value $value for name: $name"))
            MaxValueValidationName -> ColumnValidation.MaxValue(value?.jsonPrimitive?.floatOrNull ?: error("Invalid value $value for name: $name"))
            PatternValidationName -> ColumnValidation.Pattern(value?.jsonPrimitive?.contentOrNull ?: error("Invalid value $value for name: $name"))
            DefaultValueValidationName -> ColumnValidation.DefaultValue(value?.jsonPrimitive?.contentOrNull ?: error("Invalid value $value for name: $name"))
            ConstantValueValidationName -> ColumnValidation.ConstantValue(value?.jsonPrimitive?.contentOrNull ?: error("Invalid value $value for name: $name"))
            EmailValidationName -> ColumnValidation.Email
            RequiredValidationName -> ColumnValidation.Required
            UniqueValidationName -> ColumnValidation.Unique
            else -> unreachable("Unknown validation name found: $name")
        }
    }
}

@Serializable(with = ColumnValidationSerializer::class)
sealed class ColumnValidation(val name: String) {
    abstract fun validate(value: ColumnValue): Boolean

    data class MinLength(val value: Int): ColumnValidation(MinLengthValidationName) {
        override fun validate(value: ColumnValue): Boolean {
            return when (value) {
                is ColumnValue.Text -> {
                    value.text.length >= this.value
                }
                else -> unreachable("Min length validation is only allowed on Text value")
            }
        }
    }

    data class MaxLength(val value: Int): ColumnValidation(MaxLengthValidationName) {
        override fun validate(value: ColumnValue): Boolean {
            return when (value) {
                is ColumnValue.Text -> {
                    value.text.length <= this.value
                }
                else -> unreachable("Max length validation is only allowed on Text value")
            }
        }
    }
    data class MinValue(val value: Float): ColumnValidation(MinValueValidationName) {
        override fun validate(value: ColumnValue): Boolean {
            return when (value) {
                is ColumnValue.Numeric -> {
                    value.num != null && value.num.toFloat() >= this.value
                }
                else -> unreachable("Min value validation is only allowed on Text value")
            }
        }
    }

    data class MaxValue(val value: Float): ColumnValidation(MaxValueValidationName) {
        override fun validate(value: ColumnValue): Boolean {
            return when (value) {
                is ColumnValue.Numeric -> {
                    value.num != null && value.num.toFloat() <= this.value
                }
                else -> unreachable("Max value validation is only allowed on Text value")
            }
        }
    }

    data class Pattern(val value: String): ColumnValidation(PatternValidationName) {
        val regex = Regex(value)
        override fun validate(value: ColumnValue): Boolean {
            return when(value) {
                is ColumnValue.Text -> regex.matches(value.text)
                else -> unreachable("Pattern validation is only allowed on Text value")
            }
        }
    }

    data class DefaultValue(val value: String): ColumnValidation(DefaultValueValidationName) {
        override fun validate(value: ColumnValue): Boolean {
            return true
        }
    }

    data class ConstantValue(val value: String): ColumnValidation(ConstantValueValidationName) {
        override fun validate(value: ColumnValue): Boolean {
            return when (value) {
                is ColumnValue.Text -> value.text == this.value
                is ColumnValue.Numeric -> value.num != null && value.num == this.value.toFloatOrNull()
                else -> false
            }
        }
    }

    data object Email: ColumnValidation(EmailValidationName) {
        val regex = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        override fun validate(value: ColumnValue): Boolean {
            return when(value) {
                is ColumnValue.Text -> regex.matches(value.text)
                else -> unreachable("Email validation is only allowed on Text value")
            }
        }
    }

    data object Required: ColumnValidation(RequiredValidationName) {
        override fun validate(value: ColumnValue): Boolean {
            return when(value) {
                is ColumnValue.File -> !value.fileName.isNullOrBlank()
                is ColumnValue.Numeric -> value.num != null
                is ColumnValue.Text -> value.text.isNotBlank()
                is ColumnValue.Boolean -> true
                ColumnValue.Null -> false
            }
        }
    }

    data object Unique: ColumnValidation(UniqueValidationName) {
        override fun validate(value: ColumnValue): Boolean {
            TODO("Not yet implemented")
        }
    }
}

const val MinLengthValidationName     = "minLength"
const val MaxLengthValidationName     = "maxLength"
const val MinValueValidationName      = "minValue"
const val MaxValueValidationName      = "maxValue"
const val DefaultValueValidationName  = "defaultValue"
const val ConstantValueValidationName = "constantValue"
const val PatternValidationName       = "pattern"
const val EmailValidationName         = "email"
const val RequiredValidationName      = "required"
const val UniqueValidationName        = "unique"


fun List<ColumnValidation>.validate(value: ColumnValue): Boolean {
    return this.all { v -> v.validate(value) }
}