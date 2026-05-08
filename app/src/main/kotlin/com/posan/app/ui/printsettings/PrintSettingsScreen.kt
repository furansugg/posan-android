package com.posan.app.ui.printsettings

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.posan.app.domain.model.PrintAlignment
import com.posan.app.ui.components.SimpleAppBar

@Composable
fun PrintSettingsScreen(
    onBack: () -> Unit,
    onPickDevice: () -> Unit,
    viewModel: PrintSettingsViewModel = hiltViewModel()
) {
    val draft by viewModel.draft.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val testing by viewModel.testing.collectAsState()
    val message by viewModel.message.collectAsState()
    val ctx = LocalContext.current

    LaunchedEffect(Unit) { viewModel.reset() }
    LaunchedEffect(message) {
        message?.let {
            android.widget.Toast.makeText(ctx, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            SimpleAppBar(
                title = "Pengaturan Cetak",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.testPrint() }, enabled = !testing) {
                        if (testing) {
                            CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Default.Print, contentDescription = "Test print")
                        }
                    }
                    IconButton(onClick = { viewModel.save() }, enabled = !saving) {
                        if (saving) {
                            CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Default.Save, contentDescription = "Simpan")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
            ) {
                Text("Profil Toko", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(draft.storeName, viewModel::setStoreName, label = { Text("Nama toko") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(draft.storeAddress, viewModel::setStoreAddress, label = { Text("Alamat (multi baris diizinkan)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(draft.storePhone, viewModel::setStorePhone, label = { Text("Telepon") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(draft.headerText, viewModel::setHeader, label = { Text("Header tambahan") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(draft.footerText, viewModel::setFooter, label = { Text("Footer / pesan terima kasih") }, modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(16.dp))
                Text("Layout", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("Lebar kertas")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PaperWidth.entries.forEach { p ->
                        FilterChip(
                            selected = draft.paperWidth == p.name,
                            onClick = { viewModel.setPaperWidth(p) },
                            label = { Text(p.displayName) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Posisi judul/header")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PrintAlignment.entries.forEach { a ->
                        FilterChip(
                            selected = draft.titleAlignment == a.name,
                            onClick = { viewModel.setAlignment(a) },
                            label = { Text(a.displayName) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Toggle("Header tebal", draft.titleBold, viewModel::setTitleBold)
                Toggle("Header ukuran ganda", draft.titleDoubleSize, viewModel::setTitleDouble)
                Toggle("Font isi kecil", draft.bodyFontSmall, viewModel::setBodySmall)
                Toggle("Tampilkan logo", draft.showLogo, viewModel::setShowLogo)
                Toggle("Tampilkan kasir", draft.showCashier, viewModel::setShowCashier)
                Toggle("Tampilkan pelanggan", draft.showCustomer, viewModel::setShowCustomer)
                Toggle("Tampilkan SKU di item", draft.showItemSku, viewModel::setShowItemSku)
                Toggle("Potong kertas", draft.cutPaper, viewModel::setCutPaper)
                Toggle("Buka cash drawer", draft.openCashDrawer, viewModel::setOpenDrawer)

                Spacer(Modifier.height(16.dp))
                Text("Lainnya", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = draft.printCopies.toString(),
                    onValueChange = { v -> viewModel.setCopies(v.toIntOrNull() ?: 1) },
                    label = { Text("Jumlah salinan (1-5)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = if (draft.taxPercentDefault == 0.0) "" else draft.taxPercentDefault.toString(),
                    onValueChange = { v -> viewModel.setTaxDefault(v.toDoubleOrNull() ?: 0.0) },
                    label = { Text("Pajak default (%)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = draft.currencySymbol,
                    onValueChange = viewModel::setCurrency,
                    label = { Text("Simbol mata uang") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Printer Bluetooth", fontWeight = FontWeight.Medium)
                                Text(
                                    text = draft.savedDeviceName ?: "Belum dipilih",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                draft.savedDeviceAddress?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            OutlinedButton(onClick = onPickDevice) { Text("Pilih") }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.save() },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (saving) CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White, modifier = Modifier.size(16.dp).padding(end = 6.dp))
                    Text("Simpan Pengaturan")
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxSize().padding(16.dp)) {
                ReceiptPreview(text = viewModel.previewLive(), paperWidth = PaperWidth.fromName(draft.paperWidth))
            }
        }
    }
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ReceiptPreview(text: String, paperWidth: PaperWidth) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Text("Preview ${paperWidth.displayName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFF212529)
                )
            }
        }
    }
}
