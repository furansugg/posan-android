package com.posan.app.ui.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.posan.app.domain.model.StockMovementType
import com.posan.app.ui.components.SimpleAppBar
import com.posan.app.util.Format

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
        topBar = { SimpleAppBar(title = "Stok", onBack = onBack) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Produk") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Pergerakan") })
            }
            if (tab == 0) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    items(products, key = { it.id }) { product ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.Medium)
                                    Text("SKU: ${product.sku}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    "${product.stock} ${product.unit}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (product.stock <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = { viewModel.openAdjust(product) }) { Text("Atur") }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    items(movements, key = { it.id }) { mv ->
                        val productName = products.firstOrNull { it.id == mv.productId }?.name ?: "Produk"
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Row {
                                Text(productName, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                Text(
                                    "${if (mv.quantity >= 0) "+" else ""}${mv.quantity}",
                                    color = if (mv.quantity >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row {
                                Text(
                                    StockMovementType.fromName(mv.type).displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(Format.datetime(mv.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (!mv.note.isNullOrBlank()) Text(mv.note!!, style = MaterialTheme.typography.bodySmall)
                            HorizontalDivider(Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }

    if (state.showAdjust) {
        AlertDialog(
            onDismissRequest = viewModel::closeAdjust,
            title = { Text("Atur stok ${state.product?.name.orEmpty()}") },
            text = {
                Column {
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
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.quantity,
                        onValueChange = viewModel::setQuantity,
                        label = { Text(if (state.type == StockMovementType.ADJUSTMENT) "Stok akhir" else "Jumlah") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.note,
                        onValueChange = viewModel::setNote,
                        label = { Text("Catatan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!state.error.isNullOrBlank()) Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = { Button(onClick = { viewModel.submit() }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = viewModel::closeAdjust) { Text("Batal") } }
        )
    }
}
