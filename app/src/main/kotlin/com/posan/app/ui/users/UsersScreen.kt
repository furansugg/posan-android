package com.posan.app.ui.users

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.posan.app.domain.model.UserRole
import com.posan.app.ui.components.Avatar
import com.posan.app.ui.components.EmptyState
import com.posan.app.ui.components.ListItemCard
import com.posan.app.ui.components.RoleBadge
import com.posan.app.ui.components.SimpleAppBar
import com.posan.app.ui.components.StatusPill
import com.posan.app.ui.components.avatarColorFor
import com.posan.app.ui.theme.Danger500

@Composable
fun UsersScreen(
    onBack: () -> Unit,
    viewModel: UsersViewModel = hiltViewModel()
) {
    val users by viewModel.users.collectAsState()
    val state by viewModel.state.collectAsState()
    val ctx = LocalContext.current

    LaunchedEffect(state.error) {
        if (!state.error.isNullOrBlank() && !state.showForm) {
            android.widget.Toast.makeText(ctx, state.error, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.consumeError()
        }
    }

    Scaffold(
        topBar = {
            SimpleAppBar(
                title = "Pengguna",
                subtitle = if (users.isEmpty()) "Belum ada pengguna" else "${users.size} pengguna",
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
        if (users.isEmpty()) {
            EmptyState(
                title = "Belum ada pengguna",
                subtitle = "Tambahkan kasir atau admin baru",
                icon = Icons.Default.Group,
                actionLabel = "Tambah Pengguna",
                onAction = { viewModel.openForm() }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                items(users, key = { it.id }) { user ->
                    ListItemCard(
                        leading = { Avatar(text = user.name, color = avatarColorFor(user.username)) },
                        title = user.name,
                        subtitle = "@${user.username}",
                        extra = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                RoleBadge(role = user.role)
                                if (!user.active) {
                                    StatusPill(label = "Nonaktif", color = Danger500)
                                }
                            }
                        },
                        trailing = {
                            IconButton(onClick = { viewModel.openForm(user) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { viewModel.delete(user) }) {
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

    if (state.showForm) {
        AlertDialog(
            onDismissRequest = viewModel::closeForm,
            icon = { Icon(Icons.Default.Group, contentDescription = null) },
            title = { Text(if (state.editing == null) "Pengguna Baru" else "Edit Pengguna") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.username,
                        onValueChange = viewModel::setUsername,
                        label = { Text("Username") },
                        singleLine = true,
                        enabled = state.editing == null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = viewModel::setPassword,
                        label = {
                            Text(
                                if (state.editing == null) "Password"
                                else "Password baru (kosongkan jika tidak diubah)"
                            )
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = viewModel::setName,
                        label = { Text("Nama lengkap") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text("Peran", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        UserRole.entries.forEach { r ->
                            FilterChip(
                                selected = state.role == r,
                                onClick = { viewModel.setRole(r) },
                                label = { Text(r.displayName) }
                            )
                        }
                    }
                    if (state.editing != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Aktif", modifier = Modifier.weight(1f))
                            Switch(checked = state.active, onCheckedChange = viewModel::setActive)
                        }
                    }
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
