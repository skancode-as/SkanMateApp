package dk.skancode.skanmate.data.store

import dk.skancode.skanmate.data.model.LocalRowData
import dk.skancode.skanmate.data.model.LocalTableData
import dk.skancode.skanmate.data.model.MutableLocalRowData
import dk.skancode.skanmate.data.model.TableModel
import dk.skancode.skanmate.data.model.TableSummaryModel
import dk.skancode.skanmate.data.room.TableDao
import dk.skancode.skanmate.data.room.TableDataEntity
import dk.skancode.skanmate.data.room.TableEntity
import dk.skancode.skanmate.util.jsonSerializer
import dk.skancode.skanmate.util.reduceDefault

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

    suspend fun storeRowData(tableId: String, rowData: LocalRowData) {
        dao.insertDataRow(
            data = TableDataEntity(
                tableId = tableId,
                serializedData = jsonSerializer.encodeToString(rowData)
            )
        )
    }

    suspend fun loadRowData(): List<LocalTableData> {
        val entities = dao.getDataRows()

        val map: Map<String, List<LocalRowData>> = entities.reduceDefault(mutableMapOf()) { acc, cur ->
            val rowData: MutableLocalRowData = try {
                jsonSerializer.decodeFromString(cur.serializedData)
            } catch (e: Exception) {
                e.printStackTrace()
                println("Failed to deserialize local row data for tableId: ${cur.tableId}, $e")
                println("Json input: ${cur.serializedData}")
                null
            } ?: return@reduceDefault acc
            rowData.localRowId = cur.id

            if (acc.containsKey(cur.tableId)) {
                acc[cur.tableId] = acc.getValue(cur.tableId).toMutableList().apply { add(rowData) }
            } else {
                acc[cur.tableId] = listOf(rowData)
            }

            acc
        }

        return map.entries.mapNotNull { (tableId, rows) ->
            val model = loadTableModel(tableId) ?: return@mapNotNull null
            LocalTableData(model = model, rows = rows)
        }
    }

    suspend fun deleteRowData(rowId: Long): Int {
        return dao.deleteDataRow(TableDataEntity(rowId, "", ""))
    }

    suspend fun deleteTableRows(tableId: String): Int {
        return dao.deleteTableDataRows(tableId)
    }
}