package dk.skancode.skanmate.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import dk.skancode.skanmate.data.model.TableModel
import dk.skancode.skanmate.util.jsonSerializer
import kotlinx.serialization.json.Json

@Entity(tableName = "__table_meta")
data class TableEntity(
    @PrimaryKey val id: String,
    val databaseName: String,
    val name: String,
    val description: String?,
    val serializedColumns: String,
    val tenantId: String,
) {
    companion object {
        fun fromModel(
            model: TableModel,
            tenantId: String,
            serializer: Json = jsonSerializer
        ): TableEntity {
            return TableEntity(
                id = model.id,
                databaseName = model.databaseName,
                name = model.name,
                description = model.description,
                serializedColumns = serializer.encodeToString(model.columns),
                tenantId = tenantId
            )
        }
    }
}