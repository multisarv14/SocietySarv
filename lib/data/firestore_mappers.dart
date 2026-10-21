// Document <-> entity conversion.
//
// Enums are persisted as their name string so the documents stay readable in
// the Firebase console, and every read falls back to a safe default when a
// field is missing or was hand-edited to something unknown. Field names match
// the Kotlin app exactly, so both apps read and write the same documents.

import 'package:cloud_firestore/cloud_firestore.dart';

import 'models.dart';

typedef Json = Map<String, dynamic>;

String _str(Json m, String key, [String fallback = '']) {
  final v = m[key];
  return v is String ? v : fallback;
}

double _num(Json m, String key, [double fallback = 0.0]) {
  final v = m[key];
  return v is num ? v.toDouble() : fallback;
}

int _int(Json m, String key, [int fallback = 0]) {
  final v = m[key];
  return v is num ? v.toInt() : fallback;
}

bool _bool(Json m, String key, [bool fallback = false]) {
  final v = m[key];
  return v is bool ? v : fallback;
}

List<String> _strList(Json m, String key) {
  final v = m[key];
  if (v is! List) return [];
  return v.whereType<String>().toList();
}

List<Json> _mapList(Json m, String key) {
  final v = m[key];
  if (v is! List) return [];
  return v.whereType<Map>().map((e) => Map<String, dynamic>.from(e)).toList();
}

Json _data(DocumentSnapshot doc) => (doc.data() as Map<String, dynamic>?) ?? <String, dynamic>{};

// ------------------------------------------------------------------ society

SocietyInfo societyFromDoc(DocumentSnapshot doc) {
  final m = _data(doc);
  return SocietyInfo(
    docId: doc.id,
    id: _int(m, 'id', 1),
    name: _str(m, 'name'),
    registrationNumber: _str(m, 'registrationNumber'),
    societyCode: _str(m, 'societyCode'),
    address: _str(m, 'address'),
    totalWings: _int(m, 'totalWings'),
    totalFlats: _int(m, 'totalFlats'),
    bankAccountName: _str(m, 'bankAccountName'),
    bankAccountNumber: _str(m, 'bankAccountNumber'),
    ifscCode: _str(m, 'ifscCode'),
    upiId: _str(m, 'upiId'),
    paymentQrBase64: _str(m, 'paymentQrBase64'),
    currentBalanceCash: _num(m, 'currentBalanceCash'),
    currentBalanceBank: _num(m, 'currentBalanceBank'),
    lateFeePercentPerMonth: _num(m, 'lateFeePercentPerMonth', 2.0),
    lateFeeGraceDays: _int(m, 'lateFeeGraceDays', 10),
    tariff1BHK: _num(m, 'tariff1BHK', 1500.0),
    tariff2BHK: _num(m, 'tariff2BHK', 2200.0),
    tariff3BHK: _num(m, 'tariff3BHK', 3000.0),
    tariffPenthouse: _num(m, 'tariffPenthouse', 4500.0),
    tariffShop: _num(m, 'tariffShop', 3500.0),
    waterChargesPercent: _num(m, 'waterChargesPercent', 10.0),
    sinkingFundPercent: _num(m, 'sinkingFundPercent', 5.0),
    repairFundPercent: _num(m, 'repairFundPercent', 20.0),
    otherMaintenancePercent: _num(m, 'otherMaintenancePercent', 65.0),
    logoBase64: _str(m, 'logoBase64'),
    country: _str(m, 'country', 'India'),
    state: _str(m, 'state'),
    city: _str(m, 'city'),
    pinCode: _str(m, 'pinCode'),
    contactNumber: _str(m, 'contactNumber'),
    email: _str(m, 'email'),
    wingNames: _strList(m, 'wingNames'),
    wingPlans: _mapList(m, 'wingPlans').map((raw) {
      var counts = _mapList(raw, 'unitCounts')
          .map((c) => WingUnitCount(unitType: _str(c, 'unitType'), quantity: _int(c, 'quantity')))
          .toList();
      if (counts.isEmpty) {
        // Wings written before unit quantities existed stored a single type
        // with a units-per-floor count instead. Fold that into one entry.
        final floors = _int(raw, 'totalFloors') + (_bool(raw, 'hasGroundFloor') ? 1 : 0);
        final legacyTotal = floors * _int(raw, 'unitsPerFloor');
        if (legacyTotal > 0) counts = [WingUnitCount(unitType: _str(raw, 'unitType'), quantity: legacyTotal)];
      }
      return WingPlan(name: _str(raw, 'name'), totalFloors: _int(raw, 'totalFloors'), unitCounts: counts);
    }).toList(),
    unitTypes: _mapList(m, 'unitTypes')
        .map((raw) => UnitTypeConfig(name: _str(raw, 'name'), monthlyTariff: _num(raw, 'monthlyTariff')))
        .toList(),
    coveredParkingCount: _int(m, 'coveredParkingCount'),
    openParkingCount: _int(m, 'openParkingCount'),
    billingStartMonth: _str(m, 'billingStartMonth'),
  );
}

Json societyToMap(SocietyInfo s) => {
      'docId': s.societyCode,
      'id': s.id,
      'name': s.name,
      'registrationNumber': s.registrationNumber,
      'societyCode': s.societyCode,
      'address': s.address,
      'totalWings': s.totalWings,
      'totalFlats': s.totalFlats,
      'bankAccountName': s.bankAccountName,
      'bankAccountNumber': s.bankAccountNumber,
      'ifscCode': s.ifscCode,
      'upiId': s.upiId,
      'paymentQrBase64': s.paymentQrBase64,
      'currentBalanceCash': s.currentBalanceCash,
      'currentBalanceBank': s.currentBalanceBank,
      'lateFeePercentPerMonth': s.lateFeePercentPerMonth,
      'lateFeeGraceDays': s.lateFeeGraceDays,
      'tariff1BHK': s.tariff1BHK,
      'tariff2BHK': s.tariff2BHK,
      'tariff3BHK': s.tariff3BHK,
      'tariffPenthouse': s.tariffPenthouse,
      'tariffShop': s.tariffShop,
      'waterChargesPercent': s.waterChargesPercent,
      'sinkingFundPercent': s.sinkingFundPercent,
      'repairFundPercent': s.repairFundPercent,
      'otherMaintenancePercent': s.otherMaintenancePercent,
      'logoBase64': s.logoBase64,
      'country': s.country,
      'state': s.state,
      'city': s.city,
      'pinCode': s.pinCode,
      'contactNumber': s.contactNumber,
      'email': s.email,
      'wingNames': s.wingNames,
      'wingPlans': [
        for (final plan in s.wingPlans)
          {
            'name': plan.name,
            'totalFloors': plan.totalFloors,
            'unitCounts': [
              for (final c in plan.unitCounts) {'unitType': c.unitType, 'quantity': c.quantity},
            ],
          },
      ],
      'unitTypes': [
        for (final t in s.unitTypes) {'name': t.name, 'monthlyTariff': t.monthlyTariff},
      ],
      'coveredParkingCount': s.coveredParkingCount,
      'openParkingCount': s.openParkingCount,
      'billingStartMonth': s.billingStartMonth,
    };

// ----------------------------------------------------------------- resident

Resident residentFromDoc(DocumentSnapshot doc) {
  final m = _data(doc);
  return Resident(
    docId: doc.id,
    id: _int(m, 'id'),
    flatNumber: _str(m, 'flatNumber', doc.id),
    wing: _str(m, 'wing'),
    ownerName: _str(m, 'ownerName'),
    coApplicantName: _str(m, 'coApplicantName'),
    tenantName: _str(m, 'tenantName'),
    tenantContact: _str(m, 'tenantContact'),
    rentAgreementBase64: _str(m, 'rentAgreementBase64'),
    contactNumber: _str(m, 'contactNumber'),
    email: _str(m, 'email'),
    role: enumByName(UserRole.values, m['role'] as String?, UserRole.RESIDENT),
    pin: _str(m, 'pin'),
    familyMemberCount: _int(m, 'familyMemberCount'),
    vehicleTwoWheelerCount: _int(m, 'vehicleTwoWheelerCount'),
    vehicleFourWheelerCount: _int(m, 'vehicleFourWheelerCount'),
    twoWheelerNumber: _str(m, 'twoWheelerNumber'),
    fourWheelerNumber: _str(m, 'fourWheelerNumber'),
    parkingSlot: _str(m, 'parkingSlot'),
    parkingType: enumByName(ParkingType.values, m['parkingType'] as String?, ParkingType.NONE),
    unitType: _str(m, 'unitType'),
    flatType: enumByName(FlatType.values, m['flatType'] as String?, FlatType.TWO_BHK),
    squareFeet: _int(m, 'squareFeet'),
    balanceDue: _num(m, 'balanceDue'),
    advanceBalance: _num(m, 'advanceBalance'),
    // The Kotlin mapper never wrote advanceMode, so it is read leniently.
    advanceMode: enumByName(PaymentMode.values, m['advanceMode'] as String?, PaymentMode.BANK_TRANSFER),
    isOwnerLiving: _bool(m, 'isOwnerLiving', true),
    photoBase64: _str(m, 'photoBase64'),
    staffDesignation: _str(m, 'staffDesignation'),
    isActive: _bool(m, 'isActive', true),
    customRoleId: _str(m, 'customRoleId'),
    customRoleName: _str(m, 'customRoleName'),
  );
}

Json residentToMap(Resident r) => {
      'docId': r.flatNumber,
      'id': r.id,
      'flatNumber': r.flatNumber,
      'wing': r.wing,
      'ownerName': r.ownerName,
      'coApplicantName': r.coApplicantName,
      'tenantName': r.tenantName,
      'tenantContact': r.tenantContact,
      'rentAgreementBase64': r.rentAgreementBase64,
      'contactNumber': r.contactNumber,
      'email': r.email,
      'role': r.role.name,
      'pin': r.pin,
      'familyMemberCount': r.familyMemberCount,
      'vehicleTwoWheelerCount': r.vehicleTwoWheelerCount,
      'vehicleFourWheelerCount': r.vehicleFourWheelerCount,
      'twoWheelerNumber': r.twoWheelerNumber,
      'fourWheelerNumber': r.fourWheelerNumber,
      'parkingSlot': r.parkingSlot,
      'parkingType': r.parkingType.name,
      'unitType': r.unitType,
      'flatType': r.flatType.name,
      'squareFeet': r.squareFeet,
      'balanceDue': r.balanceDue,
      'advanceBalance': r.advanceBalance,
      'advanceMode': r.advanceMode.name,
      'isOwnerLiving': r.isOwnerLiving,
      'photoBase64': r.photoBase64,
      'staffDesignation': r.staffDesignation,
      'isActive': r.isActive,
      'customRoleId': r.customRoleId,
      'customRoleName': r.customRoleName,
    };

// --------------------------------------------------------- maintenance bill

MaintenanceBill billFromDoc(DocumentSnapshot doc) {
  final m = _data(doc);
  return MaintenanceBill(
    docId: doc.id,
    id: _int(m, 'id'),
    flatNumber: _str(m, 'flatNumber'),
    residentName: _str(m, 'residentName'),
    billMonth: _str(m, 'billMonth'),
    baseAmount: _num(m, 'baseAmount'),
    waterCharges: _num(m, 'waterCharges'),
    sinkingFund: _num(m, 'sinkingFund'),
    repairFund: _num(m, 'repairFund'),
    parkingCharges: _num(m, 'parkingCharges'),
    nonOccupancyCharges: _num(m, 'nonOccupancyCharges'),
    lateFeeAmount: _num(m, 'lateFeeAmount'),
    totalAmount: _num(m, 'totalAmount'),
    amountPaid: _num(m, 'amountPaid'),
    status: enumByName(PaymentStatus.values, m['status'] as String?, PaymentStatus.UNPAID),
    dueDate: _str(m, 'dueDate'),
    paidDate: _str(m, 'paidDate'),
    paymentMode: enumByNameOrNull(PaymentMode.values, m['paymentMode'] as String?),
    transactionRef: _str(m, 'transactionRef'),
    receiptNumber: _str(m, 'receiptNumber'),
    receiptImageBase64: _str(m, 'receiptImageBase64'),
  );
}

Json billToMap(MaintenanceBill b) => {
      'id': b.id,
      'flatNumber': b.flatNumber,
      'residentName': b.residentName,
      'billMonth': b.billMonth,
      'baseAmount': b.baseAmount,
      'waterCharges': b.waterCharges,
      'sinkingFund': b.sinkingFund,
      'repairFund': b.repairFund,
      'parkingCharges': b.parkingCharges,
      'nonOccupancyCharges': b.nonOccupancyCharges,
      'lateFeeAmount': b.lateFeeAmount,
      'totalAmount': b.totalAmount,
      'amountPaid': b.amountPaid,
      'status': b.status.name,
      'dueDate': b.dueDate,
      'paidDate': b.paidDate,
      'paymentMode': b.paymentMode?.name,
      'transactionRef': b.transactionRef,
      'receiptNumber': b.receiptNumber,
      'receiptImageBase64': b.receiptImageBase64,
    };

// ------------------------------------------------------------------ expense

ExpenseRecord expenseFromDoc(DocumentSnapshot doc) {
  final m = _data(doc);
  return ExpenseRecord(
    docId: doc.id,
    id: _int(m, 'id'),
    title: _str(m, 'title'),
    category: enumByName(ExpenseCategory.values, m['category'] as String?, ExpenseCategory.MISCELLANEOUS),
    amount: _num(m, 'amount'),
    date: _str(m, 'date'),
    vendorName: _str(m, 'vendorName'),
    vendorContact: _str(m, 'vendorContact'),
    paymentMode: enumByName(PaymentMode.values, m['paymentMode'] as String?, PaymentMode.BANK_TRANSFER),
    approvedBy: _str(m, 'approvedBy', 'Secretary'),
    notes: _str(m, 'notes'),
    receiptNumber: _str(m, 'receiptNumber'),
    status: enumByName(PaymentStatus.values, m['status'] as String?, PaymentStatus.PAID),
    billImageBase64: _str(m, 'billImageBase64'),
  );
}

Json expenseToMap(ExpenseRecord e) => {
      'id': e.id,
      'title': e.title,
      'category': e.category.name,
      'amount': e.amount,
      'date': e.date,
      'vendorName': e.vendorName,
      'vendorContact': e.vendorContact,
      'paymentMode': e.paymentMode.name,
      'approvedBy': e.approvedBy,
      'notes': e.notes,
      'receiptNumber': e.receiptNumber,
      'status': e.status.name,
      'billImageBase64': e.billImageBase64,
    };

// ------------------------------------------------------------ non-fee revenue

NonFeeRevenue revenueFromDoc(DocumentSnapshot doc) {
  final m = _data(doc);
  return NonFeeRevenue(
    docId: doc.id,
    id: _int(m, 'id'),
    title: _str(m, 'title'),
    sourceCategory: enumByName(RevenueSource.values, m['sourceCategory'] as String?, RevenueSource.PENALTY_FINE),
    amount: _num(m, 'amount'),
    date: _str(m, 'date'),
    payerName: _str(m, 'payerName'),
    flatNumber: _str(m, 'flatNumber'),
    paymentMode: enumByName(PaymentMode.values, m['paymentMode'] as String?, PaymentMode.UPI),
    receiptNumber: _str(m, 'receiptNumber'),
    notes: _str(m, 'notes'),
  );
}

Json revenueToMap(NonFeeRevenue r) => {
      'id': r.id,
      'title': r.title,
      'sourceCategory': r.sourceCategory.name,
      'amount': r.amount,
      'date': r.date,
      'payerName': r.payerName,
      'flatNumber': r.flatNumber,
      'paymentMode': r.paymentMode.name,
      'receiptNumber': r.receiptNumber,
      'notes': r.notes,
    };

// ------------------------------------------------------------------- visitor

VisitorLog visitorFromDoc(DocumentSnapshot doc) {
  final m = _data(doc);
  return VisitorLog(
    docId: doc.id,
    id: _int(m, 'id'),
    visitorName: _str(m, 'visitorName'),
    phone: _str(m, 'phone'),
    flatNumber: _str(m, 'flatNumber'),
    purpose: enumByName(VisitorPurpose.values, m['purpose'] as String?, VisitorPurpose.OTHER),
    vehicleNumber: _str(m, 'vehicleNumber'),
    entryTime: _str(m, 'entryTime'),
    exitTime: _str(m, 'exitTime'),
    status: enumByName(VisitorStatus.values, m['status'] as String?, VisitorStatus.INSIDE),
    passcode: _str(m, 'passcode'),
    guardName: _str(m, 'guardName', 'Gate Guard'),
    photoBase64: _str(m, 'photoBase64'),
  );
}

Json visitorToMap(VisitorLog v) => {
      'id': v.id,
      'visitorName': v.visitorName,
      'phone': v.phone,
      'flatNumber': v.flatNumber,
      'purpose': v.purpose.name,
      'vehicleNumber': v.vehicleNumber,
      'entryTime': v.entryTime,
      'exitTime': v.exitTime,
      'status': v.status.name,
      'passcode': v.passcode,
      'guardName': v.guardName,
      'photoBase64': v.photoBase64,
    };

// -------------------------------------------------------------------- notice

Notice noticeFromDoc(DocumentSnapshot doc) {
  final m = _data(doc);
  return Notice(
    docId: doc.id,
    id: _int(m, 'id'),
    title: _str(m, 'title'),
    content: _str(m, 'content'),
    publishedDate: _str(m, 'publishedDate'),
    isPinned: _bool(m, 'isPinned'),
    category: enumByName(NoticeCategory.values, m['category'] as String?, NoticeCategory.GENERAL),
    authorRole: _str(m, 'authorRole', 'Managing Committee'),
    imageBase64: _str(m, 'imageBase64'),
  );
}

Json noticeToMap(Notice n) => {
      'id': n.id,
      'title': n.title,
      'content': n.content,
      'publishedDate': n.publishedDate,
      'isPinned': n.isPinned,
      'category': n.category.name,
      'authorRole': n.authorRole,
      'imageBase64': n.imageBase64,
    };

// ----------------------------------------------------------------- complaint

Complaint complaintFromDoc(DocumentSnapshot doc) {
  final m = _data(doc);
  return Complaint(
    docId: doc.id,
    id: _int(m, 'id'),
    flatNumber: _str(m, 'flatNumber'),
    residentName: _str(m, 'residentName'),
    category: enumByName(ComplaintCategory.values, m['category'] as String?, ComplaintCategory.OTHER),
    description: _str(m, 'description'),
    filedDate: _str(m, 'filedDate'),
    status: enumByName(TicketStatus.values, m['status'] as String?, TicketStatus.OPEN),
    resolutionNotes: _str(m, 'resolutionNotes'),
    attachmentBase64: _str(m, 'attachmentBase64'),
  );
}

Json complaintToMap(Complaint c) => {
      'id': c.id,
      'flatNumber': c.flatNumber,
      'residentName': c.residentName,
      'category': c.category.name,
      'description': c.description,
      'filedDate': c.filedDate,
      'status': c.status.name,
      'resolutionNotes': c.resolutionNotes,
      'attachmentBase64': c.attachmentBase64,
    };

// --------------------------------------------------------- emergency contact

EmergencyContact emergencyContactFromDoc(DocumentSnapshot doc) {
  final m = _data(doc);
  return EmergencyContact(
    docId: doc.id,
    id: _int(m, 'id'),
    titleEn: _str(m, 'titleEn'),
    titleMr: _str(m, 'titleMr'),
    name: _str(m, 'name'),
    phoneNumber: _str(m, 'phoneNumber'),
    category: _str(m, 'category'),
    isActive: _bool(m, 'isActive', true),
  );
}

Json emergencyContactToMap(EmergencyContact c) => {
      'id': c.id,
      'titleEn': c.titleEn,
      'titleMr': c.titleMr,
      'name': c.name,
      'phoneNumber': c.phoneNumber,
      'category': c.category,
      'isActive': c.isActive,
    };

// -------------------------------------------------------------- roles

DateTime? _time(Json m, String key) {
  final v = m[key];
  if (v is Timestamp) return v.toDate();
  if (v is String) return DateTime.tryParse(v);
  return null;
}

SocietyRole societyRoleFromDoc(DocumentSnapshot doc) {
  final m = _data(doc);
  return SocietyRole(
    docId: doc.id,
    name: _str(m, 'name'),
    description: _str(m, 'description'),
    baseRole: enumByName(UserRole.values, m['baseRole'] as String?, UserRole.MANAGER),
    menus: normaliseRoleMenus(_strList(m, 'menus')),
    isActive: _bool(m, 'isActive', true),
  );
}

Json societyRoleToMap(SocietyRole r) => {
      'name': r.name.trim(),
      'description': r.description.trim(),
      'baseRole': r.baseRole.name,
      'menus': normaliseRoleMenus(r.menus),
      'isActive': r.isActive,
      'updatedAt': FieldValue.serverTimestamp(),
    };

RoleChange roleChangeFromDoc(DocumentSnapshot doc) {
  final m = _data(doc);
  return RoleChange(
    docId: doc.id,
    flatNumber: _str(m, 'flatNumber'),
    memberName: _str(m, 'memberName'),
    fromRole: enumByName(UserRole.values, m['fromRole'] as String?, UserRole.RESIDENT),
    toRole: enumByName(UserRole.values, m['toRole'] as String?, UserRole.RESIDENT),
    fromCustomRoleName: _str(m, 'fromCustomRoleName'),
    toCustomRoleName: _str(m, 'toCustomRoleName'),
    changedBy: _str(m, 'changedBy'),
    source: _str(m, 'source', 'app'),
    at: _time(m, 'at'),
  );
}

Json roleChangeToMap(RoleChange c) => {
      'flatNumber': c.flatNumber,
      'memberName': c.memberName,
      'fromRole': c.fromRole.name,
      'toRole': c.toRole.name,
      'fromCustomRoleName': c.fromCustomRoleName,
      'toCustomRoleName': c.toCustomRoleName,
      'changedBy': c.changedBy,
      'source': c.source,
      'at': FieldValue.serverTimestamp(),
    };

// ------------------------------------------------------- subscriptions

SubscriptionPlan subscriptionPlanFromDoc(DocumentSnapshot doc) {
  final m = _data(doc);
  return SubscriptionPlan(
    id: doc.id,
    name: _str(m, 'name', doc.id),
    description: _str(m, 'description'),
    price: _num(m, 'price'),
    durationMonths: _int(m, 'durationMonths', 12),
    features: _strList(m, 'features'),
    isActive: _bool(m, 'isActive', true),
    sortOrder: _int(m, 'sortOrder'),
  );
}

SocietySubscription societySubscriptionFromDoc(DocumentSnapshot doc) {
  final m = _data(doc);
  return SocietySubscription(
    planId: _str(m, 'planId'),
    planName: _str(m, 'planName'),
    price: _num(m, 'price'),
    durationMonths: _int(m, 'durationMonths'),
    features: _strList(m, 'features'),
    startDate: _str(m, 'startDate'),
    expiryDate: _str(m, 'expiryDate'),
    updatedBy: _str(m, 'updatedBy'),
  );
}

SubscriptionRequest subscriptionRequestFromDoc(DocumentSnapshot doc) {
  final m = _data(doc);
  return SubscriptionRequest(
    docId: doc.id,
    planId: _str(m, 'planId'),
    planName: _str(m, 'planName'),
    requestedBy: _str(m, 'requestedBy'),
    requestedFlat: _str(m, 'requestedFlat'),
    contactNumber: _str(m, 'contactNumber'),
    note: _str(m, 'note'),
    status: enumByName(SubscriptionRequestStatus.values, m['status'] as String?, SubscriptionRequestStatus.PENDING),
    adminNote: _str(m, 'adminNote'),
    createdAt: _time(m, 'createdAt'),
  );
}

Json subscriptionRequestToMap(SubscriptionRequest r) => {
      'planId': r.planId,
      'planName': r.planName,
      'requestedBy': r.requestedBy,
      'requestedFlat': r.requestedFlat,
      'contactNumber': r.contactNumber,
      'note': r.note.trim(),
      'status': SubscriptionRequestStatus.PENDING.name,
      'adminNote': '',
      'createdAt': FieldValue.serverTimestamp(),
    };
