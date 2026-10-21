import 'dart:async';

import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter/foundation.dart';

import 'firebase_module.dart';
import 'firestore_mappers.dart';
import 'models.dart';

/// All reads and writes for **one society at a time**.
///
/// The society is chosen at login or registration via [setActiveSociety], and
/// every query is rooted at `societies/{code}`. Because each society owns its
/// own subcollections, no query can reach another society's data.
///
/// Lists are realtime snapshot listeners: [activeSocietyCode] changes are
/// broadcast on [activeSocietyChanges], and the view model rebuilds its
/// subscriptions from the `watch*` streams for the new code.
class SocietyRepository {
  SocietyRepository() : _db = FirebaseModule.firestore;

  final FirebaseFirestore _db;

  /// Firestore caps a write batch at 500 operations.
  static const _batchLimit = 400;

  String? _activeSocietyCode;
  final _activeController = StreamController<String?>.broadcast();

  /// Login code of the society currently signed in, or null when signed out.
  String? get activeSocietyCode => _activeSocietyCode;
  Stream<String?> get activeSocietyChanges => _activeController.stream;

  void setActiveSociety(String code) {
    final next = _normalise(code);
    if (next == _activeSocietyCode) return;
    _activeSocietyCode = next;
    _activeController.add(next);
  }

  void clearActiveSociety() {
    if (_activeSocietyCode == null) return;
    _activeSocietyCode = null;
    _activeController.add(null);
  }

  String _normalise(String code) => code.trim().toUpperCase();

  DocumentReference<Map<String, dynamic>> _societyDoc(String code) =>
      _db.collection(Collections.societies).doc(_normalise(code));

  CollectionReference<Map<String, dynamic>> _sub(String code, String name) =>
      _societyDoc(code).collection(name);

  /// Subcollection of the society that is currently signed in.
  CollectionReference<Map<String, dynamic>>? _active(String name) {
    final code = _activeSocietyCode;
    return code == null ? null : _sub(code, name);
  }

  // ------------------------------------------------------------- plumbing

  Stream<List<T>> _listen<T>(Query<Map<String, dynamic>> query, T Function(DocumentSnapshot) mapper) {
    return query.snapshots().map((snapshot) {
      final out = <T>[];
      for (final doc in snapshot.docs) {
        try {
          out.add(mapper(doc));
        } catch (e) {
          debugPrint('SocietyRepository: skipped malformed ${doc.reference.path}: $e');
        }
      }
      return out;
    }).handleError((Object error) {
      debugPrint('SocietyRepository: listen failed: $error');
    });
  }

  // ------------------------------------------------------------- streams

  Stream<SocietyInfo?> watchSociety(String code) =>
      _societyDoc(code).snapshots().map((s) => s.exists ? societyFromDoc(s) : null).handleError((Object e) {
        debugPrint('SocietyRepository: society listen failed: $e');
      });

  Stream<List<Resident>> watchResidents(String code) => _listen(
        _sub(code, Collections.residents).orderBy('wing').orderBy('flatNumber'),
        residentFromDoc,
      );

  Stream<List<MaintenanceBill>> watchBills(String code) => _listen(
        _sub(code, Collections.maintenanceBills).orderBy('id', descending: true),
        billFromDoc,
      );

  Stream<List<ExpenseRecord>> watchExpenses(String code) => _listen(
        _sub(code, Collections.expenseRecords).orderBy('id', descending: true),
        expenseFromDoc,
      );

  Stream<List<NonFeeRevenue>> watchRevenues(String code) => _listen(
        _sub(code, Collections.nonFeeRevenues).orderBy('id', descending: true),
        revenueFromDoc,
      );

  Stream<List<VisitorLog>> watchVisitors(String code) => _listen(
        _sub(code, Collections.visitorLogs).orderBy('id', descending: true),
        visitorFromDoc,
      );

  Stream<List<Notice>> watchNotices(String code) => _listen(
        _sub(code, Collections.notices).orderBy('isPinned', descending: true).orderBy('id', descending: true),
        noticeFromDoc,
      );

  Stream<List<Complaint>> watchComplaints(String code) => _listen(
        _sub(code, Collections.complaints).orderBy('id', descending: true),
        complaintFromDoc,
      );

  Stream<List<EmergencyContact>> watchEmergencyContacts(String code) => _listen(
        _sub(code, Collections.emergencyContacts).orderBy('id'),
        emergencyContactFromDoc,
      );

  Stream<List<SocietyRole>> watchRoles(String code) =>
      _listen(_sub(code, Collections.roles).orderBy('name'), societyRoleFromDoc);

  Stream<List<RoleChange>> watchRoleHistory(String code) =>
      _listen(_sub(code, Collections.roleHistory).orderBy('at', descending: true).limit(100), roleChangeFromDoc);

  /// The plan catalogue is shared by every society and edited in MultiSarv Core.
  Stream<List<SubscriptionPlan>> watchSubscriptionPlans() =>
      _listen(_db.collection(Collections.subscriptionPlans), subscriptionPlanFromDoc);

  Stream<SocietySubscription?> watchSubscription(String code) => _sub(code, Collections.subscription)
          .doc(Collections.subscriptionCurrentDoc)
          .snapshots()
          .map((s) => s.exists ? societySubscriptionFromDoc(s) : null)
          .handleError((Object e) {
        debugPrint('SocietyRepository: subscription listen failed: $e');
      });

  Stream<List<SubscriptionRequest>> watchSubscriptionRequests(String code) => _listen(
        _sub(code, Collections.subscriptionRequests).orderBy('createdAt', descending: true).limit(20),
        subscriptionRequestFromDoc,
      );

  // ------------------------------------------------------- society lookup

  /// Looks a society up by its login code. Used by login and registration.
  Future<SocietyInfo?> findSocietyByCode(String code) async {
    try {
      final snap = await _societyDoc(code).get();
      return snap.exists ? societyFromDoc(snap) : null;
    } catch (e) {
      debugPrint('findSocietyByCode failed: $e');
      return null;
    }
  }

  Future<SocietyInfo?> getSocietyInfoSync() async {
    final code = _activeSocietyCode;
    if (code == null) return null;
    return findSocietyByCode(code);
  }

  // ----------------------------------------------------- one-shot lookups

  Future<List<Resident>> getAllResidentsSync() async {
    final residents = _active(Collections.residents);
    if (residents == null) return [];
    try {
      final snap = await residents.orderBy('wing').orderBy('flatNumber').get();
      return snap.docs.map(residentFromDoc).toList();
    } catch (e) {
      debugPrint('getAllResidentsSync failed: $e');
      return [];
    }
  }

  /// Flat lookup inside one society. The flat number is the document id.
  Future<Resident?> getResidentByFlat(String flatNumber, {String? societyCode}) async {
    final code = societyCode ?? _activeSocietyCode;
    if (code == null) return null;
    try {
      final flats = _sub(code, Collections.residents);
      final wanted = flatNumber.trim();
      if (wanted.isEmpty) return null;

      final direct = await flats.doc(wanted.toUpperCase()).get();
      if (direct.exists) return residentFromDoc(direct);

      final byField = await flats.where('flatNumber', isEqualTo: wanted).limit(1).get();
      if (byField.docs.isNotEmpty) return residentFromDoc(byField.docs.first);

      final all = await flats.get();
      for (final doc in all.docs) {
        final fn = doc.data()['flatNumber'];
        if (fn is String && fn.toLowerCase() == wanted.toLowerCase()) return residentFromDoc(doc);
      }
      return null;
    } catch (e) {
      debugPrint('getResidentByFlat failed: $e');
      return null;
    }
  }

  /// Mobile-number lookup inside one society.
  Future<Resident?> getResidentByMobile(String mobile, {String? societyCode}) async {
    final code = societyCode ?? _activeSocietyCode;
    if (code == null) return null;
    try {
      final wanted = mobile.trim();
      if (wanted.isEmpty) return null;
      final exact = await _sub(code, Collections.residents).where('contactNumber', isEqualTo: wanted).limit(1).get();
      if (exact.docs.isNotEmpty) return residentFromDoc(exact.docs.first);

      // Members are not always stored in one tidy format - "+91 98765 43210"
      // and "9876543210" are the same person - so compare the last ten digits.
      final digits = _last10Digits(wanted);
      if (digits.length < 10) return null;
      final all = await _sub(code, Collections.residents).get();
      for (final doc in all.docs) {
        final r = residentFromDoc(doc);
        if (_last10Digits(r.contactNumber) == digits) return r;
      }
      return null;
    } catch (e) {
      debugPrint('getResidentByMobile failed: $e');
      return null;
    }
  }

  static String _last10Digits(String raw) {
    final digits = raw.replaceAll(RegExp(r'[^0-9]'), '');
    return digits.length <= 10 ? digits : digits.substring(digits.length - 10);
  }

  // ---------------------------------------------------------------- mail

  /// Queues the "new society registered" notification for the mail worker.
  /// Keyed by society code and only written when nothing is there yet, so it
  /// can never be posted twice. Never throws.
  Future<bool> queueSocietyRegistrationEmail({
    required String societyCode,
    required String from,
    required String to,
    required String subject,
    required String bodyText,
    required String bodyHtml,
  }) async {
    try {
      final mail = _db.collection(Collections.mail).doc('society-${_normalise(societyCode)}');
      if ((await mail.get()).exists) return false;
      await mail.set({
        'to': [to],
        'from': from,
        'message': {'subject': subject, 'text': bodyText, 'html': bodyHtml},
      });
      return true;
    } catch (e) {
      debugPrint('could not queue the registration email: $e');
      return false;
    }
  }

  // ------------------------------------------------------------- society

  /// Creates or updates a society document, keyed by its login code.
  Future<void> insertOrUpdateSociety(SocietyInfo society) async {
    final code = _normalise(society.societyCode);
    await _societyDoc(code).set(societyToMap(society.copyWith(societyCode: code)));
  }

  Future<void> updateSociety(SocietyInfo society) => insertOrUpdateSociety(society);

  // ------------------------------------------------------------ residents

  Future<void> insertResident(Resident resident, {String? societyCode}) async {
    final code = societyCode ?? _activeSocietyCode;
    if (code == null) return;
    final flats = _sub(code, Collections.residents);
    final trimmed = resident.flatNumber.trim().toUpperCase();
    final docId = trimmed.isEmpty ? flats.doc().id : trimmed;
    final withId = resident.copyWith(
      id: resident.id == 0 ? await _nextId(code, Collections.residents) : resident.id,
      flatNumber: docId,
      docId: docId,
    );
    await flats.doc(docId).set(residentToMap(withId));
  }

  Future<void> updateResident(Resident resident) async {
    final flats = _active(Collections.residents);
    if (flats == null) return;
    final docId = resident.docId.isEmpty ? resident.flatNumber : resident.docId;
    await flats.doc(docId).set(residentToMap(resident));
  }

  Future<void> deleteResident(Resident resident) async {
    final flats = _active(Collections.residents);
    if (flats == null) return;
    await flats.doc(resident.docId.isEmpty ? resident.flatNumber : resident.docId).delete();
  }

  Future<void> updateResidentPin(String flatNumber, String newPin) async {
    final flats = _active(Collections.residents);
    if (flats == null) return;
    final resident = await getResidentByFlat(flatNumber);
    if (resident == null) return;
    await flats.doc(resident.docId.isEmpty ? resident.flatNumber : resident.docId).update({'pin': newPin});
  }

  /// PIN reset for a society the device is not signed in to (Forgot PIN runs
  /// before login, so there is no active society to scope against).
  Future<void> resetResidentPin(String societyCode, Resident resident, String newPin) async {
    final docId = resident.docId.isEmpty ? resident.flatNumber : resident.docId;
    await _sub(societyCode, Collections.residents).doc(docId).update({'pin': newPin});
  }

  /// Writes freshly generated, still-vacant units in batches. Flat numbers that
  /// already carry a document are left untouched.
  Future<void> insertVacantUnits(String societyCode, List<Resident> units) async {
    if (units.isEmpty) return;
    final flats = _sub(societyCode, Collections.residents);
    Set<String> existing;
    try {
      existing = (await flats.get()).docs.map((d) => d.id.toUpperCase()).toSet();
    } catch (_) {
      existing = {};
    }

    var nextId = await _nextId(societyCode, Collections.residents);
    final firstId = nextId;
    for (var start = 0; start < units.length; start += _batchLimit) {
      final chunk = units.sublist(start, (start + _batchLimit).clamp(0, units.length));
      final batch = _db.batch();
      var wrote = 0;
      for (final unit in chunk) {
        final docId = unit.flatNumber.trim().toUpperCase();
        if (docId.isEmpty || existing.contains(docId)) continue;
        batch.set(flats.doc(docId), residentToMap(unit.copyWith(docId: docId, flatNumber: docId, id: nextId++)));
        wrote++;
      }
      if (wrote > 0) await batch.commit();
    }

    // _nextId() only claimed a single id; carry the counter past the rest so the
    // next resident added by hand does not reuse one of these.
    final consumed = nextId - firstId;
    if (consumed > 1) {
      try {
        await _sub(societyCode, Collections.counters)
            .doc(Collections.residents)
            .update({'value': FieldValue.increment(consumed - 1)});
      } catch (e) {
        debugPrint('unit counter bump failed: $e');
      }
    }
  }

  // ---------------------------------------------------------------- roles

  DocumentReference<Map<String, dynamic>> _residentDoc(String code, Resident r) =>
      _sub(code, Collections.residents).doc(r.docId.isEmpty ? r.flatNumber : r.docId);

  /// Creates or updates a society staff role. Members already holding it are
  /// brought in line in the same batch, so their name and base role never
  /// drift from the role itself.
  Future<void> saveRole(SocietyRole role, {List<Resident> holders = const []}) async {
    final code = _activeSocietyCode;
    if (code == null) return;
    final roles = _sub(code, Collections.roles);
    final ref = role.docId.isEmpty ? roles.doc() : roles.doc(role.docId);
    final batch = _db.batch();
    batch.set(
        ref,
        {
          ...societyRoleToMap(role),
          if (role.docId.isEmpty) 'createdAt': FieldValue.serverTimestamp(),
        },
        SetOptions(merge: true));
    for (final member in holders) {
      batch.update(_residentDoc(code, member), {'role': role.baseRole.name, 'customRoleName': role.name.trim()});
    }
    await batch.commit();
  }

  Future<void> deleteRole(SocietyRole role) async {
    final roles = _active(Collections.roles);
    if (roles == null || role.docId.isEmpty) return;
    await roles.doc(role.docId).delete();
  }

  /// Gives one member a new role and records it in the audit trail, together.
  /// Only the role fields are touched, so nothing else on the record is lost.
  Future<void> changeMemberRole(Resident member, UserRole newRole, SocietyRole? customRole, RoleChange entry) async {
    final code = _activeSocietyCode;
    if (code == null) return;
    final batch = _db.batch();
    batch.update(_residentDoc(code, member), {
      'role': newRole.name,
      'customRoleId': customRole?.docId ?? '',
      'customRoleName': customRole?.name.trim() ?? '',
    });
    batch.set(_sub(code, Collections.roleHistory).doc(), roleChangeToMap(entry));
    await batch.commit();
  }

  /// Adds one entry to the audit trail on its own (a new hire, or a staff
  /// record rewritten from the staff form).
  Future<void> recordRoleChange(RoleChange entry) async {
    final history = _active(Collections.roleHistory);
    if (history == null) return;
    await history.add(roleChangeToMap(entry));
  }

  /// Hands the Chairman's post from [from] to [to] in one write, so the
  /// society is never left with no chairman or with two.
  Future<void> handOverChairman({
    required Resident from,
    required UserRole fromNewRole,
    required Resident to,
    required RoleChange fromEntry,
    required RoleChange toEntry,
  }) async {
    final code = _activeSocietyCode;
    if (code == null) return;
    final batch = _db.batch();
    final cleared = {'customRoleId': '', 'customRoleName': ''};
    batch.update(_residentDoc(code, to), {'role': UserRole.CHAIRMAN.name, ...cleared});
    batch.update(_residentDoc(code, from), {'role': fromNewRole.name, ...cleared});
    final history = _sub(code, Collections.roleHistory);
    batch.set(history.doc(), roleChangeToMap(toEntry));
    batch.set(history.doc(), roleChangeToMap(fromEntry));
    await batch.commit();
  }

  // -------------------------------------------------------- subscription

  /// Asks MultiSarv to start, renew or change the society's plan. The plan
  /// itself is only ever switched on from MultiSarv Core.
  Future<void> insertSubscriptionRequest(SubscriptionRequest request) async {
    final requests = _active(Collections.subscriptionRequests);
    if (requests == null) return;
    await requests.add(subscriptionRequestToMap(request));
  }

  // ------------------------------------------------------ maintenance bills

  Future<void> insertBill(MaintenanceBill bill) async {
    final code = _activeSocietyCode;
    if (code == null) return;
    final withId = bill.copyWith(id: bill.id == 0 ? await _nextId(code, Collections.maintenanceBills) : bill.id);
    await _sub(code, Collections.maintenanceBills).add(billToMap(withId));
  }

  Future<void> updateBill(MaintenanceBill bill) async {
    final bills = _active(Collections.maintenanceBills);
    if (bills == null || bill.docId.isEmpty) return;
    await bills.doc(bill.docId).set(billToMap(bill));
  }

  Future<void> deleteBill(MaintenanceBill bill) async {
    final bills = _active(Collections.maintenanceBills);
    if (bills == null || bill.docId.isEmpty) return;
    await bills.doc(bill.docId).delete();
  }

  // ------------------------------------------------------------- expenses

  Future<void> insertExpense(ExpenseRecord expense) async {
    final code = _activeSocietyCode;
    if (code == null) return;
    final withId =
        expense.copyWith(id: expense.id == 0 ? await _nextId(code, Collections.expenseRecords) : expense.id);
    await _sub(code, Collections.expenseRecords).add(expenseToMap(withId));
  }

  /// Rewrites an expense. The balance is worked out from the vouchers, so the
  /// new amount takes effect the moment this document changes.
  Future<void> updateExpense(ExpenseRecord previous, ExpenseRecord updated) async {
    final expenses = _active(Collections.expenseRecords);
    if (expenses == null) return;
    final docId = updated.docId.isEmpty ? previous.docId : updated.docId;
    if (docId.isEmpty) return;
    await expenses.doc(docId).set(expenseToMap(updated.copyWith(docId: docId)));
  }

  Future<void> deleteExpense(ExpenseRecord expense) async {
    final expenses = _active(Collections.expenseRecords);
    if (expenses == null || expense.docId.isEmpty) return;
    await expenses.doc(expense.docId).delete();
  }

  // ------------------------------------------------------- non-fee revenue

  Future<void> insertRevenue(NonFeeRevenue revenue) async {
    final code = _activeSocietyCode;
    if (code == null) return;
    final withId =
        revenue.copyWith(id: revenue.id == 0 ? await _nextId(code, Collections.nonFeeRevenues) : revenue.id);
    await _sub(code, Collections.nonFeeRevenues).add(revenueToMap(withId));
  }

  Future<void> deleteRevenue(NonFeeRevenue revenue) async {
    final revenues = _active(Collections.nonFeeRevenues);
    if (revenues == null || revenue.docId.isEmpty) return;
    await revenues.doc(revenue.docId).delete();
  }

  // ------------------------------------------------------------- visitors

  Future<void> insertVisitor(VisitorLog visitor) async {
    final code = _activeSocietyCode;
    if (code == null) return;
    final withId = visitor.copyWith(id: visitor.id == 0 ? await _nextId(code, Collections.visitorLogs) : visitor.id);
    await _sub(code, Collections.visitorLogs).add(visitorToMap(withId));
  }

  Future<void> updateVisitor(VisitorLog visitor) async {
    final visitors = _active(Collections.visitorLogs);
    if (visitors == null || visitor.docId.isEmpty) return;
    await visitors.doc(visitor.docId).set(visitorToMap(visitor));
  }

  Future<VisitorLog?> verifyVisitorPasscode(String passcode) async {
    final visitors = _active(Collections.visitorLogs);
    if (visitors == null) return null;
    try {
      final snap = await visitors.where('passcode', isEqualTo: passcode.trim()).limit(1).get();
      return snap.docs.isEmpty ? null : visitorFromDoc(snap.docs.first);
    } catch (e) {
      debugPrint('verifyVisitorPasscode failed: $e');
      return null;
    }
  }

  Future<void> markVisitorExit(String exitTime, VisitorLog visitor) =>
      updateVisitor(visitor.copyWith(status: VisitorStatus.EXITED, exitTime: exitTime));

  // -------------------------------------------------------------- notices

  Future<void> insertNotice(Notice notice, {String? societyCode}) async {
    final code = societyCode ?? _activeSocietyCode;
    if (code == null) return;
    final withId = notice.copyWith(id: notice.id == 0 ? await _nextId(code, Collections.notices) : notice.id);
    await _sub(code, Collections.notices).add(noticeToMap(withId));
  }

  Future<void> deleteNotice(Notice notice) async {
    final notices = _active(Collections.notices);
    if (notices == null || notice.docId.isEmpty) return;
    await notices.doc(notice.docId).delete();
  }

  // ----------------------------------------------------------- complaints

  Future<void> insertComplaint(Complaint complaint) async {
    final code = _activeSocietyCode;
    if (code == null) return;
    final withId =
        complaint.copyWith(id: complaint.id == 0 ? await _nextId(code, Collections.complaints) : complaint.id);
    await _sub(code, Collections.complaints).add(complaintToMap(withId));
  }

  Future<void> updateComplaint(Complaint complaint) async {
    final complaints = _active(Collections.complaints);
    if (complaints == null || complaint.docId.isEmpty) return;
    await complaints.doc(complaint.docId).set(complaintToMap(complaint));
  }

  Future<void> deleteComplaint(Complaint complaint) async {
    final complaints = _active(Collections.complaints);
    if (complaints == null || complaint.docId.isEmpty) return;
    await complaints.doc(complaint.docId).delete();
  }

  Future<void> updateComplaintStatus(Complaint complaint, TicketStatus newStatus, String notes) async {
    final complaints = _active(Collections.complaints);
    if (complaints == null || complaint.docId.isEmpty) return;
    await complaints.doc(complaint.docId).update({
      'status': newStatus.name,
      'resolutionNotes': notes.trim().isEmpty ? complaint.resolutionNotes : notes,
    });
  }

  // -------------------------------------------------- emergency contacts

  /// Creates or updates one emergency contact for the active society.
  Future<void> saveEmergencyContact(EmergencyContact contact) async {
    final code = _activeSocietyCode;
    if (code == null) return;
    final contacts = _sub(code, Collections.emergencyContacts);
    final withId =
        contact.copyWith(id: contact.id == 0 ? await _nextId(code, Collections.emergencyContacts) : contact.id);
    final docId = withId.docId.isEmpty ? contacts.doc().id : withId.docId;
    await contacts.doc(docId).set(emergencyContactToMap(withId.copyWith(docId: docId)));
  }

  Future<void> deleteEmergencyContact(EmergencyContact contact) async {
    final contacts = _active(Collections.emergencyContacts);
    if (contacts == null || contact.docId.isEmpty) return;
    await contacts.doc(contact.docId).delete();
  }

  // --------------------------------------------------------------- shared

  /// Next numeric id for a collection, from a per-society counter. Ids restart
  /// at 1 for every society.
  Future<int> _nextId(String societyCode, String collectionName) async {
    final counter = _sub(societyCode, Collections.counters).doc(collectionName);
    try {
      return await _db.runTransaction<int>((txn) async {
        final snap = await txn.get(counter);
        final current = snap.data()?['value'];
        final next = (current is num ? current.toInt() : 0) + 1;
        txn.set(counter, {'value': next, 'collection': collectionName, 'docId': collectionName});
        return next;
      });
    } catch (e) {
      debugPrint('nextId failed for $collectionName: $e');
      return DateTime.now().millisecondsSinceEpoch & 0x7FFFFFFF;
    }
  }
}
