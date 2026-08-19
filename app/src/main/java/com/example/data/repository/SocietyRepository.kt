package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.entity.Complaint
import com.example.data.entity.EmergencyContact
import com.example.data.entity.ExpenseRecord
import com.example.data.entity.MaintenanceBill
import com.example.data.entity.NonFeeRevenue
import com.example.data.entity.Notice
import com.example.data.entity.PaymentMode
import com.example.data.entity.Resident
import com.example.data.entity.SocietyInfo
import com.example.data.entity.TicketStatus
import com.example.data.entity.VisitorLog
import com.example.data.entity.VisitorStatus
import com.example.data.firebase.Collections
import com.example.data.firebase.FirebaseModule
import com.example.data.firebase.toComplaint
import com.example.data.firebase.toEmergencyContact
import com.example.data.firebase.toExpenseRecord
import com.example.data.firebase.toMaintenanceBill
import com.example.data.firebase.toMap
import com.example.data.firebase.toNonFeeRevenue
import com.example.data.firebase.toNotice
import com.example.data.firebase.toResident
import com.example.data.firebase.toSocietyInfo
import com.example.data.firebase.toVisitorLog
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Every read and write in SocietySarv goes through Cloud Firestore.
 *
 * Lists are exposed as [Flow]s backed by realtime snapshot listeners, so any
 * change made from another device (or straight from the Firebase console) shows
 * up in the app without a refresh.
 */
class SocietyRepository(context: Context) {

    private val db: FirebaseFirestore = FirebaseModule.firestore(context)

    private fun collection(name: String) = db.collection(name)

    /** Turns a Firestore query into a realtime [Flow] of mapped entities. */
    private fun <T> Query.asFlow(mapper: (DocumentSnapshot) -> T): Flow<List<T>> = callbackFlow {
        val registration = addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "listen failed", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            trySend(snapshot?.documents.orEmpty().mapNotNull { doc ->
                runCatching { mapper(doc) }.getOrNull()
            })
        }
        awaitClose { registration.remove() }
    }

    // ------------------------------------------------------------- streams

    val societyInfo: Flow<SocietyInfo?> = callbackFlow {
        val registration = collection(Collections.SOCIETY_INFO)
            .document(Collections.SOCIETY_DOC)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "society listen failed", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(if (snapshot != null && snapshot.exists()) snapshot.toSocietyInfo() else null)
            }
        awaitClose { registration.remove() }
    }

    val allResidents: Flow<List<Resident>> = collection(Collections.RESIDENTS)
        .orderBy("wing", Query.Direction.ASCENDING)
        .orderBy("flatNumber", Query.Direction.ASCENDING)
        .asFlow { it.toResident() }

    val allBills: Flow<List<MaintenanceBill>> = collection(Collections.MAINTENANCE_BILLS)
        .orderBy("id", Query.Direction.DESCENDING)
        .asFlow { it.toMaintenanceBill() }

    val allExpenses: Flow<List<ExpenseRecord>> = collection(Collections.EXPENSE_RECORDS)
        .orderBy("id", Query.Direction.DESCENDING)
        .asFlow { it.toExpenseRecord() }

    val allRevenues: Flow<List<NonFeeRevenue>> = collection(Collections.NON_FEE_REVENUES)
        .orderBy("id", Query.Direction.DESCENDING)
        .asFlow { it.toNonFeeRevenue() }

    val allVisitors: Flow<List<VisitorLog>> = collection(Collections.VISITOR_LOGS)
        .orderBy("id", Query.Direction.DESCENDING)
        .asFlow { it.toVisitorLog() }

    val activeVisitors: Flow<List<VisitorLog>> = collection(Collections.VISITOR_LOGS)
        .whereEqualTo("status", VisitorStatus.INSIDE.name)
        .orderBy("id", Query.Direction.DESCENDING)
        .asFlow { it.toVisitorLog() }

    val allNotices: Flow<List<Notice>> = collection(Collections.NOTICES)
        .orderBy("isPinned", Query.Direction.DESCENDING)
        .orderBy("id", Query.Direction.DESCENDING)
        .asFlow { it.toNotice() }

    val allComplaints: Flow<List<Complaint>> = collection(Collections.COMPLAINTS)
        .orderBy("id", Query.Direction.DESCENDING)
        .asFlow { it.toComplaint() }

    val emergencyContacts: Flow<List<EmergencyContact>> = collection(Collections.EMERGENCY_CONTACTS)
        .orderBy("id", Query.Direction.ASCENDING)
        .asFlow { it.toEmergencyContact() }

    fun getBillsForFlat(flatNumber: String): Flow<List<MaintenanceBill>> =
        collection(Collections.MAINTENANCE_BILLS)
            .whereEqualTo("flatNumber", flatNumber)
            .orderBy("id", Query.Direction.DESCENDING)
            .asFlow { it.toMaintenanceBill() }

    fun getVisitorsForFlat(flatNumber: String): Flow<List<VisitorLog>> =
        collection(Collections.VISITOR_LOGS)
            .whereEqualTo("flatNumber", flatNumber)
            .orderBy("id", Query.Direction.DESCENDING)
            .asFlow { it.toVisitorLog() }

    fun getComplaintsForFlat(flatNumber: String): Flow<List<Complaint>> =
        collection(Collections.COMPLAINTS)
            .whereEqualTo("flatNumber", flatNumber)
            .orderBy("id", Query.Direction.DESCENDING)
            .asFlow { it.toComplaint() }

    // ----------------------------------------------------- one-shot lookups

    suspend fun getSocietyInfoSync(): SocietyInfo? = runCatching {
        collection(Collections.SOCIETY_INFO)
            .document(Collections.SOCIETY_DOC)
            .get()
            .await()
            .takeIf { it.exists() }
            ?.toSocietyInfo()
    }.onFailure { Log.e(TAG, "getSocietyInfoSync failed", it) }.getOrNull()

    suspend fun getAllResidentsSync(): List<Resident> = runCatching {
        collection(Collections.RESIDENTS)
            .orderBy("wing", Query.Direction.ASCENDING)
            .orderBy("flatNumber", Query.Direction.ASCENDING)
            .get()
            .await()
            .documents
            .map { it.toResident() }
    }.onFailure { Log.e(TAG, "getAllResidentsSync failed", it) }.getOrDefault(emptyList())

    /** Login lookup by flat: the flat number *is* the document id. */
    suspend fun getResidentByFlat(flatNumber: String): Resident? = runCatching {
        val direct = collection(Collections.RESIDENTS).document(flatNumber).get().await()
        if (direct.exists()) return@runCatching direct.toResident()

        // Fall back to a field query so that a case difference or a hand-created
        // document with an auto-id still resolves.
        collection(Collections.RESIDENTS)
            .whereEqualTo("flatNumber", flatNumber)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.toResident()
            ?: collection(Collections.RESIDENTS).get().await().documents
                .firstOrNull { it.getString("flatNumber").equals(flatNumber, ignoreCase = true) }
                ?.toResident()
    }.onFailure { Log.e(TAG, "getResidentByFlat failed", it) }.getOrNull()

    /** Login lookup by registered mobile number. */
    suspend fun getResidentByMobile(mobile: String): Resident? = runCatching {
        collection(Collections.RESIDENTS)
            .whereEqualTo("contactNumber", mobile)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.toResident()
    }.onFailure { Log.e(TAG, "getResidentByMobile failed", it) }.getOrNull()

    // ------------------------------------------------------------- society

    suspend fun insertOrUpdateSociety(society: SocietyInfo) {
        collection(Collections.SOCIETY_INFO)
            .document(Collections.SOCIETY_DOC)
            .set(society.toMap())
            .await()
    }

    suspend fun updateSociety(society: SocietyInfo) = insertOrUpdateSociety(society)

    // ------------------------------------------------------------ residents

    suspend fun insertResident(resident: Resident) {
        val docId = resident.flatNumber.ifBlank { collection(Collections.RESIDENTS).document().id }
        val withId = if (resident.id == 0) {
            resident.copy(id = nextId(Collections.RESIDENTS), docId = docId)
        } else {
            resident.copy(docId = docId)
        }
        collection(Collections.RESIDENTS).document(docId).set(withId.toMap()).await()
    }

    suspend fun updateResident(resident: Resident) {
        val docId = resident.docId.ifBlank { resident.flatNumber }
        collection(Collections.RESIDENTS).document(docId).set(resident.toMap()).await()
    }

    suspend fun deleteResident(resident: Resident) {
        val docId = resident.docId.ifBlank { resident.flatNumber }
        collection(Collections.RESIDENTS).document(docId).delete().await()
    }

    suspend fun updateResidentPin(flatNumber: String, newPin: String) {
        val resident = getResidentByFlat(flatNumber) ?: return
        collection(Collections.RESIDENTS)
            .document(resident.docId.ifBlank { resident.flatNumber })
            .update("pin", newPin)
            .await()
    }

    // ------------------------------------------------------ maintenance bills

    suspend fun insertBill(bill: MaintenanceBill) {
        val withId = bill.copy(id = if (bill.id == 0) nextId(Collections.MAINTENANCE_BILLS) else bill.id)
        collection(Collections.MAINTENANCE_BILLS).add(withId.toMap()).await()
    }

    suspend fun updateBill(bill: MaintenanceBill) {
        if (bill.docId.isBlank()) return
        collection(Collections.MAINTENANCE_BILLS).document(bill.docId).set(bill.toMap()).await()
    }

    // ------------------------------------------------------------- expenses

    suspend fun insertExpense(expense: ExpenseRecord) {
        val withId = expense.copy(id = if (expense.id == 0) nextId(Collections.EXPENSE_RECORDS) else expense.id)
        collection(Collections.EXPENSE_RECORDS).add(withId.toMap()).await()
        adjustTreasury(-expense.amount, expense.paymentMode)
    }

    suspend fun deleteExpense(expense: ExpenseRecord) {
        if (expense.docId.isBlank()) return
        collection(Collections.EXPENSE_RECORDS).document(expense.docId).delete().await()
        adjustTreasury(expense.amount, expense.paymentMode)
    }

    // ------------------------------------------------------- non-fee revenue

    suspend fun insertRevenue(revenue: NonFeeRevenue) {
        val withId = revenue.copy(id = if (revenue.id == 0) nextId(Collections.NON_FEE_REVENUES) else revenue.id)
        collection(Collections.NON_FEE_REVENUES).add(withId.toMap()).await()
        adjustTreasury(revenue.amount, revenue.paymentMode)
    }

    suspend fun deleteRevenue(revenue: NonFeeRevenue) {
        if (revenue.docId.isBlank()) return
        collection(Collections.NON_FEE_REVENUES).document(revenue.docId).delete().await()
    }

    // ------------------------------------------------------------- visitors

    suspend fun insertVisitor(visitor: VisitorLog) {
        val withId = visitor.copy(id = if (visitor.id == 0) nextId(Collections.VISITOR_LOGS) else visitor.id)
        collection(Collections.VISITOR_LOGS).add(withId.toMap()).await()
    }

    suspend fun updateVisitor(visitor: VisitorLog) {
        if (visitor.docId.isBlank()) return
        collection(Collections.VISITOR_LOGS).document(visitor.docId).set(visitor.toMap()).await()
    }

    suspend fun verifyVisitorPasscode(passcode: String): VisitorLog? = runCatching {
        collection(Collections.VISITOR_LOGS)
            .whereEqualTo("passcode", passcode)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.toVisitorLog()
    }.onFailure { Log.e(TAG, "verifyVisitorPasscode failed", it) }.getOrNull()

    suspend fun markVisitorExit(visitorId: Int, exitTime: String, visitor: VisitorLog) {
        updateVisitor(visitor.copy(status = VisitorStatus.EXITED, exitTime = exitTime))
    }

    // -------------------------------------------------------------- notices

    suspend fun insertNotice(notice: Notice) {
        val withId = notice.copy(id = if (notice.id == 0) nextId(Collections.NOTICES) else notice.id)
        collection(Collections.NOTICES).add(withId.toMap()).await()
    }

    suspend fun deleteNotice(notice: Notice) {
        if (notice.docId.isBlank()) return
        collection(Collections.NOTICES).document(notice.docId).delete().await()
    }

    // ----------------------------------------------------------- complaints

    suspend fun insertComplaint(complaint: Complaint) {
        val withId = complaint.copy(id = if (complaint.id == 0) nextId(Collections.COMPLAINTS) else complaint.id)
        collection(Collections.COMPLAINTS).add(withId.toMap()).await()
    }

    suspend fun updateComplaintStatus(complaint: Complaint, newStatus: TicketStatus, notes: String) {
        if (complaint.docId.isBlank()) return
        collection(Collections.COMPLAINTS)
            .document(complaint.docId)
            .update(
                mapOf(
                    "status" to newStatus.name,
                    "resolutionNotes" to notes.ifBlank { complaint.resolutionNotes }
                )
            )
            .await()
    }

    // ---------------------------------------------------------- maintenance

    suspend fun recordMaintenancePayment(
        billId: Int,
        amount: Double,
        paymentMode: PaymentMode,
        transactionRef: String,
        paidDate: String
    ) {
        adjustTreasury(amount, paymentMode)
    }

    // --------------------------------------------------------------- shared

    /**
     * Moves money in or out of the society treasury. Cash payments hit the cash
     * balance, everything else hits the bank balance.
     */
    private suspend fun adjustTreasury(delta: Double, mode: PaymentMode) {
        val field = if (mode == PaymentMode.CASH) "currentBalanceCash" else "currentBalanceBank"
        runCatching {
            collection(Collections.SOCIETY_INFO)
                .document(Collections.SOCIETY_DOC)
                .update(field, FieldValue.increment(delta))
                .await()
        }.onFailure { Log.e(TAG, "treasury update failed", it) }
    }

    /**
     * Hands out the next numeric id for a collection. The counters collection
     * keeps the legacy auto-increment ids the UI sorts and keys on working the
     * same way they did under Room.
     */
    private suspend fun nextId(collectionName: String): Int = runCatching {
        val counter = collection(Collections.COUNTERS).document(collectionName)
        db.runTransaction { txn ->
            val current = txn.get(counter).getLong("value") ?: 0L
            val next = current + 1
            txn.set(counter, mapOf("value" to next, "collection" to collectionName, "docId" to collectionName))
            next.toInt()
        }.await()
    }.getOrElse {
        Log.e(TAG, "nextId failed for $collectionName", it)
        System.currentTimeMillis().toInt() and 0x7FFFFFFF
    }

    private companion object {
        const val TAG = "SocietyRepository"
    }
}
