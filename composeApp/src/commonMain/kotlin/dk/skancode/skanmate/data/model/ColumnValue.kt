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
private class ColumnValueSerialDescriptor(): SerialDescriptor {
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
    private fun error(): Nothing = throw IllegalStateException("ColumnValueDescriptor does not have elements")
}

private class ColumnValueSerializer(): KSerializer<ColumnValue> {
    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = ColumnValueSerialDescriptor()

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(
        encoder: Encoder,
        value: ColumnValue
    ) {
        when(value) {
            is ColumnValue.Boolean -> encoder.encodeBoolean(value.checked)
            ColumnValue.Null -> encoder.encodeNull()
            is ColumnValue.Numeric -> if(value.num != null) encoder.encodeDouble(value.num.toDouble()) else encoder.encodeNull()
            is ColumnValue.Text -> encoder.encodeString(value.text)
            is ColumnValue.File -> if(value.objectUrl != null) encoder.encodeString(value.objectUrl) else encoder.encodeNull()
        }
    }

    override fun deserialize(decoder: Decoder): ColumnValue {
        return ColumnValue.Null
    }
}

@Serializable(with = ColumnValueSerializer::class)
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
    data class File(val localUrl: String? = null, val objectUrl: String? = null) : ColumnValue() {
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
            ColumnType.User -> Null
            ColumnType.Unknown -> Null
            ColumnType.Id -> Null
            ColumnType.File -> File()
        }
    }
}
