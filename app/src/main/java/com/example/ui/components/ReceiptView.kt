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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.pref.AppPreferences
import com.example.ui.theme.PosEmerald
import com.example.utils.CurrencyFormatter
import com.example.utils.DateUtils
import com.example.utils.ReceiptPrinterHelper

@Composable
fun ReceiptDialog(
    transaction: TransactionEntity,
    items: List<TransactionItemEntity>,
    prefs: AppPreferences,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with success badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = PosEmerald,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Struk Pembayaran",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_receipt_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Thermal Receipt Card with realistic Paper styling
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFAFAFA)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Store Info
                        Text(
                            text = prefs.storeName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            textAlign = TextAlign.Center,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = prefs.storeAddress,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            textAlign = TextAlign.Center,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "Telp: ${prefs.storePhone}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            textAlign = TextAlign.Center,
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        DashedDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Transaction Meta
                        ReceiptRow("No. Struk", transaction.invoiceNo)
                        ReceiptRow("Tanggal", DateUtils.formatDateTime(transaction.timestamp))
                        ReceiptRow("Kasir", transaction.cashierName)
                        ReceiptRow("Metode", transaction.paymentMethod)

                        Spacer(modifier = Modifier.height(8.dp))
                        DashedDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Item Lines
                        items.forEach { item ->
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Text(
                                    text = item.productName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    ),
                                    color = Color(0xFF1E293B)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "  ${item.quantity} x ${CurrencyFormatter.formatNumber(item.unitPrice)}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        ),
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(item.subtotal),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        ),
                                        color = Color(0xFF1E293B)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        DashedDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Totals
                        ReceiptRow("Subtotal", CurrencyFormatter.formatRupiah(transaction.totalAmount))
                        if (transaction.discount > 0) {
                            ReceiptRow("Diskon", "-${CurrencyFormatter.formatRupiah(transaction.discount)}")
                        }
                        ReceiptRow("TOTAL", CurrencyFormatter.formatRupiah(transaction.finalAmount), isBold = true)
                        ReceiptRow("Bayar (${transaction.paymentMethod})", CurrencyFormatter.formatRupiah(transaction.amountPaid))
                        ReceiptRow("Kembalian", CurrencyFormatter.formatRupiah(transaction.changeAmount), isBold = true)

                        Spacer(modifier = Modifier.height(12.dp))
                        DashedDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Footer Note & Web App URL
                        Text(
                            text = prefs.receiptFooter,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            textAlign = TextAlign.Center,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Web: toko.akbarmediagroup.me:4760",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center,
                            color = Color(0xFF3B82F6)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons (Print, Share, Selesai)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val text = ReceiptPrinterHelper.generateReceiptText(
                                storeName = prefs.storeName,
                                storeAddress = prefs.storeAddress,
                                storePhone = prefs.storePhone,
                                transaction = transaction,
                                items = items,
                                footerNote = prefs.receiptFooter,
                                paperSize = prefs.paperSize
                            )
                            ReceiptPrinterHelper.shareReceiptText(context, text)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_receipt_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bagikan")
                    }

                    Button(
                        onClick = {
                            ReceiptPrinterHelper.printReceipt(
                                context = context,
                                storeName = prefs.storeName,
                                storeAddress = prefs.storeAddress,
                                storePhone = prefs.storePhone,
                                transaction = transaction,
                                items = items,
                                footerNote = prefs.receiptFooter,
                                paperSize = prefs.paperSize
                            )
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("print_receipt_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cetak Struk")
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                fontSize = if (isBold) 13.sp else 11.sp
            ),
            color = if (isBold) Color(0xFF0F172A) else Color(0xFF475569)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                fontSize = if (isBold) 13.sp else 11.sp
            ),
            color = if (isBold) Color(0xFF0F172A) else Color(0xFF1E293B)
        )
    }
}

@Composable
fun DashedDivider() {
    Text(
        text = "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - -",
        style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp
        ),
        color = Color(0xFF94A3B8),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
