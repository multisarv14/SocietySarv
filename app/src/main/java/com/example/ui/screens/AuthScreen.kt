package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.SocietyInfo
import com.example.data.entity.UserRole
import com.example.data.localization.AppLanguage
import com.example.data.localization.AppStrings
import com.example.ui.theme.MintSecondary
import com.example.ui.theme.SlateOutlineVariant
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryContainer
import com.example.viewmodel.SocietyViewModel

@Composable
fun AuthScreen(
    viewModel: SocietyViewModel,
    societyInfo: SocietyInfo?,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    var isWizardOpen by remember { mutableStateOf(false) }

    if (isWizardOpen) {
        SocietyRegistrationWizard(
            language = language,
            onDismiss = { isWizardOpen = false },
            onFinish = { name, reg, code, addr, wings, flats, cName, cPhone, cEmail, cFlat, cPin, bName, bAcc, ifsc, initBal, t1, t2, t3, tShop, onError ->
                viewModel.registerNewSociety(
                    name, reg, code, addr, wings, flats, cName, cPhone, cEmail, cFlat, cPin,
                    bName, bAcc, ifsc, initBal, t1, t2, t3, tShop
                ) { success, message ->
                    if (success) {
                        viewModel.showMessage(message)
                        isWizardOpen = false
                    } else {
                        onError(message)
                    }
                }
            }
        )
    } else {
        LoginFormContent(
            viewModel = viewModel,
            societyInfo = societyInfo,
            language = language,
            onOpenWizard = { isWizardOpen = true },
            modifier = modifier
        )
    }
}

@Composable
fun LoginFormContent(
    viewModel: SocietyViewModel,
    societyInfo: SocietyInfo?,
    language: AppLanguage,
    onOpenWizard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var societyCode by remember { mutableStateOf("") }
    var flatOrMobile by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Hero Logo & Title
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(TealPrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CorporateFare,
                contentDescription = "Logo",
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = societyInfo?.name ?: AppStrings.get("app_title", language),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Text(
            text = AppStrings.get("app_tagline", language),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        // Login Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, SlateOutlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = AppStrings.get("login_button", language),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = societyCode,
                    onValueChange = { societyCode = it; errorMessage = null },
                    label = { Text(AppStrings.get("society_code", language)) },
                    placeholder = { Text("Society login code") },
                    leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = TealPrimary) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_society_code_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = flatOrMobile,
                    onValueChange = { flatOrMobile = it; errorMessage = null },
                    label = { Text(AppStrings.get("unit_number", language) + " / " + AppStrings.get("mobile_number", language)) },
                    placeholder = { Text("Flat number or registered mobile") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TealPrimary) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_flat_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(6); errorMessage = null },
                    label = { Text(AppStrings.get("security_pin", language)) },
                    placeholder = { Text("4-digit PIN") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TealPrimary) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_pin_input")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        viewModel.login(societyCode, flatOrMobile, pin) { success, msg ->
                            if (!success) errorMessage = msg
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("login_submit_btn")
                ) {
                    Text(
                        text = AppStrings.get("login_button", language),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // New Society Registration Button
        OutlinedButton(
            onClick = onOpenWizard,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, TealPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("register_society_wizard_btn")
        ) {
            Icon(Icons.Default.CorporateFare, contentDescription = null, tint = TealPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = AppStrings.get("register_new_society", language),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = TealPrimary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// 4-Step Registration Wizard
@Composable
fun SocietyRegistrationWizard(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onFinish: (
        name: String, regNumber: String, code: String, address: String, wings: Int, flats: Int,
        chairmanName: String, chairmanPhone: String, chairmanEmail: String, chairmanFlat: String,
        chairmanPin: String,
        bankName: String, bankAccount: String, ifsc: String, initialBankBalance: Double,
        tariff1BHK: Double, tariff2BHK: Double, tariff3BHK: Double, tariffShop: Double,
        onError: (String) -> Unit
    ) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Step 1: Society Info
    var societyName by remember { mutableStateOf("") }
    var regNumber by remember { mutableStateOf("") }
    var societyCode by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var totalWings by remember { mutableStateOf("") }
    var totalFlats by remember { mutableStateOf("") }

    // Step 2: Chairman / Office Bearers
    var chairmanName by remember { mutableStateOf("") }
    var chairmanPhone by remember { mutableStateOf("") }
    var chairmanEmail by remember { mutableStateOf("") }
    var chairmanFlat by remember { mutableStateOf("") }
    var chairmanPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    // Step 3: Bank Account & Tariff
    var bankName by remember { mutableStateOf("") }
    var bankAccount by remember { mutableStateOf("") }
    var ifscCode by remember { mutableStateOf("") }
    var initialBankBalance by remember { mutableStateOf("") }
    var tariff1BHK by remember { mutableStateOf("") }
    var tariff2BHK by remember { mutableStateOf("") }
    var tariff3BHK by remember { mutableStateOf("") }
    var tariffShop by remember { mutableStateOf("") }

    // What each step needs before the user is allowed to move on.
    val stepError: String? = when (step) {
        1 -> when {
            societyName.isBlank() -> "Enter the society name."
            societyCode.isBlank() -> "Enter a society login code - members sign in with it."
            else -> null
        }
        2 -> when {
            chairmanName.isBlank() -> "Enter the chairman's full name."
            chairmanFlat.isBlank() -> "Enter the chairman's flat number."
            chairmanPhone.length < 10 -> "Enter a valid 10-digit mobile number."
            chairmanPin.length < 4 -> "Set a PIN of at least 4 digits."
            chairmanPin != confirmPin -> "The two PINs do not match."
            else -> null
        }
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(16.dp)
    ) {
        // Wizard Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = { if (step > 1) step-- else onDismiss() },
                modifier = Modifier.testTag("wizard_back_btn")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = AppStrings.get("register_new_society", language),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$step / 4",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = TealPrimary
            )
        }

        LinearProgressIndicator(
            progress = { step / 4f },
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
        ) {
            when (step) {
                1 -> {
                    Text(
                        text = AppStrings.get("wizard_step1", language),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = societyName,
                        onValueChange = { societyName = it },
                        label = { Text("Society Name (संस्थेचे नाव)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = regNumber,
                        onValueChange = { regNumber = it },
                        label = { Text("Registration No. (नोंदणी क्रमांक)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = societyCode,
                        onValueChange = { societyCode = it; errorMessage = null },
                        label = { Text("Society Code (लॉगिन कोड)") },
                        placeholder = { Text("e.g. SS002") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Society Address (पत्ता)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = totalWings,
                            onValueChange = { totalWings = it },
                            label = { Text("Total Wings (विंग)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = totalFlats,
                            onValueChange = { totalFlats = it },
                            label = { Text("Total Flats (सदनिका)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                2 -> {
                    Text(
                        text = AppStrings.get("wizard_step2", language),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = chairmanName,
                        onValueChange = { chairmanName = it },
                        label = { Text("Chairman Full Name (अध्यक्षांचे नाव)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = chairmanFlat,
                        onValueChange = { chairmanFlat = it },
                        label = { Text("Chairman Flat No. (फ्लॅट क्र.)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = chairmanPhone,
                        onValueChange = { chairmanPhone = it },
                        label = { Text("Chairman Mobile (मोबाईल)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = chairmanEmail,
                        onValueChange = { chairmanEmail = it },
                        label = { Text("Email (ईमेल)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    // The chairman logs in with this PIN straight after registering.
                    OutlinedTextField(
                        value = chairmanPin,
                        onValueChange = { chairmanPin = it.filter { ch -> ch.isDigit() }.take(6) },
                        label = { Text("Set Login PIN (लॉगिन पिन)") },
                        placeholder = { Text("4-digit PIN") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TealPrimary) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wizard_pin_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { confirmPin = it.filter { ch -> ch.isDigit() }.take(6) },
                        label = { Text("Confirm PIN (पिन पुन्हा टाका)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TealPrimary) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = confirmPin.isNotEmpty() && confirmPin != chairmanPin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wizard_confirm_pin_input")
                    )
                }
                3 -> {
                    Text(
                        text = AppStrings.get("wizard_step3", language),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Society Bank Name (बँकेचे नाव)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = bankAccount,
                        onValueChange = { bankAccount = it },
                        label = { Text("Bank Account Number (खाते क्र.)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = ifscCode,
                            onValueChange = { ifscCode = it },
                            label = { Text("IFSC Code") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = initialBankBalance,
                            onValueChange = { initialBankBalance = it },
                            label = { Text("Initial Balance (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Monthly Maintenance Tariff Slabs",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tariff1BHK,
                            onValueChange = { tariff1BHK = it },
                            label = { Text("1 BHK (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = tariff2BHK,
                            onValueChange = { tariff2BHK = it },
                            label = { Text("2 BHK (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tariff3BHK,
                            onValueChange = { tariff3BHK = it },
                            label = { Text("3 BHK (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = tariffShop,
                            onValueChange = { tariffShop = it },
                            label = { Text("Shop (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                4 -> {
                    Text(
                        text = AppStrings.get("wizard_step4", language),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = societyName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(text = "Code: $societyCode | Reg: $regNumber", style = MaterialTheme.typography.bodySmall)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(text = "Chairman: $chairmanName ($chairmanFlat, $chairmanPhone)")
                            Text(
                                text = "Login with -> code $societyCode, flat $chairmanFlat, and the PIN you just set.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TealPrimary
                            )
                            Text(text = "Bank: $bankName (₹$initialBankBalance)")
                            Text(text = "Tariff: 1BHK: ₹$tariff1BHK | 2BHK: ₹$tariff2BHK | 3BHK: ₹$tariff3BHK")
                        }
                    }
                }
            }
        }

        // Anything blocking the current step, or an error the backend sent back.
        val blocker = errorMessage ?: stepError
        if (blocker != null) {
            Text(
                text = blocker,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
                    .testTag("wizard_error_text")
            )
        }

        // Wizard Bottom Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("wizard_cancel_btn")
            ) {
                Text(AppStrings.get("cancel", language))
            }

            Button(
                enabled = stepError == null,
                onClick = {
                    errorMessage = null
                    if (step < 4) {
                        step++
                    } else {
                        onFinish(
                            societyName,
                            regNumber,
                            societyCode,
                            address,
                            totalWings.toIntOrNull() ?: 1,
                            totalFlats.toIntOrNull() ?: 0,
                            chairmanName,
                            chairmanPhone,
                            chairmanEmail,
                            chairmanFlat,
                            chairmanPin,
                            bankName,
                            bankAccount,
                            ifscCode,
                            initialBankBalance.toDoubleOrNull() ?: 0.0,
                            tariff1BHK.toDoubleOrNull() ?: 0.0,
                            tariff2BHK.toDoubleOrNull() ?: 0.0,
                            tariff3BHK.toDoubleOrNull() ?: 0.0,
                            tariffShop.toDoubleOrNull() ?: 0.0
                        ) { message -> errorMessage = message }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.testTag("wizard_next_btn")
            ) {
                Text(
                    text = if (step < 4) AppStrings.get("save_continue", language) else AppStrings.get("finish_setup", language)
                )
            }
        }
    }
}
