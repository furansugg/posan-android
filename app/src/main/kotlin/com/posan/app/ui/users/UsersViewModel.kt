package com.posan.app.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posan.app.data.local.entity.UserEntity
import com.posan.app.data.repository.AuthRepository
import com.posan.app.domain.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UsersUiState(
    val showForm: Boolean = false,
    val editing: UserEntity? = null,
    val username: String = "",
    val password: String = "",
    val name: String = "",
    val role: UserRole = UserRole.KASIR,
    val active: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val users: StateFlow<List<UserEntity>> = authRepository.observeUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _state = MutableStateFlow(UsersUiState())
    val state: StateFlow<UsersUiState> = _state.asStateFlow()

    fun openForm(user: UserEntity? = null) {
        _state.update {
            it.copy(
                showForm = true,
                editing = user,
                username = user?.username.orEmpty(),
                password = "",
                name = user?.name.orEmpty(),
                role = user?.role?.let { r -> UserRole.fromName(r) } ?: UserRole.KASIR,
                active = user?.active ?: true,
                error = null
            )
        }
    }
    fun closeForm() = _state.update { UsersUiState() }
    fun setUsername(v: String) = _state.update { it.copy(username = v, error = null) }
    fun setPassword(v: String) = _state.update { it.copy(password = v, error = null) }
    fun setName(v: String) = _state.update { it.copy(name = v) }
    fun setRole(v: UserRole) = _state.update { it.copy(role = v) }
    fun setActive(v: Boolean) = _state.update { it.copy(active = v) }

    fun save() {
        val s = _state.value
        viewModelScope.launch {
            val result = if (s.editing == null) {
                authRepository.createUser(s.username, s.password, s.name, s.role)
                    .map { Unit }
            } else {
                runCatching {
                    authRepository.updateUser(
                        s.editing.copy(
                            name = s.name.trim().ifBlank { s.editing.name },
                            role = s.role.name,
                            active = s.active
                        ),
                        if (s.password.isBlank()) null else s.password
                    )
                }
            }
            if (result.isSuccess) closeForm()
            else _state.update { it.copy(error = result.exceptionOrNull()?.message) }
        }
    }

    fun delete(user: UserEntity) {
        viewModelScope.launch {
            runCatching { authRepository.deleteUser(user) }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    fun consumeError() = _state.update { it.copy(error = null) }
}
