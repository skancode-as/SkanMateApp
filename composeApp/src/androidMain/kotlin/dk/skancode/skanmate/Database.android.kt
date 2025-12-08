package dk.skancode.skanmate

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import dk.skancode.skanmate.data.room.SkanMateDatabase

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<SkanMateDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("skanmate_local.db")
    return Room.databaseBuilder<SkanMateDatabase>(
        context = context,
        name = dbFile.absolutePath
    )
}



actual val database: SkanMateDatabase
    get() = SkanMateApplication.roomDatabase