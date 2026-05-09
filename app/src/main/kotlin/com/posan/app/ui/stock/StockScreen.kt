package com.posan.app.ui.stock

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.posan.app.domain.model.StockMovementType
import com.posan.app.ui.components.Avatar
import com.posan.app.ui.components.EmptyState
import com.posan.app.ui.components.IconBadge
import com.posan.app.ui.components.ListItemCard
import com.posan.app.ui.components.OutlinedSurfaceCard
import com.posan.app.ui.components.SimpleAppBar
import com.posan.app.ui.components.StatusPill
import com.posan.app.ui.components.avatarColorFor
import com.posan.app.ui.theme.Danger500
import com.posan.app.ui.theme.Success500
import com.posan.app.ui.theme.Warning500
import com.posan.app.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(
    onBack: () -> Unit,
    viewModel: StockViewModel = hiltViewModel()
) {
    val products by viewModel.products.collectAsState()
    val movements by viewModel.movements.collectAsState()
    val state by viewModel.state.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            SimpleAppBar(
                title = "Stok",
                subtitle = if (tab == 0) "Daftar produk" else "Riwayat pergerakan",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SecondaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Produk") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Pergerakan") })
            }
            if (tab == 0) {
                if (products.isEmpty()) {
                    EmptyState(
                        title = "Belum ada produk",
                        subtitle = "Tambahkan produk dari menu Produk",
                        icon = Icons.Default.Inventory2
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        items(products, key = { it.id }) { product ->
                            ListItemCard(
                                leading = { Avatar(text = product.name, color = avatarColorFor(product.name)) },
                                title = product.name,
                                subtitle = "SKU: ${product.sku}",
                                extra = {
                                    val color = when {
                                        product.stock <= 0 -> Danger500
                                        product.stock <= 5 -> Warning500
                                        else -> Success500
                                    }
                                    StatusPill(label = "${product.stock} ${product.unit}", color = color)
                                },
                                trailing = {
                                    TextButton(onClick = { viewModel.openAdjust(product) }) {
                                        Text("Atur")
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                if (movements.isEmpty()) {
                    EmptyState(
                        title = "Belum ada pergerakan stok",
                        subtitle = "Riwayat IN/OUT/Adjustment akan muncul di sini",
                        icon = Icons.Default.MoveToInbox
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        items(movements, key = { it.id }) { mv ->
                            val productName = products.firstOrNull { it.id == mv.productId }?.name ?: "Produk"
                            val type = StockMovementType.fromName(mv.type)
                            val (icon, tint) = when (type) {
                                StockMovementType.IN -> Icons.Default.MoveToInbox to Success500
                                StockMovementType.OUT -> Icons.Default.Outbox to Danger500
                                StockMovementType.ADJUSTMENT -> Icons.Default.Tune to Warning500
                                StockMovementType.SALE -> Icons.Default.Outbox to Danger500
                            }
                            OutlinedSurfaceCard {
                                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    IconBadge(icon = icon, tint = tint)
                                    Spacer(Modifier.padding(end = 12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(productName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                                        Text(
                                            "${type.displayName} · ${Format.datetime(mv.createdAt)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (!mv.note.isNullOrBlank()) {
                                            Text(
                                                mv.note!!,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        "${if (mv.quantity >= 0) "+" else ""}${mv.quantity}",
                                        color = if (mv.quantity >= 0) Success500 else Danger500,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showAdjust) {
        AlertDialog(
            onDismissRequest = viewModel::closeAdjust,
            icon = { Icon(Icons.Default.Tune, contentDescription = null) },
            title = { Text("Atur Stok") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        state.product?.name.orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            StockMovementType.IN,
                            StockMovementType.OUT,
                            StockMovementType.ADJUSTMENT
                        ).forEach { t ->
                            FilterChip(
                                selected = state.type == t,
                                onClick = { viewModel.setType(t) },
                                label = { Text(t.displayName) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = state.quantity,
                        onValueChange = viewModel::setQuantity,
                        label = { Text(if (state.type == StockMovementType.ADJUSTMENT) "Stok akhir" else "Jumlah") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        value = state.note,
                        onValueChange = viewModel::setNote,
                        label = { Text("Catatan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    if (!state.error.isNullOrBlank()) {
                        Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = { Button(onClick = { viewModel.submit() }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = viewModel::closeAdjust) { Text("Batal") } }
        )
    }
}
