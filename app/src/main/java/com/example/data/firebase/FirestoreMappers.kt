package com.example.data.firebase

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
import com.google.firebase.firestore.DocumentSnapshot

/*
 * Document <-> entity conversion.
 *
 * Enums are persisted as their `name` string so that the documents stay
 * human-readable in the Firebase console, and every read falls back to a safe
 * default when a field is missing or was hand-edited to something unknown.
 */

private fun DocumentSnapshot.str(field: String, fallback: String = ""): String =
    getString(field) ?: fallback

private fun DocumentSnapshot.num(field: String, fallback: Double = 0.0): Double =
    (get(field) as? Number)?.toDouble() ?: fallback

private fun DocumentSnapshot.int(field: String, fallback: Int = 0): Int =
    (get(field) as? Number)?.toInt() ?: fallback

private fun DocumentSnapshot.bool(field: String, fallback: Boolean = false): Boolean =
    getBoolean(field) ?: fallback

private inline fun <reified T : Enum<T>> DocumentSnapshot.enum(field: String, fallback: T): T =
    getString(field)?.let { raw -> runCatching { enumValueOf<T>(raw) }.getOrNull() } ?: fallback

// ------------------------------------------------------------------ society

fun DocumentSnapshot.toSocietyInfo(): SocietyInfo = SocietyInfo(
    docId = id,
    id = int("id", 1),
    name = str("name"),
    registrationNumber = str("registrationNumber"),
    societyCode = str("societyCode"),
    address = str("address"),
    totalWings = int("totalWings"),
    totalFlats = int("totalFlats"),
    bankAccountName = str("bankAccountName"),
    bankAccountNumber = str("bankAccountNumber"),
    ifscCode = str("ifscCode"),
    currentBalanceCash = num("currentBalanceCash"),
    currentBalanceBank = num("currentBalanceBank"),
    lateFeePercentPerMonth = num("lateFeePercentPerMonth", 2.0),
    lateFeeGraceDays = int("lateFeeGraceDays", 10),
    tariff1BHK = num("tariff1BHK", 1500.0),
    tariff2BHK = num("tariff2BHK", 2200.0),
    tariff3BHK = num("tariff3BHK", 3000.0),
    tariffPenthouse = num("tariffPenthouse", 4500.0),
    tariffShop = num("tariffShop", 3500.0),
    sinkingFundPercent = num("sinkingFundPercent", 5.0),
    repairFundPercent = num("repairFundPercent", 5.0),
    logoBase64 = str("logoBase64")
)

fun SocietyInfo.toMap(): Map<String, Any?> = mapOf(
    "docId" to societyCode,
    "id" to id,
    "name" to name,
    "registrationNumber" to registrationNumber,
    "societyCode" to societyCode,
    "address" to address,
    "totalWings" to totalWings,
    "totalFlats" to totalFlats,
    "bankAccountName" to bankAccountName,
    "bankAccountNumber" to bankAccountNumber,
    "ifscCode" to ifscCode,
    "currentBalanceCash" to currentBalanceCash,
    "currentBalanceBank" to currentBalanceBank,
    "lateFeePercentPerMonth" to lateFeePercentPerMonth,
    "lateFeeGraceDays" to lateFeeGraceDays,
    "tariff1BHK" to tariff1BHK,
    "tariff2BHK" to tariff2BHK,
    "tariff3BHK" to tariff3BHK,
    "tariffPenthouse" to tariffPenthouse,
    "tariffShop" to tariffShop,
    "sinkingFundPercent" to sinkingFundPercent,
    "repairFundPercent" to repairFundPercent,
    "logoBase64" to logoBase64
)

// ----------------------------------------------------------------- resident

fun DocumentSnapshot.toResident(): Resident = Resident(
    docId = id,
    id = int("id"),
    flatNumber = str("flatNumber", id),
    wing = str("wing"),
    ownerName = str("ownerName"),
    tenantName = str("tenantName"),
    contactNumber = str("contactNumber"),
    email = str("email"),
    role = enum("role", UserRole.RESIDENT),
    pin = str("pin"),
    familyMemberCount = int("familyMemberCount"),
    vehicleTwoWheelerCount = int("vehicleTwoWheelerCount"),
    vehicleFourWheelerCount = int("vehicleFourWheelerCount"),
    twoWheelerNumber = str("twoWheelerNumber"),
    fourWheelerNumber = str("fourWheelerNumber"),
    parkingSlot = str("parkingSlot"),
    flatType = enum("flatType", FlatType.TWO_BHK),
    squareFeet = int("squareFeet"),
    balanceDue = num("balanceDue"),
    isOwnerLiving = bool("isOwnerLiving", true),
    photoBase64 = str("photoBase64")
)

fun Resident.toMap(): Map<String, Any?> = mapOf(
    "docId" to flatNumber,
    "id" to id,
    "flatNumber" to flatNumber,
    "wing" to wing,
    "ownerName" to ownerName,
    "tenantName" to tenantName,
    "contactNumber" to contactNumber,
    "email" to email,
    "role" to role.name,
    "pin" to pin,
    "familyMemberCount" to familyMemberCount,
    "vehicleTwoWheelerCount" to vehicleTwoWheelerCount,
    "vehicleFourWheelerCount" to vehicleFourWheelerCount,
    "twoWheelerNumber" to twoWheelerNumber,
    "fourWheelerNumber" to fourWheelerNumber,
    "parkingSlot" to parkingSlot,
    "flatType" to flatType.name,
    "squareFeet" to squareFeet,
    "balanceDue" to balanceDue,
    "isOwnerLiving" to isOwnerLiving,
    "photoBase64" to photoBase64
)

// --------------------------------------------------------- maintenance bill

fun DocumentSnapshot.toMaintenanceBill(): MaintenanceBill = MaintenanceBill(
    docId = id,
    id = int("id"),
    flatNumber = str("flatNumber"),
    residentName = str("residentName"),
    billMonth = str("billMonth"),
    baseAmount = num("baseAmount"),
    waterCharges = num("waterCharges"),
    sinkingFund = num("sinkingFund"),
    repairFund = num("repairFund"),
    parkingCharges = num("parkingCharges"),
    nonOccupancyCharges = num("nonOccupancyCharges"),
    lateFeeAmount = num("lateFeeAmount"),
    totalAmount = num("totalAmount"),
    amountPaid = num("amountPaid"),
    status = enum("status", PaymentStatus.UNPAID),
    dueDate = str("dueDate"),
    paidDate = str("paidDate"),
    paymentMode = getString("paymentMode")
        ?.let { raw -> runCatching { PaymentMode.valueOf(raw) }.getOrNull() },
    transactionRef = str("transactionRef"),
    receiptNumber = str("receiptNumber"),
    receiptImageBase64 = str("receiptImageBase64")
)

fun MaintenanceBill.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "flatNumber" to flatNumber,
    "residentName" to residentName,
    "billMonth" to billMonth,
    "baseAmount" to baseAmount,
    "waterCharges" to waterCharges,
    "sinkingFund" to sinkingFund,
    "repairFund" to repairFund,
    "parkingCharges" to parkingCharges,
    "nonOccupancyCharges" to nonOccupancyCharges,
    "lateFeeAmount" to lateFeeAmount,
    "totalAmount" to totalAmount,
    "amountPaid" to amountPaid,
    "status" to status.name,
    "dueDate" to dueDate,
    "paidDate" to paidDate,
    "paymentMode" to paymentMode?.name,
    "transactionRef" to transactionRef,
    "receiptNumber" to receiptNumber,
    "receiptImageBase64" to receiptImageBase64
)

// ------------------------------------------------------------------ expense

fun DocumentSnapshot.toExpenseRecord(): ExpenseRecord = ExpenseRecord(
    docId = id,
    id = int("id"),
    title = str("title"),
    category = enum("category", ExpenseCategory.MISCELLANEOUS),
    amount = num("amount"),
    date = str("date"),
    vendorName = str("vendorName"),
    vendorContact = str("vendorContact"),
    paymentMode = enum("paymentMode", PaymentMode.BANK_TRANSFER),
    approvedBy = str("approvedBy", "Secretary"),
    notes = str("notes"),
    receiptNumber = str("receiptNumber"),
    status = enum("status", PaymentStatus.PAID),
    billImageBase64 = str("billImageBase64")
)

fun ExpenseRecord.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "title" to title,
    "category" to category.name,
    "amount" to amount,
    "date" to date,
    "vendorName" to vendorName,
    "vendorContact" to vendorContact,
    "paymentMode" to paymentMode.name,
    "approvedBy" to approvedBy,
    "notes" to notes,
    "receiptNumber" to receiptNumber,
    "status" to status.name,
    "billImageBase64" to billImageBase64
)

// ------------------------------------------------------------ non-fee revenue

fun DocumentSnapshot.toNonFeeRevenue(): NonFeeRevenue = NonFeeRevenue(
    docId = id,
    id = int("id"),
    title = str("title"),
    sourceCategory = enum("sourceCategory", RevenueSource.PENALTY_FINE),
    amount = num("amount"),
    date = str("date"),
    payerName = str("payerName"),
    flatNumber = str("flatNumber"),
    paymentMode = enum("paymentMode", PaymentMode.UPI),
    receiptNumber = str("receiptNumber"),
    notes = str("notes")
)

fun NonFeeRevenue.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "title" to title,
    "sourceCategory" to sourceCategory.name,
    "amount" to amount,
    "date" to date,
    "payerName" to payerName,
    "flatNumber" to flatNumber,
    "paymentMode" to paymentMode.name,
    "receiptNumber" to receiptNumber,
    "notes" to notes
)

// ------------------------------------------------------------------- visitor

fun DocumentSnapshot.toVisitorLog(): VisitorLog = VisitorLog(
    docId = id,
    id = int("id"),
    visitorName = str("visitorName"),
    phone = str("phone"),
    flatNumber = str("flatNumber"),
    purpose = enum("purpose", VisitorPurpose.OTHER),
    vehicleNumber = str("vehicleNumber"),
    entryTime = str("entryTime"),
    exitTime = str("exitTime"),
    status = enum("status", VisitorStatus.INSIDE),
    passcode = str("passcode"),
    guardName = str("guardName", "Gate Guard"),
    photoBase64 = str("photoBase64")
)

fun VisitorLog.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "visitorName" to visitorName,
    "phone" to phone,
    "flatNumber" to flatNumber,
    "purpose" to purpose.name,
    "vehicleNumber" to vehicleNumber,
    "entryTime" to entryTime,
    "exitTime" to exitTime,
    "status" to status.name,
    "passcode" to passcode,
    "guardName" to guardName,
    "photoBase64" to photoBase64
)

// -------------------------------------------------------------------- notice

fun DocumentSnapshot.toNotice(): Notice = Notice(
    docId = id,
    id = int("id"),
    title = str("title"),
    content = str("content"),
    publishedDate = str("publishedDate"),
    isPinned = bool("isPinned"),
    category = enum("category", NoticeCategory.GENERAL),
    authorRole = str("authorRole", "Managing Committee"),
    imageBase64 = str("imageBase64")
)

fun Notice.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "title" to title,
    "content" to content,
    "publishedDate" to publishedDate,
    "isPinned" to isPinned,
    "category" to category.name,
    "authorRole" to authorRole,
    "imageBase64" to imageBase64
)

// ----------------------------------------------------------------- complaint

fun DocumentSnapshot.toComplaint(): Complaint = Complaint(
    docId = id,
    id = int("id"),
    flatNumber = str("flatNumber"),
    residentName = str("residentName"),
    category = enum("category", ComplaintCategory.OTHER),
    description = str("description"),
    filedDate = str("filedDate"),
    status = enum("status", TicketStatus.OPEN),
    resolutionNotes = str("resolutionNotes"),
    attachmentBase64 = str("attachmentBase64")
)

fun Complaint.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "flatNumber" to flatNumber,
    "residentName" to residentName,
    "category" to category.name,
    "description" to description,
    "filedDate" to filedDate,
    "status" to status.name,
    "resolutionNotes" to resolutionNotes,
    "attachmentBase64" to attachmentBase64
)

// --------------------------------------------------------- emergency contact

fun DocumentSnapshot.toEmergencyContact(): EmergencyContact = EmergencyContact(
    docId = id,
    id = int("id"),
    titleEn = str("titleEn"),
    titleMr = str("titleMr"),
    name = str("name"),
    phoneNumber = str("phoneNumber"),
    category = str("category")
)

fun EmergencyContact.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "titleEn" to titleEn,
    "titleMr" to titleMr,
    "name" to name,
    "phoneNumber" to phoneNumber,
    "category" to category
)
