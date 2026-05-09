package com.posan.app.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posan.app.data.local.entity.ProductEntity
import com.posan.app.data.local.entity.StockMovementEntity
import com.posan.app.data.repository.ProductRepository
import com.posan.app.data.repository.StockRepository
import com.posan.app.domain.model.StockMovementType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StockUiState(
    val showAdjust: Boolean = false,
    val product: ProductEntity? = null,
    val type: StockMovementType = StockMovementType.IN,
    val quantity: String = "",
    val note: String = "",
    val error: String? = null
)

@HiltViewModel
class StockViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    stockRepository: StockRepository
) : ViewModel() {

    val products: StateFlow<List<ProductEntity>> = productRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val movements: StateFlow<List<StockMovementEntity>> = stockRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _state = MutableStateFlow(StockUiState())
    val state: StateFlow<StockUiState> = _state.asStateFlow()

    fun openAdjust(product: ProductEntity) {
        _state.update { it.copy(showAdjust = true, product = product, quantity = "", note = "", error = null) }
    }
    fun closeAdjust() = _state.update { StockUiState() }
    fun setType(type: StockMovementType) = _state.update { it.copy(type = type) }
    fun setQuantity(v: String) = _state.update { it.copy(quantity = v.filter { ch -> ch.isDigit() }) }
    fun setNote(v: String) = _state.update { it.copy(note = v) }

    fun submit() {
        val s = _state.value
        val product = s.product ?: return
        val qty = s.quantity.toIntOrNull() ?: 0
        if (qty <= 0) {
            _state.update { it.copy(error = "Jumlah harus > 0") }
            return
        }
        val delta = when (s.type) {
            StockMovementType.IN -> qty
            StockMovementType.OUT -> -qty
            StockMovementType.ADJUSTMENT -> qty - product.stock
            StockMovementType.SALE -> -qty
        }
        viewModelScope.launch {
            runCatching {
                productRepository.adjustStock(product.id, delta, s.type, s.note.ifBlank { null })
            }.onSuccess { closeAdjust() }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }
}
