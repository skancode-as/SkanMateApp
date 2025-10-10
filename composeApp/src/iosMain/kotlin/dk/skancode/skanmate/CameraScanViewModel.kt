package dk.skancode.skanmate

import dk.skancode.skanmate.util.CameraPowerListener
import dk.skancode.skanmate.util.CameraScanManagerImpl
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class CameraScanViewModel(cameraScanManager: CameraScanManagerImpl): ViewModel(), CameraPowerListener {
    private val _cameraPowerState = mutableStateOf(false)
    val cameraPowerState: State<Boolean>
        get() = _cameraPowerState

    init {
        cameraScanManager.registerScanListener(this)
    }

    override fun handle(enable: Boolean) {
        _cameraPowerState.value = enable
    }
}