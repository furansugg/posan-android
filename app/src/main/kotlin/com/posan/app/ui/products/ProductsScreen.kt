package com.posan.app.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.posan.app.ui.components.Avatar
import com.posan.app.ui.components.EmptyState
import com.posan.app.ui.components.ListItemCard
import com.posan.app.ui.components.SimpleAppBar
import com.posan.app.ui.components.StatusPill
import com.posan.app.ui.components.avatarColorFor
import com.posan.app.ui.theme.Danger500
import com.posan.app.ui.theme.Success500
import com.posan.app.ui.theme.Warning500
import com.posan.app.util.Format

@Composable
fun ProductsScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: ProductsViewModel = hiltViewModel()
) {
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val query by viewModel.query.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    var pendingDelete by remember { mutableStateOf<com.posan.app.data.local.entity.ProductEntity?>(null) }

    Scaffold(
        topBar = {
            SimpleAppBar(
                title = "Produk",
                subtitle = if (products.isEmpty()) "Belum ada produk" else "${products.size} produk",
                onBack = onBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Tambah") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.query.value = it },
                placeholder = { Text("Cari nama / SKU / barcode") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { viewModel.selectedCategoryId.value = null },
                        label = { Text("Semua") }
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = { viewModel.selectedCategoryId.value = cat.id },
                        label = { Text(cat.name) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            if (products.isEmpty()) {
                EmptyState(
                    title = "Belum ada produk",
                    subtitle = "Tambahkan produk pertama untuk mulai berjualan",
                    icon = Icons.Default.ShoppingBag,
                    actionLabel = "Tambah Produk",
                    onAction = onAdd
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        ListItemCard(
                            leading = {
                                Avatar(text = product.name, color = avatarColorFor(product.name))
                            },
                            title = product.name,
                            subtitle = buildString {
                                append("SKU: ${product.sku}")
                                if (!product.barcode.isNullOrBlank()) append(" · ${product.barcode}")
                            },
                            extra = {
                                Column {
                                    Text(
                                        text = Format.money(product.price),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    val stockColor = when {
                                        product.stock <= 0 -> Danger500
                                        product.stock <= 5 -> Warning500
                                        else -> Success500
                                    }
                                    StatusPill(
                                        label = "Stok ${product.stock} ${product.unit}",
                                        color = stockColor
                                    )
                                }
                            },
                            trailing = {
                                IconButton(onClick = { onEdit(product.id) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { pendingDelete = product }) {
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
    }

    pendingDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
            title = { Text("Hapus produk?") },
            text = { Text("Yakin menghapus ${product.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(product)
                    pendingDelete = null
                }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Batal") }
            }
        )
    }
}
