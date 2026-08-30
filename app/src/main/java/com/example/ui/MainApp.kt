package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.entity.ProductEntity
import com.example.ui.components.CameraBarcodeScannerDialog
import com.example.ui.navigation.Screen
import com.example.ui.screens.inventory.InventoryScreen
import com.example.ui.screens.login.LoginScreen
import com.example.ui.screens.pos.PosScreen
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.theme.PosAmber
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosEmeraldLight
import com.example.ui.theme.PosPrimary
import com.example.ui.theme.PosRose
import com.example.ui.viewmodel.PosViewModel
import com.example.utils.CurrencyFormatter
import com.example.utils.SoundHelper

data class NavItemSpec(
    val title: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun MainApp(
    viewModel: PosViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userRole by viewModel.currentUserRole.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Central Floating Barcode Scanner Modal State
    var showGlobalScanner by remember { mutableStateOf(false) }
    var scannedQuickProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var unknownBarcodeScanned by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessage()
        }
    }

    val isAuthScreen = currentRoute == Screen.Splash.route || currentRoute == Screen.Login.route

    val navLeftItems = remember {
        listOf(
            NavItemSpec("Kasir POS", Screen.Pos.route, Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale),
            NavItemSpec("Stok Produk", Screen.Inventory.route, Icons.Filled.Inventory2, Icons.Outlined.Inventory2)
        )
    }

    val navRightItems = remember {
        listOf(
            NavItemSpec("Laporan", Screen.Reports.route, Icons.Filled.Assessment, Icons.Outlined.Assessment),
            NavItemSpec("Pengaturan", Screen.Settings.route, Icons.Filled.Settings, Icons.Outlined.Settings)
        )
    }

    // Global Camera Barcode Scanner Dialog
    if (showGlobalScanner) {
        CameraBarcodeScannerDialog(
            products = allProducts,
            onDismiss = { showGlobalScanner = false },
            onProductFound = { product ->
                showGlobalScanner = false
                scannedQuickProduct = product
            },
            onBarcodeScanned = { barcode ->
                showGlobalScanner = false
                val found = allProducts.find { it.barcode.trim() == barcode.trim() }
                if (found != null) {
                    scannedQuickProduct = found
                } else {
                    unknownBarcodeScanned = barcode
                }
            }
        )
    }

    // Quick Product Action Modal after Scan
    scannedQuickProduct?.let { product ->
        AlertDialog(
            onDismissRequest = { scannedQuickProduct = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PosEmeraldLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = PosEmerald, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Produk Ditemukan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    IconButton(onClick = { scannedQuickProduct = null }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = product.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Barcode: ${product.barcode} • Kategori: ${product.category}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = CurrencyFormatter.formatRupiah(product.sellPrice),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = PosEmerald
                                )

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (product.stock <= product.minStock) PosRose.copy(alpha = 0.15f) else PosEmeraldLight
                                ) {
                                    Text(
                                        text = "Stok: ${product.stock} ${product.unit}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (product.stock <= product.minStock) PosRose else PosEmerald,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addToCart(product)
                        scannedQuickProduct = null
                        if (currentRoute != Screen.Pos.route) {
                            navController.navigate(Screen.Pos.route)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PosEmerald),
                    modifier = Modifier.testTag("btn_quick_add_cart")
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Tambah ke Kasir")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        viewModel.searchQuery.value = product.barcode
                        scannedQuickProduct = null
                        if (currentRoute != Screen.Inventory.route) {
                            navController.navigate(Screen.Inventory.route)
                        }
                    },
                    modifier = Modifier.testTag("btn_quick_view_inventory")
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Buka di Stok")
                }
            }
        )
    }

    // Modal when unknown barcode scanned
    unknownBarcodeScanned?.let { barcode ->
        AlertDialog(
            onDismissRequest = { unknownBarcodeScanned = null },
            title = {
                Text("Barcode Belum Terdaftar", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Barcode '$barcode' belum ada dalam database katalog.")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Buka menu Stok Produk untuk mendaftarkan barcode ini ke sistem.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.searchQuery.value = barcode
                        unknownBarcodeScanned = null
                        navController.navigate(Screen.Inventory.route)
                    }
                ) {
                    Text("Daftarkan di Stok")
                }
            },
            dismissButton = {
                TextButton(onClick = { unknownBarcodeScanned = null }) {
                    Text("Tutup")
                }
            }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 840.dp

        if (isWideScreen && !isAuthScreen) {
            // Adaptive Tablet / Desktop Layout with Side NavigationRail
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxHeight().testTag("tablet_navigation_rail"),
                    header = {
                        FloatingActionButton(
                            onClick = { showGlobalScanner = true },
                            containerColor = PosPrimary,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.padding(vertical = 12.dp).testTag("floating_scan_button_rail")
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode")
                        }
                    }
                ) {
                    val allItems = navLeftItems + navRightItems
                    allItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        val badgeCount = if (item.route == Screen.Inventory.route) lowStockProducts.size else 0

                        NavigationRailItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                if (badgeCount > 0) {
                                    BadgedBox(badge = { Badge { Text("$badgeCount") } }) {
                                        Icon(
                                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.title
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.title
                                    )
                                }
                            },
                            label = { Text(item.title) },
                            modifier = Modifier.testTag("nav_rail_${item.route}")
                        )
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    modifier = Modifier.weight(1f)
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        AppNavHost(
                            navController = navController,
                            viewModel = viewModel,
                            isLoggedIn = isLoggedIn
                        )
                    }
                }
            }
        } else {
            // Phone / Compact Layout with Bottom NavigationBar + Centered Floating Scan Button
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!isAuthScreen) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Transparent),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // Bottom Navigation Surface Bar
                            Surface(
                                tonalElevation = 8.dp,
                                shadowElevation = 12.dp,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("bottom_navigation_bar")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(68.dp)
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Left Navigation Items
                                    navLeftItems.forEach { item ->
                                        val isSelected = currentRoute == item.route
                                        val badgeCount = if (item.route == Screen.Inventory.route) lowStockProducts.size else 0

                                        NavBarButton(
                                            item = item,
                                            isSelected = isSelected,
                                            badgeCount = badgeCount,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                if (currentRoute != item.route) {
                                                    navController.navigate(item.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            }
                                        )
                                    }

                                    // Center Spacer for Floating Button
                                    Spacer(modifier = Modifier.width(68.dp))

                                    // Right Navigation Items
                                    navRightItems.forEach { item ->
                                        val isSelected = currentRoute == item.route

                                        NavBarButton(
                                            item = item,
                                            isSelected = isSelected,
                                            badgeCount = 0,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                if (currentRoute != item.route) {
                                                    navController.navigate(item.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // Centered Floating Scan Button (Tengah Navbar & Floating)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(y = (-20).dp),
                                contentAlignment = Alignment.Center
                            ) {
                                FloatingActionButton(
                                    onClick = {
                                        SoundHelper.playClick()
                                        showGlobalScanner = true
                                    },
                                    containerColor = PosPrimary,
                                    contentColor = Color.White,
                                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp, pressedElevation = 12.dp),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .shadow(10.dp, shape = CircleShape, ambientColor = PosPrimary, spotColor = PosPrimary)
                                        .testTag("floating_scan_button_center")
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = "Pindai Barcode Kamera",
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                    AppNavHost(
                        navController = navController,
                        viewModel = viewModel,
                        isLoggedIn = isLoggedIn
                    )
                }
            }
        }
    }
}

@Composable
fun NavBarButton(
    item: NavItemSpec,
    isSelected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp)
            .testTag("bottom_nav_${item.route}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (badgeCount > 0) {
            BadgedBox(badge = { Badge(containerColor = PosRose) { Text("$badgeCount") } }) {
                Icon(
                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.title,
                    tint = if (isSelected) PosPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.title,
                tint = if (isSelected) PosPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.height(2.dp))

        Text(
            text = item.title,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) PosPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
fun AppNavHost(
    navController: androidx.navigation.NavHostController,
    viewModel: PosViewModel,
    isLoggedIn: Boolean
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                isLoggedIn = isLoggedIn,
                onNavigateNext = { loggedIn ->
                    if (loggedIn) {
                        navController.navigate(Screen.Pos.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Pos.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Pos.route) {
            PosScreen(viewModel = viewModel)
        }

        composable(Screen.Inventory.route) {
            InventoryScreen(viewModel = viewModel)
        }

        composable(Screen.Reports.route) {
            ReportsScreen(viewModel = viewModel)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onLoggedOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    }
                }
            )
        }
    }
}
