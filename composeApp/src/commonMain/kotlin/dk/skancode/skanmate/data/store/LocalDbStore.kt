package dk.skancode.skanmate.data.store

import dk.skancode.skanmate.data.room.AuthDao
import dk.skancode.skanmate.data.room.TableDao
import dk.skancode.skanmate.database

object LocalDbStore {
    private val db = database

    val tableDao: TableDao
        get() = db.getTableDao()
    val authDao: AuthDao
        get() = db.getAuthDao()
}