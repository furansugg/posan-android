package com.posan.app.ui.textrecognition

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class TextRecognitionState(
    val recognizedText: String = "",
    val isProcessing: Boolean = false,
    val error: String? = null,
    val hasResult: Boolean = false,
    val parsedReceipt: ParsedReceipt? = null,
    val formattedReceipt: String = "",
    val showRawText: Boolean = false
)

@HiltViewModel
class TextRecognitionViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(TextRecognitionState())
    val state: StateFlow<TextRecognitionState> = _state.asStateFlow()

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun recognizeText(bitmap: Bitmap) {
        _state.value = _state.value.copy(isProcessing = true, error = null)
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { text ->
                if (text.text.isBlank()) {
                    _state.value = _state.value.copy(
                        recognizedText = "",
                        isProcessing = false,
                        hasResult = true,
                        error = "Tidak ada teks yang terdeteksi pada gambar"
                    )
                } else {
                    val parsed = ReceiptParser.parse(text.text)
                    val formatted = ReceiptFormatter.format(parsed)
                    _state.value = _state.value.copy(
                        recognizedText = text.text,
                        isProcessing = false,
                        hasResult = true,
                        parsedReceipt = parsed,
                        formattedReceipt = formatted,
                        error = null
                    )
                }
            }
            .addOnFailureListener { e ->
                _state.value = _state.value.copy(
                    isProcessing = false,
                    hasResult = true,
                    error = "Gagal mengenali teks: ${e.localizedMessage}"
                )
            }
    }

    fun toggleRawText() {
        _state.value = _state.value.copy(showRawText = !_state.value.showRawText)
    }

    fun updateParsedField(field: String, value: String) {
        val receipt = _state.value.parsedReceipt ?: return
        val updated = when (field) {
            "storeName" -> receipt.copy(storeName = value)
            "storeAddress" -> receipt.copy(storeAddress = value)
            "storePhone" -> receipt.copy(storePhone = value)
            "transactionCode" -> receipt.copy(transactionCode = value)
            "dateTime" -> receipt.copy(dateTime = value)
            "cashier" -> receipt.copy(cashier = value)
            "customer" -> receipt.copy(customer = value)
            "paymentMethod" -> receipt.copy(paymentMethod = value)
            "footer" -> receipt.copy(footer = value)
            else -> receipt
        }
        _state.value = _state.value.copy(
            parsedReceipt = updated,
            formattedReceipt = ReceiptFormatter.format(updated)
        )
    }

    fun updateParsedAmount(field: String, value: String) {
        val receipt = _state.value.parsedReceipt ?: return
        val amount = value.replace(".", "").replace(",", ".").toDoubleOrNull() ?: return
        val updated = when (field) {
            "subtotal" -> receipt.copy(subtotal = amount)
            "discount" -> receipt.copy(discount = amount)
            "tax" -> receipt.copy(tax = amount)
            "total" -> receipt.copy(total = amount)
            "paymentReceived" -> receipt.copy(paymentReceived = amount)
            "change" -> receipt.copy(change = amount)
            else -> receipt
        }
        _state.value = _state.value.copy(
            parsedReceipt = updated,
            formattedReceipt = ReceiptFormatter.format(updated)
        )
    }

    fun clearResult() {
        _state.value = TextRecognitionState()
    }

    override fun onCleared() {
        super.onCleared()
        recognizer.close()
    }
}
