package com.posan.app.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posan.app.data.local.entity.TransactionEntity
import com.posan.app.data.repository.DailyReport
import com.posan.app.data.repository.TransactionRepository
import com.posan.app.util.Format
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportUiState(
    val date: Long = System.currentTimeMillis(),
    val report: DailyReport? = null,
    val transactions: List<TransactionEntity> = emptyList(),
    val loading: Boolean = false
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ReportUiState())
    val state: StateFlow<ReportUiState> = _state.asStateFlow()

    init { refresh() }

    fun setDate(timestamp: Long) {
        _state.update { it.copy(date = timestamp) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val date = _state.value.date
            _state.update { it.copy(loading = true) }
            val report = repository.dailyReport(date)
            val txs = repository.observeBetween(Format.startOfDay(date), Format.endOfDay(date)).first()
            _state.update { it.copy(report = report, transactions = txs, loading = false) }
        }
    }
}
