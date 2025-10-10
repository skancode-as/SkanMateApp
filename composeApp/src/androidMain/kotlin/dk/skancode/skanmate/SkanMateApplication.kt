package dk.skancode.skanmate

import android.app.Application
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

class SkanMateApplication: Application() {
    companion object {
        lateinit var settingsFactory: Settings.Factory
    }

    override fun onCreate() {
        super.onCreate()

        settingsFactory = SharedPreferencesSettings.Factory(this)
    }
}