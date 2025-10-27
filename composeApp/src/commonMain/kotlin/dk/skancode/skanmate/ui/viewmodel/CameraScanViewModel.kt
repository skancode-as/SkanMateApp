package dk.skancode.skanmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import dk.skancode.skanmate.util.CameraPowerListener
import dk.skancode.skanmate.util.CameraScanManagerImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class CameraScanViewModel(cameraScanManager: CameraScanManagerImpl): ViewModel(),
    CameraPowerListener {
    private val _cameraPowerState = MutableStateFlow(false)
    val cameraPowerState: StateFlow<Boolean>
        get() = _cameraPowerState

    init {
        cameraScanManager.registerScanListener(this)
    }

    override fun handle(enable: Boolean) {
        _cameraPowerState.update { enable }
    }
}