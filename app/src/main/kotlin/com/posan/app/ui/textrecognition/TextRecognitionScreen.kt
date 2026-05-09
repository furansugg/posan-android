package com.posan.app.ui.textrecognition

import android.Manifest
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.posan.app.ui.components.FormSection
import com.posan.app.ui.components.IconBadge
import com.posan.app.ui.components.OutlinedSurfaceCard
import com.posan.app.ui.components.SectionHeader
import com.posan.app.ui.components.SimpleAppBar
import java.io.File

@Composable
fun TextRecognitionScreen(
    onBack: () -> Unit,
    viewModel: TextRecognitionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val photoFile = File(context.cacheDir, "ocr_capture.jpg")
    val photoUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source).copy(android.graphics.Bitmap.Config.ARGB_8888, false)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            }
            if (bitmap != null) {
                viewModel.recognizeText(bitmap)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
            if (bitmap != null) {
                viewModel.recognizeText(bitmap)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(photoUri)
        }
    }

    val launchCamera: () -> Unit = {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            SimpleAppBar(
                title = "Ekstrak Teks",
                subtitle = "Scan struk & buat struk baru",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!state.hasResult && !state.isProcessing) {
                EmptyOcrPrompt(
                    onPickGallery = { galleryLauncher.launch("image/*") },
                    onTakePhoto = launchCamera
                )
            }

            if (state.isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Memproses gambar...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (state.hasResult) {
                if (!state.error.isNullOrBlank()) {
                    Text(
                        text = state.error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (state.formattedReceipt.isNotBlank()) {
                    ReceiptPreviewCard(formattedReceipt = state.formattedReceipt)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(state.formattedReceipt))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Salin Struk")
                        }
                        OutlinedButton(
                            onClick = { viewModel.clearResult() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Hapus")
                        }
                    }
                }

                if (state.parsedReceipt != null) {
                    var showEdit by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { showEdit = !showEdit },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (showEdit) "Tutup Editor" else "Edit Data Struk")
                    }
                    if (showEdit) {
                        ReceiptEditForm(receipt = state.parsedReceipt, viewModel = viewModel)
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.toggleRawText() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (state.showRawText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.showRawText) "Sembunyikan Teks Asli" else "Lihat Teks Asli (OCR)")
                }

                if (state.showRawText && state.recognizedText.isNotBlank()) {
                    OutlinedTextField(
                        value = state.recognizedText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Teks Asli dari OCR") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        shape = MaterialTheme.shapes.medium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Galeri")
                    }
                    OutlinedButton(
                        onClick = launchCamera,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Kamera")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ReceiptPreviewCard(formattedReceipt: String) {
    SectionHeader(title = "Preview Struk Baru", subtitle = "Hasil penyusunan ulang")
    OutlinedSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Struk",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(Color.White)
                    .padding(12.dp)
            ) {
                Text(
                    text = formattedReceipt,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFF111827)
                )
            }
        }
    }
}

@Composable
private fun ReceiptEditForm(
    receipt: ParsedReceipt,
    viewModel: TextRecognitionViewModel
) {
    FormSection(title = "Info Toko", subtitle = "Edit data toko pada struk") {
        OutlinedTextField(
            value = receipt.storeName,
            onValueChange = { viewModel.updateParsedField("storeName", it) },
            label = { Text("Nama toko") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )
        OutlinedTextField(
            value = receipt.storeAddress,
            onValueChange = { viewModel.updateParsedField("storeAddress", it) },
            label = { Text("Alamat") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )
        OutlinedTextField(
            value = receipt.storePhone,
            onValueChange = { viewModel.updateParsedField("storePhone", it) },
            label = { Text("Telepon") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )
    }

    FormSection(title = "Info Transaksi", subtitle = "Detail transaksi") {
        OutlinedTextField(
            value = receipt.transactionCode,
            onValueChange = { viewModel.updateParsedField("transactionCode", it) },
            label = { Text("No. Transaksi") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )
        OutlinedTextField(
            value = receipt.dateTime,
            onValueChange = { viewModel.updateParsedField("dateTime", it) },
            label = { Text("Tanggal/Waktu") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )
        OutlinedTextField(
            value = receipt.cashier,
            onValueChange = { viewModel.updateParsedField("cashier", it) },
            label = { Text("Kasir") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )
        OutlinedTextField(
            value = receipt.customer,
            onValueChange = { viewModel.updateParsedField("customer", it) },
            label = { Text("Pelanggan") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )
    }

    if (receipt.items.isNotEmpty()) {
        FormSection(title = "Item (${receipt.items.size})", subtitle = "Daftar item terdeteksi") {
            receipt.items.forEachIndexed { idx, item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (item.quantity > 0 && item.price > 0) {
                            Text(
                                text = "${item.quantity} x Rp ${com.posan.app.util.Format.number(item.price)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (item.subtotal > 0) {
                        Text(
                            text = "Rp ${com.posan.app.util.Format.number(item.subtotal)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    FormSection(title = "Footer", subtitle = "Pesan di bawah struk") {
        OutlinedTextField(
            value = receipt.footer,
            onValueChange = { viewModel.updateParsedField("footer", it) },
            label = { Text("Footer") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )
    }
}

@Composable
private fun EmptyOcrPrompt(
    onPickGallery: () -> Unit,
    onTakePhoto: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconBadge(
            icon = Icons.Default.TextFields,
            tint = MaterialTheme.colorScheme.primary,
            boxSize = 64.dp,
            iconSize = 36.dp
        )
        Text(
            text = "Scan Struk & Buat Struk Baru",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Ambil foto struk yang sudah ada, teks akan diekstrak dan disusun ulang menjadi struk baru dengan desain rapi",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onPickGallery,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors()
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Pilih dari Galeri")
            }
            OutlinedButton(
                onClick = onTakePhoto,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Ambil Foto")
            }
        }
    }
}
