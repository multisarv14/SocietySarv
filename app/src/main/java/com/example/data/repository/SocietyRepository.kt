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
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

/**
 * All reads and writes for **one society at a time**.
 *
 * The society is chosen at login or registration via [setActiveSociety], and
 * every query below is rooted at `societies/{code}`. Because each society owns
 * its own subcollections, no query can reach another society's data even if a
 * filter were forgotten.
 *
 * Lists are realtime snapshot listeners, so a change made from another device
 * or from the Firebase console shows up without a refresh. Switching society
 * tears the old listeners down and starts fresh ones.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SocietyRepository(context: Context) {

    private val db: FirebaseFirestore = FirebaseModule.firestore(context)

    private val _activeSocietyCode = MutableStateFlow<String?>(null)

    /** Login code of the society currently signed in, or null when signed out. */
    val activeSocietyCode: StateFlow<String?> = _activeSocietyCode.asStateFlow()

    fun setActiveSociety(code: String) {
        _activeSocietyCode.value = normalise(code)
    }

    fun clearActiveSociety() {
        _activeSocietyCode.value = null
    }

    private fun normalise(code: String) = code.trim().uppercase()

    private fun societyDoc(code: String): DocumentReference =
        db.collection(Collections.SOCIETIES).document(normalise(code))

    private fun sub(code: String, name: String): CollectionReference =
        societyDoc(code).collection(name)

    /** Subcollection of the society that is currently signed in. */
    private fun active(name: String): CollectionReference? =
        _activeSocietyCode.value?.let { sub(it, name) }

    // ------------------------------------------------------------- plumbing

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

    /**
     * Rebuilds a stream whenever the active society changes, and emits an empty
     * list while signed out so a previous society's rows can never linger.
     */
    private fun <T> scoped(
        name: String,
        mapper: (DocumentSnapshot) -> T,
        query: (CollectionReference) -> Query,
    ): Flow<List<T>> = _activeSocietyCode.flatMapLatest { code ->
        if (code == null) flowOf(emptyList()) else query(sub(code, name)).asFlow(mapper)
    }

    // ------------------------------------------------------------- streams

    val societyInfo: Flow<SocietyInfo?> = _activeSocietyCode.flatMapLatest { code ->
        if (code == null) {
            flowOf(null)
        } else {
            callbackFlow {
                val registration = societyDoc(code).addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "society listen failed", error)
                        trySend(null)
                        return@addSnapshotListener
                    }
                    trySend(if (snapshot != null && snapshot.exists()) snapshot.toSocietyInfo() else null)
                }
                awaitClose { registration.remove() }
            }
        }
    }

    val allResidents: Flow<List<Resident>> = scoped(Collections.RESIDENTS, { it.toResident() }) {
        it.orderBy("wing", Query.Direction.ASCENDING)
            .orderBy("flatNumber", Query.Direction.ASCENDING)
    }

    val allBills: Flow<List<MaintenanceBill>> =
        scoped(Collections.MAINTENANCE_BILLS, { it.toMaintenanceBill() }) {
            it.orderBy("id", Query.Direction.DESCENDING)
        }

    val allExpenses: Flow<List<ExpenseRecord>> =
        scoped(Collections.EXPENSE_RECORDS, { it.toExpenseRecord() }) {
            it.orderBy("id", Query.Direction.DESCENDING)
        }

    val allRevenues: Flow<List<NonFeeRevenue>> =
        scoped(Collections.NON_FEE_REVENUES, { it.toNonFeeRevenue() }) {
            it.orderBy("id", Query.Direction.DESCENDING)
        }

    val allVisitors: Flow<List<VisitorLog>> =
        scoped(Collections.VISITOR_LOGS, { it.toVisitorLog() }) {
            it.orderBy("id", Query.Direction.DESCENDING)
        }

    val activeVisitors: Flow<List<VisitorLog>> =
        scoped(Collections.VISITOR_LOGS, { it.toVisitorLog() }) {
            it.whereEqualTo("status", VisitorStatus.INSIDE.name)
                .orderBy("id", Query.Direction.DESCENDING)
        }

    val allNotices: Flow<List<Notice>> = scoped(Collections.NOTICES, { it.toNotice() }) {
        it.orderBy("isPinned", Query.Direction.DESCENDING)
            .orderBy("id", Query.Direction.DESCENDING)
    }

    val allComplaints: Flow<List<Complaint>> = scoped(Collections.COMPLAINTS, { it.toComplaint() }) {
        it.orderBy("id", Query.Direction.DESCENDING)
    }

    val emergencyContacts: Flow<List<EmergencyContact>> =
        scoped(Collections.EMERGENCY_CONTACTS, { it.toEmergencyContact() }) {
            it.orderBy("id", Query.Direction.ASCENDING)
        }

    fun getBillsForFlat(flatNumber: String): Flow<List<MaintenanceBill>> =
        scoped(Collections.MAINTENANCE_BILLS, { it.toMaintenanceBill() }) {
            it.whereEqualTo("flatNumber", flatNumber)
                .orderBy("id", Query.Direction.DESCENDING)
        }

    fun getVisitorsForFlat(flatNumber: String): Flow<List<VisitorLog>> =
        scoped(Collections.VISITOR_LOGS, { it.toVisitorLog() }) {
            it.whereEqualTo("flatNumber", flatNumber)
                .orderBy("id", Query.Direction.DESCENDING)
        }

    fun getComplaintsForFlat(flatNumber: String): Flow<List<Complaint>> =
        scoped(Collections.COMPLAINTS, { it.toComplaint() }) {
            it.whereEqualTo("flatNumber", flatNumber)
                .orderBy("id", Query.Direction.DESCENDING)
        }

    // ------------------------------------------------------- society lookup

    /** Looks a society up by its login code. Used by login and registration. */
    suspend fun findSocietyByCode(code: String): SocietyInfo? = runCatching {
        societyDoc(code).get().await().takeIf { it.exists() }?.toSocietyInfo()
    }.onFailure { Log.e(TAG, "findSocietyByCode failed", it) }.getOrNull()

    suspend fun getSocietyInfoSync(): SocietyInfo? {
        val code = _activeSocietyCode.value ?: return null
        return findSocietyByCode(code)
    }

    // ----------------------------------------------------- one-shot lookups

    suspend fun getAllResidentsSync(): List<Resident> = runCatching {
        val residents = active(Collections.RESIDENTS) ?: return emptyList()
        residents.orderBy("wing", Query.Direction.ASCENDING)
            .orderBy("flatNumber", Query.Direction.ASCENDING)
            .get().await().documents.map { it.toResident() }
    }.onFailure { Log.e(TAG, "getAllResidentsSync failed", it) }.getOrDefault(emptyList())

    /** Flat lookup inside one society. The flat number is the document id. */
    suspend fun getResidentByFlat(flatNumber: String, societyCode: String? = null): Resident? =
        runCatching {
            val code = societyCode ?: _activeSocietyCode.value ?: return null
            val flats = sub(code, Collections.RESIDENTS)
            val wanted = flatNumber.trim()

            val direct = flats.document(wanted.uppercase()).get().await()
            if (direct.exists()) return@runCatching direct.toResident()

            flats.whereEqualTo("flatNumber", wanted).limit(1).get().await()
                .documents.firstOrNull()?.toResident()
                ?: flats.get().await().documents
                    .firstOrNull { it.getString("flatNumber").equals(wanted, ignoreCase = true) }
                    ?.toResident()
        }.onFailure { Log.e(TAG, "getResidentByFlat failed", it) }.getOrNull()

    /** Mobile-number lookup inside one society. */
    suspend fun getResidentByMobile(mobile: String, societyCode: String? = null): Resident? =
        runCatching {
            val code = societyCode ?: _activeSocietyCode.value ?: return null
            sub(code, Collections.RESIDENTS)
                .whereEqualTo("contactNumber", mobile.trim()).limit(1).get().await()
                .documents.firstOrNull()?.toResident()
        }.onFailure { Log.e(TAG, "getResidentByMobile failed", it) }.getOrNull()

    // ------------------------------------------------------------- society

    /** Creates or updates a society document, keyed by its login code. */
    suspend fun insertOrUpdateSociety(society: SocietyInfo) {
        val code = normalise(society.societyCode)
        societyDoc(code).set(society.copy(societyCode = code).toMap()).await()
    }

    suspend fun updateSociety(society: SocietyInfo) = insertOrUpdateSociety(society)

    // ------------------------------------------------------------ residents

    suspend fun insertResident(resident: Resident, societyCode: String? = null) {
        val code = societyCode ?: _activeSocietyCode.value ?: return
        val flats = sub(code, Collections.RESIDENTS)
        val docId = resident.flatNumber.trim().uppercase().ifBlank { flats.document().id }
        val withId = resident.copy(
            id = if (resident.id == 0) nextId(code, Collections.RESIDENTS) else resident.id,
            flatNumber = docId,
            docId = docId,
        )
        flats.document(docId).set(withId.toMap()).await()
    }

    suspend fun updateResident(resident: Resident) {
        val flats = active(Collections.RESIDENTS) ?: return
        val docId = resident.docId.ifBlank { resident.flatNumber }
        flats.document(docId).set(resident.toMap()).await()
    }

    suspend fun deleteResident(resident: Resident) {
        val flats = active(Collections.RESIDENTS) ?: return
        flats.document(resident.docId.ifBlank { resident.flatNumber }).delete().await()
    }

    suspend fun updateResidentPin(flatNumber: String, newPin: String) {
        val flats = active(Collections.RESIDENTS) ?: return
        val resident = getResidentByFlat(flatNumber) ?: return
        flats.document(resident.docId.ifBlank { resident.flatNumber })
            .update("pin", newPin).await()
    }

    // ------------------------------------------------------ maintenance bills

    suspend fun insertBill(bill: MaintenanceBill) {
        val code = _activeSocietyCode.value ?: return
        val withId = bill.copy(
            id = if (bill.id == 0) nextId(code, Collections.MAINTENANCE_BILLS) else bill.id
        )
        sub(code, Collections.MAINTENANCE_BILLS).add(withId.toMap()).await()
    }

    suspend fun updateBill(bill: MaintenanceBill) {
        val bills = active(Collections.MAINTENANCE_BILLS) ?: return
        if (bill.docId.isBlank()) return
        bills.document(bill.docId).set(bill.toMap()).await()
    }

    // ------------------------------------------------------------- expenses

    suspend fun insertExpense(expense: ExpenseRecord) {
        val code = _activeSocietyCode.value ?: return
        val withId = expense.copy(
            id = if (expense.id == 0) nextId(code, Collections.EXPENSE_RECORDS) else expense.id
        )
        sub(code, Collections.EXPENSE_RECORDS).add(withId.toMap()).await()
        adjustTreasury(-expense.amount, expense.paymentMode)
    }

    suspend fun deleteExpense(expense: ExpenseRecord) {
        val expenses = active(Collections.EXPENSE_RECORDS) ?: return
        if (expense.docId.isBlank()) return
        expenses.document(expense.docId).delete().await()
        adjustTreasury(expense.amount, expense.paymentMode)
    }

    // ------------------------------------------------------- non-fee revenue

    suspend fun insertRevenue(revenue: NonFeeRevenue) {
        val code = _activeSocietyCode.value ?: return
        val withId = revenue.copy(
            id = if (revenue.id == 0) nextId(code, Collections.NON_FEE_REVENUES) else revenue.id
        )
        sub(code, Collections.NON_FEE_REVENUES).add(withId.toMap()).await()
        adjustTreasury(revenue.amount, revenue.paymentMode)
    }

    suspend fun deleteRevenue(revenue: NonFeeRevenue) {
        val revenues = active(Collections.NON_FEE_REVENUES) ?: return
        if (revenue.docId.isBlank()) return
        revenues.document(revenue.docId).delete().await()
    }

    // ------------------------------------------------------------- visitors

    suspend fun insertVisitor(visitor: VisitorLog) {
        val code = _activeSocietyCode.value ?: return
        val withId = visitor.copy(
            id = if (visitor.id == 0) nextId(code, Collections.VISITOR_LOGS) else visitor.id
        )
        sub(code, Collections.VISITOR_LOGS).add(withId.toMap()).await()
    }

    suspend fun updateVisitor(visitor: VisitorLog) {
        val visitors = active(Collections.VISITOR_LOGS) ?: return
        if (visitor.docId.isBlank()) return
        visitors.document(visitor.docId).set(visitor.toMap()).await()
    }

    suspend fun verifyVisitorPasscode(passcode: String): VisitorLog? = runCatching {
        val visitors = active(Collections.VISITOR_LOGS) ?: return null
        visitors.whereEqualTo("passcode", passcode.trim()).limit(1).get().await()
            .documents.firstOrNull()?.toVisitorLog()
    }.onFailure { Log.e(TAG, "verifyVisitorPasscode failed", it) }.getOrNull()

    suspend fun markVisitorExit(visitorId: Int, exitTime: String, visitor: VisitorLog) {
        updateVisitor(visitor.copy(status = VisitorStatus.EXITED, exitTime = exitTime))
    }

    // -------------------------------------------------------------- notices

    suspend fun insertNotice(notice: Notice) {
        val code = _activeSocietyCode.value ?: return
        val withId = notice.copy(
            id = if (notice.id == 0) nextId(code, Collections.NOTICES) else notice.id
        )
        sub(code, Collections.NOTICES).add(withId.toMap()).await()
    }

    suspend fun deleteNotice(notice: Notice) {
        val notices = active(Collections.NOTICES) ?: return
        if (notice.docId.isBlank()) return
        notices.document(notice.docId).delete().await()
    }

    // ----------------------------------------------------------- complaints

    suspend fun insertComplaint(complaint: Complaint) {
        val code = _activeSocietyCode.value ?: return
        val withId = complaint.copy(
            id = if (complaint.id == 0) nextId(code, Collections.COMPLAINTS) else complaint.id
        )
        sub(code, Collections.COMPLAINTS).add(withId.toMap()).await()
    }

    suspend fun updateComplaintStatus(complaint: Complaint, newStatus: TicketStatus, notes: String) {
        val complaints = active(Collections.COMPLAINTS) ?: return
        if (complaint.docId.isBlank()) return
        complaints.document(complaint.docId).update(
            mapOf(
                "status" to newStatus.name,
                "resolutionNotes" to notes.ifBlank { complaint.resolutionNotes },
            )
        ).await()
    }

    // ---------------------------------------------------------- maintenance

    suspend fun recordMaintenancePayment(
        billId: Int,
        amount: Double,
        paymentMode: PaymentMode,
        transactionRef: String,
        paidDate: String,
    ) {
        adjustTreasury(amount, paymentMode)
    }

    // --------------------------------------------------------------- shared

    /** Cash payments move the cash balance, everything else the bank balance. */
    private suspend fun adjustTreasury(delta: Double, mode: PaymentMode) {
        val code = _activeSocietyCode.value ?: return
        val field = if (mode == PaymentMode.CASH) "currentBalanceCash" else "currentBalanceBank"
        runCatching {
            societyDoc(code).update(field, FieldValue.increment(delta)).await()
        }.onFailure { Log.e(TAG, "treasury update failed", it) }
    }

    /**
     * Next numeric id for a collection, from a per-society counter. Ids restart
     * at 1 for every society, so two societies both have a bill #1.
     */
    private suspend fun nextId(societyCode: String, collectionName: String): Int = runCatching {
        val counter = sub(societyCode, Collections.COUNTERS).document(collectionName)
        db.runTransaction { txn ->
            val next = (txn.get(counter).getLong("value") ?: 0L) + 1
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
