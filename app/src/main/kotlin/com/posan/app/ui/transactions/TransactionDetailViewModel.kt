package com.posan.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posan.app.data.local.entity.PrintSettingsEntity
import com.posan.app.data.local.entity.TransactionEntity
import com.posan.app.data.local.entity.TransactionItemEntity
import com.posan.app.data.repository.AuthRepository
import com.posan.app.data.repository.CustomerRepository
import com.posan.app.data.repository.PrintSettingsRepository
import com.posan.app.data.repository.TransactionRepository
import com.posan.app.domain.model.PaperWidth
import com.posan.app.print.BluetoothPrinterService
import com.posan.app.print.ReceiptComposer
import com.posan.app.print.ReceiptInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TxDetailState(
    val transaction: TransactionEntity? = null,
    val items: List<TransactionItemEntity> = emptyList(),
    val cashierName: String = "",
    val customerName: String = "",
    val settings: PrintSettingsEntity = PrintSettingsEntity(),
    val printingMessage: String? = null,
    val printing: Boolean = false,
    val voiding: Boolean = false,
    val voidedMessage: String? = null
)

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val authRepository: AuthRepository,
    private val customerRepository: CustomerRepository,
    private val printSettingsRepository: PrintSettingsRepository,
    private val receiptComposer: ReceiptComposer,
    private val printerService: BluetoothPrinterService
) : ViewModel() {

    private val _state = MutableStateFlow(TxDetailState())
    val state: StateFlow<TxDetailState> = _state.asStateFlow()

    fun load(id: Long) {
        viewModelScope.launch {
            val tx = repository.findById(id) ?: return@launch
            val items = repository.findItems(id)
            val settings = printSettingsRepository.get()
            val cashier = tx.userId?.let { authRepository.findById(it)?.name }.orEmpty()
            val customer = tx.customerId?.let { customerRepository.findById(it)?.name }.orEmpty()
            _state.update {
                it.copy(
                    transaction = tx,
                    items = items,
                    cashierName = cashier,
                    customerName = customer,
                    settings = settings
                )
            }
        }
    }

    fun reprint() {
        val s = _state.value
        val tx = s.transaction ?: return
        viewModelScope.launch {
            _state.update { it.copy(printing = true, printingMessage = null) }
            if (s.settings.savedDeviceAddress.isNullOrBlank()) {
                _state.update { it.copy(printing = false, printingMessage = "Belum ada printer terhubung. Buka pengaturan cetak.") }
                return@launch
            }
            val composed = receiptComposer.composeEscPos(
                ReceiptInput(
                    transaction = tx,
                    items = s.items,
                    cashierName = s.cashierName,
                    customerName = s.customerName.ifBlank { null }
                ),
                s.settings
            )
            val result = printerService.print(
                address = s.settings.savedDeviceAddress!!,
                formatted = composed,
                paperWidth = PaperWidth.fromName(s.settings.paperWidth),
                copies = s.settings.printCopies,
                cutPaper = s.settings.cutPaper,
                openCashDrawer = s.settings.openCashDrawer
            )
            _state.update {
                it.copy(
                    printing = false,
                    printingMessage = if (result.isSuccess) "Struk berhasil dicetak" else result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun voidTx() {
        val tx = _state.value.transaction ?: return
        viewModelScope.launch {
            _state.update { it.copy(voiding = true) }
            val result = repository.voidTransaction(tx.id)
            _state.update { it.copy(voiding = false, voidedMessage = result.exceptionOrNull()?.message ?: "Transaksi dibatalkan") }
            load(tx.id)
        }
    }

    fun consumeMessage() = _state.update { it.copy(printingMessage = null, voidedMessage = null) }
}
