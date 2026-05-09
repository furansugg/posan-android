package com.posan.app.ui.printer

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.posan.app.ui.components.EmptyState
import com.posan.app.ui.components.IconBadge
import com.posan.app.ui.components.ListItemCard
import com.posan.app.ui.components.SimpleAppBar
import com.posan.app.ui.components.StatusPill
import com.posan.app.ui.theme.Brand500
import com.posan.app.ui.theme.Danger500
import com.posan.app.ui.theme.Success500
import com.posan.app.ui.theme.Warning500

@Composable
fun PrinterDeviceScreen(
    onBack: () -> Unit,
    viewModel: PrinterDeviceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val ctx = LocalContext.current

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> viewModel.refresh() }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            )
        }
        viewModel.load()
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            android.widget.Toast.makeText(ctx, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            SimpleAppBar(
                title = "Printer Bluetooth",
                subtitle = "Pilih perangkat untuk cetak struk",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Muat ulang")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!state.bluetoothEnabled) {
                NoticeCard(
                    icon = Icons.Default.BluetoothDisabled,
                    title = "Bluetooth nonaktif",
                    message = "Aktifkan Bluetooth pada perangkat untuk menampilkan daftar printer",
                    tint = Danger500
                )
            }
            if (state.needPermission) {
                NoticeCard(
                    icon = Icons.Default.Bluetooth,
                    title = "Izin Bluetooth diperlukan",
                    message = "Berikan izin agar aplikasi dapat memindai dan menyambungkan printer",
                    tint = Warning500,
                    action = "Berikan izin",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            permLauncher.launch(
                                arrayOf(
                                    Manifest.permission.BLUETOOTH_CONNECT,
                                    Manifest.permission.BLUETOOTH_SCAN
                                )
                            )
                        }
                    }
                )
            }
            if (state.devices.isEmpty()) {
                EmptyState(
                    title = "Belum ada perangkat dipair",
                    subtitle = "Pair printer di pengaturan Bluetooth lalu tekan Refresh",
                    icon = Icons.Default.Bluetooth
                )
            } else {
                Text(
                    "Perangkat ter-pair (${state.devices.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.devices, key = { it.address }) { device ->
                        val isSelected = state.selectedAddress == device.address
                        ListItemCard(
                            leading = { IconBadge(icon = Icons.Default.Bluetooth, tint = Brand500) },
                            title = device.name,
                            subtitle = device.address,
                            extra = if (isSelected) {
                                {
                                    StatusPill(label = "Dipilih", color = Success500)
                                }
                            } else null,
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(
                                        onClick = { viewModel.testPrint(device) },
                                        enabled = !state.testing
                                    ) { Text("Test") }
                                    Spacer(Modifier.width(6.dp))
                                    Button(onClick = { viewModel.choose(device) }) { Text("Pilih") }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    tint: androidx.compose.ui.graphics.Color,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(icon = icon, tint = tint)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (action != null && onAction != null) {
                Button(onClick = onAction) { Text(action) }
            }
        }
    }
}
