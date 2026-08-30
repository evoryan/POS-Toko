package com.example.ui.screens.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.ProductEntity
import com.example.ui.components.CameraBarcodeScannerDialog
import com.example.ui.theme.PosAmber
import com.example.ui.theme.PosAmberLight
import com.example.ui.theme.PosBlueLight
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosEmeraldLight
import com.example.ui.theme.PosRose
import com.example.ui.theme.PosRoseLight
import com.example.ui.viewmodel.PosViewModel
import com.example.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: PosViewModel
) {
    val userRole by viewModel.currentUserRole.collectAsState()
    val isKasir = userRole == "kasir"

    val products by viewModel.filteredProducts.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var adjustingStockProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }
    var productToPrintBarcode by remember { mutableStateOf<ProductEntity?>(null) }
    var showLowStockFilter by remember { mutableStateOf(false) }
    var showBarcodeScannerForForm by remember { mutableStateOf(false) }
    var scannedBarcodeTarget by remember { mutableStateOf<((String) -> Unit)?>(null) }

    // Superadmin Auth state for Kasir edit/delete
    var authPendingAction by remember { mutableStateOf<Pair<String, ProductEntity>?>(null) }

    val displayedProducts = if (showLowStockFilter) {
        lowStockProducts
    } else {
        products
    }

    if (showBarcodeScannerForForm) {
        CameraBarcodeScannerDialog(
            products = products,
            onDismiss = { showBarcodeScannerForForm = false },
            onBarcodeScanned = { barcode ->
                scannedBarcodeTarget?.invoke(barcode)
                showBarcodeScannerForForm = false
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingProduct = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Row(modifier = Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Produk")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah Produk", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Manajemen Stok Produk",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Total ${products.size} produk terdaftar di database",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Low Stock Warning Banner if any exists
            if (lowStockProducts.isNotEmpty()) {
                Surface(
                    onClick = { showLowStockFilter = !showLowStockFilter },
                    shape = RoundedCornerShape(12.dp),
                    color = if (showLowStockFilter) PosAmber else PosAmberLight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("low_stock_warning_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (showLowStockFilter) Color.White else PosAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Perhatian: ${lowStockProducts.size} produk stok menipis / habis!",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (showLowStockFilter) Color.White else PosAmber
                                )
                            )
                        }
                        Text(
                            text = if (showLowStockFilter) "Tampilkan Semua" else "Lihat Detail",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (showLowStockFilter) Color.White else PosAmber
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Search Bar & Categories
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Cari berdasarkan nama, barcode, kategori...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Hapus")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inventory_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null && !showLowStockFilter,
                        onClick = {
                            showLowStockFilter = false
                            viewModel.selectedCategory.value = null
                        },
                        label = { Text("Semua") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category && !showLowStockFilter,
                        onClick = {
                            showLowStockFilter = false
                            viewModel.selectedCategory.value = if (selectedCategory == category) null else category
                        },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Product Inventory Items List
            if (displayedProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Belum ada produk yang cocok",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("inventory_product_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayedProducts, key = { it.id }) { product ->
                        InventoryProductRow(
                            product = product,
                            onEdit = {
                                if (isKasir) {
                                    authPendingAction = "edit" to product
                                } else {
                                    editingProduct = product
                                    showAddEditDialog = true
                                }
                            },
                            onAdjustStock = {
                                adjustingStockProduct = product
                            },
                            onPrintBarcode = {
                                productToPrintBarcode = product
                            },
                            onDelete = {
                                if (isKasir) {
                                    authPendingAction = "delete" to product
                                } else {
                                    productToDelete = product
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Superadmin Password Verification Dialog for Kasir
    authPendingAction?.let { (action, product) ->
        SuperadminAuthDialog(
            actionName = if (action == "edit") "mengedit" else "menghapus",
            productName = product.name,
            onDismiss = { authPendingAction = null },
            onVerify = { enteredPass, onError ->
                viewModel.verifySuperadminPassword(enteredPass) { isValid ->
                    if (isValid) {
                        val pending = authPendingAction
                        authPendingAction = null
                        if (pending?.first == "edit") {
                            editingProduct = pending.second
                            showAddEditDialog = true
                        } else if (pending?.first == "delete") {
                            productToDelete = pending.second
                        }
                    } else {
                        onError("Password Superadmin salah!")
                    }
                }
            }
        )
    }

    // Add / Edit Product Dialog
    if (showAddEditDialog) {
        AddEditProductDialog(
            product = editingProduct,
            existingCategories = categories,
            onDismiss = {
                showAddEditDialog = false
                editingProduct = null
            },
            onScanBarcodeRequest = { callback ->
                scannedBarcodeTarget = callback
                showBarcodeScannerForForm = true
            },
            onSave = { id, barcode, name, category, sellPrice, costPrice, stock, minStock, unit ->
                viewModel.saveProduct(
                    id = id,
                    barcode = barcode,
                    name = name,
                    category = category,
                    sellPrice = sellPrice,
                    costPrice = costPrice,
                    stock = stock,
                    minStock = minStock,
                    unit = unit
                ) {
                    showAddEditDialog = false
                    editingProduct = null
                }
            }
        )
    }

    // Barcode Print Dialog
    productToPrintBarcode?.let { product ->
        BarcodePrintDialog(
            product = product,
            storeName = viewModel.prefs.storeName,
            bluetoothPrinterName = viewModel.prefs.bluetoothPrinterName,
            onDismiss = { productToPrintBarcode = null },
            onPrint = { copies ->
                viewModel.printProductBarcode(product, copies)
                productToPrintBarcode = null
            }
        )
    }

    // Stock Adjustment Dialog (+ / -)
    adjustingStockProduct?.let { product ->
        StockAdjustmentDialog(
            product = product,
            onDismiss = { adjustingStockProduct = null },
            onConfirmAdjust = { amount, reason ->
                viewModel.adjustStock(product.id, amount, reason)
                adjustingStockProduct = null
            }
        )
    }

    // Delete Confirmation Dialog
    productToDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Hapus Produk", fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin menghapus produk '${product.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(product.id, product.name)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PosRose)
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun SuperadminAuthDialog(
    actionName: String,
    productName: String,
    onDismiss: () -> Unit,
    onVerify: (password: String, onError: (String) -> Unit) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = PosRose,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text("Otorisasi Superadmin", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Akun Kasir memerlukan otorisasi password Superadmin untuk $actionName produk '$productName'.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Password Superadmin") },
                    placeholder = { Text("Masukkan password...") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = if (passwordVisible) "Sembunyikan" else "Tampilkan")
                        }
                    },
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth().testTag("input_superadmin_auth_password")
                )
                errorMessage?.let {
                    Text(
                        text = it,
                        color = PosRose,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password.isBlank()) {
                        errorMessage = "Password tidak boleh kosong"
                        return@Button
                    }
                    isVerifying = true
                    onVerify(password) { error ->
                        isVerifying = false
                        errorMessage = error
                    }
                },
                enabled = !isVerifying && password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("btn_confirm_superadmin_auth")
            ) {
                Text("Verifikasi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun InventoryProductRow(
    product: ProductEntity,
    onEdit: () -> Unit,
    onAdjustStock: () -> Unit,
    onPrintBarcode: () -> Unit,
    onDelete: () -> Unit
) {
    val isLowStock = product.stock <= product.minStock

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inventory_row_${product.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Name & Barcode
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = product.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Barcode: ${product.barcode}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Quick Action Buttons (Print Barcode, Stock Adjust, Edit, Delete)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPrintBarcode,
                        modifier = Modifier.testTag("print_barcode_${product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Cetak Barcode",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onAdjustStock, modifier = Modifier.testTag("adjust_stock_${product.id}")) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "Sesuaikan Stok",
                            tint = PosEmerald
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_product_${product.id}")) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Produk",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_product_${product.id}")) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Produk",
                            tint = PosRose
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Pricing & Stock Meta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Harga Jual: ${CurrencyFormatter.formatRupiah(product.sellPrice)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = "Harga Beli: ${CurrencyFormatter.formatRupiah(product.costPrice)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Stock status badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isLowStock) PosAmberLight else PosEmeraldLight
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLowStock) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = PosAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = "Stok: ${product.stock} ${product.unit} (Min: ${product.minStock})",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isLowStock) PosAmber else PosEmerald
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductDialog(
    product: ProductEntity?,
    existingCategories: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onScanBarcodeRequest: ((String) -> Unit) -> Unit,
    onSave: (
        id: Long,
        barcode: String,
        name: String,
        category: String,
        sellPrice: Double,
        costPrice: Double,
        stock: Int,
        minStock: Int,
        unit: String
    ) -> Unit
) {
    val isEdit = product != null

    var name by remember { mutableStateOf(product?.name ?: "") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var category by remember { mutableStateOf(product?.category ?: if (existingCategories.isNotEmpty()) existingCategories.first() else "Sembako") }
    var sellPrice by remember { mutableStateOf(product?.sellPrice?.toLong()?.toString() ?: "") }
    var costPrice by remember { mutableStateOf(product?.costPrice?.toLong()?.toString() ?: "") }
    var stock by remember { mutableStateOf(product?.stock?.toString() ?: "10") }
    var minStock by remember { mutableStateOf(product?.minStock?.toString() ?: "5") }
    var unit by remember { mutableStateOf(product?.unit ?: "pcs") }

    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    // Combined unique category recommendations
    val defaultPresets = listOf("Sembako", "Minuman", "Makanan Ringan", "Bumbu Dapur", "Alat Tulis", "Perawatan", "Kebutuhan Rumah")
    val availableCategoryOptions = remember(existingCategories) {
        (existingCategories + defaultPresets).filter { it.isNotBlank() }.distinct()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isEdit) "Edit Produk" else "Tambah Produk Baru",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Produk *") },
                    placeholder = { Text("Contoh: Minyak Goreng 2L") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_product_name")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Barcode + Scan / Auto Gen button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Nomor Barcode *") },
                        placeholder = { Text("899xxxxxxx") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_product_barcode")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            onScanBarcodeRequest { scanned ->
                                barcode = scanned
                            }
                        },
                        modifier = Modifier.testTag("scan_barcode_for_input")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = {
                            barcode = "899" + (1000000000L..9999999999L).random().toString()
                        },
                        modifier = Modifier.testTag("generate_barcode_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Generate Barcode", tint = PosEmerald)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Category & Unit
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = isCategoryDropdownExpanded,
                        onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded },
                        modifier = Modifier.weight(1.3f)
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {
                                category = it
                                isCategoryDropdownExpanded = true
                            },
                            label = { Text("Kategori *") },
                            placeholder = { Text("Pilih kategori") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded)
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            singleLine = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("input_product_category")
                        )

                        val filteredCategories = availableCategoryOptions.filter {
                            it.contains(category, ignoreCase = true)
                        }.ifEmpty { availableCategoryOptions }

                        ExposedDropdownMenu(
                            expanded = isCategoryDropdownExpanded,
                            onDismissRequest = { isCategoryDropdownExpanded = false }
                        ) {
                            filteredCategories.forEach { categoryOption ->
                                DropdownMenuItem(
                                    text = { Text(categoryOption) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Category,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        category = categoryOption
                                        isCategoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Satuan") },
                        placeholder = { Text("pcs/bks") },
                        singleLine = true,
                        modifier = Modifier.weight(0.7f).testTag("input_product_unit")
                    )
                }

                // Quick Category Selector Chips
                Spacer(modifier = Modifier.height(6.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Pilih dari kategori yang sudah ada:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        availableCategoryOptions.take(8).forEach { catOption ->
                            val isSelected = category.equals(catOption, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { category = catOption },
                                label = { Text(catOption, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Prices
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sellPrice,
                        onValueChange = { sellPrice = it },
                        label = { Text("Harga Jual (Rp) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_product_sell_price")
                    )
                    OutlinedTextField(
                        value = costPrice,
                        onValueChange = { costPrice = it },
                        label = { Text("Harga Modal (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_product_cost_price")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stock & Min Stock Alert
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it },
                        label = { Text("Stok Awal *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_product_stock")
                    )
                    OutlinedTextField(
                        value = minStock,
                        onValueChange = { minStock = it },
                        label = { Text("Min. Alert") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_product_min_stock")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val parsedSell = sellPrice.toDoubleOrNull() ?: 0.0
                            val parsedCost = costPrice.toDoubleOrNull() ?: (parsedSell * 0.75)
                            val parsedStock = stock.toIntOrNull() ?: 0
                            val parsedMin = minStock.toIntOrNull() ?: 5

                            if (name.isNotBlank() && barcode.isNotBlank() && parsedSell > 0) {
                                onSave(
                                    product?.id ?: 0L,
                                    barcode,
                                    name,
                                    category.ifBlank { "Umum" },
                                    parsedSell,
                                    parsedCost,
                                    parsedStock,
                                    parsedMin,
                                    unit
                                )
                            }
                        },
                        enabled = name.isNotBlank() && barcode.isNotBlank() && (sellPrice.toDoubleOrNull() ?: 0.0) > 0,
                        modifier = Modifier.testTag("save_product_button")
                    ) {
                        Text("Simpan Produk")
                    }
                }
            }
        }
    }
}

@Composable
fun BarcodePrintDialog(
    product: ProductEntity,
    storeName: String,
    bluetoothPrinterName: String?,
    onDismiss: () -> Unit,
    onPrint: (copies: Int) -> Unit
) {
    var copies by remember { mutableStateOf(1) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cetak Barcode Label",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Clear, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Barcode Preview Card (Simulates Thermal Label)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (storeName.isNotBlank()) {
                            Text(
                                text = storeName.uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = CurrencyFormatter.formatRupiah(product.sellPrice),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Simulated Vertical Barcode Graphic
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(56.dp)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val barCount = 38
                                val step = size.width / barCount
                                for (i in 0 until barCount) {
                                    val isThick = (i % 3 == 0 || i % 7 == 0 || i == 0 || i == barCount - 1)
                                    val strokeWidth = if (isThick) step * 0.75f else step * 0.35f
                                    val x = i * step + step / 2
                                    drawLine(
                                        color = Color.Black,
                                        start = androidx.compose.ui.geometry.Offset(x, 0f),
                                        end = androidx.compose.ui.geometry.Offset(x, size.height),
                                        strokeWidth = strokeWidth
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = product.barcode,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = Color(0xFF1E293B)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Kategori: ${product.category} • Satuan: ${product.unit}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quantity / Number of Copies selector
                Text(
                    text = "Jumlah Lembar Cetak:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (copies > 1) copies-- },
                        enabled = copies > 1
                    ) {
                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Kurangi")
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "$copies Lembar",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    IconButton(
                        onClick = { if (copies < 50) copies++ },
                        enabled = copies < 50
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Tambah")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Preset quick chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    listOf(1, 2, 5, 10, 20).forEach { count ->
                        FilterChip(
                            selected = copies == count,
                            onClick = { copies = count },
                            label = { Text("$count") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bluetooth Printer Target Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (!bluetoothPrinterName.isNullOrBlank()) PosBlueLight else PosAmberLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            tint = if (!bluetoothPrinterName.isNullOrBlank()) MaterialTheme.colorScheme.primary else PosAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (!bluetoothPrinterName.isNullOrBlank()) {
                                "Target Printer: $bluetoothPrinterName"
                            } else {
                                "Printer Bluetooth belum dipilih (Atur di menu Pengaturan)"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = if (!bluetoothPrinterName.isNullOrBlank()) MaterialTheme.colorScheme.primary else PosAmber
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Print Action Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onPrint(copies)
                        },
                        modifier = Modifier.testTag("confirm_print_barcode_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cetak Label")
                    }
                }
            }
        }
    }
}

@Composable
fun StockAdjustmentDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirmAdjust: (amount: Int, reason: String) -> Unit
) {
    var amountText by remember { mutableStateOf("10") }
    var isAdding by remember { mutableStateOf(true) }
    var reason by remember { mutableStateOf("Restock Pembelian") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Sesuaikan Stok",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${product.name} (Saat ini: ${product.stock} ${product.unit})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle Tambah vs Kurang
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilterChip(
                        selected = isAdding,
                        onClick = {
                            isAdding = true
                            reason = "Restock Pembelian"
                        },
                        label = { Text("Tambah (+)") },
                        leadingIcon = { Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = PosEmerald) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = !isAdding,
                        onClick = {
                            isAdding = false
                            reason = "Penyesuaian / Rusak"
                        },
                        label = { Text("Kurang (-)") },
                        leadingIcon = { Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = PosRose) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Jumlah (${product.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("stock_adjust_amount_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Keterangan / Alasan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val qty = amountText.toIntOrNull() ?: 0
                            if (qty > 0) {
                                val delta = if (isAdding) qty else -qty
                                onConfirmAdjust(delta, reason)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAdding) PosEmerald else PosRose
                        )
                    ) {
                        Text(if (isAdding) "Simpan Tambah Stok" else "Simpan Kurangi Stok", color = Color.White)
                    }
                }
            }
        }
    }
}
