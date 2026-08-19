package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.ExpenseRecord
import com.example.data.entity.MaintenanceBill
import com.example.data.entity.NonFeeRevenue
import com.example.data.entity.PaymentMode
import com.example.data.entity.PaymentStatus
import com.example.data.entity.Resident
import com.example.data.entity.RevenueSource
import com.example.data.entity.SocietyInfo
import com.example.data.localization.AppLanguage
import com.example.data.localization.AppStrings
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.GoldTertiary
import com.example.ui.theme.MintSecondary
import com.example.ui.theme.SlateOutline
import com.example.ui.theme.SlateOutlineVariant
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusOverdueBg
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryContainer
import com.example.viewmodel.FinancialOverview
import com.example.viewmodel.SocietyViewModel

@Composable
fun AccountingScreen(
    viewModel: SocietyViewModel,
    financial: FinancialOverview,
    society: SocietyInfo?,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf(
        AppStrings.get("balance_sheet_title", language),
        AppStrings.get("member_ledger", language),
        AppStrings.get("expense_journal", language),
        AppStrings.get("non_fee_revenue", language)
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
                    modifier = Modifier.testTag("accounting_subtab_$index")
                )
            }
        }

        when (selectedSubTab) {
            0 -> BalanceSheetView(viewModel = viewModel, financial = financial, society = society, language = language)
            1 -> MemberLedgerView(viewModel = viewModel, society = society, language = language)
            2 -> ExpenseJournalView(viewModel = viewModel, language = language)
            3 -> NonFeeRevenueView(viewModel = viewModel, language = language)
        }
    }
}

// 1. General Balance Sheet View
@Composable
fun BalanceSheetView(
    viewModel: SocietyViewModel,
    financial: FinancialOverview,
    society: SocietyInfo?,
    language: AppLanguage
) {
    var selectedFY by remember { mutableStateOf("FY 2026-2027") }
    var selectedMonth by remember { mutableStateOf("August 2026") }
    var showExportDialog by remember { mutableStateOf(false) }

    if (showExportDialog) {
        val exportText = viewModel.generateExportSummary()
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Text(
                    text = AppStrings.get("balance_sheet_title", language),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = exportText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportDialog = false
                        viewModel.showMessage(if (language == AppLanguage.MARATHI) "ताळेबंद प्रत डाऊनलोड केली!" else "Balance Sheet exported!")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text(if (language == AppLanguage.MARATHI) "प्रत सामायिक करा" else "Export Document")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(AppStrings.get("close", language))
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Filter row & Export Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SlateSurfaceVariant,
                    border = BorderStroke(1.dp, SlateOutlineVariant)
                ) {
                    Text(
                        text = "📅 $selectedFY • $selectedMonth",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                OutlinedButton(
                    onClick = { showExportDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, TealPrimary),
                    modifier = Modifier.testTag("export_balance_sheet_btn")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = AppStrings.get("export_pdf_text", language),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = TealPrimary
                    )
                }
            }
        }

        // Summary Net Surplus Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TealPrimaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = AppStrings.get("net_surplus", language),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                        color = TealPrimary
                    )
                    Text(
                        text = "₹%,.2f".format(financial.netSurplus),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = TealPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total Gross Income: ₹%,.2f | Total Expenses: ₹%,.2f".format(financial.totalGrossIncome, financial.totalOperatingExpenses),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Statement: Income vs Expenditure
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SlateOutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = AppStrings.get("income_expenditure", language),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // Income Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "1. " + AppStrings.get("kpi_total_collection", language), style = MaterialTheme.typography.bodyMedium)
                        Text(text = "₹%,.2f".format(financial.totalMaintenanceCollected), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = StatusPaid)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "2. " + AppStrings.get("non_fee_revenue", language), style = MaterialTheme.typography.bodyMedium)
                        Text(text = "₹%,.2f".format(financial.totalNonFeeRevenue), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = StatusPaid)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = AppStrings.get("total_income", language), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text(text = "₹%,.2f".format(financial.totalGrossIncome), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = StatusPaid)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // Expense Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = AppStrings.get("kpi_operating_expenses", language), style = MaterialTheme.typography.bodyMedium)
                        Text(text = "₹%,.2f".format(financial.totalOperatingExpenses), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = StatusOverdue)
                    }
                }
            }
        }

        // Statement: Assets vs Liabilities / Funds
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SlateOutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (language == AppLanguage.MARATHI) "संस्थेची मालमत्ता व देणी (Assets & Liabilities)" else "Assets & Liabilities Summary",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Text(
                        text = if (language == AppLanguage.MARATHI) "मालमत्ता व निधी (Assets):" else "Current Assets & Funds:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = TealPrimary)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "• " + AppStrings.get("kpi_bank_balance", language) + " (${society?.bankAccountName ?: "Bank"})", style = MaterialTheme.typography.bodySmall)
                        Text(text = "₹%,.2f".format(financial.bankBalance), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "• " + AppStrings.get("kpi_cash_balance", language), style = MaterialTheme.typography.bodySmall)
                        Text(text = "₹%,.2f".format(financial.cashBalance), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "• " + AppStrings.get("kpi_outstanding_dues", language), style = MaterialTheme.typography.bodySmall)
                        Text(text = "₹%,.2f".format(financial.totalOutstandingDues), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = GoldTertiary))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (language == AppLanguage.MARATHI) "राखीव निधी व दायित्व (Liabilities & Reserves):" else "Statutory Reserves & Funds:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = GoldTertiary)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "• Sinking Reserve Fund (सिंकिंग फंड)", style = MaterialTheme.typography.bodySmall)
                        Text(text = "₹%,.2f".format(financial.totalGrossIncome * 0.15), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "• Major Repair & Painting Reserve", style = MaterialTheme.typography.bodySmall)
                        Text(text = "₹%,.2f".format(financial.totalGrossIncome * 0.10), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

// 2. Member Ledger Statements View
@Composable
fun MemberLedgerView(
    viewModel: SocietyViewModel,
    society: SocietyInfo?,
    language: AppLanguage
) {
    val residents by viewModel.residents.collectAsState()
    val bills by viewModel.bills.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedBillForReceipt by remember { mutableStateOf<MaintenanceBill?>(null) }

    if (selectedBillForReceipt != null) {
        ReceiptDialog(
            bill = selectedBillForReceipt!!,
            society = society,
            language = language,
            onDismiss = { selectedBillForReceipt = null }
        )
    }

    val filteredResidents = residents.filter {
        it.flatNumber.contains(searchQuery, ignoreCase = true) ||
                it.ownerName.contains(searchQuery, ignoreCase = true)
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
                label = { Text(AppStrings.get("search_flat", language)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ledger_search_input")
            )
        }

        items(filteredResidents) { resident ->
            val residentBills = bills.filter { it.flatNumber == resident.flatNumber }
            Card(
                shape = RoundedCornerShape(16.dp),
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
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${resident.flatType.displayName} • ${resident.squareFeet} Sq.Ft • Slot: ${resident.parkingSlot.ifBlank { "N/A" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Balance Due",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "₹%,.2f".format(resident.balanceDue),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (resident.balanceDue > 0) StatusOverdue else StatusPaid
                                )
                            )
                        }
                    }

                    if (residentBills.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                        Text(
                            text = if (language == AppLanguage.MARATHI) "मासिक विवरण व पावत्या:" else "Recent Statements & Receipts:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        residentBills.forEach { bill ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = bill.billMonth, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    Text(text = "₹%,.2f".format(bill.totalAmount), style = MaterialTheme.typography.bodySmall)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StatusBadge(status = bill.status, language = language)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { selectedBillForReceipt = bill },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Receipt, contentDescription = "Receipt", tint = TealPrimary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 3. Expense Journal View & Logger
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseJournalView(
    viewModel: SocietyViewModel,
    language: AppLanguage
) {
    val expenses by viewModel.expenses.collectAsState()
    var isAddExpenseOpen by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf<ExpenseCategory?>(null) }

    if (isAddExpenseOpen) {
        AddExpenseDialog(
            language = language,
            onDismiss = { isAddExpenseOpen = false },
            onSubmit = { title, cat, amount, date, vendor, phone, mode, notes ->
                viewModel.addExpense(title, cat, amount, date, vendor, phone, mode, notes)
                isAddExpenseOpen = false
            }
        )
    }

    val filteredExpenses = if (selectedCategoryFilter != null) {
        expenses.filter { it.category == selectedCategoryFilter }
    } else {
        expenses
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
                    text = AppStrings.get("expense_journal", language),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Button(
                    onClick = { isAddExpenseOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusOverdue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_expense_modal_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStrings.get("record_expense", language), style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        items(filteredExpenses) { expense ->
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
                                text = expense.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${if (language == AppLanguage.MARATHI) expense.category.labelMr else expense.category.labelEn} • ${expense.date}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "₹%,.2f".format(expense.amount),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = StatusOverdue)
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Vendor: ${expense.vendorName} (${expense.vendorContact.ifBlank { "Direct" }})",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Mode: ${expense.paymentMode.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TealPrimary
                        )
                    }
                    if (expense.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Notes: ${expense.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// 4. Non-Fee Revenue View & Logger
@Composable
fun NonFeeRevenueView(
    viewModel: SocietyViewModel,
    language: AppLanguage
) {
    val revenues by viewModel.revenues.collectAsState()
    var isAddRevenueOpen by remember { mutableStateOf(false) }

    if (isAddRevenueOpen) {
        AddNonFeeRevenueDialog(
            language = language,
            onDismiss = { isAddRevenueOpen = false },
            onSubmit = { title, cat, amount, date, payer, flat, mode, notes ->
                viewModel.addNonFeeRevenue(title, cat, amount, date, payer, flat, mode, notes)
                isAddRevenueOpen = false
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
                    text = AppStrings.get("non_fee_revenue", language),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Button(
                    onClick = { isAddRevenueOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_non_fee_modal_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStrings.get("record_non_fee", language), style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        items(revenues) { revenue ->
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
                                text = revenue.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${if (language == AppLanguage.MARATHI) revenue.sourceCategory.labelMr else revenue.sourceCategory.labelEn} • ${revenue.date}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "₹%,.2f".format(revenue.amount),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = StatusPaid)
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Payer: ${revenue.payerName}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "${revenue.paymentMode.name} • ${revenue.receiptNumber}", style = MaterialTheme.typography.bodySmall, color = TealPrimary)
                    }
                }
            }
        }
    }
}

// Add Expense Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSubmit: (
        title: String,
        category: ExpenseCategory,
        amount: Double,
        date: String,
        vendor: String,
        phone: String,
        mode: PaymentMode,
        notes: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.WATER_TANKER) }
    var amountStr by remember { mutableStateOf("") }
    var vendor by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(PaymentMode.UPI) }
    var notes by remember { mutableStateOf("") }
    var isCatExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = AppStrings.get("record_expense", language),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Expense Title (तपशील)") },
                    placeholder = { Text("e.g. Water tanker supply") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selector
                ExposedDropdownMenuBox(
                    expanded = isCatExpanded,
                    onExpandedChange = { isCatExpanded = !isCatExpanded }
                ) {
                    OutlinedTextField(
                        value = if (language == AppLanguage.MARATHI) selectedCategory.labelMr else selectedCategory.labelEn,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category (वर्गवारी)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCatExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isCatExpanded,
                        onDismissRequest = { isCatExpanded = false }
                    ) {
                        ExpenseCategory.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(if (language == AppLanguage.MARATHI) cat.labelMr else cat.labelEn) },
                                onClick = {
                                    selectedCategory = cat
                                    isCatExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(AppStrings.get("amount", language)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = vendor,
                    onValueChange = { vendor = it },
                    label = { Text(AppStrings.get("vendor_name", language)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Vendor Contact") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(AppStrings.get("notes", language)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amount > 0) {
                        onSubmit(title, selectedCategory, amount, "", vendor, phone, selectedMode, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.testTag("submit_expense_btn")
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

// Add Non-Fee Revenue Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNonFeeRevenueDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSubmit: (
        title: String,
        category: RevenueSource,
        amount: Double,
        date: String,
        payer: String,
        flatNumber: String,
        mode: PaymentMode,
        notes: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(RevenueSource.CLUBHOUSE_BOOKING) }
    var amountStr by remember { mutableStateOf("") }
    var payer by remember { mutableStateOf("") }
    var flatNumber by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(PaymentMode.UPI) }
    var notes by remember { mutableStateOf("") }
    var isCatExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = AppStrings.get("record_non_fee", language),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Revenue Title (तपशील)") },
                    placeholder = { Text("e.g. Clubhouse birthday booking") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selector
                ExposedDropdownMenuBox(
                    expanded = isCatExpanded,
                    onExpandedChange = { isCatExpanded = !isCatExpanded }
                ) {
                    OutlinedTextField(
                        value = if (language == AppLanguage.MARATHI) selectedCategory.labelMr else selectedCategory.labelEn,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Source (स्त्रोत)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCatExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isCatExpanded,
                        onDismissRequest = { isCatExpanded = false }
                    ) {
                        RevenueSource.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(if (language == AppLanguage.MARATHI) cat.labelMr else cat.labelEn) },
                                onClick = {
                                    selectedCategory = cat
                                    isCatExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(AppStrings.get("amount", language)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = payer,
                    onValueChange = { payer = it },
                    label = { Text("Payer Name (व्यक्तीचे नाव)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = flatNumber,
                    onValueChange = { flatNumber = it },
                    label = { Text(AppStrings.get("unit_number", language)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(AppStrings.get("notes", language)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amount > 0) {
                        onSubmit(title, selectedCategory, amount, "", payer, flatNumber, selectedMode, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.testTag("submit_non_fee_btn")
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
