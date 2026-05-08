package com.posan.app.ui.plntoken

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posan.app.data.local.entity.PrintSettingsEntity
import com.posan.app.data.prefs.SessionManager
import com.posan.app.data.repository.AuthRepository
import com.posan.app.data.repository.PrintSettingsRepository
import com.posan.app.domain.model.PaperWidth
import com.posan.app.ocr.ParsedPlnReceipt
import com.posan.app.ocr.PlnReceiptOcr
import com.posan.app.ocr.PlnReceiptParser
import com.posan.app.print.BluetoothPrinterService
import com.posan.app.print.PlnTokenInput
import com.posan.app.print.ReceiptComposer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    val kwh: String = "",
    val rpStroom: String = "",
    val adminFee: String = "2500",
    val materai: String = "0",
    val ppn: String = "0",
    val ppj: String = "0",
    val totalBayar: String = ""
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
    private val composer: ReceiptComposer,
    private val printerService: BluetoothPrinterService,
    private val sessionManager: SessionManager,
    private val ocr: PlnReceiptOcr,
    private val parser: PlnReceiptParser
) : ViewModel() {

    private val _form = MutableStateFlow(PlnTokenFormState())
    val form: StateFlow<PlnTokenFormState> = _form.asStateFlow()

    private val _settings = MutableStateFlow(PrintSettingsEntity())
    val settings: StateFlow<PrintSettingsEntity> = _settings.asStateFlow()

    private val _printing = MutableStateFlow(false)
    val printing: StateFlow<Boolean> = _printing.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        viewModelScope.launch {
            _settings.value = printSettingsRepository.get()
        }
    }

    fun setReferenceNo(v: String) = _form.update { it.copy(referenceNo = v) }
    fun setMeterNo(v: String) = _form.update { it.copy(meterNo = v) }
    fun setCustomerId(v: String) = _form.update { it.copy(customerId = v) }
    fun setCustomerName(v: String) = _form.update { it.copy(customerName = v) }
    fun setTariff(v: String) = _form.update { it.copy(tariff = v) }
    fun setPower(v: String) = _form.update { it.copy(power = v) }
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

    fun scanFromImage(uri: Uri) {
        if (_scanning.value) return
        _scanning.value = true
        viewModelScope.launch {
            try {
                val raw = ocr.extractText(uri)
                if (raw.isBlank()) {
                    _message.value = "Tidak ada teks terbaca dari gambar"
                    return@launch
                }
                val parsed = parser.parse(raw)
                if (parsed.isEmpty()) {
                    _message.value = "Tidak ada field PLN yang dikenali"
                    return@launch
                }
                applyParsed(parsed)
                _message.value = "Data berhasil diisi dari gambar. Mohon periksa kembali."
            } catch (e: Exception) {
                _message.value = "Gagal membaca gambar: ${e.message ?: "tidak diketahui"}"
            } finally {
                _scanning.value = false
            }
        }
    }

    private fun applyParsed(parsed: ParsedPlnReceipt) {
        _form.update { current ->
            current.copy(
                customerId = parsed.customerId?.takeIf { it.isNotBlank() } ?: current.customerId,
                customerName = parsed.customerName?.takeIf { it.isNotBlank() } ?: current.customerName,
                meterNo = parsed.meterNo?.takeIf { it.isNotBlank() } ?: current.meterNo,
                tariff = parsed.tariff?.takeIf { it.isNotBlank() } ?: current.tariff,
                power = parsed.power?.takeIf { it.isNotBlank() } ?: current.power,
                referenceNo = parsed.referenceNo?.takeIf { it.isNotBlank() } ?: current.referenceNo,
                token = parsed.token?.takeIf { it.isNotBlank() } ?: current.token,
                kwh = parsed.kwh?.takeIf { it.isNotBlank() } ?: current.kwh,
                rpStroom = parsed.rpStroom?.takeIf { it.isNotBlank() } ?: current.rpStroom,
                adminFee = parsed.adminFee?.takeIf { it.isNotBlank() } ?: current.adminFee,
                materai = parsed.materai?.takeIf { it.isNotBlank() } ?: current.materai,
                ppn = parsed.ppn?.takeIf { it.isNotBlank() } ?: current.ppn,
                ppj = parsed.ppj?.takeIf { it.isNotBlank() } ?: current.ppj,
                totalBayar = parsed.totalBayar?.takeIf { it.isNotBlank() } ?: current.totalBayar
            )
        }
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
