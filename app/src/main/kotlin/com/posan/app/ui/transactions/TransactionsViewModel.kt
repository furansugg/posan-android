package com.posan.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posan.app.data.local.entity.TransactionEntity
import com.posan.app.data.repository.TransactionRepository
import com.posan.app.util.Format
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    val date = MutableStateFlow(System.currentTimeMillis())

    val transactions: StateFlow<List<TransactionEntity>> = date
        .flatMapLatest { ts ->
            val start = Format.startOfDay(ts)
            val end = Format.endOfDay(ts)
            repository.observeBetween(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setDate(timestamp: Long) {
        date.value = timestamp
    }
}
