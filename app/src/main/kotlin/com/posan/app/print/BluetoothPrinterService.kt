package com.posan.app.print

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.posan.app.domain.model.PaperWidth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class PrinterDevice(
    val name: String,
    val address: String,
    val isPaired: Boolean
)

@Singleton
class BluetoothPrinterService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun isBluetoothSupported(): Boolean {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter != null
    }

    fun isBluetoothEnabled(): Boolean {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter?.isEnabled == true
    }

    fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    fun listPairedDevices(): List<PrinterDevice> {
        if (!hasConnectPermission()) return emptyList()
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter = manager?.adapter ?: return emptyList()
        return adapter.bondedDevices.orEmpty().mapNotNull { device ->
            try {
                PrinterDevice(
                    name = device.name ?: "(Tanpa nama)",
                    address = device.address,
                    isPaired = device.bondState == BluetoothDevice.BOND_BONDED
                )
            } catch (e: SecurityException) {
                null
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun findConnection(address: String): BluetoothConnection? {
        return BluetoothPrintersConnections().list?.firstOrNull { it.device.address == address }
    }

    suspend fun testPrint(address: String, paperWidth: PaperWidth): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = findConnection(address) ?: error("Printer tidak ditemukan / belum dipair")
            val printer = createPrinter(connection, paperWidth)
            printer.printFormattedTextAndCut(testReceipt())
            printer.disconnectPrinter()
            Unit
        }
    }

    suspend fun print(address: String, formatted: String, paperWidth: PaperWidth, copies: Int = 1, cutPaper: Boolean = true, openCashDrawer: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = findConnection(address) ?: error("Printer tidak ditemukan / belum dipair")
            val printer = createPrinter(connection, paperWidth)
            repeat(copies.coerceAtLeast(1)) {
                if (cutPaper) printer.printFormattedTextAndCut(formatted) else printer.printFormattedText(formatted)
            }
            if (openCashDrawer) {
                runCatching { printer.printFormattedText("[C]\u001Bp\u0000\u0019\u00FA") }
            }
            printer.disconnectPrinter()
            Unit
        }
    }

    private fun createPrinter(connection: BluetoothConnection, paperWidth: PaperWidth): EscPosPrinter {
        val widthMm = when (paperWidth) {
            PaperWidth.MM_58 -> 58f
            PaperWidth.MM_80 -> 80f
        }
        val charsPerLine = when (paperWidth) {
            PaperWidth.MM_58 -> 32
            PaperWidth.MM_80 -> 48
        }
        return EscPosPrinter(connection, 203, widthMm, charsPerLine)
    }

    private fun testReceipt(): String =
        """
        [C]<u><b>Posan POS</b></u>
        [C]Test Print
        [L]
        [L]${"-".repeat(32)}
        [L]Jika Anda dapat membaca pesan
        [L]ini, printer Bluetooth sudah
        [L]terhubung dengan benar.
        [L]${"-".repeat(32)}
        [C]Selesai
        [C]
        """.trimIndent()
}
