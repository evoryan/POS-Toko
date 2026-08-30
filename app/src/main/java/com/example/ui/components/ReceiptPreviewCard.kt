package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PosPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReceiptPreviewCard(
    storeName: String,
    storeAddress: String,
    storePhone: String,
    receiptFooter: String,
    paperSize: String = "58mm",
    modifier: Modifier = Modifier
) {
    val is80mm = paperSize == "80mm"
    val maxPaperWidth = if (is80mm) 380.dp else 300.dp
    val currentDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(Date())

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_receipt_preview")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = PosPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Live Preview Struk Fisik",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                Surface(
                    color = PosPrimary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Ukuran Kertas: $paperSize",
                        color = PosPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Realistic Thermal Paper Receipt Container
            Surface(
                color = Color(0xFFFFFFFD),
                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 10.dp, bottomEnd = 10.dp),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .width(maxPaperWidth)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Store Icon
                    Icon(
                        imageVector = Icons.Default.Store,
                        contentDescription = null,
                        tint = Color(0xFF334155),
                        modifier = Modifier.size(26.dp)
                    )

                    Spacer(Modifier.height(4.dp))

                    // Store Name
                    Text(
                        text = if (storeName.isNotBlank()) storeName.uppercase() else "NAMA TOKO ANDA",
                        fontSize = if (is80mm) 14.sp else 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )

                    // Address
                    if (storeAddress.isNotBlank()) {
                        Text(
                            text = storeAddress,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF475569),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Phone
                    if (storePhone.isNotBlank()) {
                        Text(
                            text = "Telp: $storePhone",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF475569),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }

                    ReceiptDashedDivider()

                    // Transaction Metadata
                    ReceiptMetaRow("No. Struk", "INV-20260830-882")
                    ReceiptMetaRow("Tanggal", currentDate)
                    ReceiptMetaRow("Kasir", "Kasir Utama")
                    ReceiptMetaRow("Metode", "TUNAI")

                    ReceiptDashedDivider()

                    // Sample Items
                    ReceiptItemRow("Minyak Goreng 2L", "1 x 34.000", "34.000")
                    ReceiptItemRow("Beras Ramos 5kg", "1 x 72.500", "72.500")
                    ReceiptItemRow("Indomie Goreng 85g", "3 x 3.500", "10.500")

                    ReceiptDashedDivider()

                    // Calculation
                    ReceiptCalculatedRow("Subtotal", "Rp 117.000")
                    ReceiptCalculatedRow("Diskon", "-Rp 5.000")
                    ReceiptCalculatedRow("TOTAL", "Rp 112.000", isBold = true, fontSize = 13)
                    ReceiptCalculatedRow("Bayar (TUNAI)", "Rp 120.000")
                    ReceiptCalculatedRow("Kembalian", "Rp 8.000")

                    ReceiptDashedDivider()

                    // Custom Footer
                    Text(
                        text = if (receiptFooter.isNotBlank()) receiptFooter else "Terima kasih atas kunjungan Anda!",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF334155),
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )

                    Spacer(Modifier.height(6.dp))

                    // Mock Barcode Graphic representation
                    MockReceiptBarcode()

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "pos.akbarmediagroup.me",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ReceiptMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF64748B)
        )
        Text(
            value,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
    }
}

@Composable
private fun ReceiptItemRow(name: String, qtyPrice: String, subtotal: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            name,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "  $qtyPrice",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF475569)
            )
            Text(
                subtotal,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun ReceiptCalculatedRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    fontSize: Int = 10
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isBold) 2.dp else 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = fontSize.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) Color.Black else Color(0xFF475569)
        )
        Text(
            value,
            fontSize = fontSize.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = Color.Black
        )
    }
}

@Composable
private fun ReceiptDashedDivider() {
    Text(
        text = "- - - - - - - - - - - - - - - - - - - -",
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        color = Color(0xFF94A3B8),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

@Composable
private fun MockReceiptBarcode() {
    Row(
        modifier = Modifier
            .height(24.dp)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pattern of barcode vertical bars
        val pattern = listOf(2, 1, 3, 1, 2, 4, 1, 2, 1, 3, 2, 1, 4, 2, 1, 3, 1, 2, 4, 1, 2)
        pattern.forEachIndexed { index, width ->
            val isBar = index % 2 == 0
            Box(
                modifier = Modifier
                    .width(width.dp)
                    .height(20.dp)
                    .background(if (isBar) Color.Black else Color.Transparent)
            )
            Spacer(Modifier.width(1.dp))
        }
    }
}
