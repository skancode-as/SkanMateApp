package dk.skancode.skanmate.data.room

import androidx.room.Dao
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
}