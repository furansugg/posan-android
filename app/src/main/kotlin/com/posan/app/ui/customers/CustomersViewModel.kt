package com.posan.app.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posan.app.data.local.entity.CustomerEntity
import com.posan.app.data.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomersUiState(
    val showForm: Boolean = false,
    val editing: CustomerEntity? = null,
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val note: String = "",
    val plnIdPelanggan: String = "",
    val plnMeterNo: String = "",
    val plnTarif: String = "",
    val plnDaya: String = "",
    val plnNamaLengkap: String = "",
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CustomersViewModel @Inject constructor(
    private val repository: CustomerRepository
) : ViewModel() {

    val query = MutableStateFlow("")

    val customers: StateFlow<List<CustomerEntity>> = query
        .flatMapLatest { repository.search(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _state = MutableStateFlow(CustomersUiState())
    val state: StateFlow<CustomersUiState> = _state.asStateFlow()

    fun openForm(entity: CustomerEntity? = null) {
        _state.update {
            it.copy(
                showForm = true,
                editing = entity,
                name = entity?.name.orEmpty(),
                phone = entity?.phone.orEmpty(),
                email = entity?.email.orEmpty(),
                address = entity?.address.orEmpty(),
                note = entity?.note.orEmpty(),
                plnIdPelanggan = entity?.plnIdPelanggan.orEmpty(),
                plnMeterNo = entity?.plnMeterNo.orEmpty(),
                plnTarif = entity?.plnTarif.orEmpty(),
                plnDaya = entity?.plnDaya.orEmpty(),
                plnNamaLengkap = entity?.plnNamaLengkap.orEmpty(),
                error = null
            )
        }
    }

    fun closeForm() = _state.update { CustomersUiState() }
    fun setName(v: String) = _state.update { it.copy(name = v, error = null) }
    fun setPhone(v: String) = _state.update { it.copy(phone = v) }
    fun setEmail(v: String) = _state.update { it.copy(email = v) }
    fun setAddress(v: String) = _state.update { it.copy(address = v) }
    fun setNote(v: String) = _state.update { it.copy(note = v) }
    fun setPlnIdPelanggan(v: String) = _state.update { it.copy(plnIdPelanggan = v) }
    fun setPlnMeterNo(v: String) = _state.update { it.copy(plnMeterNo = v) }
    fun setPlnTarif(v: String) = _state.update { it.copy(plnTarif = v) }
    fun setPlnDaya(v: String) = _state.update { it.copy(plnDaya = v) }
    fun setPlnNamaLengkap(v: String) = _state.update { it.copy(plnNamaLengkap = v) }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.update { it.copy(error = "Nama wajib") }
            return
        }
        viewModelScope.launch {
            runCatching {
                val entity = (s.editing ?: CustomerEntity(name = "")).copy(
                    name = s.name.trim(),
                    phone = s.phone.trim().ifBlank { null },
                    email = s.email.trim().ifBlank { null },
                    address = s.address.trim().ifBlank { null },
                    note = s.note.trim().ifBlank { null },
                    plnIdPelanggan = s.plnIdPelanggan.trim().ifBlank { null },
                    plnMeterNo = s.plnMeterNo.trim().ifBlank { null },
                    plnTarif = s.plnTarif.trim().uppercase().ifBlank { null },
                    plnDaya = normalizeDaya(s.plnDaya),
                    plnNamaLengkap = s.plnNamaLengkap.trim().ifBlank { null }
                )
                if (entity.id == 0L) repository.insert(entity) else repository.update(entity)
            }.onSuccess { closeForm() }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    private fun normalizeDaya(raw: String): String? {
        val cleaned = raw.trim().uppercase().replace(" ", "")
        if (cleaned.isBlank()) return null
        // Allow user to enter "900" and store as "900VA"; keep "VA"/"KVA" if explicitly given.
        return if (cleaned.endsWith("VA") || cleaned.endsWith("KVA")) cleaned else "${cleaned}VA"
    }

    fun delete(entity: CustomerEntity) {
        viewModelScope.launch { repository.delete(entity) }
    }
}
