package dk.skancode.skanmate.data.model

import dk.skancode.skanmate.ui.state.ColumnUiState
import dk.skancode.skanmate.util.currentDateTimeUTC
import dk.skancode.skanmate.util.formatISO
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer

private class TableDataSerializer: KSerializer<TableData> {
    @OptIn(ExperimentalSerializationApi::class)
    private val serializer: KSerializer<List<RowData>> = ListSerializer(serializer())

    override val descriptor: SerialDescriptor
        get() = serializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: TableData
    ) {
        serializer.serialize(encoder, value.rows)
    }

    override fun deserialize(decoder: Decoder): TableData {
        return TableData(rows = serializer.deserialize(decoder))
    }
}

@Serializable(with = TableDataSerializer::class)
data class TableData(val rows: List<RowData>)

typealias RowData = Map<String, ColumnValue>

fun rowDataOf(data: List<ColumnUiState>): RowData {
    return mapOf(
        *(data.map { col ->
            when (col.type) {
                ColumnType.Timestamp -> col.dbName to ColumnValue.Text(text = currentDateTimeUTC().formatISO())
                else -> col.dbName to col.value
            }
        }.toTypedArray())
    )
}