package com.example.ui.screens.pos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ProductEntity
import com.example.data.repository.CartItem
import com.example.data.repository.TransactionResult
import com.example.ui.components.CameraBarcodeScannerDialog
import com.example.ui.components.CartItemRow
import com.example.ui.components.ProductCard
import com.example.ui.components.ProductListItem
import com.example.ui.components.ReceiptDialog
import com.example.ui.theme.PosBlueLight
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosEmeraldLight
import com.example.ui.theme.PosRose
import com.example.ui.viewmodel.PosViewModel
import com.example.utils.CurrencyFormatter
import com.example.utils.ReceiptPrinterHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: PosViewModel
) {
    val context = LocalContext.current
    val products by viewModel.filteredProducts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val cartItems by viewModel.cartItems.collectAsState()
    val cartTotal by viewModel.cartTotal.collectAsState()
    val cartFinalAmount by viewModel.cartFinalAmount.collectAsState()
    val cartItemCount by viewModel.cartItemCount.collectAsState()
    val discountAmount by viewModel.discountAmount.collectAsState()

    val lastTransactionResult by viewModel.lastTransactionResult.collectAsState()
    val isCheckingOut by viewModel.isCheckingOut.collectAsState()

    var showScannerDialog by remember { mutableStateOf(false) }
    var showPhoneCheckoutSheet by remember { mutableStateOf(false) }

    // Receipt Dialog triggers automatically when lastTransactionResult is non-null
    lastTransactionResult?.let { txResult ->
        ReceiptDialog(
            transaction = txResult.transaction,
            items = txResult.items,
            prefs = viewModel.prefs,
            onDismiss = { viewModel.clearLastTransaction() }
        )
    }

    if (showScannerDialog) {
        CameraBarcodeScannerDialog(
            products = products,
            onDismiss = { showScannerDialog = false },
            onProductFound = { product ->
                viewModel.addToCart(product)
                showScannerDialog = false
            },
            onBarcodeScanned = { barcode ->
                viewModel.handleBarcodeScan(barcode)
                showScannerDialog = false
            }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 760.dp

        if (isWideScreen) {
            // Dual Pane Layout for Tablet & Desktop
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Left Pane: Catalog & Scanner (60%)
                Column(
                    modifier = Modifier
                        .weight(1.35f)
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    PosHeaderSection(
                        searchQuery = searchQuery,
                        onSearchChange = { viewModel.searchQuery.value = it },
                        onOpenScanner = { showScannerDialog = true },
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelect = { viewModel.selectedCategory.value = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Products List (Kasir POS List View)
                    ProductListSection(
                        products = products,
                        onAddToCart = { viewModel.addToCart(it) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Right Pane: Active Cashier Order & Payment Calculator (40%)
                Card(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight()
                        .padding(top = 16.dp, end = 16.dp, bottom = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    CashierCheckoutPane(
                        cartItems = cartItems,
                        cartTotal = cartTotal,
                        cartFinalAmount = cartFinalAmount,
                        discount = discountAmount,
                        onDiscountChange = { viewModel.discountAmount.value = it },
                        onQuantityChange = { id, qty -> viewModel.updateCartItemQuantity(id, qty) },
                        onRemoveItem = { viewModel.removeFromCart(it) },
                        onClearCart = { viewModel.clearCart() },
                        onCheckout = { method, paid, notes ->
                            viewModel.checkout(method, paid, notes) { txResult ->
                                if (viewModel.prefs.autoPrint) {
                                    ReceiptPrinterHelper.printReceipt(
                                        context = context,
                                        storeName = viewModel.prefs.storeName,
                                        storeAddress = viewModel.prefs.storeAddress,
                                        storePhone = viewModel.prefs.storePhone,
                                        transaction = txResult.transaction,
                                        items = txResult.items,
                                        footerNote = viewModel.prefs.receiptFooter,
                                        paperSize = viewModel.prefs.paperSize
                                    )
                                }
                            }
                        },
                        isCheckingOut = isCheckingOut,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            // Compact Layout for Phone
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                PosHeaderSection(
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.searchQuery.value = it },
                    onOpenScanner = { showScannerDialog = true },
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelect = { viewModel.selectedCategory.value = it }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Products List (Kasir POS List View)
                ProductListSection(
                    products = products,
                    onAddToCart = { viewModel.addToCart(it) },
                    modifier = Modifier.weight(1f)
                )

                // Sticky Bottom Cart Bar
                if (cartItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        onClick = { showPhoneCheckoutSheet = true },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .testTag("floating_cart_bar")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$cartItemCount",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Total Belanja",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(cartFinalAmount),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Bayar",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Phone Checkout Modal Bottom Sheet
            if (showPhoneCheckoutSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showPhoneCheckoutSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ) {
                    CashierCheckoutPane(
                        cartItems = cartItems,
                        cartTotal = cartTotal,
                        cartFinalAmount = cartFinalAmount,
                        discount = discountAmount,
                        onDiscountChange = { viewModel.discountAmount.value = it },
                        onQuantityChange = { id, qty -> viewModel.updateCartItemQuantity(id, qty) },
                        onRemoveItem = { viewModel.removeFromCart(it) },
                        onClearCart = {
                            viewModel.clearCart()
                            showPhoneCheckoutSheet = false
                        },
                        onCheckout = { method, paid, notes ->
                            viewModel.checkout(method, paid, notes) { txResult ->
                                showPhoneCheckoutSheet = false
                                if (viewModel.prefs.autoPrint) {
                                    ReceiptPrinterHelper.printReceipt(
                                        context = context,
                                        storeName = viewModel.prefs.storeName,
                                        storeAddress = viewModel.prefs.storeAddress,
                                        storePhone = viewModel.prefs.storePhone,
                                        transaction = txResult.transaction,
                                        items = txResult.items,
                                        footerNote = viewModel.prefs.receiptFooter,
                                        paperSize = viewModel.prefs.paperSize
                                    )
                                }
                            }
                        },
                        isCheckingOut = isCheckingOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f)
                    )
                }
            }
        }
    }
}

@Composable
fun PosHeaderSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onOpenScanner: () -> Unit,
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelect: (String?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Search & Barcode Button Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Cari produk atau barcode...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Hapus pencarian")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("pos_search_input")
            )

            // Scan Barcode Trigger Button
            Button(
                onClick = onOpenScanner,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(56.dp)
                    .testTag("open_barcode_scanner_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan Barcode",
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scan", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Category Filter Pills
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategorySelect(null) },
                    label = { Text("Semua") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelect(if (selectedCategory == category) null else category) },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun ProductListSection(
    products: List<ProductEntity>,
    onAddToCart: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (products.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Store,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tidak ada produk ditemukan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize().testTag("product_list"),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(products, key = { it.id }) { product ->
                ProductListItem(
                    product = product,
                    onAddToCart = onAddToCart
                )
            }
        }
    }
}

// Backward-compatible alias
@Composable
fun ProductGridSection(
    products: List<ProductEntity>,
    onAddToCart: (ProductEntity) -> Unit,
    columns: Int = 1,
    modifier: Modifier = Modifier
) {
    ProductListSection(
        products = products,
        onAddToCart = onAddToCart,
        modifier = modifier
    )
}

@Composable
fun CashierCheckoutPane(
    cartItems: List<CartItem>,
    cartTotal: Double,
    cartFinalAmount: Double,
    discount: Double,
    onDiscountChange: (Double) -> Unit,
    onQuantityChange: (Long, Int) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: (paymentMethod: String, amountPaid: Double, notes: String) -> Unit,
    isCheckingOut: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedPaymentMethod by remember { mutableStateOf("TUNAI") }
    var cashPaidInput by remember { mutableStateOf("") }
    var discountInput by remember { mutableStateOf("") }
    var showDiscountInput by remember { mutableStateOf(false) }

    val numericCashPaid = cashPaidInput.toDoubleOrNull() ?: if (selectedPaymentMethod != "TUNAI") cartFinalAmount else 0.0
    val changeAmount = (numericCashPaid - cartFinalAmount).coerceAtLeast(0.0)
    val isPaymentSufficient = numericCashPaid >= cartFinalAmount && cartItems.isNotEmpty()

    val quickCashOptions = listOf(
        cartFinalAmount to "Uang Pas",
        10000.0 to "10 Ribu",
        20000.0 to "20 Ribu",
        50000.0 to "50 Ribu",
        100000.0 to "100 Ribu",
        200000.0 to "200 Ribu"
    ).filter { it.first >= cartFinalAmount || it.second == "Uang Pas" }

    Column(
        modifier = modifier
            .padding(16.dp)
    ) {
        // Pane Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Keranjang Kasir (${cartItems.sumOf { it.quantity }})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (cartItems.isNotEmpty()) {
                TextButton(
                    onClick = onClearCart,
                    colors = ButtonDefaults.textButtonColors(contentColor = PosRose),
                    modifier = Modifier.testTag("clear_cart_button")
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Scrollable Cart Items List & Payment Controls
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Keranjang kosong.\nPilih produk di sebelah atau scan barcode.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                cartItems.forEach { item ->
                    CartItemRow(
                        item = item,
                        onQuantityChange = { newQty -> onQuantityChange(item.product.id, newQty) },
                        onRemove = { onRemoveItem(item.product.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Payment Method Selector
            Text(
                text = "Metode Pembayaran",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val methods = listOf("TUNAI", "QRIS", "DEBIT/KARTU")
                methods.forEach { method ->
                    val isSelected = selectedPaymentMethod == method
                    Surface(
                        onClick = {
                            selectedPaymentMethod = method
                            if (method != "TUNAI") {
                                cashPaidInput = cartFinalAmount.toLong().toString()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.weight(1f).testTag("payment_method_$method")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (method) {
                                    "TUNAI" -> Icons.Default.Payments
                                    "QRIS" -> Icons.Default.QrCode
                                    else -> Icons.Default.CreditCard
                                },
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = method,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cash Input & Quick nominal buttons for Tunai
            if (selectedPaymentMethod == "TUNAI") {
                OutlinedTextField(
                    value = cashPaidInput,
                    onValueChange = { cashPaidInput = it },
                    label = { Text("Jumlah Uang Tunai Diterima") },
                    placeholder = { Text(CurrencyFormatter.formatRupiah(cartFinalAmount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cash_paid_input")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Cash Pills
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(quickCashOptions) { (nominal, label) ->
                        Surface(
                            onClick = { cashPaidInput = nominal.toLong().toString() },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.testTag("quick_cash_${nominal.toLong()}")
                        ) {
                            Text(
                                text = if (label == "Uang Pas") label else CurrencyFormatter.formatRupiah(nominal),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Discount Input Toggle
            if (!showDiscountInput) {
                TextButton(
                    onClick = { showDiscountInput = true },
                    modifier = Modifier.testTag("toggle_discount_button")
                ) {
                    Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah Diskon / Potongan Harga")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = discountInput,
                        onValueChange = {
                            discountInput = it
                            val parsed = it.toDoubleOrNull() ?: 0.0
                            onDiscountChange(parsed)
                        },
                        label = { Text("Nominal Diskon (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("discount_input")
                    )
                    IconButton(onClick = {
                        showDiscountInput = false
                        discountInput = ""
                        onDiscountChange(0.0)
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Batal diskon")
                    }
                }
            }
        }

        // Totals & Pay Checkout Button
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal:", style = MaterialTheme.typography.bodySmall)
                    Text(CurrencyFormatter.formatRupiah(cartTotal), style = MaterialTheme.typography.bodySmall)
                }
                if (discount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Diskon:", style = MaterialTheme.typography.bodySmall, color = PosEmerald)
                        Text("-${CurrencyFormatter.formatRupiah(discount)}", style = MaterialTheme.typography.bodySmall, color = PosEmerald)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("TOTAL TAGIHAN:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        CurrencyFormatter.formatRupiah(cartFinalAmount),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                if (selectedPaymentMethod == "TUNAI") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Kembalian:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        Text(
                            CurrencyFormatter.formatRupiah(changeAmount),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = PosEmerald
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main Pay & Print Action Button
        Button(
            onClick = {
                val paid = if (selectedPaymentMethod == "TUNAI") numericCashPaid else cartFinalAmount
                onCheckout(selectedPaymentMethod, paid, "")
            },
            enabled = isPaymentSufficient && !isCheckingOut,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PosEmerald,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("pay_and_print_button")
        ) {
            if (isCheckingOut) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Memproses Transaksi...")
            } else {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bayar & Cetak Struk (${CurrencyFormatter.formatRupiah(cartFinalAmount)})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
