package dk.skancode.skanmate.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query

@Dao
interface TableDao {
    @Insert(onConflict = REPLACE)
    suspend fun insert(item: TableEntity)

    @Query("SELECT * FROM __table_meta WHERE id = :id LIMIT 1")
    suspend fun getTableById(id: String): TableEntity?

    @Query("SELECT * FROM __table_meta")
    suspend fun getTables(): List<TableEntity>

    @Insert
    suspend fun insertDataRow(data: TableDataEntity)
    @Delete
    suspend fun deleteDataRow(data: TableDataEntity): Int

    @Query("DELETE FROM __table_data WHERE tableId = :tableId")
    suspend fun deleteTableDataRows(tableId: String): Int

    @Query("SELECT * FROM __table_data")
    suspend fun getDataRows(): List<TableDataEntity>

    @Query("SELECT * FROM __table_data WHERE tableId = :tableId")
    suspend fun getDataRowsForTable(tableId: String): List<TableDataEntity>
}