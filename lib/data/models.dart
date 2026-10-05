// Domain entities, ported one-for-one from the Kotlin data layer.
//
// Enum value names are kept in UPPER_CASE on purpose: they are persisted to
// Firestore by name ("CHAIRMAN", "UNPAID", ...) and the existing documents
// written by the Kotlin app must keep reading back unchanged.
// ignore_for_file: constant_identifier_names

import 'package:intl/intl.dart';

enum UserRole {
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
  MANAGER;

  /// Roles that are employed by the society rather than living in it.
  bool get isStaff => const {
        UserRole.GUARD,
        UserRole.HOUSEKEEPING,
        UserRole.MAINTENANCE_STAFF,
        UserRole.ACCOUNTANT,
        UserRole.MANAGER,
      }.contains(this);

  /// The security team - the only role the Security menu is shown to.
  bool get isSecurityTeam => this == UserRole.GUARD;

  /// True for the people the membership register is about, as opposed to staff.
  bool get isMember => this == UserRole.RESIDENT || this == UserRole.TENANT;
}

/// Roles a chairman can hire into, in the order the Add Staff sheet lists them.
const List<UserRole> STAFF_ROLES = [
  UserRole.GUARD,
  UserRole.HOUSEKEEPING,
  UserRole.MAINTENANCE_STAFF,
  UserRole.ACCOUNTANT,
  UserRole.MANAGER,
];

/// Parking allotted to a unit.
enum ParkingType {
  COVERED('Covered Parking', 'आच्छादित पार्किंग', 'C'),
  OPEN('Open Parking', 'खुले पार्किंग', 'O'),
  NONE('No Parking', 'पार्किंग नाही', '');

  const ParkingType(this.labelEn, this.labelMr, this.prefix);
  final String labelEn;
  final String labelMr;
  final String prefix;
}

enum FlatType {
  ONE_BHK('1 BHK'),
  TWO_BHK('2 BHK'),
  THREE_BHK('3 BHK'),
  PENTHOUSE('Penthouse'),
  COMMERCIAL_SHOP('Commercial Shop');

  const FlatType(this.displayName);
  final String displayName;
}

enum PaymentMode {
  UPI('UPI', 'यूपीआय'),
  BANK_TRANSFER('Bank Transfer (NEFT/IMPS)', 'बँक ट्रान्सफर'),
  CHEQUE('Cheque', 'धनादेश'),
  CASH('Cash', 'रोख');

  const PaymentMode(this.labelEn, this.labelMr);
  final String labelEn;
  final String labelMr;
}

enum PaymentStatus { PAID, PARTIAL, UNPAID }

enum ExpenseCategory {
  WATER_TANKER('Water Tanker', 'पाण्याचे टँकर'),
  LIFT_AMC('Lift AMC & Repairs', 'लिफ्ट देखभाल (AMC)'),
  SECURITY_SALARY('Security Agency Salary', 'सुरक्षा रक्षक मानधन'),
  GARDENING('Gardening & Landscape', 'बागकाम व सुशोभीकरण'),
  HOUSEKEEPING('Housekeeping & Cleaning', 'स्वच्छता व कचरा व्यवस्थापन'),
  ELECTRICITY('Common Electricity Bill', 'सामायिक वीज बिल'),
  REPAIRS('General Repairs & Painting', 'दुरुस्ती व रंगकाम'),
  MISCELLANEOUS('Miscellaneous / Office', 'इतर / कार्यालयीन खर्च');

  const ExpenseCategory(this.labelEn, this.labelMr);
  final String labelEn;
  final String labelMr;
}

enum RevenueSource {
  CLUBHOUSE_BOOKING('Clubhouse Booking', 'क्लबहाऊस आरक्षण'),
  PARKING_FEE('Extra Parking Charges', 'अतिरिक्त पार्किंग शुल्क'),
  TOWER_RENT('Mobile Tower Rent', 'मोबाईल टॉवर भाडे'),
  SOLAR_CREDIT('Solar Net Metering Credit', 'सौर ऊर्जा क्रेडिट'),
  HALL_RENTAL('Community Hall Rental', 'सभागृह भाडे'),
  PENALTY_FINE('Late Penalty / Rule Fine', 'दंड वसुली');

  const RevenueSource(this.labelEn, this.labelMr);
  final String labelEn;
  final String labelMr;
}

enum VisitorStatus { INSIDE, EXITED, PRE_APPROVED, REJECTED }

enum VisitorPurpose {
  DELIVERY('Delivery (Amazon/Zomato/Blinkit)', 'डिलिव्हरी'),
  GUEST('Family / Guest', 'पाहुणे / नातेवाईक'),
  CAB('Cab / Taxi', 'कॅब / टॅक्सी'),
  SERVICE('Plumber / Electrician / Maid', 'सेवा कर्मचारी / कारागीर'),
  OTHER('Other Official', 'इतर');

  const VisitorPurpose(this.labelEn, this.labelMr);
  final String labelEn;
  final String labelMr;
}

enum NoticeCategory {
  AGM('AGM / General Meeting', 'वार्षिक सर्वसाधारण सभा (AGM)'),
  MAINTENANCE('Maintenance Notice', 'देखभाल शुल्क सूचना'),
  WATER('Water Supply Update', 'पाणीपुरवठा नियोजन'),
  FESTIVAL('Celebration & Events', 'सण व उत्सव'),
  GENERAL('General Advisory', 'सर्वसाधारण सूचना');

  const NoticeCategory(this.labelEn, this.labelMr);
  final String labelEn;
  final String labelMr;
}

enum ComplaintCategory {
  PLUMBING('Plumbing & Drainage', 'प्लंबिंग व सांडपाणी'),
  ELECTRICAL('Electrical & Common Lights', 'विद्युत व दिवे'),
  LIFT('Lift Malfunction', 'लिफ्ट समस्या'),
  LEAKAGE('Water Leakage / Seepage', 'गळती व सिपेज'),
  NOISE('Noise / Parking Dispute', 'आवाज / पार्किंग वाद'),
  SECURITY('Gate / Security Issue', 'सुरक्षा तक्रार'),
  OTHER('Other Issue', 'इतर समस्या');

  const ComplaintCategory(this.labelEn, this.labelMr);
  final String labelEn;
  final String labelMr;
}

enum TicketStatus { OPEN, IN_PROGRESS, RESOLVED }

/// Looks an enum value up by its persisted name, falling back to [fallback].
T enumByName<T extends Enum>(List<T> values, String? raw, T fallback) {
  if (raw == null) return fallback;
  for (final v in values) {
    if (v.name == raw) return v;
  }
  return fallback;
}

/// Same as [enumByName] but null when the value is missing or unknown.
T? enumByNameOrNull<T extends Enum>(List<T> values, String? raw) {
  if (raw == null) return null;
  for (final v in values) {
    if (v.name == raw) return v;
  }
  return null;
}

/// One sellable unit type in a society - "1 BHK", "Shop", "Office" - together
/// with the monthly maintenance tariff charged for it.
class UnitTypeConfig {
  const UnitTypeConfig({required this.name, this.monthlyTariff = 0.0});
  final String name;
  final double monthlyTariff;

  UnitTypeConfig copyWith({String? name, double? monthlyTariff}) =>
      UnitTypeConfig(name: name ?? this.name, monthlyTariff: monthlyTariff ?? this.monthlyTariff);

  @override
  bool operator ==(Object other) =>
      other is UnitTypeConfig && other.name == name && other.monthlyTariff == monthlyTariff;
  @override
  int get hashCode => Object.hash(name, monthlyTariff);
}

/// One unit type a wing holds, and how many units of it there are.
class WingUnitCount {
  const WingUnitCount({required this.unitType, this.quantity = 0});
  final String unitType;
  final int quantity;

  WingUnitCount copyWith({String? unitType, int? quantity}) =>
      WingUnitCount(unitType: unitType ?? this.unitType, quantity: quantity ?? this.quantity);

  @override
  bool operator ==(Object other) =>
      other is WingUnitCount && other.unitType == unitType && other.quantity == quantity;
  @override
  int get hashCode => Object.hash(unitType, quantity);
}

/// Floor inventory captured for a single wing: how many floors it has, and which
/// unit types it holds with how many units of each.
///
/// The unit numbers are spread evenly over the floors, so a wing of 3 floors
/// holding 12 units becomes A-101…A-104, A-201…A-204, A-301…A-304.
class WingPlan {
  const WingPlan({required this.name, this.totalFloors = 0, this.unitCounts = const []});
  final String name;
  final int totalFloors;
  final List<WingUnitCount> unitCounts;

  int get totalUnits => unitCounts.fold(0, (s, c) => s + (c.quantity < 0 ? 0 : c.quantity));

  int get _spreadOverFloors => totalFloors < 1 ? 1 : totalFloors;

  /// Flat numbers this wing expands into, e.g. A-101, A-102, ... A-304.
  List<String> generateUnitNumbers() {
    final total = totalUnits;
    if (total <= 0) return [];
    final floors = _spreadOverFloors;
    // Round up, so the remainder lands on the last floor rather than being lost.
    final perFloor = (total + floors - 1) ~/ floors;
    final numbers = <String>[];
    for (var floor = 1; floor <= floors; floor++) {
      for (var unit = 1; unit <= perFloor; unit++) {
        if (numbers.length >= total) return numbers;
        numbers.add('$name-$floor${unit.toString().padLeft(2, '0')}');
      }
    }
    return numbers;
  }

  /// The unit type of each generated number, same order and length as
  /// [generateUnitNumbers].
  List<String> unitTypesInOrder() => [
        for (final c in unitCounts) ...List.filled(c.quantity < 0 ? 0 : c.quantity, c.unitType),
      ];

  WingPlan copyWith({String? name, int? totalFloors, List<WingUnitCount>? unitCounts}) => WingPlan(
        name: name ?? this.name,
        totalFloors: totalFloors ?? this.totalFloors,
        unitCounts: unitCounts ?? this.unitCounts,
      );
}

/// Society overview & config -> Firestore document `societies/{CODE}`.
class SocietyInfo {
  const SocietyInfo({
    this.docId = 'main',
    this.id = 1,
    required this.name,
    required this.registrationNumber,
    required this.societyCode,
    required this.address,
    required this.totalWings,
    required this.totalFlats,
    required this.bankAccountName,
    required this.bankAccountNumber,
    required this.ifscCode,
    this.upiId = '',
    this.paymentQrBase64 = '',
    required this.currentBalanceCash,
    required this.currentBalanceBank,
    this.lateFeePercentPerMonth = 2.0,
    this.lateFeeGraceDays = 10,
    this.tariff1BHK = 1500.0,
    this.tariff2BHK = 2200.0,
    this.tariff3BHK = 3000.0,
    this.tariffPenthouse = 4500.0,
    this.tariffShop = 3500.0,
    this.waterChargesPercent = 10.0,
    this.sinkingFundPercent = 5.0,
    this.repairFundPercent = 20.0,
    this.otherMaintenancePercent = 65.0,
    this.logoBase64 = '',
    this.country = 'India',
    this.state = '',
    this.city = '',
    this.pinCode = '',
    this.contactNumber = '',
    this.email = '',
    this.wingNames = const [],
    this.wingPlans = const [],
    this.unitTypes = const [],
    this.coveredParkingCount = 0,
    this.openParkingCount = 0,
    this.billingStartMonth = '',
  });

  final String docId;
  final int id;
  final String name;
  final String registrationNumber;
  final String societyCode;
  final String address;
  final int totalWings;
  final int totalFlats;
  final String bankAccountName;
  final String bankAccountNumber;
  final String ifscCode;

  /// The society's UPI id, e.g. "society@okhdfcbank". Every payment QR is
  /// built from this.
  final String upiId;

  /// A payment QR the chairman uploaded, as a Base64 blob. When set it is the
  /// QR members are shown; otherwise one is generated from [upiId].
  final String paymentQrBase64;
  final double currentBalanceCash;
  final double currentBalanceBank;
  final double lateFeePercentPerMonth;
  final int lateFeeGraceDays;
  final double tariff1BHK;
  final double tariff2BHK;
  final double tariff3BHK;
  final double tariffPenthouse;
  final double tariffShop;

  // How one month's maintenance is divided between the society's funds.
  // Shares of the maintenance amount, expected to add up to 100%.
  final double waterChargesPercent;
  final double sinkingFundPercent;
  final double repairFundPercent;
  final double otherMaintenancePercent;

  /// Society logo as a Base64 blob (empty when none uploaded).
  final String logoBase64;
  final String country;
  final String state;
  final String city;
  final String pinCode;
  final String contactNumber;
  final String email;

  /// Wing names as entered in Unit Details, e.g. ["A", "B", "C"].
  final List<String> wingNames;

  /// Per-wing floor inventory captured in the Floor Inventory step.
  final List<WingPlan> wingPlans;

  /// Unit types this society sells/charges for, with their monthly tariff.
  final List<UnitTypeConfig> unitTypes;

  /// How many covered / open parking bays exist. 0 falls back to one per unit.
  final int coveredParkingCount;
  final int openParkingCount;

  /// The month billing starts from, as "April 2026".
  final String billingStartMonth;

  SocietyInfo copyWith({
    String? docId,
    int? id,
    String? name,
    String? registrationNumber,
    String? societyCode,
    String? address,
    int? totalWings,
    int? totalFlats,
    String? bankAccountName,
    String? bankAccountNumber,
    String? ifscCode,
    String? upiId,
    String? paymentQrBase64,
    double? currentBalanceCash,
    double? currentBalanceBank,
    double? lateFeePercentPerMonth,
    int? lateFeeGraceDays,
    double? tariff1BHK,
    double? tariff2BHK,
    double? tariff3BHK,
    double? tariffPenthouse,
    double? tariffShop,
    double? waterChargesPercent,
    double? sinkingFundPercent,
    double? repairFundPercent,
    double? otherMaintenancePercent,
    String? logoBase64,
    String? country,
    String? state,
    String? city,
    String? pinCode,
    String? contactNumber,
    String? email,
    List<String>? wingNames,
    List<WingPlan>? wingPlans,
    List<UnitTypeConfig>? unitTypes,
    int? coveredParkingCount,
    int? openParkingCount,
    String? billingStartMonth,
  }) =>
      SocietyInfo(
        docId: docId ?? this.docId,
        id: id ?? this.id,
        name: name ?? this.name,
        registrationNumber: registrationNumber ?? this.registrationNumber,
        societyCode: societyCode ?? this.societyCode,
        address: address ?? this.address,
        totalWings: totalWings ?? this.totalWings,
        totalFlats: totalFlats ?? this.totalFlats,
        bankAccountName: bankAccountName ?? this.bankAccountName,
        bankAccountNumber: bankAccountNumber ?? this.bankAccountNumber,
        ifscCode: ifscCode ?? this.ifscCode,
        upiId: upiId ?? this.upiId,
        paymentQrBase64: paymentQrBase64 ?? this.paymentQrBase64,
        currentBalanceCash: currentBalanceCash ?? this.currentBalanceCash,
        currentBalanceBank: currentBalanceBank ?? this.currentBalanceBank,
        lateFeePercentPerMonth: lateFeePercentPerMonth ?? this.lateFeePercentPerMonth,
        lateFeeGraceDays: lateFeeGraceDays ?? this.lateFeeGraceDays,
        tariff1BHK: tariff1BHK ?? this.tariff1BHK,
        tariff2BHK: tariff2BHK ?? this.tariff2BHK,
        tariff3BHK: tariff3BHK ?? this.tariff3BHK,
        tariffPenthouse: tariffPenthouse ?? this.tariffPenthouse,
        tariffShop: tariffShop ?? this.tariffShop,
        waterChargesPercent: waterChargesPercent ?? this.waterChargesPercent,
        sinkingFundPercent: sinkingFundPercent ?? this.sinkingFundPercent,
        repairFundPercent: repairFundPercent ?? this.repairFundPercent,
        otherMaintenancePercent: otherMaintenancePercent ?? this.otherMaintenancePercent,
        logoBase64: logoBase64 ?? this.logoBase64,
        country: country ?? this.country,
        state: state ?? this.state,
        city: city ?? this.city,
        pinCode: pinCode ?? this.pinCode,
        contactNumber: contactNumber ?? this.contactNumber,
        email: email ?? this.email,
        wingNames: wingNames ?? this.wingNames,
        wingPlans: wingPlans ?? this.wingPlans,
        unitTypes: unitTypes ?? this.unitTypes,
        coveredParkingCount: coveredParkingCount ?? this.coveredParkingCount,
        openParkingCount: openParkingCount ?? this.openParkingCount,
        billingStartMonth: billingStartMonth ?? this.billingStartMonth,
      );

  /// The four configured shares added together - normally 100.
  double get maintenancePercentTotal =>
      waterChargesPercent + sinkingFundPercent + repairFundPercent + otherMaintenancePercent;

  /// True when the configured shares add up to 100%, within a paisa of rounding.
  bool get hasBalancedMaintenancePercents => (maintenancePercentTotal - 100.0).abs() < 0.01;

  /// Splits a month's maintenance across the society's configured percentages.
  /// Rounding remainder lands on Other/Common Maintenance so the four always add
  /// back up to [totalAmount] exactly.
  MaintenanceSplit splitMaintenance(double totalAmount) {
    if (totalAmount <= 0.0) return const MaintenanceSplit(water: 0, sinking: 0, repair: 0, other: 0);
    final water = roundToPaise(totalAmount * waterChargesPercent / 100.0);
    final sinking = roundToPaise(totalAmount * sinkingFundPercent / 100.0);
    final repair = roundToPaise(totalAmount * repairFundPercent / 100.0);
    return MaintenanceSplit(
      water: water,
      sinking: sinking,
      repair: repair,
      other: roundToPaise(totalAmount - water - sinking - repair),
    );
  }

  /// Every parking number of one type, e.g. C-01 … C-24.
  List<String> parkingNumbers(ParkingType type) {
    final int configured;
    switch (type) {
      case ParkingType.COVERED:
        configured = coveredParkingCount;
      case ParkingType.OPEN:
        configured = openParkingCount;
      case ParkingType.NONE:
        return [];
    }
    final count = configured > 0 ? configured : totalFlats;
    return [for (var i = 1; i <= count; i++) '${type.prefix}-${i.toString().padLeft(2, '0')}'];
  }

  /// The monthly maintenance charged for one unit. The society's own unit type
  /// wins; only when a unit has no type does this fall back to a built-in slab.
  double monthlyTariffFor(Resident? resident) {
    final wantedType = resident?.unitType.trim().toLowerCase();
    for (final type in unitTypes) {
      if (type.name.isNotEmpty && type.name.trim().isNotEmpty && type.name.toLowerCase() == wantedType) {
        if (type.monthlyTariff > 0.0) return type.monthlyTariff;
        break;
      }
    }
    final slabName = resident?.flatType.displayName.replaceAll(' ', '').toLowerCase();
    for (final type in unitTypes) {
      if (type.name.replaceAll(' ', '').toLowerCase() == slabName) {
        if (type.monthlyTariff > 0.0) return type.monthlyTariff;
        break;
      }
    }
    switch (resident?.flatType) {
      case FlatType.ONE_BHK:
        return tariff1BHK;
      case FlatType.THREE_BHK:
        return tariff3BHK;
      case FlatType.PENTHOUSE:
        return tariffPenthouse;
      case FlatType.COMMERCIAL_SHOP:
        return tariffShop;
      default:
        return tariff2BHK;
    }
  }

  /// Every unit type name this society has, for the pickers that offer them.
  List<String> unitTypeNames() {
    final all = [
      for (final plan in wingPlans)
        for (final c in plan.unitCounts) c.unitType,
      for (final t in unitTypes) t.name,
    ];
    final seen = <String>{};
    final out = <String>[];
    for (final raw in all) {
      final name = raw.trim();
      if (name.isEmpty) continue;
      if (seen.add(name.toLowerCase())) out.add(name);
    }
    return out;
  }

  /// The society's balance, worked out from its own ledger: opening balance,
  /// plus what was actually collected and earned, less what was spent.
  TreasuryBalance treasuryBalance(
    List<MaintenanceBill> bills,
    List<ExpenseRecord> expenses,
    List<NonFeeRevenue> revenues, [
    List<Resident> advanceHolders = const [],
  ]) =>
      computeTreasuryBalance(this, bills, expenses, revenues, advanceHolders);
}

/// Null-safe version of [SocietyInfo.treasuryBalance].
TreasuryBalance computeTreasuryBalance(
  SocietyInfo? society,
  List<MaintenanceBill> bills,
  List<ExpenseRecord> expenses,
  List<NonFeeRevenue> revenues, [
  List<Resident> advanceHolders = const [],
]) {
  bool isCash(PaymentMode? m) => m == PaymentMode.CASH;
  double sum(Iterable<double> xs) => xs.fold(0.0, (a, b) => a + b);

  final held = advanceHolders.where((r) => r.advanceBalance > 0.0);
  final cashIn = sum(bills.where((b) => isCash(b.paymentMode)).map((b) => b.amountPaid)) +
      sum(revenues.where((r) => isCash(r.paymentMode)).map((r) => r.amount)) +
      sum(held.where((r) => isCash(r.advanceMode)).map((r) => r.advanceBalance));
  final cashOut = sum(expenses.where((e) => isCash(e.paymentMode)).map((e) => e.amount));
  final bankIn = sum(bills.where((b) => !isCash(b.paymentMode)).map((b) => b.amountPaid)) +
      sum(revenues.where((r) => !isCash(r.paymentMode)).map((r) => r.amount)) +
      sum(held.where((r) => !isCash(r.advanceMode)).map((r) => r.advanceBalance));
  final bankOut = sum(expenses.where((e) => !isCash(e.paymentMode)).map((e) => e.amount));

  return TreasuryBalance(
    cash: roundToPaise((society?.currentBalanceCash ?? 0.0) + cashIn - cashOut),
    bank: roundToPaise((society?.currentBalanceBank ?? 0.0) + bankIn - bankOut),
  );
}

/// Twelve years of months is far more than any society will ever bill for.
const int _maxBillingMonths = 144;

final DateFormat monthYearFormat = DateFormat('MMMM yyyy', 'en_US');

/// Parses "August 2026" style month labels; null when unparseable.
DateTime? parseMonthYear(String raw) {
  try {
    return monthYearFormat.parseLoose(raw.trim());
  } catch (_) {
    return null;
  }
}

/// Every billable month for this society, newest first: from its billing start
/// month up to the current month. Without a start month, the last twelve months.
List<String> billingMonths(SocietyInfo? society) {
  final now = DateTime.now();
  final startRaw = society?.billingStartMonth.trim() ?? '';
  final start = startRaw.isEmpty ? null : parseMonthYear(startRaw);

  if (start == null) {
    return [
      for (var offset = 0; offset >= -11; offset--)
        monthYearFormat.format(DateTime(now.year, now.month + offset, 1)),
    ];
  }

  final months = <String>[];
  var cursor = DateTime(start.year, start.month, 1);
  while (months.length < _maxBillingMonths &&
      (cursor.year < now.year || (cursor.year == now.year && cursor.month <= now.month))) {
    months.add(monthYearFormat.format(cursor));
    cursor = DateTime(cursor.year, cursor.month + 1, 1);
  }
  final reversed = months.reversed.toList();
  return reversed.isEmpty ? [monthYearFormat.format(start)] : reversed;
}

/// How one month's maintenance is split between the society's funds.
class MaintenanceSplit {
  const MaintenanceSplit({required this.water, required this.sinking, required this.repair, required this.other});
  final double water;
  final double sinking;
  final double repair;
  final double other;
  double get total => water + sinking + repair + other;
}

/// What the society actually holds, split between the bank and the cash box.
class TreasuryBalance {
  const TreasuryBalance({required this.cash, required this.bank});
  final double cash;
  final double bank;
  double get total => cash + bank;
}

/// The one rounding rule every maintenance amount goes through.
double roundToPaise(double value) => (value * 100.0).roundToDouble() / 100.0;

/// The largest rent agreement that can be stored (Firestore 1 MiB doc cap).
const int RENT_AGREEMENT_MAX_BYTES = 600 * 1024;

/// Resident / flat member -> `societies/{CODE}/residents/{flatNumber}`.
class Resident {
  const Resident({
    this.docId = '',
    this.id = 0,
    required this.flatNumber,
    required this.wing,
    required this.ownerName,
    this.coApplicantName = '',
    this.tenantName = '',
    this.tenantContact = '',
    this.rentAgreementBase64 = '',
    required this.contactNumber,
    this.email = '',
    this.role = UserRole.RESIDENT,
    this.pin = '',
    this.familyMemberCount = 1,
    this.vehicleTwoWheelerCount = 0,
    this.vehicleFourWheelerCount = 0,
    this.twoWheelerNumber = '',
    this.fourWheelerNumber = '',
    this.parkingSlot = '',
    this.parkingType = ParkingType.NONE,
    this.unitType = '',
    this.flatType = FlatType.TWO_BHK,
    this.squareFeet = 0,
    this.balanceDue = 0.0,
    this.advanceBalance = 0.0,
    this.advanceMode = PaymentMode.BANK_TRANSFER,
    this.isOwnerLiving = true,
    this.photoBase64 = '',
    this.staffDesignation = '',
    this.isActive = true,
  });

  final String docId;
  final int id;
  final String flatNumber;
  final String wing;
  final String ownerName;

  /// The second name on the unit's paperwork - a spouse, a parent, a partner.
  final String coApplicantName;
  final String tenantName;

  /// The tenant's own mobile number, when the flat is let out.
  final String tenantContact;

  /// The rent agreement as a Base64 blob (see [RENT_AGREEMENT_MAX_BYTES]).
  final String rentAgreementBase64;
  final String contactNumber;
  final String email;
  final UserRole role;
  final String pin;
  final int familyMemberCount;
  final int vehicleTwoWheelerCount;
  final int vehicleFourWheelerCount;
  final String twoWheelerNumber;
  final String fourWheelerNumber;
  final String parkingSlot;
  final ParkingType parkingType;

  /// The society's own unit type for this unit - "1 BHK", "Shop", "Office".
  final String unitType;
  final FlatType flatType;
  final int squareFeet;
  final double balanceDue;

  /// Money this unit has paid over and above what it owed, taken off the next bill.
  final double advanceBalance;

  /// Which account the held advance went into.
  final PaymentMode advanceMode;
  final bool isOwnerLiving;

  /// Member photo as a Base64 blob (empty when none uploaded).
  final String photoBase64;

  /// Job title shown on a staff profile ("Night Guard", "Sweeper").
  final String staffDesignation;

  /// A staff member who has left keeps their record but cannot sign in.
  final bool isActive;

  /// Staff sign in with their staff code; residents with their flat number.
  String get loginIdentifier => flatNumber;

  Resident copyWith({
    String? docId,
    int? id,
    String? flatNumber,
    String? wing,
    String? ownerName,
    String? coApplicantName,
    String? tenantName,
    String? tenantContact,
    String? rentAgreementBase64,
    String? contactNumber,
    String? email,
    UserRole? role,
    String? pin,
    int? familyMemberCount,
    int? vehicleTwoWheelerCount,
    int? vehicleFourWheelerCount,
    String? twoWheelerNumber,
    String? fourWheelerNumber,
    String? parkingSlot,
    ParkingType? parkingType,
    String? unitType,
    FlatType? flatType,
    int? squareFeet,
    double? balanceDue,
    double? advanceBalance,
    PaymentMode? advanceMode,
    bool? isOwnerLiving,
    String? photoBase64,
    String? staffDesignation,
    bool? isActive,
  }) =>
      Resident(
        docId: docId ?? this.docId,
        id: id ?? this.id,
        flatNumber: flatNumber ?? this.flatNumber,
        wing: wing ?? this.wing,
        ownerName: ownerName ?? this.ownerName,
        coApplicantName: coApplicantName ?? this.coApplicantName,
        tenantName: tenantName ?? this.tenantName,
        tenantContact: tenantContact ?? this.tenantContact,
        rentAgreementBase64: rentAgreementBase64 ?? this.rentAgreementBase64,
        contactNumber: contactNumber ?? this.contactNumber,
        email: email ?? this.email,
        role: role ?? this.role,
        pin: pin ?? this.pin,
        familyMemberCount: familyMemberCount ?? this.familyMemberCount,
        vehicleTwoWheelerCount: vehicleTwoWheelerCount ?? this.vehicleTwoWheelerCount,
        vehicleFourWheelerCount: vehicleFourWheelerCount ?? this.vehicleFourWheelerCount,
        twoWheelerNumber: twoWheelerNumber ?? this.twoWheelerNumber,
        fourWheelerNumber: fourWheelerNumber ?? this.fourWheelerNumber,
        parkingSlot: parkingSlot ?? this.parkingSlot,
        parkingType: parkingType ?? this.parkingType,
        unitType: unitType ?? this.unitType,
        flatType: flatType ?? this.flatType,
        squareFeet: squareFeet ?? this.squareFeet,
        balanceDue: balanceDue ?? this.balanceDue,
        advanceBalance: advanceBalance ?? this.advanceBalance,
        advanceMode: advanceMode ?? this.advanceMode,
        isOwnerLiving: isOwnerLiving ?? this.isOwnerLiving,
        photoBase64: photoBase64 ?? this.photoBase64,
        staffDesignation: staffDesignation ?? this.staffDesignation,
        isActive: isActive ?? this.isActive,
      );

  /// Applies what someone typed into their own profile form. When [addOnly] is
  /// set (members), details already on file are kept; only blanks are filled.
  Resident withProfileEdits({
    required String ownerName,
    required String coApplicantName,
    required String contactNumber,
    required String email,
    required String photoBase64,
    required bool addOnly,
  }) {
    String settle(String existing, String entered) {
      if (!addOnly) return entered.trim();
      if (existing.trim().isNotEmpty) return existing;
      return entered.trim();
    }

    final settledName = settle(this.ownerName, ownerName);
    return copyWith(
      // A name is the one thing that can never be blanked.
      ownerName: settledName.trim().isEmpty ? this.ownerName : settledName,
      coApplicantName: settle(this.coApplicantName, coApplicantName),
      contactNumber: settle(this.contactNumber, contactNumber),
      email: settle(this.email, email),
      photoBase64: settle(this.photoBase64, photoBase64),
    );
  }

  /// Field-wise equality on what a profile edit can change.
  bool sameProfileAs(Resident other) =>
      ownerName == other.ownerName &&
      coApplicantName == other.coApplicantName &&
      contactNumber == other.contactNumber &&
      email == other.email &&
      photoBase64 == other.photoBase64;
}

/// Maintenance billing & collection -> `societies/{CODE}/maintenance_bills/{id}`.
class MaintenanceBill {
  const MaintenanceBill({
    this.docId = '',
    this.id = 0,
    required this.flatNumber,
    required this.residentName,
    required this.billMonth,
    required this.baseAmount,
    this.waterCharges = 0.0,
    this.sinkingFund = 0.0,
    this.repairFund = 0.0,
    this.parkingCharges = 0.0,
    this.nonOccupancyCharges = 0.0,
    this.lateFeeAmount = 0.0,
    required this.totalAmount,
    this.amountPaid = 0.0,
    this.status = PaymentStatus.UNPAID,
    required this.dueDate,
    this.paidDate = '',
    this.paymentMode,
    this.transactionRef = '',
    this.receiptNumber = '',
    this.receiptImageBase64 = '',
  });

  final String docId;
  final int id;
  final String flatNumber;
  final String residentName;
  final String billMonth; // e.g. "August 2026"
  final double baseAmount;
  final double waterCharges;
  final double sinkingFund;
  final double repairFund;
  final double parkingCharges;
  final double nonOccupancyCharges;
  final double lateFeeAmount;
  final double totalAmount;
  final double amountPaid;
  final PaymentStatus status;
  final String dueDate;
  final String paidDate;
  final PaymentMode? paymentMode;
  final String transactionRef;
  final String receiptNumber;

  /// Scanned payment receipt as a Base64 blob.
  final String receiptImageBase64;

  /// Every charge on the bill except the late fee.
  double get chargesSubtotal =>
      baseAmount + waterCharges + sinkingFund + repairFund + parkingCharges + nonOccupancyCharges;

  /// What the member owes for this bill, late fee included.
  double get payableTotal => chargesSubtotal + lateFeeAmount;

  /// Still outstanding on this bill, never negative on an overpayment.
  double get pendingAmount {
    final p = payableTotal - amountPaid;
    return p < 0 ? 0.0 : p;
  }

  /// The status the numbers actually imply, whatever is stored on the document.
  PaymentStatus get derivedStatus {
    if (amountPaid <= 0.0) return PaymentStatus.UNPAID;
    if (pendingAmount <= 0.009) return PaymentStatus.PAID;
    return PaymentStatus.PARTIAL;
  }

  static const _unset = Object();

  MaintenanceBill copyWith({
    String? docId,
    int? id,
    String? flatNumber,
    String? residentName,
    String? billMonth,
    double? baseAmount,
    double? waterCharges,
    double? sinkingFund,
    double? repairFund,
    double? parkingCharges,
    double? nonOccupancyCharges,
    double? lateFeeAmount,
    double? totalAmount,
    double? amountPaid,
    PaymentStatus? status,
    String? dueDate,
    String? paidDate,
    Object? paymentMode = _unset,
    String? transactionRef,
    String? receiptNumber,
    String? receiptImageBase64,
  }) =>
      MaintenanceBill(
        docId: docId ?? this.docId,
        id: id ?? this.id,
        flatNumber: flatNumber ?? this.flatNumber,
        residentName: residentName ?? this.residentName,
        billMonth: billMonth ?? this.billMonth,
        baseAmount: baseAmount ?? this.baseAmount,
        waterCharges: waterCharges ?? this.waterCharges,
        sinkingFund: sinkingFund ?? this.sinkingFund,
        repairFund: repairFund ?? this.repairFund,
        parkingCharges: parkingCharges ?? this.parkingCharges,
        nonOccupancyCharges: nonOccupancyCharges ?? this.nonOccupancyCharges,
        lateFeeAmount: lateFeeAmount ?? this.lateFeeAmount,
        totalAmount: totalAmount ?? this.totalAmount,
        amountPaid: amountPaid ?? this.amountPaid,
        status: status ?? this.status,
        dueDate: dueDate ?? this.dueDate,
        paidDate: paidDate ?? this.paidDate,
        paymentMode: identical(paymentMode, _unset) ? this.paymentMode : paymentMode as PaymentMode?,
        transactionRef: transactionRef ?? this.transactionRef,
        receiptNumber: receiptNumber ?? this.receiptNumber,
        receiptImageBase64: receiptImageBase64 ?? this.receiptImageBase64,
      );
}

/// Operating expense record -> `expense_records`.
class ExpenseRecord {
  const ExpenseRecord({
    this.docId = '',
    this.id = 0,
    required this.title,
    required this.category,
    required this.amount,
    required this.date,
    required this.vendorName,
    this.vendorContact = '',
    this.paymentMode = PaymentMode.BANK_TRANSFER,
    this.approvedBy = 'Secretary',
    this.notes = '',
    this.receiptNumber = '',
    this.status = PaymentStatus.PAID,
    this.billImageBase64 = '',
  });

  final String docId;
  final int id;
  final String title;
  final ExpenseCategory category;
  final double amount;
  final String date;
  final String vendorName;
  final String vendorContact;
  final PaymentMode paymentMode;
  final String approvedBy;
  final String notes;
  final String receiptNumber;
  final PaymentStatus status;

  /// Scanned vendor bill as a Base64 blob.
  final String billImageBase64;

  ExpenseRecord copyWith({
    String? docId,
    int? id,
    String? title,
    ExpenseCategory? category,
    double? amount,
    String? date,
    String? vendorName,
    String? vendorContact,
    PaymentMode? paymentMode,
    String? approvedBy,
    String? notes,
    String? receiptNumber,
    PaymentStatus? status,
    String? billImageBase64,
  }) =>
      ExpenseRecord(
        docId: docId ?? this.docId,
        id: id ?? this.id,
        title: title ?? this.title,
        category: category ?? this.category,
        amount: amount ?? this.amount,
        date: date ?? this.date,
        vendorName: vendorName ?? this.vendorName,
        vendorContact: vendorContact ?? this.vendorContact,
        paymentMode: paymentMode ?? this.paymentMode,
        approvedBy: approvedBy ?? this.approvedBy,
        notes: notes ?? this.notes,
        receiptNumber: receiptNumber ?? this.receiptNumber,
        status: status ?? this.status,
        billImageBase64: billImageBase64 ?? this.billImageBase64,
      );
}

/// Non-fee revenue -> `non_fee_revenues`.
class NonFeeRevenue {
  const NonFeeRevenue({
    this.docId = '',
    this.id = 0,
    required this.title,
    required this.sourceCategory,
    required this.amount,
    required this.date,
    required this.payerName,
    this.flatNumber = '',
    this.paymentMode = PaymentMode.UPI,
    this.receiptNumber = '',
    this.notes = '',
  });

  final String docId;
  final int id;
  final String title;
  final RevenueSource sourceCategory;
  final double amount;
  final String date;
  final String payerName;
  final String flatNumber;
  final PaymentMode paymentMode;
  final String receiptNumber;
  final String notes;

  NonFeeRevenue copyWith({int? id, String? docId}) => NonFeeRevenue(
        docId: docId ?? this.docId,
        id: id ?? this.id,
        title: title,
        sourceCategory: sourceCategory,
        amount: amount,
        date: date,
        payerName: payerName,
        flatNumber: flatNumber,
        paymentMode: paymentMode,
        receiptNumber: receiptNumber,
        notes: notes,
      );
}

/// Gate security visitor log -> `visitor_logs`.
class VisitorLog {
  const VisitorLog({
    this.docId = '',
    this.id = 0,
    required this.visitorName,
    required this.phone,
    required this.flatNumber,
    required this.purpose,
    this.vehicleNumber = '',
    required this.entryTime,
    this.exitTime = '',
    this.status = VisitorStatus.INSIDE,
    this.passcode = '',
    this.guardName = 'Gate Guard',
    this.photoBase64 = '',
  });

  final String docId;
  final int id;
  final String visitorName;
  final String phone;
  final String flatNumber;
  final VisitorPurpose purpose;
  final String vehicleNumber;
  final String entryTime;
  final String exitTime;
  final VisitorStatus status;
  final String passcode;
  final String guardName;

  /// Visitor snapshot taken at the gate as a Base64 blob.
  final String photoBase64;

  VisitorLog copyWith({int? id, String? docId, VisitorStatus? status, String? exitTime}) => VisitorLog(
        docId: docId ?? this.docId,
        id: id ?? this.id,
        visitorName: visitorName,
        phone: phone,
        flatNumber: flatNumber,
        purpose: purpose,
        vehicleNumber: vehicleNumber,
        entryTime: entryTime,
        exitTime: exitTime ?? this.exitTime,
        status: status ?? this.status,
        passcode: passcode,
        guardName: guardName,
        photoBase64: photoBase64,
      );
}

/// Notice board -> `notices`.
class Notice {
  const Notice({
    this.docId = '',
    this.id = 0,
    required this.title,
    required this.content,
    required this.publishedDate,
    this.isPinned = false,
    this.category = NoticeCategory.GENERAL,
    this.authorRole = 'Managing Committee',
    this.imageBase64 = '',
  });

  final String docId;
  final int id;
  final String title;
  final String content;
  final String publishedDate;
  final bool isPinned;
  final NoticeCategory category;
  final String authorRole;

  /// Circular / poster image as a Base64 blob.
  final String imageBase64;

  Notice copyWith({int? id}) => Notice(
        docId: docId,
        id: id ?? this.id,
        title: title,
        content: content,
        publishedDate: publishedDate,
        isPinned: isPinned,
        category: category,
        authorRole: authorRole,
        imageBase64: imageBase64,
      );
}

/// Complaints & helpdesk tickets -> `complaints`.
class Complaint {
  const Complaint({
    this.docId = '',
    this.id = 0,
    required this.flatNumber,
    required this.residentName,
    required this.category,
    required this.description,
    required this.filedDate,
    this.status = TicketStatus.OPEN,
    this.resolutionNotes = '',
    this.attachmentBase64 = '',
  });

  final String docId;
  final int id;
  final String flatNumber;
  final String residentName;
  final ComplaintCategory category;
  final String description;
  final String filedDate;
  final TicketStatus status;
  final String resolutionNotes;

  /// Photo evidence attached to the ticket as a Base64 blob.
  final String attachmentBase64;

  Complaint copyWith({int? id, ComplaintCategory? category, String? description}) => Complaint(
        docId: docId,
        id: id ?? this.id,
        flatNumber: flatNumber,
        residentName: residentName,
        category: category ?? this.category,
        description: description ?? this.description,
        filedDate: filedDate,
        status: status,
        resolutionNotes: resolutionNotes,
        attachmentBase64: attachmentBase64,
      );
}

/// Emergency contacts -> `emergency_contacts`.
class EmergencyContact {
  const EmergencyContact({
    this.docId = '',
    this.id = 0,
    required this.titleEn,
    required this.titleMr,
    required this.name,
    required this.phoneNumber,
    required this.category,
    this.isActive = true,
  });

  final String docId;
  final int id;
  final String titleEn;
  final String titleMr;
  final String name;
  final String phoneNumber;
  final String category;

  /// Only active contacts are shown to members; inactive ones stay on file.
  final bool isActive;

  EmergencyContact copyWith({
    String? docId,
    int? id,
    String? titleEn,
    String? titleMr,
    String? name,
    String? phoneNumber,
    String? category,
    bool? isActive,
  }) =>
      EmergencyContact(
        docId: docId ?? this.docId,
        id: id ?? this.id,
        titleEn: titleEn ?? this.titleEn,
        titleMr: titleMr ?? this.titleMr,
        name: name ?? this.name,
        phoneNumber: phoneNumber ?? this.phoneNumber,
        category: category ?? this.category,
        isActive: isActive ?? this.isActive,
      );
}

/// How long an emergency number may be (police 100 up to a full mobile).
const int EMERGENCY_PHONE_MIN_DIGITS = 3;
const int EMERGENCY_PHONE_MAX_DIGITS = 15;

/// A society's financial year runs April to March.
String financialYearOf(String billMonth) {
  final parsed = parseMonthYear(billMonth);
  if (parsed == null) return '';
  final startYear = parsed.month <= 3 ? parsed.year - 1 : parsed.year;
  return 'FY $startYear-${startYear + 1}';
}

/// Every financial year the society has billed in, newest first.
List<String> financialYears(SocietyInfo? society) {
  final out = <String>[];
  for (final m in billingMonths(society)) {
    final fy = financialYearOf(m);
    if (fy.isNotEmpty && !out.contains(fy)) out.add(fy);
  }
  return out;
}
