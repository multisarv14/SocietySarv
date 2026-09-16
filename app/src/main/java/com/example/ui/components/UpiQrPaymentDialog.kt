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
import androidx.compose.foundation.layout.PaddingValues
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
import com.example.data.entity.UnitDues
import com.example.data.entity.pendingAmount
import com.example.data.localization.AppLanguage
import com.example.data.localization.AppStrings
import com.example.ui.theme.SlateOutlineVariant
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusOverdueBg
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryContainer
import com.example.util.UpiQr
import com.example.util.rememberBlobImage

/**
 * What a member is shown when they tap Pay by QR.
 *
 * The amount asked for is the unit's whole position - this month, the arrears
 * before it, less any credit already paid in - rather than the one bill that
 * was tapped, because that is the figure a member actually settles.
 *
 * The QR itself is either the one the chairman uploaded or one generated from
 * the society's UPI id, in that order: a society whose bank issues its own QR
 * needs that exact image, and only a society without one falls back to the
 * generated code.
 */
@Composable
fun UpiQrPaymentDialog(
    bill: MaintenanceBill,
    society: SocietyInfo?,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onPaymentRecorded: () -> Unit,
    /**
     * The unit's arrears, current month and advance credit. Left null, the
     * dialog falls back to what this one bill still owes.
     */
    dues: UnitDues? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val thisBillPending = bill.pendingAmount
    val dueAmount = dues?.netPayable ?: thisBillPending
    val societyName = society?.name ?: "Housing Society"
    // The society's real UPI id. Nothing is invented here - a QR built from a
    // made-up address takes the member's money nowhere.
    val societyVpa = society?.upiId.orEmpty().trim()
    val uploadedQr = rememberBlobImage(society?.paymentQrBase64)
    val transactionNote = "Maintenance ${bill.flatNumber} ${bill.billMonth}"

    val upiUri = remember(societyVpa, societyName, dueAmount, transactionNote) {
        UpiQr.paymentUri(
            vpa = societyVpa,
            payeeName = societyName,
            amount = dueAmount,
            transactionNote = transactionNote
        )
    }

    // Generating the symbol walks the whole matrix eight times to score the
    // masks, so it is kept off recomposition and skipped entirely when an
    // uploaded QR is going to be shown instead.
    val generatedQr = remember(upiUri, uploadedQr) {
        if (uploadedQr != null || upiUri == null) {
            null
        } else {
            runCatching { UpiQr.bitmap(upiUri, sizePx = 512).asImageBitmap() }.getOrNull()
        }
    }

    val qrImage = uploadedQr ?: generatedQr
    val hasQr = qrImage != null

    AlertDialog(
        modifier = DialogWidth,
        properties = WideDialogProperties,
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
                            text = "Unit ${bill.flatNumber} • ${bill.billMonth}",
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
                // No UPI id and no uploaded image means no QR. Showing one
                // built from a made-up address would send the money nowhere.
                if (!hasQr) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = StatusOverdueBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .testTag("upi_not_configured")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (language == AppLanguage.MARATHI) {
                                    "संस्थेचा UPI आयडी अजून सेट केलेला नाही"
                                } else {
                                    "No UPI ID set for the society yet"
                                },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = StatusOverdue
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (language == AppLanguage.MARATHI) {
                                    "अध्यक्षांना Society Profile मध्ये UPI आयडी भरायला सांगा. तोपर्यंत बँक तपशिलाने भरा."
                                } else {
                                    "Ask the Chairman to add the society's UPI ID, or upload the " +
                                        "society's own payment QR, under Society Profile. Until " +
                                        "then, pay using the bank details."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
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
                                bitmap = qrImage!!,
                                contentDescription = "UPI QR Code",
                                modifier = Modifier
                                    .size(180.dp)
                                    .testTag(
                                        if (uploadedQr != null) "uploaded_upi_qr" else "generated_upi_qr"
                                    )
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
                        /*
                         * The whole position, not just the bill that was
                         * tapped: what this month came to, what was owed before
                         * it, and the credit the member is already in. A member
                         * paying off arrears needs to see all three or the
                         * amount on the QR looks wrong to them.
                         */
                        AmountRow(
                            label = if (language == AppLanguage.MARATHI) {
                                "${bill.billMonth} चे बिल"
                            } else {
                                "${bill.billMonth} bill"
                            },
                            amount = thisBillPending
                        )
                        if (dues != null && dues.previousPending > 0.009) {
                            AmountRow(
                                label = if (language == AppLanguage.MARATHI) {
                                    "मागील थकबाकी"
                                } else {
                                    "Previous pending"
                                },
                                amount = dues.previousPending
                            )
                        }
                        if (dues != null && dues.excessCredit > 0.009) {
                            AmountRow(
                                label = if (language == AppLanguage.MARATHI) {
                                    "जमा (जास्तीची) रक्कम"
                                } else {
                                    "Advance credit"
                                },
                                amount = -dues.excessCredit,
                                color = StatusPaid
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (language == AppLanguage.MARATHI) "एकूण भरणा रक्कम" else "Total Payable",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "₹%,.2f".format(dueAmount),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = StatusOverdue
                                ),
                                modifier = Modifier.testTag("upi_total_payable")
                            )
                        }

                        if (societyVpa.isNotBlank()) {
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
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Direct App Open Button & Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Only offered when there is a real address behind it. A
                    // society that only uploaded an image has nothing for a UPI
                    // app to open, and a dead button is worse than none.
                    OutlinedButton(
                        enabled = upiUri != null,
                        onClick = {
                            val target = upiUri ?: return@OutlinedButton
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse(target)
                            }
                            runCatching {
                                context.startActivity(Intent.createChooser(intent, "Pay Maintenance via UPI"))
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

                    // Given the same weight as Pay in App. Squeezed into a
                    // 48dp square it had no room for its own icon and rendered
                    // as an empty outline.
                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    buildString {
                                        append("Payment Request for ${bill.residentName} (${bill.flatNumber})\n")
                                        append("Amount: ₹%,.2f\n".format(dueAmount))
                                        if (societyVpa.isNotBlank()) append("UPI ID: $societyVpa\n")
                                        append("Note: $transactionNote")
                                        if (upiUri != null) append("\nPay Link: $upiUri")
                                    }
                                )
                            }
                            runCatching {
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Share Payment QR Details")
                                )
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_upi_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == AppLanguage.MARATHI) "शेअर करा" else "Share",
                            style = MaterialTheme.typography.labelSmall
                        )
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

/** One line of the payable breakdown: a label on the left, a rupee figure right. */
@Composable
private fun AmountRow(
    label: String,
    amount: Double,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Text(
            text = "₹%,.2f".format(amount),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = color
        )
    }
}
