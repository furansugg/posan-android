package com.posan.app.ui.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.posan.app.data.local.entity.ProductEntity
import com.posan.app.domain.model.PaymentMethod
import com.posan.app.ui.components.SimpleAppBar
import com.posan.app.util.Format
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    onBack: () -> Unit,
    viewModel: PosViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeError()
        }
    }
    LaunchedEffect(state.printingState) {
        when (val ps = state.printingState) {
            is PrintingState.Success -> {
                Toast.makeText(context, ps.message, Toast.LENGTH_SHORT).show()
                viewModel.consumePrint()
            }
            is PrintingState.Error -> {
                Toast.makeText(context, ps.message, Toast.LENGTH_LONG).show()
                viewModel.consumePrint()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            SimpleAppBar(
                title = "Kasir",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.toggleCustomerPicker(true) }) {
                        Icon(Icons.Default.Person, contentDescription = "Pelanggan")
                    }
                }
            )
        }
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.weight(1.6f).fillMaxSize().padding(8.dp)) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = { Text("Cari nama, SKU atau barcode") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.selectedCategoryId == null,
                            onClick = { viewModel.setCategory(null) },
                            label = { Text("Semua") }
                        )
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = state.selectedCategoryId == cat.id,
                            onClick = { viewModel.setCategory(cat.id) },
                            label = { Text(cat.name) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(products) { product ->
                        ProductCard(product = product, onClick = { viewModel.addToCart(product) })
                    }
                }
            }
            CartPanel(
                state = state,
                onIncrease = viewModel::increaseQty,
                onDecrease = viewModel::decreaseQty,
                onRemove = viewModel::removeFromCart,
                onClear = viewModel::clearCart,
                onCheckout = { viewModel.toggleCheckout(true) },
                modifier = Modifier.weight(1f).fillMaxSize().padding(8.dp)
            )
        }
    }

    if (state.showCheckout) {
        CheckoutDialog(
            state = state,
            onDismiss = { viewModel.toggleCheckout(false) },
            onSetMethod = viewModel::setPaymentMethod,
            onSetReceived = viewModel::setPaymentReceived,
            onSetDiscount = viewModel::setOrderDiscount,
            onSetTax = viewModel::setTaxPercent,
            onSetNote = viewModel::setNote,
            onConfirm = { viewModel.checkout() }
        )
    }

    state.lastResult?.let { res ->
        ReceiptResultDialog(
            total = res.total,
            change = res.change,
            code = res.code,
            printing = state.printingState is PrintingState.Sending,
            onPrint = { viewModel.printLastReceipt() },
            onDone = { viewModel.consumeResult() }
        )
    }

    if (state.showCustomerPicker) {
        CustomerPickerDialog(
            customers = customers,
            selected = state.customer?.id,
            onPick = viewModel::setCustomer,
            onDismiss = { viewModel.toggleCustomerPicker(false) }
        )
    }
}

@Composable
private fun ProductCard(product: ProductEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = product.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = product.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = Format.money(product.price),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Stok: ${product.stock}",
                style = MaterialTheme.typography.bodySmall,
                color = if (product.stock <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CartPanel(
    state: PosUiState,
    onIncrease: (Long) -> Unit,
    onDecrease: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onClear: () -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Keranjang", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                if (state.cart.isNotEmpty()) {
                    TextButton(onClick = onClear) { Text("Hapus") }
                }
            }
            state.customer?.let {
                Spacer(Modifier.height(4.dp))
                AssistChip(onClick = {}, label = { Text(it.name) }, leadingIcon = { Icon(Icons.Default.Person, null) })
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            if (state.cart.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Belum ada item", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.cart, key = { it.product.id }) { line ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(line.product.name, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = "${Format.money(line.product.price)} x ${line.quantity}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDecrease(line.product.id) }) {
                                Icon(Icons.Default.Remove, contentDescription = "kurang")
                            }
                            Text(line.quantity.toString(), fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onIncrease(line.product.id) }) {
                                Icon(Icons.Default.Add, contentDescription = "tambah")
                            }
                            IconButton(onClick = { onRemove(line.product.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "hapus")
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row {
                Text("Subtotal", modifier = Modifier.weight(1f))
                Text(Format.money(state.subtotal))
            }
            if (state.orderDiscount > 0) {
                Row {
                    Text("Diskon", modifier = Modifier.weight(1f))
                    Text("-${Format.money(state.orderDiscount)}")
                }
            }
            if (state.taxPercent > 0) {
                Row {
                    Text("Pajak ${state.taxPercent}%", modifier = Modifier.weight(1f))
                    Text(Format.money(state.tax))
                }
            }
            Spacer(Modifier.height(4.dp))
            Row {
                Text("Total", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(Format.money(state.total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onCheckout,
                enabled = state.cart.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Bayar") }
        }
    }
}

@Composable
private fun CheckoutDialog(
    state: PosUiState,
    onDismiss: () -> Unit,
    onSetMethod: (PaymentMethod) -> Unit,
    onSetReceived: (Double) -> Unit,
    onSetDiscount: (Double) -> Unit,
    onSetTax: (Double) -> Unit,
    onSetNote: (String) -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pembayaran") },
        text = {
            Column {
                Row {
                    PaymentMethod.entries.forEach { method ->
                        SuggestionChip(
                            onClick = { onSetMethod(method) },
                            label = { Text(method.displayName) },
                            modifier = Modifier.padding(end = 4.dp),
                            colors = if (state.paymentMethod == method) {
                                androidx.compose.material3.SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    labelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            } else androidx.compose.material3.SuggestionChipDefaults.suggestionChipColors()
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                NumberField(label = "Diskon (Rp)", value = state.orderDiscount, onChange = onSetDiscount)
                Spacer(Modifier.height(8.dp))
                NumberField(label = "Pajak (%)", value = state.taxPercent, onChange = onSetTax, max = 100.0)
                Spacer(Modifier.height(8.dp))
                Row {
                    Text("Total", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text(Format.money(state.total), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                if (state.paymentMethod == PaymentMethod.CASH) {
                    Spacer(Modifier.height(8.dp))
                    NumberField(label = "Bayar (Rp)", value = state.paymentReceived, onChange = onSetReceived)
                    Row {
                        Text("Kembalian", modifier = Modifier.weight(1f))
                        Text(Format.money(state.change), color = if (state.paymentReceived >= state.total) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.note,
                    onValueChange = onSetNote,
                    label = { Text("Catatan (opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !state.processing && (state.paymentMethod != PaymentMethod.CASH || state.paymentReceived >= state.total)
            ) {
                if (state.processing) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text("Konfirmasi")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun ReceiptResultDialog(
    total: Double,
    change: Double,
    code: String,
    printing: Boolean,
    onPrint: () -> Unit,
    onDone: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text("Pembayaran berhasil") },
        text = {
            Column {
                Text("Kode: $code", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Row {
                    Text("Total", modifier = Modifier.weight(1f))
                    Text(Format.money(total), fontWeight = FontWeight.Bold)
                }
                if (change > 0) {
                    Row {
                        Text("Kembalian", modifier = Modifier.weight(1f))
                        Text(Format.money(change), color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onPrint, enabled = !printing, colors = ButtonDefaults.buttonColors()) {
                if (printing) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Cetak")
            }
        },
        dismissButton = { TextButton(onClick = onDone) { Text("Selesai") } }
    )
}

@Composable
private fun CustomerPickerDialog(
    customers: List<com.posan.app.data.local.entity.CustomerEntity>,
    selected: Long?,
    onPick: (com.posan.app.data.local.entity.CustomerEntity?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Pelanggan") },
        text = {
            Column(modifier = Modifier.heightIn(max = 320.dp)) {
                LazyColumn {
                    item {
                        TextButton(onClick = { onPick(null) }) { Text("Tanpa pelanggan") }
                    }
                    items(customers) { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(c) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) { Text(c.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?") }
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(c.name, fontWeight = FontWeight.Medium)
                                if (!c.phone.isNullOrBlank()) {
                                    Text(c.phone!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (selected == c.id) {
                                Text("Dipilih", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } }
    )
}

@Composable
private fun NumberField(
    label: String,
    value: Double,
    onChange: (Double) -> Unit,
    max: Double = Double.MAX_VALUE
) {
    OutlinedTextField(
        value = if (value == 0.0) "" else value.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() },
        onValueChange = { txt ->
            val cleaned = txt.replace(",", ".").filter { it.isDigit() || it == '.' }
            val parsed = cleaned.toDoubleOrNull() ?: 0.0
            onChange(parsed.coerceAtMost(max))
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}
