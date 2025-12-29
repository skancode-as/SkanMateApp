package dk.skancode.skanmate.ui.state

import dk.skancode.skanmate.util.InternalStringResource

interface SyncUiState {
    val isLoading: Boolean
    val synchronisationErrors: Map<Long, Map<String, List<InternalStringResource>>>
}

data class MutableSyncUiState(
    override val isLoading: Boolean = false,
    override val synchronisationErrors: Map<Long, Map<String, List<InternalStringResource>>> = emptyMap(),
): SyncUiState