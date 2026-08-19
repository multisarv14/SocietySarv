package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseModule
import com.example.data.entity.Complaint
import com.example.data.entity.ComplaintCategory
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
import com.example.data.entity.UserRole
import com.example.data.entity.VisitorLog
import com.example.data.entity.VisitorPurpose
import com.example.data.entity.VisitorStatus
import com.example.data.localization.AppLanguage
import com.example.data.repository.SocietyRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    GATE,
    COMMUNITY,
    RESIDENT_PORTAL
}

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

class SocietyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SocietyRepository

    init {
        // Give the Firestore SDK an identity before the first read, then talk to
        // Cloud Firestore for everything. There is no local seed data any more -
        // the society, its members and all ledgers live in societysarv-app-db.
        FirebaseModule.ensureSignedIn()
        repository = SocietyRepository(application)
    }

    private val _language = MutableStateFlow(AppLanguage.ENGLISH)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _currentUser = MutableStateFlow<Resident?>(null)
    val currentUser: StateFlow<Resident?> = _currentUser.asStateFlow()

    private val _selectedTab = MutableStateFlow(NavDestination.DASHBOARD)
    val selectedTab: StateFlow<NavDestination> = _selectedTab.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

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

    // Financial KPI Summary calculation
    val financialOverview: StateFlow<FinancialOverview> = combine(
        bills, expenses, revenues, societyInfo
    ) { billsList, expList, revList, socInfo ->
        val maintenanceCollected = billsList.filter { it.status == PaymentStatus.PAID || it.amountPaid > 0 }
            .sumOf { it.amountPaid }
        val nonFeeTotal = revList.sumOf { it.amount }
        val grossIncome = maintenanceCollected + nonFeeTotal
        val totalExpenses = expList.sumOf { it.amount }
        val netSurplus = grossIncome - totalExpenses

        val outstandingDues = billsList.filter { it.status != PaymentStatus.PAID }
            .sumOf { it.totalAmount - it.amountPaid }

        val paidCount = billsList.count { it.status == PaymentStatus.PAID }
        val pendingCount = billsList.count { it.status != PaymentStatus.PAID }

        val cashBal = socInfo?.currentBalanceCash ?: 0.0
        val bankBal = socInfo?.currentBalanceBank ?: 0.0

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
    fun login(societyCode: String, flatOrMobile: String, pin: String, onResult: (Boolean, String) -> Unit) {
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

            val society = repository.getSocietyInfoSync()
            if (society == null) {
                onResult(false, msg(
                    "अद्याप कोणतीही सोसायटी नोंदणीकृत नाही. प्रथम 'नवीन सोसायटी नोंदणी' करा.",
                    "अभी तक कोई सोसायटी पंजीकृत नहीं है। पहले 'नई सोसायटी पंजीकरण' करें।",
                    "No society is registered yet. Use 'Register New Society' first."
                ))
                return@safeLaunch
            }

            if (!society.societyCode.trim().equals(code, ignoreCase = true)) {
                onResult(false, msg(
                    "अवैध सोसायटी कोड!",
                    "अमान्य सोसायटी कोड!",
                    "Invalid society code!"
                ))
                return@safeLaunch
            }

            val resident = repository.getResidentByFlat(identifier)
                ?: repository.getResidentByMobile(identifier)

            if (resident == null) {
                onResult(false, msg(
                    "हा फ्लॅट किंवा मोबाईल क्रमांक नोंदणीकृत नाही!",
                    "यह फ्लैट या मोबाइल नंबर पंजीकृत नहीं है!",
                    "That flat or mobile number is not registered."
                ))
                return@safeLaunch
            }

            if (resident.pin.isBlank()) {
                onResult(false, msg(
                    "या सभासदासाठी पिन सेट केलेला नाही. समितीकडून पिन सेट करून घ्या.",
                    "इस सदस्य के लिए पिन सेट नहीं है। समिति से पिन सेट करवाएं।",
                    "No PIN is set for this member yet. Ask the committee to set one."
                ))
                return@safeLaunch
            }

            if (resident.pin != enteredPin) {
                onResult(false, msg("चुकीचा पिन!", "गलत पिन!", "Incorrect PIN."))
                return@safeLaunch
            }

            _currentUser.value = resident
            _selectedTab.value = landingTabFor(resident.role)
            onResult(true, msg("यशस्वी प्रवेश!", "सफल लॉगिन!", "Login successful!"))
        }
    }

    /** Where a role lands after signing in. */
    private fun landingTabFor(role: UserRole): NavDestination = when (role) {
        UserRole.GUARD -> NavDestination.GATE
        UserRole.RESIDENT, UserRole.TENANT -> NavDestination.RESIDENT_PORTAL
        else -> NavDestination.DASHBOARD
    }

    fun logout() {
        _currentUser.value = null
        _selectedTab.value = NavDestination.DASHBOARD
    }

    // Society Registration Wizard
    /**
     * Creates the society master document and its first member (the chairman)
     * in Firestore, then signs that chairman straight in. The PIN entered in the
     * wizard is the one the chairman logs in with afterwards.
     */
    fun registerNewSociety(
        name: String,
        regNumber: String,
        code: String,
        address: String,
        wings: Int,
        flats: Int,
        chairmanName: String,
        chairmanPhone: String,
        chairmanEmail: String,
        chairmanFlat: String,
        chairmanPin: String,
        bankName: String,
        bankAccount: String,
        ifsc: String,
        initialBankBalance: Double,
        tariff1BHK: Double,
        tariff2BHK: Double,
        tariff3BHK: Double,
        tariffShop: Double,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        safeLaunch {
            val societyCode = code.trim().uppercase()
            val flatNumber = chairmanFlat.trim().uppercase()
            val pin = chairmanPin.trim()

            if (name.isBlank() || societyCode.isBlank() || chairmanName.isBlank() ||
                flatNumber.isBlank() || chairmanPhone.isBlank() || pin.isBlank()
            ) {
                onResult(false, msg(
                    "कृपया सर्व आवश्यक माहिती भरा.",
                    "कृपया सभी आवश्यक जानकारी भरें।",
                    "Please fill in every required field."
                ))
                return@safeLaunch
            }

            // One society per install - re-registering would orphan the existing
            // members and ledgers that already point at the current society.
            val existing = repository.getSocietyInfoSync()
            if (existing != null && existing.societyCode.isNotBlank()) {
                onResult(false, msg(
                    "'${existing.name}' (कोड ${existing.societyCode}) आधीच नोंदणीकृत आहे. कृपया लॉगिन करा.",
                    "'${existing.name}' (कोड ${existing.societyCode}) पहले से पंजीकृत है। कृपया लॉगिन करें।",
                    "'${existing.name}' (code ${existing.societyCode}) is already registered. Please log in instead."
                ))
                return@safeLaunch
            }

            val newSoc = SocietyInfo(
                name = name.trim(),
                registrationNumber = regNumber.trim(),
                societyCode = societyCode,
                address = address.trim(),
                totalWings = wings,
                totalFlats = flats,
                bankAccountName = bankName.trim(),
                bankAccountNumber = bankAccount.trim(),
                ifscCode = ifsc.trim().uppercase(),
                currentBalanceCash = 0.0,
                currentBalanceBank = initialBankBalance,
                tariff1BHK = tariff1BHK,
                tariff2BHK = tariff2BHK,
                tariff3BHK = tariff3BHK,
                tariffPenthouse = tariff3BHK * 1.5,
                tariffShop = tariffShop
            )
            repository.insertOrUpdateSociety(newSoc)

            val chairman = Resident(
                flatNumber = flatNumber,
                wing = flatNumber.substringBefore("-").take(1).ifBlank { "A" },
                ownerName = chairmanName.trim(),
                contactNumber = chairmanPhone.trim(),
                email = chairmanEmail.trim(),
                role = UserRole.CHAIRMAN,
                pin = pin,
                flatType = FlatType.THREE_BHK,
                balanceDue = 0.0
            )
            repository.insertResident(chairman)

            _currentUser.value = repository.getResidentByFlat(flatNumber) ?: chairman
            _selectedTab.value = NavDestination.DASHBOARD
            onResult(true, msg(
                "सोसायटी नोंदणी यशस्वी! लॉगिनसाठी कोड $societyCode व फ्लॅट $flatNumber वापरा.",
                "सोसायटी पंजीकरण सफल! लॉगिन के लिए कोड $societyCode और फ्लैट $flatNumber उपयोग करें।",
                "Society registered! Log in with code $societyCode and flat $flatNumber."
            ))
        }
    }

    // Record Maintenance Collection
    fun recordMaintenancePayment(
        bill: MaintenanceBill,
        amount: Double,
        mode: PaymentMode,
        ref: String,
        dateStr: String
    ) {
        safeLaunch {
            val now = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            val receiptNo = "REC-${SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())}-${Random.nextInt(1000, 9999)}"
            val updatedBill = bill.copy(
                amountPaid = bill.amountPaid + amount,
                status = if ((bill.amountPaid + amount) >= bill.totalAmount) PaymentStatus.PAID else PaymentStatus.PARTIAL,
                paidDate = dateStr.ifBlank { now },
                paymentMode = mode,
                transactionRef = ref,
                receiptNumber = receiptNo
            )
            repository.updateBill(updatedBill)

            // Update society treasury
            val soc = repository.getSocietyInfoSync()
            if (soc != null) {
                val updatedSoc = if (mode == PaymentMode.CASH) {
                    soc.copy(currentBalanceCash = soc.currentBalanceCash + amount)
                } else {
                    soc.copy(currentBalanceBank = soc.currentBalanceBank + amount)
                }
                repository.updateSociety(updatedSoc)
            }

            // Update resident dues
            val res = repository.getResidentByFlat(bill.flatNumber)
            if (res != null) {
                val newDue = (res.balanceDue - amount).coerceAtLeast(0.0)
                repository.updateResident(res.copy(balanceDue = newDue))
            }

            showMessage(if (_language.value == AppLanguage.MARATHI) "पावती क्र. $receiptNo यशस्वीरित्या तयार झाली!" else "Receipt #$receiptNo generated successfully!")
        }
    }

    // Add new Maintenance Bill
    fun generateBill(
        flatNumber: String,
        residentName: String,
        month: String,
        baseAmount: Double,
        water: Double,
        sinking: Double,
        repair: Double,
        parking: Double,
        lateFee: Double,
        dueDate: String
    ) {
        safeLaunch {
            val total = baseAmount + water + sinking + repair + parking + lateFee
            val newBill = MaintenanceBill(
                flatNumber = flatNumber,
                residentName = residentName,
                billMonth = month,
                baseAmount = baseAmount,
                waterCharges = water,
                sinkingFund = sinking,
                repairFund = repair,
                parkingCharges = parking,
                lateFeeAmount = lateFee,
                totalAmount = total,
                amountPaid = 0.0,
                status = PaymentStatus.UNPAID,
                dueDate = dueDate,
                receiptNumber = "BILL-${SimpleDateFormat("yyMM", Locale.getDefault()).format(Date())}-${Random.nextInt(100, 999)}"
            )
            repository.insertBill(newBill)

            val res = repository.getResidentByFlat(flatNumber)
            if (res != null) {
                repository.updateResident(res.copy(balanceDue = res.balanceDue + total))
            }
            showMessage(if (_language.value == AppLanguage.MARATHI) "$flatNumber साठी मासिक बिल तयार केले!" else "Maintenance bill created for $flatNumber!")
        }
    }

    // Expense Logger
    fun addExpense(
        title: String,
        category: ExpenseCategory,
        amount: Double,
        date: String,
        vendor: String,
        vendorPhone: String,
        mode: PaymentMode,
        notes: String
    ) {
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
            showMessage(if (_language.value == AppLanguage.MARATHI) "₹$amount खर्च नोंदवला ($expNo)" else "Recorded expense ₹$amount ($expNo)")
        }
    }

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
        tenantName: String,
        phone: String,
        email: String,
        role: UserRole,
        familyCount: Int,
        twoWheelers: Int,
        fourWheelers: Int,
        twoWheelerNo: String,
        fourWheelerNo: String,
        parkingSlot: String,
        flatType: FlatType,
        sqft: Int,
        pin: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        safeLaunch {
            val flat = flatNumber.trim().uppercase()
            if (flat.isBlank() || ownerName.isBlank() || phone.isBlank() || pin.isBlank()) {
                onResult(false, msg(
                    "फ्लॅट क्रमांक, नाव, मोबाईल व पिन आवश्यक आहे.",
                    "फ्लैट नंबर, नाम, मोबाइल और पिन आवश्यक है।",
                    "Flat number, name, mobile and PIN are all required."
                ))
                return@safeLaunch
            }
            if (repository.getResidentByFlat(flat) != null) {
                onResult(false, msg(
                    "फ्लॅट $flat आधीच नोंदणीकृत आहे.",
                    "फ्लैट $flat पहले से पंजीकृत है।",
                    "Flat $flat is already registered."
                ))
                return@safeLaunch
            }
            val resident = Resident(
                flatNumber = flat,
                wing = wing.trim().ifBlank { flat.substringBefore("-").take(1) },
                ownerName = ownerName.trim(),
                tenantName = tenantName.trim(),
                contactNumber = phone.trim(),
                email = email.trim(),
                role = role,
                pin = pin.trim(),
                familyMemberCount = familyCount,
                vehicleTwoWheelerCount = twoWheelers,
                vehicleFourWheelerCount = fourWheelers,
                twoWheelerNumber = twoWheelerNo,
                fourWheelerNumber = fourWheelerNo,
                parkingSlot = parkingSlot,
                flatType = flatType,
                squareFeet = sqft,
                balanceDue = 0.0,
                isOwnerLiving = tenantName.isBlank()
            )
            repository.insertResident(resident)
            onResult(true, msg(
                "सभासद $flat जोडले! ते या फ्लॅट क्रमांक व पिनने लॉगिन करू शकतात.",
                "सदस्य $flat जोड़ा गया! वे इस फ्लैट नंबर और पिन से लॉगिन कर सकते हैं।",
                "Member $flat added. They can log in with this flat number and PIN."
            ))
        }
    }

    fun updateResident(resident: Resident) {
        safeLaunch {
            repository.updateResident(resident)
            showMessage(if (_language.value == AppLanguage.MARATHI) "माहिती अद्ययावत केली!" else "Resident details updated!")
        }
    }

    fun deleteResident(resident: Resident) {
        safeLaunch {
            repository.deleteResident(resident)
            showMessage(if (_language.value == AppLanguage.MARATHI) "सभासद हटवले!" else "Resident removed!")
        }
    }

    fun updateTariffs(bhk1: Double, bhk2: Double, bhk3: Double, shop: Double) {
        safeLaunch {
            val soc = repository.getSocietyInfoSync()
            if (soc != null) {
                repository.updateSociety(
                    soc.copy(
                        tariff1BHK = bhk1,
                        tariff2BHK = bhk2,
                        tariff3BHK = bhk3,
                        tariffShop = shop
                    )
                )
                showMessage(if (_language.value == AppLanguage.MARATHI) "मासिक दरपत्रक अद्ययावत केले!" else "Tariff slabs updated successfully!")
            }
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

    fun updateComplaintStatus(complaint: Complaint, newStatus: TicketStatus, notes: String) {
        safeLaunch {
            repository.updateComplaintStatus(complaint, newStatus, notes)
            showMessage(if (_language.value == AppLanguage.MARATHI) "तक्रार स्थिती अद्ययावत केली!" else "Ticket status updated!")
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
            appendLine("Financial Year: 2026-2027")
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
