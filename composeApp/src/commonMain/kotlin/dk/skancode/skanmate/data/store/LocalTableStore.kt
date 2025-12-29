package dk.skancode.skanmate.data.store

import dk.skancode.skanmate.data.model.ColumnModel
import dk.skancode.skanmate.data.model.LocalRowData
import dk.skancode.skanmate.data.model.LocalTableData
import dk.skancode.skanmate.data.model.MutableLocalRowData
import dk.skancode.skanmate.data.model.OfflineTableSummaryModel
import dk.skancode.skanmate.data.model.TableModel
import dk.skancode.skanmate.data.model.TableSummaryModel
import dk.skancode.skanmate.data.model.isAvailableOffline
import dk.skancode.skanmate.data.room.TableDao
import dk.skancode.skanmate.data.room.TableDataEntity
import dk.skancode.skanmate.data.room.TableEntity
import dk.skancode.skanmate.util.jsonSerializer
import dk.skancode.skanmate.util.reduceDefault

class LocalTableStore(
    val dao: TableDao = LocalDbStore.tableDao,
) {
    suspend fun storeTableModels(models: List<TableModel>, tenantId: String) {
        dao.deleteTableEntities(tenantId)

        models.forEach { model ->
            dao.insertTableEntity(TableEntity.fromModel(model = model, tenantId = tenantId))
        }
    }

    suspend fun loadTableSummaries(tenantId: String): List<TableSummaryModel> {
        return dao.getTables(tenantId = tenantId).map { entity ->
            val columns: List<ColumnModel> = jsonSerializer.decodeFromString(entity.serializedColumns)

            OfflineTableSummaryModel(
                id = entity.id,
                name = entity.name,
                description = entity.description,
                isAvailableOffline = columns.isAvailableOffline()
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

    suspend fun storeRowData(tableId: String, tenantId: String, rowData: LocalRowData) {
        dao.insertDataRow(
            data = TableDataEntity(
                tableId = tableId,
                tenantId = tenantId,
                serializedData = jsonSerializer.encodeToString(rowData)
            )
        )
    }

    suspend fun loadRowData(tenantId: String): List<LocalTableData> {
        val entities = dao.getDataRows(tenantId = tenantId)

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
        return dao.deleteDataRow(
            TableDataEntity(
                id = rowId,
                tableId = "",
                tenantId = "",
                serializedData = ""
            )
        )
    }

    suspend fun deleteTableRows(tableId: String): Int {
        return dao.deleteTableDataRows(tableId)
    }
}