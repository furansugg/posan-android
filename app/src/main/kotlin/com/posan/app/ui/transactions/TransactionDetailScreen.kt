package com.posan.app.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.posan.app.domain.model.PaymentMethod
import com.posan.app.domain.model.TransactionStatus
import com.posan.app.ui.components.SimpleAppBar
import com.posan.app.util.Format

@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    onBack: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val ctx = LocalContext.current

    LaunchedEffect(transactionId) { viewModel.load(transactionId) }
    LaunchedEffect(state.printingMessage, state.voidedMessage) {
        val msg = state.printingMessage ?: state.voidedMessage
        msg?.let {
            android.widget.Toast.makeText(ctx, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = { SimpleAppBar(title = "Detail Transaksi", onBack = onBack) }
    ) { padding ->
        val tx = state.transaction
        if (tx == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("Memuat...")
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(tx.code, fontWeight = FontWeight.Bold)
                    Text(Format.datetime(tx.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Status: ${TransactionStatus.fromName(tx.status).displayName}",
                        color = if (tx.status == TransactionStatus.VOID.name) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
                    if (state.cashierName.isNotBlank()) Text("Kasir: ${state.cashierName}", style = MaterialTheme.typography.bodySmall)
                    if (state.customerName.isNotBlank()) Text("Pelanggan: ${state.customerName}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Item", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    state.items.forEach { item ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(item.name, fontWeight = FontWeight.Medium)
                            Row {
                                Text("${item.quantity} x ${Format.money(item.price)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                Text(Format.money(item.subtotal), fontWeight = FontWeight.Medium)
                            }
                            if (item.discount > 0) {
                                Row {
                                    Text("  Diskon", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                    Text("-${Format.money(item.discount)}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                    Spacer(Modifier.height(8.dp))
                    Row { Text("Subtotal", modifier = Modifier.weight(1f)); Text(Format.money(tx.subtotal)) }
                    if (tx.discount > 0) Row { Text("Diskon", modifier = Modifier.weight(1f)); Text("-${Format.money(tx.discount)}") }
                    if (tx.taxPercent > 0) Row { Text("Pajak ${tx.taxPercent}%", modifier = Modifier.weight(1f)); Text(Format.money(tx.taxAmount)) }
                    Spacer(Modifier.height(4.dp))
                    Row {
                        Text("Total", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text(Format.money(tx.total), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row {
                        Text("Bayar (${PaymentMethod.fromName(tx.paymentMethod).displayName})", modifier = Modifier.weight(1f))
                        Text(Format.money(tx.paymentReceived))
                    }
                    if (tx.paymentMethod == PaymentMethod.CASH.name) {
                        Row { Text("Kembalian", modifier = Modifier.weight(1f)); Text(Format.money(tx.change)) }
                    }
                    if (!tx.note.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Catatan:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text(tx.note!!, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.reprint() },
                    enabled = !state.printing,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.printing) {
                        CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Cetak Ulang")
                }
                if (tx.status != TransactionStatus.VOID.name) {
                    OutlinedButton(
                        onClick = { viewModel.voidTx() },
                        enabled = !state.voiding,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Batalkan")
                    }
                }
            }
        }
    }
}
