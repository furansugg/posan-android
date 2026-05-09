package com.posan.app.ui.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posan.app.data.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val processing: Boolean = false,
    val message: String? = null,
    val replaceExisting: Boolean = true
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: BackupRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    fun toggleReplace(v: Boolean) = _state.update { it.copy(replaceExisting = v) }

    fun export(uri: Uri) {
        if (_state.value.processing) return
        _state.update { it.copy(processing = true) }
        viewModelScope.launch {
            val result = repository.export(uri)
            _state.update {
                it.copy(
                    processing = false,
                    message = if (result.isSuccess) "Backup tersimpan (${result.getOrNull()} bytes)"
                    else result.exceptionOrNull()?.message ?: "Gagal menyimpan"
                )
            }
        }
    }

    fun import(uri: Uri) {
        if (_state.value.processing) return
        _state.update { it.copy(processing = true) }
        viewModelScope.launch {
            val result = repository.import(uri, _state.value.replaceExisting)
            _state.update {
                it.copy(
                    processing = false,
                    message = if (result.isSuccess) "Restore selesai (${result.getOrNull()} record)"
                    else result.exceptionOrNull()?.message ?: "Gagal restore"
                )
            }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
