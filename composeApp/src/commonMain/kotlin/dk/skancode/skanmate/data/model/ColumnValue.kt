package dk.skancode.skanmate.data.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SealedSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@OptIn(SealedSerializationApi::class)
internal class ColumnValueSerialDescriptor() : SerialDescriptor {
    override val serialName: String
        get() = ColumnValue::class.qualifiedName ?: "dk.skancode.ColumnValue"
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
        throw IllegalStateException("ColumnValueDescriptor does not have elements")
}

internal object ColumnValueSerializer : KSerializer<ColumnValue> {
    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = ColumnValueSerialDescriptor()

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(
        encoder: Encoder,
        value: ColumnValue
    ) {
        when (value) {
            is ColumnValue.Boolean -> encoder.encodeBoolean(value.checked)
            ColumnValue.Null -> encoder.encodeNull()
            is ColumnValue.Numeric -> if (value.num != null) encoder.encodeDouble(value.num.toDouble()) else encoder.encodeNull()
            is ColumnValue.Text -> encoder.encodeString(value.text)
            is ColumnValue.File -> if (value.objectUrl != null) encoder.encodeString(value.objectUrl) else if (value.localUrl != null) encoder.encodeString(value.localUrl) else encoder.encodeNull()
            is ColumnValue.OptionList -> if (value.selected != null) encoder.encodeString(value.selected) else encoder.encodeNull()
        }
    }

    override fun deserialize(decoder: Decoder): ColumnValue {
        return ColumnValue.Null
    }
}

@Serializable(with = ColumnValueSerializer::class)
sealed class ColumnValue {
    override fun toString(): String {
        return when (this) {
            is Boolean -> "Boolean($checked)"
            is File -> "File()"
            Null -> "Null"
            is Numeric -> "Numeric($num)"
            is OptionList -> "OptionsList($selected, $options)"
            is Text -> "Text($text)"
        }
    }

    data class Boolean(val checked: kotlin.Boolean = false) : ColumnValue() {
        override fun clone(): ColumnValue = this.copy()
        override fun isEmpty(): kotlin.Boolean {
            return false
        }

        override fun equals(other: Any?): kotlin.Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Boolean

            return checked == other.checked
        }

        override fun hashCode(): Int {
            return checked.hashCode()
        }
    }

    data class Text(val text: String = "") : ColumnValue() {
        override fun clone(): ColumnValue = this.copy()
        override fun isEmpty(): kotlin.Boolean {
            return text.isEmpty()
        }

        override fun equals(other: Any?): kotlin.Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Text

            return text == other.text
        }

        override fun hashCode(): Int {
            return text.hashCode()
        }
    }

    data class Numeric(val num: Number? = null) : ColumnValue() {
        override fun clone(): ColumnValue = this.copy()
        override fun isEmpty(): kotlin.Boolean {
            return num == null
        }

        override fun equals(other: Any?): kotlin.Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Numeric

            return num == other.num
        }

        override fun hashCode(): Int {
            return num?.hashCode() ?: 0
        }

    }

    data class File(
        val fileName: String? = null,
        val localUrl: String? = null,
        val objectUrl: String? = null,
        val bytes: ByteArray? = null,
        val isUploaded: kotlin.Boolean = false,
    ) : ColumnValue() {
        override fun clone(): ColumnValue = this.copy()
        override fun isEmpty(): kotlin.Boolean {
            return fileName == null
        }

        override fun equals(other: Any?): kotlin.Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as File

            if (fileName != other.fileName) return false
            if (localUrl != other.localUrl) return false
            if (objectUrl != other.objectUrl) return false
            if (!bytes.contentEquals(other.bytes)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = fileName?.hashCode() ?: 0
            result = 31 * result + (localUrl?.hashCode() ?: 0)
            result = 31 * result + (objectUrl?.hashCode() ?: 0)
            result = 31 * result + (bytes?.contentHashCode() ?: 0)
            return result
        }
    }

    data class OptionList(val options: List<String>, val selected: String? = null) : ColumnValue() {
        override fun clone(): ColumnValue = this.copy()
        override fun isEmpty(): kotlin.Boolean {
            return selected == null
        }

        override fun equals(other: Any?): kotlin.Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as OptionList

            return options == other.options && selected == other.selected
        }

        override fun hashCode(): Int {
            var result = options.hashCode()
            result = 31 * result + (selected?.hashCode() ?: 0)
            return result
        }
    }

    data object Null : ColumnValue() {
        override fun clone(): ColumnValue = this
        override fun isEmpty(): kotlin.Boolean = true
    }

    abstract fun clone(): ColumnValue
    abstract fun isEmpty(): kotlin.Boolean
    fun isNotEmpty(): kotlin.Boolean = !isEmpty()

    companion object {
        fun fromType(t: ColumnType, options: List<String>): ColumnValue = when (t) {
            ColumnType.Boolean -> Boolean()
            ColumnType.Numeric -> Numeric()
            ColumnType.Text -> Text()
            ColumnType.Timestamp -> Text()
            ColumnType.User -> Null
            ColumnType.Unknown -> Null
            ColumnType.Id -> Null
            ColumnType.File -> File()
            ColumnType.List -> OptionList(options = options)
        }
    }
}
