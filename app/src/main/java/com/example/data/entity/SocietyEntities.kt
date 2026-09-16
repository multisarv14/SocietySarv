package com.example.data.entity

enum class UserRole {
    // Managing committee
    CHAIRMAN,
    SECRETARY,
    TREASURER,
    COMMITTEE,
    // Members living in the society
    RESIDENT,
    TENANT,
    // Society staff - they sign in with a staff code instead of a flat number
    GUARD,
    HOUSEKEEPING,
    MAINTENANCE_STAFF,
    ACCOUNTANT,
    MANAGER
}

/** Roles that are employed by the society rather than living in it. */
val UserRole.isStaff: Boolean
    get() = this in setOf(
        UserRole.GUARD,
        UserRole.HOUSEKEEPING,
        UserRole.MAINTENANCE_STAFF,
        UserRole.ACCOUNTANT,
        UserRole.MANAGER
    )

/** The security team - the only role the Security menu is shown to. */
val UserRole.isSecurityTeam: Boolean
    get() = this == UserRole.GUARD

/** Roles a chairman can hire into, in the order the Add Staff sheet lists them. */
val STAFF_ROLES: List<UserRole> = listOf(
    UserRole.GUARD,
    UserRole.HOUSEKEEPING,
    UserRole.MAINTENANCE_STAFF,
    UserRole.ACCOUNTANT,
    UserRole.MANAGER
)

/** Parking allotted to a unit. */
enum class ParkingType(val labelEn: String, val labelMr: String, val prefix: String) {
    COVERED("Covered Parking", "आच्छादित पार्किंग", "C"),
    OPEN("Open Parking", "खुले पार्किंग", "O"),
    NONE("No Parking", "पार्किंग नाही", "")
}

enum class FlatType(val displayName: String) {
    ONE_BHK("1 BHK"),
    TWO_BHK("2 BHK"),
    THREE_BHK("3 BHK"),
    PENTHOUSE("Penthouse"),
    COMMERCIAL_SHOP("Commercial Shop")
}

enum class PaymentMode(val labelEn: String, val labelMr: String) {
    UPI("UPI", "यूपीआय"),
    BANK_TRANSFER("Bank Transfer (NEFT/IMPS)", "बँक ट्रान्सफर"),
    CHEQUE("Cheque", "धनादेश"),
    CASH("Cash", "रोख")
}

enum class PaymentStatus {
    PAID,
    PARTIAL,
    UNPAID
}

enum class ExpenseCategory(val labelEn: String, val labelMr: String) {
    WATER_TANKER("Water Tanker", "पाण्याचे टँकर"),
    LIFT_AMC("Lift AMC & Repairs", "लिफ्ट देखभाल (AMC)"),
    SECURITY_SALARY("Security Agency Salary", "सुरक्षा रक्षक मानधन"),
    GARDENING("Gardening & Landscape", "बागकाम व सुशोभीकरण"),
    HOUSEKEEPING("Housekeeping & Cleaning", "स्वच्छता व कचरा व्यवस्थापन"),
    ELECTRICITY("Common Electricity Bill", "सामायिक वीज बिल"),
    REPAIRS("General Repairs & Painting", "दुरुस्ती व रंगकाम"),
    MISCELLANEOUS("Miscellaneous / Office", "इतर / कार्यालयीन खर्च")
}

enum class RevenueSource(val labelEn: String, val labelMr: String) {
    CLUBHOUSE_BOOKING("Clubhouse Booking", "क्लबहाऊस आरक्षण"),
    PARKING_FEE("Extra Parking Charges", "अतिरिक्त पार्किंग शुल्क"),
    TOWER_RENT("Mobile Tower Rent", "मोबाईल टॉवर भाडे"),
    SOLAR_CREDIT("Solar Net Metering Credit", "सौर ऊर्जा क्रेडिट"),
    HALL_RENTAL("Community Hall Rental", "सभागृह भाडे"),
    PENALTY_FINE("Late Penalty / Rule Fine", "दंड वसुली")
}

enum class VisitorStatus {
    INSIDE,
    EXITED,
    PRE_APPROVED,
    REJECTED
}

enum class VisitorPurpose(val labelEn: String, val labelMr: String) {
    DELIVERY("Delivery (Amazon/Zomato/Blinkit)", "डिलिव्हरी"),
    GUEST("Family / Guest", "पाहुणे / नातेवाईक"),
    CAB("Cab / Taxi", "कॅब / टॅक्सी"),
    SERVICE("Plumber / Electrician / Maid", "सेवा कर्मचारी / कारागीर"),
    OTHER("Other Official", "इतर")
}

enum class NoticeCategory(val labelEn: String, val labelMr: String) {
    AGM("AGM / General Meeting", "वार्षिक सर्वसाधारण सभा (AGM)"),
    MAINTENANCE("Maintenance Notice", "देखभाल शुल्क सूचना"),
    WATER("Water Supply Update", "पाणीपुरवठा नियोजन"),
    FESTIVAL("Celebration & Events", "सण व उत्सव"),
    GENERAL("General Advisory", "सर्वसाधारण सूचना")
}

enum class ComplaintCategory(val labelEn: String, val labelMr: String) {
    PLUMBING("Plumbing & Drainage", "प्लंबिंग व सांडपाणी"),
    ELECTRICAL("Electrical & Common Lights", "विद्युत व दिवे"),
    LIFT("Lift Malfunction", "लिफ्ट समस्या"),
    LEAKAGE("Water Leakage / Seepage", "गळती व सिपेज"),
    NOISE("Noise / Parking Dispute", "आवाज / पार्किंग वाद"),
    SECURITY("Gate / Security Issue", "सुरक्षा तक्रार"),
    OTHER("Other Issue", "इतर समस्या")
}

enum class TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED
}

/**
 * One sellable unit type in a society - "1 BHK", "Shop", "Office" - together
 * with the monthly maintenance tariff charged for it. The list is configured
 * during registration (Floor Inventory step) and stored on the society doc, so
 * a society is not limited to the fixed [FlatType] slabs.
 */
data class UnitTypeConfig(
    val name: String,
    val monthlyTariff: Double = 0.0
)

/** One unit type a wing holds, and how many units of it there are. */
data class WingUnitCount(
    val unitType: String,
    val quantity: Int = 0
)

/**
 * Floor inventory captured for a single wing: how many floors it has, and which
 * unit types it holds with how many units of each.
 *
 * The unit numbers are spread evenly over the floors, so a wing of 3 floors
 * holding 12 units becomes A-101…A-104, A-201…A-204, A-301…A-304. Types are
 * handed out in the order they were entered, so the first 6 of those units are
 * the first type, the next 6 the second, and so on.
 */
data class WingPlan(
    val name: String,
    val totalFloors: Int = 0,
    val unitCounts: List<WingUnitCount> = emptyList()
) {
    val totalUnits: Int get() = unitCounts.sumOf { it.quantity.coerceAtLeast(0) }

    /** Floors the units are spread over; a wing with none entered is one floor. */
    private val spreadOverFloors: Int get() = totalFloors.coerceAtLeast(1)

    /** Flat numbers this wing expands into, e.g. A-101, A-102, ... A-304. */
    fun generateUnitNumbers(): List<String> {
        val total = totalUnits
        if (total <= 0) return emptyList()
        val floors = spreadOverFloors
        // Round up, so the remainder lands on the last floor rather than being lost.
        val perFloor = (total + floors - 1) / floors
        val numbers = mutableListOf<String>()
        for (floor in 1..floors) {
            for (unit in 1..perFloor) {
                if (numbers.size >= total) return numbers
                numbers += "$name-$floor%02d".format(unit)
            }
        }
        return numbers
    }

    /**
     * The unit type of each generated number, in the same order and the same
     * length as [generateUnitNumbers].
     */
    fun unitTypesInOrder(): List<String> =
        unitCounts.flatMap { count -> List(count.quantity.coerceAtLeast(0)) { count.unitType } }
}

// 1. Society Overview & Config -> Firestore collection "society_info" (doc "main")
data class SocietyInfo(
    val docId: String = "main",
    val id: Int = 1,
    val name: String,
    val registrationNumber: String,
    val societyCode: String,
    val address: String,
    val totalWings: Int,
    val totalFlats: Int,
    val bankAccountName: String,
    val bankAccountNumber: String,
    val ifscCode: String,
    /**
     * The society's UPI id, e.g. "society@okhdfcbank". Every payment QR is
     * built from this - without it there is nothing real to pay into.
     */
    val upiId: String = "",
    /**
     * A payment QR the chairman uploaded, as a Base64 blob.
     *
     * Some societies bank with a provider whose QR carries more than the VPA -
     * a merchant id, a signature - and one built from the UPI id alone will not
     * do. When this is set it is the QR members are shown; otherwise the app
     * generates one from [upiId].
     */
    val paymentQrBase64: String = "",
    val currentBalanceCash: Double,
    val currentBalanceBank: Double,
    val lateFeePercentPerMonth: Double = 2.0,
    val lateFeeGraceDays: Int = 10,
    val tariff1BHK: Double = 1500.0,
    val tariff2BHK: Double = 2200.0,
    val tariff3BHK: Double = 3000.0,
    val tariffPenthouse: Double = 4500.0,
    val tariffShop: Double = 3500.0,
    /*
     * How one month's maintenance is divided between the society's funds.
     * These are shares of the maintenance amount, not additions to it, so they
     * are expected to add up to 100%.
     */
    val waterChargesPercent: Double = 10.0,
    val sinkingFundPercent: Double = 5.0,
    val repairFundPercent: Double = 20.0,
    val otherMaintenancePercent: Double = 65.0,
    /** Society logo as a Base64 blob (empty when none uploaded). */
    val logoBase64: String = "",
    // Address & contact captured in the registration wizard (Society Details).
    val country: String = "India",
    val state: String = "",
    val city: String = "",
    val pinCode: String = "",
    val contactNumber: String = "",
    val email: String = "",
    /** Wing names as entered in Unit Details, e.g. ["A", "B", "C"]. */
    val wingNames: List<String> = emptyList(),
    /** Per-wing floor inventory captured in the Floor Inventory step. */
    val wingPlans: List<WingPlan> = emptyList(),
    /** Unit types this society sells/charges for, with their monthly tariff. */
    val unitTypes: List<UnitTypeConfig> = emptyList(),
    /** How many covered / open parking bays exist. 0 falls back to one per unit. */
    val coveredParkingCount: Int = 0,
    val openParkingCount: Int = 0,
    /**
     * The month billing starts from for this society, as "April 2026". Every
     * billing month list in the app runs from here to the current month, so a
     * society can never be billed for a month before it went live.
     */
    val billingStartMonth: String = ""
)

/**
 * Every billable month for this society, newest first: from
 * [SocietyInfo.billingStartMonth] up to the current month. Societies that never
 * configured a start month fall back to the last twelve months.
 */
fun SocietyInfo?.billingMonths(): List<String> {
    val formatter = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.ENGLISH)
    val now = java.util.Calendar.getInstance()
    val start = this?.billingStartMonth?.takeIf { it.isNotBlank() }
        ?.let { runCatching { formatter.parse(it) }.getOrNull() }
        ?.let { java.util.Calendar.getInstance().apply { time = it } }

    if (start == null) {
        return (0 downTo -11).map { offset ->
            (now.clone() as java.util.Calendar).apply { add(java.util.Calendar.MONTH, offset) }
                .let { formatter.format(it.time) }
        }
    }

    val months = mutableListOf<String>()
    val cursor = start.clone() as java.util.Calendar
    // Guard against a start month in the future, and against a runaway list.
    while (months.size < MAX_BILLING_MONTHS &&
        (cursor.get(java.util.Calendar.YEAR) < now.get(java.util.Calendar.YEAR) ||
            (cursor.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR) &&
                cursor.get(java.util.Calendar.MONTH) <= now.get(java.util.Calendar.MONTH)))
    ) {
        months += formatter.format(cursor.time)
        cursor.add(java.util.Calendar.MONTH, 1)
    }
    return months.reversed().ifEmpty { listOf(formatter.format(start.time)) }
}

/** Twelve years of months is far more than any society will ever bill for. */
private const val MAX_BILLING_MONTHS = 144

/**
 * How one month's maintenance is split between the society's funds.
 *
 * These are shares of the maintenance amount rather than charges added on top
 * of it, so the four always add back up to the amount they were split from.
 */
data class MaintenanceSplit(
    val water: Double,
    val sinking: Double,
    val repair: Double,
    val other: Double
) {
    val total: Double get() = water + sinking + repair + other
}

/** The four configured shares added together - normally 100. */
val SocietyInfo?.maintenancePercentTotal: Double
    get() = if (this == null) 0.0 else {
        waterChargesPercent + sinkingFundPercent + repairFundPercent + otherMaintenancePercent
    }

/** True when the configured shares add up to 100%, within a paisa of rounding. */
val SocietyInfo?.hasBalancedMaintenancePercents: Boolean
    get() = kotlin.math.abs(maintenancePercentTotal - 100.0) < 0.01

/**
 * Splits a month's maintenance across the society's configured percentages.
 *
 * Each share is rounded to paise, and whatever the rounding leaves over lands
 * on Other/Common Maintenance - so the four amounts always add back up to
 * [totalAmount] exactly, however the percentages divide.
 */
fun SocietyInfo?.splitMaintenance(totalAmount: Double): MaintenanceSplit {
    if (this == null || totalAmount <= 0.0) return MaintenanceSplit(0.0, 0.0, 0.0, 0.0)
    val water = roundToPaise(totalAmount * waterChargesPercent / 100.0)
    val sinking = roundToPaise(totalAmount * sinkingFundPercent / 100.0)
    val repair = roundToPaise(totalAmount * repairFundPercent / 100.0)
    return MaintenanceSplit(
        water = water,
        sinking = sinking,
        repair = repair,
        other = roundToPaise(totalAmount - water - sinking - repair)
    )
}

/** What the society actually holds, split between the bank and the cash box. */
data class TreasuryBalance(val cash: Double, val bank: Double) {
    val total: Double get() = cash + bank
}

/**
 * The society's balance, worked out from its own ledger.
 *
 * Opening balance, plus what members actually paid and what non-fee revenue
 * came in, less what was spent. Deliberately not a stored running total: one of
 * those drifts the moment a bill or a voucher is deleted, which is how a
 * society with no entries left ended up still showing money it never had.
 */
fun SocietyInfo?.treasuryBalance(
    bills: List<MaintenanceBill>,
    expenses: List<ExpenseRecord>,
    revenues: List<NonFeeRevenue>,
    /**
     * Units holding advance credit. The society has that money in hand even
     * though no bill carries it yet, so it is counted here and drops out again
     * the moment the credit is set against a bill.
     */
    advanceHolders: List<Resident> = emptyList()
): TreasuryBalance {
    fun isCash(mode: PaymentMode?) = mode == PaymentMode.CASH

    val heldAdvances = advanceHolders.filter { it.advanceBalance > 0.0 }
    val cashIn = bills.filter { isCash(it.paymentMode) }.sumOf { it.amountPaid } +
        revenues.filter { isCash(it.paymentMode) }.sumOf { it.amount } +
        heldAdvances.filter { isCash(it.advanceMode) }.sumOf { it.advanceBalance }
    val cashOut = expenses.filter { isCash(it.paymentMode) }.sumOf { it.amount }
    val bankIn = bills.filterNot { isCash(it.paymentMode) }.sumOf { it.amountPaid } +
        revenues.filterNot { isCash(it.paymentMode) }.sumOf { it.amount } +
        heldAdvances.filterNot { isCash(it.advanceMode) }.sumOf { it.advanceBalance }
    val bankOut = expenses.filterNot { isCash(it.paymentMode) }.sumOf { it.amount }

    return TreasuryBalance(
        cash = roundToPaise((this?.currentBalanceCash ?: 0.0) + cashIn - cashOut),
        bank = roundToPaise((this?.currentBalanceBank ?: 0.0) + bankIn - bankOut)
    )
}

/** The one rounding rule every maintenance amount goes through. */
fun roundToPaise(value: Double): Double = kotlin.math.round(value * 100.0) / 100.0

/**
 * Every parking number of one type, e.g. C-01 … C-24. A society that never
 * configured a count gets one bay per unit so the dropdown is never empty.
 */
fun SocietyInfo.parkingNumbers(type: ParkingType): List<String> {
    val configured = when (type) {
        ParkingType.COVERED -> coveredParkingCount
        ParkingType.OPEN -> openParkingCount
        ParkingType.NONE -> return emptyList()
    }
    val count = if (configured > 0) configured else totalFlats
    return (1..count).map { "${type.prefix}-%02d".format(it) }
}

// 2. Resident / Flat Member -> Firestore collection "residents" (doc id = flatNumber)
data class Resident(
    val docId: String = "",
    val id: Int = 0,
    val flatNumber: String,
    val wing: String,
    val ownerName: String,
    /**
     * The second name on the unit's paperwork - a spouse, a parent, a business
     * partner. Societies need it on a receipt and in the register, and only the
     * member reliably knows it, so the member can fill it in themselves.
     */
    val coApplicantName: String = "",
    val tenantName: String = "",
    /** The tenant's own mobile number, when the flat is let out. */
    val tenantContact: String = "",
    /**
     * The rent agreement as a Base64 blob. Firestore caps a document at 1 MiB,
     * so the upload refuses anything that would not fit - see RENT_AGREEMENT_MAX_BYTES.
     */
    val rentAgreementBase64: String = "",
    val contactNumber: String,
    val email: String = "",
    val role: UserRole = UserRole.RESIDENT,
    val pin: String = "",
    val familyMemberCount: Int = 1,
    val vehicleTwoWheelerCount: Int = 0,
    val vehicleFourWheelerCount: Int = 0,
    val twoWheelerNumber: String = "",
    val fourWheelerNumber: String = "",
    val parkingSlot: String = "",
    val parkingType: ParkingType = ParkingType.NONE,
    /**
     * The society's own unit type for this unit - "1 BHK", "Shop", "Office" -
     * as named in the floor inventory. This is what the monthly tariff is
     * looked up against; [flatType] is only the nearest built-in slab.
     */
    val unitType: String = "",
    val flatType: FlatType = FlatType.TWO_BHK,
    val squareFeet: Int = 0,
    val balanceDue: Double = 0.0,
    /**
     * Money this unit has paid over and above what it owed.
     *
     * A member who hands over a round figure often overshoots the arrears. The
     * difference is held here and taken off the next bill raised for the unit,
     * so it is neither lost nor refunded in cash.
     */
    val advanceBalance: Double = 0.0,
    /**
     * Which account the held advance went into, so the society's balance counts
     * it in the right place until it is set against a bill. It follows the most
     * recent overpayment - a unit rarely holds credit from two receipts at once.
     */
    val advanceMode: PaymentMode = PaymentMode.BANK_TRANSFER,
    val isOwnerLiving: Boolean = true,
    /** Member photo as a Base64 blob (empty when none uploaded). */
    val photoBase64: String = "",
    /**
     * Job title shown on a staff profile ("Night Guard", "Sweeper"). Blank for
     * residents - the [role] already says everything about them.
     */
    val staffDesignation: String = "",
    /** A staff member who has left keeps their record but cannot sign in. */
    val isActive: Boolean = true
)

/**
 * The monthly maintenance charged for one unit.
 *
 * The society's own unit type wins - that is what the committee prices under
 * Maintenance > Tariff. Only when a unit has no type against it does this fall
 * back to the nearest built-in slab, so an unconfigured unit still bills
 * something rather than nothing.
 */
fun SocietyInfo?.monthlyTariffFor(resident: Resident?): Double {
    if (this == null) return 0.0
    val named = unitTypes.firstOrNull { type ->
        type.name.isNotBlank() && type.name.equals(resident?.unitType?.trim(), ignoreCase = true)
    }
    if (named != null && named.monthlyTariff > 0.0) return named.monthlyTariff

    val bySlab = unitTypes.firstOrNull { type ->
        type.name.replace(" ", "")
            .equals(resident?.flatType?.displayName?.replace(" ", ""), ignoreCase = true)
    }
    if (bySlab != null && bySlab.monthlyTariff > 0.0) return bySlab.monthlyTariff

    return when (resident?.flatType) {
        FlatType.ONE_BHK -> tariff1BHK
        FlatType.THREE_BHK -> tariff3BHK
        FlatType.PENTHOUSE -> tariffPenthouse
        FlatType.COMMERCIAL_SHOP -> tariffShop
        else -> tariff2BHK
    }
}

/** Every unit type name this society has, for the pickers that offer them. */
fun SocietyInfo?.unitTypeNames(): List<String> {
    if (this == null) return emptyList()
    val fromWings = wingPlans.flatMap { plan -> plan.unitCounts.map { it.unitType } }
    return (fromWings + unitTypes.map { it.name })
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
}

/**
 * The largest rent agreement that can be stored.
 *
 * A Firestore document is capped at 1 MiB and Base64 adds a third on top, so
 * anything past this simply would not save. Refusing it up front beats a write
 * that fails silently after the committee thinks the file is filed.
 */
const val RENT_AGREEMENT_MAX_BYTES: Int = 600 * 1024

/** Staff sign in with their staff code; residents with their flat number. */
val Resident.loginIdentifier: String
    get() = flatNumber

/** True for the people the membership register is about, as opposed to staff. */
val UserRole.isMember: Boolean
    get() = this == UserRole.RESIDENT || this == UserRole.TENANT

/**
 * Applies what someone typed into their own profile form.
 *
 * A member fills in blanks; they do not rewrite the register. Their name, unit,
 * mobile number and the rest are the society's own record of who lives where,
 * and a register a member can quietly change is not a register - a wrong mobile
 * number on it is a member the committee can no longer reach. So when
 * [addOnly] is set, every detail already on file is kept exactly as it is,
 * however the form was filled in, and only genuinely empty fields take what was
 * typed.
 *
 * The committee and the staff edit their own details freely; the rule is about
 * the membership register, not about everybody's contact card.
 */
fun Resident.withProfileEdits(
    ownerName: String,
    coApplicantName: String,
    contactNumber: String,
    email: String,
    photoBase64: String,
    addOnly: Boolean
): Resident {
    fun settle(existing: String, entered: String): String = when {
        !addOnly -> entered.trim()
        existing.isNotBlank() -> existing
        else -> entered.trim()
    }

    return copy(
        // A name is the one thing that can never be blanked: a unit with no
        // name against it reads as vacant everywhere else in the app.
        ownerName = settle(this.ownerName, ownerName).ifBlank { this.ownerName },
        coApplicantName = settle(this.coApplicantName, coApplicantName),
        contactNumber = settle(this.contactNumber, contactNumber),
        email = settle(this.email, email),
        photoBase64 = settle(this.photoBase64, photoBase64)
    )
}

// 3. Maintenance Billing & Collection -> Firestore collection "maintenance_bills"
data class MaintenanceBill(
    val docId: String = "",
    val id: Int = 0,
    val flatNumber: String,
    val residentName: String,
    val billMonth: String, // e.g. "August 2026"
    val baseAmount: Double,
    val waterCharges: Double = 0.0,
    val sinkingFund: Double = 0.0,
    val repairFund: Double = 0.0,
    val parkingCharges: Double = 0.0,
    val nonOccupancyCharges: Double = 0.0,
    val lateFeeAmount: Double = 0.0,
    val totalAmount: Double,
    val amountPaid: Double = 0.0,
    val status: PaymentStatus = PaymentStatus.UNPAID,
    val dueDate: String,
    val paidDate: String = "",
    val paymentMode: PaymentMode? = null,
    val transactionRef: String = "",
    val receiptNumber: String = "",
    /** Scanned payment receipt as a Base64 blob. */
    val receiptImageBase64: String = ""
)

/*
 * Bill arithmetic lives with the entity so the list, the Generate Bill sheet
 * and the PDF receipt can never disagree about what a bill comes to.
 *
 *   charges  = every line item except the late fee
 *   payable  = charges + late fee
 *   pending  = payable - what has actually been paid
 */

/** Every charge on the bill except the late fee. */
val MaintenanceBill.chargesSubtotal: Double
    get() = baseAmount + waterCharges + sinkingFund + repairFund + parkingCharges + nonOccupancyCharges

/** What the member owes for this bill, late fee included. */
val MaintenanceBill.payableTotal: Double
    get() = chargesSubtotal + lateFeeAmount

/** Still outstanding on this bill, never negative on an overpayment. */
val MaintenanceBill.pendingAmount: Double
    get() = (payableTotal - amountPaid).coerceAtLeast(0.0)

/** The status the numbers actually imply, whatever is stored on the document. */
val MaintenanceBill.derivedStatus: PaymentStatus
    get() = when {
        amountPaid <= 0.0 -> PaymentStatus.UNPAID
        pendingAmount <= 0.009 -> PaymentStatus.PAID
        else -> PaymentStatus.PARTIAL
    }

// 4. Operating Expense Record -> Firestore collection "expense_records"
data class ExpenseRecord(
    val docId: String = "",
    val id: Int = 0,
    val title: String,
    val category: ExpenseCategory,
    val amount: Double,
    val date: String,
    val vendorName: String,
    val vendorContact: String = "",
    val paymentMode: PaymentMode = PaymentMode.BANK_TRANSFER,
    val approvedBy: String = "Secretary",
    val notes: String = "",
    val receiptNumber: String = "",
    val status: PaymentStatus = PaymentStatus.PAID,
    /** Scanned vendor bill as a Base64 blob. */
    val billImageBase64: String = ""
)

// 5. Non-Fee Revenue -> Firestore collection "non_fee_revenues"
data class NonFeeRevenue(
    val docId: String = "",
    val id: Int = 0,
    val title: String,
    val sourceCategory: RevenueSource,
    val amount: Double,
    val date: String,
    val payerName: String,
    val flatNumber: String = "",
    val paymentMode: PaymentMode = PaymentMode.UPI,
    val receiptNumber: String = "",
    val notes: String = ""
)

// 6. Gate Security Visitor Log -> Firestore collection "visitor_logs"
data class VisitorLog(
    val docId: String = "",
    val id: Int = 0,
    val visitorName: String,
    val phone: String,
    val flatNumber: String,
    val purpose: VisitorPurpose,
    val vehicleNumber: String = "",
    val entryTime: String,
    val exitTime: String = "",
    val status: VisitorStatus = VisitorStatus.INSIDE,
    val passcode: String = "",
    val guardName: String = "Gate Guard",
    /** Visitor snapshot taken at the gate as a Base64 blob. */
    val photoBase64: String = ""
)

// 7. Notice Board -> Firestore collection "notices"
data class Notice(
    val docId: String = "",
    val id: Int = 0,
    val title: String,
    val content: String,
    val publishedDate: String,
    val isPinned: Boolean = false,
    val category: NoticeCategory = NoticeCategory.GENERAL,
    val authorRole: String = "Managing Committee",
    /** Circular / poster image as a Base64 blob. */
    val imageBase64: String = ""
)

// 8. Complaints & Helpdesk Tickets -> Firestore collection "complaints"
data class Complaint(
    val docId: String = "",
    val id: Int = 0,
    val flatNumber: String,
    val residentName: String,
    val category: ComplaintCategory,
    val description: String,
    val filedDate: String,
    val status: TicketStatus = TicketStatus.OPEN,
    val resolutionNotes: String = "",
    /** Photo evidence attached to the ticket as a Base64 blob. */
    val attachmentBase64: String = ""
)

// 9. Emergency Contacts -> Firestore collection "emergency_contacts"
data class EmergencyContact(
    val docId: String = "",
    val id: Int = 0,
    val titleEn: String,
    val titleMr: String,
    val name: String,
    val phoneNumber: String,
    val category: String,
    /** Only active contacts are shown to members; inactive ones stay on file. */
    val isActive: Boolean = true
)

/**
 * How long an emergency number may be.
 *
 * Wide on purpose: this list carries police (100) and fire (101) alongside a
 * plumber's mobile, so a flat ten-digit rule would reject the very numbers that
 * matter most.
 */
val EMERGENCY_PHONE_DIGITS: IntRange = 3..15

/**
 * A society's financial year runs April to March, so a bill month belongs to
 * the year that started in the April on or before it.
 */
fun financialYearOf(billMonth: String): String = runCatching {
    val parsed = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.ENGLISH).parse(billMonth)
        ?: return@runCatching ""
    val calendar = java.util.Calendar.getInstance().apply { time = parsed }
    val year = calendar.get(java.util.Calendar.YEAR)
    // Calendar months are zero-based, so MARCH is 2.
    val startYear = if (calendar.get(java.util.Calendar.MONTH) <= java.util.Calendar.MARCH) {
        year - 1
    } else {
        year
    }
    "FY $startYear-${startYear + 1}"
}.getOrDefault("")

/** Every financial year the society has billed in, newest first. */
fun SocietyInfo?.financialYears(): List<String> =
    billingMonths().map { financialYearOf(it) }.filter { it.isNotBlank() }.distinct()
