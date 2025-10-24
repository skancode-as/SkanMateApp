package dk.skancode.skanmate

import android.app.Application
import androidx.core.net.toUri
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

class SkanMateApplication: Application() {
    companion object {
        private lateinit var instance: SkanMateApplication
        lateinit var settingsFactory: Settings.Factory

        fun deleteLocalFile(path: String) {
            instance.deleteLocalFile(path)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        settingsFactory = SharedPreferencesSettings.Factory(this)
    }

    fun deleteLocalFile(path: String) {
        println("SkanMateApplication::deleteLocalFile($path)")
        val rows = contentResolver.delete(path.toUri(), null, null)
        if (rows == 0) {
            println("Could not delete file at:\n$path")
        } else {
            println("File deleted at:\n$path")
        }
    }
}