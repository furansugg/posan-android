package com.posan.app.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.posan.app.ui.components.SimpleAppBar
import com.posan.app.util.Format

@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val ctx = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.export(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.import(it) } }

    LaunchedEffect(state.message) {
        state.message?.let {
            android.widget.Toast.makeText(ctx, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = { SimpleAppBar(title = "Backup & Restore", onBack = onBack) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Backup", fontWeight = FontWeight.SemiBold)
                    Text("Simpan seluruh data ke file JSON. Anda dapat mengembalikan data ini kapan saja.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { exportLauncher.launch("posan-backup-${Format.date(System.currentTimeMillis()).replace(" ", "-")}.json") },
                        enabled = !state.processing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.processing) CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White, modifier = Modifier.padding(end = 6.dp))
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(Modifier.padding(end = 6.dp))
                        Text("Buat Backup")
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Restore", fontWeight = FontWeight.SemiBold)
                    Text("Pulihkan data dari file backup. Pilih opsi 'Ganti data' untuk menimpa data lokal.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Ganti data lokal", modifier = Modifier.weight(1f))
                        Switch(checked = state.replaceExisting, onCheckedChange = viewModel::toggleReplace)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { importLauncher.launch("application/json") },
                        enabled = !state.processing,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.processing) CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White, modifier = Modifier.padding(end = 6.dp))
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(Modifier.padding(end = 6.dp))
                        Text("Pilih File Backup")
                    }
                }
            }
        }
    }
}
