# Posan POS — Android

Aplikasi Point of Sale (POS) Android menggunakan Kotlin + Jetpack Compose, Room, Hilt, dan Bluetooth ESC/POS thermal printer.

## Fitur

- **Master data**: Produk, Kategori, dan Pelanggan (CRUD lengkap dengan pencarian).
- **Penjualan**: Keranjang, diskon, pajak, multi metode pembayaran (Tunai, QRIS, Kartu Debit/Kredit).
- **Riwayat & Laporan**: Riwayat transaksi per tanggal, laporan harian dengan rincian metode pembayaran.
- **Stok**: Penyesuaian stok (IN / OUT / ADJUSTMENT) dengan riwayat pergerakan.
- **Multi user**: Login dengan role ADMIN dan KASIR (password di-hash SHA-256).
- **Backup / Restore**: Ekspor & impor seluruh data ke file JSON.
- **Cetak struk**: Bluetooth ESC/POS, layout struk lengkap dengan **live preview**:
  - Logo, nama toko, alamat, telepon
  - Header & footer custom
  - Lebar kertas 58mm / 80mm
  - Alignment header (LEFT / CENTER / RIGHT)
  - Bold, double size, small font
  - Toggle: tampilkan kasir, pelanggan, SKU, logo
  - Jumlah copy, auto cut, buka cash drawer
  - Pajak default & simbol mata uang

## Tech stack

| Lapisan          | Teknologi                                                           |
| ---------------- | ------------------------------------------------------------------- |
| UI               | Jetpack Compose + Material 3                                        |
| Arsitektur       | MVVM dengan Repository pattern                                      |
| DI               | Hilt                                                                |
| Database         | Room                                                                |
| State            | StateFlow / MutableStateFlow                                        |
| Navigation       | androidx.navigation.compose                                         |
| Preferences      | DataStore (sesi user, dsb.)                                         |
| Printing         | [DantSu/ESCPOS-ThermalPrinter-Android](https://github.com/DantSu/ESCPOS-ThermalPrinter-Android) |
| Backup           | Gson                                                                |

## Build

```sh
# Sebelum build, pastikan Android SDK 34 terpasang (cmdline-tools, platform-tools, build-tools 34.0.0).
./gradlew assembleDebug    # Build debug APK
./gradlew lintDebug        # Lint check
```

APK debug tersedia di `app/build/outputs/apk/debug/app-debug.apk`.

## Login default

Saat database baru pertama kali dibuat, ada user default:

- Username: `admin`
- Password: `admin123`
- Role: `ADMIN`

Ubah password ini segera dari menu **Manajemen User** setelah login.

## Konfigurasi printer

1. Pair printer Bluetooth ESC/POS Anda dari pengaturan Bluetooth Android.
2. Buka aplikasi → menu **Printer** → pilih perangkat → opsional: tekan **Test Print**.
3. Buka menu **Pengaturan Cetak** untuk menyesuaikan layout struk dan lihat preview-nya.

Aplikasi memerlukan permission `BLUETOOTH_CONNECT` dan `BLUETOOTH_SCAN` di Android 12+.

## Struktur direktori

```
app/src/main/kotlin/com/posan/app/
├── data/                # Room entities, DAOs, repositories, DataStore
├── di/                  # Hilt modules
├── domain/              # Domain models & enums (PaymentMethod, UserRole, dst.)
├── print/               # BluetoothPrinterService + ReceiptComposer (ESC/POS)
├── ui/                  # Compose screens & ViewModels (per fitur)
└── util/                # Format, hashing, dsb.
```
