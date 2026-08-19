package com.example.ui.screens

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
import androidx.compose.material.icons.filled.QrCode
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
import com.example.data.localization.AppStrings
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.StatusBadge
import com.example.ui.components.UpiQrPaymentDialog
import com.example.ui.theme.MintSecondary
import com.example.ui.theme.SlateOutlineVariant
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusOverdueBg
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPending
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryContainer
import com.example.viewmodel.SocietyViewModel

@Composable
fun MaintenanceScreen(
    viewModel: SocietyViewModel,
    society: SocietyInfo?,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf(
        AppStrings.get("nav_maintenance", language),
        AppStrings.get("tariff_configuration", language),
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
            1 -> TariffConfigurationView(viewModel = viewModel, society = society, language = language)
            2 -> LateFeeRulesView(viewModel = viewModel, society = society, language = language)
            3 -> ResidentDirectoryView(viewModel = viewModel, language = language)
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
    var selectedBillForPayment by remember { mutableStateOf<MaintenanceBill?>(null) }
    var selectedBillForQr by remember { mutableStateOf<MaintenanceBill?>(null) }
    var selectedBillForReceipt by remember { mutableStateOf<MaintenanceBill?>(null) }
    var isGenerateBillOpen by remember { mutableStateOf(false) }
    var statusFilter by remember { mutableStateOf<PaymentStatus?>(null) }

    if (selectedBillForQr != null) {
        UpiQrPaymentDialog(
            bill = selectedBillForQr!!,
            society = society,
            language = language,
            onDismiss = { selectedBillForQr = null },
            onPaymentRecorded = {
                val b = selectedBillForQr!!
                selectedBillForQr = null
                selectedBillForPayment = b
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
            onSubmit = { amount, mode, ref, date ->
                viewModel.recordMaintenancePayment(selectedBillForPayment!!, amount, mode, ref, date)
                selectedBillForPayment = null
            }
        )
    }

    if (isGenerateBillOpen) {
        GenerateBillDialog(
            viewModel = viewModel,
            society = society,
            language = language,
            onDismiss = { isGenerateBillOpen = false },
            onSubmit = { flat, name, month, base, water, sinking, repair, parking, lateFee, due ->
                viewModel.generateBill(flat, name, month, base, water, sinking, repair, parking, lateFee, due)
                isGenerateBillOpen = false
            }
        )
    }

    val filteredBills = if (statusFilter != null) {
        bills.filter { it.status == statusFilter }
    } else {
        bills
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
                        selected = statusFilter == null,
                        onClick = { statusFilter = null },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimaryContainer)
                    )
                    FilterChip(
                        selected = statusFilter == PaymentStatus.UNPAID,
                        onClick = { statusFilter = PaymentStatus.UNPAID },
                        label = { Text(AppStrings.get("status_unpaid", language)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = StatusOverdueBg)
                    )
                    FilterChip(
                        selected = statusFilter == PaymentStatus.PAID,
                        onClick = { statusFilter = PaymentStatus.PAID },
                        label = { Text(AppStrings.get("status_paid", language)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = StatusPaidBg)
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
                                text = "₹%,.2f".format(bill.totalAmount),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            StatusBadge(status = bill.status, language = language)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Paid: ₹%,.2f".format(bill.amountPaid),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (bill.amountPaid >= bill.totalAmount) StatusPaid else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (bill.status != PaymentStatus.PAID) {
                                OutlinedButton(
                                    onClick = { selectedBillForQr = bill },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, TealPrimary),
                                    modifier = Modifier.testTag("upi_qr_btn_${bill.flatNumber}")
                                ) {
                                    Icon(Icons.Default.QrCode, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (language == AppLanguage.MARATHI) "QR कोड" else "QR Pay", style = MaterialTheme.typography.labelSmall, color = TealPrimary)
                                }

                                Button(
                                    onClick = { selectedBillForPayment = bill },
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("collect_payment_btn_${bill.flatNumber}")
                                ) {
                                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(AppStrings.get("record_payment", language), style = MaterialTheme.typography.labelSmall)
                                }
                            } else {
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

// 2. Unit Configuration & Tariff Plans View
@Composable
fun TariffConfigurationView(
    viewModel: SocietyViewModel,
    society: SocietyInfo?,
    language: AppLanguage
) {
    var t1 by remember { mutableStateOf((society?.tariff1BHK ?: 1500.0).toString()) }
    var t2 by remember { mutableStateOf((society?.tariff2BHK ?: 2200.0).toString()) }
    var t3 by remember { mutableStateOf((society?.tariff3BHK ?: 3000.0).toString()) }
    var tShop by remember { mutableStateOf((society?.tariffShop ?: 3500.0).toString()) }

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
                    text = AppStrings.get("tariff_configuration", language),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (language == AppLanguage.MARATHI) "सदनिका प्रकारानुसार मासिक देखभाल दर निश्चित करा." else "Set monthly maintenance slabs per unit type.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                OutlinedTextField(
                    value = t1,
                    onValueChange = { t1 = it },
                    label = { Text("1 BHK Monthly Tariff (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = t2,
                    onValueChange = { t2 = it },
                    label = { Text("2 BHK Monthly Tariff (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = t3,
                    onValueChange = { t3 = it },
                    label = { Text("3 BHK Monthly Tariff (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = tShop,
                    onValueChange = { tShop = it },
                    label = { Text("Commercial Shop Monthly Tariff (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val b1 = t1.toDoubleOrNull() ?: 1500.0
                        val b2 = t2.toDoubleOrNull() ?: 2200.0
                        val b3 = t3.toDoubleOrNull() ?: 3000.0
                        val bShop = tShop.toDoubleOrNull() ?: 3500.0
                        viewModel.updateTariffs(b1, b2, b3, bShop)
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

// 4. Resident Directory View
@Composable
fun ResidentDirectoryView(
    viewModel: SocietyViewModel,
    language: AppLanguage
) {
    val residents by viewModel.residents.collectAsState()
    var isAddResidentOpen by remember { mutableStateOf(false) }

    if (isAddResidentOpen) {
        AddResidentDialog(
            language = language,
            onDismiss = { isAddResidentOpen = false },
            onSubmit = { flat, wing, owner, tenant, phone, email, role, family, twoW, fourW, twoWNo, fourWNo, slot, type, sqft, pin ->
                viewModel.addResident(
                    flat, wing, owner, tenant, phone, email, role, family, twoW, fourW,
                    twoWNo, fourWNo, slot, type, sqft, pin
                ) { _, message -> viewModel.showMessage(message) }
                isAddResidentOpen = false
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.get("member_directory", language),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Button(
                    onClick = { isAddResidentOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_resident_btn")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStrings.get("add_member", language), style = MaterialTheme.typography.labelSmall)
                }
            }
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Family: ${resident.familyMemberCount} • 2W: ${resident.vehicleTwoWheelerCount} • 4W: ${resident.vehicleFourWheelerCount}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        IconButton(
                            onClick = { viewModel.deleteResident(resident) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusOverdue, modifier = Modifier.size(16.dp))
                        }
                    }
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
    onDismiss: () -> Unit,
    onSubmit: (amount: Double, mode: PaymentMode, ref: String, date: String) -> Unit
) {
    var amountStr by remember { mutableStateOf(bill.totalAmount.toString()) }
    var selectedMode by remember { mutableStateOf(PaymentMode.UPI) }
    var refNo by remember { mutableStateOf("") }
    var isModeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: bill.totalAmount
                    onSubmit(amount, selectedMode, refNo, "")
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
@Composable
fun GenerateBillDialog(
    viewModel: SocietyViewModel,
    society: SocietyInfo?,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSubmit: (
        flat: String, name: String, month: String, base: Double, water: Double,
        sinking: Double, repair: Double, parking: Double, lateFee: Double, due: String
    ) -> Unit
) {
    val residents by viewModel.residents.collectAsState()
    var selectedFlat by remember { mutableStateOf(residents.firstOrNull()?.flatNumber ?: "") }
    var residentName by remember { mutableStateOf(residents.firstOrNull()?.ownerName ?: "Member") }
    var month by remember { mutableStateOf("September 2026") }
    var baseAmount by remember { mutableStateOf("2200") }
    var water by remember { mutableStateOf("300") }
    var sinking by remember { mutableStateOf("110") }
    var repair by remember { mutableStateOf("110") }
    var parking by remember { mutableStateOf("200") }
    var lateFee by remember { mutableStateOf("0") }
    var dueDate by remember { mutableStateOf("10 Sep 2026") }

    AlertDialog(
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
                OutlinedTextField(
                    value = selectedFlat,
                    onValueChange = {
                        selectedFlat = it
                        val match = residents.firstOrNull { r -> r.flatNumber.equals(it, ignoreCase = true) }
                        if (match != null) residentName = match.ownerName
                    },
                    label = { Text("Flat Number") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = residentName,
                    onValueChange = { residentName = it },
                    label = { Text("Resident Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = month,
                    onValueChange = { month = it },
                    label = { Text("Billing Month") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = baseAmount,
                    onValueChange = { baseAmount = it },
                    label = { Text("Base Maintenance (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        selectedFlat,
                        residentName,
                        month,
                        baseAmount.toDoubleOrNull() ?: 2200.0,
                        water.toDoubleOrNull() ?: 300.0,
                        sinking.toDoubleOrNull() ?: 110.0,
                        repair.toDoubleOrNull() ?: 110.0,
                        parking.toDoubleOrNull() ?: 200.0,
                        lateFee.toDoubleOrNull() ?: 0.0,
                        dueDate
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
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

// Dialog: Add Resident
@Composable
fun AddResidentDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSubmit: (
        flat: String, wing: String, owner: String, tenant: String, phone: String, email: String,
        role: UserRole, family: Int, twoW: Int, fourW: Int, twoWNo: String, fourWNo: String,
        slot: String, type: FlatType, sqft: Int, pin: String
    ) -> Unit
) {
    var flat by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf("") }
    var tenant by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var slot by remember { mutableStateOf("") }
    var twoWNo by remember { mutableStateOf("") }
    var fourWNo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = AppStrings.get("add_member", language),
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
                OutlinedTextField(
                    value = flat,
                    onValueChange = { flat = it },
                    label = { Text("Flat No. (e.g. B-402)") },
                    modifier = Modifier.fillMaxWidth()
                )
                // The member signs in with their flat number and this PIN.
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(6) },
                    label = { Text("Login PIN (लॉगिन पिन)") },
                    placeholder = { Text("4-digit PIN for this member") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = owner,
                    onValueChange = { owner = it },
                    label = { Text("Owner Full Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tenant,
                    onValueChange = { tenant = it },
                    label = { Text("Tenant Name (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(AppStrings.get("mobile_number", language)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = slot,
                    onValueChange = { slot = it },
                    label = { Text("Allocated Parking Slot (e.g. P-12)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = twoWNo,
                    onValueChange = { twoWNo = it },
                    label = { Text("2-Wheeler Number") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fourWNo,
                    onValueChange = { fourWNo = it },
                    label = { Text("4-Wheeler Number") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (flat.isNotBlank() && owner.isNotBlank() && phone.isNotBlank() && pin.length >= 4) {
                        onSubmit(
                            flat,
                            flat.take(1),
                            owner,
                            tenant,
                            phone,
                            email,
                            UserRole.RESIDENT,
                            3,
                            if (twoWNo.isNotBlank()) 1 else 0,
                            if (fourWNo.isNotBlank()) 1 else 0,
                            twoWNo,
                            fourWNo,
                            slot,
                            FlatType.TWO_BHK,
                            950,
                            pin
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
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
