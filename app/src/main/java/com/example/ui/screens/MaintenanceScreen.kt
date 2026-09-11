package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.util.ImageBlob
import com.example.util.rememberBlobImage
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.FlatType
import com.example.data.entity.MaintenanceBill
import com.example.data.entity.PaymentMode
import com.example.data.entity.PaymentStatus
import com.example.data.entity.Resident
import com.example.data.entity.SocietyInfo
import com.example.data.entity.UserRole
import com.example.data.localization.AppLanguage
import com.example.ui.components.DialogWidth
import com.example.ui.components.WideDialogProperties
import com.example.data.localization.AppStrings
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.StatusBadge
import com.example.ui.theme.MintSecondary
import com.example.ui.theme.SlateOutlineVariant
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusOverdueBg
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryContainer
import com.example.viewmodel.SocietyViewModel
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.style.TextAlign
import com.example.data.entity.chargesSubtotal
import com.example.data.entity.UnitTypeConfig
import com.example.data.entity.billingMonths
import com.example.data.entity.monthlyTariffFor
import com.example.data.entity.hasBalancedMaintenancePercents
import com.example.data.entity.splitMaintenance
import com.example.data.entity.unitTypeNames
import com.example.data.entity.derivedStatus
import com.example.data.entity.isStaff
import com.example.data.entity.payableTotal
import com.example.data.entity.pendingAmount
import com.example.ui.components.SearchableDropdownField
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun MaintenanceScreen(
    viewModel: SocietyViewModel,
    society: SocietyInfo?,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val currentUser by viewModel.currentUser.collectAsState()
    val canEditCharges = currentUser?.role == UserRole.CHAIRMAN ||
        currentUser?.role == UserRole.TREASURER
    val subTabs = listOf(
        AppStrings.get("nav_maintenance", language),
        AppStrings.get("receipt_list", language),
        AppStrings.get("tariff_configuration", language),
        AppStrings.get("charge_setup", language),
        AppStrings.get("late_fee_rules", language),
        AppStrings.get("member_directory", language)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = TealPrimary,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                    color = TealPrimary
                )
            }
        ) {
            subTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSubTab == index,
                    onClick = { selectedSubTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selectedSubTab == index) FontWeight.Bold else FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    },
                    modifier = Modifier.testTag("maint_subtab_$index")
                )
            }
        }

        when (selectedSubTab) {
            0 -> MaintenanceBillingView(viewModel = viewModel, society = society, language = language)
            1 -> ReceiptListView(
                viewModel = viewModel,
                society = society,
                language = language,
                canEdit = canEditCharges
            )
            2 -> TariffConfigurationView(viewModel = viewModel, society = society, language = language)
            3 -> MaintenanceChargeSetupView(
                viewModel = viewModel,
                society = society,
                language = language,
                canEdit = canEditCharges
            )
            4 -> LateFeeRulesView(viewModel = viewModel, society = society, language = language)
            5 -> ResidentDirectoryView(viewModel = viewModel, language = language)
        }
    }
}

// 1. Maintenance Billing & Payment Logger View
@Composable
fun MaintenanceBillingView(
    viewModel: SocietyViewModel,
    society: SocietyInfo?,
    language: AppLanguage
) {
    val bills by viewModel.bills.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val canEdit = currentUser?.role == UserRole.CHAIRMAN || currentUser?.role == UserRole.TREASURER
    var selectedBillForPayment by remember { mutableStateOf<MaintenanceBill?>(null) }
    var selectedBillForReceipt by remember { mutableStateOf<MaintenanceBill?>(null) }
    var selectedBillForEdit by remember { mutableStateOf<MaintenanceBill?>(null) }
    var billPendingDeletion by remember { mutableStateOf<MaintenanceBill?>(null) }
    var isGenerateBillOpen by remember { mutableStateOf(false) }
    // Bills open on what still needs collecting.
    var statusFilter by remember { mutableStateOf(PaymentStatus.UNPAID) }

    selectedBillForEdit?.let { bill ->
        EditBillDialog(
            bill = bill,
            society = society,
            language = language,
            onDismiss = { selectedBillForEdit = null },
            onSave = { month, base, water, sinking, repair, lateFee, dueDate, onError ->
                viewModel.updateBill(bill, month, base, water, sinking, repair, lateFee, dueDate) { ok, message ->
                    if (ok) {
                        viewModel.showMessage(message)
                        selectedBillForEdit = null
                    } else {
                        onError(message)
                    }
                }
            }
        )
    }

    billPendingDeletion?.let { bill ->
        AlertDialog(
            modifier = DialogWidth,
            properties = WideDialogProperties,
            onDismissRequest = { billPendingDeletion = null },
            title = {
                Text(
                    text = if (language == AppLanguage.MARATHI) "बिल हटवायचे?" else "Delete this bill?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "${bill.flatNumber} • ${bill.billMonth} • ₹%,.2f".format(bill.payableTotal) +
                        if (bill.amountPaid > 0) {
                            "\n\n₹%,.2f has already been collected against this bill.".format(bill.amountPaid)
                        } else {
                            ""
                        },
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBill(bill) { _, message -> viewModel.showMessage(message) }
                        billPendingDeletion = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusOverdue),
                    modifier = Modifier.testTag("confirm_delete_bill_btn")
                ) {
                    Text(if (language == AppLanguage.MARATHI) "हटवा" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { billPendingDeletion = null }) {
                    Text(AppStrings.get("cancel", language))
                }
            }
        )
    }

    if (selectedBillForReceipt != null) {
        ReceiptDialog(
            bill = selectedBillForReceipt!!,
            society = society,
            language = language,
            onDismiss = { selectedBillForReceipt = null }
        )
    }

    if (selectedBillForPayment != null) {
        RecordPaymentDialog(
            bill = selectedBillForPayment!!,
            language = language,
            onDismiss = { selectedBillForPayment = null },
            onSubmit = { amount, mode, ref, date, screenshot ->
                viewModel.recordMaintenancePayment(
                    selectedBillForPayment!!, amount, mode, ref, date, screenshot
                )
                selectedBillForPayment = null
            }
        )
    }

    if (isGenerateBillOpen) {
        GenerateBillDialog(
            viewModel = viewModel,
            society = society,
            language = language,
            onDismiss = { isGenerateBillOpen = false }
        )
    }

    val filteredBills = bills.filter { it.derivedStatus == statusFilter }

    /*
     * How much catching up there is: every billable unit crossed with every
     * month from the society's billing start that it has no bill for. Recomputed
     * whenever the bills or the members move, so it disappears once the run is
     * done.
     */
    val residents by viewModel.residents.collectAsState()
    val outstandingRun = remember(bills, residents, society) { viewModel.pendingBillRun() }
    val isReadyToBill = society.hasBalancedMaintenancePercents &&
        society?.billingStartMonth?.isNotBlank() == true
    var isBulkConfirmOpen by remember { mutableStateOf(false) }

    if (isBulkConfirmOpen) {
        val months = outstandingRun.map { it.month }.distinct().size
        val units = outstandingRun.map { it.unit.flatNumber }.distinct().size
        AlertDialog(
            modifier = DialogWidth,
            properties = WideDialogProperties,
            onDismissRequest = { isBulkConfirmOpen = false },
            title = {
                Text(
                    text = if (language == AppLanguage.MARATHI) {
                        "सर्व युनिट्सची बिले तयार करायची?"
                    } else {
                        "Generate bills for all units?"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "${outstandingRun.size} bill(s) will be raised - $units unit(s) " +
                        "across $months month(s) that have no bill yet.\n\n" +
                        "Each one uses the unit's own tariff, split by the society's " +
                        "charge setup. Bills that already exist are left alone.",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isBulkConfirmOpen = false
                        viewModel.generateMissingBills { _, message -> viewModel.showMessage(message) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.testTag("confirm_bulk_bills_btn")
                ) {
                    Text(if (language == AppLanguage.MARATHI) "तयार करा" else "Generate")
                }
            },
            dismissButton = {
                TextButton(onClick = { isBulkConfirmOpen = false }) {
                    Text(AppStrings.get("cancel", language))
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Actions & Filters Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = statusFilter == PaymentStatus.UNPAID,
                        onClick = { statusFilter = PaymentStatus.UNPAID },
                        label = { Text(AppStrings.get("status_unpaid", language)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = StatusOverdueBg),
                        modifier = Modifier.testTag("bills_filter_unpaid")
                    )
                    FilterChip(
                        selected = statusFilter == PaymentStatus.PARTIAL,
                        onClick = { statusFilter = PaymentStatus.PARTIAL },
                        label = { Text(AppStrings.get("status_partial", language)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = StatusPendingBg),
                        modifier = Modifier.testTag("bills_filter_partial")
                    )
                    FilterChip(
                        selected = statusFilter == PaymentStatus.PAID,
                        onClick = { statusFilter = PaymentStatus.PAID },
                        label = { Text(AppStrings.get("status_paid", language)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = StatusPaidBg),
                        modifier = Modifier.testTag("bills_filter_paid")
                    )
                }

                Button(
                    onClick = { isGenerateBillOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("generate_bill_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (language == AppLanguage.MARATHI) "बिल तयार करा" else "New Bill", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Catch-up banner: only worth showing once the society can actually
        // bill, and only while something is still unbilled.
        if (canEdit && isReadyToBill && outstandingRun.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = TealPrimaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bulk_bill_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (language == AppLanguage.MARATHI) {
                                "${outstandingRun.size} बिले अजून तयार व्हायची आहेत"
                            } else {
                                "${outstandingRun.size} bill(s) still to be raised"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TealPrimary
                        )
                        Text(
                            text = "${outstandingRun.map { it.unit.flatNumber }.distinct().size} unit(s) • " +
                                "${outstandingRun.map { it.month }.distinct().size} month(s) pending",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { isBulkConfirmOpen = true },
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bulk_generate_bills_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == AppLanguage.MARATHI) {
                                    "सर्व युनिट्सची बिले तयार करा"
                                } else {
                                    "Generate Bills for All Units"
                                },
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }

        items(filteredBills) { bill ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SlateOutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${bill.flatNumber} • ${bill.residentName}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${bill.billMonth} • Due: ${bill.dueDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "₹%,.2f".format(bill.payableTotal),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            StatusBadge(status = bill.derivedStatus, language = language)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Bill ₹%,.0f + Late ₹%,.0f".format(bill.chargesSubtotal, bill.lateFeeAmount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Paid ₹%,.2f • Pending ₹%,.2f".format(bill.amountPaid, bill.pendingAmount),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (bill.pendingAmount <= 0.009) StatusPaid else StatusOverdue
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // An existing bill can be corrected - a wrong tariff,
                            // a missed water charge - without deleting it and
                            // starting again. Payments already taken stay put.
                            if (canEdit) {
                                IconButton(
                                    onClick = { selectedBillForEdit = bill },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("edit_bill_btn_${bill.flatNumber}")
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit bill",
                                        tint = TealPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                // A settled bill is a receipted record. Undoing
                                // it goes through the receipt, not by deleting
                                // the bill out from under the payment.
                                if (bill.derivedStatus != PaymentStatus.PAID) {
                                    IconButton(
                                        onClick = { billPendingDeletion = bill },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("delete_bill_btn_${bill.flatNumber}")
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete bill",
                                            tint = StatusOverdue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            if (bill.derivedStatus != PaymentStatus.PAID) {
                                // Compact: it sits beside the edit and delete
                                // icons, so it takes only the room it needs.
                                Button(
                                    onClick = { selectedBillForPayment = bill },
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .heightIn(min = 32.dp)
                                        .testTag("collect_payment_btn_${bill.flatNumber}")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(AppStrings.get("pay", language), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            if (bill.amountPaid > 0.0) {
                                OutlinedButton(
                                    onClick = { selectedBillForReceipt = bill },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, TealPrimary),
                                    modifier = Modifier.testTag("view_receipt_btn_${bill.flatNumber}")
                                ) {
                                    Icon(Icons.Default.Receipt, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (language == AppLanguage.MARATHI) "पावती पहा" else "Receipt", style = MaterialTheme.typography.labelSmall, color = TealPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Every receipt the society has issued.
 *
 * A receipt lives on the bill it settled, so this is the bills that carry a
 * payment - receipt number, unit, member, date, amount, mode and the proof that
 * was attached. Correcting one restates that payment; deleting one puts the
 * bill back to unpaid and the money back on the member's dues.
 */
@Composable
fun ReceiptListView(
    viewModel: SocietyViewModel,
    society: SocietyInfo?,
    language: AppLanguage,
    canEdit: Boolean
) {
    val bills by viewModel.bills.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var receiptBeingEdited by remember { mutableStateOf<MaintenanceBill?>(null) }
    var receiptPendingDeletion by remember { mutableStateOf<MaintenanceBill?>(null) }
    var receiptToView by remember { mutableStateOf<MaintenanceBill?>(null) }

    val receipts = remember(bills, searchQuery) {
        bills.filter { it.amountPaid > 0.0 }
            .filter { receipt ->
                searchQuery.isBlank() ||
                    receipt.receiptNumber.contains(searchQuery, ignoreCase = true) ||
                    receipt.flatNumber.contains(searchQuery, ignoreCase = true) ||
                    receipt.residentName.contains(searchQuery, ignoreCase = true)
            }
            .sortedByDescending { monthSortKey(it.billMonth) }
    }

    receiptToView?.let { receipt ->
        ReceiptDialog(
            bill = receipt,
            society = society,
            language = language,
            onDismiss = { receiptToView = null }
        )
    }

    receiptBeingEdited?.let { receipt ->
        RecordPaymentDialog(
            bill = receipt,
            language = language,
            isEditingReceipt = true,
            onDismiss = { receiptBeingEdited = null },
            onSubmit = { amount, mode, ref, date, screenshot ->
                viewModel.updateRecordedPayment(receipt, amount, mode, ref, date, screenshot) { ok, message ->
                    viewModel.showMessage(message)
                    if (ok) receiptBeingEdited = null
                }
            }
        )
    }

    receiptPendingDeletion?.let { receipt ->
        AlertDialog(
            modifier = DialogWidth,
            properties = WideDialogProperties,
            onDismissRequest = { receiptPendingDeletion = null },
            title = {
                Text(
                    text = if (language == AppLanguage.MARATHI) "पावती रद्द करायची?" else "Delete this receipt?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "${receipt.receiptNumber} • ${receipt.flatNumber} • ₹%,.2f\n\n".format(receipt.amountPaid) +
                        "The payment is reversed: the bill for ${receipt.billMonth} goes back to " +
                        "unpaid and ₹%,.2f returns to the member's dues.".format(receipt.amountPaid),
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteReceipt(receipt) { _, message -> viewModel.showMessage(message) }
                        receiptPendingDeletion = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusOverdue),
                    modifier = Modifier.testTag("confirm_delete_receipt_btn")
                ) {
                    Text(if (language == AppLanguage.MARATHI) "रद्द करा" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { receiptPendingDeletion = null }) {
                    Text(AppStrings.get("cancel", language))
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by receipt no., unit or member") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("receipt_search_input")
            )
        }

        item {
            Text(
                text = "${receipts.size} receipt(s) • ₹%,.2f collected".format(
                    receipts.sumOf { it.amountPaid }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (receipts.isEmpty()) {
            item {
                Text(
                    text = if (language == AppLanguage.MARATHI) {
                        "अजून एकही पावती तयार झालेली नाही."
                    } else {
                        "No receipts have been issued yet."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(receipts) { receipt ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SlateOutlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("receipt_card_${receipt.receiptNumber}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = receipt.receiptNumber.ifBlank { "(no receipt number)" },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${receipt.flatNumber} • ${receipt.residentName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "₹%,.2f".format(receipt.amountPaid),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = StatusPaid
                                )
                            )
                            StatusBadge(status = receipt.derivedStatus, language = language)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    ReceiptField("Bill", "${receipt.billMonth} • ₹%,.2f".format(receipt.payableTotal))
                    ReceiptField(
                        AppStrings.get("payment_date", language),
                        receipt.paidDate.ifBlank { "-" }
                    )
                    ReceiptField(
                        AppStrings.get("payment_mode", language),
                        receipt.paymentMode?.let {
                            if (language == AppLanguage.MARATHI) it.labelMr else it.labelEn
                        } ?: "-"
                    )
                    ReceiptField("Reference", receipt.transactionRef.ifBlank { "-" })
                    ReceiptField(
                        "Proof",
                        if (receipt.receiptImageBase64.isBlank()) "Not attached" else "Screenshot attached"
                    )

                    // The proof itself, so it can be checked without opening
                    // anything - it is the whole point of having asked for it.
                    val proof = rememberBlobImage(receipt.receiptImageBase64)
                    if (proof != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(
                            bitmap = proof,
                            contentDescription = "Payment screenshot",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .testTag("receipt_proof_${receipt.receiptNumber}")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { receiptToView = receipt }) {
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View", color = TealPrimary, style = MaterialTheme.typography.labelMedium)
                        }
                        if (canEdit) {
                            TextButton(
                                onClick = { receiptBeingEdited = receipt },
                                modifier = Modifier.testTag("edit_receipt_btn_${receipt.receiptNumber}")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    AppStrings.get("edit_profile", language),
                                    color = TealPrimary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            TextButton(
                                onClick = { receiptPendingDeletion = receipt },
                                modifier = Modifier.testTag("delete_receipt_btn_${receipt.receiptNumber}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = StatusOverdue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (language == AppLanguage.MARATHI) "रद्द" else "Delete",
                                    color = StatusOverdue,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One labelled line on a receipt card. */
@Composable
private fun ReceiptField(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1
        )
    }
}

// 2. Unit Configuration & Tariff Plans View
/**
 * Tariff Configuration.
 *
 * Shows the society wing by wing: what each wing actually holds - its floors,
 * its units and the unit type they were created as - and the monthly tariff
 * charged for that type. Only unit types a wing actually uses are listed, so a
 * society of 1 BHKs is never asked to price a penthouse.
 */
@Composable
fun TariffConfigurationView(
    viewModel: SocietyViewModel,
    society: SocietyInfo?,
    language: AppLanguage
) {
    val residents by viewModel.members.collectAsState()

    // Unit types this society genuinely has: whatever its wings were built with,
    // plus anything else configured against a tariff.
    val liveTypes = remember(society) {
        val fromWings = society?.wingPlans.orEmpty()
            .flatMap { plan -> plan.unitCounts.map { it.unitType } }
            .filter { it.isNotBlank() }
        val configured = society?.unitTypes.orEmpty()
        val names = (fromWings + configured.map { it.name }).filter { it.isNotBlank() }.distinct()
        names.map { name ->
            configured.firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?: UnitTypeConfig(name, 0.0)
        }
    }

    // Edited amounts, keyed by unit type name.
    var tariffs by remember(liveTypes) {
        mutableStateOf(
            liveTypes.associate { it.name to (it.monthlyTariff.takeIf { amt -> amt > 0 }?.toInt()?.toString() ?: "") }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = AppStrings.get("tariff_configuration", language),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (language == AppLanguage.MARATHI) {
                        "विंगनिहाय सदनिका व त्यांचे मासिक देखभाल दर."
                    } else {
                        "Units wing by wing, with the monthly maintenance charged for each type."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // One card per wing: what it holds and what its units are charged.
        items(society?.wingPlans.orEmpty()) { plan ->
            val wingUnits = residents.filter { it.wing.equals(plan.name, ignoreCase = true) }
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SlateOutlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tariff_wing_card_${plan.name}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Wing ${plan.name}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${plan.totalFloors} floors • " +
                                    plan.unitCounts.joinToString(", ") { "${it.unitType} × ${it.quantity}" }
                                        .ifBlank { "no unit types set" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = TealPrimaryContainer) {
                            Text(
                                text = "${plan.totalUnits} units",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TealPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (wingUnits.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${wingUnits.count { it.ownerName.isNotBlank() }} occupied • " +
                                "${wingUnits.count { it.ownerName.isBlank() }} vacant",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // One tariff per unit type this wing actually holds.
                    plan.unitCounts.filter { it.unitType.isNotBlank() }.forEach { count ->
                        OutlinedTextField(
                            value = tariffs[count.unitType].orEmpty(),
                            onValueChange = { raw ->
                                tariffs = tariffs + (count.unitType to raw.filter { it.isDigit() })
                            },
                            label = { Text("${count.unitType} (${count.quantity} units) - ₹ / month") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .testTag("tariff_input_${plan.name}_${count.unitType}")
                        )
                    }
                }
            }
        }

        // Any unit type the society charges for that no wing was built with.
        val extraTypes = liveTypes.filter { type ->
            society?.wingPlans.orEmpty().none { plan ->
                plan.unitCounts.any { it.unitType.equals(type.name, ignoreCase = true) }
            }
        }
        if (extraTypes.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SlateOutlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (language == AppLanguage.MARATHI) "इतर सदनिका प्रकार" else "Other Unit Types",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        extraTypes.forEach { type ->
                            OutlinedTextField(
                                value = tariffs[type.name].orEmpty(),
                                onValueChange = { raw ->
                                    tariffs = tariffs + (type.name to raw.filter { it.isDigit() })
                                },
                                label = { Text("${type.name} - Monthly Tariff (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .testTag("tariff_input_type_${type.name}")
                            )
                        }
                    }
                }
            }
        }

        if (society?.wingPlans.isNullOrEmpty() && liveTypes.isEmpty()) {
            item {
                Text(
                    text = "No wings are configured yet. Open the Society Profile and add the " +
                        "floor inventory to set tariffs wing by wing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Button(
                onClick = {
                    viewModel.updateUnitTypeTariffs(
                        liveTypes.map { type ->
                            type.copy(monthlyTariff = tariffs[type.name]?.toDoubleOrNull() ?: type.monthlyTariff)
                        }
                    ) { _, message -> viewModel.showMessage(message) }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_tariffs_btn")
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(AppStrings.get("save", language))
            }
        }
    }
}

/**
 * Maintenance Charge Configuration.
 *
 * A month's maintenance is divided between the society's funds by percentage
 * rather than by charges added on top of it, so the four shares have to add up
 * to 100%. The preview below shows what those shares come to for a given
 * maintenance amount, and saving is refused until they balance.
 */
@Composable
fun MaintenanceChargeSetupView(
    viewModel: SocietyViewModel,
    society: SocietyInfo?,
    language: AppLanguage,
    canEdit: Boolean
) {
    var water by remember(society) { mutableStateOf(percentText(society?.waterChargesPercent)) }
    var sinking by remember(society) { mutableStateOf(percentText(society?.sinkingFundPercent)) }
    var repair by remember(society) { mutableStateOf(percentText(society?.repairFundPercent)) }
    var other by remember(society) { mutableStateOf(percentText(society?.otherMaintenancePercent)) }

    // What the preview is worked out against: the society's own tariff when it
    // has one, so the numbers on screen are the ones it actually bills.
    var previewAmount by remember(society) {
        mutableStateOf(
            society?.unitTypes.orEmpty().firstOrNull { it.monthlyTariff > 0 }
                ?.monthlyTariff?.toInt()?.toString()
                ?: DEFAULT_PREVIEW_AMOUNT
        )
    }
    var error by remember { mutableStateOf<String?>(null) }

    val waterPct = water.toDoubleOrNull() ?: 0.0
    val sinkingPct = sinking.toDoubleOrNull() ?: 0.0
    val repairPct = repair.toDoubleOrNull() ?: 0.0
    val otherPct = other.toDoubleOrNull() ?: 0.0
    val percentTotal = waterPct + sinkingPct + repairPct + otherPct
    val isBalanced = kotlin.math.abs(percentTotal - 100.0) < 0.01

    // The preview follows what is typed, not what is saved, so the amounts move
    // as soon as a percentage changes.
    val previewValue = previewAmount.toDoubleOrNull() ?: 0.0
    val preview = remember(previewValue, waterPct, sinkingPct, repairPct, otherPct) {
        society?.copy(
            waterChargesPercent = waterPct,
            sinkingFundPercent = sinkingPct,
            repairFundPercent = repairPct,
            otherMaintenancePercent = otherPct
        ).splitMaintenance(previewValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, SlateOutlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = AppStrings.get("charge_setup", language),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (language == AppLanguage.MARATHI) {
                        "देखभाल रक्कम विविध निधींमध्ये किती टक्के विभागायची."
                    } else {
                        "What share of the maintenance amount goes to each fund."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                PercentField("Water Charges (%)", water, "charge_water_percent", canEdit) {
                    water = it; error = null
                }
                Spacer(modifier = Modifier.height(10.dp))
                PercentField("Sinking Fund (%)", sinking, "charge_sinking_percent", canEdit) {
                    sinking = it; error = null
                }
                Spacer(modifier = Modifier.height(10.dp))
                PercentField("Repair & Maintenance Fund (%)", repair, "charge_repair_percent", canEdit) {
                    repair = it; error = null
                }
                Spacer(modifier = Modifier.height(10.dp))
                PercentField("Other / Common Maintenance (%)", other, "charge_other_percent", canEdit) {
                    other = it; error = null
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "%.2f%%".format(percentTotal),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isBalanced) StatusPaid else StatusOverdue,
                        modifier = Modifier.testTag("charge_percent_total")
                    )
                }
                if (!isBalanced) {
                    Text(
                        // Every literal percent sign in a format string has to
                        // be doubled, or Formatter reads "%." as a precision.
                        text = "The percentages must add up to 100%%. They currently add up to %.2f%%."
                            .format(percentTotal),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("charge_percent_error")
                    )
                }
            }
        }

        // Live preview.
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("charge_preview_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (language == AppLanguage.MARATHI) "उदाहरण (Preview)" else "Live Preview",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = previewAmount,
                    onValueChange = { raw -> previewAmount = raw.filter { it.isDigit() || it == '.' } },
                    label = { Text("Maintenance Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("charge_preview_amount")
                )
                Spacer(modifier = Modifier.height(10.dp))
                ChargePreviewRow("Water Charges", waterPct, preview.water)
                ChargePreviewRow("Sinking Fund", sinkingPct, preview.sinking)
                ChargePreviewRow("Repair Fund", repairPct, preview.repair)
                ChargePreviewRow(
                    "Other Maintenance",
                    if (previewValue > 0) preview.other / previewValue * 100.0 else otherPct,
                    preview.other
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "₹%,.2f".format(preview.total),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TealPrimary,
                        modifier = Modifier.testTag("charge_preview_total")
                    )
                }
            }
        }

        error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("charge_save_error")
            )
        }

        if (canEdit) {
            Button(
                enabled = isBalanced,
                onClick = {
                    viewModel.updateMaintenanceCharges(waterPct, sinkingPct, repairPct, otherPct) { ok, message ->
                        if (ok) {
                            error = null
                            viewModel.showMessage(message)
                        } else {
                            error = message
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_charge_setup_btn")
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(AppStrings.get("save", language))
            }

            Text(
                text = if (language == AppLanguage.MARATHI) {
                    "हे फक्त यापुढे तयार होणाऱ्या बिलांना लागू होईल. आधीची बिले जशी आहेत तशीच राहतील."
                } else {
                    "This applies to bills raised from now on. Bills already generated are left as they are."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** The amount the preview opens on when the society has no tariff set yet. */
private const val DEFAULT_PREVIEW_AMOUNT = "1100"

private fun percentText(value: Double?): String = when {
    value == null -> ""
    value == value.toInt().toDouble() -> value.toInt().toString()
    else -> "%.2f".format(value)
}

@Composable
private fun PercentField(
    label: String,
    value: String,
    testTag: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw -> onValueChange(raw.filter { it.isDigit() || it == '.' }.take(6)) },
        label = { Text(label) },
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}

/** One share of a bill's maintenance, as configured under Charge Setup. */
@Composable
private fun SplitRow(label: String, percent: Double, amount: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label (%.2f%%)".format(percent),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "₹%,.2f".format(amount),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

/** "Water Charges  10% -> ₹110.00" */
@Composable
private fun ChargePreviewRow(label: String, percent: Double, amount: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label: %.2f%%".format(percent),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "₹%,.2f".format(amount),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

// 3. Late Fee Rules View
@Composable
fun LateFeeRulesView(
    viewModel: SocietyViewModel,
    society: SocietyInfo?,
    language: AppLanguage
) {
    var lateFeePercent by remember { mutableStateOf((society?.lateFeePercentPerMonth ?: 2.0).toString()) }
    var graceDays by remember { mutableStateOf((society?.lateFeeGraceDays ?: 10).toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, SlateOutlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = AppStrings.get("late_fee_rules", language),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (language == AppLanguage.MARATHI) "मुदतीनंतर थकीत बिलावर दंड व व्याजाची आकारणी." else "Configure monthly overdue interest & grace periods.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                OutlinedTextField(
                    value = lateFeePercent,
                    onValueChange = { lateFeePercent = it },
                    label = { Text("Interest Rate per Month (% प्रति महिना व्याज)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = graceDays,
                    onValueChange = { graceDays = it },
                    label = { Text("Grace Period Days (सवलतीचे दिवस)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val pct = lateFeePercent.toDoubleOrNull() ?: 2.0
                        val days = graceDays.toIntOrNull() ?: 10
                        viewModel.updateLateFeeRules(pct, days)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_late_fee_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(AppStrings.get("save", language))
                }
            }
        }
    }
}

// 4. Resident Directory View (read-only here - members are added and edited
// under Residents > Members, which only the Chairman can open)
@Composable
fun ResidentDirectoryView(
    viewModel: SocietyViewModel,
    language: AppLanguage
) {
    val residents by viewModel.members.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = AppStrings.get("member_directory", language),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        items(residents) { resident ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SlateOutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${resident.flatNumber} • ${resident.ownerName}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            if (resident.tenantName.isNotBlank()) {
                                Text(
                                    text = "Tenant: ${resident.tenantName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TealPrimary
                                )
                            }
                            // Wing and unit type - the two things that decide
                            // which tariff this unit is billed.
                            Text(
                                text = listOfNotNull(
                                    resident.wing.takeIf { it.isNotBlank() }?.let { "Wing $it" },
                                    resident.unitType.takeIf { it.isNotBlank() } ?: "Unit type not set"
                                ).joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (resident.unitType.isBlank()) StatusOverdue else TealPrimary
                            )
                            Text(
                                text = "📞 ${resident.contactNumber} • Slot: ${resident.parkingSlot.ifBlank { "N/A" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TealPrimaryContainer
                        ) {
                            Text(
                                text = resident.role.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TealPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Read-only: members are added and edited under
                    // Residents > Members, and are never deleted from here.
                    Text(
                        text = "Family: ${resident.familyMemberCount} • 2W: ${resident.vehicleTwoWheelerCount} • 4W: ${resident.vehicleFourWheelerCount}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// Dialog: Record Payment
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentDialog(
    bill: MaintenanceBill,
    language: AppLanguage,
    /** True when an existing receipt is being corrected rather than a new one taken. */
    isEditingReceipt: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (
        amount: Double,
        mode: PaymentMode,
        ref: String,
        date: String,
        screenshotBase64: String
    ) -> Unit
) {
    var amountStr by remember(bill, isEditingReceipt) {
        mutableStateOf(
            if (isEditingReceipt) {
                bill.amountPaid.toInt().toString()
            } else {
                bill.pendingAmount.toInt().toString()
            }
        )
    }
    var selectedMode by remember(bill) { mutableStateOf(bill.paymentMode ?: PaymentMode.UPI) }
    var refNo by remember(bill) { mutableStateOf(bill.transactionRef) }
    var isModeExpanded by remember { mutableStateOf(false) }
    // The day the money actually moved, which is not always the day it is
    // entered - a cheque banked last week is recorded today.
    var paidDate by remember(bill) {
        mutableStateOf(bill.paidDate.ifBlank { todayLabel() })
    }
    var screenshot by remember(bill) { mutableStateOf(bill.receiptImageBase64) }

    AlertDialog(
        modifier = DialogWidth,
        properties = WideDialogProperties,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${AppStrings.get("record_payment", language)} (${bill.flatNumber})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "Member: ${bill.residentName} (${bill.billMonth})")
                Text(
                    text = "Bill ₹%,.2f  +  Late fee ₹%,.2f  =  Total ₹%,.2f".format(
                        bill.chargesSubtotal, bill.lateFeeAmount, bill.payableTotal
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Already paid ₹%,.2f  •  Pending ₹%,.2f".format(bill.amountPaid, bill.pendingAmount),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = if (bill.pendingAmount > 0) StatusOverdue else StatusPaid
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(AppStrings.get("amount", language)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = isModeExpanded,
                    onExpandedChange = { isModeExpanded = !isModeExpanded }
                ) {
                    OutlinedTextField(
                        value = if (language == AppLanguage.MARATHI) selectedMode.labelMr else selectedMode.labelEn,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(AppStrings.get("payment_mode", language)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isModeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isModeExpanded,
                        onDismissRequest = { isModeExpanded = false }
                    ) {
                        PaymentMode.values().forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(if (language == AppLanguage.MARATHI) mode.labelMr else mode.labelEn) },
                                onClick = {
                                    selectedMode = mode
                                    isModeExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = refNo,
                    onValueChange = { refNo = it },
                    label = { Text(AppStrings.get("transaction_ref", language)) },
                    placeholder = { Text("e.g. UPI/623401992384 or Cheque No.") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = paidDate,
                    onValueChange = { paidDate = it },
                    label = { Text(AppStrings.get("payment_date", language)) },
                    placeholder = { Text("dd MMM yyyy") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_date_input")
                )

                PaymentScreenshotPicker(
                    screenshotBase64 = screenshot,
                    onScreenshot = { screenshot = it },
                    language = language
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: bill.totalAmount
                    onSubmit(amount, selectedMode, refNo, paidDate, screenshot)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.testTag("submit_record_payment_btn")
            ) {
                Text(AppStrings.get("submit", language))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", language))
            }
        }
    )
}

// Dialog: Generate Bill
/**
 * Raises the next month's bill for one unit.
 *
 * The unit is picked from a dropdown, which fills in the resident's name (read
 * only - members are edited in the Resident Directory, not here), and the sheet
 * then shows exactly what that unit already owes: every past bill month by
 * month with its late fee, total and what was paid against it, and the pending
 * amount they add up to.
 */
@Composable
fun GenerateBillDialog(
    viewModel: SocietyViewModel,
    society: SocietyInfo?,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val residents by viewModel.residents.collectAsState()
    val allBills by viewModel.bills.collectAsState()

    // Only units that actually have a member behind them can be billed.
    val billableUnits = remember(residents) {
        residents.filter { it.ownerName.isNotBlank() }.sortedBy { it.flatNumber }
    }
    var selectedFlat by remember(billableUnits) {
        mutableStateOf(billableUnits.firstOrNull()?.flatNumber.orEmpty())
    }
    val resident = remember(selectedFlat, billableUnits) {
        billableUnits.firstOrNull { it.flatNumber.equals(selectedFlat, ignoreCase = true) }
    }

    // Only this unit's bills - never another unit's - so the history below the
    // picker always belongs to the unit that is selected.
    val unitBills = remember(selectedFlat, allBills) {
        allBills.filter { it.flatNumber.equals(selectedFlat, ignoreCase = true) }
    }
    val totalPending = remember(unitBills) { unitBills.sumOf { it.pendingAmount } }
    val unitLabels = remember(billableUnits) {
        billableUnits.associate { it.flatNumber.uppercase() to unitLabel(it) }
    }

    // Billing runs from the society's configured start month up to today.
    val months = remember(society) { society.billingMonths() }
    var month by remember(months) { mutableStateOf(months.firstOrNull().orEmpty()) }
    // The whole month's maintenance. The society's configured percentages
    // divide it up; they are not added on top of it.
    var maintenanceAmount by remember { mutableStateOf("") }
    var lateFee by remember { mutableStateOf("0") }
    var dueDate by remember(month) { mutableStateOf(defaultDueDate(month)) }
    var error by remember { mutableStateOf<String?>(null) }

    // The maintenance for this unit comes from its unit type's tariff, and the
    // committee can still override it for this one bill.
    LaunchedEffect(resident?.flatNumber, society) {
        // Charged against the unit's own type, so two unit types in one wing
        // are billed differently.
        maintenanceAmount = society.monthlyTariffFor(resident).toInt().toString()
    }

    val maintenanceValue = maintenanceAmount.toDoubleOrNull() ?: 0.0
    val split = remember(maintenanceValue, society) { society.splitMaintenance(maintenanceValue) }

    // A unit already carrying dues past its due date attracts the society's
    // late fee. Suggested, not forced - the committee can still override it.
    LaunchedEffect(selectedFlat, totalPending, society) {
        val percent = society?.lateFeePercentPerMonth ?: 0.0
        lateFee = if (totalPending > 0 && percent > 0) {
            (totalPending * percent / 100).toInt().toString()
        } else {
            "0"
        }
    }

    val charges = split.total
    val lateFeeValue = lateFee.toDoubleOrNull() ?: 0.0
    val newBillTotal = charges + lateFeeValue

    AlertDialog(
        modifier = DialogWidth,
        properties = WideDialogProperties,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (language == AppLanguage.MARATHI) "मासिक बिल तयार करा" else "Generate Maintenance Bill",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Listed as "101 (Raju Anbhule)" so the committee can pick by
                // the member's name as easily as by the unit number.
                SearchableDropdownField(
                    label = AppStrings.get("unit_number_label", language),
                    value = unitLabels[selectedFlat.uppercase()].orEmpty().ifBlank { selectedFlat },
                    options = billableUnits.map { unitLabel(it) },
                    onValueChange = { picked ->
                        selectedFlat = flatNumberFromLabel(picked)
                        error = null
                    },
                    allowFreeText = false,
                    testTag = "bill_unit_dropdown"
                )

                // Vacant units cannot be billed - there is nobody to bill - so
                // say how many are being left out rather than silently hiding
                // them from a list that is meant to hold every unit.
                val vacantCount = residents.count { it.ownerName.isBlank() && !it.role.isStaff }
                Text(
                    text = "${billableUnits.size} unit(s) can be billed" +
                        if (vacantCount > 0) " • $vacantCount vacant unit(s) have no member yet" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("bill_unit_count")
                )

                // Filled in from the unit; changed in the Resident Directory.
                OutlinedTextField(
                    value = resident?.ownerName.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Resident Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bill_resident_name")
                )

                // Total Pending across every bill raised for this unit.
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (totalPending > 0) StatusOverdueBg else StatusPaidBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppStrings.get("total_pending", language),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "₹%,.2f".format(totalPending),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (totalPending > 0) StatusOverdue else StatusPaid,
                            modifier = Modifier.testTag("bill_total_pending")
                        )
                    }
                }

                // The last six months for this unit alone.
                if (unitBills.isNotEmpty()) {
                    MonthWiseBillTable(
                        bills = unitBills,
                        language = language,
                        maxMonths = RECENT_BILL_MONTHS,
                        heading = if (language == AppLanguage.MARATHI) {
                            "$selectedFlat - मागील ६ महिन्यांची बिले"
                        } else {
                            "$selectedFlat - Last 6 Months' Bills"
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = if (language == AppLanguage.MARATHI) "नवीन बिल" else "New Bill",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                SearchableDropdownField(
                    label = "Billing Month",
                    value = month,
                    options = months,
                    onValueChange = { month = it },
                    allowFreeText = false,
                    testTag = "bill_month_dropdown"
                )
                AmountField(
                    "Total Maintenance (₹)",
                    maintenanceAmount,
                    "bill_maintenance_input"
                ) { maintenanceAmount = it }

                // Split by the society's configured percentages, shown so the
                // committee can see where the money goes. Change the split
                // under Maintenance > Charge Setup, not here.
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        SplitRow("Water Charges", society?.waterChargesPercent ?: 0.0, split.water)
                        SplitRow("Sinking Fund", society?.sinkingFundPercent ?: 0.0, split.sinking)
                        SplitRow("Repair Fund", society?.repairFundPercent ?: 0.0, split.repair)
                        // Other carries whatever the rounding leaves over, so
                        // its share is read back off the amount rather than
                        // from the config - otherwise a society whose
                        // percentages do not yet add up to 100% would show a
                        // percentage that does not match its own rupees.
                        SplitRow(
                            "Other Maintenance",
                            if (maintenanceValue > 0) split.other / maintenanceValue * 100.0 else 0.0,
                            split.other
                        )
                    }
                }

                AmountField("Late Fee (₹)", lateFee, "bill_late_fee_input") { lateFee = it }
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // What the member will be asked to pay for this month.
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        TotalRow("Bill Amount", charges)
                        TotalRow("Late Fee", lateFeeValue)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        TotalRow("Total Payable", newBillTotal, bold = true, testTag = "bill_total_payable")
                        if (totalPending > 0) {
                            TotalRow("Previous Pending", totalPending)
                            TotalRow(
                                "Grand Outstanding",
                                newBillTotal + totalPending,
                                bold = true,
                                testTag = "bill_grand_outstanding"
                            )
                        }
                    }
                }

                error?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = resident != null,
                onClick = {
                    val member = resident
                    if (member == null) {
                        error = "Pick a unit that has a resident against it."
                        return@Button
                    }
                    if (charges <= 0.0) {
                        error = "The bill amount must be more than zero."
                        return@Button
                    }
                    if (unitBills.any { it.billMonth.equals(month, ignoreCase = true) }) {
                        error = "A bill for $month already exists for ${member.flatNumber}."
                        return@Button
                    }
                    // Other/Common Maintenance is carried in the bill's base
                    // amount; the four shares add back up to the maintenance.
                    viewModel.generateBill(
                        member.flatNumber,
                        member.ownerName,
                        month,
                        split.other,
                        split.water,
                        split.sinking,
                        split.repair,
                        lateFeeValue,
                        dueDate
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.testTag("bill_submit_btn")
            ) {
                Text(AppStrings.get("submit", language))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", language))
            }
        }
    )
}

/**
 * Edit an existing maintenance bill.
 *
 * Everything that was charged can be corrected - the month, each line item, the
 * late fee and the due date. What has already been collected against the bill
 * is shown but never touched here; that is what Record Payment is for.
 */
@Composable
fun EditBillDialog(
    bill: MaintenanceBill,
    society: SocietyInfo?,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (
        month: String,
        base: Double,
        water: Double,
        sinking: Double,
        repair: Double,
        lateFee: Double,
        dueDate: String,
        onError: (String) -> Unit
    ) -> Unit
) {
    val months = remember(society, bill) {
        // The bill's own month is always offered, even for a bill raised before
        // the society's billing start month was set.
        (society.billingMonths() + bill.billMonth).distinct()
    }
    var month by remember(bill) { mutableStateOf(bill.billMonth) }
    var baseAmount by remember(bill) { mutableStateOf(bill.baseAmount.toInt().toString()) }
    var water by remember(bill) { mutableStateOf(bill.waterCharges.toInt().toString()) }
    var sinking by remember(bill) { mutableStateOf(bill.sinkingFund.toInt().toString()) }
    var repair by remember(bill) { mutableStateOf(bill.repairFund.toInt().toString()) }
    var lateFee by remember(bill) { mutableStateOf(bill.lateFeeAmount.toInt().toString()) }
    var dueDate by remember(bill) { mutableStateOf(bill.dueDate) }
    var error by remember { mutableStateOf<String?>(null) }

    val charges = listOf(baseAmount, water, sinking, repair).sumOf { it.toDoubleOrNull() ?: 0.0 }
    val lateFeeValue = lateFee.toDoubleOrNull() ?: 0.0
    val payable = charges + lateFeeValue

    AlertDialog(
        modifier = DialogWidth,
        properties = WideDialogProperties,
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (language == AppLanguage.MARATHI) "बिल संपादित करा" else "Edit Maintenance Bill",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${bill.flatNumber} • ${bill.residentName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TealPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchableDropdownField(
                    label = "Billing Month",
                    value = month,
                    options = months,
                    onValueChange = { month = it; error = null },
                    allowFreeText = false,
                    testTag = "edit_bill_month_dropdown"
                )
                AmountField("Other / Common Maintenance (₹)", baseAmount, "edit_bill_base_input") { baseAmount = it }
                AmountField("Water Charges (₹)", water) { water = it }
                AmountField("Sinking Fund (₹)", sinking) { sinking = it }
                AmountField("Repair Fund (₹)", repair) { repair = it }
                AmountField("Late Fee (₹)", lateFee, "edit_bill_late_fee_input") { lateFee = it }
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        TotalRow("Bill Amount", charges)
                        TotalRow("Late Fee", lateFeeValue)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        TotalRow("Total Payable", payable, bold = true, testTag = "edit_bill_total_payable")
                        TotalRow("Already Paid", bill.amountPaid)
                        TotalRow(
                            "Pending After Edit",
                            (payable - bill.amountPaid).coerceAtLeast(0.0),
                            bold = true,
                            testTag = "edit_bill_pending"
                        )
                    }
                }

                error?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (charges <= 0.0) {
                        error = "The bill amount must be more than zero."
                        return@Button
                    }
                    onSave(
                        month,
                        baseAmount.toDoubleOrNull() ?: 0.0,
                        water.toDoubleOrNull() ?: 0.0,
                        sinking.toDoubleOrNull() ?: 0.0,
                        repair.toDoubleOrNull() ?: 0.0,
                        lateFeeValue,
                        dueDate
                    ) { message -> error = message }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.testTag("edit_bill_save_btn")
            ) {
                Text(AppStrings.get("save", language))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", language))
            }
        }
    )
}

/**
 * Month | Bill Amount | Late Fee | Total | Paid, one row per billed month.
 *
 * [maxMonths] keeps the table to the most recent months - the Generate Bill
 * sheet shows the last six for the selected unit, not its whole history.
 */
@Composable
internal fun MonthWiseBillTable(
    bills: List<MaintenanceBill>,
    language: AppLanguage,
    maxMonths: Int = Int.MAX_VALUE,
    heading: String = if (language == AppLanguage.MARATHI) "महिनानिहाय बिले" else "Month-wise Bills"
) {
    val ordered = remember(bills, maxMonths) {
        bills.sortedBy { monthSortKey(it.billMonth) }.takeLast(maxMonths)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = heading,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TealPrimaryContainer, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            TableCell("Month", weight = 2.2f, bold = true)
            TableCell("Bill", weight = 1.4f, bold = true, end = true)
            TableCell("Late Fee", weight = 1.3f, bold = true, end = true)
            TableCell("Total", weight = 1.4f, bold = true, end = true)
            TableCell("Paid", weight = 1.4f, bold = true, end = true)
        }
        ordered.forEach { bill ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                TableCell(bill.billMonth, weight = 2.2f)
                TableCell("%,.0f".format(bill.chargesSubtotal), weight = 1.4f, end = true)
                TableCell("%,.0f".format(bill.lateFeeAmount), weight = 1.3f, end = true)
                TableCell("%,.0f".format(bill.payableTotal), weight = 1.4f, end = true, bold = true)
                TableCell(
                    "%,.0f".format(bill.amountPaid),
                    weight = 1.4f,
                    end = true,
                    colour = if (bill.pendingAmount <= 0.009) StatusPaid else StatusOverdue
                )
            }
            HorizontalDivider(color = SlateOutlineVariant)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            TableCell("Total", weight = 2.2f, bold = true)
            TableCell("%,.0f".format(ordered.sumOf { it.chargesSubtotal }), weight = 1.4f, bold = true, end = true)
            TableCell("%,.0f".format(ordered.sumOf { it.lateFeeAmount }), weight = 1.3f, bold = true, end = true)
            TableCell("%,.0f".format(ordered.sumOf { it.payableTotal }), weight = 1.4f, bold = true, end = true)
            TableCell("%,.0f".format(ordered.sumOf { it.amountPaid }), weight = 1.4f, bold = true, end = true)
        }
    }
}

@Composable
private fun RowScope.TableCell(
    text: String,
    weight: Float,
    bold: Boolean = false,
    end: Boolean = false,
    colour: Color = Color.Unspecified
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        ),
        color = colour,
        maxLines = 1,
        textAlign = if (end) TextAlign.End else TextAlign.Start,
        modifier = Modifier.weight(weight)
    )
}

@Composable
private fun TotalRow(label: String, amount: Double, bold: Boolean = false, testTag: String = "") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .then(if (testTag.isBlank()) Modifier else Modifier.testTag(testTag)),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
            )
        )
        Text(
            text = "₹%,.2f".format(amount),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}

@Composable
private fun AmountField(
    label: String,
    value: String,
    testTag: String = "",
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw -> onValueChange(raw.filter { it.isDigit() || it == '.' }) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag.isBlank()) Modifier else Modifier.testTag(testTag))
    )
}

/** "101 (Raju Anbhule)", or just the unit number when nobody lives there. */
private fun unitLabel(unit: Resident): String =
    if (unit.ownerName.isBlank()) unit.flatNumber else "${unit.flatNumber} (${unit.ownerName})"

/** The unit number back out of a label - everything before the bracket. */
private fun flatNumberFromLabel(label: String): String =
    label.substringBefore(" (").trim()

/** Today, in the format every paid date on a bill is written in. */
internal fun todayLabel(): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(java.util.Date())

/**
 * The payment proof attached to a receipt.
 *
 * Downscaled through [ImageBlob] before it is stored, the same as every other
 * picture the app keeps, so a full-size screenshot cannot overrun the document.
 */
@Composable
private fun PaymentScreenshotPicker(
    screenshotBase64: String,
    onScreenshot: (String) -> Unit,
    language: AppLanguage
) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    val preview = rememberBlobImage(screenshotBase64)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri).use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream) ?: error("unsupported image")
                ImageBlob.fromBitmap(bitmap)
            }
        }.onSuccess { onScreenshot(it); error = null }
            .onFailure { error = "Could not read that image. Pick a JPG or PNG." }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { picker.launch("image/*") },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, TealPrimary),
                modifier = Modifier.testTag("payment_screenshot_btn")
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (screenshotBase64.isBlank()) {
                        AppStrings.get("upload_screenshot", language)
                    } else {
                        AppStrings.get("replace_screenshot", language)
                    },
                    color = TealPrimary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            if (screenshotBase64.isNotBlank()) {
                TextButton(onClick = { onScreenshot(""); error = null }) {
                    Text("Remove", color = StatusOverdue, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        if (preview != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Image(
                bitmap = preview,
                contentDescription = "Payment screenshot",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .testTag("payment_screenshot_preview")
            )
        }
        error?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

/** How many months of history the Generate Bill sheet shows for a unit. */
internal const val RECENT_BILL_MONTHS = 6

/** The 10th of the billed month, the usual society due date. */
private fun defaultDueDate(billMonth: String): String = runCatching {
    val parsed = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).parse(billMonth) ?: return@runCatching billMonth
    val calendar = Calendar.getInstance().apply {
        time = parsed
        set(Calendar.DAY_OF_MONTH, 10)
    }
    SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(calendar.time)
}.getOrDefault(billMonth)

/** Sorts "August 2026" style months chronologically rather than alphabetically. */
private fun monthSortKey(billMonth: String): Long = runCatching {
    SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).parse(billMonth)?.time ?: 0L
}.getOrDefault(0L)
