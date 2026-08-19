package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.sp
import com.example.data.entity.Notice
import com.example.data.entity.Resident
import com.example.data.entity.SocietyInfo
import com.example.data.entity.UserRole
import com.example.data.entity.VisitorStatus
import com.example.data.localization.AppLanguage
import com.example.data.localization.AppStrings
import com.example.ui.components.AnimatedSectionCard
import com.example.ui.components.FinancialKpiCard
import com.example.ui.components.GateActivityFeed
import com.example.ui.components.MaintenanceStatusDashboard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.VisitorStatusBadge
import com.example.ui.theme.GoldTertiary
import com.example.ui.theme.MintSecondary
import com.example.ui.theme.SlateOutlineVariant
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusOverdueBg
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryContainer
import com.example.viewmodel.FinancialOverview
import com.example.viewmodel.NavDestination
import com.example.viewmodel.SocietyViewModel

@Composable
fun DashboardScreen(
    viewModel: SocietyViewModel,
    financial: FinancialOverview,
    society: SocietyInfo?,
    currentUser: Resident?,
    language: AppLanguage,
    onNavigate: (NavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val notices by viewModel.notices.collectAsState()
    val visitors by viewModel.visitors.collectAsState()
    val bills by viewModel.bills.collectAsState()
    val expenses by viewModel.expenses.collectAsState()

    var showExportDialog by remember { mutableStateOf(false) }

    if (showExportDialog) {
        val summaryText = viewModel.generateExportSummary()
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Text(
                    text = AppStrings.get("export_pdf_text", language),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = summaryText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportDialog = false
                        viewModel.showMessage(
                            when (language) {
                                AppLanguage.MARATHI -> "वित्तीय विवरण प्रत तयार केली!"
                                AppLanguage.HINDI -> "वित्तीय विवरण सारांश तैयार!"
                                AppLanguage.ENGLISH -> "Statement summary exported!"
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.testTag("confirm_export_btn")
                ) {
                    Text(
                        when (language) {
                            AppLanguage.MARATHI -> "प्रत सामायिक करा"
                            AppLanguage.HINDI -> "साझा करें"
                            AppLanguage.ENGLISH -> "Share Statement"
                        }
                    )
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
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Pinned Notices Banner (Animated)
        val pinnedNotices = notices.filter { it.isPinned }
        if (pinnedNotices.isNotEmpty()) {
            item {
                AnimatedSectionCard(delayMillis = 50) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(NavDestination.COMMUNITY) }
                            .testTag("pinned_notice_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = GoldTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pinnedNotices.first().title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF92400E),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = pinnedNotices.first().content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB45309),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. MAINTENANCE STATUS DASHBOARD COMPONENT (NEW)
        item {
            AnimatedSectionCard(delayMillis = 100) {
                MaintenanceStatusDashboard(
                    bills = bills,
                    language = language,
                    onNavigateToMaintenance = { onNavigate(NavDestination.MAINTENANCE) }
                )
            }
        }

        // 3. GATE ACTIVITY FEED COMPONENT (NEW)
        item {
            AnimatedSectionCard(delayMillis = 150) {
                GateActivityFeed(
                    visitors = visitors,
                    language = language,
                    onNavigateToGate = { onNavigate(NavDestination.GATE) }
                )
            }
        }

        // 4. Financial Overview Top KPI Cards
        item {
            AnimatedSectionCard(delayMillis = 200) {
                Column {
                    SectionHeader(
                        title = when (language) {
                            AppLanguage.MARATHI -> "वित्तीय डॅशबोर्ड व ताळेबंद अहवाल"
                            AppLanguage.HINDI -> "वित्तीय डैशबोर्ड एवं तुलन पत्र"
                            AppLanguage.ENGLISH -> "Executive Financial Overview"
                        },
                        subtitle = "FY 2026-27 • ${society?.name ?: ""}",
                        actionLabel = AppStrings.get("export_pdf_text", language),
                        onActionClick = { showExportDialog = true },
                        actionTestTag = "dashboard_export_btn"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FinancialKpiCard(
                            title = AppStrings.get("kpi_total_collection", language),
                            amount = financial.totalMaintenanceCollected,
                            subtitle = "${financial.paidFlatsCount} ${if (language == AppLanguage.MARATHI) "सदनिका जमा" else if (language == AppLanguage.HINDI) "फ्लैट जमा" else "flats paid"}",
                            icon = Icons.Default.Payments,
                            accentColor = StatusPaid,
                            modifier = Modifier.weight(1f)
                        )
                        FinancialKpiCard(
                            title = AppStrings.get("kpi_operating_expenses", language),
                            amount = financial.totalOperatingExpenses,
                            subtitle = "${expenses.size} ${if (language == AppLanguage.MARATHI) "नोंदवलेले खर्च" else if (language == AppLanguage.HINDI) "व्यय प्रविष्टियां" else "vouchers"}",
                            icon = Icons.Default.TrendingDown,
                            accentColor = StatusOverdue,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FinancialKpiCard(
                            title = AppStrings.get("kpi_net_treasury", language),
                            amount = financial.netTreasuryBalance,
                            subtitle = "Bank + Cash",
                            icon = Icons.Default.AccountBalanceWallet,
                            accentColor = TealPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        FinancialKpiCard(
                            title = AppStrings.get("kpi_outstanding_dues", language),
                            amount = financial.totalOutstandingDues,
                            subtitle = "${financial.pendingFlatsCount} ${if (language == AppLanguage.MARATHI) "थकीत सदनिका" else if (language == AppLanguage.HINDI) "बकाया फ्लैट" else "overdue"}",
                            icon = Icons.Default.Warning,
                            accentColor = GoldTertiary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 5. Quick Action Hub
        item {
            AnimatedSectionCard(delayMillis = 250) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SlateOutlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = when (language) {
                                AppLanguage.MARATHI -> "जलद कामकाज (Quick Actions)"
                                AppLanguage.HINDI -> "त्वरित कार्य (Quick Actions)"
                                AppLanguage.ENGLISH -> "Operations & Actions"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            QuickActionItem(
                                label = AppStrings.get("record_payment", language),
                                icon = Icons.Default.ReceiptLong,
                                color = TealPrimary,
                                onClick = { onNavigate(NavDestination.MAINTENANCE) },
                                testTag = "quick_record_payment_btn"
                            )
                            QuickActionItem(
                                label = AppStrings.get("record_expense", language),
                                icon = Icons.Default.MoneyOff,
                                color = StatusOverdue,
                                onClick = { onNavigate(NavDestination.ACCOUNTS) },
                                testTag = "quick_record_expense_btn"
                            )
                            QuickActionItem(
                                label = AppStrings.get("gate_terminal_title", language),
                                icon = Icons.Default.Security,
                                color = MintSecondary,
                                onClick = { onNavigate(NavDestination.GATE) },
                                testTag = "quick_gate_btn"
                            )
                            QuickActionItem(
                                label = AppStrings.get("post_notice", language),
                                icon = Icons.Default.Campaign,
                                color = GoldTertiary,
                                onClick = { onNavigate(NavDestination.COMMUNITY) },
                                testTag = "quick_notice_btn"
                            )
                        }
                    }
                }
            }
        }

        // 6. Treasury Account Split Details
        item {
            AnimatedSectionCard(delayMillis = 300) {
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
                            Text(
                                text = when (language) {
                                    AppLanguage.MARATHI -> "संस्था बँक व रोख निधी"
                                    AppLanguage.HINDI -> "सोसायटी बैंक एवं नकद शेष"
                                    AppLanguage.ENGLISH -> "Bank & Cash Balances"
                                },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Net: ₹%,.2f".format(financial.netTreasuryBalance),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TealPrimary)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SlateSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = AppStrings.get("kpi_bank_balance", language),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "₹%,.2f".format(financial.bankBalance),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TealPrimary
                                    )
                                    Text(
                                        text = society?.bankAccountName ?: "HDFC Bank A/C",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SlateSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = AppStrings.get("kpi_cash_balance", language),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "₹%,.2f".format(financial.cashBalance),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = GoldTertiary
                                    )
                                    Text(
                                        text = if (language == AppLanguage.MARATHI) "पेटी कॅश (हँड कॅश)" else if (language == AppLanguage.HINDI) "पेटी नकद" else "Petty Cash in Safe",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionItem(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .testTag(testTag)
            .padding(4.dp)
            .width(72.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
    }
}
