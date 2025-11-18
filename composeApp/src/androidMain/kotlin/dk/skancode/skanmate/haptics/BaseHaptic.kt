package dk.skancode.skanmate.haptics

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import dk.skancode.skanmate.util.Haptic

@RequiresApi(Build.VERSION_CODES.O)
abstract class BaseHaptic(
    val context: Context,
    val numPulses: Int,
): Haptic {
    lateinit var effect: VibrationEffect

    @RequiresPermission(Manifest.permission.VIBRATE)
    override fun start() {
        val vibrator = context.getSystemService<Vibrator>()
        vibrator?.vibrate(effect)
    }
}