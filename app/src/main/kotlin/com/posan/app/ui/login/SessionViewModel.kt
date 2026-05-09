package com.posan.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posan.app.data.prefs.SessionManager
import com.posan.app.data.repository.AuthRepository
import com.posan.app.data.repository.PlnTokenTemplateRepository
import com.posan.app.data.repository.PrintSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val printSettingsRepository: PrintSettingsRepository,
    private val plnTokenTemplateRepository: PlnTokenTemplateRepository,
    sessionManager: SessionManager
) : ViewModel() {
    val userId: Flow<Long?> = sessionManager.currentUserId.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )
    val userName: Flow<String?> = sessionManager.currentUserName
    val userRole: Flow<String?> = sessionManager.currentUserRole

    fun ensureSeed() {
        viewModelScope.launch {
            authRepository.seedDefaultAdminIfEmpty()
            printSettingsRepository.ensureSeeded()
            plnTokenTemplateRepository.seedDefaultsIfEmpty()
        }
    }
}
