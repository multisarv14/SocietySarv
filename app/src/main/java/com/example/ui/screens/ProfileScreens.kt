package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.entity.Resident
import com.example.data.entity.SocietyInfo
import com.example.data.entity.UnitTypeConfig
import com.example.data.entity.UserRole
import com.example.data.entity.WingPlan
import com.example.data.localization.AppLanguage
import com.example.ui.components.DialogWidth
import com.example.ui.components.WideDialogProperties
import com.example.data.localization.AppStrings
import com.example.data.localization.LocationData
import com.example.ui.components.MemberAvatar
import com.example.ui.components.SocietyLogoMark
import com.example.ui.components.getRoleLabel
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.TealPrimary
import com.example.util.ImageBlob
import com.example.viewmodel.SocietyViewModel

/**
 * Society Profile, opened by tapping the society logo in the top bar.
 *
 * Everyone can read it; only the Chairman gets the Edit button, and the
 * ViewModel refuses the write for anyone else regardless.
 */
@Composable
fun SocietyProfileDialog(
    society: SocietyInfo?,
    currentUser: Resident?,
    language: AppLanguage,
    onDismiss: () -> Unit,
    /**
     * Opens the editing wizard. It is hosted by the caller as a full-screen
     * screen rather than nested in a dialog window: a dialog's own window
     * reports its insets unreliably, which left the Cancel / Save row laid out
     * under the gesture bar and clipped off the bottom of the display.
     */
    onEdit: () -> Unit
) {
    if (society == null) {
        onDismiss()
        return
    }
    val canEdit = currentUser?.role == UserRole.CHAIRMAN

    AlertDialog(
        modifier = DialogWidth,
        properties = WideDialogProperties,
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = AppStrings.get("society_profile", language),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                if (canEdit) {
                    TextButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_society_profile_btn")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = TealPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppStrings.get("edit_profile", language), color = TealPrimary)
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SocietyLogoMark(logoBase64 = society.logoBase64, size = 56)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = society.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Login Code: ${society.societyCode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TealPrimary
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                ProfileRow("Registration No.", society.registrationNumber)
                ProfileRow(
                    "Location",
                    listOf(society.city, society.state, society.country)
                        .filter { it.isNotBlank() }.joinToString(", ")
                )
                ProfileRow("Address", society.address)
                ProfileRow("PIN Code", society.pinCode)
                ProfileRow("Contact", society.contactNumber)
                ProfileRow("Email", society.email)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ProfileRow("Wings", society.wingNames.joinToString(", ").ifBlank { society.totalWings.toString() })
                ProfileRow("Total Units", society.totalFlats.toString())
                society.wingPlans.forEach { plan ->
                    ProfileRow(
                        "Wing ${plan.name}",
                        "${plan.totalFloors} floors • ${plan.totalUnits} units" +
                            plan.unitCounts
                                .joinToString(", ", prefix = " (", postfix = ")") {
                                    "${it.unitType} × ${it.quantity}"
                                }
                                .takeIf { plan.unitCounts.isNotEmpty() }.orEmpty()
                    )
                }
                ProfileRow(
                    "Unit Types",
                    society.unitTypes.joinToString(", ") { type ->
                        if (type.monthlyTariff > 0) "${type.name} (₹${type.monthlyTariff.toInt()})" else type.name
                    }
                )
                ProfileRow("Billing Starts", society.billingStartMonth.ifBlank { "Not set" })
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ProfileRow("Bank", society.bankAccountName)
                ProfileRow("Account No.", society.bankAccountNumber)
                ProfileRow("IFSC", society.ifscCode)

                if (!canEdit) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = AppStrings.get("chairman_only_edit", language),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("chairman_only_note")
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

/**
 * The society's registration wizard, reopened for editing.
 *
 * Same shape as registration - one step at a time, a progress bar, Back and
 * Save & Continue - and the same fields: society details with the billing start
 * month, wings, floor inventory with the ground floor, unit types and their
 * tariffs, and parking bays. Each step is blocked until it is valid, exactly as
 * registration blocks it, so a half-filled society can never be saved.
 *
 * Office Bearers is the one registration step missing: the chairman already
 * exists and is edited under Residents > Members. The login code and the
 * treasury balances are shown but not editable - members sign in against the
 * code, and the ledger owns the balances.
 */
@Composable
fun SocietyProfileEditorScreen(
    viewModel: SocietyViewModel,
    society: SocietyInfo,
    language: AppLanguage,
    onClose: () -> Unit
) {
    val members by viewModel.members.collectAsState()

    SocietyRegistrationEditor(
        society = society,
        members = members,
        language = language,
        onRenameWingOnUnits = { from, to ->
            viewModel.renameWingOnUnits(from, to) { _, message -> viewModel.showMessage(message) }
        },
        onDeleteWingWithUnits = { wing ->
            viewModel.deleteWingWithUnits(wing) { _, message -> viewModel.showMessage(message) }
        },
        onCancel = onClose,
        onSave = { updated, onError ->
            viewModel.saveSocietyConfiguration(updated) { ok, message ->
                if (ok) {
                    viewModel.showMessage(message)
                    onClose()
                } else {
                    onError(message)
                }
            }
        }
    )
}

@Composable
private fun SocietyRegistrationEditor(
    society: SocietyInfo,
    members: List<Resident>,
    language: AppLanguage,
    onRenameWingOnUnits: (from: String, to: String) -> Unit,
    onDeleteWingWithUnits: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: (SocietyInfo, onError: (String) -> Unit) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }

    // 1. Society Details
    var name by remember(society) { mutableStateOf(society.name) }
    var regNumber by remember(society) { mutableStateOf(society.registrationNumber) }
    var country by remember(society) { mutableStateOf(society.country.ifBlank { LocationData.DEFAULT_COUNTRY }) }
    var state by remember(society) { mutableStateOf(society.state) }
    var city by remember(society) { mutableStateOf(society.city) }
    var address by remember(society) { mutableStateOf(society.address) }
    var pinCode by remember(society) { mutableStateOf(society.pinCode) }
    var phone by remember(society) { mutableStateOf(society.contactNumber) }
    var email by remember(society) { mutableStateOf(society.email) }
    var logoBase64 by remember(society) { mutableStateOf(society.logoBase64) }
    var bankName by remember(society) { mutableStateOf(society.bankAccountName) }
    var bankAccount by remember(society) { mutableStateOf(society.bankAccountNumber) }
    var ifsc by remember(society) { mutableStateOf(society.ifscCode) }
    var upiId by remember(society) { mutableStateOf(society.upiId) }
    var billingStartMonth by remember(society) {
        mutableStateOf(society.billingStartMonth.ifBlank { currentMonthLabel() })
    }

    // 2. Unit Details
    var totalWings by remember(society) {
        mutableStateOf(
            society.wingNames.size.takeIf { it > 0 }?.toString()
                ?: society.totalWings.takeIf { it > 0 }?.toString().orEmpty()
        )
    }
    var wingNamesRaw by remember(society) { mutableStateOf(society.wingNames.joinToString(", ")) }

    // 3. Floor Inventory
    var wingPlans by remember(society) { mutableStateOf(society.wingPlans) }
    // Unit types follow the wings they were entered against; their tariffs are
    // set under Maintenance > Tariff, not here.
    val unitTypes = remember(wingPlans) { unitTypesOf(wingPlans) }

    // 4. Parking & Review
    // Opening balances: what the society held before it started keeping books
    // here. Everything after that is worked out from the bills and vouchers.
    var openingBank by remember(society) {
        mutableStateOf(society.currentBalanceBank.takeIf { it != 0.0 }?.toInt()?.toString().orEmpty())
    }
    var openingCash by remember(society) {
        mutableStateOf(society.currentBalanceCash.takeIf { it != 0.0 }?.toInt()?.toString().orEmpty())
    }
    var coveredParking by remember(society) {
        mutableStateOf(society.coveredParkingCount.takeIf { it > 0 }?.toString().orEmpty())
    }
    var openParking by remember(society) {
        mutableStateOf(society.openParkingCount.takeIf { it > 0 }?.toString().orEmpty())
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val wingNames = remember(wingNamesRaw) { parseWingNames(wingNamesRaw) }
    // Wings the units carry that the society never listed - invisible until
    // now, which is why they could not be tidied up.
    // Stray wings the chairman has dismissed in this sitting. They carry no
    // units, so there is nothing to write - they just stop being listed.
    var forgottenWings by remember(society) { mutableStateOf(emptySet<String>()) }
    val strayWings = remember(members, wingNames) {
        members.map { it.wing.trim() }
            .filter { it.isNotBlank() && !it.equals("STAFF", ignoreCase = true) }
            .distinctBy { it.lowercase() }
            .filterNot { stray -> wingNames.any { it.equals(stray, ignoreCase = true) } }
            .sorted()
    }
    val plannedUnits = remember(wingPlans) { wingPlans.sumOf { it.totalUnits } }

    // The same gate registration uses, step for step.
    val stepError: String? = when (step) {
        1 -> when {
            name.isBlank() -> "Enter the society name."
            pinCode.isNotBlank() && pinCode.length != 6 -> "PIN code must be exactly 6 digits."
            email.isNotBlank() && !email.contains("@") -> "Enter a valid society email id."
            else -> null
        }
        /*
         * Editing is not registering.
         *
         * A society registered before floor inventory existed has no wing names
         * and no plans stored, and it must still be able to correct a phone
         * number without being forced to re-enter its whole structure. So these
         * two steps only object to a half-filled answer - never to an empty
         * one, which simply means "leave this as it is".
         */
        2 -> when {
            // Nothing typed and nothing ever stored: this society predates wing
            // names, and correcting a phone number must not demand them. The
            // wing count is prefilled from the society, so it cannot be the
            // thing that decides whether the step was answered.
            wingNamesRaw.isBlank() && society.wingNames.isEmpty() -> null
            (totalWings.toIntOrNull() ?: 0) <= 0 -> "Enter how many wings the society has."
            wingNames.isEmpty() -> "Enter the wing names, separated by commas."
            wingNames.size != (totalWings.toIntOrNull() ?: 0) ->
                "You entered ${wingNames.size} wing name(s) for ${totalWings.toIntOrNull() ?: 0} wing(s)."
            else -> null
        }
        3 -> when {
            unitTypes.isNotEmpty() && unitTypes.none { it.name.isNotBlank() } ->
                "Give every unit type a name, or remove the blank ones."
            else -> null
        }
        else -> null
    }

    // Laid out exactly like the registration wizard - a plain full-screen
    // surface, not a dialog window - so the Cancel / Save row sits above the
    // gesture bar instead of being clipped off the bottom of the display.
    BackHandler(onBack = onCancel)

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(16.dp)
        ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { if (step > 1) step-- else onCancel() },
                        modifier = Modifier.testTag("society_editor_back_btn")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = AppStrings.get("society_profile", language),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$step / $EDITOR_STEPS",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = TealPrimary
                    )
                }

                LinearProgressIndicator(
                    progress = { step / EDITOR_STEPS.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    color = TealPrimary
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                        .testTag("society_editor_step_$step")
                ) {
                    when (step) {
                        1 -> SocietyDetailsStep(
                            language = language,
                            societyName = name, onSocietyName = { name = it },
                            regNumber = regNumber, onRegNumber = { regNumber = it },
                            societyCode = society.societyCode, onSocietyCode = {},
                            country = country,
                            onCountry = { country = it; state = ""; city = "" },
                            state = state, onState = { state = it; city = "" },
                            city = city, onCity = { city = it },
                            address = address, onAddress = { address = it },
                            pinCode = pinCode,
                            onPinCode = { pinCode = it.filter { ch -> ch.isDigit() }.take(6) },
                            phone = phone, onPhone = { phone = it },
                            email = email, onEmail = { email = it },
                            logoBase64 = logoBase64, onLogo = { logoBase64 = it },
                            bankName = bankName, onBankName = { bankName = it },
                            bankAccount = bankAccount, onBankAccount = { bankAccount = it },
                            ifscCode = ifsc, onIfsc = { ifsc = it },
                            upiId = upiId, onUpiId = { upiId = it },
                            initialBankBalance = "", onInitialBalance = {},
                            billingStartMonth = billingStartMonth,
                            onBillingStartMonth = { billingStartMonth = it },
                            codeEditable = false,
                            showOpeningBalance = false
                        )

                        2 -> UnitDetailsStep(
                            language = language,
                            totalWings = totalWings,
                            onTotalWings = { totalWings = it },
                            wingNamesRaw = wingNamesRaw,
                            onWingNamesRaw = { wingNamesRaw = it },
                            parsedWingNames = wingNames,
                            strayWings = strayWings - forgottenWings,
                            unitCountFor = { wing ->
                                members.count { it.wing.equals(wing, ignoreCase = true) }
                            },
                            onRenameUnits = onRenameWingOnUnits,
                            onDeleteStrayWing = { forgottenWings = forgottenWings + it },
                            onDeleteWingWithUnits = onDeleteWingWithUnits
                        )

                        3 -> FloorInventoryStep(
                            language = language,
                            wingNames = wingNames,
                            wingPlans = wingPlans,
                            onWingPlans = { wingPlans = it }
                        )

                        4 -> SocietyParkingReviewStep(
                            society = society,
                            wingNames = wingNames,
                            wingPlans = wingPlans,
                            unitTypes = unitTypes,
                            plannedUnits = plannedUnits,
                            billingStartMonth = billingStartMonth,
                            coveredParking = coveredParking,
                            onCoveredParking = { coveredParking = it },
                            openParking = openParking,
                            onOpenParking = { openParking = it },
                            openingBank = openingBank,
                            onOpeningBank = { openingBank = it },
                            openingCash = openingCash,
                            onOpeningCash = { openingCash = it }
                        )
                    }
                }

                val blocker = errorMessage ?: stepError
                if (blocker != null) {
                    Text(
                        text = blocker,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                            .testTag("society_editor_error")
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag("society_editor_cancel_btn")
                    ) {
                        Text(AppStrings.get("cancel", language))
                    }

                    Button(
                        enabled = stepError == null,
                        onClick = {
                            errorMessage = null
                            if (step < EDITOR_STEPS) {
                                step++
                                return@Button
                            }
                            onSave(
                                society.copy(
                                    name = name.trim(),
                                    registrationNumber = regNumber.trim(),
                                    country = country,
                                    state = state,
                                    city = city,
                                    address = address.trim(),
                                    pinCode = pinCode,
                                    contactNumber = phone.trim(),
                                    email = email.trim(),
                                    logoBase64 = logoBase64,
                                    bankAccountName = bankName.trim(),
                                    bankAccountNumber = bankAccount.trim(),
                                    ifscCode = ifsc.trim().uppercase(),
                                    upiId = upiId.trim(),
                                    wingNames = wingNames,
                                    wingPlans = wingPlans,
                                    unitTypes = unitTypes,
                                    coveredParkingCount = coveredParking.toIntOrNull() ?: 0,
                                    openParkingCount = openParking.toIntOrNull() ?: 0,
                                    currentBalanceBank = openingBank.toDoubleOrNull() ?: 0.0,
                                    currentBalanceCash = openingCash.toDoubleOrNull() ?: 0.0,
                                    billingStartMonth = billingStartMonth.trim()
                                )
                            ) { message -> errorMessage = message }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        modifier = Modifier.testTag("save_society_profile_btn")
                    ) {
                        Text(
                            text = if (step < EDITOR_STEPS) {
                                AppStrings.get("save_continue", language)
                            } else {
                                AppStrings.get("save_profile", language)
                            }
                        )
                    }
                }
            }
        }
    }

/** Society Details, Unit Details, Floor Inventory, then Parking & Review. */
private const val EDITOR_STEPS = 4

/**
 * The editor's last step: how many parking bays the society has, and a summary
 * of everything the earlier steps are about to save.
 */
@Composable
private fun SocietyParkingReviewStep(
    society: SocietyInfo,
    wingNames: List<String>,
    wingPlans: List<WingPlan>,
    unitTypes: List<UnitTypeConfig>,
    plannedUnits: Int,
    billingStartMonth: String,
    coveredParking: String,
    onCoveredParking: (String) -> Unit,
    openParking: String,
    onOpenParking: (String) -> Unit,
    openingBank: String,
    onOpeningBank: (String) -> Unit,
    openingCash: String,
    onOpeningCash: (String) -> Unit
) {
    StepTitle("4. Parking & Review")

    SubHeading("Parking Bays")
    Text(
        text = "How many bays of each kind the society has. Left blank, one bay " +
            "per unit is offered.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = coveredParking,
            onValueChange = { onCoveredParking(it.filter { ch -> ch.isDigit() }.take(4)) },
            label = { Text("Covered Bays") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .testTag("society_editor_covered_parking")
        )
        OutlinedTextField(
            value = openParking,
            onValueChange = { onOpenParking(it.filter { ch -> ch.isDigit() }.take(4)) },
            label = { Text("Open Bays") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .testTag("society_editor_open_parking")
        )
    }

    Spacer(modifier = Modifier.height(18.dp))
    SubHeading("Opening Balance")
    Text(
        text = "What the society held before it started keeping books here. " +
            "Everything after that is added up from its own bills and vouchers, " +
            "so leave these at zero unless you are carrying a balance forward.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = openingBank,
            onValueChange = { onOpeningBank(it.filter { ch -> ch.isDigit() || ch == '.' }) },
            label = { Text("Opening Bank (₹)") },
            placeholder = { Text("0") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .testTag("society_editor_opening_bank")
        )
        OutlinedTextField(
            value = openingCash,
            onValueChange = { onOpeningCash(it.filter { ch -> ch.isDigit() || ch == '.' }) },
            label = { Text("Opening Cash (₹)") },
            placeholder = { Text("0") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .testTag("society_editor_opening_cash")
        )
    }

    Spacer(modifier = Modifier.height(18.dp))
    SubHeading("Review")
    Spacer(modifier = Modifier.height(6.dp))

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            ProfileRow("Login Code", society.societyCode)
            ProfileRow("Wings", wingNames.joinToString(", ").ifBlank { "-" })
            wingPlans.forEach { plan ->
                ProfileRow(
                    "Wing ${plan.name}",
                    "${plan.totalFloors} floors • ${plan.totalUnits} units"
                )
            }
            ProfileRow("Units Planned", plannedUnits.toString())
            ProfileRow(
                "Unit Types",
                unitTypes.filter { it.name.isNotBlank() }.joinToString(", ") { type ->
                    if (type.monthlyTariff > 0) "${type.name} (₹${type.monthlyTariff.toInt()})" else type.name
                }.ifBlank { "-" }
            )
            ProfileRow("Billing Starts", billingStartMonth.ifBlank { "-" })
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = "Saving creates any new wing or floor as vacant units. Units that " +
            "already exist are never overwritten.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * The signed-in member's own profile, opened from their photo in the top bar.
 * Logout lives at the bottom of this sheet rather than in the top bar.
 */
@Composable
fun UserProfileDialog(
    viewModel: SocietyViewModel,
    currentUser: Resident?,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    if (currentUser == null) {
        onDismiss()
        return
    }
    var isEditing by remember { mutableStateOf(false) }
    var name by remember(currentUser) { mutableStateOf(currentUser.ownerName) }
    var phone by remember(currentUser) { mutableStateOf(currentUser.contactNumber) }
    var email by remember(currentUser) { mutableStateOf(currentUser.email) }
    var photoBase64 by remember(currentUser) { mutableStateOf(currentUser.photoBase64) }

    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        modifier = DialogWidth,
        properties = WideDialogProperties,
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = AppStrings.get("user_profile", language),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                if (!isEditing) {
                    TextButton(
                        onClick = { isEditing = true },
                        modifier = Modifier.testTag("edit_user_profile_btn")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = TealPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppStrings.get("edit_profile", language), color = TealPrimary)
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Card(
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(containerColor = TealPrimary)
                    ) {
                        MemberAvatar(photoBase64 = photoBase64, name = currentUser.ownerName, size = 60)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEditing) name else currentUser.ownerName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${currentUser.flatNumber} • ${getRoleLabel(currentUser.role, language)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TealPrimary
                        )
                    }
                }

                if (isEditing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LogoPickerButton(
                        label = if (photoBase64.isBlank()) "Upload Photo" else "Change Photo",
                        onPicked = { photoBase64 = it },
                        onCleared = { photoBase64 = "" },
                        showClear = photoBase64.isNotBlank()
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                if (isEditing) {
                    ProfileField("Full Name", name) { name = it }
                    ProfileField("Mobile Number", phone, keyboardType = KeyboardType.Phone) {
                        phone = it.filter { ch -> ch.isDigit() }.take(10)
                    }
                    ProfileField("Email", email, keyboardType = KeyboardType.Email) { email = it }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Change Login PIN (optional)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ProfileField(
                        label = "New 4-Digit PIN",
                        value = newPin,
                        keyboardType = KeyboardType.NumberPassword,
                        isPassword = true
                    ) { newPin = it.filter { ch -> ch.isDigit() }.take(4) }
                    ProfileField(
                        label = "Confirm New PIN",
                        value = confirmPin,
                        keyboardType = KeyboardType.NumberPassword,
                        isPassword = true,
                        isError = confirmPin.isNotEmpty() && confirmPin != newPin
                    ) { confirmPin = it.filter { ch -> ch.isDigit() }.take(4) }
                } else {
                    ProfileRow("Unit No.", currentUser.flatNumber)
                    ProfileRow("Wing", currentUser.wing)
                    ProfileRow("Role", getRoleLabel(currentUser.role, language))
                    ProfileRow("Mobile", currentUser.contactNumber)
                    ProfileRow("Email", currentUser.email)
                    if (currentUser.tenantName.isNotBlank()) ProfileRow("Tenant", currentUser.tenantName)
                    // The society's own unit type is what the unit was created
                    // and is billed as. flatType is only the nearest built-in
                    // slab, so showing it here reported the wrong type for any
                    // society whose types are its own ("Shop", "Office").
                    ProfileRow(
                        "Unit Type",
                        currentUser.unitType.ifBlank { currentUser.flatType.displayName }
                    )
                    ProfileRow("Parking Slot", currentUser.parkingSlot.ifBlank { "-" })
                    ProfileRow("Outstanding Dues", "₹%,.2f".format(currentUser.balanceDue))
                }

                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }

                // Logout now belongs to the profile, not the top bar.
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onLogout()
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, StatusOverdue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_logout_btn")
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = StatusOverdue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(AppStrings.get("logout", language), color = StatusOverdue)
                }
            }
        },
        confirmButton = {
            if (isEditing) {
                Button(
                    onClick = {
                        if (newPin.isNotBlank()) {
                            if (newPin.length != 4) {
                                error = "The PIN must be exactly 4 digits."
                                return@Button
                            }
                            if (newPin != confirmPin) {
                                error = "The two PINs do not match."
                                return@Button
                            }
                        }
                        viewModel.updateMyProfile(name, phone, email, photoBase64) { ok, message ->
                            if (!ok) {
                                error = message
                                return@updateMyProfile
                            }
                            if (newPin.length == 4) viewModel.updatePin(newPin) else viewModel.showMessage(message)
                            isEditing = false
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.testTag("save_user_profile_btn")
                ) {
                    Text(AppStrings.get("save_profile", language))
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text(AppStrings.get("close", language))
                }
            }
        },
        dismissButton = {
            if (isEditing) {
                OutlinedButton(onClick = { isEditing = false; error = null }) {
                    Text(AppStrings.get("cancel", language))
                }
            }
        }
    )
}

// ------------------------------------------------------------------ helpers

@Composable
private fun ProfileRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    isError: Boolean = false,
    supporting: String? = null,
    minLines: Int = 1,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = minLines == 1,
        minLines = minLines,
        isError = isError,
        supportingText = supporting?.let { { Text(it) } },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        leadingIcon = if (isPassword) {
            { Icon(Icons.Default.Lock, contentDescription = null, tint = TealPrimary) }
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )
}

@Composable
private fun LogoPickerButton(
    label: String,
    onPicked: (String) -> Unit,
    onCleared: () -> Unit,
    showClear: Boolean
) {
    val context = LocalContext.current
    var pickError by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri).use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream) ?: error("unsupported image")
                ImageBlob.fromBitmap(bitmap)
            }
        }.onSuccess { onPicked(it); pickError = null }
            .onFailure { pickError = "Could not read that image. Pick a JPG or PNG." }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
            onClick = { picker.launch("image/*") },
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, TealPrimary)
        ) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = TealPrimary)
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = TealPrimary)
        }
        if (showClear) {
            TextButton(onClick = onCleared) {
                Text("Remove", color = MaterialTheme.colorScheme.error)
            }
        }
    }
    pickError?.let {
        Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
}

/** Card wrapper used for the read-only blocks in both profiles. */
@Composable
fun ProfileSection(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}
