package com.posan.app.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posan.app.data.local.entity.CategoryEntity
import com.posan.app.data.local.entity.ProductEntity
import com.posan.app.data.repository.CategoryRepository
import com.posan.app.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    val query = MutableStateFlow("")
    val selectedCategoryId = MutableStateFlow<Long?>(null)

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val combined = kotlinx.coroutines.flow.combine(query, selectedCategoryId) { q, cat ->
        q to cat
    }

    val products: StateFlow<List<ProductEntity>> = combined
        .flatMapLatest { (q, cat) -> productRepository.search(q, cat) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(product: ProductEntity) {
        viewModelScope.launch { productRepository.delete(product) }
    }
}
