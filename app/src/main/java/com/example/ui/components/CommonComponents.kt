package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.MaintenanceBill
import com.example.data.entity.PaymentStatus
import com.example.data.entity.Resident
import com.example.data.entity.SocietyInfo
import com.example.data.entity.TicketStatus
import com.example.data.entity.UserRole
import com.example.data.entity.VisitorStatus
import com.example.data.entity.chargesSubtotal
import com.example.data.entity.payableTotal
import com.example.data.entity.pendingAmount
import com.example.data.localization.AppLanguage
import com.example.R
import com.example.data.localization.AppStrings
import com.example.util.ReceiptPdf
import com.example.util.rememberBlobImage
import com.example.ui.theme.SlateOutlineVariant
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusOverdueBg
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.TealPrimary

/**
 * Top bar for every signed-in screen.
 *
 * Three tap targets, and nothing else: the society logo opens the Society
 * Profile, the language globe switches language, and the member's photo opens
 * their own profile (which is where Logout now lives).
 */
@Composable
fun SocietyTopAppBar(
    societyInfo: SocietyInfo?,
    currentUser: Resident?,
    currentLanguage: AppLanguage,
    onToggleLanguage: () -> Unit,
    onSelectLanguage: ((AppLanguage) -> Unit)? = null,
    onOpenSocietyProfile: () -> Unit = {},
    onOpenUserProfile: () -> Unit = {},
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
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenSocietyProfile() }
                        .testTag("society_logo_btn")
                ) {
                    SocietyLogoMark(logoBase64 = societyInfo?.logoBase64, size = 40)
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
                    // Language switcher - icon only, no label.
                    Box {
                        IconButton(
                            onClick = {
                                if (onSelectLanguage != null) showLangMenu = true else onToggleLanguage()
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("language_toggle_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = AppStrings.get("switch_language", currentLanguage),
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        if (onSelectLanguage != null) {
                            DropdownMenu(
                                expanded = showLangMenu,
                                onDismissRequest = { showLangMenu = false }
                            ) {
                                AppLanguage.entries.forEach { lang ->
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
                        Spacer(modifier = Modifier.width(4.dp))
                        MemberAvatar(
                            photoBase64 = currentUser.photoBase64,
                            name = currentUser.ownerName,
                            size = 38,
                            modifier = Modifier
                                .clickable { onOpenUserProfile() }
                                .testTag("user_profile_btn")
                        )
                    }
                }
            }
        }
    }
}

/** The society's uploaded logo, falling back to the SocietySarv mark. */
@Composable
fun SocietyLogoMark(logoBase64: String?, size: Int) {
    val logo = rememberBlobImage(logoBase64)
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 4).dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (logo != null) {
            Image(
                bitmap = logo,
                contentDescription = "Society logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size.dp)
            )
        } else {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "SocietySarv logo",
                modifier = Modifier.size(size.dp)
            )
        }
    }
}

/** Member photo, or their initial when no photo has been uploaded. */
@Composable
fun MemberAvatar(
    photoBase64: String?,
    name: String,
    size: Int,
    modifier: Modifier = Modifier
) {
    val photo = rememberBlobImage(photoBase64)
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center
    ) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size.dp)
            )
        } else {
            Text(
                text = name.trim().firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

fun getRoleLabel(role: UserRole, language: AppLanguage): String = when (role) {
    UserRole.CHAIRMAN -> AppStrings.get("role_chairman", language)
    UserRole.SECRETARY -> AppStrings.get("role_secretary", language)
    UserRole.TREASURER -> AppStrings.get("role_treasurer", language)
    UserRole.COMMITTEE -> AppStrings.get("role_committee", language)
    UserRole.RESIDENT -> AppStrings.get("role_resident", language)
    UserRole.TENANT -> AppStrings.get("role_tenant", language)
    UserRole.GUARD -> AppStrings.get("role_guard", language)
    UserRole.HOUSEKEEPING -> AppStrings.get("role_housekeeping", language)
    UserRole.MAINTENANCE_STAFF -> AppStrings.get("role_maintenance_staff", language)
    UserRole.ACCOUNTANT -> AppStrings.get("role_accountant", language)
    UserRole.MANAGER -> AppStrings.get("role_manager", language)
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
        // A turned-away visitor is a gate outcome, not a payment one. This
        // used to borrow the billing string and read as "Unpaid".
        VisitorStatus.REJECTED -> Triple(StatusOverdueBg, StatusOverdue, "visitor_rejected")
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

/**
 * On-screen preview of the payment receipt, with the two things a committee
 * actually needs from it: a proper PDF saved to the device, and a share sheet
 * to send it on to the member.
 */
@Composable
fun ReceiptDialog(
    bill: MaintenanceBill,
    society: SocietyInfo?,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

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
                Text(
                    text = "${AppStrings.get("receipt_generated", language)} #${bill.receiptNumber}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StatusPaid
                )
            }
        },
        text = {
            Column {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SocietyLogoMark(logoBase64 = society?.logoBase64, size = 36)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = society?.name.orEmpty(),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TealPrimary
                                )
                                Text(
                                    text = "Reg No: ${society?.registrationNumber.orEmpty().ifBlank { "-" }}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        ReceiptRow(label = "Unit / Member", value = "${bill.flatNumber} - ${bill.residentName}")
                        ReceiptRow(label = "Month", value = bill.billMonth)
                        ReceiptRow(label = "Bill Amount", value = "₹%,.2f".format(bill.chargesSubtotal))
                        ReceiptRow(label = "Late Fee", value = "₹%,.2f".format(bill.lateFeeAmount))
                        ReceiptRow(label = "Total Payable", value = "₹%,.2f".format(bill.payableTotal))
                        ReceiptRow(label = "Amount Paid", value = "₹%,.2f".format(bill.amountPaid), isBold = true)
                        ReceiptRow(label = "Balance Outstanding", value = "₹%,.2f".format(bill.pendingAmount))
                        ReceiptRow(label = "Payment Mode", value = bill.paymentMode?.labelEn ?: "-")
                        ReceiptRow(label = "Reference No.", value = bill.transactionRef.ifBlank { "-" })
                        ReceiptRow(label = "Paid On", value = bill.paidDate.ifBlank { "-" })
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val file = ReceiptPdf.generate(context, bill, society)
                            isError = file == null
                            status = if (file == null) {
                                "Could not create the receipt PDF."
                            } else {
                                "Saved as ${file.name}"
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, TealPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("receipt_pdf_btn")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = TealPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(AppStrings.get("download_receipt", language), color = TealPrimary)
                    }
                    Button(
                        onClick = {
                            val failure = ReceiptPdf.share(context, bill, society)
                            isError = failure != null
                            status = failure
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("receipt_share_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(AppStrings.get("share_receipt", language))
                    }
                }

                status?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) MaterialTheme.colorScheme.error else StatusPaid
                    )
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

/**
 * How wide a dialog sits on the screen.
 *
 * Material's default leaves a lot of margin, which cramps the forms in this
 * app - month-wise tables, the bill sheet, the registration steps. Every dialog
 * pairs this with [WideDialogProperties] so they are all the same width.
 */
val DialogWidth: Modifier = Modifier.fillMaxWidth(0.96f)

/** Lets a dialog take [DialogWidth] instead of the platform's own width. */
val WideDialogProperties: DialogProperties =
    DialogProperties(usePlatformDefaultWidth = false)

/** Far more rows than any real unit, month or city list will ever hold. */
private const val MAX_DROPDOWN_OPTIONS = 1000

/**
 * Dropdown that still lets the user type a value that is not on the list -
 * there is no list of Indian cities that is ever complete enough to block a
 * registration on.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableDropdownField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    allowFreeText: Boolean = true,
    testTag: String = ""
) {
    var expanded by remember { mutableStateOf(false) }

    /*
     * What the list is narrowed to.
     *
     * The typed value is a search term only while the field is a free-text one
     * *and* what is in it is not already one of the options. A pick-only field
     * (unit number, billing month, unit type) treats its value as a selection,
     * so choosing "August 2026" or "2 BHK" must never leave that the only row
     * the list will show afterwards.
     */
    val matches = remember(value, options, allowFreeText) {
        val typed = value.trim()
        val isSelection = options.any { it.equals(typed, ignoreCase = true) }
        if (!allowFreeText || typed.isBlank() || isSelection) {
            options
        } else {
            options.filter { it.contains(typed, ignoreCase = true) }
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded && matches.isNotEmpty(),
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { typed -> if (allowFreeText) { onValueChange(typed); expanded = true } },
            readOnly = !allowFreeText,
            label = { Text(label) },
            supportingText = if (allowFreeText && options.isEmpty()) {
                { Text("Type it in - no list available for this selection.") }
            } else null,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .then(if (testTag.isBlank()) Modifier else Modifier.testTag(testTag))
        )
        ExposedDropdownMenu(
            expanded = expanded && matches.isNotEmpty(),
            onDismissRequest = { expanded = false }
        ) {
            // Every option is listed - a society with 200 units has to be able
            // to reach its last unit - and the menu scrolls when it has to.
            matches.take(MAX_DROPDOWN_OPTIONS).forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Picks a month and a year and hands back one "April 2026" string - the format
 * every bill month in the app is written in.
 *
 * Used for the society's billing start month, which is what every billing month
 * list in the app is generated from.
 */
@Composable
fun MonthYearPickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    supportingText: String? = null,
    yearsBack: Int = 10,
    yearsForward: Int = 1,
    testTagPrefix: String = "month_year"
) {
    val currentYear = remember {
        java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    }
    val parts = remember(value) { value.trim().split(" ").filter { it.isNotBlank() } }
    val month = parts.getOrNull(0).orEmpty()
    val year = parts.getOrNull(1).orEmpty()
    val years = remember(currentYear) {
        ((currentYear - yearsBack)..(currentYear + yearsForward)).map { it.toString() }.reversed()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1.3f)) {
                SearchableDropdownField(
                    label = "Month",
                    value = month,
                    options = MONTH_NAMES,
                    onValueChange = { picked ->
                        onValueChange("$picked ${year.ifBlank { currentYear.toString() }}")
                    },
                    allowFreeText = false,
                    testTag = "${testTagPrefix}_month_dropdown"
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                SearchableDropdownField(
                    label = "Year",
                    value = year,
                    options = years,
                    onValueChange = { picked ->
                        onValueChange("${month.ifBlank { MONTH_NAMES.first() }} $picked")
                    },
                    allowFreeText = false,
                    testTag = "${testTagPrefix}_year_dropdown"
                )
            }
        }
        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/** English month names, the same spelling every bill month is stored with. */
val MONTH_NAMES: List<String> = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)
