package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PosDatabase
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.UserEntity
import com.example.data.pref.AppPreferences
import com.example.data.repository.CartItem
import com.example.data.repository.DailySalesReport
import com.example.data.repository.PosRepository
import com.example.data.repository.TransactionResult
import com.example.utils.BluetoothPrinterHelper
import com.example.utils.DateUtils
import com.example.utils.SoundHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PosDatabase.getDatabase(application)
    val prefs = AppPreferences(application)
    val repository = PosRepository(application, db.productDao(), db.transactionDao(), db.userDao(), prefs)

    // UI Message / Toast state
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Auth State
    private val _isLoggedIn = MutableStateFlow(prefs.isLoggedIn)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUserRole = MutableStateFlow(prefs.userRole)
    val currentUserRole: StateFlow<String> = _currentUserRole.asStateFlow()

    private val _currentUserName = MutableStateFlow(prefs.userName)
    val currentUserName: StateFlow<String> = _currentUserName.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    // Users (Superadmin only)
    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Products & Categories
    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<ProductEntity>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<String?>(null)

    // Filtered Products for Cashier / POS Screen
    val filteredProducts: StateFlow<List<ProductEntity>> = combine(
        allProducts,
        searchQuery,
        selectedCategory
    ) { products, query, category ->
        products.filter { product ->
            val matchesQuery = query.isBlank() ||
                product.name.contains(query, ignoreCase = true) ||
                product.barcode.contains(query, ignoreCase = true) ||
                product.category.contains(query, ignoreCase = true)

            val matchesCategory = category == null || product.category == category

            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart Management
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    val discountAmount = MutableStateFlow(0.0)

    val cartTotal: StateFlow<Double> = _cartItems.combine(discountAmount) { items, _ ->
        items.sumOf { it.product.sellPrice * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartFinalAmount: StateFlow<Double> = combine(cartTotal, discountAmount) { total, discount ->
        (total - discount).coerceAtLeast(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartItemCount: StateFlow<Int> = _cartItems.combine(cartTotal) { items, _ ->
        items.sumOf { it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Checkout & Last Receipt State
    private val _isCheckingOut = MutableStateFlow(false)
    val isCheckingOut: StateFlow<Boolean> = _isCheckingOut.asStateFlow()

    private val _lastTransactionResult = MutableStateFlow<TransactionResult?>(null)
    val lastTransactionResult: StateFlow<TransactionResult?> = _lastTransactionResult.asStateFlow()

    // Reports State
    val selectedReportDate = MutableStateFlow(System.currentTimeMillis())
    private val _dailyReport = MutableStateFlow<DailySalesReport?>(null)
    val dailyReport: StateFlow<DailySalesReport?> = _dailyReport.asStateFlow()

    private val _isReportLoading = MutableStateFlow(false)
    val isReportLoading: StateFlow<Boolean> = _isReportLoading.asStateFlow()

    // Backend Connection Status
    private val _isServerOnline = MutableStateFlow<Boolean?>(null)
    val isServerOnline: StateFlow<Boolean?> = _isServerOnline.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            checkServerStatus()
            loadDailyReport(System.currentTimeMillis())
            refreshLocalBackups()
        }
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    // Authentication
    fun login(username: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            val result = repository.loginRemote(username, pass)
            _isAuthLoading.value = false
            result.onSuccess { response ->
                _isLoggedIn.value = true
                _currentUserRole.value = prefs.userRole
                _currentUserName.value = prefs.userName
                SoundHelper.playSuccess()
                _userMessage.value = response.message ?: "Selamat datang, ${prefs.userName}!"
                onSuccess()
            }.onFailure { error ->
                SoundHelper.playError()
                _userMessage.value = error.message ?: "Gagal login"
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        prefs.logout()
        _isLoggedIn.value = false
        _currentUserRole.value = "kasir"
        _currentUserName.value = ""
        _cartItems.value = emptyList()
        onLoggedOut()
    }

    // Cart Operations
    fun addToCart(product: ProductEntity, quantity: Int = 1) {
        if (product.stock <= 0) {
            SoundHelper.playError()
            _userMessage.value = "Stok ${product.name} habis!"
            return
        }

        val currentList = _cartItems.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.product.id == product.id }

        if (existingIndex >= 0) {
            val currentQty = currentList[existingIndex].quantity
            if (currentQty + quantity > product.stock) {
                SoundHelper.playError()
                _userMessage.value = "Stok ${product.name} tidak cukup (sisa: ${product.stock})"
                return
            }
            currentList[existingIndex] = currentList[existingIndex].copy(quantity = currentQty + quantity)
        } else {
            if (quantity > product.stock) {
                SoundHelper.playError()
                _userMessage.value = "Stok ${product.name} tidak cukup (sisa: ${product.stock})"
                return
            }
            currentList.add(CartItem(product = product, quantity = quantity))
        }

        _cartItems.value = currentList
        SoundHelper.playBeep()
    }

    fun updateCartItemQuantity(productId: Long, newQty: Int) {
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val product = currentList[index].product
            if (newQty <= 0) {
                currentList.removeAt(index)
            } else if (newQty > product.stock) {
                _userMessage.value = "Maksimal stok ${product.name}: ${product.stock}"
                currentList[index] = currentList[index].copy(quantity = product.stock)
            } else {
                currentList[index] = currentList[index].copy(quantity = newQty)
            }
            _cartItems.value = currentList
        }
    }

    fun removeFromCart(productId: Long) {
        _cartItems.value = _cartItems.value.filter { it.product.id != productId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        discountAmount.value = 0.0
    }

    // Quick Barcode Scanning Add
    fun handleBarcodeScan(scannedCode: String) {
        val cleanBarcode = scannedCode.trim()
        if (cleanBarcode.isBlank()) return

        viewModelScope.launch {
            val product = repository.getProductByBarcode(cleanBarcode)
            if (product != null) {
                addToCart(product, 1)
                _userMessage.value = "Ditambahkan: ${product.name}"
            } else {
                SoundHelper.playError()
                _userMessage.value = "Produk barcode '$cleanBarcode' tidak terdaftar"
            }
        }
    }

    // Checkout
    fun checkout(
        paymentMethod: String,
        amountPaid: Double,
        notes: String = "",
        onSuccess: (TransactionResult) -> Unit
    ) {
        viewModelScope.launch {
            _isCheckingOut.value = true
            val result = repository.processCheckout(
                cashierName = prefs.userName,
                cartItems = _cartItems.value,
                discount = discountAmount.value,
                paymentMethod = paymentMethod,
                amountPaid = amountPaid,
                notes = notes
            )
            _isCheckingOut.value = false

            result.onSuccess { txResult ->
                _lastTransactionResult.value = txResult
                clearCart()
                SoundHelper.playSuccess()
                _userMessage.value = "Transaksi berhasil! No: ${txResult.transaction.invoiceNo}"
                // Refresh report
                loadDailyReport(System.currentTimeMillis())
                onSuccess(txResult)
            }.onFailure { error ->
                SoundHelper.playError()
                _userMessage.value = error.message ?: "Transaksi gagal"
            }
        }
    }

    fun clearLastTransaction() {
        _lastTransactionResult.value = null
    }

    // Stock & Inventory Management
    fun saveProduct(
        id: Long = 0,
        barcode: String,
        name: String,
        category: String,
        sellPrice: Double,
        costPrice: Double,
        stock: Int,
        minStock: Int,
        unit: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val product = ProductEntity(
                id = id,
                barcode = barcode.trim(),
                name = name.trim(),
                category = category.trim(),
                sellPrice = sellPrice,
                costPrice = costPrice,
                stock = stock,
                minStock = minStock,
                unit = unit.trim(),
                updatedAt = System.currentTimeMillis()
            )
            repository.insertOrUpdateProduct(product)
            _userMessage.value = "Produk '${product.name}' berhasil disimpan"
            onSuccess()
        }
    }

    fun deleteProduct(id: Long, name: String) {
        viewModelScope.launch {
            repository.deleteProduct(id)
            _userMessage.value = "Produk '$name' telah dihapus"
        }
    }

    fun adjustStock(productId: Long, amountToAdd: Int, reason: String = "") {
        viewModelScope.launch {
            val success = repository.adjustStock(productId, amountToAdd)
            if (success) {
                val action = if (amountToAdd >= 0) "penambahan" else "pengurangan"
                _userMessage.value = "Berhasil $action stok ($amountToAdd)"
            }
        }
    }

    // Reports
    fun loadDailyReport(dateTimestamp: Long) {
        selectedReportDate.value = dateTimestamp
        viewModelScope.launch {
            _isReportLoading.value = true
            val report = repository.getDailyReport(dateTimestamp)
            _dailyReport.value = report
            _isReportLoading.value = false
        }
    }

    // Backend sync & status
    fun checkServerStatus() {
        viewModelScope.launch {
            val isOnline = repository.checkServerHealth()
            _isServerOnline.value = isOnline
        }
    }

    fun syncDataWithRemote() {
        viewModelScope.launch {
            _isSyncing.value = true
            val syncResult = repository.syncAllProductsFromRemote()
            _isSyncing.value = false
            syncResult.onSuccess { count ->
                _userMessage.value = "Sinkronisasi berhasil! $count produk diperbarui dari server."
            }.onFailure { err ->
                _userMessage.value = "Sinkronisasi offline: ${err.localizedMessage}"
            }
        }
    }

    fun resetAndSeedCatalog() {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            _userMessage.value = "Katalog demo Toko Akbar siap digunakan!"
        }
    }

    // Superadmin: User Management
    fun addUser(username: String, pass: String, name: String, role: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = repository.addUser(username, pass, name, role)
            result.onSuccess { user ->
                _userMessage.value = "User '${user.name}' (${user.role}) berhasil ditambahkan!"
                SoundHelper.playSuccess()
                onSuccess()
            }.onFailure { err ->
                _userMessage.value = err.message ?: "Gagal menambah user"
                SoundHelper.playError()
            }
        }
    }

    fun deleteUser(userId: Long, username: String) {
        viewModelScope.launch {
            val result = repository.deleteUser(userId, username)
            result.onSuccess {
                _userMessage.value = "User '$username' berhasil dihapus"
            }.onFailure { err ->
                _userMessage.value = err.message ?: "Gagal menghapus user"
            }
        }
    }

    fun syncUsers() {
        viewModelScope.launch {
            val result = repository.syncUsersFromRemote()
            result.onSuccess { count ->
                _userMessage.value = "Berhasil sinkronisasi $count user dari server"
            }.onFailure { err ->
                _userMessage.value = "Sinkronisasi user: ${err.message}"
            }
        }
    }

    // Bluetooth Thermal Printing
    fun printBluetoothReceipt(
        transaction: com.example.data.local.entity.TransactionEntity,
        items: List<com.example.data.local.entity.TransactionItemEntity>
    ) {
        val mac = prefs.bluetoothPrinterMac
        if (mac.isNullOrBlank()) {
            _userMessage.value = "Printer Bluetooth belum dipasangkan di Pengaturan"
            return
        }

        viewModelScope.launch {
            val result = BluetoothPrinterHelper.printReceipt(
                macAddress = mac,
                storeName = prefs.storeName,
                storeAddress = prefs.storeAddress,
                storePhone = prefs.storePhone,
                transaction = transaction,
                items = items,
                footerNote = prefs.receiptFooter,
                paperSize = prefs.paperSize
            )
            result.onSuccess {
                _userMessage.value = "Struk berhasil dicetak ke printer Bluetooth"
                SoundHelper.playSuccess()
            }.onFailure { err ->
                _userMessage.value = err.message ?: "Gagal mencetak struk"
                SoundHelper.playError()
            }
        }
    }

    fun testPrintBluetooth(macAddress: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = BluetoothPrinterHelper.printTestReceipt(
                macAddress = macAddress,
                storeName = prefs.storeName,
                paperSize = prefs.paperSize
            )
            result.onSuccess {
                _userMessage.value = "Tes cetak berhasil terkirim ke printer Bluetooth!"
                SoundHelper.playSuccess()
                onResult(true, "Tes cetak berhasil!")
            }.onFailure { err ->
                val msg = err.message ?: "Gagal tes cetak"
                _userMessage.value = msg
                SoundHelper.playError()
                onResult(false, msg)
            }
        }
    }

    fun printProductBarcode(product: ProductEntity, copies: Int = 1, onResult: ((Boolean, String) -> Unit)? = null) {
        val mac = prefs.bluetoothPrinterMac
        if (mac.isNullOrBlank()) {
            val msg = "Printer Bluetooth belum dipilih di menu Pengaturan"
            _userMessage.value = msg
            SoundHelper.playError()
            onResult?.invoke(false, msg)
            return
        }

        viewModelScope.launch {
            val result = BluetoothPrinterHelper.printBarcodeLabel(
                macAddress = mac,
                storeName = prefs.storeName,
                product = product,
                copies = copies,
                paperSize = prefs.paperSize
            )
            result.onSuccess {
                val msg = "Label barcode '${product.name}' ($copies lembar) berhasil dicetak!"
                _userMessage.value = msg
                SoundHelper.playSuccess()
                onResult?.invoke(true, msg)
            }.onFailure { err ->
                val msg = err.message ?: "Gagal mencetak label barcode"
                _userMessage.value = msg
                SoundHelper.playError()
                onResult?.invoke(false, msg)
            }
        }
    }

    // Superadmin: Verify Superadmin Password for protected actions
    fun verifySuperadminPassword(pass: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val isValid = repository.verifySuperadminPassword(pass)
            if (!isValid) {
                SoundHelper.playError()
            }
            onResult(isValid)
        }
    }

    // Superadmin: Delete Transaction with automatic inventory restock
    fun deleteTransaction(invoiceNo: String, restoreStock: Boolean = true, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.deleteTransaction(invoiceNo, restoreStock)
            result.onSuccess {
                _userMessage.value = "Transaksi $invoiceNo berhasil dihapus (stok produk dikembalikan)"
                SoundHelper.playSuccess()
                loadDailyReport(selectedReportDate.value)
                onSuccess()
            }.onFailure { err ->
                _userMessage.value = "Gagal menghapus transaksi: ${err.message}"
                SoundHelper.playError()
            }
        }
    }

    // Backup and Restore Product Database
    private val _localBackups = MutableStateFlow<List<java.io.File>>(emptyList())
    val localBackups: StateFlow<List<java.io.File>> = _localBackups.asStateFlow()
    val savedBackups: StateFlow<List<java.io.File>> = _localBackups.asStateFlow()

    fun refreshLocalBackups() {
        _localBackups.value = repository.getSavedLocalBackups()
    }

    fun backupProductDatabase(onResult: ((Boolean, String, java.io.File?) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val json = repository.exportProductsJson()
                val file = repository.saveBackupToInternalStorage(json)
                refreshLocalBackups()
                val msg = "Backup berhasil disimpan: ${file.name}"
                _userMessage.value = msg
                SoundHelper.playSuccess()
                onResult?.invoke(true, msg, file)
            } catch (e: Exception) {
                val msg = "Gagal membuat backup: ${e.message}"
                _userMessage.value = msg
                SoundHelper.playError()
                onResult?.invoke(false, msg, null)
            }
        }
    }

    fun restoreProductDatabaseFromJson(file: java.io.File, overwrite: Boolean = true, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val jsonString = file.readText()
                val result = repository.importProductsFromJson(jsonString, overwrite)
                result.onSuccess { count ->
                    val msg = "Berhasil memulihkan $count produk ke database!"
                    _userMessage.value = msg
                    SoundHelper.playSuccess()
                    refreshLocalBackups()
                    onResult(true, msg)
                }.onFailure { err ->
                    val msg = err.message ?: "Gagal memulihkan database"
                    _userMessage.value = msg
                    SoundHelper.playError()
                    onResult(false, msg)
                }
            } catch (e: Exception) {
                val msg = "Gagal membaca file backup: ${e.message}"
                _userMessage.value = msg
                SoundHelper.playError()
                onResult(false, msg)
            }
        }
    }

    fun restoreProductDatabaseFromJson(jsonString: String, overwrite: Boolean = true, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.importProductsFromJson(jsonString, overwrite)
            result.onSuccess { count ->
                val msg = "Berhasil memulihkan $count produk ke database!"
                _userMessage.value = msg
                SoundHelper.playSuccess()
                refreshLocalBackups()
                onResult(true, msg)
            }.onFailure { err ->
                val msg = err.message ?: "Gagal memulihkan database"
                _userMessage.value = msg
                SoundHelper.playError()
                onResult(false, msg)
            }
        }
    }

    fun deleteBackupFile(file: java.io.File) {
        repository.deleteLocalBackup(file)
        refreshLocalBackups()
        _userMessage.value = "File backup '${file.name}' telah dihapus"
    }

    suspend fun getExportProductsJsonString(): String {
        return repository.exportProductsJson()
    }
}
