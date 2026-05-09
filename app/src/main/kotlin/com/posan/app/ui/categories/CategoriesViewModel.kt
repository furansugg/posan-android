package com.posan.app.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posan.app.data.local.entity.CategoryEntity
import com.posan.app.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val showForm: Boolean = false,
    val editing: CategoryEntity? = null,
    val name: String = "",
    val error: String? = null
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repository: CategoryRepository
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _state = MutableStateFlow(CategoriesUiState())
    val state: StateFlow<CategoriesUiState> = _state.asStateFlow()

    fun openForm(entity: CategoryEntity? = null) {
        _state.update { it.copy(showForm = true, editing = entity, name = entity?.name.orEmpty(), error = null) }
    }
    fun closeForm() = _state.update { it.copy(showForm = false, editing = null, name = "", error = null) }
    fun setName(value: String) = _state.update { it.copy(name = value, error = null) }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.update { it.copy(error = "Nama wajib") }
            return
        }
        viewModelScope.launch {
            val result = runCatching {
                if (s.editing == null) {
                    repository.insert(CategoryEntity(name = s.name.trim()))
                } else {
                    repository.update(s.editing.copy(name = s.name.trim()))
                }
            }
            if (result.isSuccess) closeForm()
            else _state.update { it.copy(error = result.exceptionOrNull()?.message) }
        }
    }

    fun delete(entity: CategoryEntity) {
        viewModelScope.launch { repository.delete(entity) }
    }
}
