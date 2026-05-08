package com.posan.app.ui.plntoken

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.posan.app.domain.model.PaperWidth
import com.posan.app.ui.components.FormSection
import com.posan.app.ui.components.IconBadge
import com.posan.app.ui.components.OutlinedSurfaceCard
import com.posan.app.ui.components.SectionHeader
import com.posan.app.ui.components.SimpleAppBar
import com.posan.app.util.Format

@Composable
fun PlnTokenScreen(
    onBack: () -> Unit,
    viewModel: PlnTokenViewModel = hiltViewModel()
) {
    val form by viewModel.form.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val printing by viewModel.printing.collectAsState()
    val message by viewModel.message.collectAsState()
    val ctx = LocalContext.current

    LaunchedEffect(message) {
        message?.let {
            android.widget.Toast.makeText(ctx, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            SimpleAppBar(
                title = "Token PLN",
                subtitle = "Cetak struk pulsa listrik",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.resetForm() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset form")
                    }
                }
            )
        }
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
            val isWide = maxWidth >= 720.dp
            if (isWide) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        TokenForm(viewModel = viewModel, form = form, printing = printing)
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxSize().padding(16.dp)) {
                        ReceiptPreview(
                            text = viewModel.previewLive(),
                            paperWidth = PaperWidth.fromName(settings.paperWidth)
                        )
                    }
                }
            } else {
                TokenForm(viewModel = viewModel, form = form, printing = printing)
            }
        }
    }
}

@Composable
private fun TokenForm(
    viewModel: PlnTokenViewModel,
    form: PlnTokenFormState,
    printing: Boolean,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedSurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon = Icons.Default.Bolt, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Token Listrik PLN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Isi data pelanggan & nomor token, lalu cetak.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        FormSection(title = "Pelanggan", subtitle = "Identitas pemilik meter") {
            OutlinedTextField(
                value = form.customerId,
                onValueChange = viewModel::setCustomerId,
                label = { Text("ID Pelanggan") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            OutlinedTextField(
                value = form.customerName,
                onValueChange = viewModel::setCustomerName,
                label = { Text("Nama Pelanggan") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            OutlinedTextField(
                value = form.meterNo,
                onValueChange = viewModel::setMeterNo,
                label = { Text("No Meter") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.tariff,
                    onValueChange = viewModel::setTariff,
                    label = { Text("Tarif") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = form.power,
                    onValueChange = viewModel::setPower,
                    label = { Text("Daya") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                )
            }
        }

        FormSection(title = "Token & kWh", subtitle = "Hasil pembelian dari sistem PLN") {
            OutlinedTextField(
                value = form.token,
                onValueChange = viewModel::setToken,
                label = { Text("No Token (20 digit)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            OutlinedTextField(
                value = form.kwh,
                onValueChange = viewModel::setKwh,
                label = { Text("Jumlah kWh") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
        }

        FormSection(title = "Rincian Biaya") {
            OutlinedTextField(
                value = form.rpStroom,
                onValueChange = viewModel::setRpStroom,
                label = { Text("Rp Stroom (nilai isi)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.adminFee,
                    onValueChange = viewModel::setAdminFee,
                    label = { Text("Admin Bank") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = form.materai,
                    onValueChange = viewModel::setMaterai,
                    label = { Text("Materai") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.ppn,
                    onValueChange = viewModel::setPpn,
                    label = { Text("PPN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = form.ppj,
                    onValueChange = viewModel::setPpj,
                    label = { Text("PPJ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                )
            }
            OutlinedTextField(
                value = form.totalBayar,
                onValueChange = viewModel::setTotalBayar,
                label = { Text("Total Bayar (kosongkan untuk hitung otomatis)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Total terhitung",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = Format.money(form.totalBayarComputed, settings.currencySymbol),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        FormSection(title = "Referensi") {
            OutlinedTextField(
                value = form.referenceNo,
                onValueChange = viewModel::setReferenceNo,
                label = { Text("No Referensi") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
        }

        SectionHeader(title = "Preview Struk", subtitle = PaperWidth.fromName(settings.paperWidth).displayName)
        ReceiptPreview(
            text = viewModel.previewLive(),
            paperWidth = PaperWidth.fromName(settings.paperWidth),
            modifier = Modifier.height(320.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { viewModel.resetForm() },
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Reset")
            }
            Button(
                onClick = { viewModel.print() },
                enabled = !printing,
                modifier = Modifier.weight(2f).height(48.dp)
            ) {
                if (printing) {
                    CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                } else {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                }
                Text("Cetak Struk")
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ReceiptPreview(text: String, paperWidth: PaperWidth, modifier: Modifier = Modifier) {
    OutlinedSurfaceCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Print,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Preview ${paperWidth.displayName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.small)
                    .background(Color.White)
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFF111827)
                )
            }
        }
    }
}
