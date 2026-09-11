package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Resident
import com.example.data.entity.VisitorLog
import com.example.data.entity.VisitorPurpose
import com.example.data.entity.VisitorStatus
import com.example.data.localization.AppLanguage
import com.example.ui.components.DialogWidth
import com.example.ui.components.WideDialogProperties
import com.example.data.localization.AppStrings
import com.example.ui.components.VisitorStatusBadge
import com.example.ui.theme.MintSecondary
import com.example.ui.theme.SlateOutlineVariant
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryContainer
import com.example.viewmodel.SocietyViewModel

@Composable
fun GateSecurityScreen(
    viewModel: SocietyViewModel,
    currentUser: Resident?,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf(
        AppStrings.get("gate_terminal_title", language),
        if (language == AppLanguage.MARATHI) "अभ्यागत इतिहास" else "Visitor Log History"
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
                    modifier = Modifier.testTag("gate_subtab_$index")
                )
            }
        }

        when (selectedSubTab) {
            0 -> LiveGateTerminalView(viewModel = viewModel, language = language)
            1 -> VisitorHistoryView(viewModel = viewModel, language = language)
        }
    }
}

// 1. Live Gate Terminal View
@Composable
fun LiveGateTerminalView(
    viewModel: SocietyViewModel,
    language: AppLanguage
) {
    val activeVisitors by viewModel.activeVisitors.collectAsState()
    val residents by viewModel.residents.collectAsState()
    val context = LocalContext.current

    var isCheckInOpen by remember { mutableStateOf(false) }
    var isVerifyPasscodeOpen by remember { mutableStateOf(false) }

    if (isCheckInOpen) {
        CheckInVisitorDialog(
            language = language,
            onDismiss = { isCheckInOpen = false },
            onSubmit = { name, phone, flat, purpose, vehicle, passcode ->
                viewModel.checkInVisitor(name, phone, flat, purpose, vehicle, passcode)
                isCheckInOpen = false
            }
        )
    }

    if (isVerifyPasscodeOpen) {
        VerifyPasscodeDialog(
            viewModel = viewModel,
            language = language,
            onDismiss = { isVerifyPasscodeOpen = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick Security Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { isCheckInOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("gate_checkin_btn")
                ) {
                    Icon(Icons.Default.Person, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(AppStrings.get("check_in_visitor", language), style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = { isVerifyPasscodeOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MintSecondary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("gate_verify_passcode_btn")
                ) {
                    Icon(Icons.Default.Key, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(AppStrings.get("verify_passcode", language), style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Active Inside Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (language == AppLanguage.MARATHI) "सध्या आवारात असलेले अभ्यागत (${activeVisitors.size})" else "Active Inside Premises (${activeVisitors.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        if (activeVisitors.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SlateOutlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusPaid, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (language == AppLanguage.MARATHI) "सध्या आवारात कोणताही बाहेरील अभ्यागत नाही." else "No active external visitors inside premises.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(activeVisitors) { visitor ->
                val flatResident = residents.firstOrNull { it.flatNumber == visitor.flatNumber }
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
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(TealPrimaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getPurposeIcon(visitor.purpose),
                                        contentDescription = null,
                                        tint = TealPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = visitor.visitorName,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${visitor.purpose.name} • ${visitor.flatNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TealPrimary
                                    )
                                }
                            }

                            VisitorStatusBadge(status = visitor.status, language = language)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "In: ${visitor.entryTime} • ${visitor.phone}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (visitor.vehicleNumber.isNotBlank()) {
                                    Text(
                                        text = "Vehicle: ${visitor.vehicleNumber}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (flatResident != null && flatResident.contactNumber.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                                data = Uri.parse("tel:${flatResident.contactNumber}")
                                            }
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(TealPrimaryContainer, CircleShape)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = "Call Unit", tint = TealPrimary, modifier = Modifier.size(18.dp))
                                    }
                                }

                                Button(
                                    onClick = { viewModel.markVisitorExit(visitor) },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusOverdue),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("mark_exit_btn_${visitor.id}")
                                ) {
                                    Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(AppStrings.get("visitor_exited", language), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 2. Visitor History View
@Composable
fun VisitorHistoryView(
    viewModel: SocietyViewModel,
    language: AppLanguage
) {
    val visitors by viewModel.visitors.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filtered = visitors.filter {
        it.visitorName.contains(searchQuery, ignoreCase = true) ||
                it.flatNumber.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by name, flat or phone") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(filtered) { visitor ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SlateOutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${visitor.visitorName} • ${visitor.flatNumber}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${visitor.purpose.name} • In: ${visitor.entryTime} | Out: ${visitor.exitTime.ifBlank { "--" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    VisitorStatusBadge(status = visitor.status, language = language)
                }
            }
        }
    }
}

fun getPurposeIcon(purpose: VisitorPurpose): ImageVector {
    return when (purpose) {
        VisitorPurpose.DELIVERY -> Icons.Default.DeliveryDining
        VisitorPurpose.GUEST -> Icons.Default.Person
        VisitorPurpose.CAB -> Icons.Default.LocalTaxi
        VisitorPurpose.SERVICE -> Icons.Default.SupportAgent
        VisitorPurpose.OTHER -> Icons.Default.Security
    }
}

// Dialog: Check-In Visitor
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInVisitorDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSubmit: (name: String, phone: String, flat: String, purpose: VisitorPurpose, vehicle: String, passcode: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var flat by remember { mutableStateOf("") }
    var selectedPurpose by remember { mutableStateOf(VisitorPurpose.DELIVERY) }
    var vehicle by remember { mutableStateOf("") }
    var isPurposeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = DialogWidth,
        properties = WideDialogProperties,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = AppStrings.get("check_in_visitor", language),
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
                    label = { Text("Visitor Name (अभ्यागताचे नाव)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number (मोबाईल)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = flat,
                    onValueChange = { flat = it },
                    label = { Text("Visiting Unit (सदनिका क्र.)") },
                    placeholder = { Text("e.g. A-101") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = isPurposeExpanded,
                    onExpandedChange = { isPurposeExpanded = !isPurposeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedPurpose.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Purpose (उद्देश)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPurposeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isPurposeExpanded,
                        onDismissRequest = { isPurposeExpanded = false }
                    ) {
                        VisitorPurpose.values().forEach { pur ->
                            DropdownMenuItem(
                                text = { Text(pur.name) },
                                onClick = {
                                    selectedPurpose = pur
                                    isPurposeExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = vehicle,
                    onValueChange = { vehicle = it },
                    label = { Text("Vehicle Number (वाहनाचा क्र.)") },
                    placeholder = { Text("e.g. MH 12 AB 1234") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && flat.isNotBlank()) {
                        onSubmit(name, phone, flat, selectedPurpose, vehicle, "")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.testTag("submit_checkin_btn")
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

// Dialog: Verify Passcode
@Composable
fun VerifyPasscodeDialog(
    viewModel: SocietyViewModel,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    var codeInput by remember { mutableStateOf("") }
    var verifiedLog by remember { mutableStateOf<VisitorLog?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        modifier = DialogWidth,
        properties = WideDialogProperties,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = AppStrings.get("verify_passcode", language),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (language == AppLanguage.MARATHI) "रहिवाशाने दिलेला ६-अंकी पासकोड टाका:" else "Enter 6-digit passcode provided by resident:"
                )
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it },
                    label = { Text("6-Digit Code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(text = errorMsg ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                if (verifiedLog != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TealPrimaryContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "VALID PASSCODE ✅", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TealPrimary))
                            Text(text = "Visitor: ${verifiedLog?.visitorName}")
                            Text(text = "Visiting Flat: ${verifiedLog?.flatNumber}")
                            Text(text = "Purpose: ${verifiedLog?.purpose?.name}")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (verifiedLog != null) {
                        viewModel.checkInVisitor(
                            verifiedLog!!.visitorName,
                            verifiedLog!!.phone,
                            verifiedLog!!.flatNumber,
                            verifiedLog!!.purpose,
                            verifiedLog!!.vehicleNumber,
                            verifiedLog!!.passcode
                        )
                        onDismiss()
                    } else {
                        viewModel.verifyPasscode(codeInput) { log ->
                            if (log != null) {
                                verifiedLog = log
                                errorMsg = null
                            } else {
                                errorMsg = if (language == AppLanguage.MARATHI) "अवैध किंवा कालबाह्य पासकोड!" else "Invalid or expired passcode!"
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.testTag("submit_verify_passcode_btn")
            ) {
                Text(if (verifiedLog != null) AppStrings.get("check_in_visitor", language) else "Verify")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("close", language))
            }
        }
    )
}
