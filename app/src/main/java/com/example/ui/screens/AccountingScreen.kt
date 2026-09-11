package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import com.example.ui.components.DialogWidth
import com.example.ui.components.WideDialogProperties
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
import com.example.util.LedgerPdf
import com.example.viewmodel.FinancialOverview
import com.example.viewmodel.SocietyViewModel
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import com.example.data.entity.billingMonths
import com.example.data.entity.chargesSubtotal
import com.example.data.entity.derivedStatus
import com.example.data.entity.payableTotal
import com.example.data.entity.pendingAmount
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.data.entity.UserRole

@Composable
fun AccountingScreen(
    viewModel: SocietyViewModel,
    financial: FinancialOverview,
    society: SocietyInfo?,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    // Chairman and Treasurer keep the write actions; everyone else sees the
    // same books, read-only.
    val currentUser by viewModel.currentUser.collectAsState()
    val canEdit = currentUser?.role == UserRole.CHAIRMAN || currentUser?.role == UserRole.TREASURER

    // A member opens Accounts to see what the society holds - nothing else.
    // No sub-tabs, no ledgers, no journals and no export.
    val isMember = currentUser?.role == UserRole.RESIDENT || currentUser?.role == UserRole.TENANT
    if (isMember) {
        MemberBalanceView(
            financial = financial,
            society = society,
            language = language,
            modifier = modifier
        )
        return
    }

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
            2 -> ExpenseJournalView(viewModel = viewModel, language = language, canEdit = canEdit)
            3 -> NonFeeRevenueView(viewModel = viewModel, language = language, canEdit = canEdit)
        }
    }
}

/**
 * Accounts as a member sees it: the society's balances, and nothing else.
 *
 * The ledgers, the expense journal, the revenue register and every export
 * belong to the committee, so none of them are reachable from here.
 */
@Composable
fun MemberBalanceView(
    financial: FinancialOverview,
    society: SocietyInfo?,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TealPrimaryContainer),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("member_balance_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = AppStrings.get("kpi_net_treasury", language),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    color = TealPrimary
                )
                Text(
                    text = "₹%,.2f".format(financial.netTreasuryBalance),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = TealPrimary,
                    modifier = Modifier.testTag("member_net_treasury_amount")
                )
                Text(
                    text = society?.name.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, SlateOutlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = AppStrings.get("kpi_bank_balance", language), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "₹%,.2f".format(financial.bankBalance),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TealPrimary
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = AppStrings.get("kpi_cash_balance", language), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "₹%,.2f".format(financial.cashBalance),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = GoldTertiary
                    )
                }
                if (society?.bankAccountName?.isNotBlank() == true) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = society.bankAccountName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
    // The period the sheet covers: the society's billing window, not a date
    // hard-coded into the screen.
    val billingMonths = remember(society) { society.billingMonths() }
    val selectedMonth = billingMonths.firstOrNull().orEmpty()
    val selectedFY = remember(billingMonths) {
        val from = billingMonths.lastOrNull()?.substringAfterLast(" ").orEmpty()
        val to = billingMonths.firstOrNull()?.substringAfterLast(" ").orEmpty()
        when {
            from.isBlank() || to.isBlank() -> ""
            from == to -> "FY $from"
            else -> "FY $from-$to"
        }
    }
    var showExportDialog by remember { mutableStateOf(false) }

    if (showExportDialog) {
        val exportText = viewModel.generateExportSummary()
        AlertDialog(
            modifier = DialogWidth,
            properties = WideDialogProperties,
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
                        Text(text = "• " + AppStrings.get("kpi_bank_balance", language), style = MaterialTheme.typography.bodySmall)
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
                        Text(text = "• " + AppStrings.get("fund_sinking", language), style = MaterialTheme.typography.bodySmall)
                        Text(text = "₹%,.2f".format(financial.totalGrossIncome * 0.15), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "• " + AppStrings.get("fund_major_repair", language), style = MaterialTheme.typography.bodySmall)
                        Text(text = "₹%,.2f".format(financial.totalGrossIncome * 0.10), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "• " + AppStrings.get("fund_csr", language), style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "₹%,.2f".format(financial.totalGrossIncome * 0.02),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.testTag("fund_csr_amount")
                        )
                    }
                }
            }
        }

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
                        text = "📅 ${selectedFY.ifBlank { "Billing" }} • ${selectedMonth.ifBlank { "-" }}",
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
    }
}

// 2. Member Ledger Statements View
/**
 * Member Ledger.
 *
 * Opens on the members who still owe something - that is what a chairman came
 * here for - with a Paid / All toggle to widen it. Tapping a member card opens
 * their month-by-month ledger: what was billed, what was paid, what is left,
 * and the payment details behind each row.
 */
@Composable
fun MemberLedgerView(
    viewModel: SocietyViewModel,
    society: SocietyInfo?,
    language: AppLanguage
) {
    val residents by viewModel.members.collectAsState()
    val bills by viewModel.bills.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var ledgerFilter by remember { mutableStateOf(LedgerFilter.UNPAID) }
    var openMember by remember { mutableStateOf<Resident?>(null) }
    var selectedBillForReceipt by remember { mutableStateOf<MaintenanceBill?>(null) }

    if (selectedBillForReceipt != null) {
        ReceiptDialog(
            bill = selectedBillForReceipt!!,
            society = society,
            language = language,
            onDismiss = { selectedBillForReceipt = null }
        )
    }

    openMember?.let { member ->
        MemberLedgerDetailDialog(
            member = member,
            bills = bills.filter { it.flatNumber.equals(member.flatNumber, ignoreCase = true) },
            society = society,
            language = language,
            onDismiss = { openMember = null },
            onOpenReceipt = { selectedBillForReceipt = it }
        )
    }

    // Pending is taken from the bills themselves, so the list can never
    // disagree with what the member's own ledger shows.
    val pendingByFlat = remember(bills) {
        bills.groupBy { it.flatNumber.uppercase() }
            .mapValues { (_, list) -> list.sumOf { it.pendingAmount } }
    }
    // How many bills each unit has. A unit that was never billed owes nothing,
    // which is not the same thing as having paid - see the filter below.
    val billCountByFlat = remember(bills) {
        bills.groupingBy { it.flatNumber.uppercase() }.eachCount()
    }
    val billedByFlat = remember(bills) {
        bills.groupBy { it.flatNumber.uppercase() }
            .mapValues { (_, list) -> list.sumOf { it.payableTotal } }
    }
    val paidByFlat = remember(bills) {
        bills.groupBy { it.flatNumber.uppercase() }
            .mapValues { (_, list) -> list.sumOf { it.amountPaid } }
    }

    val visibleMembers = residents
        .filter { it.ownerName.isNotBlank() }
        .filter {
            it.flatNumber.contains(searchQuery, ignoreCase = true) ||
                it.ownerName.contains(searchQuery, ignoreCase = true)
        }
        .filter { member ->
            val pending = pendingByFlat[member.flatNumber.uppercase()] ?: 0.0
            val billCount = billCountByFlat[member.flatNumber.uppercase()] ?: 0
            when (ledgerFilter) {
                LedgerFilter.UNPAID -> pending > 0.009
                /*
                 * Paid means "billed and settled". A unit that has never been
                 * billed also has nothing pending, and used to be listed here
                 * as paid ₹0 - which read as a collection that never happened.
                 */
                LedgerFilter.PAID -> billCount > 0 && pending <= 0.009
                LedgerFilter.ALL -> true
            }
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

        // Unpaid / Paid / All - the list follows the selection.
        item {
            Column {
                Text(
                    text = AppStrings.get("filter_label", language),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LedgerFilter.entries.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ledger_filter_${option.name.lowercase()}")
                        ) {
                            RadioButton(
                                selected = ledgerFilter == option,
                                onClick = { ledgerFilter = option },
                                colors = RadioButtonDefaults.colors(selectedColor = TealPrimary)
                            )
                            Text(
                                text = AppStrings.get(option.labelKey, language),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (ledgerFilter == option) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
                Text(
                    text = "${visibleMembers.size} member(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(visibleMembers) { resident ->
            val pending = pendingByFlat[resident.flatNumber.uppercase()] ?: 0.0
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SlateOutlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openMember = resident }
                    .testTag("ledger_member_${resident.flatNumber}")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${resident.flatNumber} • ${resident.ownerName}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        // Wing and unit type, the same two facts the member
                        // list shows, so the ledger reads the same way.
                        Text(
                            text = listOfNotNull(
                                resident.wing.takeIf { it.isNotBlank() }?.let { "Wing $it" },
                                resident.unitType.takeIf { it.isNotBlank() }
                                    ?: resident.flatType.displayName
                            ).joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val billCount = billCountByFlat[resident.flatNumber.uppercase()] ?: 0
                        Spacer(modifier = Modifier.height(4.dp))
                        // In the All list the rows are a mix, so each one says
                        // where it stands rather than leaving it to be worked
                        // out from the numbers on the right.
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                billCount == 0 -> SlateSurfaceVariant
                                pending > 0.009 -> StatusOverdueBg
                                else -> StatusPaidBg
                            }
                        ) {
                            Text(
                                text = when {
                                    billCount == 0 -> "No bills raised yet"
                                    pending > 0.009 -> AppStrings.get("status_unpaid", language)
                                    else -> AppStrings.get("status_paid", language)
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = when {
                                    billCount == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                                    pending > 0.009 -> StatusOverdue
                                    else -> StatusPaid
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    // Billed and paid alongside pending, so "All" says what
                    // actually happened rather than only what is left.
                    Column(horizontalAlignment = Alignment.End) {
                        val billed = billedByFlat[resident.flatNumber.uppercase()] ?: 0.0
                        val paid = paidByFlat[resident.flatNumber.uppercase()] ?: 0.0
                        Text(
                            text = "Billed ₹%,.0f".format(billed),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Paid ₹%,.0f".format(paid),
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusPaid
                        )
                        Text(
                            text = "Pending ₹%,.2f".format(pending),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (pending > 0.009) StatusOverdue else StatusPaid
                            )
                        )
                    }
                }
            }
        }
    }
}

/** The three states the member list can be filtered to. */
enum class LedgerFilter(val labelKey: String) {
    UNPAID("status_unpaid"),
    PAID("status_paid"),
    ALL("filter_all")
}

/**
 * One member's ledger, laid out as a plain month-wise table - Month, Bill, Late
 * Fee, Total, Paid, Pending - with a running outstanding under it and a button
 * that writes the same table out as a PDF to save or share.
 */
@Composable
private fun MemberLedgerDetailDialog(
    member: Resident,
    bills: List<MaintenanceBill>,
    society: SocietyInfo?,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onOpenReceipt: (MaintenanceBill) -> Unit
) {
    val context = LocalContext.current
    val ordered = remember(bills) { bills.sortedBy { ledgerMonthKey(it.billMonth) } }
    val totalBilled = ordered.sumOf { it.payableTotal }
    val totalPaid = ordered.sumOf { it.amountPaid }
    val totalPending = ordered.sumOf { it.pendingAmount }
    var pdfStatus by remember { mutableStateOf<String?>(null) }
    var isPdfError by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = DialogWidth,
        properties = WideDialogProperties,
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "${member.flatNumber} • ${member.ownerName}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = AppStrings.get("member_ledger", language),
                    style = MaterialTheme.typography.bodySmall,
                    color = TealPrimary
                )
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (ordered.isEmpty()) {
                    Text(
                        text = "No bills have been raised for this unit yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // Header row.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TealPrimaryContainer, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        LedgerCell("Month", weight = 2.0f, bold = true)
                        LedgerCell("Bill", weight = 1.3f, bold = true, end = true)
                        LedgerCell("Late", weight = 1.0f, bold = true, end = true)
                        LedgerCell("Total", weight = 1.3f, bold = true, end = true)
                        LedgerCell("Paid", weight = 1.3f, bold = true, end = true)
                        LedgerCell("Pending", weight = 1.3f, bold = true, end = true)
                    }

                    ordered.forEach { bill ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { if (bill.amountPaid > 0) { onDismiss(); onOpenReceipt(bill) } }
                                .padding(horizontal = 6.dp, vertical = 7.dp)
                                .testTag("ledger_row_${bill.billMonth}")
                        ) {
                            LedgerCell(bill.billMonth, weight = 2.0f)
                            LedgerCell("%,.0f".format(bill.chargesSubtotal), weight = 1.3f, end = true)
                            LedgerCell("%,.0f".format(bill.lateFeeAmount), weight = 1.0f, end = true)
                            LedgerCell("%,.0f".format(bill.payableTotal), weight = 1.3f, end = true, bold = true)
                            LedgerCell(
                                "%,.0f".format(bill.amountPaid),
                                weight = 1.3f,
                                end = true,
                                colour = StatusPaid
                            )
                            LedgerCell(
                                "%,.0f".format(bill.pendingAmount),
                                weight = 1.3f,
                                end = true,
                                bold = true,
                                colour = if (bill.pendingAmount > 0.009) StatusOverdue else StatusPaid
                            )
                        }
                        HorizontalDivider(color = SlateOutlineVariant)
                    }

                    // Closing totals.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateSurfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        LedgerCell("Total", weight = 2.0f, bold = true)
                        LedgerCell("%,.0f".format(ordered.sumOf { it.chargesSubtotal }), weight = 1.3f, bold = true, end = true)
                        LedgerCell("%,.0f".format(ordered.sumOf { it.lateFeeAmount }), weight = 1.0f, bold = true, end = true)
                        LedgerCell("%,.0f".format(totalBilled), weight = 1.3f, bold = true, end = true)
                        LedgerCell("%,.0f".format(totalPaid), weight = 1.3f, bold = true, end = true)
                        LedgerCell(
                            "%,.0f".format(totalPending),
                            weight = 1.3f,
                            bold = true,
                            end = true,
                            colour = if (totalPending > 0.009) StatusOverdue else StatusPaid
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap a paid month to open its receipt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val file = LedgerPdf.generate(context, member, ordered, society)
                                isPdfError = file == null
                                pdfStatus = if (file == null) {
                                    "Could not create the ledger PDF."
                                } else {
                                    "Saved as ${file.name}"
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, TealPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ledger_pdf_btn")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(AppStrings.get("download_pdf", language), color = TealPrimary, style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = {
                                val failure = LedgerPdf.share(context, member, ordered, society)
                                isPdfError = failure != null
                                pdfStatus = failure
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ledger_share_pdf_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(AppStrings.get("share_pdf", language), style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    pdfStatus?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isPdfError) MaterialTheme.colorScheme.error else StatusPaid
                        )
                    }
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

/** One cell of the month-wise ledger table. */
@Composable
private fun RowScope.LedgerCell(
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
private fun LedgerLine(
    label: String,
    value: String,
    bold: Boolean = false,
    colour: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
            ),
            color = colour
        )
    }
}

/** Chronological ordering for "August 2026" style month labels. */
private fun ledgerMonthKey(billMonth: String): Long = runCatching {
    SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).parse(billMonth)?.time ?: 0L
}.getOrDefault(0L)

// 3. Expense Journal View & Logger
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseJournalView(
    viewModel: SocietyViewModel,
    language: AppLanguage,
    canEdit: Boolean
) {
    val expenses by viewModel.expenses.collectAsState()
    val financial by viewModel.financialOverview.collectAsState()
    var isAddExpenseOpen by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseRecord?>(null) }
    var expensePendingDeletion by remember { mutableStateOf<ExpenseRecord?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<ExpenseCategory?>(null) }

    if (isAddExpenseOpen || editingExpense != null) {
        val editing = editingExpense
        AddExpenseDialog(
            language = language,
            availableBank = financial.bankBalance,
            availableCash = financial.cashBalance,
            existing = editing,
            onDismiss = { isAddExpenseOpen = false; editingExpense = null },
            onSubmit = { title, cat, amount, date, vendor, phone, mode, notes ->
                if (editing == null) {
                    viewModel.addExpense(title, cat, amount, date, vendor, phone, mode, notes) { ok, message ->
                        if (!ok) viewModel.showMessage(message)
                    }
                } else {
                    viewModel.updateExpense(editing, title, cat, amount, date, vendor, phone, mode, notes) { _, message ->
                        viewModel.showMessage(message)
                    }
                }
                isAddExpenseOpen = false
                editingExpense = null
            }
        )
    }

    expensePendingDeletion?.let { expense ->
        AlertDialog(
            modifier = DialogWidth,
            properties = WideDialogProperties,
            onDismissRequest = { expensePendingDeletion = null },
            title = {
                Text(
                    text = if (language == AppLanguage.MARATHI) "खर्च हटवायचा?" else "Delete this expense?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "${expense.title} • ₹%,.2f".format(expense.amount) +
                        "\n\nThe amount goes back into the " +
                        (if (expense.paymentMode == PaymentMode.CASH) "cash" else "bank") +
                        " balance.",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteExpense(expense) { _, message -> viewModel.showMessage(message) }
                        expensePendingDeletion = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusOverdue),
                    modifier = Modifier.testTag("confirm_delete_expense_btn")
                ) {
                    Text(if (language == AppLanguage.MARATHI) "हटवा" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { expensePendingDeletion = null }) {
                    Text(AppStrings.get("cancel", language))
                }
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

                if (canEdit) {
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
                } else {
                    ViewOnlyChip(language)
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

                    // A voucher entered wrong can be corrected or withdrawn -
                    // both put the treasury back where it should be.
                    if (canEdit) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { editingExpense = expense },
                                modifier = Modifier.testTag("edit_expense_btn_${expense.receiptNumber}")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(AppStrings.get("edit_profile", language), color = TealPrimary, style = MaterialTheme.typography.labelMedium)
                            }
                            TextButton(
                                onClick = { expensePendingDeletion = expense },
                                modifier = Modifier.testTag("delete_expense_btn_${expense.receiptNumber}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = StatusOverdue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (language == AppLanguage.MARATHI) "हटवा" else "Delete",
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

// 4. Non-Fee Revenue View & Logger
@Composable
fun NonFeeRevenueView(
    viewModel: SocietyViewModel,
    language: AppLanguage,
    canEdit: Boolean
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

                if (canEdit) {
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
                } else {
                    ViewOnlyChip(language)
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
/**
 * Record Expense.
 *
 * Category comes first because it is what decides the rest of the form; the
 * title is optional and falls back to the category name. The society's own cash
 * and bank balances are shown against the amount, and an expense larger than
 * what it holds cannot be submitted at all.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    language: AppLanguage,
    availableBank: Double,
    availableCash: Double,
    /** The expense being edited, or null when recording a new one. */
    existing: ExpenseRecord? = null,
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
    var selectedCategory by remember(existing) {
        mutableStateOf(existing?.category ?: ExpenseCategory.WATER_TANKER)
    }
    var title by remember(existing) { mutableStateOf(existing?.title.orEmpty()) }
    var amountStr by remember(existing) {
        mutableStateOf(existing?.amount?.takeIf { it > 0 }?.toInt()?.toString().orEmpty())
    }
    var vendor by remember(existing) { mutableStateOf(existing?.vendorName.orEmpty()) }
    var phone by remember(existing) { mutableStateOf(existing?.vendorContact.orEmpty()) }
    var selectedMode by remember(existing) { mutableStateOf(existing?.paymentMode ?: PaymentMode.UPI) }
    var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }
    var isCatExpanded by remember { mutableStateOf(false) }
    var isModeExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Editing an expense that is already recorded: the money it took out of the
    // treasury is available to it again, so an edit is not blocked by its own
    // spending.
    val totalAvailable = availableBank + availableCash + (existing?.amount ?: 0.0)
    val amount = amountStr.toDoubleOrNull() ?: 0.0
    val isOverBudget = amount > totalAvailable

    AlertDialog(
        modifier = DialogWidth,
        properties = WideDialogProperties,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existing == null) {
                    AppStrings.get("record_expense", language)
                } else {
                    AppStrings.get("edit_expense", language)
                },
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
                // What the society can actually spend, before anything is typed.
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = AppStrings.get("available_balance", language),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Bank ₹%,.2f  •  Cash ₹%,.2f".format(availableBank, availableCash),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Total ₹%,.2f".format(totalAvailable),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TealPrimary,
                            modifier = Modifier.testTag("expense_available_balance")
                        )
                    }
                }

                // 1. Category
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
                            .testTag("expense_category_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = isCatExpanded,
                        onDismissRequest = { isCatExpanded = false }
                    ) {
                        ExpenseCategory.entries.forEach { cat ->
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

                // 2. Title - optional, defaults to the category name.
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Expense Title (optional)") },
                    placeholder = { Text(selectedCategory.labelEn) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_title_input")
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { raw ->
                        amountStr = raw.filter { it.isDigit() || it == '.' }
                        error = null
                    },
                    label = { Text(AppStrings.get("amount", language)) },
                    isError = isOverBudget,
                    supportingText = {
                        if (isOverBudget) {
                            Text(
                                text = AppStrings.get("insufficient_balance", language),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input")
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
                        PaymentMode.entries.forEach { mode ->
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
                    value = vendor,
                    onValueChange = { vendor = it },
                    label = { Text(AppStrings.get("vendor_name", language)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { ch -> ch.isDigit() }.take(10) },
                    label = { Text("Vendor Contact") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(AppStrings.get("notes", language)) },
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("expense_error_text")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                // Nothing can be submitted that the society cannot pay for.
                enabled = amount > 0 && !isOverBudget,
                onClick = {
                    if (amount <= 0) {
                        error = "Enter an amount greater than zero."
                        return@Button
                    }
                    if (amount > totalAvailable) {
                        error = AppStrings.get("insufficient_balance", language)
                        return@Button
                    }
                    onSubmit(
                        title.ifBlank { selectedCategory.labelEn },
                        selectedCategory,
                        amount,
                        existing?.date.orEmpty(),
                        vendor,
                        phone,
                        selectedMode,
                        notes
                    )
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
        modifier = DialogWidth,
        properties = WideDialogProperties,
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

/** Badge shown where a write button would be, for read-only viewers. */
@Composable
private fun ViewOnlyChip(language: AppLanguage) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SlateSurfaceVariant,
        border = BorderStroke(1.dp, SlateOutlineVariant),
        modifier = Modifier.testTag("accounts_view_only_chip")
    ) {
        Text(
            text = AppStrings.get("view_only", language),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
