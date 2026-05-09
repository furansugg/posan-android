package com.posan.app.ui.printer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posan.app.data.repository.PrintSettingsRepository
import com.posan.app.domain.model.PaperWidth
import com.posan.app.print.BluetoothPrinterService
import com.posan.app.print.PrinterDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrinterUiState(
    val devices: List<PrinterDevice> = emptyList(),
    val selectedAddress: String? = null,
    val selectedName: String? = null,
    val message: String? = null,
    val testing: Boolean = false,
    val bluetoothEnabled: Boolean = true,
    val needPermission: Boolean = false
)

@HiltViewModel
class PrinterDeviceViewModel @Inject constructor(
    private val printerService: BluetoothPrinterService,
    private val printSettingsRepository: PrintSettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PrinterUiState())
    val state: StateFlow<PrinterUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val settings = printSettingsRepository.get()
            _state.update {
                it.copy(
                    selectedAddress = settings.savedDeviceAddress,
                    selectedName = settings.savedDeviceName,
                    bluetoothEnabled = printerService.isBluetoothEnabled(),
                    needPermission = !printerService.hasConnectPermission()
                )
            }
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    devices = printerService.listPairedDevices(),
                    bluetoothEnabled = printerService.isBluetoothEnabled(),
                    needPermission = !printerService.hasConnectPermission()
                )
            }
        }
    }

    fun choose(device: PrinterDevice) {
        viewModelScope.launch {
            val current = printSettingsRepository.get()
            printSettingsRepository.save(current.copy(savedDeviceAddress = device.address, savedDeviceName = device.name))
            _state.update { it.copy(selectedAddress = device.address, selectedName = device.name, message = "Printer ${device.name} dipilih") }
        }
    }

    fun testPrint(device: PrinterDevice) {
        viewModelScope.launch {
            _state.update { it.copy(testing = true) }
            val current = printSettingsRepository.get()
            val result = printerService.testPrint(device.address, PaperWidth.fromName(current.paperWidth))
            _state.update {
                it.copy(
                    testing = false,
                    message = if (result.isSuccess) "Test print sukses" else result.exceptionOrNull()?.message ?: "Gagal mencetak"
                )
            }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
