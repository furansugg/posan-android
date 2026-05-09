package com.posan.app.ui.plntemplates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.posan.app.data.local.entity.PlnTokenTemplateEntity
import com.posan.app.ui.components.EmptyState
import com.posan.app.ui.components.IconBadge
import com.posan.app.ui.components.ListItemCard
import com.posan.app.ui.components.SimpleAppBar
import com.posan.app.util.Format

@Composable
fun PlnTokenTemplatesScreen(
    onBack: () -> Unit,
    viewModel: PlnTokenTemplatesViewModel = hiltViewModel()
) {
    val templates by viewModel.templates.collectAsState()
    val form by viewModel.form.collectAsState()
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
                title = "Template Token PLN",
                subtitle = if (templates.isEmpty()) "Belum ada template"
                    else "${templates.size} template",
                onBack = onBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openForm() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Tambah") }
            )
        }
    ) { padding ->
        if (templates.isEmpty()) {
            EmptyState(
                title = "Belum ada template",
                subtitle = "Tambahkan template breakdown nominal token (kWh, admin, PPJ, dll) per kombinasi tarif/daya.",
                icon = Icons.Default.Bolt,
                actionLabel = "Tambah Template",
                onAction = { viewModel.openForm() },
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(templates, key = { it.id }) { t ->
                    ListItemCard(
                        leading = {
                            IconBadge(
                                icon = Icons.Default.Bolt,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        title = "${t.tarif} / ${t.daya} · ${Format.money(t.nominalRupiah.toDouble(), "Rp")}",
                        subtitle = buildString {
                            append("${trimZero(t.kwh)} kWh")
                            append(" · Stroom ")
                            append(Format.money(t.rpStroom, "Rp"))
                            if (t.adminFee > 0) append(" · Admin ${Format.money(t.adminFee, "Rp")}")
                            if (t.ppj > 0) append(" · PPJ ${Format.money(t.ppj, "Rp")}")
                        },
                        trailing = {
                            IconButton(onClick = { viewModel.openForm(t) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { viewModel.delete(t) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Hapus",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    if (form.show) {
        TemplateFormDialog(viewModel = viewModel)
    }
}

@Composable
private fun TemplateFormDialog(viewModel: PlnTokenTemplatesViewModel) {
    val form by viewModel.form.collectAsState()
    AlertDialog(
        onDismissRequest = viewModel::closeForm,
        icon = { Icon(Icons.Default.Bolt, contentDescription = null) },
        title = { Text(if (form.editing == null) "Template Baru" else "Edit Template") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = form.tarif,
                        onValueChange = viewModel::setTarif,
                        label = { Text("Tarif (R1, R1M)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        value = form.daya,
                        onValueChange = viewModel::setDaya,
                        label = { Text("Daya (900VA)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    )
                }
                OutlinedTextField(
                    value = form.nominal,
                    onValueChange = viewModel::setNominal,
                    label = { Text("Nominal Token (Rp)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(20_000, 50_000, 100_000).forEach { n ->
                        AssistChip(
                            onClick = { viewModel.setNominal(n.toString()) },
                            label = { Text("${n / 1000}k") },
                            colors = AssistChipDefaults.assistChipColors()
                        )
                    }
                }
                OutlinedTextField(
                    value = form.rpStroom,
                    onValueChange = viewModel::setRpStroom,
                    label = { Text("Rp Stroom") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Total estimasi",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    val total = (form.rpStroom.toDoubleOrNull() ?: 0.0) +
                        (form.adminFee.toDoubleOrNull() ?: 0.0) +
                        (form.materai.toDoubleOrNull() ?: 0.0) +
                        (form.ppn.toDoubleOrNull() ?: 0.0) +
                        (form.ppj.toDoubleOrNull() ?: 0.0)
                    Text(
                        Format.money(total, "Rp"),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (!form.error.isNullOrBlank()) {
                    Text(
                        form.error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { viewModel.save() }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = viewModel::closeForm) { Text("Batal") } }
    )
}

private fun trimZero(v: Double): String {
    if (v == 0.0) return "0"
    val asInt = v.toLong()
    return if (asInt.toDouble() == v) asInt.toString() else v.toString()
}

private fun PlnTokenTemplateEntity.summary(): String =
    "$tarif/$daya · ${nominalRupiah / 1000}k"
