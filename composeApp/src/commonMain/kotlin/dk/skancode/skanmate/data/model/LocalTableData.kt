@file:Suppress("SERIALIZER_TYPE_INCOMPATIBLE")

package dk.skancode.skanmate.data.model

import dk.skancode.skanmate.ui.state.ColumnUiState
import dk.skancode.skanmate.util.currentDateTimeUTC
import dk.skancode.skanmate.util.formatISO
import dk.skancode.skanmate.util.jsonSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer

@Serializable
data class LocalTableData(
    val model: TableModel,
    val rows: List<LocalRowData>,
)

@Serializable(with = MutableLocalRowDataSerializer::class)
interface LocalRowData: Map<String, LocalColumnValue> {
    val localRowId: Long
    val cols: Map<String, LocalColumnValue>
}

@Serializable(with = MutableLocalRowDataSerializer::class)
data class MutableLocalRowData(
    override var localRowId: Long = -1,
    override val cols: Map<String, LocalColumnValue>
): LocalRowData {

    override val size: Int
        get() = cols.size
    override val keys: Set<String>
        get() = cols.keys
    override val values: Collection<LocalColumnValue>
        get() = cols.values
    override val entries: Set<Map.Entry<String, LocalColumnValue>>
        get() = cols.entries

    override fun isEmpty(): Boolean = cols.isEmpty()

    override fun containsKey(key: String): Boolean = cols.isNotEmpty()

    override fun containsValue(value: LocalColumnValue): Boolean = cols.containsValue(value)

    override operator fun get(key: String): LocalColumnValue? = cols[key]
}

@Serializable(with = LocalColumnValueSerializer::class)
data class LocalColumnValue(
    val id: String,
    val dbName: String,
    val name: String,
    val type: ColumnType,
    val value: ColumnValue,
    val constraints: List<ColumnConstraint>,
    val listOptions: List<String>,
)

fun localRowDataOf(data: List<ColumnUiState>): LocalRowData {
    return MutableLocalRowData(
        cols = mapOf(
            *(data.map { col ->
                val columnValue = when (col.type) {
                    ColumnType.Timestamp -> ColumnValue.Text(text = currentDateTimeUTC().formatISO())
                    else -> col.value
                }
                val options = when (columnValue) {
                    is ColumnValue.OptionList -> columnValue.options
                    else -> emptyList()
                }

                col.dbName to LocalColumnValue(
                    id = col.id,
                    dbName = col.dbName,
                    name = col.name,
                    type = col.type,
                    value = columnValue,
                    constraints = col.constraints,
                    listOptions = options,
                )
            }.toTypedArray())
        ),
    )
}

private object MutableLocalRowDataSerializer: KSerializer<MutableLocalRowData> {
    private val colSerializer: KSerializer<Map<String, LocalColumnValue>> = MapSerializer(keySerializer = serializer(), valueSerializer = serializer())

    override val descriptor: SerialDescriptor
        get() = colSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: MutableLocalRowData
    ) {
        colSerializer.serialize(encoder = encoder, value = value.cols)
    }

    override fun deserialize(decoder: Decoder): MutableLocalRowData {
        val cols = colSerializer.deserialize(decoder)

        return MutableLocalRowData(cols = cols)
    }
}

private object LocalColumnValueSerializer: KSerializer<LocalColumnValue> {
    private val constraintListSerializer: KSerializer<List<ColumnConstraint>> = ListSerializer(serializer())
    private val optionsListSerializer: KSerializer<List<String>> = ListSerializer(serializer())
    private val columnTypeSerializer: KSerializer<ColumnType> = ColumnTypeSerializer
    private val columnValueSerializer: KSerializer<ColumnValue> = ColumnValueSerializer

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("dk.skancode.LocalColumnValue") {
        element("id", PrimitiveSerialDescriptor("dk.skancode.LocalColumnValue.id", PrimitiveKind.STRING))
        element("dbName", PrimitiveSerialDescriptor("dk.skancode.LocalColumnValue.dbName", PrimitiveKind.STRING))
        element("name", PrimitiveSerialDescriptor("dk.skancode.LocalColumnValue.name", PrimitiveKind.STRING))
        element("type", columnTypeSerializer.descriptor)
        element("constraints", constraintListSerializer.descriptor)
        element("listOptions", optionsListSerializer.descriptor)
        element("value", columnValueSerializer.descriptor)
    }

    override fun serialize(
        encoder: Encoder,
        value: LocalColumnValue
    ) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, descriptor.getElementIndex("id"), value.id)
            encodeStringElement(descriptor, descriptor.getElementIndex("dbName"), value.dbName)
            encodeStringElement(descriptor, descriptor.getElementIndex("name"), value.name)

            encodeSerializableElement(descriptor, descriptor.getElementIndex("type"), columnTypeSerializer, value.type)
            encodeSerializableElement(descriptor, descriptor.getElementIndex("constraints"), constraintListSerializer, value.constraints)
            encodeSerializableElement(descriptor, descriptor.getElementIndex("listOptions"), optionsListSerializer, value.listOptions)
            encodeSerializableElement(descriptor, descriptor.getElementIndex("value"), columnValueSerializer, value.value)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): LocalColumnValue {
        return when(decoder) {
            is JsonDecoder -> {
                val obj = decoder.decodeJsonElement().jsonObject

                val id = (obj["id"] as? JsonPrimitive)?.contentOrNull ?: error("'id' missing in LocalColumnValue deserialization")
                val dbName = (obj["dbName"] as? JsonPrimitive)?.contentOrNull ?: error("'dbName' missing in LocalColumnValue deserialization")
                val name = (obj["name"] as? JsonPrimitive)?.contentOrNull ?: error("'name' missing in LocalColumnValue deserialization")
                val type = jsonSerializer.decodeFromJsonElement(columnTypeSerializer, obj["type"] ?: error("'type' missing in LocalColumnValue deserialization"))
                val constraints = jsonSerializer.decodeFromJsonElement(constraintListSerializer, obj["constraints"] ?: error("'constraints' missing in LocalColumnValue deserialization"))
                val listOptions = jsonSerializer.decodeFromJsonElement(optionsListSerializer, obj["listOptions"] ?: error("'listOptions' missing in LocalColumnValue deserialization"))
                val value = obj["value"]?.jsonPrimitive?: error("'value' missing in LocalColumnValue deserialization")

                val columnValue = when(type) {
                    ColumnType.Boolean -> ColumnValue.Boolean(checked = value.boolean)
                    ColumnType.File -> ColumnValue.File(localUrl = value.content)
                    ColumnType.Numeric -> ColumnValue.Numeric(num = value.intOrNull ?: value.doubleOrNull)
                    ColumnType.Text -> ColumnValue.Text(value.content)
                    ColumnType.Timestamp -> ColumnValue.Text(value.content)
                    ColumnType.User -> {
                        val content = value.contentOrNull
                        if (content != null) ColumnValue.Text(content) else ColumnValue.Null
                    }
                    ColumnType.Unknown,
                    ColumnType.Id -> ColumnValue.Null
                    ColumnType.List -> ColumnValue.OptionList(options = listOptions, selected = value.contentOrNull)
                }

                LocalColumnValue(
                    id = id,
                    dbName = dbName,
                    name = name,
                    type = type,
                    value = columnValue,
                    constraints = constraints,
                    listOptions = listOptions,
                )
            }
            else -> decoder.decodeStructure(descriptor) {
                val id = decodeStringElement(descriptor, descriptor.getElementIndex("id"))
                val dbName = decodeStringElement(descriptor, descriptor.getElementIndex("dbName"))
                val name = decodeStringElement(descriptor, descriptor.getElementIndex("name"))

                val type = decodeSerializableElement(descriptor, descriptor.getElementIndex("type"), columnTypeSerializer)
                val constraints = decodeSerializableElement(descriptor, descriptor.getElementIndex("constraints"), constraintListSerializer)
                val options = decodeSerializableElement(descriptor, descriptor.getElementIndex("listOptions"), optionsListSerializer)

                val columnValue = when(type) {
                    ColumnType.Boolean -> ColumnValue.Boolean(checked = decodeBooleanElement(descriptor, descriptor.getElementIndex("value")))
                    ColumnType.File -> ColumnValue.File(localUrl = decodeStringOrNull(descriptor, descriptor.getElementIndex("value")))
                    ColumnType.Numeric -> ColumnValue.Numeric(num = decodeDoubleOrNull(descriptor, descriptor.getElementIndex("value")))
                    ColumnType.Text -> ColumnValue.Text(decodeStringElement(descriptor, descriptor.getElementIndex("value")))
                    ColumnType.Timestamp -> ColumnValue.Text(decodeStringElement(descriptor, descriptor.getElementIndex("value")))
                    ColumnType.User,
                    ColumnType.Unknown,
                    ColumnType.Id -> {
                        decodeStringOrNull(descriptor, descriptor.getElementIndex("value"))
                        ColumnValue.Null
                    }
                    ColumnType.List -> ColumnValue.OptionList(options = options, selected = decodeStringOrNull(descriptor, descriptor.getElementIndex("value")))
                }

                LocalColumnValue(
                    id = id,
                    dbName = dbName,
                    name = name,
                    type = type,
                    value = columnValue,
                    constraints = constraints,
                    listOptions = options,
                )
            }
        }

    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun CompositeDecoder.decodeStringOrNull(descriptor: SerialDescriptor, index: Int, previousValue: String? = null): String? =
        decodeNullableSerializableElement(descriptor, index, String.serializer(), previousValue)

    @OptIn(ExperimentalSerializationApi::class)
    private fun CompositeDecoder.decodeDoubleOrNull(descriptor: SerialDescriptor, index: Int, previousValue: Double? = null): Double? =
        decodeNullableSerializableElement(descriptor, index, Double.serializer(), previousValue)
}