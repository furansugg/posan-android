package com.posan.app.ui.printsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posan.app.data.local.entity.PrintSettingsEntity
import com.posan.app.data.local.entity.TransactionEntity
import com.posan.app.data.local.entity.TransactionItemEntity
import com.posan.app.data.repository.PrintSettingsRepository
import com.posan.app.domain.model.PaperWidth
import com.posan.app.domain.model.PaymentMethod
import com.posan.app.domain.model.PrintAlignment
import com.posan.app.print.BluetoothPrinterService
import com.posan.app.print.ReceiptComposer
import com.posan.app.print.ReceiptInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrintSettingsViewModel @Inject constructor(
    private val repository: PrintSettingsRepository,
    private val composer: ReceiptComposer,
    private val printerService: BluetoothPrinterService
) : ViewModel() {

    private val _draft = MutableStateFlow(PrintSettingsEntity())
    val draft: StateFlow<PrintSettingsEntity> = _draft.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _testing = MutableStateFlow(false)
    val testing: StateFlow<Boolean> = _testing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val preview: StateFlow<String> = repository.observe().map { settings ->
        _draft.value = settings
        composer.composePlain(sampleInput(), settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun reset() {
        viewModelScope.launch {
            _draft.value = repository.get()
        }
    }

    fun update(transform: (PrintSettingsEntity) -> PrintSettingsEntity) {
        _draft.update(transform)
    }

    fun setStoreName(v: String) = update { it.copy(storeName = v) }
    fun setStoreAddress(v: String) = update { it.copy(storeAddress = v) }
    fun setStorePhone(v: String) = update { it.copy(storePhone = v) }
    fun setHeader(v: String) = update { it.copy(headerText = v) }
    fun setFooter(v: String) = update { it.copy(footerText = v) }
    fun setShowLogo(v: Boolean) = update { it.copy(showLogo = v) }
    fun setShowCashier(v: Boolean) = update { it.copy(showCashier = v) }
    fun setShowCustomer(v: Boolean) = update { it.copy(showCustomer = v) }
    fun setShowItemSku(v: Boolean) = update { it.copy(showItemSku = v) }
    fun setPaperWidth(v: PaperWidth) = update { it.copy(paperWidth = v.name) }
    fun setAlignment(v: PrintAlignment) = update { it.copy(titleAlignment = v.name) }
    fun setTitleBold(v: Boolean) = update { it.copy(titleBold = v) }
    fun setTitleDouble(v: Boolean) = update { it.copy(titleDoubleSize = v) }
    fun setBodySmall(v: Boolean) = update { it.copy(bodyFontSmall = v) }
    fun setCopies(v: Int) = update { it.copy(printCopies = v.coerceIn(1, 5)) }
    fun setCutPaper(v: Boolean) = update { it.copy(cutPaper = v) }
    fun setMmFeedBeforeCut(v: Int) = update { it.copy(mmFeedBeforeCut = v.coerceIn(0, 30)) }
    fun setOpenDrawer(v: Boolean) = update { it.copy(openCashDrawer = v) }
    fun setTaxDefault(v: Double) = update { it.copy(taxPercentDefault = v.coerceIn(0.0, 100.0)) }
    fun setCurrency(v: String) = update { it.copy(currencySymbol = v) }

    fun previewLive(): String = composer.composePlain(sampleInput(), _draft.value)

    fun save() {
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            repository.save(_draft.value)
            _saving.value = false
            _message.value = "Pengaturan disimpan"
        }
    }

    fun testPrint() {
        if (_testing.value) return
        _testing.value = true
        viewModelScope.launch {
            val settings = _draft.value
            if (settings.savedDeviceAddress.isNullOrBlank()) {
                _message.value = "Belum ada printer terhubung"
                _testing.value = false
                return@launch
            }
            val composed = composer.composeEscPos(sampleInput(), settings)
            val result = printerService.print(
                address = settings.savedDeviceAddress,
                formatted = composed,
                paperWidth = PaperWidth.fromName(settings.paperWidth),
                copies = 1,
                cutPaper = settings.cutPaper,
                openCashDrawer = false,
                mmFeedBeforeCut = settings.mmFeedBeforeCut
            )
            _testing.value = false
            _message.value = if (result.isSuccess) "Test print berhasil" else result.exceptionOrNull()?.message ?: "Gagal mencetak"
        }
    }

    fun consumeMessage() = _message.update { null }

    private fun sampleInput(): ReceiptInput {
        val now = System.currentTimeMillis()
        val tx = TransactionEntity(
            id = 0L,
            code = "TX-PREVIEW",
            userId = null,
            customerId = null,
            subtotal = 35000.0,
            discount = 2000.0,
            taxPercent = 10.0,
            taxAmount = 3300.0,
            total = 36300.0,
            paymentMethod = PaymentMethod.CASH.name,
            paymentReceived = 50000.0,
            change = 13700.0,
            note = null,
            status = com.posan.app.domain.model.TransactionStatus.PAID.name,
            createdAt = now
        )
        val items = listOf(
            TransactionItemEntity(transactionId = 0L, productId = 1L, name = "Kopi Susu Gula Aren", sku = "KSG-01", price = 18000.0, quantity = 1, discount = 0.0, subtotal = 18000.0),
            TransactionItemEntity(transactionId = 0L, productId = 2L, name = "Roti Cokelat", sku = "RTC-02", price = 8500.0, quantity = 2, discount = 0.0, subtotal = 17000.0)
        )
        return ReceiptInput(
            transaction = tx,
            items = items,
            cashierName = "Kasir Demo",
            customerName = "Pelanggan"
        )
    }
}
