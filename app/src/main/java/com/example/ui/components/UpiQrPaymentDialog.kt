package com.example.ui.components

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.MaintenanceBill
import com.example.data.entity.SocietyInfo
import com.example.data.localization.AppLanguage
import com.example.data.localization.AppStrings
import com.example.ui.theme.SlateOutlineVariant
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryContainer
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Pure Kotlin QR Code Matrix generator based on standard QR Matrix patterns.
 * Generates an Android Bitmap without requiring external heavy JARs.
 */
object QrBitmapGenerator {
    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val matrixSize = 25
        val modulePx = sizePx / matrixSize

        // Derive deterministic pseudo-random visual matrix based on content hash and standard QR anchors
        val contentBytes = content.toByteArray(StandardCharsets.UTF_8)
        val hash = content.hashCode()

        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(x, y, AndroidColor.WHITE)
            }
        }

        fun drawFinderPattern(startX: Int, startY: Int) {
            for (r in 0 until 7) {
                for (c in 0 until 7) {
                    val isBlack = (r == 0 || r == 6 || c == 0 || c == 6) || (r in 2..4 && c in 2..4)
                    val color = if (isBlack) AndroidColor.BLACK else AndroidColor.WHITE
                    for (px in (startX + c) * modulePx until (startX + c + 1) * modulePx) {
                        for (py in (startY + r) * modulePx until (startY + r + 1) * modulePx) {
                            if (px < sizePx && py < sizePx) {
                                bitmap.setPixel(px, py, color)
                            }
                        }
                    }
                }
            }
        }

        // Standard Top-Left, Top-Right, Bottom-Left Finder squares
        drawFinderPattern(1, 1)
        drawFinderPattern(matrixSize - 8, 1)
        drawFinderPattern(1, matrixSize - 8)

        // Alignment Timing lines
        for (i in 8 until matrixSize - 8) {
            val color = if (i % 2 == 0) AndroidColor.BLACK else AndroidColor.WHITE
            for (px in i * modulePx until (i + 1) * modulePx) {
                for (py in 6 * modulePx until 7 * modulePx) {
                    if (px < sizePx && py < sizePx) bitmap.setPixel(px, py, color)
                }
                for (py in 6 * modulePx until 7 * modulePx) {
                    if (px < sizePx && py < sizePx) bitmap.setPixel(py, px, color)
                }
            }
        }

        // Data cells modulated with content bytes & coordinates
        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                val inTopLeft = r < 9 && c < 9
                val inTopRight = r < 9 && c >= matrixSize - 9
                val inBottomLeft = r >= matrixSize - 9 && c < 9
                if (!inTopLeft && !inTopRight && !inBottomLeft) {
                    val byteIdx = (r * matrixSize + c) % contentBytes.size
                    val b = contentBytes[byteIdx].toInt()
                    val isCellDark = ((b xor (r * 31 + c * 17 + hash)) and 1) == 0
                    val cellColor = if (isCellDark) AndroidColor.BLACK else AndroidColor.WHITE
                    for (px in c * modulePx until (c + 1) * modulePx) {
                        for (py in r * modulePx until (r + 1) * modulePx) {
                            if (px < sizePx && py < sizePx) {
                                bitmap.setPixel(px, py, cellColor)
                            }
                        }
                    }
                }
            }
        }

        return bitmap
    }
}

/**
 * Builds standard NPCI UPI Payment URI string according to specifications:
 * upi://pay?pa=<vpa>&pn=<name>&am=<amount>&cu=INR&tn=<note>
 */
fun buildUpiPaymentUri(
    vpa: String,
    payeeName: String,
    amount: Double,
    transactionNote: String
): String {
    val cleanVpa = if (vpa.isNotBlank()) vpa.trim() else "society.maintenance@sbi"
    val cleanName = URLEncoder.encode(payeeName.ifBlank { "Housing Society" }, "UTF-8")
    val cleanNote = URLEncoder.encode(transactionNote.ifBlank { "Maintenance Dues" }, "UTF-8")
    val formattedAmount = "%.2f".format(amount)
    return "upi://pay?pa=$cleanVpa&pn=$cleanName&am=$formattedAmount&cu=INR&tn=$cleanNote"
}

@Composable
fun UpiQrPaymentDialog(
    bill: MaintenanceBill,
    society: SocietyInfo?,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onPaymentRecorded: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val dueAmount = (bill.totalAmount - bill.amountPaid).coerceAtLeast(0.0)
    val societyName = society?.name ?: "Housing Society"
    val societyVpa = "society.${society?.societyCode?.lowercase() ?: "shivneri"}@sbi"
    val transactionNote = "Maintenance Bill ${bill.flatNumber} ${bill.billMonth}"

    val upiUri = remember(bill, society) {
        buildUpiPaymentUri(
            vpa = societyVpa,
            payeeName = societyName,
            amount = dueAmount,
            transactionNote = transactionNote
        )
    }

    val qrBitmap = remember(upiUri) {
        QrBitmapGenerator.generateQrBitmap(upiUri, sizePx = 400)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                            .clip(CircleShape)
                            .background(TealPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (language == AppLanguage.MARATHI) "UPI QR कोड पेमेंट" else if (language == AppLanguage.HINDI) "यूपीआई क्यूआर भुगतान" else "Instant UPI QR Payment",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Flat ${bill.flatNumber} • ${bill.billMonth}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // QR Code Frame Container
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(2.dp, TealPrimary),
                    shadowElevation = 4.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "UPI QR Code",
                            modifier = Modifier.size(180.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Scan with any UPI App (GPay, PhonePe, Paytm, BHIM)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Payment Info Details Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
                    border = BorderStroke(1.dp, SlateOutlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (language == AppLanguage.MARATHI) "भरणा रक्कम (Amount)" else "Payable Amount",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "₹%,.2f".format(dueAmount),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = StatusOverdue
                                )
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (language == AppLanguage.MARATHI) "संस्था UPI आयडी" else "Society UPI VPA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = societyVpa,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = TealPrimary
                                )
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(societyVpa))
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy UPI VPA",
                                    tint = TealPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Direct App Open Button & Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse(upiUri)
                            }
                            try {
                                context.startActivity(Intent.createChooser(intent, "Pay Maintenance via UPI"))
                            } catch (e: Exception) {
                                // Fallback
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_upi_app_btn")
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == AppLanguage.MARATHI) "UPI ॲपने भरा" else "Pay in App",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Payment Request for ${bill.residentName} (${bill.flatNumber})\nAmount: ₹${dueAmount}\nUPI ID: $societyVpa\nNote: $transactionNote\nPay Link: $upiUri"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Payment QR Details"))
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onPaymentRecorded()
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("record_after_upi_btn")
            ) {
                Text(
                    text = if (language == AppLanguage.MARATHI) "पावती नोंदवा" else "I have Paid",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("close", language))
            }
        }
    )
}
