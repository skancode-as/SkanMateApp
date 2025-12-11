package dk.skancode.skanmate.data.room

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [
        TableEntity::class,
        TableDataEntity::class,
        UserEntity::class,
        TenantEntity::class,
        SessionEntity::class
    ],
    version = 1,
)
@ConstructedBy(SkanMateDatabaseConstructor::class)
abstract class SkanMateDatabase: RoomDatabase() {
    abstract fun getTableDao(): TableDao
    abstract fun getAuthDao(): AuthDao
}

@Suppress("KotlinNoActualForExpect", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object SkanMateDatabaseConstructor: RoomDatabaseConstructor<SkanMateDatabase> {
    override fun initialize(): SkanMateDatabase
}