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
    val hasResult: Boolean = false
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
                _state.value = _state.value.copy(
                    recognizedText = text.text,
                    isProcessing = false,
                    hasResult = true,
                    error = if (text.text.isBlank()) "Tidak ada teks yang terdeteksi pada gambar" else null
                )
            }
            .addOnFailureListener { e ->
                _state.value = _state.value.copy(
                    isProcessing = false,
                    error = "Gagal mengenali teks: ${e.localizedMessage}"
                )
            }
    }

    fun clearResult() {
        _state.value = TextRecognitionState()
    }

    override fun onCleared() {
        super.onCleared()
        recognizer.close()
    }
}
