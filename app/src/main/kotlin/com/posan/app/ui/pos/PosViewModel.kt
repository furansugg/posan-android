package com.posan.app.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posan.app.data.local.entity.CategoryEntity
import com.posan.app.data.local.entity.CustomerEntity
import com.posan.app.data.local.entity.PrintSettingsEntity
import com.posan.app.data.local.entity.ProductEntity
import com.posan.app.data.local.entity.TransactionEntity
import com.posan.app.data.prefs.SessionManager
import com.posan.app.data.repository.CartLine
import com.posan.app.data.repository.CategoryRepository
import com.posan.app.data.repository.CheckoutInput
import com.posan.app.data.repository.CheckoutResult
import com.posan.app.data.repository.CustomerRepository
import com.posan.app.data.repository.PrintSettingsRepository
import com.posan.app.data.repository.ProductRepository
import com.posan.app.data.repository.TransactionRepository
import com.posan.app.domain.model.PaperWidth
import com.posan.app.domain.model.PaymentMethod
import com.posan.app.print.BluetoothPrinterService
import com.posan.app.print.ReceiptComposer
import com.posan.app.print.ReceiptInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CartItem(
    val product: ProductEntity,
    val quantity: Int,
    val discount: Double = 0.0
) {
    val subtotal: Double get() = product.price * quantity - discount
}

data class PosUiState(
    val query: String = "",
    val selectedCategoryId: Long? = null,
    val cart: List<CartItem> = emptyList(),
    val customer: CustomerEntity? = null,
    val orderDiscount: Double = 0.0,
    val taxPercent: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val paymentReceived: Double = 0.0,
    val note: String = "",
    val showCheckout: Boolean = false,
    val showCustomerPicker: Boolean = false,
    val processing: Boolean = false,
    val error: String? = null,
    val lastResult: CheckoutResult? = null,
    val printingState: PrintingState = PrintingState.Idle
) {
    val subtotal: Double get() = cart.sumOf { it.subtotal }
    val afterDiscount: Double get() = (subtotal - orderDiscount).coerceAtLeast(0.0)
    val tax: Double get() = afterDiscount * (taxPercent / 100.0)
    val total: Double get() = afterDiscount + tax
    val change: Double get() = (paymentReceived - total).coerceAtLeast(0.0)
}

sealed interface PrintingState {
    data object Idle : PrintingState
    data object Sending : PrintingState
    data class Success(val message: String) : PrintingState
    data class Error(val message: String) : PrintingState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PosViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    categoryRepository: CategoryRepository,
    customerRepository: CustomerRepository,
    private val transactionRepository: TransactionRepository,
    private val printSettingsRepository: PrintSettingsRepository,
    private val receiptComposer: ReceiptComposer,
    private val printerService: BluetoothPrinterService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(PosUiState())
    val state: StateFlow<PosUiState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow<Pair<String, Long?>>("" to null)

    val products: StateFlow<List<ProductEntity>> = queryFlow
        .flatMapLatest { (q, cat) -> productRepository.search(q, cat) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<CustomerEntity>> = customerRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val printSettings: StateFlow<PrintSettingsEntity> = printSettingsRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PrintSettingsEntity())

    init {
        combine(printSettingsRepository.observe()) { (settings) ->
            _state.update { it.copy(taxPercent = settings.taxPercentDefault) }
        }.launchIn(viewModelScope)
    }

    fun setQuery(value: String) {
        _state.update { it.copy(query = value) }
        queryFlow.value = value to _state.value.selectedCategoryId
    }

    fun setCategory(id: Long?) {
        _state.update { it.copy(selectedCategoryId = id) }
        queryFlow.value = _state.value.query to id
    }

    fun addToCart(product: ProductEntity) {
        if (product.stock <= 0) {
            _state.update { it.copy(error = "Stok ${product.name} habis") }
            return
        }
        _state.update { current ->
            val existing = current.cart.find { it.product.id == product.id }
            val newCart = if (existing == null) {
                current.cart + CartItem(product, 1)
            } else {
                if (existing.quantity + 1 > product.stock) {
                    return@update current.copy(error = "Stok ${product.name} tidak cukup")
                }
                current.cart.map { if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it }
            }
            current.copy(cart = newCart, error = null)
        }
    }

    fun increaseQty(productId: Long) = _state.update { current ->
        val updated = current.cart.map {
            if (it.product.id == productId) {
                if (it.quantity + 1 > it.product.stock) {
                    return@update current.copy(error = "Stok ${it.product.name} tidak cukup")
                }
                it.copy(quantity = it.quantity + 1)
            } else it
        }
        current.copy(cart = updated, error = null)
    }

    fun decreaseQty(productId: Long) = _state.update { current ->
        val updated = current.cart.mapNotNull {
            if (it.product.id == productId) {
                if (it.quantity - 1 <= 0) null else it.copy(quantity = it.quantity - 1)
            } else it
        }
        current.copy(cart = updated, error = null)
    }

    fun removeFromCart(productId: Long) = _state.update {
        it.copy(cart = it.cart.filterNot { item -> item.product.id == productId })
    }

    fun clearCart() = _state.update {
        it.copy(
            cart = emptyList(),
            orderDiscount = 0.0,
            paymentReceived = 0.0,
            customer = null,
            note = "",
            error = null
        )
    }

    fun setOrderDiscount(value: Double) = _state.update { it.copy(orderDiscount = value.coerceAtLeast(0.0)) }
    fun setTaxPercent(value: Double) = _state.update { it.copy(taxPercent = value.coerceIn(0.0, 100.0)) }
    fun setPaymentMethod(value: PaymentMethod) = _state.update { it.copy(paymentMethod = value) }
    fun setPaymentReceived(value: Double) = _state.update { it.copy(paymentReceived = value.coerceAtLeast(0.0)) }
    fun setNote(value: String) = _state.update { it.copy(note = value) }
    fun setCustomer(c: CustomerEntity?) = _state.update { it.copy(customer = c, showCustomerPicker = false) }
    fun toggleCheckout(show: Boolean) {
        _state.update { current ->
            current.copy(
                showCheckout = show,
                paymentReceived = if (show && current.paymentMethod != PaymentMethod.CASH) current.total else current.paymentReceived
            )
        }
    }
    fun toggleCustomerPicker(show: Boolean) = _state.update { it.copy(showCustomerPicker = show) }
    fun consumeError() = _state.update { it.copy(error = null) }
    fun consumeResult() = _state.update {
        it.copy(
            lastResult = null,
            cart = emptyList(),
            orderDiscount = 0.0,
            paymentReceived = 0.0,
            customer = null,
            note = "",
            showCheckout = false
        )
    }
    fun consumePrint() = _state.update { it.copy(printingState = PrintingState.Idle) }

    fun checkout() {
        val current = _state.value
        if (current.processing) return
        _state.update { it.copy(processing = true, error = null) }
        viewModelScope.launch {
            val userId = sessionManager.currentUserId.first()
            val input = CheckoutInput(
                userId = userId,
                customerId = current.customer?.id,
                lines = current.cart.map { line ->
                    CartLine(
                        productId = line.product.id,
                        name = line.product.name,
                        sku = line.product.sku,
                        price = line.product.price,
                        quantity = line.quantity,
                        discount = line.discount
                    )
                },
                discount = current.orderDiscount,
                taxPercent = current.taxPercent,
                paymentMethod = current.paymentMethod,
                paymentReceived = if (current.paymentMethod == PaymentMethod.CASH) current.paymentReceived else current.total,
                note = current.note.ifBlank { null }
            )
            val result = transactionRepository.checkout(input)
            _state.update {
                it.copy(
                    processing = false,
                    error = result.exceptionOrNull()?.message,
                    lastResult = result.getOrNull()
                )
            }
        }
    }

    fun printLastReceipt() {
        val result = _state.value.lastResult ?: return
        viewModelScope.launch {
            _state.update { it.copy(printingState = PrintingState.Sending) }
            val settings = printSettingsRepository.get()
            if (settings.savedDeviceAddress.isNullOrBlank()) {
                _state.update { it.copy(printingState = PrintingState.Error("Belum ada printer terhubung. Buka pengaturan cetak.")) }
                return@launch
            }
            val tx = transactionRepository.findById(result.transactionId)
                ?: return@launch _state.update { it.copy(printingState = PrintingState.Error("Transaksi tidak ditemukan")) }
            val items = transactionRepository.findItems(result.transactionId)
            val cashierName = sessionManager.currentUserName.first().orEmpty()
            val composed = receiptComposer.composeEscPos(
                ReceiptInput(
                    transaction = tx,
                    items = items,
                    cashierName = cashierName,
                    customerName = _state.value.customer?.name
                ),
                settings
            )
            val res = printerService.print(
                address = settings.savedDeviceAddress,
                formatted = composed,
                paperWidth = PaperWidth.fromName(settings.paperWidth),
                copies = settings.printCopies,
                cutPaper = settings.cutPaper,
                openCashDrawer = settings.openCashDrawer
            )
            _state.update {
                it.copy(
                    printingState = if (res.isSuccess) PrintingState.Success("Struk berhasil dicetak")
                    else PrintingState.Error(res.exceptionOrNull()?.message ?: "Gagal mencetak")
                )
            }
        }
    }
}
