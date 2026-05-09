package com.posan.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posan.app.data.prefs.SessionManager
import com.posan.app.data.repository.AuthRepository
import com.posan.app.data.repository.ProductRepository
import com.posan.app.data.repository.TransactionRepository
import com.posan.app.util.Format
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardState(
    val name: String = "",
    val role: String = "",
    val todayRevenue: Double = 0.0,
    val todayTransactions: Int = 0,
    val lowStockCount: Int = 0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository,
    private val transactionRepository: TransactionRepository,
    productRepository: ProductRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        combine(sessionManager.currentUserName, sessionManager.currentUserRole) { name, role ->
            _state.update { it.copy(name = name.orEmpty(), role = role.orEmpty()) }
        }.launchIn(viewModelScope)

        productRepository.observeLowStockCount(5)
            .onEach { count -> _state.update { it.copy(lowStockCount = count) } }
            .launchIn(viewModelScope)

        refreshToday()
    }

    fun refreshToday() {
        viewModelScope.launch {
            val report = transactionRepository.dailyReport(System.currentTimeMillis())
            _state.update {
                it.copy(
                    todayRevenue = report.totalRevenue,
                    todayTransactions = report.transactionCount
                )
            }
        }
    }

    fun logout(onLogout: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLogout()
        }
    }

    fun moneyText(value: Double): String = Format.money(value)
}
