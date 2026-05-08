package com.posan.app.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.posan.app.ui.components.IconBadge
import com.posan.app.ui.components.OutlinedSurfaceCard
import com.posan.app.ui.components.SectionHeader
import com.posan.app.ui.components.SimpleAppBar
import com.posan.app.ui.theme.Brand500
import com.posan.app.ui.theme.Brand700
import com.posan.app.ui.theme.CatBlue
import com.posan.app.ui.theme.CatGreen
import com.posan.app.ui.theme.CatOrange
import com.posan.app.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    onBack: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SimpleAppBar(
                title = "Laporan Harian",
                subtitle = Format.date(state.date),
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showPicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Pilih tanggal")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            val report = state.report
            if (report == null) {
                Box(modifier = Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("Memuat...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Brand700, Brand500)))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(Color.White.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Total Pendapatan",
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = Format.money(report.totalRevenue),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.displaySmall
                        )
                        Text(
                            text = "${report.transactionCount} transaksi",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeader(title = "Per Metode Pembayaran")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PaymentStat(
                            label = "Tunai",
                            value = Format.money(report.cashRevenue),
                            tint = CatGreen,
                            icon = Icons.Default.Payments,
                            modifier = Modifier.weight(1f)
                        )
                        PaymentStat(
                            label = "QRIS",
                            value = Format.money(report.qrisRevenue),
                            tint = CatBlue,
                            icon = Icons.Default.QrCode2,
                            modifier = Modifier.weight(1f)
                        )
                        PaymentStat(
                            label = "Kartu",
                            value = Format.money(report.cardRevenue),
                            tint = CatOrange,
                            icon = Icons.Default.CreditCard,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    SectionHeader(title = "Transaksi", subtitle = "${state.transactions.size} entri")
                    if (state.transactions.isEmpty()) {
                        OutlinedSurfaceCard {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Belum ada transaksi pada tanggal ini",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        OutlinedSurfaceCard {
                            Column {
                                state.transactions.forEachIndexed { index, tx ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconBadge(
                                            icon = Icons.Default.Receipt,
                                            tint = MaterialTheme.colorScheme.primary,
                                            boxSize = 32.dp,
                                            iconSize = 16.dp
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                tx.code,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                Format.timeOnly(tx.createdAt),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            Format.money(tx.total),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (index < state.transactions.lastIndex) {
                                        androidx.compose.material3.HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.date)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { viewModel.setDate(it) }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Batal") } }
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun PaymentStat(
    label: String,
    value: String,
    tint: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    OutlinedSurfaceCard(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            IconBadge(icon = icon, tint = tint, boxSize = 32.dp, iconSize = 16.dp)
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
