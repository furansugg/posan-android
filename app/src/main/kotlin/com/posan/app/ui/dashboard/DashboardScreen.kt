package com.posan.app.ui.dashboard

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.posan.app.ui.Routes
import com.posan.app.ui.components.IconBadge
import com.posan.app.ui.components.SectionHeader
import com.posan.app.ui.theme.Brand500
import com.posan.app.ui.theme.Brand700
import com.posan.app.ui.theme.CatBlue
import com.posan.app.ui.theme.CatCyan
import com.posan.app.ui.theme.CatGray
import com.posan.app.ui.theme.CatGreen
import com.posan.app.ui.theme.CatOrange
import com.posan.app.ui.theme.CatPink
import com.posan.app.ui.theme.CatPurple
import com.posan.app.ui.theme.CatRed
import com.posan.app.ui.theme.CatRose
import com.posan.app.ui.theme.CatTeal
import com.posan.app.ui.theme.Warning500

@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            DashboardHero(
                name = state.name,
                role = state.role,
                revenueText = viewModel.moneyText(state.todayRevenue),
                txCount = state.todayTransactions,
                onLogout = { viewModel.logout(onLogout) }
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.lowStockCount > 0) {
                    LowStockBanner(count = state.lowStockCount, onClick = { onNavigate(Routes.STOCK) })
                }

                SectionHeader(title = "Aksi Cepat", subtitle = "Mulai dengan satu sentuhan")
                QuickActionsRow(onNavigate = onNavigate)

                SectionHeader(title = "Manajemen", subtitle = "Kelola data toko Anda")
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(((MENU_ITEMS.size / 3 + if (MENU_ITEMS.size % 3 != 0) 1 else 0) * 108).dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    userScrollEnabled = false
                ) {
                    items(MENU_ITEMS) { item ->
                        MenuTile(
                            title = item.title,
                            icon = item.icon,
                            tint = item.tint,
                            onClick = { onNavigate(item.route) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DashboardHero(
    name: String,
    role: String,
    revenueText: String,
    txCount: Int,
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Brand700, Brand500)))
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Selamat datang",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        text = name.ifBlank { "Posan POS" },
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    if (role.isNotBlank()) {
                        Text(
                            text = role,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
                IconButton(onClick = onLogout) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Keluar",
                        tint = Color.White
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroStat(
                    label = "Pendapatan hari ini",
                    value = revenueText,
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
                HeroStat(
                    label = "Transaksi",
                    value = txCount.toString(),
                    icon = Icons.Default.ReceiptLong,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(Color.White.copy(alpha = 0.18f))
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.size(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LowStockBanner(count: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Warning500.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(icon = Icons.Default.Warning, tint = Warning500, boxSize = 40.dp, iconSize = 22.dp)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Perhatian stok",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$count produk hampir/habis stok",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Tinjau",
                style = MaterialTheme.typography.labelLarge,
                color = Warning500,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun QuickActionsRow(onNavigate: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickAction(
            title = "Buka Kasir",
            subtitle = "Mulai transaksi",
            icon = Icons.Default.PointOfSale,
            tint = Brand500,
            modifier = Modifier.weight(1f),
            onClick = { onNavigate(Routes.POS) }
        )
        QuickAction(
            title = "Riwayat",
            subtitle = "Cek penjualan",
            icon = Icons.Default.Receipt,
            tint = CatPurple,
            modifier = Modifier.weight(1f),
            onClick = { onNavigate(Routes.TRANSACTIONS) }
        )
    }
}

@Composable
private fun QuickAction(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            IconBadge(icon = icon, tint = tint)
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MenuTile(
    title: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(98.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconBadge(icon = icon, tint = tint, boxSize = 40.dp, iconSize = 20.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private data class MenuItem(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val tint: Color
)

private val MENU_ITEMS = listOf(
    MenuItem("Produk", Icons.Default.ShoppingBag, Routes.PRODUCTS, CatTeal),
    MenuItem("Kategori", Icons.Default.Category, Routes.CATEGORIES, CatOrange),
    MenuItem("Pelanggan", Icons.Default.People, Routes.CUSTOMERS, CatPink),
    MenuItem("Laporan", Icons.Default.TrendingUp, Routes.REPORT, CatRose),
    MenuItem("Stok", Icons.Default.Inventory2, Routes.STOCK, CatGreen),
    MenuItem("Pengguna", Icons.Default.Group, Routes.USERS, CatGray),
    MenuItem("Token PLN", Icons.Default.Bolt, Routes.PLN_TOKEN, CatOrange),
    MenuItem("Template PLN", Icons.Default.Bolt, Routes.PLN_TEMPLATES, CatPurple),
    MenuItem("Cetak", Icons.Default.Print, Routes.PRINT_SETTINGS, CatRed),
    MenuItem("Backup", Icons.Default.Backup, Routes.BACKUP, CatCyan),
    MenuItem("Printer", Icons.Default.Print, Routes.PRINTER_DEVICES, CatBlue),
    MenuItem("Ekstrak Teks", Icons.Default.TextFields, Routes.TEXT_RECOGNITION, CatCyan)
)
