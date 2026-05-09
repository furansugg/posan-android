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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.posan.app.domain.model.PaymentMethod
import com.posan.app.domain.model.TransactionStatus
import com.posan.app.ui.components.IconBadge
import com.posan.app.ui.components.OutlinedSurfaceCard
import com.posan.app.ui.components.PaymentMethodBadge
import com.posan.app.ui.components.SectionHeader
import com.posan.app.ui.components.SimpleAppBar
import com.posan.app.ui.components.TransactionStatusBadge
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
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Memuat...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedSurfaceCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(icon = Icons.Default.Receipt, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx.code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                Format.datetime(tx.createdAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TransactionStatusBadge(status = tx.status)
                    }
                    Spacer(Modifier.height(12.dp))
                    if (state.cashierName.isNotBlank()) {
                        InfoRow(icon = Icons.Default.Person, label = "Kasir", value = state.cashierName)
                    }
                    if (state.customerName.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        InfoRow(icon = Icons.Default.Person, label = "Pelanggan", value = state.customerName)
                    }
                }
            }

            SectionHeader(title = "Item", subtitle = "${state.items.size} produk")

            OutlinedSurfaceCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    state.items.forEachIndexed { index, item ->
                        if (index > 0) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(
                                icon = Icons.Default.ShoppingBag,
                                tint = MaterialTheme.colorScheme.tertiary,
                                boxSize = 36.dp,
                                iconSize = 18.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "${item.quantity} x ${Format.money(item.price)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (item.discount > 0) {
                                    Text(
                                        "Diskon -${Format.money(item.discount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Text(
                                Format.money(item.subtotal),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            OutlinedSurfaceCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    SummaryLine("Subtotal", Format.money(tx.subtotal))
                    if (tx.discount > 0) SummaryLine("Diskon", "-${Format.money(tx.discount)}")
                    if (tx.taxPercent > 0) SummaryLine("Pajak ${tx.taxPercent}%", Format.money(tx.taxAmount))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row {
                        Text(
                            text = "Total",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = Format.money(tx.total),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pembayaran", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        PaymentMethodBadge(method = tx.paymentMethod)
                    }
                    Spacer(Modifier.height(4.dp))
                    SummaryLine("Diterima", Format.money(tx.paymentReceived))
                    if (tx.paymentMethod == PaymentMethod.CASH.name) {
                        SummaryLine("Kembalian", Format.money(tx.change))
                    }
                    if (!tx.note.isNullOrBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text("Catatan", style = MaterialTheme.typography.labelLarge)
                        Text(tx.note!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.reprint() },
                    enabled = !state.printing,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    if (state.printing) {
                        CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                    } else {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("Cetak Ulang")
                }
                if (tx.status != TransactionStatus.VOID.name) {
                    OutlinedButton(
                        onClick = { viewModel.voidTx() },
                        enabled = !state.voiding,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Batalkan")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text("$label: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
