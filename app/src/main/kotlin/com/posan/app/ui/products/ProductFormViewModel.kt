package com.posan.app.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posan.app.data.local.entity.CategoryEntity
import com.posan.app.data.local.entity.ProductEntity
import com.posan.app.data.repository.CategoryRepository
import com.posan.app.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductFormState(
    val id: Long = 0,
    val name: String = "",
    val sku: String = "",
    val barcode: String = "",
    val price: String = "",
    val cost: String = "",
    val stock: String = "0",
    val unit: String = "pcs",
    val categoryId: Long? = null,
    val active: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class ProductFormViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProductFormState())
    val state: StateFlow<ProductFormState> = _state.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun load(id: Long) {
        if (id <= 0) return
        viewModelScope.launch {
            productRepository.findById(id)?.let { p ->
                _state.update {
                    ProductFormState(
                        id = p.id,
                        name = p.name,
                        sku = p.sku,
                        barcode = p.barcode.orEmpty(),
                        price = if (p.price > 0) p.price.toLong().toString() else "",
                        cost = if (p.cost > 0) p.cost.toLong().toString() else "",
                        stock = p.stock.toString(),
                        unit = p.unit,
                        categoryId = p.categoryId,
                        active = p.active
                    )
                }
            }
        }
    }

    fun setName(v: String) = _state.update { it.copy(name = v, error = null) }
    fun setSku(v: String) = _state.update { it.copy(sku = v, error = null) }
    fun setBarcode(v: String) = _state.update { it.copy(barcode = v) }
    fun setPrice(v: String) = _state.update { it.copy(price = v.filter { ch -> ch.isDigit() }) }
    fun setCost(v: String) = _state.update { it.copy(cost = v.filter { ch -> ch.isDigit() }) }
    fun setStock(v: String) = _state.update { it.copy(stock = v.filter { ch -> ch.isDigit() }) }
    fun setUnit(v: String) = _state.update { it.copy(unit = v) }
    fun setCategory(id: Long?) = _state.update { it.copy(categoryId = id) }
    fun setActive(v: Boolean) = _state.update { it.copy(active = v) }

    fun save() {
        val s = _state.value
        if (s.saving) return
        if (s.name.isBlank()) {
            _state.update { it.copy(error = "Nama wajib") }
            return
        }
        if (s.sku.isBlank()) {
            _state.update { it.copy(error = "SKU wajib") }
            return
        }
        val price = s.price.toDoubleOrNull() ?: 0.0
        if (price <= 0) {
            _state.update { it.copy(error = "Harga jual wajib") }
            return
        }
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val entity = ProductEntity(
                id = s.id,
                name = s.name.trim(),
                sku = s.sku.trim(),
                barcode = s.barcode.trim().ifBlank { null },
                price = price,
                cost = s.cost.toDoubleOrNull() ?: 0.0,
                stock = s.stock.toIntOrNull() ?: 0,
                unit = s.unit.ifBlank { "pcs" },
                categoryId = s.categoryId,
                active = s.active
            )
            val res = runCatching {
                if (s.id == 0L) productRepository.insert(entity) else productRepository.update(entity)
            }
            _state.update {
                it.copy(
                    saving = false,
                    error = res.exceptionOrNull()?.message,
                    saved = res.isSuccess
                )
            }
        }
    }
}
