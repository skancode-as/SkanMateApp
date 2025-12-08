package dk.skancode.skanmate.data.store

import dk.skancode.skanmate.data.model.TableModel
import dk.skancode.skanmate.data.model.TableSummaryModel
import dk.skancode.skanmate.data.room.TableDao
import dk.skancode.skanmate.data.room.TableEntity
import dk.skancode.skanmate.util.jsonSerializer

class LocalTableStore(
    val dao: TableDao = LocalDbStore.tableDao,
) {
    suspend fun storeTableModels(models: List<TableModel>) {
        models.forEach { model ->
            dao.insert(TableEntity.fromModel(model))
        }
    }

    suspend fun loadTableSummaries(): List<TableSummaryModel> {
        return dao.getTables().map { entity ->
            TableSummaryModel(
                id = entity.id,
                name = entity.name,
                description = entity.description,
            )
        }
    }

    suspend fun loadTableModel(id: String): TableModel? {
        return dao.getTableById(id)?.let { entity ->
            TableModel(
                id = entity.id,
                databaseName = entity.databaseName,
                name = entity.name,
                description = entity.description,
                columns = jsonSerializer.decodeFromString(entity.serializedColumns)
            )
        }
    }
}