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
import androidx.compose.material.icons.filled.SupportAgent
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ComplaintCategory
import com.example.data.entity.MaintenanceBill
import com.example.data.entity.PaymentMode
import com.example.data.entity.PaymentStatus
import com.example.data.entity.Resident
import com.example.data.entity.SocietyInfo
import com.example.data.entity.VisitorPurpose
import com.example.data.localization.AppLanguage
import com.example.data.localization.AppStrings
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
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

    val myFlat = currentUser?.flatNumber ?: ""
    val myBills = bills.filter { it.flatNumber == myFlat }
    val myComplaints = complaints.filter { it.flatNumber == myFlat }

    var selectedBillForReceipt by remember { mutableStateOf<MaintenanceBill?>(null) }
    var selectedBillForPayment by remember { mutableStateOf<MaintenanceBill?>(null) }
    var selectedBillForQr by remember { mutableStateOf<MaintenanceBill?>(null) }
    var isPasscodeGenOpen by remember { mutableStateOf(false) }
    var isFileComplaintOpen by remember { mutableStateOf(false) }
    var isChangePinOpen by remember { mutableStateOf(false) }
    var generatedPasscode by remember { mutableStateOf<String?>(null) }

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

    if (isFileComplaintOpen) {
        FileComplaintDialog(
            currentUser = currentUser,
            language = language,
            onDismiss = { isFileComplaintOpen = false },
            onSubmit = { cat, desc, flat, name ->
                viewModel.fileComplaint(cat, desc, flat, name)
                isFileComplaintOpen = false
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
        // Resident Profile & Dues Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TealPrimaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(TealPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Home, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${currentUser?.flatNumber ?: ""} • ${currentUser?.ownerName ?: "Member"}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TealPrimary
                                )
                                Text(
                                    text = "${currentUser?.flatType?.displayName ?: "2 BHK"} • Wing ${currentUser?.wing ?: "A"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = { isChangePinOpen = true }) {
                            Icon(Icons.Default.Lock, contentDescription = "Change PIN", tint = TealPrimary)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (language == AppLanguage.MARATHI) "थकीत देखभाल शुल्क (Total Due)" else "Outstanding Due Balance",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "₹%,.2f".format(currentUser?.balanceDue ?: 0.0),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if ((currentUser?.balanceDue ?: 0.0) > 0) StatusOverdue else StatusPaid
                                )
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if ((currentUser?.balanceDue ?: 0.0) > 0) StatusOverdue.copy(alpha = 0.15f) else StatusPaidBg
                        ) {
                            Text(
                                text = if ((currentUser?.balanceDue ?: 0.0) > 0) AppStrings.get("status_unpaid", language) else AppStrings.get("status_paid", language),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if ((currentUser?.balanceDue ?: 0.0) > 0) StatusOverdue else StatusPaid,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Resident Quick Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { isPasscodeGenOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("gen_passcode_btn")
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (language == AppLanguage.MARATHI) "अभ्यागत पासकोड" else "Passcode", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = { isFileComplaintOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusOverdue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("portal_file_complaint_btn")
                ) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(AppStrings.get("file_complaint", language), style = MaterialTheme.typography.labelMedium)
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
                        text = if (language == AppLanguage.MARATHI) "माझी वाहने व पार्किंग जागा" else "My Parking & Vehicles",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🅿️ Allocated Slot: ${currentUser?.parkingSlot?.ifBlank { "P-12" }}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TealPrimary
                    )
                    if (currentUser?.fourWheelerNumber?.isNotBlank() == true) {
                        Text(text = "🚗 4W Car: ${currentUser.fourWheelerNumber}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (currentUser?.twoWheelerNumber?.isNotBlank() == true) {
                        Text(text = "🛵 2W Scooter: ${currentUser.twoWheelerNumber}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // My Maintenance Bills
        item {
            Text(
                text = if (language == AppLanguage.MARATHI) "माझी देखभाल बिले व पावत्या" else "My Maintenance Statements",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        items(myBills) { bill ->
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
                        Column {
                            Text(text = bill.billMonth, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text(text = "Due: ${bill.dueDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "₹%,.2f".format(bill.totalAmount), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            StatusBadge(status = bill.status, language = language)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (bill.status != PaymentStatus.PAID) {
                            OutlinedButton(
                                onClick = { selectedBillForQr = bill },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, TealPrimary),
                                modifier = Modifier.testTag("portal_upi_qr_btn_${bill.flatNumber}")
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (language == AppLanguage.MARATHI) "QR कोड" else "QR Pay", style = MaterialTheme.typography.labelSmall, color = TealPrimary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { selectedBillForPayment = bill },
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(AppStrings.get("record_payment", language), style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { selectedBillForReceipt = bill },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, TealPrimary)
                            ) {
                                Icon(Icons.Default.Receipt, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (language == AppLanguage.MARATHI) "पावती पहा" else "View Receipt", style = MaterialTheme.typography.labelSmall, color = TealPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

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
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (language == AppLanguage.MARATHI) "अभ्यागत पूर्व-मंजूरी पासकोड" else "Generate Visitor Passcode",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
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
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (language == AppLanguage.MARATHI) "४-अंकी पिन बदला" else "Change 4-Digit Security PIN",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
