package com.posan.app.ui.plntoken

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.posan.app.data.local.entity.CustomerEntity
import com.posan.app.domain.model.PaperWidth
import com.posan.app.ui.components.Avatar
import com.posan.app.ui.components.FormSection
import com.posan.app.ui.components.IconBadge
import com.posan.app.ui.components.OutlinedSurfaceCard
import com.posan.app.ui.components.SectionHeader
import com.posan.app.ui.components.SimpleAppBar
import com.posan.app.ui.components.avatarColorFor
import com.posan.app.util.Format

private val NOMINAL_OPTIONS = longArrayOf(20_000L, 50_000L, 100_000L)

@Composable
fun PlnTokenScreen(
    onBack: () -> Unit,
    onManageCustomers: () -> Unit = {},
    onManageTemplates: () -> Unit = {},
    viewModel: PlnTokenViewModel = hiltViewModel()
) {
    val form by viewModel.form.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val printing by viewModel.printing.collectAsState()
    val message by viewModel.message.collectAsState()
    val plnCustomers by viewModel.plnCustomers.collectAsState()
    val ctx = LocalContext.current

    LaunchedEffect(message) {
        message?.let {
            android.widget.Toast.makeText(ctx, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.consumeMessage()
        }
    }

    var showCustomerPicker by remember { mutableStateOf(false) }

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
                        TokenForm(
                            viewModel = viewModel,
                            form = form,
                            printing = printing,
                            plnCustomerCount = plnCustomers.size,
                            onPickCustomer = { showCustomerPicker = true },
                            onManageCustomers = onManageCustomers,
                            onManageTemplates = onManageTemplates
                        )
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxSize().padding(16.dp)) {
                        ReceiptPreview(
                            text = viewModel.previewLive(),
                            paperWidth = PaperWidth.fromName(settings.paperWidth)
                        )
                    }
                }
            } else {
                TokenForm(
                    viewModel = viewModel,
                    form = form,
                    printing = printing,
                    plnCustomerCount = plnCustomers.size,
                    onPickCustomer = { showCustomerPicker = true },
                    onManageCustomers = onManageCustomers,
                    onManageTemplates = onManageTemplates
                )
            }
        }
    }

    if (showCustomerPicker) {
        CustomerPickerDialog(
            customers = plnCustomers,
            onDismiss = { showCustomerPicker = false },
            onPick = { customer ->
                viewModel.selectCustomer(customer)
                showCustomerPicker = false
            },
            onManage = {
                showCustomerPicker = false
                onManageCustomers()
            }
        )
    }
}

@Composable
private fun TokenForm(
    viewModel: PlnTokenViewModel,
    form: PlnTokenFormState,
    printing: Boolean,
    plnCustomerCount: Int,
    onPickCustomer: () -> Unit,
    onManageCustomers: () -> Unit,
    onManageTemplates: () -> Unit,
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
                    Text(
                        "Token Listrik PLN",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Pilih pelanggan & nominal — rincian biaya akan terisi otomatis dari template.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        FormSection(
            title = "Pelanggan",
            subtitle = if (plnCustomerCount > 0) "$plnCustomerCount pelanggan PLN tersedia"
                else "Belum ada pelanggan dengan data PLN"
        ) {
            if (form.selectedCustomerId != null) {
                SelectedCustomerCard(
                    name = form.customerName,
                    idPelanggan = form.customerId,
                    meterNo = form.meterNo,
                    tariff = form.tariff,
                    power = form.power,
                    onChange = onPickCustomer,
                    onClear = { viewModel.clearCustomer() }
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onPickCustomer,
                        enabled = plnCustomerCount > 0,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.People, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Pilih Pelanggan")
                    }
                    OutlinedButton(
                        onClick = onManageCustomers,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Kelola")
                    }
                }
                if (plnCustomerCount == 0) {
                    Text(
                        "Tambah pelanggan dengan data PLN (ID Pelanggan, No Meter, Tarif/Daya) di menu Pelanggan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Manual override fields — useful for one-off transactions where the
            // customer isn't (yet) saved.
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

        FormSection(
            title = "Nominal Token",
            subtitle = "Pilih nominal — rincian otomatis dari template ${form.tariff}/${form.power}"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NOMINAL_OPTIONS.forEach { n ->
                    val selected = form.nominal == n
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (selected) viewModel.clearNominal() else viewModel.selectNominal(n)
                        },
                        label = { Text(Format.money(n.toDouble(), "Rp")) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null,
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors()
                    )
                }
            }
            TextButton(onClick = onManageTemplates) {
                Text("Kelola Template")
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

        FormSection(title = "Rincian Biaya", subtitle = "Bisa di-override manual jika perlu") {
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
                    value = form.ppj,
                    onValueChange = viewModel::setPpj,
                    label = { Text("PPJ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.materai,
                    onValueChange = viewModel::setMaterai,
                    label = { Text("Materai") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = form.ppn,
                    onValueChange = viewModel::setPpn,
                    label = { Text("PPN") },
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

        SectionHeader(
            title = "Preview Struk",
            subtitle = PaperWidth.fromName(settings.paperWidth).displayName
        )
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
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
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
private fun SelectedCustomerCard(
    name: String,
    idPelanggan: String,
    meterNo: String,
    tariff: String,
    power: String,
    onChange: () -> Unit,
    onClear: () -> Unit
) {
    OutlinedSurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(text = name.ifBlank { "?" }, color = avatarColorFor(name.ifBlank { "?" }))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name.ifBlank { "Pelanggan" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    buildString {
                        if (idPelanggan.isNotBlank()) append("ID $idPelanggan")
                        if (meterNo.isNotBlank()) {
                            if (isNotEmpty()) append(" · ")
                            append("Meter $meterNo")
                        }
                    }.ifBlank { "Belum ada data PLN" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (tariff.isNotBlank() || power.isNotBlank()) {
                    Text(
                        listOfNotNull(
                            tariff.takeIf { it.isNotBlank() },
                            power.takeIf { it.isNotBlank() }
                        ).joinToString(" / "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            TextButton(onClick = onChange) { Text("Ganti") }
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = "Hapus pilihan")
            }
        }
    }
}

@Composable
private fun CustomerPickerDialog(
    customers: List<CustomerEntity>,
    onDismiss: () -> Unit,
    onPick: (CustomerEntity) -> Unit,
    onManage: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.People, contentDescription = null) },
        title = { Text("Pilih Pelanggan") },
        text = {
            if (customers.isEmpty()) {
                Text("Belum ada pelanggan dengan data PLN.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.heightIn(max = 480.dp)
                ) {
                    items(customers, key = { it.id }) { c ->
                        OutlinedSurfaceCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(c) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Avatar(text = c.name, color = avatarColorFor(c.name))
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        c.plnNamaLengkap?.takeIf { it.isNotBlank() } ?: c.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val ids = listOfNotNull(
                                        c.plnIdPelanggan?.takeIf { it.isNotBlank() }?.let { "ID $it" },
                                        c.plnMeterNo?.takeIf { it.isNotBlank() }?.let { "Meter $it" }
                                    ).joinToString(" · ")
                                    if (ids.isNotBlank()) {
                                        Text(
                                            ids,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    val td = listOfNotNull(
                                        c.plnTarif?.takeIf { it.isNotBlank() },
                                        c.plnDaya?.takeIf { it.isNotBlank() }
                                    ).joinToString(" / ")
                                    if (td.isNotBlank()) {
                                        Text(
                                            td,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onManage) { Text("Kelola Pelanggan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
private fun ReceiptPreview(text: String, paperWidth: PaperWidth, modifier: Modifier = Modifier) {
    OutlinedSurfaceCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp)
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
