package com.posan.app.ui.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.posan.app.ui.components.Avatar
import com.posan.app.ui.components.EmptyState
import com.posan.app.ui.components.ListItemCard
import com.posan.app.ui.components.SimpleAppBar
import com.posan.app.ui.components.avatarColorFor

@Composable
fun CustomersScreen(
    onBack: () -> Unit,
    viewModel: CustomersViewModel = hiltViewModel()
) {
    val customers by viewModel.customers.collectAsState()
    val state by viewModel.state.collectAsState()
    val query by viewModel.query.collectAsState()

    Scaffold(
        topBar = {
            SimpleAppBar(
                title = "Pelanggan",
                subtitle = if (customers.isEmpty()) "Belum ada pelanggan" else "${customers.size} pelanggan",
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
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.query.value = it },
                placeholder = { Text("Cari nama atau telepon") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(Modifier.height(12.dp))
            if (customers.isEmpty()) {
                EmptyState(
                    title = "Belum ada pelanggan",
                    subtitle = "Tambahkan data pelanggan untuk mempercepat checkout",
                    icon = Icons.Default.People,
                    actionLabel = "Tambah Pelanggan",
                    onAction = { viewModel.openForm() }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(customers, key = { it.id }) { c ->
                        ListItemCard(
                            leading = { Avatar(text = c.name, color = avatarColorFor(c.name)) },
                            title = c.name,
                            subtitle = listOfNotNull(
                                c.phone?.takeIf { it.isNotBlank() },
                                c.email?.takeIf { it.isNotBlank() }
                            ).joinToString(" · ").ifBlank { "Tanpa kontak" },
                            trailing = {
                                IconButton(onClick = { viewModel.openForm(c) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { viewModel.delete(c) }) {
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

    if (state.showForm) {
        AlertDialog(
            onDismissRequest = viewModel::closeForm,
            icon = { Icon(Icons.Default.People, contentDescription = null) },
            title = { Text(if (state.editing == null) "Pelanggan Baru" else "Edit Pelanggan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        state.name,
                        viewModel::setName,
                        label = { Text("Nama *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        state.phone,
                        viewModel::setPhone,
                        label = { Text("Telepon") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        state.email,
                        viewModel::setEmail,
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        state.address,
                        viewModel::setAddress,
                        label = { Text("Alamat") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        state.note,
                        viewModel::setNote,
                        label = { Text("Catatan") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    if (!state.error.isNullOrBlank()) {
                        Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.save() }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = viewModel::closeForm) { Text("Batal") } }
        )
    }
}
