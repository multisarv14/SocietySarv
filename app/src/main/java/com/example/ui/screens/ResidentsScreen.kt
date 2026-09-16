package com.example.ui.screens

import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.ui.platform.LocalContext
import com.example.data.entity.RENT_AGREEMENT_MAX_BYTES
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.entity.FlatType
import com.example.data.entity.ParkingType
import com.example.data.entity.Resident
import com.example.data.entity.STAFF_ROLES
import com.example.data.entity.SocietyInfo
import com.example.data.entity.unitTypeNames
import com.example.data.entity.UserRole
import com.example.data.entity.parkingNumbers
import com.example.data.localization.AppLanguage
import com.example.ui.components.DialogWidth
import com.example.ui.components.WideDialogProperties
import com.example.data.localization.AppStrings
import com.example.ui.components.MemberAvatar
import com.example.ui.components.SearchableDropdownField
import com.example.ui.components.getRoleLabel
import com.example.ui.theme.SlateOutlineVariant
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryContainer
import com.example.viewmodel.SocietyViewModel

/**
 * Residents - the Chairman's own section, sitting right after Accounts.
 *
 * Members : the resident directory, and the only place a new resident is added
 * Parking : who holds which bay, and which bays are still free
 * Staff   : hiring, login credentials and access for society employees
 */
@Composable
fun ResidentsScreen(
    viewModel: SocietyViewModel,
    society: SocietyInfo?,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf(
        AppStrings.get("tab_members", language),
        AppStrings.get("tab_parking", language),
        AppStrings.get("tab_society_staff", language)
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
                    modifier = Modifier.testTag("residents_subtab_$index")
                )
            }
        }

        when (selectedSubTab) {
            0 -> MembersTab(viewModel = viewModel, society = society, language = language)
            1 -> ParkingTab(viewModel = viewModel, society = society, language = language)
            else -> SocietyStaffTab(viewModel = viewModel, language = language)
        }
    }
}

// ----------------------------------------------------------------- 1. members

@Composable
private fun MembersTab(
    viewModel: SocietyViewModel,
    society: SocietyInfo?,
    language: AppLanguage
) {
    val members by viewModel.members.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val canDelete = currentUser?.role == UserRole.CHAIRMAN
    var editing by remember { mutableStateOf<Resident?>(null) }
    var isAddOpen by remember { mutableStateOf(false) }
    var memberPendingDeletion by remember { mutableStateOf<Resident?>(null) }

    /*
     * Wing filter. The options come from the units themselves as well as the
     * society's configured wings, so a wing that only exists on residents -
     * because it was typed in before the wing list was set up - is still
     * reachable here.
     */
    val wingOptions = remember(members, society) { wingsInUse(members, society) }
    var wingFilter by remember { mutableStateOf(ALL_WINGS) }
    val visibleMembers = if (wingFilter == ALL_WINGS) {
        members
    } else {
        members.filter { it.wing.equals(wingFilter, ignoreCase = true) }
    }

    memberPendingDeletion?.let { member ->
        AlertDialog(
            modifier = DialogWidth,
            properties = WideDialogProperties,
            onDismissRequest = { memberPendingDeletion = null },
            title = {
                Text(
                    text = when {
                        member.ownerName.isBlank() ->
                            if (language == AppLanguage.MARATHI) "रिक्त सदनिका हटवायची?" else "Delete this vacant unit?"
                        language == AppLanguage.MARATHI -> "सभासद हटवायचा?"
                        else -> "Remove this member?"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = if (member.ownerName.isBlank()) {
                        // Nobody lives here, so the unit itself can go.
                        "Unit ${member.flatNumber} has no member against it and will be " +
                            "removed from the society altogether. Its bills and history go with it."
                    } else {
                        "${member.flatNumber} • ${member.ownerName}\n\n" +
                            "The unit itself stays in the directory - only the member's " +
                            "details and their login are removed." +
                            if (member.balanceDue > 0) {
                                "\n\nThey still owe ₹%,.2f.".format(member.balanceDue)
                            } else {
                                ""
                            }
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (member.ownerName.isBlank()) {
                            viewModel.deleteVacantUnit(member) { _, message -> viewModel.showMessage(message) }
                        } else {
                            viewModel.removeMemberFromUnit(member) { _, message -> viewModel.showMessage(message) }
                        }
                        memberPendingDeletion = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusOverdue),
                    modifier = Modifier.testTag("confirm_delete_member_btn")
                ) {
                    Text(if (language == AppLanguage.MARATHI) "हटवा" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberPendingDeletion = null }) {
                    Text(AppStrings.get("cancel", language))
                }
            }
        )
    }

    if (isAddOpen || editing != null) {
        AddResidentDialog(
            viewModel = viewModel,
            society = society,
            existing = editing,
            language = language,
            onDismiss = { isAddOpen = false; editing = null }
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = AppStrings.get("member_directory", language),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${visibleMembers.count { it.ownerName.isNotBlank() }} occupied • " +
                            "${visibleMembers.count { it.ownerName.isBlank() }} vacant",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { isAddOpen = true },
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

        if (wingOptions.size > 1) {
            item {
                SearchableDropdownField(
                    label = "Wing",
                    value = wingFilter,
                    options = wingOptions,
                    onValueChange = { wingFilter = it },
                    allowFreeText = false,
                    testTag = "members_wing_filter"
                )
            }
        }

        items(visibleMembers) { member ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SlateOutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    // The member's own photo, once they have uploaded one in
                    // their profile. A vacant unit has nobody to show.
                    if (member.ownerName.isNotBlank()) {
                        Surface(shape = RoundedCornerShape(22.dp), color = TealPrimary) {
                            MemberAvatar(
                                photoBase64 = member.photoBase64,
                                name = member.ownerName,
                                size = 40,
                                modifier = Modifier.testTag("member_photo_${member.flatNumber}")
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${member.flatNumber} • ${member.ownerName.ifBlank { "Vacant unit" }}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (member.ownerName.isBlank()) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        if (member.coApplicantName.isNotBlank()) {
                            Text(
                                text = "Co-applicant: ${member.coApplicantName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (member.tenantName.isNotBlank()) {
                            Text(
                                text = "Tenant: ${member.tenantName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TealPrimary
                            )
                        }
                        // Wing and unit type on the card - the two things that
                        // decide which tariff this unit is billed.
                        Text(
                            text = listOfNotNull(
                                member.wing.takeIf { it.isNotBlank() }?.let { "Wing $it" },
                                member.unitType.takeIf { it.isNotBlank() } ?: "Unit type not set"
                            ).joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (member.unitType.isBlank()) StatusOverdue else TealPrimary
                        )
                        if (member.ownerName.isNotBlank()) {
                            Text(
                                text = "${member.contactNumber} • ${member.familyMemberCount} family member(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (member.parkingType == ParkingType.NONE) {
                                    "No parking"
                                } else {
                                    "${member.parkingType.labelEn} • ${member.parkingSlot.ifBlank { "unallotted" }}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = { editing = member },
                        modifier = Modifier.testTag("edit_member_${member.flatNumber}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit member", tint = TealPrimary)
                    }
                    // Only the Chairman can turf a member out, and only after
                    // confirming it.
                    if (canDelete) {
                        IconButton(
                            onClick = { memberPendingDeletion = member },
                            modifier = Modifier.testTag("delete_member_${member.flatNumber}")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = if (member.ownerName.isBlank()) {
                                    "Delete vacant unit"
                                } else {
                                    "Remove member"
                                },
                                tint = StatusOverdue
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Add (or fill in) one resident against a unit.
 *
 * The unit comes from a dropdown of what the society actually has, and parking
 * is picked in two steps: the type first, then a bay number - and only bays
 * that are still free are offered.
 */
@Composable
fun AddResidentDialog(
    viewModel: SocietyViewModel,
    society: SocietyInfo?,
    existing: Resident?,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val members by viewModel.members.collectAsState()
    val unitNumbers = remember(members) { members.map { it.flatNumber }.sorted() }

    var unitNumber by remember { mutableStateOf(existing?.flatNumber.orEmpty().ifBlank { unitNumbers.firstOrNull().orEmpty() }) }
    var ownerName by remember { mutableStateOf(existing?.ownerName.orEmpty()) }
    var coApplicantName by remember { mutableStateOf(existing?.coApplicantName.orEmpty()) }
    var tenantName by remember { mutableStateOf(existing?.tenantName.orEmpty()) }
    var familyCount by remember { mutableStateOf((existing?.familyMemberCount ?: 1).toString()) }
    var phone by remember { mutableStateOf(existing?.contactNumber.orEmpty()) }
    var pin by remember { mutableStateOf(existing?.pin.orEmpty()) }
    var email by remember { mutableStateOf(existing?.email.orEmpty()) }
    var parkingType by remember { mutableStateOf(existing?.parkingType ?: ParkingType.NONE) }
    var parkingSlot by remember { mutableStateOf(existing?.parkingSlot.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    // The society's own unit types. Which one a unit is decides the monthly
    // tariff it is billed, so it is asked for here rather than guessed.
    val unitTypeOptions = remember(society) { society.unitTypeNames() }
    var unitType by remember(existing) { mutableStateOf(existing?.unitType.orEmpty()) }
    var wing by remember(existing) { mutableStateOf(existing?.wing.orEmpty()) }
    var tenantContact by remember(existing) { mutableStateOf(existing?.tenantContact.orEmpty()) }
    var rentAgreement by remember(existing) { mutableStateOf(existing?.rentAgreementBase64.orEmpty()) }
    // Every wing the society has, so a unit can be moved onto the right one
    // rather than a new spelling being invented for it.
    val wingChoices = remember(members, society) {
        wingsInUse(members, society).filterNot { it == ALL_WINGS }
    }

    // Picking a unit that is already filled in pulls its details up so the
    // sheet doubles as an edit form.
    LaunchedEffect(unitNumber) {
        if (existing != null) return@LaunchedEffect
        val match = members.firstOrNull { it.flatNumber.equals(unitNumber, ignoreCase = true) }
        if (match != null && match.ownerName.isNotBlank()) {
            ownerName = match.ownerName
            coApplicantName = match.coApplicantName
            tenantName = match.tenantName
            familyCount = match.familyMemberCount.toString()
            phone = match.contactNumber
            email = match.email
            pin = match.pin
            parkingType = match.parkingType
            parkingSlot = match.parkingSlot
            tenantContact = match.tenantContact
            rentAgreement = match.rentAgreementBase64
        }
        // A vacant unit still knows what type it was created as.
        if (match != null && match.unitType.isNotBlank()) unitType = match.unitType
        if (match != null && match.wing.isNotBlank()) wing = match.wing
    }

    val freeBays = remember(parkingType, unitNumber, members, society) {
        viewModel.availableParking(parkingType, keepFor = unitNumber)
    }

    AlertDialog(
        modifier = DialogWidth,
        properties = WideDialogProperties,
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
                SearchableDropdownField(
                    label = AppStrings.get("unit_number_label", language),
                    value = unitNumber,
                    options = unitNumbers,
                    onValueChange = { unitNumber = it; error = null },
                    allowFreeText = unitNumbers.isEmpty(),
                    testTag = "resident_unit_dropdown"
                )
                SearchableDropdownField(
                    label = "Wing",
                    value = wing,
                    options = wingChoices,
                    onValueChange = { wing = it },
                    // A society that has no wings yet can still type one in.
                    allowFreeText = wingChoices.isEmpty(),
                    testTag = "resident_wing_dropdown"
                )
                SearchableDropdownField(
                    label = "Unit Type" + if (unitType.isBlank()) " *" else "",
                    value = unitType,
                    options = unitTypeOptions,
                    onValueChange = { unitType = it; error = null },
                    // A society that never named its types can still type one in.
                    allowFreeText = unitTypeOptions.isEmpty(),
                    testTag = "resident_unit_type_dropdown"
                )
                if (unitType.isBlank()) {
                    Text(
                        text = "Pick the unit type - the monthly tariff is charged against it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Owner Full Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resident_owner_input")
                )
                // The second name on the unit's paperwork. Members can fill
                // this in themselves from their own profile when it is blank.
                OutlinedTextField(
                    value = coApplicantName,
                    onValueChange = { coApplicantName = it },
                    label = { Text("Co-Applicant Name (optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resident_co_applicant_input")
                )
                OutlinedTextField(
                    value = tenantName,
                    onValueChange = { tenantName = it },
                    label = { Text("Tenant Name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // A let flat has a tenant to reach and an agreement behind it,
                // so those are asked for only once a tenant is actually named.
                if (tenantName.isNotBlank()) {
                    OutlinedTextField(
                        value = tenantContact,
                        onValueChange = { tenantContact = it.filter { ch -> ch.isDigit() }.take(10) },
                        label = { Text("Tenant Mobile Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("resident_tenant_phone_input")
                    )
                    RentAgreementPicker(
                        agreementBase64 = rentAgreement,
                        onAgreement = { rentAgreement = it }
                    )
                }

                OutlinedTextField(
                    value = familyCount,
                    onValueChange = { familyCount = it.filter { ch -> ch.isDigit() }.take(2) },
                    label = { Text("Total Number of Family Members") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resident_family_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { ch -> ch.isDigit() }.take(10) },
                    label = { Text(AppStrings.get("mobile_number", language)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resident_phone_input")
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = { Text("Login PIN") },
                    supportingText = { Text("${pin.length}/4 digits") },
                    isError = pin.isNotEmpty() && pin.length != 4,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resident_pin_input")
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email ID") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = AppStrings.get("parking_details", language),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                SearchableDropdownField(
                    label = AppStrings.get("parking_type", language),
                    value = parkingType.labelEn,
                    options = ParkingType.entries.map { it.labelEn },
                    onValueChange = { picked ->
                        parkingType = ParkingType.entries.firstOrNull { it.labelEn == picked } ?: ParkingType.NONE
                        parkingSlot = ""
                    },
                    allowFreeText = false,
                    testTag = "parking_type_dropdown"
                )

                // Only shown once a real parking type is chosen.
                if (parkingType != ParkingType.NONE) {
                    /*
                     * The generated bay numbers (C-01, O-01 …) are a
                     * convenience, not a rule - a society whose bays are
                     * painted "B2-14" or "Stilt 3" has to be able to type that
                     * in rather than being forced onto a number it never used.
                     */
                    SearchableDropdownField(
                        label = AppStrings.get("parking_list", language),
                        value = parkingSlot,
                        options = freeBays,
                        onValueChange = { parkingSlot = it },
                        allowFreeText = true,
                        testTag = "parking_list_dropdown"
                    )
                    Text(
                        text = "${freeBays.size} free ${parkingType.labelEn.lowercase()} bay(s) suggested - " +
                            "or type your own bay number.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                error?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (unitNumber.isBlank() || ownerName.isBlank() || phone.isBlank()) {
                        error = "Unit number, owner name and mobile number are all required."
                        return@Button
                    }
                    if (pin.length != 4) {
                        error = "The login PIN must be exactly 4 digits."
                        return@Button
                    }
                    if (parkingType != ParkingType.NONE && parkingSlot.isBlank()) {
                        error = "Pick a parking number, or choose No Parking."
                        return@Button
                    }
                    val existingMember = members.firstOrNull { it.flatNumber.equals(unitNumber, ignoreCase = true) }
                    val request = { message: String ->
                        viewModel.showMessage(message)
                        onDismiss()
                    }
                    if (existingMember != null && existingMember.ownerName.isNotBlank()) {
                        // Already occupied: update in place rather than refuse.
                        viewModel.updateResident(
                            existingMember.copy(
                                ownerName = ownerName.trim(),
                                coApplicantName = coApplicantName.trim(),
                                tenantName = tenantName.trim(),
                                familyMemberCount = familyCount.toIntOrNull() ?: 1,
                                contactNumber = phone.trim(),
                                email = email.trim(),
                                pin = pin,
                                parkingType = parkingType,
                                parkingSlot = if (parkingType == ParkingType.NONE) "" else parkingSlot,
                                unitType = unitType.trim(),
                                wing = wing.trim(),
                                tenantContact = if (tenantName.isBlank()) "" else tenantContact.trim(),
                                rentAgreementBase64 = if (tenantName.isBlank()) "" else rentAgreement,
                                isOwnerLiving = tenantName.isBlank()
                            )
                        )
                        onDismiss()
                    } else {
                        viewModel.addResident(
                            flatNumber = unitNumber,
                            wing = wing.trim().ifBlank { existingMember?.wing.orEmpty() },
                            ownerName = ownerName,
                            coApplicantName = coApplicantName,
                            tenantName = tenantName,
                            tenantPhone = tenantContact,
                            rentAgreementBase64 = rentAgreement,
                            phone = phone,
                            email = email,
                            role = UserRole.RESIDENT,
                            familyCount = familyCount.toIntOrNull() ?: 1,
                            twoWheelers = 0,
                            fourWheelers = 0,
                            twoWheelerNo = "",
                            fourWheelerNo = "",
                            parkingSlot = parkingSlot,
                            parkingType = parkingType,
                            unitType = unitType,
                            flatType = existingMember?.flatType ?: FlatType.TWO_BHK,
                            sqft = existingMember?.squareFeet ?: 0,
                            pin = pin
                        ) { ok, message ->
                            if (ok) request(message) else error = message
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.testTag("resident_submit_btn")
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

// ----------------------------------------------------------------- 2. parking

/** Every bay in the society: its number, type, the unit holding it and who that is. */
@Composable
private fun ParkingTab(
    viewModel: SocietyViewModel,
    society: SocietyInfo?,
    language: AppLanguage
) {
    val members by viewModel.members.collectAsState()

    // Bay -> holder, built once from the directory.
    val allottedBy = remember(members) {
        members.filter { it.parkingSlot.isNotBlank() }
            .associateBy { it.parkingSlot.trim().uppercase() }
    }
    val allBays = remember(society) {
        (society?.parkingNumbers(ParkingType.COVERED).orEmpty().map { it to ParkingType.COVERED }) +
            (society?.parkingNumbers(ParkingType.OPEN).orEmpty().map { it to ParkingType.OPEN })
    }
    val allottedCount = allBays.count { (bay, _) -> allottedBy.containsKey(bay.uppercase()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = AppStrings.get("tab_parking", language),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "$allottedCount of ${allBays.size} bays allotted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Column headers for the list below.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TealPrimaryContainer, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text("Parking No.", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1.1f))
                Text("Type", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1.1f))
                Text("Unit", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                Text("Owner", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1.6f))
            }
        }

        if (allBays.isEmpty()) {
            item {
                Text(
                    text = "No parking bays configured yet. Set the covered / open counts in the Society Profile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(allBays) { (bay, type) ->
            val holder = allottedBy[bay.uppercase()]
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (holder == null) SlateSurfaceVariant else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, SlateOutlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("parking_row_$bay")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.1f)) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = if (holder == null) MaterialTheme.colorScheme.onSurfaceVariant else TealPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = bay,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Text(
                        text = if (language == AppLanguage.MARATHI) type.labelMr else type.labelEn,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1.1f)
                    )
                    Text(
                        text = holder?.flatNumber ?: "-",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = holder?.ownerName ?: "Available",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (holder == null) StatusPaid else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1.6f)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------- 3. society staff

@Composable
private fun SocietyStaffTab(
    viewModel: SocietyViewModel,
    language: AppLanguage
) {
    val staff by viewModel.staff.collectAsState()
    var editing by remember { mutableStateOf<Resident?>(null) }
    var isAddOpen by remember { mutableStateOf(false) }

    if (isAddOpen || editing != null) {
        StaffDialog(
            viewModel = viewModel,
            existing = editing,
            language = language,
            onDismiss = { isAddOpen = false; editing = null }
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = AppStrings.get("tab_society_staff", language),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Staff sign in with their staff code and PIN.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { isAddOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_staff_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStrings.get("add_staff", language), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        items(staff) { member ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SlateOutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Surface(shape = RoundedCornerShape(24.dp), color = TealPrimary) {
                        MemberAvatar(photoBase64 = member.photoBase64, name = member.ownerName, size = 44)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = member.ownerName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${getRoleLabel(member.role, language)}" +
                                if (member.staffDesignation.isNotBlank()) " • ${member.staffDesignation}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = TealPrimary
                        )
                        Text(
                            text = "Login: ${member.flatNumber} • ${member.contactNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (member.isActive) "Active" else "Disabled",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (member.isActive) StatusPaid else StatusOverdue
                    )
                    IconButton(
                        onClick = { editing = member },
                        modifier = Modifier.testTag("edit_staff_${member.flatNumber}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit staff", tint = TealPrimary)
                    }
                }
            }
        }
    }
}

/** Create or update one staff member, including the credentials they sign in with. */
@Composable
fun StaffDialog(
    viewModel: SocietyViewModel,
    existing: Resident?,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    var staffCode by remember { mutableStateOf(existing?.flatNumber.orEmpty()) }
    var name by remember { mutableStateOf(existing?.ownerName.orEmpty()) }
    var role by remember { mutableStateOf(existing?.role ?: UserRole.GUARD) }
    var designation by remember { mutableStateOf(existing?.staffDesignation.orEmpty()) }
    var phone by remember { mutableStateOf(existing?.contactNumber.orEmpty()) }
    var email by remember { mutableStateOf(existing?.email.orEmpty()) }
    var pin by remember { mutableStateOf(existing?.pin.orEmpty()) }
    var isActive by remember { mutableStateOf(existing?.isActive ?: true) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        modifier = DialogWidth,
        properties = WideDialogProperties,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existing == null) {
                    AppStrings.get("add_staff", language)
                } else {
                    AppStrings.get("tab_society_staff", language)
                },
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
                    value = staffCode,
                    onValueChange = { staffCode = it.uppercase().filter { ch -> ch.isLetterOrDigit() || ch == '-' } },
                    label = { Text("Staff Login Code") },
                    placeholder = { Text("e.g. GATE-01") },
                    enabled = existing == null,
                    supportingText = { Text("This is what the staff member signs in with.") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("staff_code_input")
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("staff_name_input")
                )
                SearchableDropdownField(
                    label = "Staff Role",
                    value = getRoleLabel(role, language),
                    options = STAFF_ROLES.map { getRoleLabel(it, language) },
                    onValueChange = { picked ->
                        role = STAFF_ROLES.firstOrNull { getRoleLabel(it, language) == picked } ?: UserRole.GUARD
                    },
                    allowFreeText = false,
                    testTag = "staff_role_dropdown"
                )
                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text("Designation (optional)") },
                    placeholder = { Text("e.g. Night Shift Guard") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { ch -> ch.isDigit() }.take(10) },
                    label = { Text(AppStrings.get("mobile_number", language)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("staff_phone_input")
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email ID (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = { Text("Login PIN") },
                    supportingText = { Text("${pin.length}/4 digits") },
                    isError = pin.isNotEmpty() && pin.length != 4,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("staff_pin_input")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Login enabled", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Turn off to stop this staff member signing in.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        modifier = Modifier.testTag("staff_active_switch")
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Access for this role",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = accessSummary(role),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    viewModel.saveStaff(
                        staffCode = staffCode,
                        name = name,
                        role = role,
                        designation = designation,
                        phone = phone,
                        email = email,
                        pin = pin,
                        isActive = isActive,
                        existing = existing
                    ) { ok, message ->
                        if (ok) {
                            viewModel.showMessage(message)
                            onDismiss()
                        } else {
                            error = message
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.testTag("staff_submit_btn")
            ) {
                Text(AppStrings.get("submit", language))
            }
        },
        dismissButton = {
            Row {
                if (existing != null) {
                    TextButton(
                        onClick = {
                            viewModel.deleteStaff(existing) { _, message -> viewModel.showMessage(message) }
                            onDismiss()
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = StatusOverdue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppStrings.get("delete", language), color = StatusOverdue)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(AppStrings.get("cancel", language))
                }
            }
        }
    )
}

/** Plain-English version of what [destinationsFor] grants a staff role. */
/** The "show everything" row at the top of a wing picker. */
internal const val ALL_WINGS = "All Wings"

/**
 * Every wing the society actually has: the ones it configured, plus any that
 * only exist on a unit because they were typed in before the wing list did.
 * Without the second half a stray wing is invisible and cannot be cleaned up.
 */
internal fun wingsInUse(members: List<Resident>, society: SocietyInfo?): List<String> {
    val configured = society?.wingNames.orEmpty()
    val onUnits = members.map { it.wing }
    val known = (configured + onUnits)
        .map { it.trim() }
        .filter { it.isNotBlank() && !it.equals("STAFF", ignoreCase = true) }
        .distinctBy { it.lowercase() }
        .sorted()
    return listOf(ALL_WINGS) + known
}

/**
 * Picks a rent agreement PDF and keeps it as a Base64 blob on the unit.
 *
 * Firestore caps a document at 1 MiB and Base64 costs a third on top, so a
 * large scan is refused here rather than failing on write once the committee
 * believes it is filed.
 */
@Composable
fun RentAgreementPicker(
    agreementBase64: String,
    onAgreement: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val read = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("could not read the file")
        }
        read.onSuccess { bytes ->
            if (bytes.size > RENT_AGREEMENT_MAX_BYTES) {
                error = "That file is %.1f MB. The limit is %.1f MB - upload a smaller scan.".format(
                    bytes.size / (1024.0 * 1024.0),
                    RENT_AGREEMENT_MAX_BYTES / (1024.0 * 1024.0)
                )
            } else {
                onAgreement(Base64.encodeToString(bytes, Base64.NO_WRAP))
                error = null
            }
        }.onFailure { error = "Could not read that file. Pick a PDF." }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { picker.launch("application/pdf") },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, TealPrimary),
                modifier = Modifier.testTag("rent_agreement_picker_btn")
            ) {
                Icon(Icons.Default.AttachFile, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (agreementBase64.isBlank()) "Upload Rent Agreement" else "Replace Agreement",
                    color = TealPrimary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            if (agreementBase64.isNotBlank()) {
                TextButton(onClick = { onAgreement(""); error = null }) {
                    Text("Remove", color = StatusOverdue, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        if (agreementBase64.isNotBlank()) {
            Text(
                text = "Agreement on file (%.0f KB)".format(agreementBase64.length * 3 / 4 / 1024.0),
                style = MaterialTheme.typography.bodySmall,
                color = StatusPaid
            )
        }
        error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("rent_agreement_error")
            )
        }
    }
}

private fun accessSummary(role: UserRole): String = when (role) {
    UserRole.GUARD -> "Security (gate terminal) and Community only."
    UserRole.ACCOUNTANT -> "Home, Accounts, Bills and Community."
    UserRole.MANAGER -> "Home, Accounts, Bills and Community."
    UserRole.HOUSEKEEPING, UserRole.MAINTENANCE_STAFF -> "Community only - notices and the complaints they attend."
    else -> "Community only."
}
