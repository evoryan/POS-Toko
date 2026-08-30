package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.local.entity.ProductEntity
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosEmeraldLight
import com.example.ui.theme.PosGreen
import com.example.ui.theme.PosOrange
import com.example.ui.theme.PosPrimary
import com.example.ui.viewmodel.PosViewModel
import com.example.utils.CurrencyFormatter
import com.example.utils.SoundHelper
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraBarcodeScannerDialog(
    viewModel: PosViewModel? = null,
    products: List<ProductEntity> = emptyList(),
    onDismiss: () -> Unit,
    onBarcodeScanned: ((String) -> Unit)? = null,
    onProductFound: ((ProductEntity) -> Unit)? = null,
    onAddProductRequested: ((scannedBarcode: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var isTorchEnabled by remember { mutableStateOf(false) }
    var scannedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var scannedCode by remember { mutableStateOf<String?>(null) }
    var isNotFound by remember { mutableStateOf(false) }
    var lastScannedTime by remember { mutableStateOf(0L) }
    var manualSearchCode by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    fun processBarcode(barcode: String) {
        val now = System.currentTimeMillis()
        if (now - lastScannedTime < 1200L) return // Debounce scanning
        lastScannedTime = now

        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isBlank()) return

        scannedCode = cleanBarcode

        if (onBarcodeScanned != null) {
            SoundHelper.playBeep()
            SoundHelper.vibrate(context, 60)
            onBarcodeScanned(cleanBarcode)
            return
        }

        isSearching = true

        coroutineScope.launch {
            val localList = products.ifEmpty { viewModel?.allProducts?.value ?: emptyList() }
            var product = localList.find { it.barcode.trim() == cleanBarcode }
            if (product == null && viewModel != null) {
                product = viewModel.repository.getProductByBarcode(cleanBarcode)
            }
            isSearching = false

            if (product != null) {
                scannedProduct = product
                isNotFound = false
                SoundHelper.playBeep()
                SoundHelper.vibrate(context, 60)
                if (onProductFound != null) {
                    onProductFound(product)
                }
            } else {
                scannedProduct = null
                isNotFound = true
                SoundHelper.playError()
                SoundHelper.vibrate(context, 100)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (!hasCameraPermission) {
                // Camera Permission Required UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = PosPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Izin Kamera Dibutuhkan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Aplikasi memerlukan izin akses kamera fisik perangkat untuk memindai barcode / QR code produk secara instan.",
                        textAlign = TextAlign.Center,
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = PosPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("grant_camera_permission_btn")
                    ) {
                        Text("Izinkan Kamera")
                    }

                    Spacer(Modifier.height(8.dp))

                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = Color.Gray)
                    }
                }
            } else {
                // Realtime CameraX ML Kit Barcode Scanner
                val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
                val barcodeScanner = remember {
                    val options = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                            Barcode.FORMAT_EAN_13,
                            Barcode.FORMAT_EAN_8,
                            Barcode.FORMAT_UPC_A,
                            Barcode.FORMAT_UPC_E,
                            Barcode.FORMAT_CODE_128,
                            Barcode.FORMAT_CODE_39,
                            Barcode.FORMAT_CODE_93,
                            Barcode.FORMAT_QR_CODE
                        )
                        .build()
                    BarcodeScanning.getClient(options)
                }

                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    barcodeScanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            for (barcode in barcodes) {
                                                val raw = barcode.rawValue
                                                if (!raw.isNullOrBlank()) {
                                                    coroutineScope.launch(Dispatchers.Main) {
                                                        processBarcode(raw)
                                                    }
                                                    break
                                                }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("CameraScanner", "Barcode scan failed: ${e.message}")
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }

                            try {
                                cameraProvider.unbindAll()
                                cameraInstance = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                Log.e("CameraScanner", "Use case binding failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                DisposableEffect(Unit) {
                    onDispose {
                        cameraExecutor.shutdown()
                        barcodeScanner.close()
                    }
                }

                // Visual Laser Overlay Scanner Frame
                TargetingScannerOverlay()

                // Top Controls Toolbar (Torch & Close)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                        modifier = Modifier.size(44.dp).testTag("close_camera_scanner_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = PosEmerald, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Pemindai Barcode Kamera",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val newTorchState = !isTorchEnabled
                            isTorchEnabled = newTorchState
                            cameraInstance?.cameraControl?.enableTorch(newTorchState)
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                        modifier = Modifier.size(44.dp).testTag("toggle_torch_btn")
                    ) {
                        Icon(
                            imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash",
                            tint = if (isTorchEnabled) PosOrange else Color.White
                        )
                    }
                }

                // Bottom Content & Result Cards
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    // Scanned Product Found Card
                    scannedProduct?.let { product ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.95f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .border(1.5.dp, PosEmerald, RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            product.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            "Barcode: ${product.barcode} • Kat: ${product.category}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.LightGray
                                        )
                                    }

                                    Surface(
                                        color = if (product.stock <= product.minStock) PosOrange.copy(alpha = 0.2f) else PosEmeraldLight,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Stok: ${product.stock} ${product.unit}",
                                            color = if (product.stock <= product.minStock) PosOrange else PosEmerald,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Harga Jual", fontSize = 11.sp, color = Color.Gray)
                                        Text(
                                            CurrencyFormatter.formatRupiah(product.sellPrice),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = Color(0xFF38BDF8)
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            viewModel?.addToCart(product, 1)
                                            SoundHelper.playSuccess()
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PosEmerald),
                                        modifier = Modifier.testTag("btn_scan_add_cart")
                                    ) {
                                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("+ Keranjang")
                                    }
                                }
                            }
                        }
                    }

                    // Scanned Code Not Found Alert
                    if (isNotFound && scannedCode != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.95f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .border(1.dp, PosOrange.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Produk Belum Terdaftar",
                                    fontWeight = FontWeight.Bold,
                                    color = PosOrange,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "Barcode '$scannedCode' belum ada di katalog.",
                                    color = Color.LightGray,
                                    fontSize = 13.sp
                                )
                                Spacer(Modifier.height(10.dp))
                                onAddProductRequested?.let { onAdd ->
                                    Button(
                                        onClick = {
                                            onAdd(scannedCode!!)
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PosPrimary),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("+ Daftarkan Produk Baru Ini")
                                    }
                                }
                            }
                        }
                    }

                    // Manual Search Box Barcode Fallback
                    Surface(
                        color = Color(0xFF0F172A).copy(alpha = 0.85f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = manualSearchCode,
                                onValueChange = { manualSearchCode = it },
                                placeholder = { Text("Ketik kode barcode manual...", color = Color.Gray, fontSize = 13.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = PosPrimary,
                                    unfocusedBorderColor = Color.DarkGray
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_manual_barcode")
                            )
                            Spacer(Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = {
                                    if (manualSearchCode.isNotBlank()) {
                                        processBarcode(manualSearchCode)
                                    }
                                },
                                modifier = Modifier.testTag("btn_manual_barcode_submit")
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Cari", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TargetingScannerOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "Laser")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserPosition"
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val boxWidth = maxWidth * 0.76f
        val boxHeight = 220.dp

        Box(
            modifier = Modifier
                .size(boxWidth, boxHeight)
                .border(2.dp, PosEmerald.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
        ) {
            // Scanner Laser Animation Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .offset(y = (boxHeight - 6.dp) * laserPosition)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                PosEmerald,
                                PosEmeraldLight,
                                PosEmerald,
                                Color.Transparent
                            )
                        )
                    )
            )

            // Scanning Instruction Label
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = "Arahkan barcode produk ke kotak ini",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
