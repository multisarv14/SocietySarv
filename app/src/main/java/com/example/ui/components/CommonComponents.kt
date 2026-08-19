package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.entity.MaintenanceBill
import com.example.data.entity.PaymentStatus
import com.example.data.entity.Resident
import com.example.data.entity.SocietyInfo
import com.example.data.entity.TicketStatus
import com.example.data.entity.UserRole
import com.example.data.entity.VisitorStatus
import com.example.data.localization.AppLanguage
import com.example.data.localization.AppStrings
import com.example.ui.theme.SlateOutlineVariant
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusOverdueBg
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.TealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocietyTopAppBar(
    societyInfo: SocietyInfo?,
    currentUser: Resident?,
    currentLanguage: AppLanguage,
    onToggleLanguage: () -> Unit,
    onSelectLanguage: ((AppLanguage) -> Unit)? = null,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onLogout: () -> Unit,
    onSwitchProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLangMenu by remember { mutableStateOf(false) }

    Surface(
        color = TealPrimary,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = "Society Logo",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = societyInfo?.name ?: AppStrings.get("app_title", currentLanguage),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (currentUser != null) {
                                "${currentUser.flatNumber} • ${getRoleLabel(currentUser.role, currentLanguage)}"
                            } else {
                                AppStrings.get("app_tagline", currentLanguage)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Global Language Switcher Chip / Dropdown (English, Hindi, Marathi)
                    Box {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier
                                .clickable {
                                    if (onSelectLanguage != null) {
                                        showLangMenu = true
                                    } else {
                                        onToggleLanguage()
                                    }
                                }
                                .testTag("language_toggle_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "Language",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when (currentLanguage) {
                                        AppLanguage.ENGLISH -> "ENG"
                                        AppLanguage.HINDI -> "हिन्दी"
                                        AppLanguage.MARATHI -> "मराठी"
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                if (onSelectLanguage != null) {
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        if (onSelectLanguage != null) {
                            DropdownMenu(
                                expanded = showLangMenu,
                                onDismissRequest = { showLangMenu = false }
                            ) {
                                AppLanguage.values().forEach { lang ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${lang.displayName} (${lang.nativeName})",
                                                    fontWeight = if (lang == currentLanguage) FontWeight.Bold else FontWeight.Normal
                                                )
                                                if (lang == currentLanguage) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = TealPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            onSelectLanguage(lang)
                                            showLangMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (currentUser != null) {
                        IconButton(
                            onClick = onSwitchProfile,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("switch_profile_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Switch Profile",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = onLogout,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("logout_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Logout",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getRoleLabel(role: UserRole, language: AppLanguage): String {
    return when (role) {
        UserRole.CHAIRMAN -> AppStrings.get("role_chairman", language)
        UserRole.SECRETARY -> AppStrings.get("role_secretary", language)
        UserRole.TREASURER -> AppStrings.get("role_treasurer", language)
        UserRole.COMMITTEE -> AppStrings.get("role_committee", language)
        UserRole.RESIDENT -> AppStrings.get("role_resident", language)
        UserRole.TENANT -> AppStrings.get("role_tenant", language)
        UserRole.GUARD -> AppStrings.get("role_guard", language)
    }
}

@Composable
fun FinancialKpiCard(
    title: String,
    amount: Double,
    subtitle: String? = null,
    icon: ImageVector,
    accentColor: Color = TealPrimary,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SlateOutlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "₹%,.2f".format(amount),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    actionTestTag: String = "section_action_btn",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionLabel != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                modifier = Modifier.testTag(actionTestTag)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TealPrimary
                )
            }
        }
    }
}

@Composable
fun StatusBadge(
    status: PaymentStatus,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, labelKey) = when (status) {
        PaymentStatus.PAID -> Triple(StatusPaidBg, StatusPaid, "status_paid")
        PaymentStatus.UNPAID -> Triple(StatusOverdueBg, StatusOverdue, "status_unpaid")
        PaymentStatus.PARTIAL -> Triple(StatusPendingBg, StatusPending, "status_partial")
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Text(
            text = AppStrings.get(labelKey, language),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun VisitorStatusBadge(
    status: VisitorStatus,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, labelKey) = when (status) {
        VisitorStatus.INSIDE -> Triple(StatusPaidBg, StatusPaid, "visitor_inside")
        VisitorStatus.EXITED -> Triple(SlateSurfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "visitor_exited")
        VisitorStatus.PRE_APPROVED -> Triple(StatusPendingBg, StatusPending, "generate_passcode")
        VisitorStatus.REJECTED -> Triple(StatusOverdueBg, StatusOverdue, "status_unpaid")
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Text(
            text = AppStrings.get(labelKey, language),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun TicketStatusBadge(
    status: TicketStatus,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, labelKey) = when (status) {
        TicketStatus.OPEN -> Triple(StatusOverdueBg, StatusOverdue, "status_open")
        TicketStatus.IN_PROGRESS -> Triple(StatusPendingBg, StatusPending, "status_in_progress")
        TicketStatus.RESOLVED -> Triple(StatusPaidBg, StatusPaid, "status_resolved")
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Text(
            text = AppStrings.get(labelKey, language),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ReceiptDialog(
    bill: MaintenanceBill,
    society: SocietyInfo?,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${AppStrings.get("receipt_generated", language)} #${bill.receiptNumber}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StatusPaid
                )
            }
        },
        text = {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = society?.name ?: "",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TealPrimary
                    )
                    Text(
                        text = "Reg No: ${society?.registrationNumber ?: "MUM/HSG/2022/984"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    ReceiptRow(label = "Flat / Member", value = "${bill.flatNumber} - ${bill.residentName}")
                    ReceiptRow(label = "Month", value = bill.billMonth)
                    ReceiptRow(label = "Total Bill", value = "₹%,.2f".format(bill.totalAmount))
                    ReceiptRow(label = "Amount Paid", value = "₹%,.2f".format(bill.amountPaid), isBold = true)
                    ReceiptRow(label = "Payment Mode", value = bill.paymentMode?.name ?: "UPI")
                    ReceiptRow(label = "Reference No.", value = if (bill.transactionRef.isNotBlank()) bill.transactionRef else "N/A")
                    ReceiptRow(label = "Paid On", value = if (bill.paidDate.isNotBlank()) bill.paidDate else "Today")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text(AppStrings.get("close", language))
            }
        }
    )
}

@Composable
fun ReceiptRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = if (isBold) MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = StatusPaid) else MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
    }
}
