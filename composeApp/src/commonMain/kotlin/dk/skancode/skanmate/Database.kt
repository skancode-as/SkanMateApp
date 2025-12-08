package dk.skancode.skanmate

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dk.skancode.skanmate.data.room.SkanMateDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

expect val database: SkanMateDatabase

fun getRoomDatabase(
    builder: RoomDatabase.Builder<SkanMateDatabase>
): SkanMateDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}