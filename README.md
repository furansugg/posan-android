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

## Persyaratan minimum

- Android 7.0 (API 24) atau lebih baru
- ~30 MB ruang penyimpanan
- Printer thermal Bluetooth ESC/POS (opsional, hanya jika ingin cetak struk)
- Bluetooth aktif (untuk fitur cetak)

---

## Instalasi — untuk pengguna akhir (install APK langsung)

### A. Install dari APK rilis

1. **Download APK** dari halaman [Releases](https://github.com/furansugg/posan-android/releases) (cari file `app-debug.apk` pada rilis terbaru).
2. Pindahkan APK ke perangkat Android (via USB, email, atau cloud).
3. Di perangkat Android, buka **Setelan → Keamanan → Sumber tidak dikenal** (atau saat membuka APK, izinkan "Install dari sumber ini" untuk file manager / browser yang dipakai).
4. Buka file APK dengan file manager → ketuk **Install**.
5. Setelah selesai, ketuk **Open** atau cari ikon **Posan POS** di app drawer.

### B. Install via ADB (untuk developer / Android Studio user)

```sh
adb install -r app-debug.apk
```

Gunakan flag `-r` untuk overwrite versi sebelumnya.

### Pertama kali jalankan

1. Buka aplikasi.
2. Login dengan akun default:
   - **Username**: `admin`
   - **Password**: `admin123`
   - **Role**: ADMIN
3. **WAJIB**: segera ganti password admin lewat menu **Manajemen User**.
4. Lengkapi data toko di menu **Pengaturan Cetak** (nama, alamat, telepon).
5. Tambah **Kategori** & **Produk** dari menu masing-masing.
6. (Opsional) tambah **Pelanggan** dari menu Pelanggan.
7. (Opsional) tambah user kasir tambahan dari **Manajemen User**.

### Konfigurasi printer Bluetooth

Sebelum cetak struk pertama kali:

1. Hidupkan printer thermal ESC/POS Anda.
2. Buka **Setelan Android → Bluetooth** → cari & **pair** printer (umumnya PIN `0000` / `1234`).
3. Kembali ke aplikasi → menu **Printer** → izinkan permission Bluetooth saat diminta.
4. Pilih printer dari daftar perangkat yang sudah dipair.
5. (Opsional) ketuk **Test Print** untuk memastikan koneksi berjalan.
6. Buka menu **Pengaturan Cetak** untuk atur layout (lebar kertas 58/80mm, alignment, font, dll.) — lihat preview real-time di kanan layar, lalu **Simpan**.

> **Catatan permission**: Android 12+ butuh izin runtime `BLUETOOTH_CONNECT` & `BLUETOOTH_SCAN`. Android ≤ 11 butuh `ACCESS_FINE_LOCATION` (untuk discovery Bluetooth lama). Aplikasi akan meminta permission ini otomatis.

---

## Instalasi — untuk developer (build dari source)

### Prasyarat

- **JDK 17** (`java -version` harus menampilkan 17.x)
- **Android Studio Hedgehog (2023.1.1) atau lebih baru**, _atau_ command-line:
  - Android SDK dengan **build-tools 34.0.0**, **platform-tools**, dan **platforms;android-34**
  - Set `ANDROID_HOME` ke direktori SDK (mis. `~/Android/Sdk` atau `~/android-sdk`)

### Build via Android Studio

1. **Clone repo**:

   ```sh
   git clone https://github.com/furansugg/posan-android.git
   cd posan-android
   ```

2. Buka folder project di Android Studio → tunggu Gradle sync selesai (akan otomatis download dependency dari Maven Central + JitPack).
3. Pilih konfigurasi run **app** → klik tombol **Run** ▶ (atau Shift+F10).
4. Pilih emulator (API 24+) atau device fisik (USB debugging aktif) → install & launch.

### Build via command line

```sh
# Clone
git clone https://github.com/furansugg/posan-android.git
cd posan-android

# (Opsional) buat local.properties dengan SDK path jika belum di-export
echo "sdk.dir=$ANDROID_HOME" > local.properties

# Build APK debug
./gradlew assembleDebug
# → APK ada di: app/build/outputs/apk/debug/app-debug.apk

# Lint check
./gradlew lintDebug

# Install langsung ke device/emulator yang aktif
./gradlew installDebug

# Build APK release (perlu signing config; lihat bagian "Build APK release" di bawah)
./gradlew assembleRelease
```

Build pertama akan men-download semua dependency (~5–10 menit tergantung koneksi). Build berikutnya jauh lebih cepat (incremental + cache Gradle).

### Build APK release (signed)

APK release perlu ditandatangani sebelum bisa diinstal di device. Generate keystore:

```sh
keytool -genkey -v -keystore release.jks -alias posan -keyalg RSA -keysize 2048 -validity 10000
```

Tambah `keystore.properties` di root project (sudah ada di `.gitignore`):

```
storeFile=../release.jks
storePassword=...
keyAlias=posan
keyPassword=...
```

Tambahkan signing config di `app/build.gradle.kts` (belum dikonfigurasi default — silakan setup sesuai kebutuhan), lalu:

```sh
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

### Menjalankan unit / instrumented test

Belum ada test suite di rilis awal ini. Lihat issue tracker untuk progres.

---

## Backup data

Disarankan backup berkala via menu **Backup / Restore**:

- **Ekspor**: pilih lokasi file → akan disimpan sebagai `posan-backup-{timestamp}.json`.
- **Impor**: pilih file backup → toggle "Replace existing data" sesuai kebutuhan.

File backup berisi seluruh data: produk, kategori, pelanggan, user, transaksi, item transaksi, pergerakan stok, dan pengaturan cetak.

## Troubleshooting

| Masalah | Solusi |
| --- | --- |
| **"App not installed" saat install APK** | Uninstall versi lama dulu, atau pakai `adb install -r ...` |
| **Printer tidak muncul di daftar** | Pastikan sudah di-pair dari Setelan Bluetooth Android. Aplikasi hanya menampilkan device yang sudah terpasang. |
| **Print error / printer tidak respon** | Cek baterai/kertas printer, cabut-pasang ulang Bluetooth, lalu Test Print dari menu Printer. |
| **Lupa password admin** | Reinstall app (akan reset DB, semua data hilang) — atau gunakan fitur **Restore** dari backup terakhir. |
| **Lambat saat banyak produk** | Pastikan device tidak low-memory. Rilis `release` lebih cepat dari `debug`. |

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
