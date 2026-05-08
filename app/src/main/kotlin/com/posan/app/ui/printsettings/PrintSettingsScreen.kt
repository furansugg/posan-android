package com.posan.app.ui.printsettings

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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
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
import com.posan.app.ui.components.FormSection
import com.posan.app.ui.components.IconBadge
import com.posan.app.ui.components.OutlinedSurfaceCard
import com.posan.app.ui.components.SectionHeader
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
                subtitle = "Header, layout, dan printer",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.testPrint() }, enabled = !testing) {
                        if (testing) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(Icons.Default.Print, contentDescription = "Test print")
                        }
                    }
                    IconButton(onClick = { viewModel.save() }, enabled = !saving) {
                        if (saving) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = "Simpan")
                        }
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
                        SettingsForm(
                            viewModel = viewModel,
                            draft = draft,
                            saving = saving,
                            onPickDevice = onPickDevice
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        ReceiptPreview(
                            text = viewModel.previewLive(),
                            paperWidth = PaperWidth.fromName(draft.paperWidth)
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    SettingsForm(
                        viewModel = viewModel,
                        draft = draft,
                        saving = saving,
                        onPickDevice = onPickDevice,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsForm(
    viewModel: PrintSettingsViewModel,
    draft: com.posan.app.data.local.entity.PrintSettingsEntity,
    saving: Boolean,
    onPickDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedSurfaceCard {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon = Icons.Default.Bluetooth)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Printer Bluetooth", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = draft.savedDeviceName ?: "Belum dipilih",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    draft.savedDeviceAddress?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedButton(onClick = onPickDevice) { Text("Pilih") }
            }
        }

        FormSection(title = "Profil Toko", subtitle = "Tampil di header struk") {
            OutlinedTextField(
                draft.storeName,
                viewModel::setStoreName,
                label = { Text("Nama toko") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            OutlinedTextField(
                draft.storeAddress,
                viewModel::setStoreAddress,
                label = { Text("Alamat (multi baris)") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            OutlinedTextField(
                draft.storePhone,
                viewModel::setStorePhone,
                label = { Text("Telepon") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            OutlinedTextField(
                draft.headerText,
                viewModel::setHeader,
                label = { Text("Header tambahan") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            OutlinedTextField(
                draft.footerText,
                viewModel::setFooter,
                label = { Text("Footer / pesan terima kasih") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
        }

        FormSection(title = "Layout", subtitle = "Lebar kertas dan posisi header") {
            Column {
                Text("Lebar kertas", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PaperWidth.entries.forEach { p ->
                        FilterChip(
                            selected = draft.paperWidth == p.name,
                            onClick = { viewModel.setPaperWidth(p) },
                            label = { Text(p.displayName) }
                        )
                    }
                }
            }
            Column {
                Text("Alignment header", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PrintAlignment.entries.forEach { a ->
                        FilterChip(
                            selected = draft.titleAlignment == a.name,
                            onClick = { viewModel.setAlignment(a) },
                            label = { Text(a.displayName) }
                        )
                    }
                }
            }
        }

        FormSection(title = "Gaya Teks") {
            Toggle("Header tebal", draft.titleBold, viewModel::setTitleBold)
            Toggle("Header ukuran ganda", draft.titleDoubleSize, viewModel::setTitleDouble)
            Toggle("Font isi kecil", draft.bodyFontSmall, viewModel::setBodySmall)
        }

        FormSection(title = "Konten Struk") {
            Toggle("Tampilkan logo", draft.showLogo, viewModel::setShowLogo)
            Toggle("Tampilkan kasir", draft.showCashier, viewModel::setShowCashier)
            Toggle("Tampilkan pelanggan", draft.showCustomer, viewModel::setShowCustomer)
            Toggle("Tampilkan SKU di item", draft.showItemSku, viewModel::setShowItemSku)
            Toggle("Potong kertas", draft.cutPaper, viewModel::setCutPaper)
            Toggle("Buka cash drawer", draft.openCashDrawer, viewModel::setOpenDrawer)
        }

        FormSection(title = "Lainnya", subtitle = "Mata uang, salinan, pajak default") {
            OutlinedTextField(
                value = draft.printCopies.toString(),
                onValueChange = { v -> viewModel.setCopies(v.toIntOrNull() ?: 1) },
                label = { Text("Jumlah salinan (1-5)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            OutlinedTextField(
                value = if (draft.taxPercentDefault == 0.0) "" else draft.taxPercentDefault.toString(),
                onValueChange = { v -> viewModel.setTaxDefault(v.toDoubleOrNull() ?: 0.0) },
                label = { Text("Pajak default (%)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            OutlinedTextField(
                value = draft.currencySymbol,
                onValueChange = viewModel::setCurrency,
                label = { Text("Simbol mata uang") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
        }

        SectionHeader(title = "Preview Struk", subtitle = "${PaperWidth.fromName(draft.paperWidth).displayName}")
        ReceiptPreview(
            text = viewModel.previewLive(),
            paperWidth = PaperWidth.fromName(draft.paperWidth),
            modifier = Modifier.height(280.dp)
        )

        Button(
            onClick = { viewModel.save() },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            if (saving) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text("Simpan Pengaturan")
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ReceiptPreview(text: String, paperWidth: PaperWidth, modifier: Modifier = Modifier) {
    OutlinedSurfaceCard(modifier = modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
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
