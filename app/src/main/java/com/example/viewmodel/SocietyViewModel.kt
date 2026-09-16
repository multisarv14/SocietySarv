package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseModule
import com.example.data.entity.Complaint
import com.example.data.entity.ComplaintCategory
import com.example.data.entity.EMERGENCY_PHONE_DIGITS
import com.example.data.entity.EmergencyContact
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.ExpenseRecord
import com.example.data.entity.FlatType
import com.example.data.entity.MaintenanceBill
import com.example.data.entity.NonFeeRevenue
import com.example.data.entity.Notice
import com.example.data.entity.NoticeCategory
import com.example.data.entity.PaymentMode
import com.example.data.entity.PaymentStatus
import com.example.data.entity.Resident
import com.example.data.entity.RevenueSource
import com.example.data.entity.SocietyInfo
import com.example.data.entity.TicketStatus
import com.example.data.entity.UnitDues
import com.example.data.entity.allocatePayment
import com.example.data.entity.billMonthKey
import com.example.data.entity.oldestMonthFirst
import com.example.data.entity.roundToPaise
import com.example.data.entity.unitDues
import com.example.data.entity.ParkingType
import com.example.data.entity.UnitTypeConfig
import com.example.data.entity.billingMonths
import com.example.data.entity.derivedStatus
import com.example.data.entity.isMember
import com.example.data.entity.isStaff
import com.example.data.entity.withProfileEdits
import com.example.data.entity.monthlyTariffFor
import com.example.data.entity.pendingAmount
import com.example.data.entity.splitMaintenance
import com.example.data.entity.treasuryBalance
import com.example.data.entity.parkingNumbers
import com.example.data.entity.payableTotal
import com.example.data.entity.UserRole
import com.example.data.entity.VisitorLog
import com.example.data.entity.VisitorPurpose
import com.example.data.entity.VisitorStatus
import com.example.data.entity.WingPlan
import com.example.data.localization.AppLanguage
import com.example.data.repository.SessionStore
import com.example.data.repository.SocietyRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

enum class NavDestination {
    DASHBOARD,
    ACCOUNTS,
    MAINTENANCE,
    RESIDENTS,
    GATE,
    COMMUNITY,
    RESIDENT_PORTAL
}

/**
 * What each role is allowed to open, in the order the bottom bar shows it.
 *
 * This is the single place menu visibility is decided:
 *  - Residents is the Chairman's alone.
 *  - Security belongs to the security team and nobody else - not residents,
 *    and not the committee either.
 *  - Every other staff role sees only the screens their job needs.
 */
fun destinationsFor(role: UserRole): List<NavDestination> = when (role) {
    UserRole.CHAIRMAN -> listOf(
        NavDestination.DASHBOARD,
        NavDestination.ACCOUNTS,
        NavDestination.MAINTENANCE,
        NavDestination.RESIDENTS,
        NavDestination.COMMUNITY
    )
    UserRole.SECRETARY, UserRole.TREASURER, UserRole.COMMITTEE -> listOf(
        NavDestination.DASHBOARD,
        NavDestination.ACCOUNTS,
        NavDestination.MAINTENANCE,
        NavDestination.COMMUNITY
    )
    /*
     * My Unit leads, because it is what a member opens the app for: their own
     * bills, their own dues, their own gate log. It is also where they land
     * after signing in, so it being the first tab keeps the bar and the landing
     * screen agreeing with each other.
     *
     * Members can open Accounts, but only to look at the society's balance -
     * see canEditAccounts. Their own bills live in My Unit, so the committee's
     * Bills screen is not part of a member's menu at all.
     */
    UserRole.RESIDENT, UserRole.TENANT -> listOf(
        NavDestination.RESIDENT_PORTAL,
        NavDestination.DASHBOARD,
        NavDestination.ACCOUNTS,
        NavDestination.COMMUNITY
    )
    // Security team: the gate terminal, plus notices and complaints.
    UserRole.GUARD -> listOf(
        NavDestination.GATE,
        NavDestination.COMMUNITY
    )
    UserRole.ACCOUNTANT, UserRole.MANAGER -> listOf(
        NavDestination.DASHBOARD,
        NavDestination.ACCOUNTS,
        NavDestination.MAINTENANCE,
        NavDestination.COMMUNITY
    )
    // Housekeeping and maintenance crew only need what they are told to do.
    UserRole.HOUSEKEEPING, UserRole.MAINTENANCE_STAFF -> listOf(
        NavDestination.COMMUNITY
    )
}

/** The office bearers who run the society, as opposed to living in it. */
private val COMMITTEE_ROLES = setOf(
    UserRole.CHAIRMAN,
    UserRole.SECRETARY,
    UserRole.TREASURER,
    UserRole.COMMITTEE,
    UserRole.MANAGER
)

data class FinancialOverview(
    val totalMaintenanceCollected: Double = 0.0,
    val totalNonFeeRevenue: Double = 0.0,
    val totalGrossIncome: Double = 0.0,
    val totalOperatingExpenses: Double = 0.0,
    val netSurplus: Double = 0.0,
    val netTreasuryBalance: Double = 0.0,
    val cashBalance: Double = 0.0,
    val bankBalance: Double = 0.0,
    val totalOutstandingDues: Double = 0.0,
    val paidFlatsCount: Int = 0,
    val pendingFlatsCount: Int = 0
)

/**
 * Everything the 5-step registration wizard collects, handed over in one go.
 *
 * The wizard is wide enough now (society details, unit details, floor
 * inventory, office bearers) that passing it as a positional argument list was
 * a bug waiting to happen.
 */
data class SocietyRegistrationRequest(
    // 1. Society Details
    val name: String,
    val regNumber: String = "",
    val code: String,
    val country: String = "India",
    val state: String = "",
    val city: String = "",
    val address: String = "",
    val pinCode: String = "",
    val contactNumber: String = "",
    val email: String = "",
    val logoBase64: String = "",
    val bankName: String = "",
    val bankAccount: String = "",
    val ifsc: String = "",
    val upiId: String = "",
    /** The society's own payment QR image, when its bank issues one. */
    val paymentQrBase64: String = "",
    val initialBankBalance: Double = 0.0,
    /** Month billing starts from, as "April 2026". */
    val billingStartMonth: String = "",
    // 2. Unit Details / 3. Floor Inventory
    val wingNames: List<String> = emptyList(),
    val wingPlans: List<WingPlan> = emptyList(),
    val unitTypes: List<UnitTypeConfig> = emptyList(),
    // 4. Office Bearers
    val chairmanName: String,
    val chairmanPhone: String,
    val chairmanEmail: String = "",
    val chairmanFlat: String,
    val chairmanPin: String
) {
    /**
     * Monthly tariff configured for a unit type, matched loosely so "1BHK",
     * "1 BHK" and "1 bhk" all land on the same slab. Falls back to [fallback]
     * when the society did not configure that type at all.
     */
    fun tariffFor(unitTypeName: String, fallback: Double): Double {
        val wanted = unitTypeName.lowercase().replace(" ", "")
        return unitTypes
            .firstOrNull { it.name.lowercase().replace(" ", "") == wanted }
            ?.monthlyTariff
            ?.takeIf { it > 0.0 }
            ?: fallback
    }
}

/** A society that was just created, waiting on the chairman's confirmation. */
data class PendingRegistration(
    val society: SocietyInfo,
    val chairman: Resident
)

class SocietyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SocietyRepository
    private val sessionStore: SessionStore

    init {
        // Give the Firestore SDK an identity before the first read, then talk to
        // Cloud Firestore for everything. There is no local seed data any more -
        // the society, its members and all ledgers live in societysarv-app-db.
        FirebaseModule.ensureSignedIn()
        repository = SocietyRepository(application)
        sessionStore = SessionStore(application)
    }

    private val _language = MutableStateFlow(AppLanguage.ENGLISH)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _currentUser = MutableStateFlow<Resident?>(null)
    val currentUser: StateFlow<Resident?> = _currentUser.asStateFlow()

    /** True while a remembered session is being checked, before the login page shows. */
    private val _isRestoringSession = MutableStateFlow(false)
    val isRestoringSession: StateFlow<Boolean> = _isRestoringSession.asStateFlow()

    /**
     * The unit the member is currently looking at. A member who owns a single
     * unit never sees this; one who owns several picks between them and every
     * unit-scoped screen follows the selection.
     */
    private val _selectedUnitNumber = MutableStateFlow<String?>(null)
    val selectedUnitNumber: StateFlow<String?> = _selectedUnitNumber.asStateFlow()

    private val _selectedTab = MutableStateFlow(NavDestination.DASHBOARD)
    val selectedTab: StateFlow<NavDestination> = _selectedTab.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    /** Set between "society registered" and the chairman acknowledging it. */
    private val _pendingRegistration = MutableStateFlow<PendingRegistration?>(null)
    val pendingRegistration: StateFlow<PendingRegistration?> = _pendingRegistration.asStateFlow()

    /**
     * Every Firestore call can fail - rejected by the security rules, offline,
     * over quota. Without a handler that exception reaches the coroutine's
     * default handler and crashes the process, so surface it to the user
     * instead and keep the app alive.
     */
    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "operation failed", throwable)
        _userMessage.value = msg(
            "क्रिया अयशस्वी. इंटरनेट तपासा आणि पुन्हा प्रयत्न करा.",
            "कार्रवाई विफल। इंटरनेट जांचें और पुनः प्रयास करें।",
            "That did not go through. Check your connection and try again."
        )
    }

    private fun safeLaunch(block: suspend CoroutineScope.() -> Unit): Job =
        viewModelScope.launch(errorHandler, block = block)

    val societyInfo: StateFlow<SocietyInfo?> = repository.societyInfo
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val residents: StateFlow<List<Resident>> = repository.allResidents
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val bills: StateFlow<List<MaintenanceBill>> = repository.allBills
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val expenses: StateFlow<List<ExpenseRecord>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val revenues: StateFlow<List<NonFeeRevenue>> = repository.allRevenues
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val visitors: StateFlow<List<VisitorLog>> = repository.allVisitors
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeVisitors: StateFlow<List<VisitorLog>> = repository.activeVisitors
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val notices: StateFlow<List<Notice>> = repository.allNotices
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val complaints: StateFlow<List<Complaint>> = repository.allComplaints
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val emergencyContacts: StateFlow<List<EmergencyContact>> = repository.emergencyContacts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Every unit the signed-in member holds.
     *
     * A member who bought two flats is registered against both unit numbers
     * under the same mobile number, so the mobile number - not the unit number -
     * is what identifies the person. Their own unit is always first in the list.
     */
    val myUnits: StateFlow<List<Resident>> = combine(residents, currentUser) { list, user ->
        if (user == null) return@combine emptyList()
        val digits = user.contactNumber.filter { it.isDigit() }.takeLast(10)
        val owned = list.filter { unit ->
            !unit.role.isStaff && (
                unit.flatNumber.equals(user.flatNumber, ignoreCase = true) ||
                    (digits.length == 10 &&
                        unit.contactNumber.filter { ch -> ch.isDigit() }.takeLast(10) == digits)
                )
        }
        val mine = owned.ifEmpty { listOf(user) }
        mine.sortedByDescending { it.flatNumber.equals(user.flatNumber, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * The complaints the signed-in user is allowed to see.
     *
     * A complaint is between the unit that raised it and the people who have to
     * act on it. Unit 304's leaking bathroom is not Unit 202's business, and
     * the list used to show it to them.
     *
     * So a member sees only tickets raised from their own units. Everyone else
     * signed in - the committee who answer for every ticket, and the staff who
     * are sent to fix them - sees the lot; the helpdesk is the whole of the
     * maintenance crew's screen, and a crew that cannot see a complaint cannot
     * be sent to it.
     */
    val visibleComplaints: StateFlow<List<Complaint>> =
        combine(complaints, myUnits, currentUser) { all, units, user ->
            val role = user?.role ?: return@combine emptyList()
            if (role != UserRole.RESIDENT && role != UserRole.TENANT) return@combine all

            val mine = units.map { it.flatNumber.uppercase() }
                .ifEmpty { listOf(user.flatNumber.uppercase()) }
            all.filter { it.flatNumber.uppercase() in mine }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** True when the member holds more than one unit and needs a unit picker. */
    val hasMultipleUnits: StateFlow<Boolean> = myUnits
        .map { it.size > 1 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * The unit every "my …" screen is scoped to: the one the member picked, or
     * their own unit when they have made no choice.
     */
    val activeUnit: StateFlow<Resident?> =
        combine(myUnits, _selectedUnitNumber, currentUser) { units, picked, user ->
            units.firstOrNull { it.flatNumber.equals(picked, ignoreCase = true) }
                ?: units.firstOrNull { it.flatNumber.equals(user?.flatNumber, ignoreCase = true) }
                ?: units.firstOrNull()
                ?: user
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectUnit(flatNumber: String) {
        _selectedUnitNumber.value = flatNumber
    }

    // Financial KPI Summary calculation
    val financialOverview: StateFlow<FinancialOverview> = combine(
        bills, expenses, revenues, societyInfo, residents
    ) { billsList, expList, revList, socInfo, residentList ->
        // Every figure below comes off the bill's own line items rather than a
        // stored total, so editing a bill moves the dashboard with it.
        val maintenanceCollected = billsList.sumOf { it.amountPaid }
        val nonFeeTotal = revList.sumOf { it.amount }
        val grossIncome = maintenanceCollected + nonFeeTotal
        val totalExpenses = expList.sumOf { it.amount }
        val netSurplus = grossIncome - totalExpenses

        val outstandingDues = billsList.sumOf { it.pendingAmount }

        val paidCount = billsList.count { it.derivedStatus == PaymentStatus.PAID }
        val pendingCount = billsList.count { it.derivedStatus != PaymentStatus.PAID }

        val treasury = socInfo.treasuryBalance(billsList, expList, revList, residentList)
        val cashBal = treasury.cash
        val bankBal = treasury.bank

        FinancialOverview(
            totalMaintenanceCollected = maintenanceCollected,
            totalNonFeeRevenue = nonFeeTotal,
            totalGrossIncome = grossIncome,
            totalOperatingExpenses = totalExpenses,
            netSurplus = netSurplus,
            netTreasuryBalance = cashBal + bankBal,
            cashBalance = cashBal,
            bankBalance = bankBal,
            totalOutstandingDues = outstandingDues,
            paidFlatsCount = paidCount,
            pendingFlatsCount = pendingCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialOverview())

    /*
     * Restoring a remembered session has to happen after every state holder
     * above exists - it writes to _currentUser and _isRestoringSession - so it
     * runs from its own init block down here rather than from the one at the
     * top of the class, where those properties are still null.
     */
    init {
        restoreSession()
    }

    fun toggleLanguage() {
        _language.value = when (_language.value) {
            AppLanguage.ENGLISH -> AppLanguage.HINDI
            AppLanguage.HINDI -> AppLanguage.MARATHI
            AppLanguage.MARATHI -> AppLanguage.ENGLISH
        }
    }

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
    }

    fun selectTab(tab: NavDestination) {
        if (!canOpen(tab)) return
        _selectedTab.value = tab
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    // Authentication
    private fun msg(mr: String, hi: String, en: String): String = when (_language.value) {
        AppLanguage.MARATHI -> mr
        AppLanguage.HINDI -> hi
        AppLanguage.ENGLISH -> en
    }

    /**
     * Signs a member in against the `residents` collection in Firestore.
     *
     * The identifier is resolved by looking the flat number up first and falling
     * back to the registered mobile number - never by guessing from the first
     * character, which used to lock out every wing beyond A/B/C.
     */
    fun login(
        societyCode: String,
        flatOrMobile: String,
        pin: String,
        rememberMe: Boolean = true,
        onResult: (Boolean, String) -> Unit
    ) {
        safeLaunch {
            val code = societyCode.trim()
            val identifier = flatOrMobile.trim()
            val enteredPin = pin.trim()

            if (code.isEmpty() || identifier.isEmpty() || enteredPin.isEmpty()) {
                onResult(false, msg(
                    "कृपया सर्व माहिती भरा.",
                    "कृपया सभी जानकारी भरें।",
                    "Please fill in every field."
                ))
                return@safeLaunch
            }

            // Find the society by its login code. Each society is its own
            // document, so any number of them can coexist.
            val society = repository.findSocietyByCode(code)
            if (society == null) {
                onResult(false, msg(
                    "हा सोसायटी कोड सापडला नाही. कोड तपासा किंवा 'नवीन सोसायटी नोंदणी' करा.",
                    "यह सोसायटी कोड नहीं मिला। कोड जांचें या 'नई सोसायटी पंजीकरण' करें।",
                    "No society found with that code. Check the code, or use 'Register New Society'."
                ))
                return@safeLaunch
            }

            // Scope every following read and write to this society.
            repository.setActiveSociety(society.societyCode)

            val resident = repository.getResidentByFlat(identifier, society.societyCode)
                ?: repository.getResidentByMobile(identifier, society.societyCode)

            if (resident == null) {
                repository.clearActiveSociety()
                onResult(false, msg(
                    "हा फ्लॅट किंवा मोबाईल क्रमांक ${society.name} मध्ये नोंदणीकृत नाही!",
                    "यह फ्लैट या मोबाइल नंबर ${society.name} में पंजीकृत नहीं है!",
                    "That unit or mobile number is not registered in ${society.name}."
                ))
                return@safeLaunch
            }

            // A staff member who has been deactivated keeps their record but
            // is not allowed back in.
            if (resident.role.isStaff && !resident.isActive) {
                repository.clearActiveSociety()
                onResult(false, msg(
                    "हे कर्मचारी खाते बंद केले आहे. अध्यक्षांशी संपर्क साधा.",
                    "यह स्टाफ खाता निष्क्रिय है। अध्यक्ष से संपर्क करें।",
                    "This staff account has been deactivated. Contact your chairman."
                ))
                return@safeLaunch
            }

            if (resident.pin.isBlank()) {
                repository.clearActiveSociety()
                onResult(false, msg(
                    "या सभासदासाठी पिन सेट केलेला नाही. समितीकडून पिन सेट करून घ्या.",
                    "इस सदस्य के लिए पिन सेट नहीं है। समिति से पिन सेट करवाएं।",
                    "No PIN is set for this member yet. Ask the committee to set one."
                ))
                return@safeLaunch
            }

            if (resident.pin != enteredPin) {
                repository.clearActiveSociety()
                onResult(false, msg("चुकीचा पिन!", "गलत पिन!", "Incorrect PIN."))
                return@safeLaunch
            }

            // Remember Me: the code and the identifier only, never the PIN.
            if (rememberMe) {
                sessionStore.save(society.societyCode, identifier)
            } else {
                sessionStore.clear()
            }

            _selectedUnitNumber.value = null
            _currentUser.value = resident
            _selectedTab.value = landingTabFor(resident.role)
            onResult(true, msg("यशस्वी प्रवेश!", "सफल लॉगिन!", "Login successful!"))
        }
    }

    /**
     * Signs a remembered member back in when the app is reopened.
     *
     * The member is re-read from Firestore every time rather than trusted from
     * the device, so a removed or deactivated account is turned away and the
     * remembered session is dropped.
     */
    private fun restoreSession() {
        val saved = sessionStore.read() ?: return
        _isRestoringSession.value = true
        safeLaunch {
            try {
                val society = repository.findSocietyByCode(saved.societyCode)
                if (society == null) {
                    sessionStore.clear()
                    return@safeLaunch
                }
                repository.setActiveSociety(society.societyCode)

                val resident = repository.getResidentByFlat(saved.identifier, society.societyCode)
                    ?: repository.getResidentByMobile(saved.identifier, society.societyCode)

                if (resident == null || resident.pin.isBlank() ||
                    (resident.role.isStaff && !resident.isActive)
                ) {
                    repository.clearActiveSociety()
                    sessionStore.clear()
                    return@safeLaunch
                }

                _currentUser.value = resident
                _selectedTab.value = landingTabFor(resident.role)
            } finally {
                _isRestoringSession.value = false
            }
        }
    }

    /** Where a role lands after signing in - always its first allowed screen. */
    private fun landingTabFor(role: UserRole): NavDestination = when (role) {
        UserRole.RESIDENT, UserRole.TENANT -> NavDestination.RESIDENT_PORTAL
        else -> destinationsFor(role).firstOrNull() ?: NavDestination.COMMUNITY
    }

    /** Guards tab switches so a role can never land on a screen it cannot open. */
    fun canOpen(destination: NavDestination): Boolean {
        val role = _currentUser.value?.role ?: return false
        if (destination in destinationsFor(role)) return true
        // My Unit is every member's own, and the gate terminal is reachable
        // from the committee's dashboard even though it is not a tab of theirs.
        return destination == NavDestination.RESIDENT_PORTAL ||
            (destination == NavDestination.GATE && role in COMMITTEE_ROLES)
    }

    fun logout() {
        repository.clearActiveSociety()
        sessionStore.clear()
        _selectedUnitNumber.value = null
        _currentUser.value = null
        _selectedTab.value = NavDestination.DASHBOARD
    }

    // Forgot PIN (OTP on the registered mobile number)

    /**
     * Step one of a PIN reset: confirm the mobile number really is registered
     * against a flat in that society, before any OTP is spent on it.
     *
     * Deliberately does not make the society active - nobody is signed in yet.
     */
    fun findMemberForPinReset(
        societyCode: String,
        mobile: String,
        onResult: (Resident?, String) -> Unit
    ) {
        safeLaunch {
            val code = societyCode.trim()
            val number = mobile.trim()
            if (code.isEmpty() || number.length < 10) {
                onResult(null, msg(
                    "सोसायटी कोड व नोंदणीकृत १० अंकी मोबाईल क्रमांक टाका.",
                    "सोसायटी कोड और पंजीकृत 10 अंकों का मोबाइल नंबर डालें।",
                    "Enter the society code and the registered 10-digit mobile number."
                ))
                return@safeLaunch
            }

            val society = repository.findSocietyByCode(code)
            if (society == null) {
                onResult(null, msg(
                    "हा सोसायटी कोड सापडला नाही.",
                    "यह सोसायटी कोड नहीं मिला।",
                    "No society found with that code."
                ))
                return@safeLaunch
            }

            val resident = repository.getResidentByMobile(number, society.societyCode)
            if (resident == null) {
                onResult(null, msg(
                    "हा मोबाईल क्रमांक ${society.name} मध्ये नोंदणीकृत नाही. अध्यक्षांशी संपर्क साधा.",
                    "यह मोबाइल नंबर ${society.name} में पंजीकृत नहीं है। अध्यक्ष से संपर्क करें।",
                    "That mobile number is not registered in ${society.name}. Contact your chairman."
                ))
                return@safeLaunch
            }

            onResult(resident, society.societyCode)
        }
    }

    /**
     * Step two: the OTP is already verified by this point, so all that is left
     * is writing the new 4-digit PIN against the member's flat.
     */
    fun resetPinAfterOtp(
        societyCode: String,
        resident: Resident,
        newPin: String,
        onResult: (Boolean, String) -> Unit
    ) {
        safeLaunch {
            val pin = newPin.trim()
            if (pin.length != 4 || pin.any { !it.isDigit() }) {
                onResult(false, msg(
                    "पिन नेमका ४ अंकी असावा.",
                    "पिन ठीक 4 अंकों का होना चाहिए।",
                    "The PIN must be exactly 4 digits."
                ))
                return@safeLaunch
            }
            runCatching { repository.resetResidentPin(societyCode, resident, pin) }
                .onSuccess {
                    onResult(true, msg(
                        "नवीन पिन सेट झाला! आता ${resident.flatNumber} व नवीन पिनने लॉगिन करा.",
                        "नया पिन सेट हो गया! अब ${resident.flatNumber} और नए पिन से लॉगिन करें।",
                        "New PIN set. Log in with ${resident.flatNumber} and your new PIN."
                    ))
                }
                .onFailure { e ->
                    Log.e(TAG, "PIN reset failed", e)
                    onResult(false, msg(
                        "पिन बदलता आला नाही. पुन्हा प्रयत्न करा.",
                        "पिन नहीं बदल सका। दोबारा प्रयास करें।",
                        "Could not change the PIN. Please try again."
                    ))
                }
        }
    }

    // Society Registration Wizard
    /**
     * Creates the society master document and its first member (the chairman)
     * in Firestore, then signs that chairman straight in. The PIN entered in the
     * wizard is the one the chairman logs in with afterwards.
     */
    fun registerNewSociety(
        request: SocietyRegistrationRequest,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        safeLaunch {
            val societyCode = request.code.trim().uppercase()
            val flatNumber = request.chairmanFlat.trim().uppercase()
            val pin = request.chairmanPin.trim()

            if (request.name.isBlank() || societyCode.isBlank() || request.chairmanName.isBlank() ||
                flatNumber.isBlank() || request.chairmanPhone.isBlank() || pin.isBlank()
            ) {
                onResult(false, msg(
                    "कृपया सर्व आवश्यक माहिती भरा.",
                    "कृपया सभी आवश्यक जानकारी भरें।",
                    "Please fill in every required field."
                ))
                return@safeLaunch
            }

            if (pin.length != 4 || pin.any { !it.isDigit() }) {
                onResult(false, msg(
                    "लॉगिन पिन नेमका ४ अंकी असावा.",
                    "लॉगिन पिन ठीक 4 अंकों का होना चाहिए।",
                    "The login PIN must be exactly 4 digits."
                ))
                return@safeLaunch
            }

            // Any number of societies can exist side by side; the login code is
            // what tells them apart, so it has to be unique.
            val clash = repository.findSocietyByCode(societyCode)
            if (clash != null) {
                onResult(false, msg(
                    "कोड $societyCode आधीच '${clash.name}' साठी वापरला जातो. दुसरा कोड निवडा.",
                    "कोड $societyCode पहले से '${clash.name}' के लिए उपयोग में है। दूसरा कोड चुनें।",
                    "Code $societyCode is already used by '${clash.name}'. Please pick a different code."
                ))
                return@safeLaunch
            }

            val wingPlans = request.wingPlans.filter { it.totalUnits > 0 }
            val generatedUnits = wingPlans.flatMap { plan -> plan.toVacantUnits() }

            val newSoc = SocietyInfo(
                name = request.name.trim(),
                registrationNumber = request.regNumber.trim(),
                societyCode = societyCode,
                address = request.address.trim(),
                totalWings = request.wingNames.size.coerceAtLeast(wingPlans.size),
                totalFlats = generatedUnits.size,
                bankAccountName = request.bankName.trim(),
                bankAccountNumber = request.bankAccount.trim(),
                ifscCode = request.ifsc.trim().uppercase(),
                upiId = request.upiId.trim(),
                paymentQrBase64 = request.paymentQrBase64,
                currentBalanceCash = 0.0,
                currentBalanceBank = request.initialBankBalance,
                tariff1BHK = request.tariffFor("1 BHK", 1500.0),
                tariff2BHK = request.tariffFor("2 BHK", 2200.0),
                tariff3BHK = request.tariffFor("3 BHK", 3000.0),
                tariffPenthouse = request.tariffFor("Penthouse", request.tariffFor("3 BHK", 3000.0) * 1.5),
                tariffShop = request.tariffFor("Shop", 3500.0),
                logoBase64 = request.logoBase64,
                country = request.country.trim(),
                state = request.state.trim(),
                city = request.city.trim(),
                pinCode = request.pinCode.trim(),
                contactNumber = request.contactNumber.trim(),
                email = request.email.trim(),
                wingNames = request.wingNames,
                wingPlans = wingPlans,
                unitTypes = request.unitTypes,
                billingStartMonth = request.billingStartMonth.trim()
            )
            repository.insertOrUpdateSociety(newSoc)
            repository.setActiveSociety(societyCode)

            // Floor inventory first: the flats land in the Resident Directory as
            // vacant units, ready for the committee to fill in owners against.
            repository.insertVacantUnits(societyCode, generatedUnits)

            val chairman = Resident(
                flatNumber = flatNumber,
                wing = wingOf(flatNumber, request.wingNames),
                ownerName = request.chairmanName.trim(),
                contactNumber = request.chairmanPhone.trim(),
                email = request.chairmanEmail.trim(),
                role = UserRole.CHAIRMAN,
                pin = pin,
                flatType = FlatType.THREE_BHK,
                balanceDue = 0.0
            )
            // Written after the vacant units so the chairman's own flat keeps
            // his details even when floor inventory generated that same number.
            repository.insertResident(chairman, societyCode)

            // The registration notice to head office. Queued only now that the
            // society and its chairman are actually written, and never allowed
            // to fail the registration - a society is registered whether or not
            // the notification goes out.
            runCatching {
                queueRegistrationEmail(newSoc, chairman)
            }.onFailure { Log.w(TAG, "registration email not queued", it) }

            // 5. Confirmation - a standing record of the login details inside
            // the society itself, on top of the SMS/email the wizard offers.
            runCatching {
                repository.insertNotice(
                    Notice(
                        title = "Society registered on SocietySarv",
                        content = registrationConfirmationText(newSoc, chairman),
                        publishedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()),
                        isPinned = true,
                        category = NoticeCategory.GENERAL,
                        authorRole = "SocietySarv"
                    )
                )
            }.onFailure { Log.w(TAG, "confirmation notice failed", it) }

            // The chairman is not signed in yet on purpose: the wizard's
            // Confirmation step needs to stay on screen long enough to hand the
            // login details over. completeRegistrationSignIn() finishes the job.
            _pendingRegistration.value = PendingRegistration(
                society = newSoc,
                chairman = repository.getResidentByFlat(flatNumber, societyCode) ?: chairman
            )

            val unitsNote = if (generatedUnits.isEmpty()) "" else " ${generatedUnits.size} units created."
            onResult(true, msg(
                "सोसायटी नोंदणी यशस्वी! लॉगिनसाठी कोड $societyCode व फ्लॅट $flatNumber वापरा.$unitsNote",
                "सोसायटी पंजीकरण सफल! लॉगिन के लिए कोड $societyCode और फ्लैट $flatNumber उपयोग करें।$unitsNote",
                "Society registered! Log in with code $societyCode and flat $flatNumber.$unitsNote"
            ))
        }
    }

    /**
     * Signs the chairman in once they have acknowledged the confirmation
     * screen. A no-op if nothing was just registered.
     */
    fun completeRegistrationSignIn() {
        val pending = _pendingRegistration.value ?: return
        sessionStore.save(pending.society.societyCode, pending.chairman.flatNumber)
        _selectedUnitNumber.value = null
        _currentUser.value = pending.chairman
        _selectedTab.value = NavDestination.DASHBOARD
        _pendingRegistration.value = null
    }

    fun clearPendingRegistration() {
        _pendingRegistration.value = null
    }

    /** Wing a flat belongs to: the configured wing it is prefixed with, else its first token. */
    private fun wingOf(flatNumber: String, wingNames: List<String>): String =
        wingNames.firstOrNull { flatNumber.startsWith("${it.uppercase()}-", ignoreCase = true) }
            ?: flatNumber.substringBefore("-").take(2).ifBlank { "A" }

    /**
     * Expands one wing into the vacant units it plans for.
     *
     * The unit numbers are spread over the floors and the types are handed out
     * in the order they were entered, so the two lists line up one for one.
     */
    private fun WingPlan.toVacantUnits(): List<Resident> {
        val numbers = generateUnitNumbers()
        val types = unitTypesInOrder()
        return numbers.mapIndexed { index, unitNumber ->
            val type = types.getOrElse(index) { "" }
            Resident(
                flatNumber = unitNumber,
                wing = name,
                ownerName = "",
                contactNumber = "",
                role = UserRole.RESIDENT,
                pin = "",
                unitType = type,
                flatType = flatTypeFor(type),
                balanceDue = 0.0
            )
        }
    }

    /** Best-effort mapping of a free-text unit type onto the built-in slabs. */
    private fun flatTypeFor(unitTypeName: String): FlatType {
        val name = unitTypeName.lowercase().replace(" ", "")
        return when {
            name.contains("1bhk") || name.contains("1rk") -> FlatType.ONE_BHK
            name.contains("3bhk") -> FlatType.THREE_BHK
            name.contains("penthouse") -> FlatType.PENTHOUSE
            name.contains("shop") || name.contains("office") ||
                name.contains("commercial") -> FlatType.COMMERCIAL_SHOP
            else -> FlatType.TWO_BHK
        }
    }

    /**
     * Sends head office the details of a society that has just registered.
     *
     * The message is queued into the `mail` collection for the Trigger Email
     * worker to deliver; the app holds no mail credentials of its own. The
     * queue is keyed by the society code, so the same registration can never
     * post the message twice.
     */
    private suspend fun queueRegistrationEmail(society: SocietyInfo, chairman: Resident) {
        val registeredOn = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date())
        val subject = "New Society Registered - ${society.name}"

        val details = listOf(
            "Society Name" to society.name,
            "Society Code" to society.societyCode,
            "Chairman Name" to chairman.ownerName,
            "Chairman Mobile Number" to chairman.contactNumber,
            "Registration Date" to registeredOn,
            "Registration No." to society.registrationNumber.ifBlank { "-" },
            "Address" to listOf(society.address, society.city, society.state, society.pinCode)
                .filter { it.isNotBlank() }.joinToString(", ").ifBlank { "-" },
            "Contact" to society.contactNumber.ifBlank { "-" },
            "Email" to society.email.ifBlank { "-" },
            "Wings" to society.wingNames.joinToString(", ").ifBlank { society.totalWings.toString() },
            "Units Created" to society.totalFlats.toString()
        )

        val text = buildString {
            appendLine("Dear Team,")
            appendLine()
            appendLine("A new society has been successfully registered in the system.")
            appendLine()
            appendLine("Society Details:")
            details.forEach { (label, value) -> appendLine("$label: $value") }
            appendLine()
            appendLine("Please find the complete society registration details in the system.")
            appendLine()
            appendLine("Regards,")
            append("MultiSarv System")
        }

        val html = buildString {
            append("<p>Dear Team,</p>")
            append("<p>A new society has been successfully registered in the system.</p>")
            append("<p><strong>Society Details:</strong></p><ul>")
            details.forEach { (label, value) ->
                append("<li><strong>$label:</strong> ${escapeHtml(value)}</li>")
            }
            append("</ul>")
            append("<p>Please find the complete society registration details in the system.</p>")
            append("<p>Regards,<br/>MultiSarv System</p>")
        }

        repository.queueSocietyRegistrationEmail(
            societyCode = society.societyCode,
            from = REGISTRATION_EMAIL_FROM,
            to = REGISTRATION_EMAIL_TO,
            subject = subject,
            bodyText = text,
            bodyHtml = html
        )
    }

    /** Keeps a society's own name or address from breaking the HTML body. */
    private fun escapeHtml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    /**
     * The confirmation the chairman receives - also what the "Send to Chairman"
     * buttons on the success screen put into the SMS / email body.
     */
    fun registrationConfirmationText(society: SocietyInfo, chairman: Resident): String = buildString {
        appendLine("SocietySarv - Society Registered")
        appendLine("--------------------------------")
        appendLine("Society : ${society.name}")
        if (society.registrationNumber.isNotBlank()) appendLine("Reg. No : ${society.registrationNumber}")
        appendLine("Login Code : ${society.societyCode}")
        appendLine("Chairman : ${chairman.ownerName} (Unit ${chairman.flatNumber})")
        appendLine("Mobile : ${chairman.contactNumber}")
        appendLine("Wings : ${society.wingNames.joinToString(", ").ifBlank { society.totalWings.toString() }}")
        appendLine("Units created : ${society.totalFlats}")
        appendLine("--------------------------------")
        appendLine("Log in with:")
        appendLine("  Society Login Code : ${society.societyCode}")
        appendLine("  Unit / Mobile      : ${chairman.flatNumber}")
        appendLine("  4-Digit Login PIN  : the PIN set during registration")
        appendLine("Keep these details safe.")
    }

    // Record Maintenance Collection
    /**
     * Receipts a payment against one bill.
     *
     * The bill takes only what it is owed; anything over that is not forced
     * onto it but held as the unit's advance credit, which the next bill raised
     * for the unit is settled from automatically. Overpaying used to leave a
     * bill showing more collected than it ever charged, and the money had
     * nowhere to go at the start of the next month.
     */
    fun recordMaintenancePayment(
        bill: MaintenanceBill,
        amount: Double,
        mode: PaymentMode,
        ref: String,
        dateStr: String,
        screenshotBase64: String = ""
    ) {
        safeLaunch {
            val now = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            val receiptNo = newReceiptNumber()
            // Settle against what the bill actually comes to (charges + late fee),
            // not a stored total that a later edit could have left stale.
            val appliedToBill = roundToPaise(minOf(amount.coerceAtLeast(0.0), bill.pendingAmount))
            val excess = roundToPaise(amount.coerceAtLeast(0.0) - appliedToBill)
            val paidSoFar = roundToPaise(bill.amountPaid + appliedToBill)
            val updatedBill = bill.copy(
                amountPaid = paidSoFar,
                totalAmount = bill.payableTotal,
                status = if (paidSoFar >= bill.payableTotal - 0.009) PaymentStatus.PAID else PaymentStatus.PARTIAL,
                paidDate = dateStr.ifBlank { now },
                paymentMode = mode,
                transactionRef = ref,
                receiptNumber = receiptNo,
                receiptImageBase64 = screenshotBase64
            )
            repository.updateBill(updatedBill)

            // Update resident dues, and park any overpayment as advance credit.
            val res = repository.getResidentByFlat(bill.flatNumber)
            if (res != null) {
                repository.updateResident(
                    res.copy(
                        balanceDue = (res.balanceDue - appliedToBill).coerceAtLeast(0.0),
                        advanceBalance = roundToPaise(res.advanceBalance + excess),
                        advanceMode = if (excess > 0.009) mode else res.advanceMode
                    )
                )
            }

            val carried = if (excess > 0.009) {
                msg(
                    " ₹%,.2f जास्तीची रक्कम पुढील बिलात वजा होईल.".format(excess),
                    " ₹%,.2f अतिरिक्त राशि अगले बिल में समायोजित होगी।".format(excess),
                    " ₹%,.2f extra carried forward to the next bill.".format(excess)
                )
            } else {
                ""
            }
            showMessage(
                msg(
                    "पावती क्र. $receiptNo तयार झाली!$carried",
                    "रसीद क्र. $receiptNo तैयार हुई!$carried",
                    "Receipt #$receiptNo generated.$carried"
                )
            )
        }
    }

    /**
     * Receipts one payment against however many months it covers.
     *
     * A member who owes five months and hands over part of it should not cost
     * the chairman five separate entries. The amount is spread over the unit's
     * pending bills oldest month first, every bill it touches carries the same
     * receipt number, whatever it does not reach stays pending, and anything
     * left over becomes advance credit against the next bill.
     */
    fun recordBulkPayment(
        flatNumber: String,
        amount: Double,
        mode: PaymentMode,
        ref: String,
        dateStr: String,
        screenshotBase64: String = "",
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (!canEditAccounts) {
            onResult(false, accountsReadOnlyMessage())
            return
        }
        if (amount <= 0.0) {
            onResult(false, msg(
                "रक्कम शून्यापेक्षा जास्त असावी.",
                "राशि शून्य से अधिक होनी चाहिए।",
                "Enter an amount greater than zero."
            ))
            return
        }
        val pending = pendingBillsFor(flatNumber)
        if (pending.isEmpty() && amount > 0.0) {
            onResult(false, msg(
                "$flatNumber ची सर्व बिले आधीच भरली आहेत.",
                "$flatNumber के सभी बिल पहले ही भरे गए हैं।",
                "$flatNumber has no pending bills to settle."
            ))
            return
        }
        safeLaunch {
            val now = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            val receiptNo = newReceiptNumber()
            val paidOn = dateStr.ifBlank { now }
            val allocation = allocatePayment(pending, amount)

            allocation.touched.forEach { line ->
                val paidSoFar = roundToPaise(line.bill.amountPaid + line.applied)
                repository.updateBill(
                    line.bill.copy(
                        amountPaid = paidSoFar,
                        totalAmount = line.bill.payableTotal,
                        status = if (line.isSettled) PaymentStatus.PAID else PaymentStatus.PARTIAL,
                        paidDate = paidOn,
                        paymentMode = mode,
                        transactionRef = ref,
                        receiptNumber = receiptNo,
                        receiptImageBase64 = screenshotBase64
                    )
                )
            }

            val res = repository.getResidentByFlat(flatNumber)
            if (res != null) {
                repository.updateResident(
                    res.copy(
                        balanceDue = (res.balanceDue - allocation.applied).coerceAtLeast(0.0),
                        advanceBalance = roundToPaise(res.advanceBalance + allocation.excess),
                        advanceMode = if (allocation.excess > 0.009) mode else res.advanceMode
                    )
                )
            }

            val monthsCovered = allocation.touched.size
            val stillOwed = roundToPaise(
                allocation.allocations.sumOf { it.pendingAfter }
            )
            val tail = buildString {
                if (stillOwed > 0.009) {
                    append(
                        msg(
                            " ₹%,.2f अजून थकीत आहे.".format(stillOwed),
                            " ₹%,.2f अब भी बकाया है।".format(stillOwed),
                            " ₹%,.2f still pending.".format(stillOwed)
                        )
                    )
                }
                if (allocation.excess > 0.009) {
                    append(
                        msg(
                            " ₹%,.2f पुढील बिलात वजा होईल.".format(allocation.excess),
                            " ₹%,.2f अगले बिल में समायोजित होगी।".format(allocation.excess),
                            " ₹%,.2f carried forward to the next bill.".format(allocation.excess)
                        )
                    )
                }
            }
            onResult(true, msg(
                "पावती $receiptNo - $monthsCovered महिन्यांसाठी ₹%,.2f नोंदवले.$tail".format(allocation.applied),
                "रसीद $receiptNo - $monthsCovered महीनों के लिए ₹%,.2f दर्ज किया।$tail".format(allocation.applied),
                "Receipt $receiptNo - ₹%,.2f recorded across $monthsCovered month(s).$tail".format(allocation.applied)
            ))
        }
    }

    /** A unit's bills that still owe something, oldest month first. */
    fun pendingBillsFor(flatNumber: String): List<MaintenanceBill> =
        bills.value
            .filter { it.flatNumber.equals(flatNumber, ignoreCase = true) && it.pendingAmount > 0.009 }
            .oldestMonthFirst()

    /** Advance credit sitting against a unit, from an earlier overpayment. */
    fun advanceCreditFor(flatNumber: String): Double =
        residents.value
            .firstOrNull { it.flatNumber.equals(flatNumber, ignoreCase = true) }
            ?.advanceBalance
            ?.coerceAtLeast(0.0)
            ?: 0.0

    /** What one unit owes: arrears, this month, and the credit set against them. */
    fun duesFor(flatNumber: String): UnitDues = unitDues(
        bills = bills.value.filter { it.flatNumber.equals(flatNumber, ignoreCase = true) },
        excessCredit = advanceCreditFor(flatNumber)
    )

    private fun newReceiptNumber(): String =
        "REC-${SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())}-" +
            "${Random.nextInt(1000, 9999)}"

    /**
     * Corrects a receipt that has already been taken.
     *
     * The amount replaces what was recorded rather than adding to it - this is
     * the same receipt being restated, not a second payment - so the member's
     * dues move by the difference alone.
     */
    fun updateRecordedPayment(
        bill: MaintenanceBill,
        amount: Double,
        mode: PaymentMode,
        ref: String,
        dateStr: String,
        screenshotBase64: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (!canEditAccounts) {
            onResult(false, accountsReadOnlyMessage())
            return
        }
        if (amount <= 0.0) {
            onResult(false, msg(
                "रक्कम शून्यापेक्षा जास्त असावी.",
                "राशि शून्य से अधिक होनी चाहिए।",
                "Enter an amount greater than zero."
            ))
            return
        }
        safeLaunch {
            // The bill takes only what it charged; a restatement that overshoots
            // it carries forward as credit exactly as a fresh payment would,
            // rather than sitting on the bill as money it never billed for.
            val onBill = roundToPaise(minOf(amount, bill.payableTotal))
            val excess = roundToPaise(amount - onBill)
            val updated = bill.copy(
                amountPaid = onBill,
                totalAmount = bill.payableTotal,
                status = if (onBill >= bill.payableTotal - 0.009) PaymentStatus.PAID else PaymentStatus.PARTIAL,
                paidDate = dateStr.ifBlank { bill.paidDate },
                paymentMode = mode,
                transactionRef = ref,
                receiptImageBase64 = screenshotBase64
            )
            repository.updateBill(updated)

            val delta = onBill - bill.amountPaid
            val res = repository.getResidentByFlat(bill.flatNumber)
            if (res != null && (delta != 0.0 || excess > 0.009)) {
                repository.updateResident(
                    res.copy(
                        balanceDue = (res.balanceDue - delta).coerceAtLeast(0.0),
                        advanceBalance = roundToPaise(res.advanceBalance + excess),
                        advanceMode = if (excess > 0.009) mode else res.advanceMode
                    )
                )
            }
            onResult(true, msg(
                "पावती ${bill.receiptNumber} अद्ययावत केली!",
                "रसीद ${bill.receiptNumber} अपडेट की गई!",
                "Receipt ${bill.receiptNumber} updated."
            ))
        }
    }

    /**
     * Withdraws a receipt, putting the bill back to unpaid.
     *
     * The money comes off the bill and back onto the member's dues, so the
     * treasury - which is worked out from what bills say was paid - follows on
     * its own. The bill itself stays; only the payment against it is undone.
     */
    fun deleteReceipt(bill: MaintenanceBill, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        if (!canEditAccounts) {
            onResult(false, accountsReadOnlyMessage())
            return
        }
        if (bill.amountPaid <= 0.0) {
            onResult(false, msg(
                "या बिलावर पावती नाही.",
                "इस बिल पर रसीद नहीं है।",
                "There is no receipt against this bill."
            ))
            return
        }
        safeLaunch {
            val reversed = bill.amountPaid
            repository.updateBill(
                bill.copy(
                    amountPaid = 0.0,
                    status = PaymentStatus.UNPAID,
                    paidDate = "",
                    paymentMode = null,
                    transactionRef = "",
                    receiptNumber = "",
                    receiptImageBase64 = ""
                )
            )
            val res = repository.getResidentByFlat(bill.flatNumber)
            if (res != null) {
                repository.updateResident(res.copy(balanceDue = res.balanceDue + reversed))
            }
            onResult(true, msg(
                "पावती रद्द केली. बिल पुन्हा थकीत झाले.",
                "रसीद हटाई गई। बिल फिर से बकाया है।",
                "Receipt deleted. The bill is unpaid again."
            ))
        }
    }

    // Add new Maintenance Bill
    /**
     * Raises one month's bill for a unit.
     *
     * Any advance the unit is holding - money it paid over and above what it
     * owed - is set against the new bill as it is created, so an overpayment
     * lands on the very next month rather than waiting for somebody to remember
     * it.
     */
    fun generateBill(
        flatNumber: String,
        residentName: String,
        month: String,
        baseAmount: Double,
        water: Double,
        sinking: Double,
        repair: Double,
        lateFee: Double,
        dueDate: String
    ) {
        safeLaunch {
            val total = baseAmount + water + sinking + repair + lateFee
            val res = repository.getResidentByFlat(flatNumber)
            val credit = res?.advanceBalance?.coerceAtLeast(0.0) ?: 0.0
            val fromAdvance = roundToPaise(minOf(credit, total))

            repository.insertBill(
                MaintenanceBill(
                    flatNumber = flatNumber,
                    residentName = residentName,
                    billMonth = month,
                    baseAmount = baseAmount,
                    waterCharges = water,
                    sinkingFund = sinking,
                    repairFund = repair,
                    lateFeeAmount = lateFee,
                    totalAmount = total,
                    amountPaid = fromAdvance,
                    status = when {
                        fromAdvance <= 0.009 -> PaymentStatus.UNPAID
                        fromAdvance >= total - 0.009 -> PaymentStatus.PAID
                        else -> PaymentStatus.PARTIAL
                    },
                    dueDate = dueDate,
                    paidDate = if (fromAdvance > 0.009) {
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                    } else {
                        ""
                    },
                    paymentMode = if (fromAdvance > 0.009) res?.advanceMode else null,
                    transactionRef = if (fromAdvance > 0.009) ADVANCE_ADJUSTMENT_REF else "",
                    receiptNumber = "BILL-${SimpleDateFormat("yyMM", Locale.getDefault()).format(Date())}-${Random.nextInt(100, 999)}"
                )
            )

            if (res != null) {
                repository.updateResident(
                    res.copy(
                        balanceDue = (res.balanceDue + total - fromAdvance).coerceAtLeast(0.0),
                        advanceBalance = roundToPaise(credit - fromAdvance)
                    )
                )
            }

            val adjusted = if (fromAdvance > 0.009) {
                msg(
                    " मागील ₹%,.2f जमा रक्कम वजा केली.".format(fromAdvance),
                    " पिछली ₹%,.2f जमा राशि समायोजित की।".format(fromAdvance),
                    " ₹%,.2f of advance credit adjusted against it.".format(fromAdvance)
                )
            } else {
                ""
            }
            showMessage(msg(
                "$flatNumber साठी मासिक बिल तयार केले!$adjusted",
                "$flatNumber के लिए मासिक बिल बनाया!$adjusted",
                "Maintenance bill created for $flatNumber.$adjusted"
            ))
        }
    }


    /**
     * One unit-and-month that has no bill against it yet.
     *
     * Used to tell the committee how much catching up there is before they ask
     * for it, and then to do exactly that work.
     */
    data class MissingBill(val unit: Resident, val month: String)

    /**
     * Every bill that ought to exist but does not: each billable unit, for each
     * month from the society's billing start up to the current one.
     *
     * Only units that have a member behind them are counted - a vacant flat has
     * nobody to bill.
     */
    fun pendingBillRun(): List<MissingBill> {
        val society = societyInfo.value ?: return emptyList()
        val months = society.billingMonths()
        if (months.isEmpty()) return emptyList()

        val billable = residents.value.filter { !it.role.isStaff && it.ownerName.isNotBlank() }
        val alreadyBilled = bills.value
            .map { it.flatNumber.uppercase() to it.billMonth.lowercase() }
            .toSet()

        return billable.flatMap { unit ->
            months.filterNot { month ->
                (unit.flatNumber.uppercase() to month.lowercase()) in alreadyBilled
            }.map { month -> MissingBill(unit, month) }
        }
    }

    /**
     * Raises every bill [pendingBillRun] found, in one go.
     *
     * Each bill is worked out exactly as the Generate Bill sheet would: the
     * unit's own tariff, divided by the society's configured percentages. Bills
     * that already exist are never touched, so running this twice is harmless.
     */
    fun generateMissingBills(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        if (!canEditAccounts) {
            onResult(false, accountsReadOnlyMessage())
            return
        }
        val society = societyInfo.value
        if (society == null) {
            onResult(false, "Society not loaded yet.")
            return
        }
        val outstanding = pendingBillRun()
        if (outstanding.isEmpty()) {
            onResult(false, msg(
                "सर्व युनिट्सची बिले आधीच तयार आहेत.",
                "सभी इकाइयों के बिल पहले से तैयार हैं।",
                "Every unit is already billed for every month."
            ))
            return
        }
        safeLaunch {
            // Dues are added up per unit and written once, rather than once per
            // bill, so a long run does not hammer the same document.
            val duesAdded = mutableMapOf<String, Double>()
            var raised = 0

            /*
             * Advance credit a unit is holding is spent on the oldest month it
             * is billed for, then what is left carries on to the next - exactly
             * as a payment would be spread. The remainder is written back once,
             * after the whole run.
             */
            val creditLeft = mutableMapOf<String, Double>()
            val now = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

            outstanding.sortedBy { billMonthKey(it.month) }.forEach { missing ->
                val maintenance = society.monthlyTariffFor(missing.unit)
                if (maintenance <= 0.0) return@forEach
                val split = society.splitMaintenance(maintenance)
                val flat = missing.unit.flatNumber
                val credit = creditLeft.getOrPut(flat) { missing.unit.advanceBalance.coerceAtLeast(0.0) }
                val fromAdvance = roundToPaise(minOf(credit, split.total))
                creditLeft[flat] = roundToPaise(credit - fromAdvance)

                repository.insertBill(
                    MaintenanceBill(
                        flatNumber = flat,
                        residentName = missing.unit.ownerName,
                        billMonth = missing.month,
                        baseAmount = split.other,
                        waterCharges = split.water,
                        sinkingFund = split.sinking,
                        repairFund = split.repair,
                        totalAmount = split.total,
                        amountPaid = fromAdvance,
                        status = when {
                            fromAdvance <= 0.009 -> PaymentStatus.UNPAID
                            fromAdvance >= split.total - 0.009 -> PaymentStatus.PAID
                            else -> PaymentStatus.PARTIAL
                        },
                        dueDate = dueDateFor(missing.month),
                        paidDate = if (fromAdvance > 0.009) now else "",
                        paymentMode = if (fromAdvance > 0.009) missing.unit.advanceMode else null,
                        transactionRef = if (fromAdvance > 0.009) ADVANCE_ADJUSTMENT_REF else "",
                        receiptNumber = "BILL-${SimpleDateFormat("yyMM", Locale.getDefault()).format(Date())}-${Random.nextInt(100, 999)}"
                    )
                )
                duesAdded[flat] = (duesAdded[flat] ?: 0.0) + split.total - fromAdvance
                raised++
            }

            duesAdded.forEach { (flatNumber, added) ->
                val res = repository.getResidentByFlat(flatNumber)
                if (res != null) {
                    repository.updateResident(
                        res.copy(
                            balanceDue = (res.balanceDue + added).coerceAtLeast(0.0),
                            advanceBalance = creditLeft[flatNumber] ?: res.advanceBalance
                        )
                    )
                }
            }

            val skipped = outstanding.size - raised
            val note = if (skipped > 0) {
                msg(
                    " $skipped युनिट्सना दर नसल्याने वगळले.",
                    " $skipped इकाइयां दर न होने से छोड़ी गईं।",
                    " $skipped skipped - no tariff set for their unit type."
                )
            } else {
                ""
            }
            onResult(true, msg(
                "$raised बिले तयार केली!$note",
                "$raised बिल तैयार किए गए!$note",
                "Raised $raised bill(s).$note"
            ))
        }
    }

    /** The 10th of the billed month, the usual society due date. */
    private fun dueDateFor(billMonth: String): String = runCatching {
        val parsed = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).parse(billMonth)
            ?: return@runCatching billMonth
        val calendar = java.util.Calendar.getInstance().apply {
            time = parsed
            set(java.util.Calendar.DAY_OF_MONTH, 10)
        }
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(calendar.time)
    }.getOrDefault(billMonth)

    /**
     * Rewrites an existing bill's line items.
     *
     * Payments already recorded against the bill are left alone - only what is
     * charged changes - and the member's outstanding balance is moved by the
     * difference so the directory and the ledger stay in step.
     */
    fun updateBill(
        original: MaintenanceBill,
        month: String,
        baseAmount: Double,
        water: Double,
        sinking: Double,
        repair: Double,
        lateFee: Double,
        dueDate: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (!canEditAccounts) {
            onResult(false, accountsReadOnlyMessage())
            return
        }
        safeLaunch {
            val charges = baseAmount + water + sinking + repair
            val payable = charges + lateFee
            val updated = original.copy(
                billMonth = month,
                baseAmount = baseAmount,
                waterCharges = water,
                sinkingFund = sinking,
                repairFund = repair,
                parkingCharges = 0.0,
                lateFeeAmount = lateFee,
                totalAmount = payable,
                dueDate = dueDate,
                status = when {
                    original.amountPaid <= 0.0 -> PaymentStatus.UNPAID
                    original.amountPaid >= payable - 0.009 -> PaymentStatus.PAID
                    else -> PaymentStatus.PARTIAL
                }
            )
            repository.updateBill(updated)

            val delta = payable - original.payableTotal
            val res = repository.getResidentByFlat(original.flatNumber)
            if (res != null && delta != 0.0) {
                repository.updateResident(
                    res.copy(balanceDue = (res.balanceDue + delta).coerceAtLeast(0.0))
                )
            }
            onResult(true, msg(
                "${original.flatNumber} चे $month चे बिल अद्ययावत केले!",
                "${original.flatNumber} का $month का बिल अपडेट किया गया!",
                "Bill for ${original.flatNumber} ($month) updated."
            ))
        }
    }

    /** Removes a bill and takes what it charged back off the member's dues. */
    fun deleteBill(bill: MaintenanceBill, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        if (!canEditAccounts) {
            onResult(false, accountsReadOnlyMessage())
            return
        }
        // A paid bill has a receipt against it; deleting it would leave the
        // receipt pointing at nothing. Withdraw the receipt first.
        if (bill.derivedStatus == PaymentStatus.PAID) {
            onResult(false, msg(
                "भरलेले बिल हटवता येत नाही. आधी पावती रद्द करा.",
                "भुगतान किया गया बिल नहीं हटा सकते। पहले रसीद हटाएं।",
                "A paid bill cannot be deleted. Delete its receipt first."
            ))
            return
        }
        safeLaunch {
            repository.deleteBill(bill)
            val res = repository.getResidentByFlat(bill.flatNumber)
            if (res != null) {
                val owed = (bill.payableTotal - bill.amountPaid).coerceAtLeast(0.0)
                repository.updateResident(
                    res.copy(balanceDue = (res.balanceDue - owed).coerceAtLeast(0.0))
                )
            }
            onResult(true, msg("बिल हटवले!", "बिल हटाया गया!", "Bill deleted."))
        }
    }

    // Expense Logger
    /** Cash plus bank - what the society can actually pay an expense out of. */
    val availableFunds: Double
        get() = financialOverview.value.cashBalance + financialOverview.value.bankBalance

    /** Funds sitting in whichever account the expense would be paid from. */
    fun fundsFor(mode: PaymentMode): Double = if (mode == PaymentMode.CASH) {
        financialOverview.value.cashBalance
    } else {
        financialOverview.value.bankBalance
    }

    fun addExpense(
        title: String,
        category: ExpenseCategory,
        amount: Double,
        date: String,
        vendor: String,
        vendorPhone: String,
        mode: PaymentMode,
        notes: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (!canEditAccounts) {
            onResult(false, accountsReadOnlyMessage())
            return
        }
        // The society cannot spend money it does not hold. Checked here as well
        // as in the form, so no caller can slip an overdraft past it.
        val available = availableFunds
        if (amount > available) {
            onResult(false, msg(
                "पुरेशी शिल्लक नाही. हा खर्च नोंदवण्यापूर्वी कृपया निधी जमा करा.",
                "अपर्याप्त शेष राशि। यह व्यय दर्ज करने से पहले कृपया धनराशि जोड़ें।",
                "Insufficient balance. Please add funds before recording this expense."
            ))
            return
        }
        safeLaunch {
            val expNo = "EXP-${SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())}-${Random.nextInt(100, 999)}"
            val expense = ExpenseRecord(
                title = title,
                category = category,
                amount = amount,
                date = date.ifBlank { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()) },
                vendorName = vendor,
                vendorContact = vendorPhone,
                paymentMode = mode,
                approvedBy = _currentUser.value?.role?.name ?: "Secretary",
                notes = notes,
                receiptNumber = expNo,
                status = PaymentStatus.PAID
            )
            repository.insertExpense(expense)
            val done = if (_language.value == AppLanguage.MARATHI) {
                "₹$amount खर्च नोंदवला ($expNo)"
            } else {
                "Recorded expense ₹$amount ($expNo)"
            }
            showMessage(done)
            onResult(true, done)
        }
    }

    /**
     * Rewrites an expense that was already recorded.
     *
     * The overdraft guard has to allow for the money this expense already took
     * out of the treasury, otherwise editing a large expense down would be
     * rejected for being over budget against its own spending.
     */
    fun updateExpense(
        previous: ExpenseRecord,
        title: String,
        category: ExpenseCategory,
        amount: Double,
        date: String,
        vendor: String,
        vendorPhone: String,
        mode: PaymentMode,
        notes: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (!canEditAccounts) {
            onResult(false, accountsReadOnlyMessage())
            return
        }
        if (amount <= 0.0) {
            onResult(false, msg(
                "रक्कम शून्यापेक्षा जास्त असावी.",
                "राशि शून्य से अधिक होनी चाहिए।",
                "Enter an amount greater than zero."
            ))
            return
        }
        if (amount > availableFunds + previous.amount) {
            onResult(false, msg(
                "पुरेशी शिल्लक नाही. हा खर्च नोंदवण्यापूर्वी कृपया निधी जमा करा.",
                "अपर्याप्त शेष राशि। यह व्यय दर्ज करने से पहले कृपया धनराशि जोड़ें।",
                "Insufficient balance. Please add funds before recording this expense."
            ))
            return
        }
        safeLaunch {
            repository.updateExpense(
                previous,
                previous.copy(
                    title = title.ifBlank { category.labelEn },
                    category = category,
                    amount = amount,
                    date = date.ifBlank { previous.date },
                    vendorName = vendor,
                    vendorContact = vendorPhone,
                    paymentMode = mode,
                    notes = notes
                )
            )
            onResult(true, msg(
                "खर्च अद्ययावत केला (${previous.receiptNumber}).",
                "व्यय अपडेट किया गया (${previous.receiptNumber})।",
                "Expense ${previous.receiptNumber} updated."
            ))
        }
    }

    /** Deletes an expense and puts the money back where it was paid from. */
    fun deleteExpense(expense: ExpenseRecord, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        if (!canEditAccounts) {
            onResult(false, accountsReadOnlyMessage())
            return
        }
        safeLaunch {
            repository.deleteExpense(expense)
            onResult(true, msg("खर्च हटवला!", "व्यय हटाया गया!", "Expense removed."))
        }
    }

    private fun accountsReadOnlyMessage(): String = msg(
        "हिशोबात बदल फक्त अध्यक्ष व खजिनदार करू शकतात.",
        "खातों में बदलाव केवल अध्यक्ष और कोषाध्यक्ष कर सकते हैं।",
        "Only the Chairman and Treasurer can edit the accounts."
    )

    // Non-fee Revenue Logger
    fun addNonFeeRevenue(
        title: String,
        category: RevenueSource,
        amount: Double,
        date: String,
        payer: String,
        flatNumber: String,
        mode: PaymentMode,
        notes: String
    ) {
        if (!canEditAccounts) {
            showMessage(accountsReadOnlyMessage())
            return
        }
        safeLaunch {
            val recNo = "NFR-${SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())}-${Random.nextInt(100, 999)}"
            val revenue = NonFeeRevenue(
                title = title,
                sourceCategory = category,
                amount = amount,
                date = date.ifBlank { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()) },
                payerName = payer,
                flatNumber = flatNumber,
                paymentMode = mode,
                receiptNumber = recNo,
                notes = notes
            )
            repository.insertRevenue(revenue)
            showMessage(if (_language.value == AppLanguage.MARATHI) "₹$amount इतर महसूल नोंदवला ($recNo)" else "Recorded non-fee revenue ₹$amount ($recNo)")
        }
    }

    // Resident Management
    fun addResident(
        flatNumber: String,
        wing: String,
        ownerName: String,
        coApplicantName: String = "",
        tenantName: String,
        tenantPhone: String,
        rentAgreementBase64: String,
        phone: String,
        email: String,
        role: UserRole,
        familyCount: Int,
        twoWheelers: Int,
        fourWheelers: Int,
        twoWheelerNo: String,
        fourWheelerNo: String,
        parkingSlot: String,
        parkingType: ParkingType,
        unitType: String,
        flatType: FlatType,
        sqft: Int,
        pin: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        safeLaunch {
            val flat = flatNumber.trim().uppercase()
            if (pin.isNotBlank() && (pin.trim().length != 4 || pin.trim().any { !it.isDigit() })) {
                onResult(false, msg(
                    "लॉगिन पिन नेमका ४ अंकी असावा.",
                    "लॉगिन पिन ठीक 4 अंकों का होना चाहिए।",
                    "The login PIN must be exactly 4 digits."
                ))
                return@safeLaunch
            }
            // A bay can only be allotted once.
            if (parkingType != ParkingType.NONE && parkingSlot.isNotBlank()) {
                val clash = repository.getAllResidentsSync().firstOrNull { other ->
                    other.parkingSlot.equals(parkingSlot.trim(), ignoreCase = true) &&
                        !other.flatNumber.equals(flat, ignoreCase = true)
                }
                if (clash != null) {
                    onResult(false, msg(
                        "पार्किंग $parkingSlot आधीच ${clash.flatNumber} ला दिले आहे.",
                        "पार्किंग $parkingSlot पहले से ${clash.flatNumber} को दिया गया है।",
                        "Parking $parkingSlot is already allotted to ${clash.flatNumber}."
                    ))
                    return@safeLaunch
                }
            }
            if (flat.isBlank() || ownerName.isBlank() || phone.isBlank() || pin.isBlank()) {
                onResult(false, msg(
                    "फ्लॅट क्रमांक, नाव, मोबाईल व पिन आवश्यक आहे.",
                    "फ्लैट नंबर, नाम, मोबाइल और पिन आवश्यक है।",
                    "Unit number, name, mobile and PIN are all required."
                ))
                return@safeLaunch
            }
            // A flat generated by the Floor Inventory step exists but has no
            // owner yet - creating a resident fills that vacant unit in rather
            // than being turned away as a duplicate.
            val existing = repository.getResidentByFlat(flat)
            if (existing != null && existing.ownerName.isNotBlank()) {
                onResult(false, msg(
                    "फ्लॅट $flat आधीच नोंदणीकृत आहे.",
                    "फ्लैट $flat पहले से पंजीकृत है।",
                    "Unit $flat is already registered."
                ))
                return@safeLaunch
            }
            val resident = Resident(
                docId = existing?.docId.orEmpty(),
                id = existing?.id ?: 0,
                flatNumber = flat,
                wing = wing.trim().ifBlank { flat.substringBefore("-").take(1) },
                ownerName = ownerName.trim(),
                coApplicantName = coApplicantName.trim(),
                tenantName = tenantName.trim(),
                tenantContact = if (tenantName.isBlank()) "" else tenantPhone.trim(),
                rentAgreementBase64 = if (tenantName.isBlank()) "" else rentAgreementBase64,
                contactNumber = phone.trim(),
                email = email.trim(),
                role = role,
                pin = pin.trim(),
                familyMemberCount = familyCount,
                vehicleTwoWheelerCount = twoWheelers,
                vehicleFourWheelerCount = fourWheelers,
                twoWheelerNumber = twoWheelerNo,
                fourWheelerNumber = fourWheelerNo,
                parkingSlot = if (parkingType == ParkingType.NONE) "" else parkingSlot,
                parkingType = parkingType,
                unitType = unitType.trim(),
                // Kept in step with the chosen unit type so older screens that
                // still read the built-in slab stay right.
                flatType = if (unitType.isBlank()) flatType else flatTypeFor(unitType),
                squareFeet = sqft,
                balanceDue = 0.0,
                isOwnerLiving = tenantName.isBlank()
            )
            repository.insertResident(resident)
            onResult(true, msg(
                "सभासद $flat जोडले! ते या फ्लॅट क्रमांक व पिनने लॉगिन करू शकतात.",
                "सदस्य $flat जोड़ा गया! वे इस फ्लैट नंबर और पिन से लॉगिन कर सकते हैं।",
                "Member $flat added. They can log in with this unit number and PIN."
            ))
        }
    }

    /**
     * Takes the member off a unit, leaving the unit itself in the directory.
     *
     * A society's flats do not disappear when somebody moves out - the unit
     * stays as a vacant one, ready for the next owner, and its bills and
     * history stay with it.
     */
    /**
     * Renames a wing on every unit that carries it.
     *
     * Units entered before the wing list was set up often carry a wing name of
     * their own - "1", "A", the society's own name - and renaming only the
     * society's list would leave them stranded on the old one. This moves the
     * units too, which is what makes the two agree again.
     */
    fun renameWingOnUnits(from: String, to: String, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        if (!canEditSociety) {
            onResult(false, msg(
                "फक्त अध्यक्षच विंग बदलू शकतात.",
                "केवल अध्यक्ष ही विंग बदल सकते हैं।",
                "Only the Chairman can rename a wing."
            ))
            return
        }
        val oldName = from.trim()
        val newName = to.trim()
        if (oldName.isBlank() || newName.isBlank() || oldName.equals(newName, ignoreCase = true)) {
            onResult(false, "Enter a different wing name.")
            return
        }
        safeLaunch {
            val affected = repository.getAllResidentsSync()
                .filter { it.wing.equals(oldName, ignoreCase = true) }
            affected.forEach { unit -> repository.updateResident(unit.copy(wing = newName)) }
            onResult(true, msg(
                "विंग $oldName चे नाव $newName केले (${affected.size} सदनिका).",
                "विंग $oldName का नाम $newName किया (${affected.size} इकाइयां)।",
                "Wing $oldName renamed to $newName on ${affected.size} unit(s)."
            ))
        }
    }

    /**
     * Deletes a unit that nobody lives in.
     *
     * Refused outright for an occupied unit: a flat with a member behind it is
     * emptied with [removeMemberFromUnit], never deleted from under them.
     */
    /** What deleting a wing would take with it, so the chairman sees it first. */
    data class WingContents(val units: Int, val occupied: Int, val bills: Int)

    fun wingContents(wing: String): WingContents {
        val units = residents.value.filter { it.wing.equals(wing.trim(), ignoreCase = true) }
        val unitNumbers = units.map { it.flatNumber.uppercase() }.toSet()
        return WingContents(
            units = units.size,
            occupied = units.count { it.ownerName.isNotBlank() },
            bills = bills.value.count { it.flatNumber.uppercase() in unitNumbers }
        )
    }

    /**
     * Deletes a wing and every unit on it.
     *
     * Destructive on purpose: a wing that was created by mistake takes its
     * flats with it, otherwise they are left orphaned on a wing that no longer
     * exists. Bills raised against those units are removed too, so no bill is
     * left pointing at a unit that is gone.
     */
    fun deleteWingWithUnits(wing: String, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        if (!canEditSociety) {
            onResult(false, msg(
                "फक्त अध्यक्षच विंग हटवू शकतात.",
                "केवल अध्यक्ष ही विंग हटा सकते हैं।",
                "Only the Chairman can delete a wing."
            ))
            return
        }
        val name = wing.trim()
        if (name.isBlank()) {
            onResult(false, "Pick a wing to delete.")
            return
        }
        safeLaunch {
            val units = repository.getAllResidentsSync()
                .filter { it.wing.equals(name, ignoreCase = true) }
            val unitNumbers = units.map { it.flatNumber.uppercase() }.toSet()

            bills.value.filter { it.flatNumber.uppercase() in unitNumbers }
                .forEach { repository.deleteBill(it) }
            units.forEach { repository.deleteResident(it) }

            // Take the wing off the society's own list as well.
            val soc = repository.getSocietyInfoSync()
            if (soc != null) {
                val keptNames = soc.wingNames.filterNot { it.equals(name, ignoreCase = true) }
                val keptPlans = soc.wingPlans.filterNot { it.name.equals(name, ignoreCase = true) }
                repository.updateSociety(
                    soc.copy(
                        wingNames = keptNames,
                        wingPlans = keptPlans,
                        totalWings = maxOf(keptNames.size, keptPlans.size),
                        totalFlats = (soc.totalFlats - units.size).coerceAtLeast(0)
                    )
                )
            }

            onResult(true, msg(
                "विंग $name व त्यातील ${units.size} सदनिका हटवल्या.",
                "विंग $name और उसकी ${units.size} इकाइयां हटाई गईं।",
                "Deleted wing $name and its ${units.size} unit(s)."
            ))
        }
    }

    fun deleteVacantUnit(unit: Resident, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        if (!canEditSociety) {
            onResult(false, msg(
                "फक्त अध्यक्षच सदनिका हटवू शकतात.",
                "केवल अध्यक्ष ही इकाई हटा सकते हैं।",
                "Only the Chairman can delete a unit."
            ))
            return
        }
        if (unit.ownerName.isNotBlank()) {
            onResult(false, msg(
                "या सदनिकेत सभासद आहे. आधी सभासद काढा.",
                "इस इकाई में सदस्य है। पहले सदस्य हटाएं।",
                "That unit has a member in it. Remove the member first."
            ))
            return
        }
        safeLaunch {
            repository.deleteResident(unit)
            onResult(true, msg(
                "सदनिका ${unit.flatNumber} हटवली.",
                "इकाई ${unit.flatNumber} हटाई गई।",
                "Unit ${unit.flatNumber} deleted."
            ))
        }
    }

    fun removeMemberFromUnit(member: Resident, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        if (!canEditSociety) {
            onResult(false, msg(
                "फक्त अध्यक्षच सभासद काढू शकतात.",
                "केवल अध्यक्ष ही सदस्य हटा सकते हैं।",
                "Only the Chairman can remove a member."
            ))
            return
        }
        safeLaunch {
            repository.updateResident(
                member.copy(
                    ownerName = "",
                    tenantName = "",
                    contactNumber = "",
                    email = "",
                    pin = "",
                    photoBase64 = "",
                    familyMemberCount = 1,
                    parkingSlot = "",
                    parkingType = ParkingType.NONE,
                    twoWheelerNumber = "",
                    fourWheelerNumber = "",
                    vehicleTwoWheelerCount = 0,
                    vehicleFourWheelerCount = 0,
                    isOwnerLiving = true
                )
            )
            onResult(true, msg(
                "${member.flatNumber} मधील सभासद काढला. सदनिका रिक्त झाली.",
                "${member.flatNumber} से सदस्य हटाया गया। इकाई रिक्त है।",
                "Member removed from ${member.flatNumber}. The unit is now vacant."
            ))
        }
    }

    fun updateResident(resident: Resident, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        safeLaunch {
            /*
             * A bay can only be allotted once. Adding a member has always
             * checked this; editing one never did, which mattered little while
             * the bay came off a generated list and matters a great deal now
             * that a committee can type its own bay numbers in.
             */
            if (resident.parkingType != ParkingType.NONE && resident.parkingSlot.isNotBlank()) {
                val clash = repository.getAllResidentsSync().firstOrNull { other ->
                    other.parkingSlot.trim().equals(resident.parkingSlot.trim(), ignoreCase = true) &&
                        !other.flatNumber.equals(resident.flatNumber, ignoreCase = true)
                }
                if (clash != null) {
                    val message = msg(
                        "पार्किंग ${resident.parkingSlot} आधीच ${clash.flatNumber} ला दिले आहे.",
                        "पार्किंग ${resident.parkingSlot} पहले से ${clash.flatNumber} को दिया गया है।",
                        "Parking ${resident.parkingSlot} is already allotted to ${clash.flatNumber}."
                    )
                    showMessage(message)
                    onResult(false, message)
                    return@safeLaunch
                }
            }
            repository.updateResident(resident)
            val done = msg("माहिती अद्ययावत केली!", "जानकारी अपडेट की गई!", "Resident details updated!")
            showMessage(done)
            onResult(true, done)
        }
    }

    /**
     * Saves how a month's maintenance is divided between the society's funds.
     *
     * The four shares are of the maintenance amount, not additions to it, so
     * they have to add up to 100% - a society whose shares do not is refused
     * rather than quietly billed wrong. Bills already generated keep the
     * amounts they were raised with; this only affects bills raised afterwards.
     */
    fun updateMaintenanceCharges(
        waterPercent: Double,
        sinkingPercent: Double,
        repairPercent: Double,
        otherPercent: Double,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (!canEditAccounts) {
            onResult(false, accountsReadOnlyMessage())
            return
        }
        val shares = listOf(waterPercent, sinkingPercent, repairPercent, otherPercent)
        if (shares.any { it < 0.0 }) {
            onResult(false, msg(
                "टक्केवारी ऋण असू शकत नाही.",
                "प्रतिशत ऋणात्मक नहीं हो सकता।",
                "A percentage cannot be negative."
            ))
            return
        }
        val total = shares.sum()
        if (kotlin.math.abs(total - 100.0) >= 0.01) {
            onResult(false, msg(
                "एकूण टक्केवारी १००%% असावी. सध्या ती %.2f%% आहे.".format(total),
                "कुल प्रतिशत 100%% होना चाहिए। अभी यह %.2f%% है।".format(total),
                "The percentages must add up to 100%%. They currently add up to %.2f%%.".format(total)
            ))
            return
        }
        safeLaunch {
            val soc = repository.getSocietyInfoSync()
            if (soc == null) {
                onResult(false, "Society not loaded yet.")
                return@safeLaunch
            }
            repository.updateSociety(
                soc.copy(
                    waterChargesPercent = waterPercent,
                    sinkingFundPercent = sinkingPercent,
                    repairFundPercent = repairPercent,
                    otherMaintenancePercent = otherPercent
                )
            )
            onResult(true, msg(
                "देखभाल शुल्क रचना जतन केली! पुढील बिलांना ती लागू होईल.",
                "रखरखाव शुल्क संरचना सहेजी गई! अगले बिलों पर लागू होगी।",
                "Maintenance charge split saved. It applies to bills raised from now on."
            ))
        }
    }

    fun updateLateFeeRules(percent: Double, graceDays: Int) {
        safeLaunch {
            val soc = repository.getSocietyInfoSync()
            if (soc != null) {
                repository.updateSociety(
                    soc.copy(
                        lateFeePercentPerMonth = percent,
                        lateFeeGraceDays = graceDays
                    )
                )
                showMessage(if (_language.value == AppLanguage.MARATHI) "व्याज व दंड नियम अद्ययावत केले!" else "Late fee rules updated!")
            }
        }
    }

    // Gate & Visitor
    fun checkInVisitor(
        name: String,
        phone: String,
        flatNumber: String,
        purpose: VisitorPurpose,
        vehicleNo: String,
        passcode: String
    ) {
        safeLaunch {
            val nowTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            val visitor = VisitorLog(
                visitorName = name,
                phone = phone,
                flatNumber = flatNumber,
                purpose = purpose,
                vehicleNumber = vehicleNo,
                entryTime = nowTime,
                status = VisitorStatus.INSIDE,
                passcode = passcode.ifBlank { Random.nextInt(100000, 999999).toString() },
                guardName = _currentUser.value?.ownerName ?: "Gate Guard"
            )
            repository.insertVisitor(visitor)
            showMessage(if (_language.value == AppLanguage.MARATHI) "अभ्यागत नोंद केली (फ्लॅट $flatNumber)" else "Visitor checked in for $flatNumber")
        }
    }

    /**
     * The member at the unit says a visitor is expected, or is not.
     *
     * Only the unit the visitor was logged against can answer for them, which
     * is checked here as well as in the UI so a stale screen cannot approve
     * somebody else's caller.
     */
    fun answerVisitor(
        visitor: VisitorLog,
        isApproved: Boolean,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val user = _currentUser.value
        val ownsUnit = myUnits.value.any { it.flatNumber.equals(visitor.flatNumber, ignoreCase = true) } ||
            user?.flatNumber.equals(visitor.flatNumber, ignoreCase = true)
        val isCommittee = user?.role == UserRole.CHAIRMAN || user?.role == UserRole.SECRETARY
        if (!ownsUnit && !isCommittee) {
            onResult(false, msg(
                "हा अभ्यागत तुमच्या सदनिकेसाठी नाही.",
                "यह आगंतुक आपकी इकाई के लिए नहीं है।",
                "That visitor is not for your unit."
            ))
            return
        }
        safeLaunch {
            repository.updateVisitor(
                visitor.copy(
                    status = if (isApproved) VisitorStatus.INSIDE else VisitorStatus.REJECTED
                )
            )
            onResult(true, if (isApproved) {
                msg(
                    "${visitor.visitorName} ना परवानगी दिली.",
                    "${visitor.visitorName} को अनुमति दी गई।",
                    "Approved ${visitor.visitorName}."
                )
            } else {
                msg(
                    "${visitor.visitorName} ना नकार दिला.",
                    "${visitor.visitorName} को अस्वीकार किया।",
                    "Turned ${visitor.visitorName} away."
                )
            })
        }
    }

    fun markVisitorExit(visitor: VisitorLog) {
        safeLaunch {
            val nowTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            repository.markVisitorExit(visitor.id, nowTime, visitor)
            showMessage(if (_language.value == AppLanguage.MARATHI) "${visitor.visitorName} बाहेर पडले" else "Marked exit for ${visitor.visitorName}")
        }
    }

    fun generateVisitorPasscode(
        visitorName: String,
        phone: String,
        flatNumber: String,
        purpose: VisitorPurpose,
        onGenerated: (String) -> Unit
    ) {
        safeLaunch {
            val code = Random.nextInt(100000, 999999).toString()
            val visitor = VisitorLog(
                visitorName = visitorName,
                phone = phone,
                flatNumber = flatNumber,
                purpose = purpose,
                entryTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
                status = VisitorStatus.PRE_APPROVED,
                passcode = code
            )
            repository.insertVisitor(visitor)
            onGenerated(code)
            showMessage(if (_language.value == AppLanguage.MARATHI) "६-अंकी पासकोड तयार झाला: $code" else "Generated 6-digit passcode: $code")
        }
    }

    fun verifyPasscode(code: String, onResult: (VisitorLog?) -> Unit) {
        safeLaunch {
            val log = repository.verifyVisitorPasscode(code.trim())
            onResult(log)
        }
    }

    // Community Notices
    fun postNotice(title: String, content: String, isPinned: Boolean, category: NoticeCategory) {
        safeLaunch {
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            val notice = Notice(
                title = title,
                content = content,
                publishedDate = dateStr,
                isPinned = isPinned,
                category = category,
                authorRole = _currentUser.value?.role?.name ?: "Managing Committee"
            )
            repository.insertNotice(notice)
            showMessage(if (_language.value == AppLanguage.MARATHI) "नवीन सूचना फलकावर प्रसिद्ध केली!" else "Notice published to board!")
        }
    }

    fun deleteNotice(notice: Notice) {
        safeLaunch {
            repository.deleteNotice(notice)
            showMessage(if (_language.value == AppLanguage.MARATHI) "सूचना हटवली!" else "Notice removed!")
        }
    }

    // Helpdesk & Complaints
    fun fileComplaint(category: ComplaintCategory, description: String, flatNumber: String, residentName: String) {
        safeLaunch {
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            val complaint = Complaint(
                flatNumber = flatNumber,
                residentName = residentName,
                category = category,
                description = description,
                filedDate = dateStr,
                status = TicketStatus.OPEN
            )
            repository.insertComplaint(complaint)
            showMessage(if (_language.value == AppLanguage.MARATHI) "तक्रार दाखल केली! समिती लवकरच कार्यवाही करेल." else "Complaint ticket raised successfully!")
        }
    }

    /**
     * A member correcting their own complaint.
     *
     * Only the unit that raised it may edit it, and only while it is still
     * open - once the committee has picked it up, changing the description
     * underneath them would be rewriting history.
     */
    fun editMyComplaint(
        complaint: Complaint,
        category: ComplaintCategory,
        description: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val refusal = complaintRefusal(complaint)
        if (refusal != null) {
            onResult(false, refusal)
            return
        }
        if (description.isBlank()) {
            onResult(false, msg("तपशील लिहा.", "विवरण लिखें।", "Write what the problem is."))
            return
        }
        safeLaunch {
            repository.updateComplaint(
                complaint.copy(category = category, description = description.trim())
            )
            onResult(true, msg("तक्रार अद्ययावत केली!", "शिकायत अपडेट की गई!", "Complaint updated."))
        }
    }

    /** A member withdrawing their own complaint, while it is still open. */
    fun deleteMyComplaint(complaint: Complaint, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        val refusal = complaintRefusal(complaint)
        if (refusal != null) {
            onResult(false, refusal)
            return
        }
        safeLaunch {
            repository.deleteComplaint(complaint)
            onResult(true, msg("तक्रार मागे घेतली.", "शिकायत वापस ली गई।", "Complaint withdrawn."))
        }
    }

    /** Why this member may not touch this complaint, or null when they may. */
    private fun complaintRefusal(complaint: Complaint): String? {
        val mine = myUnits.value.any { it.flatNumber.equals(complaint.flatNumber, ignoreCase = true) } ||
            _currentUser.value?.flatNumber.equals(complaint.flatNumber, ignoreCase = true)
        if (!mine) {
            return msg(
                "ही तक्रार तुमच्या सदनिकेची नाही.",
                "यह शिकायत आपकी इकाई की नहीं है।",
                "That complaint was not raised by your unit."
            )
        }
        if (complaint.status != TicketStatus.OPEN) {
            return msg(
                "समितीने ही तक्रार हाती घेतली आहे, आता बदलता येणार नाही.",
                "समिति ने यह शिकायत ले ली है, अब बदली नहीं जा सकती।",
                "The committee has already picked this up, so it can no longer be changed."
            )
        }
        return null
    }

    fun updateComplaintStatus(complaint: Complaint, newStatus: TicketStatus, notes: String) {
        safeLaunch {
            repository.updateComplaintStatus(complaint, newStatus, notes)
            showMessage(if (_language.value == AppLanguage.MARATHI) "तक्रार स्थिती अद्ययावत केली!" else "Ticket status updated!")
        }
    }

    // Society staff - hired, managed and given login credentials by the chairman

    /** Members living in the society, staff excluded. */
    val members: StateFlow<List<Resident>> = residents
        .map { list -> list.filterNot { it.role.isStaff } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Everyone on the society payroll. */
    val staff: StateFlow<List<Resident>> = residents
        .map { list -> list.filter { it.role.isStaff } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Parking bays that are free, plus whichever bay [keepFor] already holds so
     * an edit does not have to give its own slot up first.
     */
    fun availableParking(type: ParkingType, keepFor: String = ""): List<String> {
        val society = societyInfo.value ?: return emptyList()
        val taken = residents.value
            .filter { !it.flatNumber.equals(keepFor, ignoreCase = true) }
            .map { it.parkingSlot.trim().uppercase() }
            .filter { it.isNotBlank() }
            .toSet()
        return society.parkingNumbers(type).filterNot { it.uppercase() in taken }
    }

    /**
     * Creates or updates a staff member. The staff code doubles as their login
     * id, exactly as a flat number does for a resident, so the existing login
     * screen works for them with no change at all.
     */
    fun saveStaff(
        staffCode: String,
        name: String,
        role: UserRole,
        designation: String,
        phone: String,
        email: String,
        pin: String,
        isActive: Boolean,
        existing: Resident? = null,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (!canEditSociety) {
            onResult(false, msg(
                "फक्त अध्यक्षच कर्मचारी व्यवस्थापित करू शकतात.",
                "केवल अध्यक्ष ही कर्मचारी प्रबंधित कर सकते हैं।",
                "Only the Chairman can manage society staff."
            ))
            return
        }
        safeLaunch {
            val code = staffCode.trim().uppercase()
            val trimmedPin = pin.trim()
            if (code.isBlank() || name.isBlank() || phone.isBlank()) {
                onResult(false, msg(
                    "कर्मचारी कोड, नाव व मोबाईल आवश्यक आहे.",
                    "स्टाफ कोड, नाम और मोबाइल आवश्यक है।",
                    "Staff code, name and mobile number are all required."
                ))
                return@safeLaunch
            }
            if (trimmedPin.length != 4 || trimmedPin.any { !it.isDigit() }) {
                onResult(false, msg(
                    "लॉगिन पिन नेमका ४ अंकी असावा.",
                    "लॉगिन पिन ठीक 4 अंकों का होना चाहिए।",
                    "The login PIN must be exactly 4 digits."
                ))
                return@safeLaunch
            }
            // A staff code shares the residents collection with flat numbers,
            // so it must not collide with a real unit.
            val clash = repository.getResidentByFlat(code)
            if (clash != null && !clash.role.isStaff && existing == null) {
                onResult(false, msg(
                    "$code हा फ्लॅट क्रमांक आधीच वापरात आहे. दुसरा कर्मचारी कोड निवडा.",
                    "$code पहले से एक फ्लैट नंबर है। दूसरा स्टाफ कोड चुनें।",
                    "$code is already a unit number. Pick a different staff code."
                ))
                return@safeLaunch
            }

            repository.insertResident(
                Resident(
                    docId = existing?.docId.orEmpty(),
                    id = existing?.id ?: 0,
                    flatNumber = code,
                    wing = "STAFF",
                    ownerName = name.trim(),
                    contactNumber = phone.trim(),
                    email = email.trim(),
                    role = role,
                    pin = trimmedPin,
                    staffDesignation = designation.trim(),
                    isActive = isActive,
                    flatType = FlatType.COMMERCIAL_SHOP,
                    balanceDue = 0.0
                )
            )
            onResult(true, msg(
                "कर्मचारी $code जतन केला. ते या कोड व पिनने लॉगिन करू शकतात.",
                "स्टाफ $code सहेजा गया। वे इस कोड और पिन से लॉगिन कर सकते हैं।",
                "Staff $code saved. They can sign in with this code and PIN."
            ))
        }
    }

    fun deleteStaff(member: Resident, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        if (!canEditSociety) {
            onResult(false, msg(
                "फक्त अध्यक्षच कर्मचारी व्यवस्थापित करू शकतात.",
                "केवल अध्यक्ष ही कर्मचारी प्रबंधित कर सकते हैं।",
                "Only the Chairman can manage society staff."
            ))
            return
        }
        safeLaunch {
            repository.deleteResident(member)
            onResult(true, msg("कर्मचारी हटवला!", "स्टाफ हटाया गया!", "Staff member removed."))
        }
    }

    // Profiles

    /** True when the signed-in member is the one allowed to edit society details. */
    val canEditSociety: Boolean
        get() = _currentUser.value?.role == UserRole.CHAIRMAN

    /**
     * Who may record expenses and revenue: the Chairman and the Treasurer.
     * Everyone else - members included - gets Accounts read-only.
     */
    val canEditAccounts: Boolean
        get() = _currentUser.value?.role in setOf(UserRole.CHAIRMAN, UserRole.TREASURER)

    /**
     * Saves the Society Profile in full - the same ground the registration
     * wizard covers: identity and address, bank details, wings, floor
     * inventory, unit types with their tariffs, parking counts and the month
     * billing starts from.
     *
     * Wings and floors that were added since registration are expanded into
     * vacant units here, exactly as registration does, so the new flats land in
     * the Resident Directory ready for owners to be filled in. Units that
     * already exist are never touched, so nobody's details are overwritten.
     */
    fun saveSocietyConfiguration(
        updated: SocietyInfo,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (!canEditSociety) {
            onResult(false, msg(
                "फक्त अध्यक्षच संस्थेची माहिती बदलू शकतात.",
                "केवल अध्यक्ष ही सोसायटी की जानकारी बदल सकते हैं।",
                "Only the Chairman can edit society details."
            ))
            return
        }
        safeLaunch {
            val current = repository.getSocietyInfoSync()
            if (current == null) {
                onResult(false, "Society not loaded yet.")
                return@safeLaunch
            }

            // An empty section of the form means "leave this alone", never
            // "wipe it": a society that predates floor inventory keeps the
            // wings and unit count it already has.
            val wingPlans = updated.wingPlans.filter { it.totalUnits > 0 }
                .ifEmpty { current.wingPlans }
            val wingNames = updated.wingNames
                .ifEmpty { wingPlans.map { it.name } }
                .ifEmpty { current.wingNames }
            val newUnits = wingPlans.flatMap { plan -> plan.toVacantUnits() }

            /*
             * The unit types follow the wings, but their monthly tariffs do
             * not - those are set under Maintenance > Tariff. So a type that
             * already carries a tariff keeps it; only genuinely new types come
             * in at zero.
             */
            val mergedUnitTypes = updated.unitTypes
                .filter { it.name.isNotBlank() }
                .map { type ->
                    if (type.monthlyTariff > 0.0) {
                        type
                    } else {
                        val known = current.unitTypes
                            .firstOrNull { it.name.equals(type.name, ignoreCase = true) }
                        type.copy(monthlyTariff = known?.monthlyTariff ?: 0.0)
                    }
                }
                .ifEmpty { current.unitTypes }

            repository.updateSociety(
                updated.copy(
                    // The login code, the document identity and the treasury
                    // balances belong to the ledger, not to this form.
                    societyCode = current.societyCode,
                    docId = current.docId,
                    id = current.id,
                    wingNames = wingNames,
                    wingPlans = wingPlans,
                    totalWings = maxOf(wingNames.size, wingPlans.size, current.totalWings),
                    totalFlats = newUnits.size.coerceAtLeast(current.totalFlats),
                    unitTypes = mergedUnitTypes,
                    tariff1BHK = tariffOf(mergedUnitTypes, "1 BHK", current.tariff1BHK),
                    tariff2BHK = tariffOf(mergedUnitTypes, "2 BHK", current.tariff2BHK),
                    tariff3BHK = tariffOf(mergedUnitTypes, "3 BHK", current.tariff3BHK),
                    tariffPenthouse = tariffOf(mergedUnitTypes, "Penthouse", current.tariffPenthouse),
                    tariffShop = tariffOf(mergedUnitTypes, "Shop", current.tariffShop)
                )
            )

            // Only ever adds; insertVacantUnits skips every unit number that
            // already has a document behind it.
            if (newUnits.isNotEmpty()) {
                repository.insertVacantUnits(current.societyCode, newUnits)
            }

            onResult(true, msg(
                "संस्थेची माहिती जतन केली!",
                "सोसायटी जानकारी सहेजी गई!",
                "Society profile saved."
            ))
        }
    }

    /** Monthly tariff configured for a named unit type, matched loosely. */
    private fun tariffOf(types: List<UnitTypeConfig>, name: String, fallback: Double): Double {
        val wanted = name.lowercase().replace(" ", "")
        return types.firstOrNull { it.name.lowercase().replace(" ", "") == wanted }
            ?.monthlyTariff
            ?.takeIf { it > 0.0 }
            ?: fallback
    }

    /**
     * Saves the society's unit types and their monthly tariffs from the Tariff
     * screen. The fixed BHK slabs are kept in step with them so older bills and
     * the Generate Bill sheet never fall back to a stale number.
     */
    fun updateUnitTypeTariffs(
        types: List<UnitTypeConfig>,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (!canEditSociety && !canEditAccounts) {
            onResult(false, accountsReadOnlyMessage())
            return
        }
        safeLaunch {
            val soc = repository.getSocietyInfoSync()
            if (soc == null) {
                onResult(false, "Society not loaded yet.")
                return@safeLaunch
            }
            val cleaned = types.filter { it.name.isNotBlank() }
            repository.updateSociety(
                soc.copy(
                    unitTypes = cleaned,
                    tariff1BHK = tariffOf(cleaned, "1 BHK", soc.tariff1BHK),
                    tariff2BHK = tariffOf(cleaned, "2 BHK", soc.tariff2BHK),
                    tariff3BHK = tariffOf(cleaned, "3 BHK", soc.tariff3BHK),
                    tariffPenthouse = tariffOf(cleaned, "Penthouse", soc.tariffPenthouse),
                    tariffShop = tariffOf(cleaned, "Shop", soc.tariffShop)
                )
            )
            onResult(true, msg(
                "मासिक दरपत्रक अद्ययावत केले!",
                "मासिक दर सूची अपडेट की गई!",
                "Tariff slabs updated successfully!"
            ))
        }
    }

    /** Updates the signed-in member's own contact details and photo. */
    /**
     * Updates the signed-in user's own profile.
     *
     * A member may fill in what the society never recorded, and nothing else.
     * Their name, unit, mobile number and the rest are the society's register,
     * and a register a member can quietly rewrite is not a register - a wrong
     * mobile number there is a member the committee can no longer reach. So for
     * a resident or tenant every field that already holds something is kept as
     * it is, however the form was filled in, and only the blanks are taken.
     *
     * The committee and the staff edit their own details freely; the rule is
     * about the membership register, not about everybody's contact card.
     */
    fun updateMyProfile(
        ownerName: String,
        phone: String,
        email: String,
        photoBase64: String,
        coApplicantName: String = "",
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val current = _currentUser.value
        if (current == null) {
            onResult(false, "Not signed in.")
            return
        }

        safeLaunch {
            // Enforced here rather than only in the form, so a locked field
            // cannot be got around by anything that calls this.
            val updated = current.withProfileEdits(
                ownerName = ownerName,
                coApplicantName = coApplicantName,
                contactNumber = phone,
                email = email,
                photoBase64 = photoBase64,
                addOnly = current.role.isMember
            )

            if (updated == current) {
                onResult(true, msg(
                    "भरण्यासारखे काही नवीन नव्हते.",
                    "भरने के लिए कुछ नया नहीं था।",
                    "There was nothing new to fill in."
                ))
                return@safeLaunch
            }

            repository.updateResident(updated)
            _currentUser.value = updated
            onResult(true, msg("प्रोफाइल जतन केले!", "प्रोफ़ाइल सहेजी गई!", "Profile saved."))
        }
    }

    // Emergency contacts - chairman-managed

    fun saveEmergencyContact(
        contact: EmergencyContact,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (!canEditSociety) {
            onResult(false, msg(
                "फक्त अध्यक्षच तातडीचे संपर्क व्यवस्थापित करू शकतात.",
                "केवल अध्यक्ष ही आपातकालीन संपर्क प्रबंधित कर सकते हैं।",
                "Only the Chairman can manage emergency contacts."
            ))
            return
        }
        if (contact.titleEn.isBlank()) {
            onResult(false, msg(
                "संपर्काचे नाव लिहा.",
                "संपर्क का नाम लिखें।",
                "Enter a name for the contact."
            ))
            return
        }
        // Checked here as well as in the form: an emergency number that cannot
        // be dialled is the one thing this list must never hold.
        val digits = contact.phoneNumber.filter { it.isDigit() }
        if (digits.length !in EMERGENCY_PHONE_DIGITS) {
            onResult(false, msg(
                "वैध दूरध्वनी क्रमांक टाका (३ ते १५ अंक).",
                "मान्य फोन नंबर डालें (3 से 15 अंक)।",
                "Enter a valid phone number - between 3 and 15 digits."
            ))
            return
        }
        safeLaunch {
            repository.saveEmergencyContact(contact)
            onResult(true, msg("संपर्क जतन केला!", "संपर्क सहेजा गया!", "Contact saved."))
        }
    }

    fun deleteEmergencyContact(contact: EmergencyContact, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        if (!canEditSociety) {
            onResult(false, msg(
                "फक्त अध्यक्षच तातडीचे संपर्क व्यवस्थापित करू शकतात.",
                "केवल अध्यक्ष ही आपातकालीन संपर्क प्रबंधित कर सकते हैं।",
                "Only the Chairman can manage emergency contacts."
            ))
            return
        }
        safeLaunch {
            repository.deleteEmergencyContact(contact)
            onResult(true, msg("संपर्क हटवला!", "संपर्क हटाया गया!", "Contact removed."))
        }
    }

    fun updatePin(newPin: String) {
        safeLaunch {
            val cur = _currentUser.value
            if (cur != null) {
                repository.updateResidentPin(cur.flatNumber, newPin)
                _currentUser.value = cur.copy(pin = newPin)
                showMessage(if (_language.value == AppLanguage.MARATHI) "सुरक्षा पिन बदलला!" else "Security PIN changed successfully!")
            }
        }
    }

    private companion object {
        const val TAG = "SocietyViewModel"

        /** Marks a bill line settled from held credit rather than a fresh payment. */
        const val ADVANCE_ADJUSTMENT_REF = "ADVANCE-ADJUSTMENT"

        /** Where the "new society registered" notice comes from and goes to. */
        const val REGISTRATION_EMAIL_FROM = "r@multisarv.com"
        const val REGISTRATION_EMAIL_TO = "info@multisarv.com"
    }

    // Generate Printable / Downloadable Balance Sheet & Accounting Summary text
    fun generateExportSummary(): String {
        val soc = societyInfo.value
        val fin = financialOverview.value
        val billList = bills.value
        val expList = expenses.value
        val revList = revenues.value

        val dateStr = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        return buildString {
            appendLine("=====================================================")
            appendLine("       ${soc?.name ?: "Housing Society"}")
            appendLine("       Registration No: ${soc?.registrationNumber ?: "N/A"}")
            appendLine("       Address: ${soc?.address ?: "N/A"}")
            appendLine("=====================================================")
            appendLine("GENERAL BALANCE SHEET & FINANCIAL STATEMENT")
            appendLine("Generated on: $dateStr")
            // The period the society actually bills for, not a fixed year.
            val months = soc.billingMonths()
            appendLine(
                "Billing Period: ${months.lastOrNull() ?: "-"} to ${months.firstOrNull() ?: "-"}"
            )
            appendLine("-----------------------------------------------------")
            appendLine("TREASURY & BALANCES:")
            appendLine("  • Net Treasury Fund   : ₹%,.2f".format(fin.netTreasuryBalance))
            appendLine("  • Bank Balance        : ₹%,.2f (%s)".format(fin.bankBalance, soc?.bankAccountName ?: "Bank"))
            appendLine("  • Cash in Hand        : ₹%,.2f".format(fin.cashBalance))
            appendLine("  • Total Outstanding   : ₹%,.2f".format(fin.totalOutstandingDues))
            appendLine("-----------------------------------------------------")
            appendLine("INCOME SUMMARY:")
            appendLine("  • Maintenance Receipts: ₹%,.2f".format(fin.totalMaintenanceCollected))
            appendLine("  • Non-Fee Revenues    : ₹%,.2f".format(fin.totalNonFeeRevenue))
            appendLine("  • Gross Total Income  : ₹%,.2f".format(fin.totalGrossIncome))
            appendLine("-----------------------------------------------------")
            appendLine("EXPENDITURE SUMMARY:")
            appendLine("  • Total Operational Exp: ₹%,.2f".format(fin.totalOperatingExpenses))
            appendLine("  • NET SURPLUS / (DEF)  : ₹%,.2f".format(fin.netSurplus))
            appendLine("-----------------------------------------------------")
            appendLine("TOP OPERATING EXPENSES:")
            expList.take(5).forEach { exp ->
                appendLine("  - [${exp.date}] ${exp.category.name}: ₹%,.2f (%s)".format(exp.amount, exp.title))
            }
            appendLine("-----------------------------------------------------")
            appendLine("MEMBER DUES SUMMARY:")
            appendLine("  - Total Flats: ${soc?.totalFlats ?: 0} | Paid: ${fin.paidFlatsCount} | Pending: ${fin.pendingFlatsCount}")
            appendLine("=====================================================")
            appendLine("Verified & Certified by:")
            appendLine("Hon. Chairman / Hon. Secretary / Hon. Treasurer")
            appendLine("SocietySarv Digital Accounting Management System")
        }
    }
}
