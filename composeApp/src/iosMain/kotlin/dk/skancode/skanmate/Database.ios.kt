package dk.skancode.skanmate

import androidx.room.Room
import androidx.room.RoomDatabase
import dk.skancode.skanmate.data.room.SkanMateDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual val database: SkanMateDatabase = getRoomDatabase(getDatabaseBuilder())

fun getDatabaseBuilder(): RoomDatabase.Builder<SkanMateDatabase> {
    val dbFilePath = documentDirectory() + "/my_room.db"
    return Room.databaseBuilder<SkanMateDatabase>(
        name = dbFilePath,
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path)
}