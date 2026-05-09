package com.posan.app.ui.pos

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.posan.app.data.local.entity.CustomerEntity
import com.posan.app.data.local.entity.ProductEntity
import com.posan.app.domain.model.PaymentMethod
import com.posan.app.ui.components.Avatar
import com.posan.app.ui.components.EmptyState
import com.posan.app.ui.components.IconBadge
import com.posan.app.ui.components.OutlinedSurfaceCard
import com.posan.app.ui.components.SectionHeader
import com.posan.app.ui.components.SimpleAppBar
import com.posan.app.ui.components.StatusPill
import com.posan.app.ui.components.avatarColorFor
import com.posan.app.ui.theme.Brand500
import com.posan.app.ui.theme.Danger500
import com.posan.app.ui.theme.Success500
import com.posan.app.ui.theme.Warning500
import com.posan.app.util.Format
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()
    var showCart by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

    val cartCount = state.cart.sumOf { it.quantity }

    Scaffold(
        topBar = {
            SimpleAppBar(
                title = "Kasir",
                subtitle = state.customer?.let { "Pelanggan: ${it.name}" } ?: "Tambahkan produk ke keranjang",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.toggleCustomerPicker(true) }) {
                        Icon(Icons.Default.Person, contentDescription = "Pelanggan")
                    }
                }
            )
        }
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
            val isWide = maxWidth >= 720.dp
            if (isWide) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1.6f).fillMaxHeight()) {
                        ProductBrowser(
                            state = state,
                            products = products,
                            categories = categories,
                            onQuery = viewModel::setQuery,
                            onCategory = viewModel::setCategory,
                            onAdd = { viewModel.addToCart(it) }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(380.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        CartPanel(
                            state = state,
                            onIncrease = viewModel::increaseQty,
                            onDecrease = viewModel::decreaseQty,
                            onRemove = viewModel::removeFromCart,
                            onClear = viewModel::clearCart,
                            onCheckout = { viewModel.toggleCheckout(true) }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    ProductBrowser(
                        state = state,
                        products = products,
                        categories = categories,
                        onQuery = viewModel::setQuery,
                        onCategory = viewModel::setCategory,
                        onAdd = { viewModel.addToCart(it) }
                    )
                    if (state.cart.isNotEmpty()) {
                        ExtendedFloatingActionButton(
                            onClick = { showCart = true },
                            containerColor = Brand500,
                            contentColor = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(20.dp),
                            text = {
                                Text(
                                    "${cartCount} item · ${Format.money(state.total)}",
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            icon = {
                                BadgedBox(badge = { Badge { Text(cartCount.toString()) } }) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                                }
                            }
                        )
                    }
                }
                if (showCart) {
                    ModalBottomSheet(
                        onDismissRequest = { showCart = false },
                        sheetState = sheetState,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        CartPanel(
                            state = state,
                            onIncrease = viewModel::increaseQty,
                            onDecrease = viewModel::decreaseQty,
                            onRemove = viewModel::removeFromCart,
                            onClear = {
                                viewModel.clearCart()
                                scope.launch { sheetState.hide() }
                                showCart = false
                            },
                            onCheckout = {
                                showCart = false
                                viewModel.toggleCheckout(true)
                            },
                            modifier = Modifier.heightIn(min = 260.dp, max = 560.dp)
                        )
                    }
                }
            }
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
private fun ProductBrowser(
    state: PosUiState,
    products: List<ProductEntity>,
    categories: List<com.posan.app.data.local.entity.CategoryEntity>,
    onQuery: (String) -> Unit,
    onCategory: (Long?) -> Unit,
    onAdd: (ProductEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQuery,
            placeholder = { Text("Cari nama, SKU, atau barcode") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { onQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Bersihkan")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = state.selectedCategoryId == null,
                    onClick = { onCategory(null) },
                    label = { Text("Semua") }
                )
            }
            items(categories, key = { it.id }) { cat ->
                FilterChip(
                    selected = state.selectedCategoryId == cat.id,
                    onClick = { onCategory(cat.id) },
                    label = { Text(cat.name) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (products.isEmpty()) {
            EmptyState(
                title = if (state.query.isBlank()) "Belum ada produk" else "Tidak ada hasil",
                subtitle = if (state.query.isBlank())
                    "Tambahkan produk dari menu Manajemen Produk"
                else "Coba kata kunci lain atau ubah kategori",
                icon = Icons.Default.Search
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 168.dp),
                contentPadding = PaddingValues(bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(products, key = { it.id }) { product ->
                    ProductCard(product = product, onClick = { onAdd(product) })
                }
            }
        }
    }
}

@Composable
private fun ProductCard(product: ProductEntity, onClick: () -> Unit) {
    val color = avatarColorFor(product.name)
    val outOfStock = product.stock <= 0
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 168.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        enabled = !outOfStock
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = product.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                if (outOfStock) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        StatusPill(label = "Habis", color = Danger500)
                    }
                } else if (product.stock <= 5) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        StatusPill(label = "Stok ${product.stock}", color = Warning500)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = product.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (!product.sku.isNullOrBlank()) {
                Text(
                    text = "SKU: ${product.sku}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = Format.money(product.price),
                style = MaterialTheme.typography.titleMedium,
                color = Brand500,
                fontWeight = FontWeight.Bold
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
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = Icons.Default.ShoppingCart, tint = Brand500, boxSize = 38.dp, iconSize = 20.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Keranjang", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (state.cart.isEmpty()) "Kosong" else "${state.cart.sumOf { it.quantity }} item",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state.cart.isNotEmpty()) {
                TextButton(onClick = onClear) { Text("Kosongkan") }
            }
        }
        state.customer?.let {
            Spacer(Modifier.height(8.dp))
            AssistChip(
                onClick = {},
                label = { Text(it.name) },
                leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp)) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Brand500.copy(alpha = 0.1f),
                    labelColor = Brand500,
                    leadingIconContentColor = Brand500
                )
            )
        }
        Spacer(Modifier.height(10.dp))
        if (state.cart.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconBadge(icon = Icons.Default.ShoppingCart, tint = MaterialTheme.colorScheme.onSurfaceVariant, boxSize = 56.dp, iconSize = 28.dp)
                    Spacer(Modifier.height(10.dp))
                    Text("Belum ada item", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Pilih produk untuk ditambahkan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false).heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.cart, key = { it.product.id }) { line ->
                    CartLineRow(
                        name = line.product.name,
                        price = line.product.price,
                        qty = line.quantity,
                        subtotal = line.subtotal,
                        onIncrease = { onIncrease(line.product.id) },
                        onDecrease = { onDecrease(line.product.id) },
                        onRemove = { onRemove(line.product.id) }
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedSurfaceCard(contentPadding = PaddingValues(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SummaryRow("Subtotal", Format.money(state.subtotal))
                if (state.orderDiscount > 0) {
                    SummaryRow("Diskon", "-${Format.money(state.orderDiscount)}", color = Danger500)
                }
                if (state.taxPercent > 0) {
                    SummaryRow("Pajak ${state.taxPercent.formatTax()}%", Format.money(state.tax))
                }
                HorizontalDivider()
                Row {
                    Text("Total", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        Format.money(state.total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Brand500
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onCheckout,
            enabled = state.cart.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Brand500)
        ) {
            Text("Bayar ${Format.money(state.total)}", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CartLineRow(
    name: String,
    price: Double,
    qty: Int,
    subtotal: Double,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    OutlinedSurfaceCard(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(text = name, size = 36.dp, color = avatarColorFor(name))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${Format.money(price)} × $qty",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                QtyStepper(qty = qty, onMinus = onDecrease, onPlus = onIncrease)
                Spacer(Modifier.weight(1f))
                Text(
                    Format.money(subtotal),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun QtyStepper(qty: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMinus, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp))
        }
        Text(
            text = qty.toString(),
            modifier = Modifier.padding(horizontal = 8.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onPlus, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon = Icons.Default.ShoppingCart, tint = Brand500, boxSize = 36.dp, iconSize = 20.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Pembayaran", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${state.cart.sumOf { it.quantity }} item · ${Format.money(state.subtotal)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 520.dp).fillMaxWidth()) {
                SectionHeader(title = "Metode Pembayaran")
                Spacer(Modifier.height(6.dp))
                val methods = PaymentMethod.entries
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    methods.forEachIndexed { idx, method ->
                        SegmentedButton(
                            selected = state.paymentMethod == method,
                            onClick = { onSetMethod(method) },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = methods.size)
                        ) { Text(method.displayName) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField(
                        label = "Diskon (Rp)",
                        value = state.orderDiscount,
                        onChange = onSetDiscount,
                        modifier = Modifier.weight(1f)
                    )
                    NumberField(
                        label = "Pajak (%)",
                        value = state.taxPercent,
                        onChange = onSetTax,
                        max = 100.0,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedSurfaceCard(contentPadding = PaddingValues(14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SummaryRow("Subtotal", Format.money(state.subtotal))
                        if (state.orderDiscount > 0) {
                            SummaryRow("Diskon", "-${Format.money(state.orderDiscount)}", color = Danger500)
                        }
                        if (state.taxPercent > 0) {
                            SummaryRow("Pajak ${state.taxPercent.formatTax()}%", Format.money(state.tax))
                        }
                        HorizontalDivider()
                        Row {
                            Text("Total", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            Text(Format.money(state.total), fontWeight = FontWeight.Bold, color = Brand500)
                        }
                    }
                }
                if (state.paymentMethod == PaymentMethod.CASH) {
                    Spacer(Modifier.height(10.dp))
                    NumberField(
                        label = "Tunai diterima (Rp)",
                        value = state.paymentReceived,
                        onChange = onSetReceived,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Row {
                        Text("Kembalian", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            Format.money(state.change),
                            color = if (state.paymentReceived >= state.total) Success500 else Danger500,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.note,
                    onValueChange = onSetNote,
                    label = { Text("Catatan (opsional)") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !state.processing && (state.paymentMethod != PaymentMethod.CASH || state.paymentReceived >= state.total),
                colors = ButtonDefaults.buttonColors(containerColor = Brand500)
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
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Success500.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Success500, modifier = Modifier.size(32.dp))
            }
        },
        title = { Text("Pembayaran Berhasil", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Text(code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OutlinedSurfaceCard(contentPadding = PaddingValues(14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row {
                            Text("Total", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Format.money(total), fontWeight = FontWeight.Bold)
                        }
                        if (change > 0) {
                            Row {
                                Text("Kembalian", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(Format.money(change), color = Success500, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onPrint,
                enabled = !printing,
                colors = ButtonDefaults.buttonColors(containerColor = Brand500)
            ) {
                if (printing) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                } else {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text("Cetak Struk")
            }
        },
        dismissButton = { OutlinedButton(onClick = onDone) { Text("Selesai") } }
    )
}

@Composable
private fun CustomerPickerDialog(
    customers: List<CustomerEntity>,
    selected: Long?,
    onPick: (CustomerEntity?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Pelanggan", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(modifier = Modifier.heightIn(max = 360.dp)) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        OutlinedSurfaceCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(null) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconBadge(icon = Icons.Default.Person, tint = MaterialTheme.colorScheme.onSurfaceVariant, boxSize = 36.dp, iconSize = 18.dp)
                                Spacer(Modifier.width(10.dp))
                                Text("Tanpa pelanggan", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                if (selected == null) StatusPill(label = "Aktif", color = Brand500)
                            }
                        }
                    }
                    items(customers, key = { it.id }) { c ->
                        OutlinedSurfaceCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(c) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Avatar(text = c.name, size = 36.dp)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(c.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (!c.phone.isNullOrBlank()) {
                                        Text(c.phone!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (selected == c.id) StatusPill(label = "Dipilih", color = Brand500)
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
    max: Double = Double.MAX_VALUE,
    modifier: Modifier = Modifier
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
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    )
}

private fun Double.formatTax(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()
