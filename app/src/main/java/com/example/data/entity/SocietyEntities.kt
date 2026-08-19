package com.example.data.entity

enum class UserRole {
    CHAIRMAN,
    SECRETARY,
    TREASURER,
    COMMITTEE,
    RESIDENT,
    TENANT,
    GUARD
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
    val currentBalanceCash: Double,
    val currentBalanceBank: Double,
    val lateFeePercentPerMonth: Double = 2.0,
    val lateFeeGraceDays: Int = 10,
    val tariff1BHK: Double = 1500.0,
    val tariff2BHK: Double = 2200.0,
    val tariff3BHK: Double = 3000.0,
    val tariffPenthouse: Double = 4500.0,
    val tariffShop: Double = 3500.0,
    val sinkingFundPercent: Double = 5.0,
    val repairFundPercent: Double = 5.0,
    /** Society logo as a Base64 blob (empty when none uploaded). */
    val logoBase64: String = ""
)

// 2. Resident / Flat Member -> Firestore collection "residents" (doc id = flatNumber)
data class Resident(
    val docId: String = "",
    val id: Int = 0,
    val flatNumber: String,
    val wing: String,
    val ownerName: String,
    val tenantName: String = "",
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
    val flatType: FlatType = FlatType.TWO_BHK,
    val squareFeet: Int = 0,
    val balanceDue: Double = 0.0,
    val isOwnerLiving: Boolean = true,
    /** Member photo as a Base64 blob (empty when none uploaded). */
    val photoBase64: String = ""
)

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
    val category: String
)
