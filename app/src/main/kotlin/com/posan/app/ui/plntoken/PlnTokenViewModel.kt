package com.posan.app.ui.plntoken

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posan.app.data.local.entity.CustomerEntity
import com.posan.app.data.local.entity.PlnTokenTemplateEntity
import com.posan.app.data.local.entity.PrintSettingsEntity
import com.posan.app.data.prefs.SessionManager
import com.posan.app.data.repository.AuthRepository
import com.posan.app.data.repository.CustomerRepository
import com.posan.app.data.repository.PlnTokenTemplateRepository
import com.posan.app.data.repository.PrintSettingsRepository
import com.posan.app.domain.model.PaperWidth
import com.posan.app.print.BluetoothPrinterService
import com.posan.app.print.PlnTokenInput
import com.posan.app.print.ReceiptComposer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class PlnTokenFormState(
    val referenceNo: String = autoRef(),
    val meterNo: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val tariff: String = "R1",
    val power: String = "900VA",
    val token: String = "",
    val nominal: Long = 0L,
    val kwh: String = "",
    val rpStroom: String = "",
    val adminFee: String = "2500",
    val materai: String = "0",
    val ppn: String = "0",
    val ppj: String = "0",
    val totalBayar: String = "",
    val selectedCustomerId: Long? = null,
    val selectedTemplateId: Long? = null
) {
    val totalBayarComputed: Double
        get() {
            val explicit = totalBayar.toDoubleOrNull()
            if (explicit != null && explicit > 0) return explicit
            return (rpStroom.toDoubleOrNull() ?: 0.0) +
                (adminFee.toDoubleOrNull() ?: 0.0) +
                (materai.toDoubleOrNull() ?: 0.0) +
                (ppn.toDoubleOrNull() ?: 0.0) +
                (ppj.toDoubleOrNull() ?: 0.0)
        }
}

private fun autoRef(): String {
    val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
    return "PLN-${sdf.format(Date())}"
}

@HiltViewModel
class PlnTokenViewModel @Inject constructor(
    private val printSettingsRepository: PrintSettingsRepository,
    private val authRepository: AuthRepository,
    private val customerRepository: CustomerRepository,
    private val templateRepository: PlnTokenTemplateRepository,
    private val composer: ReceiptComposer,
    private val printerService: BluetoothPrinterService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _form = MutableStateFlow(PlnTokenFormState())
    val form: StateFlow<PlnTokenFormState> = _form.asStateFlow()

    private val _settings = MutableStateFlow(PrintSettingsEntity())
    val settings: StateFlow<PrintSettingsEntity> = _settings.asStateFlow()

    private val _printing = MutableStateFlow(false)
    val printing: StateFlow<Boolean> = _printing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** Customers that have any PLN-related field filled in. */
    val plnCustomers: StateFlow<List<CustomerEntity>> = customerRepository.observeAll()
        .map { list -> list.filter { it.hasPlnData } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { _settings.value = printSettingsRepository.get() }
    }

    fun setReferenceNo(v: String) = _form.update { it.copy(referenceNo = v) }
    fun setMeterNo(v: String) = _form.update { it.copy(meterNo = v, selectedCustomerId = null) }
    fun setCustomerId(v: String) = _form.update { it.copy(customerId = v, selectedCustomerId = null) }
    fun setCustomerName(v: String) = _form.update { it.copy(customerName = v) }
    fun setTariff(v: String) = _form.update { it.copy(tariff = v.uppercase().trim(), selectedTemplateId = null) }
    fun setPower(v: String) = _form.update { it.copy(power = v.uppercase().replace(" ", ""), selectedTemplateId = null) }
    fun setToken(v: String) = _form.update { it.copy(token = v) }
    fun setKwh(v: String) = _form.update { it.copy(kwh = v) }
    fun setRpStroom(v: String) = _form.update { it.copy(rpStroom = v) }
    fun setAdminFee(v: String) = _form.update { it.copy(adminFee = v) }
    fun setMaterai(v: String) = _form.update { it.copy(materai = v) }
    fun setPpn(v: String) = _form.update { it.copy(ppn = v) }
    fun setPpj(v: String) = _form.update { it.copy(ppj = v) }
    fun setTotalBayar(v: String) = _form.update { it.copy(totalBayar = v) }

    fun consumeMessage() = _message.update { null }

    fun resetForm() {
        _form.value = PlnTokenFormState()
    }

    /**
     * Populate the form with the selected customer's PLN data. If a nominal was already
     * picked we re-apply the matching template afterwards so the breakdown stays in sync.
     */
    fun selectCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            val fresh = customerRepository.findById(customer.id) ?: customer
            val displayName = fresh.plnNamaLengkap?.takeIf { it.isNotBlank() } ?: fresh.name
            _form.update { current ->
                current.copy(
                    selectedCustomerId = fresh.id,
                    customerId = fresh.plnIdPelanggan.orEmpty(),
                    customerName = displayName,
                    meterNo = fresh.plnMeterNo.orEmpty(),
                    tariff = fresh.plnTarif?.takeIf { it.isNotBlank() } ?: current.tariff,
                    power = fresh.plnDaya?.takeIf { it.isNotBlank() } ?: current.power,
                    selectedTemplateId = null
                )
            }
            applyTemplateForCurrent()
        }
    }

    fun clearCustomer() {
        _form.update {
            it.copy(
                selectedCustomerId = null,
                customerId = "",
                customerName = "",
                meterNo = ""
            )
        }
    }

    /**
     * Pick a nominal denomination (e.g. 20_000, 50_000, 100_000) and immediately fetch
     * the matching template for the current tariff/power. Hint via [_message] if no
     * exact match exists.
     */
    fun selectNominal(nominal: Long) {
        _form.update { it.copy(nominal = nominal) }
        applyTemplateForCurrent()
    }

    fun clearNominal() {
        _form.update { it.copy(nominal = 0L, selectedTemplateId = null) }
    }

    private fun applyTemplateForCurrent() {
        val current = _form.value
        if (current.nominal <= 0L) return
        viewModelScope.launch {
            val template = templateRepository.findExact(
                tarif = current.tariff,
                daya = current.power,
                nominal = current.nominal.toInt()
            )
            if (template == null) {
                _message.value = "Template ${current.tariff}/${current.power} ${current.nominal / 1000}k belum ada — buat di menu Template Token PLN"
                return@launch
            }
            applyTemplate(template)
        }
    }

    private fun applyTemplate(template: PlnTokenTemplateEntity) {
        _form.update { current ->
            current.copy(
                selectedTemplateId = template.id,
                kwh = trimZero(template.kwh),
                rpStroom = trimZero(template.rpStroom),
                adminFee = trimZero(template.adminFee),
                materai = trimZero(template.materai),
                ppn = trimZero(template.ppn),
                ppj = trimZero(template.ppj),
                totalBayar = ""
            )
        }
    }

    private fun trimZero(v: Double): String {
        if (v == 0.0) return "0"
        val asInt = v.toLong()
        return if (asInt.toDouble() == v) asInt.toString() else v.toString()
    }

    private suspend fun buildInput(): PlnTokenInput {
        val f = _form.value
        val cashier = sessionManager.currentUserId.first()?.let { authRepository.findById(it)?.name }
        return PlnTokenInput(
            referenceNo = f.referenceNo.ifBlank { autoRef() },
            meterNo = f.meterNo,
            customerId = f.customerId,
            customerName = f.customerName,
            tariff = f.tariff,
            power = f.power,
            token = f.token,
            kwh = f.kwh.toDoubleOrNull() ?: 0.0,
            rpStroom = f.rpStroom.toDoubleOrNull() ?: 0.0,
            adminFee = f.adminFee.toDoubleOrNull() ?: 0.0,
            materai = f.materai.toDoubleOrNull() ?: 0.0,
            ppn = f.ppn.toDoubleOrNull() ?: 0.0,
            ppj = f.ppj.toDoubleOrNull() ?: 0.0,
            totalBayar = f.totalBayarComputed,
            cashierName = cashier
        )
    }

    fun previewLive(): String {
        val f = _form.value
        val cashier = "" // ignore cashier in live preview to avoid Flow blocking
        val input = PlnTokenInput(
            referenceNo = f.referenceNo.ifBlank { autoRef() },
            meterNo = f.meterNo.ifBlank { "11111111111" },
            customerId = f.customerId.ifBlank { "0123456789" },
            customerName = f.customerName.ifBlank { "Pelanggan" },
            tariff = f.tariff.ifBlank { "R1" },
            power = f.power.ifBlank { "900VA" },
            token = f.token.ifBlank { "12345678901234567890" },
            kwh = f.kwh.toDoubleOrNull() ?: 0.0,
            rpStroom = f.rpStroom.toDoubleOrNull() ?: 0.0,
            adminFee = f.adminFee.toDoubleOrNull() ?: 0.0,
            materai = f.materai.toDoubleOrNull() ?: 0.0,
            ppn = f.ppn.toDoubleOrNull() ?: 0.0,
            ppj = f.ppj.toDoubleOrNull() ?: 0.0,
            totalBayar = f.totalBayarComputed,
            cashierName = cashier.ifBlank { null }
        )
        return composer.composePlnTokenPlain(input, _settings.value)
    }

    private fun validate(): String? {
        val f = _form.value
        if (f.meterNo.isBlank()) return "No meter wajib diisi"
        if (f.token.isBlank()) return "No token wajib diisi"
        if ((f.rpStroom.toDoubleOrNull() ?: 0.0) <= 0.0) return "Rp Stroom wajib diisi"
        return null
    }

    fun print() {
        if (_printing.value) return
        val err = validate()
        if (err != null) {
            _message.value = err
            return
        }
        val settings = _settings.value
        if (settings.savedDeviceAddress.isNullOrBlank()) {
            _message.value = "Belum ada printer terhubung"
            return
        }
        _printing.value = true
        viewModelScope.launch {
            val input = buildInput()
            val composed = composer.composePlnTokenEscPos(input, settings)
            val result = printerService.print(
                address = settings.savedDeviceAddress,
                formatted = composed,
                paperWidth = PaperWidth.fromName(settings.paperWidth),
                copies = settings.printCopies,
                cutPaper = settings.cutPaper,
                openCashDrawer = false,
                mmFeedBeforeCut = settings.mmFeedBeforeCut
            )
            _printing.value = false
            _message.value = if (result.isSuccess) "Struk token berhasil dicetak"
                else result.exceptionOrNull()?.message ?: "Gagal mencetak"
        }
    }
}
