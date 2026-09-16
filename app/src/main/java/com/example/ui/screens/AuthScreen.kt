package com.example.ui.screens

import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.entity.Resident
import com.example.data.entity.SocietyInfo
import com.example.data.entity.UnitTypeConfig
import com.example.data.entity.WingPlan
import com.example.data.entity.WingUnitCount
import com.example.data.firebase.PhoneOtpManager
import com.example.data.localization.AppLanguage
import com.example.ui.components.DialogWidth
import com.example.ui.components.WideDialogProperties
import com.example.data.localization.AppStrings
import com.example.data.localization.LocationData
import com.example.ui.components.MonthYearPickerField
import com.example.ui.components.SearchableDropdownField
import com.example.ui.theme.SlateOutlineVariant
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.TealPrimary
import com.example.util.ImageBlob
import com.example.util.rememberBlobImage
import com.example.viewmodel.SocietyRegistrationRequest
import com.example.viewmodel.SocietyViewModel

@Composable
fun AuthScreen(
    viewModel: SocietyViewModel,
    societyInfo: SocietyInfo?,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    var isWizardOpen by remember { mutableStateOf(false) }
    val pendingRegistration by viewModel.pendingRegistration.collectAsState()

    // Shown once the society exists but before the chairman is signed in, so
    // the login details can actually be handed over (5. Confirmation).
    pendingRegistration?.let { pending ->
        RegistrationSuccessDialog(
            society = pending.society,
            chairman = pending.chairman,
            body = viewModel.registrationConfirmationText(pending.society, pending.chairman),
            language = language,
            onContinue = {
                isWizardOpen = false
                viewModel.completeRegistrationSignIn()
            }
        )
    }

    if (isWizardOpen) {
        SocietyRegistrationWizard(
            language = language,
            onDismiss = { isWizardOpen = false },
            onFinish = { request, onError ->
                viewModel.registerNewSociety(request) { success, message ->
                    if (success) viewModel.showMessage(message) else onError(message)
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

// ---------------------------------------------------------------- login page

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
    var isPinVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isForgotPinOpen by remember { mutableStateOf(false) }
    // On by default: a member should not have to type their PIN every time the
    // app is opened. Unticking it makes this a one-off, device-shared sign-in.
    var rememberMe by remember { mutableStateOf(true) }

    if (isForgotPinOpen) {
        ForgotPinDialog(
            viewModel = viewModel,
            language = language,
            prefilledSocietyCode = societyCode,
            onDismiss = { isForgotPinOpen = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState())
            // Keeps the field being typed into above the keyboard instead of
            // buried underneath it.
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        SocietySarvLogo(logoBase64 = societyInfo?.logoBase64, size = 96)

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
                    placeholder = { Text("e.g. SS002") },
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
                    placeholder = { Text("Unit number or registered mobile") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TealPrimary) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_flat_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(4); errorMessage = null },
                    label = { Text(AppStrings.get("security_pin", language)) },
                    placeholder = { Text("••••") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TealPrimary) },
                    trailingIcon = {
                        PinVisibilityToggle(
                            isVisible = isPinVisible,
                            onToggle = { isPinVisible = !isPinVisible },
                            testTag = "login_pin_visibility_btn"
                        )
                    },
                    visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
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

                Spacer(modifier = Modifier.height(4.dp))

                // Remember Me: keeps the member signed in the next time the app
                // is opened. Only the society code and the unit / mobile number
                // are kept on the device - never the PIN.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { rememberMe = !rememberMe }
                            .testTag("login_remember_me_row")
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = TealPrimary),
                            modifier = Modifier.testTag("login_remember_me_checkbox")
                        )
                        Text(
                            text = AppStrings.get("remember_me", language),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    TextButton(
                        onClick = { isForgotPinOpen = true },
                        modifier = Modifier.testTag("forgot_pin_btn")
                    ) {
                        Text(
                            text = AppStrings.get("forgot_pin", language),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = TealPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        viewModel.login(societyCode, flatOrMobile, pin, rememberMe) { success, msg ->
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

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = AppStrings.get("contact_chairman_note", language),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("contact_chairman_note")
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** The society's own logo once it has uploaded one, else the SocietySarv mark. */
@Composable
private fun SocietySarvLogo(logoBase64: String?, size: Int) {
    val societyLogo = rememberBlobImage(logoBase64)
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 4).dp)),
        contentAlignment = Alignment.Center
    ) {
        if (societyLogo != null) {
            Image(
                bitmap = societyLogo,
                contentDescription = "Society logo",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "SocietySarv logo",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun PinVisibilityToggle(isVisible: Boolean, onToggle: () -> Unit, testTag: String) {
    IconButton(onClick = onToggle, modifier = Modifier.testTag(testTag)) {
        Icon(
            imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = if (isVisible) "Hide PIN" else "Show PIN",
            tint = TealPrimary
        )
    }
}

// ------------------------------------------------------------- forgot my PIN

/**
 * Resets a login PIN against the mobile number registered for the flat, proven
 * with a Firebase SMS OTP. Runs in three stages: identify -> verify -> set.
 */
@Composable
fun ForgotPinDialog(
    viewModel: SocietyViewModel,
    language: AppLanguage,
    prefilledSocietyCode: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    var stage by remember { mutableIntStateOf(STAGE_IDENTIFY) }
    var societyCode by remember { mutableStateOf(prefilledSocietyCode) }
    var mobile by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isPinVisible by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var resolvedMember by remember { mutableStateOf<Resident?>(null) }
    var resolvedSocietyCode by remember { mutableStateOf("") }

    fun close() {
        PhoneOtpManager.reset()
        onDismiss()
    }

    AlertDialog(
        modifier = DialogWidth,
        properties = WideDialogProperties,
        onDismissRequest = { close() },
        title = {
            Text(
                text = AppStrings.get("reset_pin_title", language),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                when (stage) {
                    STAGE_IDENTIFY -> {
                        Text(
                            text = when (language) {
                                AppLanguage.MARATHI -> "तुमचा सोसायटी लॉगिन कोड व नोंदणीकृत मोबाईल क्रमांक टाका. त्या क्रमांकावर OTP पाठवला जाईल."
                                AppLanguage.HINDI -> "अपना सोसायटी लॉगिन कोड और पंजीकृत मोबाइल नंबर डालें। उस नंबर पर ओटीपी भेजा जाएगा।"
                                AppLanguage.ENGLISH -> "Enter your society login code and the mobile number registered against your flat. We will send an OTP to that number."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = societyCode,
                            onValueChange = { societyCode = it; error = null },
                            label = { Text(AppStrings.get("society_code", language)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("forgot_society_code_input")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = mobile,
                            onValueChange = { mobile = it.filter { ch -> ch.isDigit() }.take(10); error = null },
                            label = { Text(AppStrings.get("mobile_number", language)) },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = TealPrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("forgot_mobile_input")
                        )
                    }

                    STAGE_VERIFY -> {
                        Text(
                            text = "OTP sent to ${PhoneOtpManager.toE164(mobile)} (${resolvedMember?.flatNumber.orEmpty()}).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = otp,
                            onValueChange = { otp = it.filter { ch -> ch.isDigit() }.take(6); error = null },
                            label = { Text(AppStrings.get("enter_otp", language)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("forgot_otp_input")
                        )
                        TextButton(
                            enabled = !isBusy && activity != null,
                            onClick = {
                                val act = activity ?: return@TextButton
                                isBusy = true
                                error = null
                                PhoneOtpManager.sendOtp(
                                    activity = act,
                                    phoneNumber = mobile,
                                    resend = true,
                                    onCodeSent = { isBusy = false; info = "OTP sent again." },
                                    onAutoVerified = { isBusy = false; stage = STAGE_SET_PIN },
                                    onError = { isBusy = false; error = it }
                                )
                            }
                        ) { Text(AppStrings.get("resend_otp", language), color = TealPrimary) }
                    }

                    STAGE_SET_PIN -> {
                        Text(
                            text = "Verified. Set a new 4-digit login PIN for ${resolvedMember?.flatNumber.orEmpty()}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { newPin = it.filter { ch -> ch.isDigit() }.take(4); error = null },
                            label = { Text(AppStrings.get("new_pin", language)) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TealPrimary) },
                            trailingIcon = {
                                PinVisibilityToggle(
                                    isVisible = isPinVisible,
                                    onToggle = { isPinVisible = !isPinVisible },
                                    testTag = "forgot_pin_visibility_btn"
                                )
                            },
                            visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("forgot_new_pin_input")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = confirmPin,
                            onValueChange = { confirmPin = it.filter { ch -> ch.isDigit() }.take(4); error = null },
                            label = { Text(AppStrings.get("confirm_new_pin", language)) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TealPrimary) },
                            visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            isError = confirmPin.isNotEmpty() && confirmPin != newPin,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("forgot_confirm_pin_input")
                        )
                    }
                }

                if (activity == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "OTP cannot be sent from here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                info?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = TealPrimary)
                }
                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("forgot_pin_error")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                onClick = {
                    error = null
                    info = null
                    when (stage) {
                        STAGE_IDENTIFY -> {
                            val act = activity
                            if (act == null) {
                                error = "OTP cannot be sent from here."
                                return@Button
                            }
                            isBusy = true
                            viewModel.findMemberForPinReset(societyCode, mobile) { member, detail ->
                                if (member == null) {
                                    isBusy = false
                                    error = detail
                                } else {
                                    resolvedMember = member
                                    resolvedSocietyCode = detail
                                    PhoneOtpManager.sendOtp(
                                        activity = act,
                                        phoneNumber = mobile,
                                        onCodeSent = { isBusy = false; stage = STAGE_VERIFY },
                                        onAutoVerified = { isBusy = false; stage = STAGE_SET_PIN },
                                        onError = { isBusy = false; error = it }
                                    )
                                }
                            }
                        }

                        STAGE_VERIFY -> {
                            if (otp.length != 6) {
                                error = "Enter the 6-digit OTP."
                                return@Button
                            }
                            isBusy = true
                            PhoneOtpManager.verifyCode(otp) { ok, detail ->
                                isBusy = false
                                if (ok) stage = STAGE_SET_PIN else error = detail
                            }
                        }

                        STAGE_SET_PIN -> {
                            if (newPin.length != 4) {
                                error = "The PIN must be exactly 4 digits."
                                return@Button
                            }
                            if (newPin != confirmPin) {
                                error = "The two PINs do not match."
                                return@Button
                            }
                            val member = resolvedMember ?: return@Button
                            isBusy = true
                            viewModel.resetPinAfterOtp(resolvedSocietyCode, member, newPin) { ok, detail ->
                                isBusy = false
                                if (ok) {
                                    viewModel.showMessage(detail)
                                    close()
                                } else {
                                    error = detail
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.testTag("forgot_pin_primary_btn")
            ) {
                Text(
                    text = when (stage) {
                        STAGE_IDENTIFY -> AppStrings.get("send_otp", language)
                        STAGE_VERIFY -> AppStrings.get("verify_otp", language)
                        else -> AppStrings.get("save_new_pin", language)
                    }
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { close() }) {
                Text(AppStrings.get("cancel", language))
            }
        }
    )
}

private const val STAGE_IDENTIFY = 0
private const val STAGE_VERIFY = 1
private const val STAGE_SET_PIN = 2

// ------------------------------------------------------- registration wizard

/**
 * 5-step society registration.
 *
 * 1. Society Details   - identity, address, contact, logo, bank account
 * 2. Unit Details      - how many wings and what they are called
 * 3. Floor Inventory   - floors and units per wing, plus the unit types
 * 4. Office Bearers    - the chairman and the 4-digit PIN he logs in with
 * 5. Confirmation      - review, then hand the login details over
 */
@Composable
fun SocietyRegistrationWizard(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onFinish: (request: SocietyRegistrationRequest, onError: (String) -> Unit) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 1. Society Details
    var societyName by remember { mutableStateOf("") }
    var regNumber by remember { mutableStateOf("") }
    var societyCode by remember { mutableStateOf("") }
    var country by remember { mutableStateOf(LocationData.DEFAULT_COUNTRY) }
    var state by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var pinCode by remember { mutableStateOf("") }
    var societyPhone by remember { mutableStateOf("") }
    var societyEmail by remember { mutableStateOf("") }
    var logoBase64 by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var bankAccount by remember { mutableStateOf("") }
    var ifscCode by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var paymentQrBase64 by remember { mutableStateOf("") }
    var initialBankBalance by remember { mutableStateOf("") }
    // Billing opens on the current month unless the committee moves it back.
    var billingStartMonth by remember { mutableStateOf(currentMonthLabel()) }

    // 2. Unit Details
    var totalWings by remember { mutableStateOf("") }
    var wingNamesRaw by remember { mutableStateOf("") }

    // 3. Floor Inventory
    var wingPlans by remember { mutableStateOf<List<WingPlan>>(emptyList()) }

    // 4. Office Bearers
    var chairmanName by remember { mutableStateOf("") }
    var chairmanPhone by remember { mutableStateOf("") }
    var chairmanEmail by remember { mutableStateOf("") }
    var chairmanFlat by remember { mutableStateOf("") }
    var chairmanPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    val wingNames = remember(wingNamesRaw) { parseWingNames(wingNamesRaw) }
    val plannedUnits = remember(wingPlans) { wingPlans.sumOf { it.totalUnits } }
    // The society's unit types are simply the ones its wings were built from;
    // what each type is charged per month is set under Maintenance > Tariff.
    val unitTypes = remember(wingPlans) { unitTypesOf(wingPlans) }

    val stepError: String? = when (step) {
        1 -> when {
            societyName.isBlank() -> "Enter the society name."
            societyCode.isBlank() -> "Enter a society login code - members sign in with it."
            pinCode.isNotBlank() && pinCode.length != 6 -> "PIN code must be exactly 6 digits."
            societyEmail.isNotBlank() && !societyEmail.contains("@") -> "Enter a valid society email id."
            else -> null
        }
        2 -> when {
            (totalWings.toIntOrNull() ?: 0) <= 0 -> "Enter how many wings the society has."
            wingNames.isEmpty() -> "Enter the wing names, separated by commas."
            wingNames.size != (totalWings.toIntOrNull() ?: 0) ->
                "You entered ${wingNames.size} wing name(s) for ${totalWings.toIntOrNull() ?: 0} wing(s)."
            else -> null
        }
        3 -> when {
            wingPlans.none { it.totalUnits > 0 } ->
                "Add at least one unit type with a quantity, and save the wing."
            else -> null
        }
        4 -> when {
            chairmanName.isBlank() -> "Enter the chairman's full name."
            chairmanFlat.isBlank() -> "Enter the chairman's unit number."
            chairmanPhone.length < 10 -> "Enter a valid 10-digit mobile number."
            chairmanPin.length != 4 -> "The login PIN must be exactly 4 digits."
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
            .imePadding()
            .padding(16.dp)
    ) {
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
                text = "$step / $TOTAL_STEPS",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = TealPrimary
            )
        }

        LinearProgressIndicator(
            progress = { step / TOTAL_STEPS.toFloat() },
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
                1 -> SocietyDetailsStep(
                    language = language,
                    societyName = societyName, onSocietyName = { societyName = it },
                    regNumber = regNumber, onRegNumber = { regNumber = it },
                    societyCode = societyCode, onSocietyCode = { societyCode = it; errorMessage = null },
                    country = country,
                    onCountry = { country = it; state = ""; city = "" },
                    state = state, onState = { state = it; city = "" },
                    city = city, onCity = { city = it },
                    address = address, onAddress = { address = it },
                    pinCode = pinCode, onPinCode = { pinCode = it },
                    phone = societyPhone, onPhone = { societyPhone = it },
                    email = societyEmail, onEmail = { societyEmail = it },
                    logoBase64 = logoBase64, onLogo = { logoBase64 = it },
                    bankName = bankName, onBankName = { bankName = it },
                    bankAccount = bankAccount, onBankAccount = { bankAccount = it },
                    ifscCode = ifscCode, onIfsc = { ifscCode = it },
                    upiId = upiId, onUpiId = { upiId = it },
                    paymentQrBase64 = paymentQrBase64, onPaymentQr = { paymentQrBase64 = it },
                    initialBankBalance = initialBankBalance, onInitialBalance = { initialBankBalance = it },
                    billingStartMonth = billingStartMonth,
                    onBillingStartMonth = { billingStartMonth = it }
                )

                2 -> UnitDetailsStep(
                    language = language,
                    totalWings = totalWings,
                    onTotalWings = { totalWings = it },
                    wingNamesRaw = wingNamesRaw,
                    onWingNamesRaw = { wingNamesRaw = it },
                    parsedWingNames = wingNames
                )

                3 -> FloorInventoryStep(
                    language = language,
                    wingNames = wingNames,
                    wingPlans = wingPlans,
                    onWingPlans = { wingPlans = it }
                )

                4 -> OfficeBearersStep(
                    language = language,
                    name = chairmanName, onName = { chairmanName = it },
                    flat = chairmanFlat, onFlat = { chairmanFlat = it },
                    phone = chairmanPhone, onPhone = { chairmanPhone = it },
                    email = chairmanEmail, onEmail = { chairmanEmail = it },
                    pin = chairmanPin, onPin = { chairmanPin = it },
                    confirmPin = confirmPin, onConfirmPin = { confirmPin = it }
                )

                5 -> ConfirmationStep(
                    language = language,
                    societyName = societyName,
                    societyCode = societyCode,
                    regNumber = regNumber,
                    location = listOf(city, state, country).filter { it.isNotBlank() }.joinToString(", "),
                    address = address,
                    pinCode = pinCode,
                    societyPhone = societyPhone,
                    societyEmail = societyEmail,
                    logoBase64 = logoBase64,
                    wingNames = wingNames,
                    wingPlans = wingPlans,
                    unitTypes = unitTypes,
                    plannedUnits = plannedUnits,
                    chairmanName = chairmanName,
                    chairmanFlat = chairmanFlat,
                    chairmanPhone = chairmanPhone,
                    chairmanEmail = chairmanEmail,
                    bankName = bankName,
                    initialBankBalance = initialBankBalance
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
                    .testTag("wizard_error_text")
            )
        }

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
                    if (step < TOTAL_STEPS) {
                        step++
                    } else {
                        onFinish(
                            SocietyRegistrationRequest(
                                name = societyName,
                                regNumber = regNumber,
                                code = societyCode,
                                country = country,
                                state = state,
                                city = city,
                                address = address,
                                pinCode = pinCode,
                                contactNumber = societyPhone,
                                email = societyEmail,
                                logoBase64 = logoBase64,
                                bankName = bankName,
                                bankAccount = bankAccount,
                                ifsc = ifscCode,
                                upiId = upiId,
                                paymentQrBase64 = paymentQrBase64,
                                initialBankBalance = initialBankBalance.toDoubleOrNull() ?: 0.0,
                                billingStartMonth = billingStartMonth,
                                wingNames = wingNames,
                                wingPlans = wingPlans,
                                unitTypes = unitTypes.filter { it.name.isNotBlank() },
                                chairmanName = chairmanName,
                                chairmanPhone = chairmanPhone,
                                chairmanEmail = chairmanEmail,
                                chairmanFlat = chairmanFlat,
                                chairmanPin = chairmanPin
                            )
                        ) { message -> errorMessage = message }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.testTag("wizard_next_btn")
            ) {
                Text(
                    text = if (step < TOTAL_STEPS) {
                        AppStrings.get("save_continue", language)
                    } else {
                        AppStrings.get("finish_setup", language)
                    }
                )
            }
        }
    }
}

private const val TOTAL_STEPS = 5

/**
 * The distinct unit types a set of wings is built from, in the order they were
 * first entered. Tariffs start at zero and are set under Maintenance > Tariff.
 */
internal fun unitTypesOf(wingPlans: List<WingPlan>): List<UnitTypeConfig> =
    wingPlans.flatMap { it.unitCounts }
        .map { it.unitType.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
        .map { UnitTypeConfig(it, 0.0) }

/** This month as "August 2026" - the format every bill month is stored in. */
internal fun currentMonthLabel(): String =
    java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.ENGLISH)
        .format(java.util.Date())

/** "A, B , c" -> ["A", "B", "C"], duplicates and stray commas dropped. */
internal fun parseWingNames(raw: String): List<String> =
    raw.split(",")
        .map { it.trim().uppercase() }
        .filter { it.isNotBlank() }
        .distinct()

// ------------------------------------------------------ step 1: society details

@Composable
internal fun SocietyDetailsStep(
    language: AppLanguage,
    societyName: String, onSocietyName: (String) -> Unit,
    regNumber: String, onRegNumber: (String) -> Unit,
    societyCode: String, onSocietyCode: (String) -> Unit,
    country: String, onCountry: (String) -> Unit,
    state: String, onState: (String) -> Unit,
    city: String, onCity: (String) -> Unit,
    address: String, onAddress: (String) -> Unit,
    pinCode: String, onPinCode: (String) -> Unit,
    phone: String, onPhone: (String) -> Unit,
    email: String, onEmail: (String) -> Unit,
    logoBase64: String, onLogo: (String) -> Unit,
    bankName: String, onBankName: (String) -> Unit,
    bankAccount: String, onBankAccount: (String) -> Unit,
    ifscCode: String, onIfsc: (String) -> Unit,
    upiId: String, onUpiId: (String) -> Unit,
    paymentQrBase64: String, onPaymentQr: (String) -> Unit,
    initialBankBalance: String, onInitialBalance: (String) -> Unit,
    billingStartMonth: String, onBillingStartMonth: (String) -> Unit,
    /**
     * False once the society exists: the login code is what every member signs
     * in against, so the Society Profile shows it without letting it be edited.
     */
    codeEditable: Boolean = true,
    /** False in the Society Profile - the opening balance belongs to the ledger. */
    showOpeningBalance: Boolean = true
) {
    StepTitle(AppStrings.get("wizard_step1", language))

    OutlinedTextField(
        value = societyName,
        onValueChange = onSocietyName,
        label = { Text("Society Name (संस्थेचे नाव)") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wizard_society_name_input")
    )
    FieldGap()
    OutlinedTextField(
        value = regNumber,
        onValueChange = onRegNumber,
        label = { Text("Registration No. (नोंदणी क्रमांक)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    FieldGap()
    OutlinedTextField(
        value = societyCode,
        onValueChange = { if (codeEditable) onSocietyCode(it) },
        readOnly = !codeEditable,
        label = { Text("Society Login Code (लॉगिन कोड)") },
        placeholder = { Text("e.g. SS002") },
        supportingText = if (codeEditable) null else {
            { Text("Members sign in with this code, so it cannot be changed.") }
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wizard_society_code_input")
    )
    FieldGap()

    SearchableDropdownField(
        label = "Country (देश)",
        value = country,
        options = LocationData.countries,
        onValueChange = onCountry,
        testTag = "wizard_country_dropdown"
    )
    FieldGap()
    SearchableDropdownField(
        label = "State (राज्य)",
        value = state,
        options = LocationData.statesOf(country),
        onValueChange = onState,
        testTag = "wizard_state_dropdown"
    )
    FieldGap()
    SearchableDropdownField(
        label = "City (शहर)",
        value = city,
        options = LocationData.citiesOf(country, state),
        onValueChange = onCity,
        testTag = "wizard_city_dropdown"
    )
    FieldGap()

    OutlinedTextField(
        value = address,
        onValueChange = onAddress,
        label = { Text("Society Address (पत्ता)") },
        minLines = 2,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wizard_address_input")
    )
    FieldGap()
    OutlinedTextField(
        value = pinCode,
        onValueChange = { onPinCode(it.filter { ch -> ch.isDigit() }.take(6)) },
        label = { Text("PIN Code (पिन कोड)") },
        placeholder = { Text("6 digits") },
        supportingText = { Text("${pinCode.length}/6") },
        isError = pinCode.isNotBlank() && pinCode.length != 6,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wizard_pincode_input")
    )
    FieldGap()
    OutlinedTextField(
        value = phone,
        onValueChange = { onPhone(it.filter { ch -> ch.isDigit() }.take(10)) },
        label = { Text("Society Contact Number (संपर्क क्रमांक)") },
        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = TealPrimary) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wizard_society_phone_input")
    )
    FieldGap()
    OutlinedTextField(
        value = email,
        onValueChange = onEmail,
        label = { Text("Society Email ID (ईमेल)") },
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TealPrimary) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wizard_society_email_input")
    )
    FieldGap()

    LogoUploadRow(logoBase64 = logoBase64, onLogo = onLogo)

    Spacer(modifier = Modifier.height(18.dp))
    SubHeading("Society Bank Account")
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = bankName,
        onValueChange = onBankName,
        label = { Text("Society Bank Name (बँकेचे नाव)") },
        leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = TealPrimary) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    FieldGap()
    OutlinedTextField(
        value = bankAccount,
        onValueChange = onBankAccount,
        label = { Text("Bank Account Number (खाते क्र.)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    FieldGap()
    // Every payment QR in the app is built from this.
    OutlinedTextField(
        value = upiId,
        onValueChange = onUpiId,
        label = { Text("UPI ID (यूपीआय आयडी)") },
        placeholder = { Text("society@okhdfcbank") },
        supportingText = {
            Text("Members' payment QR codes are generated from this UPI ID.")
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wizard_upi_id_input")
    )
    FieldGap()
    PaymentQrUploadRow(qrBase64 = paymentQrBase64, onQr = onPaymentQr)
    FieldGap()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = ifscCode,
            onValueChange = onIfsc,
            label = { Text("IFSC Code") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        if (showOpeningBalance) {
            OutlinedTextField(
                value = initialBankBalance,
                onValueChange = onInitialBalance,
                label = { Text("Initial Balance (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(modifier = Modifier.height(18.dp))
    SubHeading("Billing Start Month & Year")
    Spacer(modifier = Modifier.height(8.dp))
    MonthYearPickerField(
        label = "Society Bill Starting Month & Year",
        value = billingStartMonth,
        onValueChange = onBillingStartMonth,
        supportingText = "Billing runs from this month onwards - every bill month " +
            "list in the app starts here.",
        testTagPrefix = "wizard_billing_start"
    )
}

/** Picks an image, shrinks it and keeps it as the Base64 blob on the society doc. */
@Composable
internal fun LogoUploadRow(logoBase64: String, onLogo: (String) -> Unit) {
    val context = LocalContext.current
    var pickError by remember { mutableStateOf<String?>(null) }
    val preview = rememberBlobImage(logoBase64)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val encoded = runCatching {
            context.contentResolver.openInputStream(uri).use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream) ?: error("unsupported image")
                ImageBlob.fromBitmap(bitmap)
            }
        }
        encoded.onSuccess { onLogo(it); pickError = null }
            .onFailure { pickError = "Could not read that image. Pick a JPG or PNG." }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SlateSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (preview != null) {
                Image(
                    bitmap = preview,
                    contentDescription = "Society logo",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = TealPrimary)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { picker.launch("image/*") },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, TealPrimary),
                modifier = Modifier.testTag("wizard_logo_upload_btn")
            ) {
                Text(
                    text = if (logoBase64.isBlank()) "Upload Society Logo" else "Change Logo",
                    color = TealPrimary
                )
            }
            if (logoBase64.isNotBlank()) {
                TextButton(onClick = { onLogo("") }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            }
            pickError?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/**
 * The society's own payment QR, for banks whose code carries more than the VPA.
 *
 * A generated QR is built from the UPI id alone. Some providers issue a QR that
 * also carries a merchant id or a signature, and only that exact image will
 * resolve - so the chairman can upload theirs, and members are shown it in
 * place of the generated one.
 */
@Composable
internal fun PaymentQrUploadRow(qrBase64: String, onQr: (String) -> Unit) {
    val context = LocalContext.current
    var pickError by remember { mutableStateOf<String?>(null) }
    val preview = rememberBlobImage(qrBase64)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val encoded = runCatching {
            context.contentResolver.openInputStream(uri).use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream) ?: error("unsupported image")
                // A QR is read module by module, so it is kept at a higher
                // quality than a logo - JPEG smudging the modules is the
                // difference between a code that scans and one that does not.
                ImageBlob.fromBitmap(bitmap, quality = 95)
            }
        }
        encoded.onSuccess { onQr(it); pickError = null }
            .onFailure { pickError = "Could not read that image. Pick a JPG or PNG of the QR." }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SlateSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (preview != null) {
                Image(
                    bitmap = preview,
                    contentDescription = "Society payment QR",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Icons.Default.QrCode2, contentDescription = null, tint = TealPrimary)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { picker.launch("image/*") },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, TealPrimary),
                modifier = Modifier.testTag("wizard_payment_qr_upload_btn")
            ) {
                Text(
                    text = if (qrBase64.isBlank()) "Upload Payment QR" else "Change Payment QR",
                    color = TealPrimary
                )
            }
            Text(
                text = if (qrBase64.isBlank()) {
                    "Optional. Leave this empty and members get a QR generated from the UPI ID above."
                } else {
                    "Members will be shown this QR instead of the generated one."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (qrBase64.isNotBlank()) {
                TextButton(
                    onClick = { onQr("") },
                    modifier = Modifier.testTag("wizard_payment_qr_remove_btn")
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            }
            pickError?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// --------------------------------------------------------- step 2: unit details

@Composable
internal fun UnitDetailsStep(
    language: AppLanguage,
    totalWings: String,
    onTotalWings: (String) -> Unit,
    wingNamesRaw: String,
    onWingNamesRaw: (String) -> Unit,
    parsedWingNames: List<String>,
    /**
     * Wings that exist on the society's units but are not on the list above -
     * typed in before the wing list was set up. Shown so they can be seen and
     * tidied rather than staying invisible.
     */
    strayWings: List<String> = emptyList(),
    /** How many units carry a given wing name. */
    unitCountFor: (String) -> Int = { 0 },
    /** Renames a wing on the units themselves, not just on this form. */
    onRenameUnits: (from: String, to: String) -> Unit = { _, _ -> },
    /** Forgets a stray wing that has no units left on it. */
    onDeleteStrayWing: (String) -> Unit = { },
    /** Deletes a wing along with every unit on it. */
    onDeleteWingWithUnits: (String) -> Unit = { }
) {
    StepTitle(AppStrings.get("wizard_step2", language))

    OutlinedTextField(
        value = totalWings,
        onValueChange = { onTotalWings(it.filter { ch -> ch.isDigit() }.take(2)) },
        label = { Text("Total Wings (एकूण विंग)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wizard_total_wings_input")
    )
    FieldGap()
    OutlinedTextField(
        value = wingNamesRaw,
        onValueChange = onWingNamesRaw,
        label = { Text("Enter Wing Names (विंगची नावे)") },
        placeholder = { Text("A, B, C") },
        supportingText = {
            Text(
                if (parsedWingNames.isEmpty()) {
                    "Separate each wing name with a comma."
                } else {
                    "${parsedWingNames.size} wing(s): ${parsedWingNames.joinToString(", ")}"
                }
            )
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wizard_wing_names_input")
    )

    val count = totalWings.toIntOrNull() ?: 0
    if (count in 1..26) {
        FieldGap()
        OutlinedButton(
            onClick = { onWingNamesRaw((0 until count).joinToString(", ") { ('A' + it).toString() }) },
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, TealPrimary),
            modifier = Modifier.testTag("wizard_autofill_wings_btn")
        ) {
            Text("Auto-fill A … ${('A' + count - 1)}", color = TealPrimary)
        }
    }

    // The wings as they stand, so a name can be corrected or a wing dropped
    // without retyping the whole comma-separated line.
    if (parsedWingNames.isNotEmpty() || strayWings.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        SubHeading("Wings Added")
        Spacer(modifier = Modifier.height(6.dp))
        parsedWingNames.forEach { wing ->
            val units = unitCountFor(wing)
            WingRow(
                wing = wing,
                unitCount = units,
                isStray = false,
                onRename = { renamed ->
                    val updated = parsedWingNames.map { if (it == wing) renamed else it }
                    onWingNamesRaw(updated.joinToString(", "))
                    onTotalWings(updated.size.toString())
                    onRenameUnits(wing, renamed)
                },
                onDelete = {
                    if (units == 0) {
                        val updated = parsedWingNames.filterNot { it == wing }
                        onWingNamesRaw(updated.joinToString(", "))
                        onTotalWings(updated.size.toString())
                    } else {
                        // The wing holds flats, so removing it has to take them
                        // with it - leaving them on a wing that no longer
                        // exists is what made them unreachable in the first place.
                        onDeleteWingWithUnits(wing)
                    }
                }
            )
        }

        if (strayWings.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "These wings are on the society's units but not on the list " +
                    "above. Rename one onto a wing you keep, and its units move with it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            strayWings.forEach { wing ->
                val units = unitCountFor(wing)
                WingRow(
                    wing = wing,
                    unitCount = units,
                    isStray = true,
                    onRename = { renamed -> onRenameUnits(wing, renamed) },
                    // A stray wing with nothing on it is just a leftover name -
                    // it can go. One with units has to be renamed instead, so
                    // its flats travel with it.
                    onDelete = {
                        if (units == 0) onDeleteStrayWing(wing) else onDeleteWingWithUnits(wing)
                    }
                )
            }
        }
    }
}

/**
 * One wing in the Unit Details list.
 *
 * Renaming a wing renames it on the units that carry it too, so the society's
 * list and its flats never drift apart. A wing that is only on the units - not
 * on the list - has nothing to remove, so it offers rename alone.
 */
@Composable
private fun WingRow(
    wing: String,
    unitCount: Int,
    isStray: Boolean,
    onRename: (String) -> Unit,
    onDelete: (() -> Unit)?
) {
    var isEditing by remember(wing) { mutableStateOf(false) }
    var draft by remember(wing) { mutableStateOf(wing) }
    var isConfirmingDelete by remember(wing) { mutableStateOf(false) }

    if (isConfirmingDelete && onDelete != null) {
        AlertDialog(
            modifier = DialogWidth,
            properties = WideDialogProperties,
            onDismissRequest = { isConfirmingDelete = false },
            title = {
                Text(
                    text = "Remove Wing $wing?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = if (unitCount > 0) {
                        "Wing $wing and all $unitCount unit(s) on it will be deleted, along " +
                            "with any bills raised against them. This cannot be undone.\n\n" +
                            "To keep the flats, rename the wing instead - they move with it."
                    } else {
                        "Wing $wing is dropped from the society's wing list. No units carry it."
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(
                    onClick = { isConfirmingDelete = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_wing_$wing")
                ) {
                    Text(if (unitCount > 0) "Delete Wing & Units" else "Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { isConfirmingDelete = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .testTag("wing_row_$wing")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            if (isEditing) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.uppercase().take(24) },
                    label = { Text("Wing Name") },
                    supportingText = if (unitCount > 0) {
                        { Text("Renames $unitCount unit(s) too") }
                    } else {
                        null
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("wing_edit_input_$wing")
                )
                TextButton(
                    enabled = draft.isNotBlank(),
                    onClick = { isEditing = false; if (draft.trim() != wing) onRename(draft.trim()) }
                ) {
                    Text("Save", color = TealPrimary)
                }
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Wing $wing",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "$unitCount unit(s)" + if (isStray) " • not on the wing list" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isStray) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                IconButton(
                    onClick = { draft = wing; isEditing = true },
                    modifier = Modifier.testTag("edit_wing_btn_$wing")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename wing", tint = TealPrimary)
                }
                if (onDelete != null) {
                    IconButton(
                        onClick = { isConfirmingDelete = true },
                        modifier = Modifier.testTag("delete_wing_btn_$wing")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove wing", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------ step 3: floor inventory

@Composable
internal fun FloorInventoryStep(
    language: AppLanguage,
    wingNames: List<String>,
    wingPlans: List<WingPlan>,
    onWingPlans: (List<WingPlan>) -> Unit
) {
    StepTitle(AppStrings.get("wizard_step3", language))

    // The wing being configured: the first one still unconfigured, so finishing
    // a wing rolls straight on to the next one.
    var selectedWing by remember(wingNames) {
        mutableStateOf(wingNames.firstOrNull().orEmpty())
    }
    // Opening on a wing that is already configured - which is what the Society
    // Profile does - shows that wing's numbers rather than an empty form.
    val openingPlan = remember(wingNames, wingPlans) {
        wingPlans.firstOrNull { it.name == wingNames.firstOrNull() }
    }
    var floors by remember(openingPlan) {
        mutableStateOf(openingPlan?.totalFloors?.takeIf { it > 0 }?.toString().orEmpty())
    }
    var unitCounts by remember(openingPlan) {
        mutableStateOf(openingPlan?.unitCounts.orEmpty())
    }

    SearchableDropdownField(
        label = "Select Wing (विंग निवडा)",
        value = selectedWing,
        options = wingNames,
        onValueChange = { picked ->
            selectedWing = picked
            val existing = wingPlans.firstOrNull { it.name == picked }
            floors = existing?.totalFloors?.takeIf { it > 0 }?.toString().orEmpty()
            unitCounts = existing?.unitCounts.orEmpty()
        },
        allowFreeText = false,
        testTag = "wizard_wing_select_dropdown"
    )
    FieldGap()
    OutlinedTextField(
        value = floors,
        onValueChange = { floors = it.filter { ch -> ch.isDigit() }.take(3) },
        label = { Text("Total Floors") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wizard_total_floors_input")
    )

    Spacer(modifier = Modifier.height(16.dp))
    WingUnitCountsEditor(
        wingName = selectedWing,
        unitCounts = unitCounts,
        onUnitCounts = { unitCounts = it }
    )

    val floorCount = floors.toIntOrNull() ?: 0
    val plannedUnits = unitCounts.sumOf { it.quantity.coerceAtLeast(0) }
    val isPlanValid = selectedWing.isNotBlank() && plannedUnits > 0 &&
        unitCounts.all { it.unitType.isNotBlank() }

    if (isPlanValid) {
        val sample = WingPlan(selectedWing, floorCount, unitCounts).generateUnitNumbers()
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${sample.size} units will be created: ${sample.take(4).joinToString(", ")}" +
                if (sample.size > 4) " … ${sample.last()}" else "",
            style = MaterialTheme.typography.bodySmall,
            color = TealPrimary
        )
    }

    Spacer(modifier = Modifier.height(10.dp))
    Button(
        enabled = isPlanValid,
        onClick = {
            val updated = wingPlans.filterNot { it.name == selectedWing } +
                WingPlan(selectedWing, floorCount, unitCounts.filter { it.unitType.isNotBlank() })
            onWingPlans(updated.sortedBy { plan -> wingNames.indexOf(plan.name) })

            // Move on to the next wing that still needs configuring.
            val next = wingNames.firstOrNull { name -> updated.none { it.name == name } }
            if (next != null) {
                selectedWing = next
                floors = ""
                unitCounts = emptyList()
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wizard_save_wing_btn")
    ) {
        val remaining = wingNames.count { name -> wingPlans.none { it.name == name } && name != selectedWing }
        Text(if (remaining > 0) "Save Wing & Configure Next ($remaining left)" else "Save Wing $selectedWing")
    }

    if (wingPlans.isNotEmpty()) {
        Spacer(modifier = Modifier.height(14.dp))
        SubHeading("Configured Wings")
        Spacer(modifier = Modifier.height(6.dp))
        wingPlans.forEach { plan ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Wing ${plan.name} • ${plan.totalUnits} units",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${plan.totalFloors} floors • " +
                                plan.unitCounts.joinToString(", ") { "${it.unitType} × ${it.quantity}" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onWingPlans(wingPlans.filterNot { it.name == plan.name }) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove wing", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

/**
 * The unit types one wing holds, and how many units of each.
 *
 * This is what decides how many flats the wing is expanded into - the monthly
 * tariff for each type is set later, under Maintenance > Tariff.
 */
@Composable
internal fun WingUnitCountsEditor(
    wingName: String,
    unitCounts: List<WingUnitCount>,
    onUnitCounts: (List<WingUnitCount>) -> Unit
) {
    var newTypeName by remember { mutableStateOf("") }

    SubHeading(
        if (wingName.isBlank()) "Unit Types & Quantity" else "Wing $wingName - Unit Types & Quantity"
    )
    Text(
        text = "Which unit types this wing has, and how many units of each.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))

    unitCounts.forEachIndexed { index, count ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            OutlinedTextField(
                value = count.unitType,
                onValueChange = { renamed ->
                    onUnitCounts(
                        unitCounts.toMutableList().also { it[index] = count.copy(unitType = renamed) }
                    )
                },
                label = { Text("Unit Type") },
                singleLine = true,
                modifier = Modifier.weight(1.2f)
            )
            OutlinedTextField(
                value = if (count.quantity > 0) count.quantity.toString() else "",
                onValueChange = { raw ->
                    val quantity = raw.filter { ch -> ch.isDigit() }.take(4).toIntOrNull() ?: 0
                    onUnitCounts(
                        unitCounts.toMutableList().also { it[index] = count.copy(quantity = quantity) }
                    )
                },
                label = { Text("Quantity") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("wizard_unit_quantity_${count.unitType}")
            )
            IconButton(onClick = { onUnitCounts(unitCounts.filterIndexed { i, _ -> i != index }) }) {
                Icon(Icons.Default.Delete, contentDescription = "Remove unit type", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = newTypeName,
            onValueChange = { newTypeName = it },
            label = { Text("Add Unit Type") },
            placeholder = { Text("2 BHK") },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .testTag("wizard_new_unit_type_input")
        )
        Button(
            enabled = newTypeName.isNotBlank() &&
                unitCounts.none { it.unitType.equals(newTypeName.trim(), ignoreCase = true) },
            onClick = {
                onUnitCounts(unitCounts + WingUnitCount(newTypeName.trim(), 0))
                newTypeName = ""
            },
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("wizard_add_unit_type_btn")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add unit type")
        }
    }

    val total = unitCounts.sumOf { it.quantity.coerceAtLeast(0) }
    if (total > 0) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "$total unit(s) in this wing",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = TealPrimary,
            modifier = Modifier.testTag("wizard_wing_total_units")
        )
    }
}

// ------------------------------------------------------- step 4: office bearers

@Composable
private fun OfficeBearersStep(
    language: AppLanguage,
    name: String, onName: (String) -> Unit,
    flat: String, onFlat: (String) -> Unit,
    phone: String, onPhone: (String) -> Unit,
    email: String, onEmail: (String) -> Unit,
    pin: String, onPin: (String) -> Unit,
    confirmPin: String, onConfirmPin: (String) -> Unit
) {
    var isPinVisible by remember { mutableStateOf(false) }

    StepTitle(AppStrings.get("wizard_step4", language))

    OutlinedTextField(
        value = name,
        onValueChange = onName,
        label = { Text("Chairman Full Name (अध्यक्षांचे नाव)") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wizard_chairman_name_input")
    )
    FieldGap()
    OutlinedTextField(
        value = flat,
        onValueChange = { onFlat(it.uppercase()) },
        label = { Text("Chairman Unit No. (सदनिका क्र.)") },
        placeholder = { Text("e.g. A-101") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wizard_chairman_flat_input")
    )
    FieldGap()
    OutlinedTextField(
        value = phone,
        onValueChange = { onPhone(it.filter { ch -> ch.isDigit() }.take(10)) },
        label = { Text("Chairman Mobile (मोबाईल)") },
        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = TealPrimary) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wizard_chairman_phone_input")
    )
    FieldGap()
    OutlinedTextField(
        value = email,
        onValueChange = onEmail,
        label = { Text("Email (ईमेल)") },
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TealPrimary) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    FieldGap()
    // Exactly four digits - this is the PIN the chairman logs in with.
    OutlinedTextField(
        value = pin,
        onValueChange = { onPin(it.filter { ch -> ch.isDigit() }.take(4)) },
        label = { Text("Set 4-Digit Login PIN (लॉगिन पिन)") },
        supportingText = { Text("${pin.length}/4 digits") },
        isError = pin.isNotEmpty() && pin.length != 4,
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TealPrimary) },
        trailingIcon = {
            PinVisibilityToggle(
                isVisible = isPinVisible,
                onToggle = { isPinVisible = !isPinVisible },
                testTag = "wizard_pin_visibility_btn"
            )
        },
        visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wizard_pin_input")
    )
    FieldGap()
    OutlinedTextField(
        value = confirmPin,
        onValueChange = { onConfirmPin(it.filter { ch -> ch.isDigit() }.take(4)) },
        label = { Text("Confirm PIN (पिन पुन्हा टाका)") },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TealPrimary) },
        visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
        isError = confirmPin.isNotEmpty() && confirmPin != pin,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wizard_confirm_pin_input")
    )
}

// -------------------------------------------------------- step 5: confirmation

@Composable
private fun ConfirmationStep(
    language: AppLanguage,
    societyName: String,
    societyCode: String,
    regNumber: String,
    location: String,
    address: String,
    pinCode: String,
    societyPhone: String,
    societyEmail: String,
    logoBase64: String,
    wingNames: List<String>,
    wingPlans: List<WingPlan>,
    unitTypes: List<UnitTypeConfig>,
    plannedUnits: Int,
    chairmanName: String,
    chairmanFlat: String,
    chairmanPhone: String,
    chairmanEmail: String,
    bankName: String,
    initialBankBalance: String
) {
    StepTitle(AppStrings.get("wizard_step5", language))

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SocietySarvLogo(logoBase64 = logoBase64, size = 48)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = societyName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Code: $societyCode" + if (regNumber.isNotBlank()) " | Reg: $regNumber" else "",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            if (location.isNotBlank()) SummaryLine("Location", location)
            if (address.isNotBlank()) SummaryLine("Address", address + if (pinCode.isNotBlank()) " - $pinCode" else "")
            if (societyPhone.isNotBlank()) SummaryLine("Contact", societyPhone)
            if (societyEmail.isNotBlank()) SummaryLine("Email", societyEmail)

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            SummaryLine("Wings", wingNames.joinToString(", ").ifBlank { "-" })
            SummaryLine(
                "Floor inventory",
                wingPlans.joinToString(" | ") { plan ->
                    "${plan.name}: ${plan.totalFloors} floors, ${plan.totalUnits} units"
                }.ifBlank { "-" }
            )
            SummaryLine("Units to create", plannedUnits.toString())
            SummaryLine(
                "Unit types",
                unitTypes.filter { it.name.isNotBlank() }
                    .joinToString(", ") { type ->
                        if (type.monthlyTariff > 0) "${type.name} (₹${type.monthlyTariff.toInt()})" else type.name
                    }
                    .ifBlank { "-" }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            SummaryLine("Chairman", "$chairmanName ($chairmanFlat)")
            SummaryLine("Chairman mobile", chairmanPhone)
            if (chairmanEmail.isNotBlank()) SummaryLine("Chairman email", chairmanEmail)
            if (bankName.isNotBlank()) {
                SummaryLine("Bank", bankName + if (initialBankBalance.isNotBlank()) " (₹$initialBankBalance)" else "")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            Text(
                text = "On finish, the login details are sent to the chairman and the flats above are created in the Resident Directory.",
                style = MaterialTheme.typography.bodySmall,
                color = TealPrimary
            )
        }
    }
}

/**
 * Shown the moment the society exists. Hands the login details to the chairman
 * over SMS or email before anyone is signed in.
 */
@Composable
fun RegistrationSuccessDialog(
    society: SocietyInfo,
    chairman: Resident,
    body: String,
    language: AppLanguage,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    var sendError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        modifier = DialogWidth,
        properties = WideDialogProperties,
        onDismissRequest = onContinue,
        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TealPrimary) },
        title = {
            Text(
                text = AppStrings.get("registration_done", language),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        sendError = context.sendSms(chairman.contactNumber, body)
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, TealPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("send_chairman_sms_btn")
                ) {
                    Icon(Icons.Default.Sms, contentDescription = null, tint = TealPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(AppStrings.get("send_to_chairman_sms", language), color = TealPrimary)
                }

                if (chairman.email.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            sendError = context.sendEmail(
                                address = chairman.email,
                                subject = "SocietySarv login details - ${society.name}",
                                body = body
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, TealPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("send_chairman_email_btn")
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = TealPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(AppStrings.get("send_to_chairman_email", language), color = TealPrimary)
                    }
                }

                sendError?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.testTag("registration_continue_btn")
            ) {
                Text("Continue")
            }
        }
    )
}

// -------------------------------------------------------------- small helpers

@Composable
internal fun StepTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(14.dp))
}

@Composable
internal fun SubHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
internal fun FieldGap() {
    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(modifier = Modifier.padding(bottom = 2.dp)) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(text = value, style = MaterialTheme.typography.bodySmall)
    }
}

/** Unwraps the Activity a Compose context is hosted in, for Firebase phone auth. */
private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun android.content.Context.sendSms(number: String, body: String): String? = runCatching {
    startActivity(
        Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${number.trim()}"))
            .putExtra("sms_body", body)
    )
    null
}.getOrElse { "No messaging app available on this device." }

private fun android.content.Context.sendEmail(address: String, subject: String, body: String): String? = runCatching {
    startActivity(
        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${address.trim()}"))
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, body)
    )
    null
}.getOrElse { "No email app available on this device." }
