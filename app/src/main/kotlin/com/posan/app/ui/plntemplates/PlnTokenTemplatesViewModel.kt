package com.posan.app.ui.plntemplates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posan.app.data.local.entity.PlnTokenTemplateEntity
import com.posan.app.data.repository.PlnTokenTemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TemplateFormState(
    val show: Boolean = false,
    val editing: PlnTokenTemplateEntity? = null,
    val tarif: String = "R1",
    val daya: String = "900VA",
    val nominal: String = "20000",
    val rpStroom: String = "",
    val adminFee: String = "2500",
    val materai: String = "0",
    val ppn: String = "0",
    val ppj: String = "0",
    val kwh: String = "",
    val error: String? = null
)

@HiltViewModel
class PlnTokenTemplatesViewModel @Inject constructor(
    private val repository: PlnTokenTemplateRepository
) : ViewModel() {

    val templates: StateFlow<List<PlnTokenTemplateEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _form = MutableStateFlow(TemplateFormState())
    val form: StateFlow<TemplateFormState> = _form.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun consumeMessage() = _message.update { null }

    fun openForm(entity: PlnTokenTemplateEntity? = null) {
        _form.update {
            if (entity == null) {
                TemplateFormState(show = true)
            } else {
                TemplateFormState(
                    show = true,
                    editing = entity,
                    tarif = entity.tarif,
                    daya = entity.daya,
                    nominal = entity.nominalRupiah.toString(),
                    rpStroom = trimTrailingZero(entity.rpStroom),
                    adminFee = trimTrailingZero(entity.adminFee),
                    materai = trimTrailingZero(entity.materai),
                    ppn = trimTrailingZero(entity.ppn),
                    ppj = trimTrailingZero(entity.ppj),
                    kwh = trimTrailingZero(entity.kwh)
                )
            }
        }
    }

    fun closeForm() = _form.update { TemplateFormState() }

    fun setTarif(v: String) = _form.update { it.copy(tarif = v.uppercase().trim(), error = null) }
    fun setDaya(v: String) = _form.update { it.copy(daya = v.uppercase().trim(), error = null) }
    fun setNominal(v: String) = _form.update { it.copy(nominal = v.filter { c -> c.isDigit() }, error = null) }
    fun setRpStroom(v: String) = _form.update { it.copy(rpStroom = v) }
    fun setAdminFee(v: String) = _form.update { it.copy(adminFee = v) }
    fun setMaterai(v: String) = _form.update { it.copy(materai = v) }
    fun setPpn(v: String) = _form.update { it.copy(ppn = v) }
    fun setPpj(v: String) = _form.update { it.copy(ppj = v) }
    fun setKwh(v: String) = _form.update { it.copy(kwh = v) }

    fun save() {
        val s = _form.value
        val tarif = s.tarif.trim().uppercase()
        val daya = normalizeDaya(s.daya)
        val nominal = s.nominal.toIntOrNull() ?: 0
        if (tarif.isBlank() || daya.isBlank()) {
            _form.update { it.copy(error = "Tarif & daya wajib") }
            return
        }
        if (nominal <= 0) {
            _form.update { it.copy(error = "Nominal harus lebih dari 0") }
            return
        }
        val entity = (s.editing ?: PlnTokenTemplateEntity(
            tarif = tarif,
            daya = daya,
            nominalRupiah = nominal
        )).copy(
            tarif = tarif,
            daya = daya,
            nominalRupiah = nominal,
            rpStroom = s.rpStroom.toDoubleOrNull() ?: 0.0,
            adminFee = s.adminFee.toDoubleOrNull() ?: 0.0,
            materai = s.materai.toDoubleOrNull() ?: 0.0,
            ppn = s.ppn.toDoubleOrNull() ?: 0.0,
            ppj = s.ppj.toDoubleOrNull() ?: 0.0,
            kwh = s.kwh.toDoubleOrNull() ?: 0.0,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            runCatching { repository.upsert(entity) }
                .onSuccess {
                    _message.value = if (s.editing == null) "Template ditambahkan" else "Template disimpan"
                    closeForm()
                }
                .onFailure { e -> _form.update { it.copy(error = e.message ?: "Gagal menyimpan") } }
        }
    }

    fun delete(entity: PlnTokenTemplateEntity) {
        viewModelScope.launch {
            repository.delete(entity)
            _message.value = "Template dihapus"
        }
    }

    private fun normalizeDaya(raw: String): String {
        val cleaned = raw.trim().uppercase().replace(" ", "")
        if (cleaned.isBlank()) return ""
        return if (cleaned.endsWith("VA") || cleaned.endsWith("KVA")) cleaned else "${cleaned}VA"
    }

    private fun trimTrailingZero(v: Double): String {
        if (v == 0.0) return "0"
        val asInt = v.toLong()
        return if (asInt.toDouble() == v) asInt.toString() else v.toString()
    }
}
