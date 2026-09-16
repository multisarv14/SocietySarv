package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ComplaintCategory
import com.example.data.entity.MaintenanceBill
import com.example.data.entity.PaymentMode
import com.example.data.entity.PaymentStatus
import com.example.data.entity.Resident
import com.example.data.entity.SocietyInfo
import com.example.data.entity.VisitorPurpose
import com.example.data.entity.VisitorStatus
import com.example.data.entity.derivedStatus
import com.example.data.entity.financialYearOf
import com.example.data.entity.financialYears
import com.example.data.entity.payableTotal
import com.example.data.entity.pendingAmount
import com.example.data.entity.unitDues
import com.example.data.localization.AppLanguage
import com.example.ui.components.DialogWidth
import com.example.ui.components.WideDialogProperties
import com.example.data.localization.AppStrings
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.SearchableDropdownField
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.VisitorStatusBadge
import com.example.ui.components.TicketStatusBadge
import com.example.ui.components.UpiQrPaymentDialog
import com.example.ui.theme.MintSecondary
import com.example.ui.theme.SlateOutlineVariant
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryContainer
import com.example.viewmodel.SocietyViewModel

@Composable
fun ResidentPortalScreen(
    viewModel: SocietyViewModel,
    currentUser: Resident?,
    society: SocietyInfo?,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val bills by viewModel.bills.collectAsState()
    val complaints by viewModel.complaints.collectAsState()

    /*
     * Everything on this screen belongs to one unit.
     *
     * A member with a single flat never notices; a member who owns several
     * picks one from the selector below and the dues, the bills, the parking
     * and the complaint form all follow that choice.
     */
    val myUnits by viewModel.myUnits.collectAsState()
    val activeUnit by viewModel.activeUnit.collectAsState()
    val unit = activeUnit ?: currentUser

    val myFlat = unit?.flatNumber ?: ""
    val unitBills = bills.filter { it.flatNumber.equals(myFlat, ignoreCase = true) }

    /*
     * Financial year filter.
     *
     * A member looking for last year's receipts needs a way back to it, so the
     * years the society has billed in are offered and everything below - the
     * month-wise table, the statements, the receipts - follows the choice.
     */
    val financialYears = remember(society, unitBills) {
        (society.financialYears() + unitBills.map { financialYearOf(it.billMonth) })
            .filter { it.isNotBlank() }
            .distinct()
    }
    var selectedYear by remember(financialYears) {
        mutableStateOf(financialYears.firstOrNull().orEmpty())
    }
    val myBills = if (selectedYear.isBlank()) {
        unitBills
    } else {
        unitBills.filter { financialYearOf(it.billMonth) == selectedYear }
    }

    /*
     * The gate log for this member's own units and nothing else - a member has
     * no business seeing who called on their neighbours. A member who holds
     * several units sees the callers at all of them.
     */
    val visitors by viewModel.visitors.collectAsState()
    val myUnitNumbers = remember(myUnits, currentUser) {
        myUnits.map { it.flatNumber.uppercase() }
            .ifEmpty { listOfNotNull(currentUser?.flatNumber?.uppercase()) }
    }
    val myVisitors = visitors.filter { it.flatNumber.uppercase() in myUnitNumbers }

    /*
     * What this unit owes, in the three parts a member reads separately: the
     * arrears behind them, this month's bill, and any credit they are already
     * in from an overpayment. The card below shows all three and the net of
     * them, rather than a single figure that quietly disagreed with the
     * month-wise table.
     */
    val advanceCredit = (unit?.advanceBalance ?: 0.0).coerceAtLeast(0.0)
    // Every bill the unit has, not just the financial year on screen: what a
    // member owes does not reset because they looked back at last year.
    val dues = remember(unitBills, advanceCredit) { unitDues(unitBills, advanceCredit) }

    /*
     * Every payment this unit has been receipted for, newest month first.
     *
     * A bill that is only part paid carries a receipt just as a settled one
     * does, and a member had no way to reach either: the bill card offers the
     * receipt only once the bill is fully paid. This lists them all in one
     * place, which is where a member goes looking for last March anyway.
     */
    val myReceipts = remember(myBills) {
        myBills.filter { it.amountPaid > 0.009 }
            .sortedByDescending { portalMonthKey(it.billMonth) }
    }

    var selectedBillForReceipt by remember { mutableStateOf<MaintenanceBill?>(null) }
    var selectedBillForQr by remember { mutableStateOf<MaintenanceBill?>(null) }
    var isPasscodeGenOpen by remember { mutableStateOf(false) }
    var isChangePinOpen by remember { mutableStateOf(false) }
    var generatedPasscode by remember { mutableStateOf<String?>(null) }

    if (selectedBillForQr != null) {
        UpiQrPaymentDialog(
            bill = selectedBillForQr!!,
            society = society,
            language = language,
            dues = dues,
            onDismiss = { selectedBillForQr = null },
            // The committee receipts the payment once it lands; the member
            // just closes the sheet.
            onPaymentRecorded = { selectedBillForQr = null }
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

    if (isPasscodeGenOpen) {
        GenerateVisitorPasscodeDialog(
            flatNumber = myFlat,
            language = language,
            onDismiss = { isPasscodeGenOpen = false },
            onSubmit = { name, phone, pur ->
                viewModel.generateVisitorPasscode(name, phone, myFlat, pur) { code ->
                    generatedPasscode = code
                }
                isPasscodeGenOpen = false
            }
        )
    }

    if (generatedPasscode != null) {
        AlertDialog(
            modifier = DialogWidth,
            properties = WideDialogProperties,
            onDismissRequest = { generatedPasscode = null },
            title = {
                Text(
                    text = if (language == AppLanguage.MARATHI) "अभ्यागत ६-अंकी पासकोड" else "Visitor Pre-Approved Passcode",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (language == AppLanguage.MARATHI) "हा पासकोड आपल्या पाहुण्याला / डिलिव्हरी बॉयला द्या:" else "Share this one-time passcode with your visitor:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = TealPrimaryContainer,
                        border = BorderStroke(1.5.dp, TealPrimary)
                    ) {
                        Text(
                            text = generatedPasscode ?: "------",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 4.sp,
                                color = TealPrimary
                            ),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { generatedPasscode = null },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text(AppStrings.get("close", language))
                }
            }
        )
    }

    if (isChangePinOpen) {
        ChangePinDialog(
            language = language,
            onDismiss = { isChangePinOpen = false },
            onSubmit = { newPin ->
                viewModel.updatePin(newPin)
                isChangePinOpen = false
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // A member who owns more than one unit picks which one this screen is
        // about; everything below follows the selection.
        if (myUnits.size > 1) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, TealPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("unit_selector_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = AppStrings.get("select_unit", language),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${myUnits.size} ${AppStrings.get("units_you_own", language)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SearchableDropdownField(
                            label = AppStrings.get("unit_number_label", language),
                            value = myFlat,
                            options = myUnits.map { it.flatNumber },
                            onValueChange = { viewModel.selectUnit(it) },
                            allowFreeText = false,
                            testTag = "portal_unit_dropdown"
                        )
                    }
                }
            }
        }

        /*
         * Member card.
         *
         * Deliberately compact - a smaller avatar, one line of identity, tight
         * padding - so the dues underneath it are on screen without scrolling.
         * It is a plain item of this list and holds no scroll of its own, so it
         * moves with the page rather than sliding under its own heading.
         */
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = TealPrimaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("portal_member_card")
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(TealPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Home,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${unit?.flatNumber ?: ""} • ${unit?.ownerName ?: "Member"}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = TealPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${unit?.flatType?.displayName ?: "2 BHK"} • Wing ${unit?.wing ?: "A"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }

                        IconButton(
                            onClick = { isChangePinOpen = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Change PIN",
                                tint = TealPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    /*
                     * Every figure here comes off the bills themselves, so this
                     * can never disagree with the month-wise table below it.
                     * Arrears, this month and any credit are each shown, then
                     * the one number the member actually has to pay.
                     */
                    if (dues.previousPending > 0.009) {
                        PortalDuesLine(
                            label = if (language == AppLanguage.MARATHI) "मागील थकबाकी" else "Previous pending",
                            amount = dues.previousPending
                        )
                    }
                    if (dues.currentPending > 0.009) {
                        PortalDuesLine(
                            label = if (language == AppLanguage.MARATHI) "या महिन्याचे बिल" else "Current bill",
                            amount = dues.currentPending
                        )
                    }
                    if (dues.excessCredit > 0.009) {
                        PortalDuesLine(
                            label = if (language == AppLanguage.MARATHI) "जमा (जास्तीची) रक्कम" else "Advance credit",
                            amount = -dues.excessCredit,
                            color = StatusPaid,
                            testTag = "portal_advance_credit"
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == AppLanguage.MARATHI) {
                                    "एकूण भरायची रक्कम"
                                } else {
                                    "Total Payable"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "₹%,.2f".format(dues.netPayable),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (dues.isSettled) StatusPaid else StatusOverdue
                                ),
                                modifier = Modifier.testTag("portal_outstanding_amount")
                            )
                            // Credit beyond everything owed is the member's, and
                            // comes off whatever is billed next.
                            if (dues.creditInHand > 0.009) {
                                Text(
                                    text = if (language == AppLanguage.MARATHI) {
                                        "₹%,.2f पुढील बिलात वजा होईल".format(dues.creditInHand)
                                    } else {
                                        "₹%,.2f credit carries to your next bill".format(dues.creditInHand)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusPaid,
                                    modifier = Modifier.testTag("portal_credit_in_hand")
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (dues.isSettled) StatusPaidBg else StatusOverdue.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (dues.isSettled) {
                                    AppStrings.get("status_paid", language)
                                } else {
                                    AppStrings.get("status_unpaid", language)
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (dues.isSettled) StatusPaid else StatusOverdue,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        if (financialYears.size > 1) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SlateOutlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("portal_financial_year_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SearchableDropdownField(
                            label = AppStrings.get("financial_year", language),
                            value = selectedYear,
                            options = financialYears,
                            onValueChange = { selectedYear = it },
                            allowFreeText = false,
                            testTag = "portal_financial_year_dropdown"
                        )
                        Text(
                            text = "${myBills.size} bill(s) in $selectedYear",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Month-wise billing for this unit: what was billed, what was paid,
        // what is still due for the month, and the running outstanding.
        if (myBills.isNotEmpty()) {
            item {
                UnitMonthlyBillingTable(bills = myBills, unitNumber = myFlat, language = language)
            }
        }

        /*
         * Resident quick actions.
         *
         * Raising a complaint lives on the Helpdesk, where the member can also
         * see what came of it. Offering it here as well put the form in one
         * place and the answer in another, so this screen keeps only the action
         * that has nowhere else to be.
         */
        item {
            Button(
                onClick = { isPasscodeGenOpen = true },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("gen_passcode_btn")
            ) {
                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (language == AppLanguage.MARATHI) {
                        "अभ्यागत पासकोड तयार करा"
                    } else {
                        "Visitor Passcode"
                    },
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        /*
         * Visitors the gate logged against this unit.
         *
         * The passcode is shown here and nowhere else - it belongs to the
         * member the visitor came to see, not to the whole society - and the
         * member answers for anyone still waiting at the gate.
         */
        if (myVisitors.isNotEmpty()) {
            item {
                Text(
                    text = AppStrings.get("gate_activity_log", language),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            items(myVisitors) { visitor ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SlateOutlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("portal_visitor_${visitor.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = visitor.visitorName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${visitor.flatNumber} • ${visitor.purpose.labelEn} • ${visitor.entryTime}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (visitor.vehicleNumber.isNotBlank()) {
                                    Text(
                                        text = "🚗 ${visitor.vehicleNumber}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            VisitorStatusBadge(status = visitor.status, language = language)
                        }

                        if (visitor.passcode.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = TealPrimaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = if (language == AppLanguage.MARATHI) "पासकोड" else "Passcode",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TealPrimary
                                    )
                                    Text(
                                        text = visitor.passcode,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 3.sp
                                        ),
                                        color = TealPrimary,
                                        modifier = Modifier.testTag("portal_visitor_passcode_${visitor.id}")
                                    )
                                }
                            }
                        }

                        // Only somebody still at the gate needs answering.
                        if (visitor.status == VisitorStatus.PRE_APPROVED ||
                            visitor.status == VisitorStatus.INSIDE
                        ) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.answerVisitor(visitor, isApproved = true) { _, message ->
                                            viewModel.showMessage(message)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("portal_approve_visitor_${visitor.id}")
                                ) {
                                    Text(
                                        text = if (language == AppLanguage.MARATHI) "परवानगी द्या" else "Approve",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.answerVisitor(visitor, isApproved = false) { _, message ->
                                            viewModel.showMessage(message)
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, StatusOverdue),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("portal_reject_visitor_${visitor.id}")
                                ) {
                                    Text(
                                        text = if (language == AppLanguage.MARATHI) "नकार" else "Deny",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = StatusOverdue
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // My Vehicles & Parking Slot
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SlateOutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (language == AppLanguage.MARATHI) {
                            "$myFlat - वाहने व पार्किंग जागा"
                        } else {
                            "$myFlat - Parking & Vehicles"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🅿️ Allocated Slot: ${unit?.parkingSlot?.ifBlank { "Not allotted" } ?: "Not allotted"}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TealPrimary
                    )
                    if (unit?.fourWheelerNumber?.isNotBlank() == true) {
                        Text(text = "🚗 4W Car: ${unit.fourWheelerNumber}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (unit?.twoWheelerNumber?.isNotBlank() == true) {
                        Text(text = "🛵 2W Scooter: ${unit.twoWheelerNumber}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // My Maintenance Bills
        item {
            Text(
                text = if (language == AppLanguage.MARATHI) {
                    "$myFlat - देखभाल बिले व पावत्या"
                } else {
                    "$myFlat - Maintenance Statements"
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        /*
         * Bill cards.
         *
         * A single row: the month and its status on the left, the amount and
         * the one action on the right. The old two-row card with a divider
         * through it was twice as tall for no more information, and a member
         * with a year of bills had to scroll past all of it.
         */
        items(myBills) { bill ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SlateOutlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("portal_bill_card_${bill.billMonth.replace(" ", "_")}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bill.billMonth,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            // Derived, not stored, so an edited bill never
                            // disagrees with the month-wise table above.
                            text = "₹%,.2f".format(bill.payableTotal) +
                                if (bill.pendingAmount > 0.009 && bill.amountPaid > 0.009) {
                                    " • ₹%,.2f due".format(bill.pendingAmount)
                                } else {
                                    " • Due ${bill.dueDate}"
                                },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(status = bill.derivedStatus, language = language)
                    Spacer(modifier = Modifier.width(8.dp))

                    /*
                     * A member pays, they do not record payments. Marking a
                     * bill settled is the committee's entry to make, so the
                     * member is offered the QR to pay with and, once the
                     * committee has receipted it, the receipt to keep.
                     */
                    if (bill.derivedStatus != PaymentStatus.PAID) {
                        OutlinedButton(
                            onClick = { selectedBillForQr = bill },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, TealPrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .heightIn(min = 32.dp)
                                .testTag("portal_upi_qr_btn_${bill.flatNumber}")
                        ) {
                            Icon(
                                Icons.Default.QrCode,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (language == AppLanguage.MARATHI) "QR" else "Pay",
                                style = MaterialTheme.typography.labelSmall,
                                color = TealPrimary
                            )
                        }
                    } else {
                        // Only a settled bill has a receipt behind it.
                        OutlinedButton(
                            onClick = { selectedBillForReceipt = bill },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, TealPrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .heightIn(min = 32.dp)
                                .testTag("portal_receipt_btn_${bill.flatNumber}")
                        ) {
                            Icon(
                                Icons.Default.Receipt,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (language == AppLanguage.MARATHI) "पावती" else "Receipt",
                                style = MaterialTheme.typography.labelSmall,
                                color = TealPrimary
                            )
                        }
                    }
                }
            }
        }

        // Every receipt this unit holds, to open, download or share.
        if (myReceipts.isNotEmpty()) {
            item {
                Text(
                    text = AppStrings.get("my_receipts", language),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.testTag("portal_receipts_heading")
                )
            }

            items(myReceipts) { receipt ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SlateOutlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("portal_receipt_row_${receipt.billMonth.replace(" ", "_")}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = receipt.receiptNumber.ifBlank { receipt.billMonth },
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${receipt.billMonth} • ₹%,.2f".format(receipt.amountPaid) +
                                    receipt.paidDate.takeIf { it.isNotBlank() }?.let { " • $it" }.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { selectedBillForReceipt = receipt },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, TealPrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .heightIn(min = 32.dp)
                                .testTag("portal_open_receipt_${receipt.billMonth.replace(" ", "_")}")
                        ) {
                            Icon(
                                Icons.Default.Receipt,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (language == AppLanguage.MARATHI) "उघडा" else "Open",
                                style = MaterialTheme.typography.labelSmall,
                                color = TealPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

/** One line of the member's dues breakdown: a label left, a rupee figure right. */
@Composable
private fun PortalDuesLine(
    label: String,
    amount: Double,
    color: Color = MaterialTheme.colorScheme.onSurface,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .then(if (testTag.isBlank()) Modifier else Modifier.testTag(testTag)),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(
            text = "₹%,.2f".format(amount),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = color
        )
    }
}

/**
 * One unit's billing, month by month.
 *
 *  - Total Bill    what was charged for that month, late fee included
 *  - Paid Amount   what has been collected against it
 *  - Due Amount    what is still owed for that month alone
 *  - Pending       everything still owed up to and including that month
 *
 * Due is the month on its own; Pending is the running arrear, which is the
 * number a member is actually chased for.
 */
@Composable
fun UnitMonthlyBillingTable(
    bills: List<MaintenanceBill>,
    unitNumber: String,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val ordered = remember(bills) { bills.sortedBy { portalMonthKey(it.billMonth) } }

    // Running arrear: each month carries forward everything left before it.
    val rows = remember(ordered) {
        var running = 0.0
        ordered.map { bill ->
            running += bill.pendingAmount
            bill to running
        }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SlateOutlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag("unit_monthly_billing_table")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = if (language == AppLanguage.MARATHI) {
                    "$unitNumber - महिनानिहाय बिल तपशील"
                } else {
                    "$unitNumber - Month-wise Billing"
                },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TealPrimaryContainer, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                PortalCell("Month", weight = 1.9f, bold = true)
                PortalCell("Total Bill", weight = 1.4f, bold = true, end = true)
                PortalCell("Paid", weight = 1.2f, bold = true, end = true)
                PortalCell("Due", weight = 1.2f, bold = true, end = true)
                PortalCell("Pending", weight = 1.3f, bold = true, end = true)
            }

            rows.forEach { (bill, running) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 7.dp)
                        .testTag("portal_bill_row_${bill.billMonth}")
                ) {
                    PortalCell(bill.billMonth, weight = 1.9f)
                    PortalCell("%,.0f".format(bill.payableTotal), weight = 1.4f, end = true)
                    PortalCell(
                        "%,.0f".format(bill.amountPaid),
                        weight = 1.2f,
                        end = true,
                        colour = StatusPaid
                    )
                    PortalCell(
                        "%,.0f".format(bill.pendingAmount),
                        weight = 1.2f,
                        end = true,
                        colour = if (bill.pendingAmount > 0.009) StatusOverdue else StatusPaid
                    )
                    PortalCell(
                        "%,.0f".format(running),
                        weight = 1.3f,
                        end = true,
                        bold = true,
                        colour = if (running > 0.009) StatusOverdue else StatusPaid
                    )
                }
                HorizontalDivider(color = SlateOutlineVariant)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            ) {
                PortalCell("Total", weight = 1.9f, bold = true)
                PortalCell("%,.0f".format(ordered.sumOf { it.payableTotal }), weight = 1.4f, bold = true, end = true)
                PortalCell("%,.0f".format(ordered.sumOf { it.amountPaid }), weight = 1.2f, bold = true, end = true)
                PortalCell("%,.0f".format(ordered.sumOf { it.pendingAmount }), weight = 1.2f, bold = true, end = true)
                PortalCell(
                    "%,.0f".format(rows.lastOrNull()?.second ?: 0.0),
                    weight = 1.3f,
                    bold = true,
                    end = true,
                    colour = if ((rows.lastOrNull()?.second ?: 0.0) > 0.009) StatusOverdue else StatusPaid
                )
            }
        }
    }
}

@Composable
private fun RowScope.PortalCell(
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

/** Chronological ordering for "August 2026" style month labels. */
private fun portalMonthKey(billMonth: String): Long = runCatching {
    java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.ENGLISH).parse(billMonth)?.time ?: 0L
}.getOrDefault(0L)

// Dialog: Generate Passcode for visitor
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateVisitorPasscodeDialog(
    flatNumber: String,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSubmit: (name: String, phone: String, pur: VisitorPurpose) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedPurpose by remember { mutableStateOf(VisitorPurpose.GUEST) }
    var isCatExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = DialogWidth,
        properties = WideDialogProperties,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (language == AppLanguage.MARATHI) "अभ्यागत पूर्व-मंजूरी पासकोड" else "Generate Visitor Passcode",
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
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Visitor Name (पाहुण्याचे नाव)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Visitor Mobile") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = isCatExpanded,
                    onExpandedChange = { isCatExpanded = !isCatExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedPurpose.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Purpose") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCatExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isCatExpanded,
                        onDismissRequest = { isCatExpanded = false }
                    ) {
                        VisitorPurpose.values().forEach { pur ->
                            DropdownMenuItem(
                                text = { Text(pur.name) },
                                onClick = {
                                    selectedPurpose = pur
                                    isCatExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSubmit(name, phone, selectedPurpose)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text(if (language == AppLanguage.MARATHI) "पासकोड तयार करा" else "Generate Code")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", language))
            }
        }
    )
}

// Dialog: Change PIN
@Composable
fun ChangePinDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSubmit: (newPin: String) -> Unit
) {
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        modifier = DialogWidth,
        properties = WideDialogProperties,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (language == AppLanguage.MARATHI) "४-अंकी पिन बदला" else "Change 4-Digit Security PIN",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it },
                    label = { Text("New PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it },
                    label = { Text("Confirm PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(text = errorMsg ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPin.length == 4 && newPin == confirmPin) {
                        onSubmit(newPin)
                    } else {
                        errorMsg = if (language == AppLanguage.MARATHI) "पिन ४ अंकांचा असावा आणि दोन्ही जुळले पाहिजेत!" else "PIN must be 4 digits and match!"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
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
