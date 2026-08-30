package com.example.ui.screens.settings

import android.Manifest
import java.io.File
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.entity.UserEntity
import com.example.ui.components.ReceiptPreviewCard
import com.example.ui.theme.PosBlueLight
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosEmeraldLight
import com.example.ui.theme.PosOrange
import com.example.ui.theme.PosPrimary
import com.example.ui.theme.PosRose
import com.example.ui.theme.PosRoseLight
import com.example.ui.viewmodel.PosViewModel
import com.example.utils.BluetoothPrinterHelper
import com.example.utils.BtDeviceItem

@Composable
fun SettingsScreen(
    viewModel: PosViewModel,
    onLoggedOut: () -> Unit
) {
    val context = LocalContext.current
    val prefs = viewModel.prefs

    val currentRole by viewModel.currentUserRole.collectAsState()
    val isSuperAdmin = currentRole == "superadmin" || currentRole == "owner"
    val isAdmin = isSuperAdmin || currentRole == "admin"

    var storeName by remember { mutableStateOf(prefs.storeName) }
    var storeAddress by remember { mutableStateOf(prefs.storeAddress) }
    var storePhone by remember { mutableStateOf(prefs.storePhone) }
    var receiptFooter by remember { mutableStateOf(prefs.receiptFooter) }
    var paperSize by remember { mutableStateOf(prefs.paperSize) }
    var autoPrint by remember { mutableStateOf(prefs.autoPrint) }

    // Bluetooth Printer State
    var selectedPrinterMac by remember { mutableStateOf(prefs.bluetoothPrinterMac) }
    var selectedPrinterName by remember { mutableStateOf(prefs.bluetoothPrinterName) }
    var pairedDevices by remember { mutableStateOf<List<BtDeviceItem>>(emptyList()) }
    var isTestingPrint by remember { mutableStateOf(false) }

    val allUsers by viewModel.allUsers.collectAsState()
    val savedBackups by viewModel.savedBackups.collectAsState()

    var showAddUserDialog by remember { mutableStateOf(false) }
    var restoreTargetFile by remember { mutableStateOf<java.io.File?>(null) }
    var backupToDelete by remember { mutableStateOf<java.io.File?>(null) }
    var overwriteOnRestore by remember { mutableStateOf(false) }
    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }

    // Storage permission request handling
    val storagePermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    var hasStoragePermission by remember {
        mutableStateOf(
            storagePermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasStoragePermission = results.values.all { it }
    }

    // Bluetooth permission request handling
    val requiredBluetoothPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    var hasBluetoothPermission by remember {
        mutableStateOf(
            requiredBluetoothPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasBluetoothPermission = results.values.all { it }
        if (hasBluetoothPermission) {
            pairedDevices = BluetoothPrinterHelper.getPairedDevices()
        }
    }

    fun refreshBluetoothDevices() {
        if (hasBluetoothPermission) {
            pairedDevices = BluetoothPrinterHelper.getPairedDevices()
        } else {
            bluetoothPermissionLauncher.launch(requiredBluetoothPermissions)
        }
    }

    LaunchedEffect(Unit) {
        if (hasBluetoothPermission) {
            pairedDevices = BluetoothPrinterHelper.getPairedDevices()
        }
    }

    // Track expanded status for each settings list item
    val expandedStates = remember {
        mutableStateMapOf(
            "store" to isAdmin,
            "users" to isSuperAdmin,
            "printer" to true,
            "system" to false
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            // Title Header with Role Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pengaturan Sistem",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Konfigurasi toko, printer thermal, dan akses pengguna",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (currentRole) {
                        "superadmin", "owner" -> PosRose.copy(alpha = 0.15f)
                        "admin" -> PosPrimary.copy(alpha = 0.15f)
                        else -> PosEmerald.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = currentRole.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = when (currentRole) {
                            "superadmin", "owner" -> PosRose
                            "admin" -> PosPrimary
                            else -> PosEmerald
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // ==========================================
        // 1. MANAJEMEN USER (HANYA SUPERADMIN)
        // ==========================================
        if (isSuperAdmin) {
            item {
                val isExpanded = expandedStates["users"] ?: false
                ExpandableSettingsCard(
                    title = "Manajemen User & Hak Akses",
                    subtitle = "Tambah Admin, Kasir, atau atur peran",
                    icon = Icons.Default.AdminPanelSettings,
                    iconTint = PosRose,
                    iconBackground = PosRoseLight,
                    isExpanded = isExpanded,
                    onToggle = { expandedStates["users"] = !isExpanded },
                    badge = {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PosRoseLight
                        ) {
                            Text(
                                text = "${allUsers.size} User",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = PosRose,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    },
                    testTag = "expand_user_management"
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Daftar Pengguna Aktif",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Button(
                                onClick = { showAddUserDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PosPrimary),
                                modifier = Modifier.testTag("btn_open_add_user")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Tambah User", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        allUsers.forEach { user ->
                            UserItemCard(
                                user = user,
                                isCurrentUser = user.username.equals(prefs.username, ignoreCase = true),
                                onDelete = { viewModel.deleteUser(user.id, user.username) }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. PROFIL TOKO & LIVE PREVIEW STRUK (ADMIN & SUPERADMIN)
        // ==========================================
        if (isAdmin) {
            item {
                val isExpanded = expandedStates["store"] ?: false
                ExpandableSettingsCard(
                    title = "Profil Toko & Header Struk",
                    subtitle = if (storeName.isNotBlank()) storeName else "Nama toko, alamat, dan kontak",
                    icon = Icons.Default.Store,
                    iconTint = MaterialTheme.colorScheme.primary,
                    iconBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    isExpanded = isExpanded,
                    onToggle = { expandedStates["store"] = !isExpanded },
                    testTag = "expand_store_settings"
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = storeName,
                            onValueChange = { storeName = it },
                            label = { Text("Nama Toko") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("setting_store_name")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = storeAddress,
                            onValueChange = { storeAddress = it },
                            label = { Text("Alamat Toko") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("setting_store_address")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = storePhone,
                            onValueChange = { storePhone = it },
                            label = { Text("No. Telepon / WhatsApp") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("setting_store_phone")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = receiptFooter,
                            onValueChange = { receiptFooter = it },
                            label = { Text("Pesan Kaki Struk (Footer)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("setting_receipt_footer")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                prefs.storeName = storeName.trim()
                                prefs.storeAddress = storeAddress.trim()
                                prefs.storePhone = storePhone.trim()
                                prefs.receiptFooter = receiptFooter.trim()
                                viewModel.showMessage("Profil toko & struk berhasil disimpan!")
                            },
                            modifier = Modifier.align(Alignment.End).testTag("save_store_settings_button")
                        ) {
                            Text("Simpan Profil")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // LIVE RECEIPT PREVIEW (Requested by user)
                        ReceiptPreviewCard(
                            storeName = storeName,
                            storeAddress = storeAddress,
                            storePhone = storePhone,
                            receiptFooter = receiptFooter,
                            paperSize = paperSize
                        )
                    }
                }
            }
        }

        // ==========================================
        // 3. PRINTER BLUETOOTH & CETAK STRUK (ALL ROLES)
        // ==========================================
        item {
            val isExpanded = expandedStates["printer"] ?: false
            ExpandableSettingsCard(
                title = "Penyandingan Printer Bluetooth",
                subtitle = if (selectedPrinterName != null) "Terpilih: $selectedPrinterName" else "Pilih printer thermal bluetooth kasir",
                icon = Icons.Default.Print,
                iconTint = Color(0xFFD97706),
                iconBackground = Color(0xFFFEF3C7),
                isExpanded = isExpanded,
                onToggle = { expandedStates["printer"] = !isExpanded },
                badge = {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (selectedPrinterMac != null) PosEmeraldLight else Color(0xFFFEF3C7)
                    ) {
                        Text(
                            text = if (selectedPrinterMac != null) "Tersambung" else paperSize,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (selectedPrinterMac != null) PosEmerald else Color(0xFFB45309),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                },
                testTag = "expand_printer_settings"
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Bluetooth Permission and Device List Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = if (hasBluetoothPermission) PosPrimary else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Perangkat Printer Bluetooth",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { refreshBluetoothDevices() },
                            modifier = Modifier.testTag("btn_scan_bluetooth")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Pindai Ulang", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!hasBluetoothPermission) {
                        Surface(
                            color = PosOrange.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Izin Bluetooth belum diberikan untuk mendeteksi printer fisik.",
                                    fontSize = 12.sp,
                                    color = PosOrange,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { bluetoothPermissionLauncher.launch(requiredBluetoothPermissions) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PosOrange)
                                ) {
                                    Text("Beri Izin", fontSize = 11.sp)
                                }
                            }
                        }
                    } else if (pairedDevices.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Belum ada printer Bluetooth terpasang",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    "Pastikan printer thermal Bluetooth Anda telah dipasangkan (Paired) di pengaturan Bluetooth perangkat fisik Android Anda.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    } else {
                        // List of Bluetooth devices
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            pairedDevices.forEach { device ->
                                val isSelected = device.address == selectedPrinterMac
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) PosPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) PosPrimary else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            selectedPrinterMac = device.address
                                            selectedPrinterName = device.name
                                            prefs.bluetoothPrinterMac = device.address
                                            prefs.bluetoothPrinterName = device.name
                                            viewModel.showMessage("Printer '${device.name}' dipilih sebagai printer kasir")
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = if (isSelected) PosPrimary else Color.Gray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = device.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (isSelected) PosPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "MAC: ${device.address}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (isSelected) {
                                            Surface(
                                                color = PosEmeraldLight,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    "Aktif",
                                                    color = PosEmerald,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Test Print Button
                    if (selectedPrinterMac != null) {
                        Button(
                            onClick = {
                                isTestingPrint = true
                                viewModel.testPrintBluetooth(selectedPrinterMac!!) { _, _ ->
                                    isTestingPrint = false
                                }
                            },
                            enabled = !isTestingPrint,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            modifier = Modifier.fillMaxWidth().testTag("btn_test_print_bluetooth")
                        ) {
                            if (isTestingPrint) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Mengirim ke Printer...", fontSize = 13.sp)
                            } else {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Tes Cetak Struk ke Printer (${selectedPrinterName ?: "Bluetooth"})", fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Paper width selector
                    Text(
                        text = "Lebar Kertas Struk:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = paperSize == "58mm",
                            onClick = {
                                paperSize = "58mm"
                                prefs.paperSize = "58mm"
                            },
                            label = { Text("58mm (Standar)") }
                        )
                        FilterChip(
                            selected = paperSize == "80mm",
                            onClick = {
                                paperSize = "80mm"
                                prefs.paperSize = "80mm"
                            },
                            label = { Text("80mm (Lebar)") }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Auto print switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Otomatis Cetak Struk Setelah Pembayaran",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Langsung cetak ke printer Bluetooth tanpa konfirmasi tambahan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoPrint,
                            onCheckedChange = {
                                autoPrint = it
                                prefs.autoPrint = it
                            }
                        )
                    }
                }
            }
        }

        // ==========================================
        // 4. BACKUP & RESTORE DATABASE PRODUK
        // ==========================================
        item {
            val isExpanded = expandedStates["backup"] ?: false
            ExpandableSettingsCard(
                title = "Backup & Restore Database Produk",
                subtitle = "Cadangkan atau pulihkan katalog produk ke penyimpanan internal",
                icon = Icons.Default.Storage,
                iconTint = Color(0xFF0284C7),
                iconBackground = Color(0xFFE0F2FE),
                isExpanded = isExpanded,
                onToggle = { expandedStates["backup"] = !isExpanded },
                badge = {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFE0F2FE)
                    ) {
                        Text(
                            text = "${savedBackups.size} File",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF0369A1),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                },
                testTag = "expand_backup_settings"
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Storage Permission Notice if not granted
                    if (!hasStoragePermission) {
                        Surface(
                            color = PosOrange.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        "Izin Penyimpanan Diperlukan",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = PosOrange
                                    )
                                    Text(
                                        "Berikan izin akses penyimpanan perangkat untuk menyimpan file backup .JSON dan memulihkan database.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = { storagePermissionLauncher.launch(storagePermissions) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PosOrange)
                                ) {
                                    Text("Beri Izin", fontSize = 11.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Create Backup Action Button
                    Button(
                        onClick = {
                            if (!hasStoragePermission) {
                                storagePermissionLauncher.launch(storagePermissions)
                            }
                            isBackingUp = true
                            viewModel.backupProductDatabase { success, message, _ ->
                                isBackingUp = false
                                viewModel.showMessage(message)
                            }
                        },
                        enabled = !isBackingUp,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        modifier = Modifier.fillMaxWidth().testTag("btn_create_backup")
                    ) {
                        if (isBackingUp) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Sedang Membuat File Backup...", fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Buat File Cadangan Produk (.JSON)", fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Daftar File Cadangan di Penyimpanan Internal:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (savedBackups.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Belum ada file cadangan tersimpan",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Klik tombol di atas untuk membuat cadangan database produk pertama Anda.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            savedBackups.forEach { file ->
                                val fileSizeKb = (file.length() / 1024.0).let { String.format("%.1f KB", it) }
                                val lastModifiedStr = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(file.lastModified()))

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFE0F2FE)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Description,
                                                    contentDescription = null,
                                                    tint = Color(0xFF0284C7),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = file.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "$lastModifiedStr • $fileSizeKb",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            OutlinedButton(
                                                onClick = {
                                                    restoreTargetFile = file
                                                    overwriteOnRestore = false
                                                },
                                                modifier = Modifier.testTag("btn_restore_${file.name}")
                                            ) {
                                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Pulihkan", fontSize = 11.sp)
                                            }
                                            Spacer(Modifier.width(4.dp))
                                            IconButton(
                                                onClick = { backupToDelete = file },
                                                modifier = Modifier.size(34.dp).testTag("btn_delete_backup_${file.name}")
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Hapus Backup",
                                                    tint = PosRose,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 5. AKUN & PEMELIHARAAN SISTEM
        // ==========================================
        item {
            val isExpanded = expandedStates["system"] ?: false
            ExpandableSettingsCard(
                title = "Akun & Pemeliharaan Data",
                subtitle = "User login, reset katalog, dan keluar",
                icon = Icons.Default.ManageAccounts,
                iconTint = Color(0xFF6B7280),
                iconBackground = MaterialTheme.colorScheme.surfaceVariant,
                isExpanded = isExpanded,
                onToggle = { expandedStates["system"] = !isExpanded },
                testTag = "expand_system_settings"
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Active account info
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (currentRole) {
                                            "superadmin", "owner" -> PosRose
                                            "admin" -> PosPrimary
                                            else -> PosEmerald
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (prefs.userName.firstOrNull() ?: 'U').uppercase(),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = prefs.userName.ifBlank { "User Toko" },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Role: ${currentRole.uppercase()} • Username: ${prefs.username}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isSuperAdmin) {
                            OutlinedButton(
                                onClick = { viewModel.resetAndSeedCatalog() },
                                modifier = Modifier.weight(1f).testTag("seed_demo_data_button")
                            ) {
                                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset Data")
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.logout(onLoggedOut)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PosRose),
                            modifier = Modifier.weight(1f).testTag("logout_button")
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Keluar")
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Add User Modal Dialog (Superadmin only)
    if (showAddUserDialog) {
        AddUserDialog(
            onDismiss = { showAddUserDialog = false },
            onConfirm = { username, password, name, role ->
                viewModel.addUser(username, password, name, role) {
                    showAddUserDialog = false
                }
            }
        )
    }

    // Restore Backup Confirmation Dialog
    restoreTargetFile?.let { file ->
        AlertDialog(
            onDismissRequest = {
                if (!isRestoring) restoreTargetFile = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Pulihkan Database Produk", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Apakah Anda ingin memulihkan database produk dari file '${file.name}'?",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { overwriteOnRestore = !overwriteOnRestore }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    "Timpa Semua Produk Saat Ini",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    if (overwriteOnRestore) "Semua produk lama akan dihapus dan diganti dengan isi backup" else "Produk dari backup akan digabungkan/diperbarui dengan produk saat ini",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = overwriteOnRestore,
                                onCheckedChange = { overwriteOnRestore = it }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isRestoring = true
                        viewModel.restoreProductDatabaseFromJson(file, overwrite = overwriteOnRestore) { success, msg ->
                            isRestoring = false
                            restoreTargetFile = null
                        }
                    },
                    enabled = !isRestoring,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.testTag("btn_confirm_restore")
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Memulihkan...")
                    } else {
                        Text("Pulihkan Data")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { restoreTargetFile = null },
                    enabled = !isRestoring
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // Delete Backup File Confirmation Dialog
    backupToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { backupToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = PosRose,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Hapus File Cadangan", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Apakah Anda yakin ingin menghapus file backup '${file.name}' dari penyimpanan internal?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBackupFile(file)
                        backupToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PosRose),
                    modifier = Modifier.testTag("btn_confirm_delete_backup")
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun UserItemCard(
    user: UserEntity,
    isCurrentUser: Boolean,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            when (user.role) {
                                "superadmin", "owner" -> PosRose
                                "admin" -> PosPrimary
                                else -> PosEmerald
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.firstOrNull()?.uppercase() ?: "U",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        if (isCurrentUser) {
                            Spacer(Modifier.width(4.dp))
                            Text("(Anda)", fontSize = 11.sp, color = PosPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(
                        text = "Username: @${user.username}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (user.role) {
                        "superadmin", "owner" -> PosRoseLight
                        "admin" -> PosPrimary.copy(alpha = 0.12f)
                        else -> PosEmeraldLight
                    }
                ) {
                    Text(
                        text = when (user.role) {
                            "superadmin", "owner" -> "Owner"
                            "admin" -> "Manajer"
                            else -> "Kasir"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (user.role) {
                            "superadmin", "owner" -> PosRose
                            "admin" -> PosPrimary
                            else -> PosEmerald
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (!isCurrentUser && user.role != "superadmin" && user.role != "owner") {
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Hapus User",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (username: String, pass: String, name: String, role: String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("kasir") } // "admin" or "kasir"
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = PosPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Tambah User Baru", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Buat akun staf baru untuk login ke sistem kasir.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap") },
                    placeholder = { Text("cth: Rahmat Hidayat") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_user_name")
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username Login") },
                    placeholder = { Text("cth: rahmat_kasir") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_user_username")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    placeholder = { Text("Minimal 6 karakter") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("input_user_password")
                )

                Text("Pilih Hak Akses (Role):", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedRole == "kasir",
                        onClick = { selectedRole = "kasir" },
                        label = { Text("Kasir & Stok") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PosEmeraldLight,
                            selectedLabelColor = PosEmerald
                        ),
                        modifier = Modifier.testTag("chip_role_kasir")
                    )

                    FilterChip(
                        selected = selectedRole == "admin",
                        onClick = { selectedRole = "admin" },
                        label = { Text("Admin / Manajer") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PosPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = PosPrimary
                        ),
                        modifier = Modifier.testTag("chip_role_admin")
                    )
                }

                errorText?.let {
                    Text(it, color = PosRose, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || username.isBlank() || password.isBlank()) {
                        errorText = "Semua kolom wajib diisi."
                    } else if (password.length < 4) {
                        errorText = "Password minimal 4 karakter."
                    } else {
                        onConfirm(username.trim(), password.trim(), name.trim(), selectedRole)
                    }
                },
                modifier = Modifier.testTag("btn_submit_add_user")
            ) {
                Text("Simpan User")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

/**
 * Reusable Expandable Settings Card Component
 */
@Composable
fun ExpandableSettingsCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    badge: (@Composable () -> Unit)? = null,
    testTag: String = "",
    content: @Composable () -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "expand_arrow_rotation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(RoundedCornerShape(16.dp))
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 3.dp else 1.5.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Tappable Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (badge != null) {
                        badge()
                    }

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Tutup menu" else "Buka menu",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(rotationAngle)
                    )
                }
            }

            // Expandable Content Body
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                content()
            }
        }
    }
}
