import 'dart:async';

import 'package:flutter/foundation.dart';

import '../data/firebase_module.dart';
import '../data/models.dart';
import '../data/payment_allocation.dart';
import '../data/session_store.dart';
import '../data/society_repository.dart';
import '../l10n/app_strings.dart';
import '../util/format.dart';

enum NavDestination { DASHBOARD, ACCOUNTS, MAINTENANCE, RESIDENTS, GATE, COMMUNITY, RESIDENT_PORTAL }

/// What each role is allowed to open, in the order the bottom bar shows it.
///
/// This is the single place menu visibility is decided:
///  - Residents is the Chairman's alone.
///  - Security belongs to the security team and nobody else.
///  - Every other staff role sees only the screens their job needs.
List<NavDestination> destinationsFor(UserRole role) {
  switch (role) {
    case UserRole.CHAIRMAN:
      return const [
        NavDestination.DASHBOARD,
        NavDestination.ACCOUNTS,
        NavDestination.MAINTENANCE,
        NavDestination.RESIDENTS,
        NavDestination.COMMUNITY,
      ];
    case UserRole.SECRETARY:
    case UserRole.TREASURER:
    case UserRole.COMMITTEE:
      return const [
        NavDestination.DASHBOARD,
        NavDestination.ACCOUNTS,
        NavDestination.MAINTENANCE,
        NavDestination.COMMUNITY,
      ];
    // My Unit leads, because it is what a member opens the app for. Members can
    // open Accounts, but only to look at the society's balance.
    case UserRole.RESIDENT:
    case UserRole.TENANT:
      return const [
        NavDestination.RESIDENT_PORTAL,
        NavDestination.DASHBOARD,
        NavDestination.ACCOUNTS,
        NavDestination.COMMUNITY,
      ];
    // Security team: the gate terminal, plus notices and complaints.
    case UserRole.GUARD:
      return const [NavDestination.GATE, NavDestination.COMMUNITY];
    case UserRole.ACCOUNTANT:
    case UserRole.MANAGER:
      return const [
        NavDestination.DASHBOARD,
        NavDestination.ACCOUNTS,
        NavDestination.MAINTENANCE,
        NavDestination.COMMUNITY,
      ];
    // Housekeeping and maintenance crew only need what they are told to do.
    case UserRole.HOUSEKEEPING:
    case UserRole.MAINTENANCE_STAFF:
      return const [NavDestination.COMMUNITY];
  }
}

/// What one signed-in member may open. A society staff role decides by its
/// own screen list; everyone else (or a staff role since deleted) falls back to
/// their built-in role.
List<NavDestination> destinationsForMember(Resident member, List<SocietyRole> roles) {
  final custom = customRoleOf(member, roles);
  if (custom == null) return destinationsFor(member.role);
  return [
    for (final key in normaliseRoleMenus(custom.menus))
      for (final d in NavDestination.values)
        if (d.name == key) d,
  ];
}

/// The office bearers who run the society, as opposed to living in it.
const _committeeRoles = {
  UserRole.CHAIRMAN,
  UserRole.SECRETARY,
  UserRole.TREASURER,
  UserRole.COMMITTEE,
  UserRole.MANAGER,
};

class FinancialOverview {
  const FinancialOverview({
    this.totalMaintenanceCollected = 0.0,
    this.totalNonFeeRevenue = 0.0,
    this.totalGrossIncome = 0.0,
    this.totalOperatingExpenses = 0.0,
    this.netSurplus = 0.0,
    this.netTreasuryBalance = 0.0,
    this.cashBalance = 0.0,
    this.bankBalance = 0.0,
    this.totalOutstandingDues = 0.0,
    this.paidFlatsCount = 0,
    this.pendingFlatsCount = 0,
  });

  final double totalMaintenanceCollected;
  final double totalNonFeeRevenue;
  final double totalGrossIncome;
  final double totalOperatingExpenses;
  final double netSurplus;
  final double netTreasuryBalance;
  final double cashBalance;
  final double bankBalance;
  final double totalOutstandingDues;
  final int paidFlatsCount;
  final int pendingFlatsCount;
}

/// Everything the 5-step registration wizard collects, handed over in one go.
class SocietyRegistrationRequest {
  const SocietyRegistrationRequest({
    // 1. Society Details
    required this.name,
    this.regNumber = '',
    required this.code,
    this.country = 'India',
    this.state = '',
    this.city = '',
    this.address = '',
    this.pinCode = '',
    this.contactNumber = '',
    this.email = '',
    this.logoBase64 = '',
    this.bankName = '',
    this.bankAccount = '',
    this.ifsc = '',
    this.upiId = '',
    this.paymentQrBase64 = '',
    this.initialBankBalance = 0.0,
    this.billingStartMonth = '',
    // 2. Unit Details / 3. Floor Inventory
    this.wingNames = const [],
    this.wingPlans = const [],
    this.unitTypes = const [],
    // 4. Office Bearers
    required this.chairmanName,
    required this.chairmanPhone,
    this.chairmanEmail = '',
    required this.chairmanFlat,
    required this.chairmanPin,
  });

  final String name;
  final String regNumber;
  final String code;
  final String country;
  final String state;
  final String city;
  final String address;
  final String pinCode;
  final String contactNumber;
  final String email;
  final String logoBase64;
  final String bankName;
  final String bankAccount;
  final String ifsc;
  final String upiId;

  /// The society's own payment QR image, when its bank issues one.
  final String paymentQrBase64;
  final double initialBankBalance;

  /// Month billing starts from, as "April 2026".
  final String billingStartMonth;
  final List<String> wingNames;
  final List<WingPlan> wingPlans;
  final List<UnitTypeConfig> unitTypes;
  final String chairmanName;
  final String chairmanPhone;
  final String chairmanEmail;
  final String chairmanFlat;
  final String chairmanPin;

  /// Monthly tariff configured for a unit type, matched loosely so "1BHK",
  /// "1 BHK" and "1 bhk" all land on the same slab.
  double tariffFor(String unitTypeName, double fallback) => _tariffOf(unitTypes, unitTypeName, fallback);
}

/// A society that was just created, waiting on the chairman's confirmation.
class PendingRegistration {
  const PendingRegistration({required this.society, required this.chairman});
  final SocietyInfo society;
  final Resident chairman;
}

/// One unit-and-month that has no bill against it yet.
class MissingBill {
  const MissingBill(this.unit, this.month);
  final Resident unit;
  final String month;
}

/// What deleting a wing would take with it, so the chairman sees it first.
class WingContents {
  const WingContents({required this.units, required this.occupied, required this.bills});
  final int units;
  final int occupied;
  final int bills;
}

typedef ResultCallback = void Function(bool ok, String message);

void _noop(bool ok, String message) {}

double _tariffOf(List<UnitTypeConfig> types, String name, double fallback) {
  final wanted = name.toLowerCase().replaceAll(' ', '');
  for (final t in types) {
    if (t.name.toLowerCase().replaceAll(' ', '') == wanted) {
      return t.monthlyTariff > 0.0 ? t.monthlyTariff : fallback;
    }
  }
  return fallback;
}

bool _eqIgnoreCase(String? a, String? b) => a != null && b != null && a.toLowerCase() == b.toLowerCase();

String _last10(String raw) {
  final d = raw.replaceAll(RegExp(r'[^0-9]'), '');
  return d.length <= 10 ? d : d.substring(d.length - 10);
}

bool _isFourDigitPin(String pin) => pin.length == 4 && RegExp(r'^[0-9]{4}$').hasMatch(pin);

/// Port of the Kotlin `SocietyViewModel`. Every StateFlow became a getter and
/// every change calls [notifyListeners]; widgets read it via Provider.
class SocietyViewModel extends ChangeNotifier {
  SocietyViewModel({SocietyRepository? repository, SessionStore? sessionStore})
      : _repository = repository ?? SocietyRepository(),
        _sessionStore = sessionStore ?? SessionStore() {
    // Give the Firestore SDK an identity before the first read.
    FirebaseModule.ensureSignedIn();
    _activeSub = _repository.activeSocietyChanges.listen(_onActiveSocietyChanged);
    _restoreSession();
  }

  static const _tag = 'SocietyViewModel';

  /// Marks a bill line settled from held credit rather than a fresh payment.
  static const advanceAdjustmentRef = 'ADVANCE-ADJUSTMENT';

  /// Where the "new society registered" notice comes from and goes to.
  static const _registrationEmailFrom = 'r@multisarv.com';
  static const _registrationEmailTo = 'info@multisarv.com';

  final SocietyRepository _repository;
  final SessionStore _sessionStore;
  StreamSubscription<String?>? _activeSub;
  final List<StreamSubscription<dynamic>> _dataSubs = [];
  bool _disposed = false;

  // ---------------------------------------------------------------- state

  AppLanguage _language = AppLanguage.english;
  AppLanguage get language => _language;

  Resident? _currentUser;
  Resident? get currentUser => _currentUser;

  /// True while a remembered session is being checked, before the login page shows.
  bool _isRestoringSession = false;
  bool get isRestoringSession => _isRestoringSession;

  /// The unit the member is currently looking at (members owning several units).
  String? _selectedUnitNumber;
  String? get selectedUnitNumber => _selectedUnitNumber;

  NavDestination _selectedTab = NavDestination.DASHBOARD;
  NavDestination get selectedTab => _selectedTab;

  String? _userMessage;
  String? get userMessage => _userMessage;

  /// Set between "society registered" and the chairman acknowledging it.
  PendingRegistration? _pendingRegistration;
  PendingRegistration? get pendingRegistration => _pendingRegistration;

  SocietyInfo? _societyInfo;
  SocietyInfo? get societyInfo => _societyInfo;

  List<Resident> _residents = const [];
  List<Resident> get residents => _residents;

  List<MaintenanceBill> _bills = const [];
  List<MaintenanceBill> get bills => _bills;

  List<ExpenseRecord> _expenses = const [];
  List<ExpenseRecord> get expenses => _expenses;

  List<NonFeeRevenue> _revenues = const [];
  List<NonFeeRevenue> get revenues => _revenues;

  List<VisitorLog> _visitors = const [];
  List<VisitorLog> get visitors => _visitors;

  /// Visitors currently inside the gate (same as the Kotlin status==INSIDE query).
  List<VisitorLog> get activeVisitors => _visitors.where((v) => v.status == VisitorStatus.INSIDE).toList();

  List<Notice> _notices = const [];
  List<Notice> get notices => _notices;

  List<Complaint> _complaints = const [];
  List<Complaint> get complaints => _complaints;

  List<EmergencyContact> _emergencyContacts = const [];
  List<EmergencyContact> get emergencyContacts => _emergencyContacts;

  List<SocietyRole> _roles = const [];

  /// The society's own staff roles.
  List<SocietyRole> get roles => _roles;

  List<RoleChange> _roleHistory = const [];
  List<RoleChange> get roleHistory => _roleHistory;

  List<SubscriptionPlan> _plans = const [];

  /// Plans MultiSarv currently offers, in display order.
  List<SubscriptionPlan> get subscriptionPlans => offeredPlans(_plans);

  SocietySubscription? _subscription;
  SocietySubscription? get subscription => _subscription;
  SubscriptionStatus get subscriptionStatus => subscriptionStatusOf(_subscription, DateTime.now());

  List<SubscriptionRequest> _subscriptionRequests = const [];
  List<SubscriptionRequest> get subscriptionRequests => _subscriptionRequests;

  /// The request still waiting on MultiSarv, if there is one.
  SubscriptionRequest? get pendingSubscriptionRequest {
    for (final r in _subscriptionRequests) {
      if (r.status == SubscriptionRequestStatus.PENDING) return r;
    }
    return null;
  }

  // ------------------------------------------------------- UI progress state

  /// Operations started through [_safeLaunch] that have not finished yet.
  /// Drives button spinners and the progress bar under the top bar.
  int _pendingOps = 0;
  bool get isBusy => _pendingOps > 0;

  /// Data streams that have not delivered their first snapshot yet.
  int _awaitingFirstData = 0;

  /// True from the moment a society is opened until every stream has
  /// delivered once - the screens show loading skeletons meanwhile.
  bool _isInitialLoading = false;
  bool get isInitialLoading => _isInitialLoading;
  String? _activeCode;
  Completer<void>? _dataReady;

  // ------------------------------------------------------- subscriptions

  void _onActiveSocietyChanged(String? code) {
    for (final s in _dataSubs) {
      s.cancel();
    }
    _dataSubs.clear();
    // Emit empty lists while switching, so a previous society's rows can never linger.
    _societyInfo = null;
    _residents = const [];
    _bills = const [];
    _expenses = const [];
    _revenues = const [];
    _visitors = const [];
    _notices = const [];
    _complaints = const [];
    _emergencyContacts = const [];
    _roles = const [];
    _roleHistory = const [];
    _plans = const [];
    _subscription = null;
    _subscriptionRequests = const [];
    _activeCode = code;
    _isInitialLoading = code != null;
    _notify();
    if (code == null) return;
    _subscribeAll(code);
  }

  /// Pull-to-refresh: listens to the society's collections afresh (keeping
  /// what is on screen meanwhile) and completes once every one has answered.
  Future<void> refresh() async {
    final code = _activeCode;
    if (code == null) return;
    for (final s in _dataSubs) {
      s.cancel();
    }
    _dataSubs.clear();
    _subscribeAll(code);
    await (_dataReady?.future ?? Future<void>.value()).timeout(const Duration(seconds: 10), onTimeout: () {});
  }

  void _subscribeAll(String code) {
    _awaitingFirstData = 0;
    final ready = Completer<void>();
    _dataReady = ready;

    void sub<T>(Stream<T> stream, void Function(T) onData) {
      var first = true;
      _awaitingFirstData++;
      void arrived() {
        if (!first) return;
        first = false;
        _awaitingFirstData--;
        if (_awaitingFirstData <= 0) {
          _isInitialLoading = false;
          if (!ready.isCompleted) ready.complete();
        }
      }

      _dataSubs.add(stream.listen((value) {
        onData(value);
        arrived();
        _notify();
      }, onError: (Object e) {
        debugPrint('$_tag: stream error $e');
        arrived();
        _notify();
      }));
    }

    sub<SocietyInfo?>(_repository.watchSociety(code), (v) => _societyInfo = v);
    sub<List<Resident>>(_repository.watchResidents(code), (v) {
      _residents = v;
      _syncCurrentUser();
    });
    sub<List<MaintenanceBill>>(_repository.watchBills(code), (v) => _bills = v);
    sub<List<ExpenseRecord>>(_repository.watchExpenses(code), (v) => _expenses = v);
    sub<List<NonFeeRevenue>>(_repository.watchRevenues(code), (v) => _revenues = v);
    sub<List<VisitorLog>>(_repository.watchVisitors(code), (v) => _visitors = v);
    sub<List<Notice>>(_repository.watchNotices(code), (v) => _notices = v);
    sub<List<Complaint>>(_repository.watchComplaints(code), (v) => _complaints = v);
    sub<List<EmergencyContact>>(_repository.watchEmergencyContacts(code), (v) => _emergencyContacts = v);
    sub<List<SocietyRole>>(_repository.watchRoles(code), (v) {
      _roles = v;
      _syncCurrentUser();
    });
    sub<List<RoleChange>>(_repository.watchRoleHistory(code), (v) => _roleHistory = v);
    sub<List<SubscriptionPlan>>(_repository.watchSubscriptionPlans(), (v) => _plans = v);
    sub<SocietySubscription?>(_repository.watchSubscription(code), (v) => _subscription = v);
    sub<List<SubscriptionRequest>>(_repository.watchSubscriptionRequests(code), (v) => _subscriptionRequests = v);
  }

  /// Keeps the signed-in member in step with their record, so a role changed
  /// by the Chairman or from MultiSarv Core takes effect at once: the bottom
  /// bar, the landing screen and every permission check follow it.
  void _syncCurrentUser() {
    final user = _currentUser;
    if (user == null) return;
    Resident? fresh;
    for (final r in _residents) {
      if (r.docId == user.docId) {
        fresh = r;
        break;
      }
    }
    if (fresh == null) return;
    if (fresh.role.isStaff && !fresh.isActive) {
      logout();
      showMessage(
        msg(
          'हे कर्मचारी खाते बंद केले आहे. अध्यक्षांशी संपर्क साधा.',
          'यह स्टाफ खाता निष्क्रिय है। अध्यक्ष से संपर्क करें।',
          'This staff account has been deactivated. Contact your chairman.',
        ),
      );
      return;
    }
    _currentUser = fresh;
    if (!canOpen(_selectedTab)) _selectedTab = _landingTabFor(fresh);
  }

  void _notify() {
    if (!_disposed) notifyListeners();
  }

  @override
  void dispose() {
    _disposed = true;
    _activeSub?.cancel();
    for (final s in _dataSubs) {
      s.cancel();
    }
    super.dispose();
  }

  /// Every Firestore call can fail - rejected by the rules, offline, over
  /// quota. Surface it to the user instead of crashing.
  Future<void> _safeLaunch(Future<void> Function() block) async {
    _pendingOps++;
    _notify();
    try {
      await block();
    } catch (e, st) {
      debugPrint('$_tag: operation failed: $e\n$st');
      _userMessage = msg(
        'क्रिया अयशस्वी. इंटरनेट तपासा आणि पुन्हा प्रयत्न करा.',
        'कार्रवाई विफल। इंटरनेट जांचें और पुनः प्रयास करें।',
        'That did not go through. Check your connection and try again.',
      );
      _notify();
    } finally {
      _pendingOps--;
      _notify();
    }
  }

  // ------------------------------------------------------- derived state

  /// Every unit the signed-in member holds. A member who bought two flats is
  /// registered against both under the same mobile number, so the mobile number
  /// identifies the person. Their own unit is always first in the list.
  List<Resident> get myUnits {
    final user = _currentUser;
    if (user == null) return const [];
    final digits = _last10(user.contactNumber);
    final owned = _residents.where((unit) {
      if (unit.role.isStaff) return false;
      return _eqIgnoreCase(unit.flatNumber, user.flatNumber) ||
          (digits.length == 10 && _last10(unit.contactNumber) == digits);
    }).toList();
    final mine = owned.isEmpty ? [user] : owned;
    // Stable sort: own unit first, the rest in their existing order.
    final own = mine.where((u) => _eqIgnoreCase(u.flatNumber, user.flatNumber));
    final others = mine.where((u) => !_eqIgnoreCase(u.flatNumber, user.flatNumber));
    return [...own, ...others];
  }

  /// The complaints the signed-in user is allowed to see. A member sees only
  /// tickets raised from their own units; committee and staff see them all.
  List<Complaint> get visibleComplaints {
    final user = _currentUser;
    final role = user?.role;
    if (role == null) return const [];
    if (role != UserRole.RESIDENT && role != UserRole.TENANT) return _complaints;
    var mine = myUnits.map((u) => u.flatNumber.toUpperCase()).toList();
    if (mine.isEmpty) mine = [user!.flatNumber.toUpperCase()];
    return _complaints.where((c) => mine.contains(c.flatNumber.toUpperCase())).toList();
  }

  /// True when the member holds more than one unit and needs a unit picker.
  bool get hasMultipleUnits => myUnits.length > 1;

  /// The unit every "my …" screen is scoped to.
  Resident? get activeUnit {
    final units = myUnits;
    final user = _currentUser;
    for (final u in units) {
      if (_eqIgnoreCase(u.flatNumber, _selectedUnitNumber)) return u;
    }
    for (final u in units) {
      if (_eqIgnoreCase(u.flatNumber, user?.flatNumber)) return u;
    }
    return units.isNotEmpty ? units.first : user;
  }

  void selectUnit(String flatNumber) {
    _selectedUnitNumber = flatNumber;
    _notify();
  }

  /// Financial KPI summary. Every figure comes off the bill's own line items
  /// rather than a stored total, so editing a bill moves the dashboard with it.
  FinancialOverview get financialOverview {
    double sum(Iterable<double> xs) => xs.fold(0.0, (a, b) => a + b);
    final maintenanceCollected = sum(_bills.map((b) => b.amountPaid));
    final nonFeeTotal = sum(_revenues.map((r) => r.amount));
    final grossIncome = maintenanceCollected + nonFeeTotal;
    final totalExpenses = sum(_expenses.map((e) => e.amount));
    final treasury = computeTreasuryBalance(_societyInfo, _bills, _expenses, _revenues, _residents);
    return FinancialOverview(
      totalMaintenanceCollected: maintenanceCollected,
      totalNonFeeRevenue: nonFeeTotal,
      totalGrossIncome: grossIncome,
      totalOperatingExpenses: totalExpenses,
      netSurplus: grossIncome - totalExpenses,
      netTreasuryBalance: treasury.cash + treasury.bank,
      cashBalance: treasury.cash,
      bankBalance: treasury.bank,
      totalOutstandingDues: sum(_bills.map((b) => b.pendingAmount)),
      paidFlatsCount: _bills.where((b) => b.derivedStatus == PaymentStatus.PAID).length,
      pendingFlatsCount: _bills.where((b) => b.derivedStatus != PaymentStatus.PAID).length,
    );
  }

  /// Members living in the society, staff excluded.
  List<Resident> get members => _residents.where((r) => !r.role.isStaff).toList();

  /// Everyone on the society payroll.
  List<Resident> get staff => _residents.where((r) => r.role.isStaff).toList();

  /// True when the signed-in member is the one allowed to edit society details.
  bool get canEditSociety => _currentUser?.role == UserRole.CHAIRMAN;

  /// Creating staff roles and changing anyone's role is the Chairman's alone.
  bool get canManageRoles => _currentUser?.role == UserRole.CHAIRMAN;

  /// The committee (and the manager running the office) can see the plan.
  bool get canViewSubscription => _committeeRoles.contains(_currentUser?.role);

  /// Only the Chairman asks MultiSarv for a plan on the society's behalf.
  bool get canRequestSubscription => _currentUser?.role == UserRole.CHAIRMAN;

  /// Screens the signed-in member may open, in bottom-bar order.
  List<NavDestination> get destinations {
    final user = _currentUser;
    return user == null ? const [] : destinationsForMember(user, _roles);
  }

  /// The role name to show for [member]: their society role if they hold one.
  SocietyRole? customRoleFor(Resident member) => customRoleOf(member, _roles);

  /// Who may record expenses and revenue: the Chairman and the Treasurer.
  bool get canEditAccounts =>
      _currentUser?.role == UserRole.CHAIRMAN || _currentUser?.role == UserRole.TREASURER;

  // ------------------------------------------------------------- general

  void toggleLanguage() {
    switch (_language) {
      case AppLanguage.english:
        _language = AppLanguage.hindi;
      case AppLanguage.hindi:
        _language = AppLanguage.marathi;
      case AppLanguage.marathi:
        _language = AppLanguage.english;
    }
    _notify();
  }

  void setLanguage(AppLanguage lang) {
    _language = lang;
    _notify();
  }

  void selectTab(NavDestination tab) {
    if (!canOpen(tab)) return;
    _selectedTab = tab;
    _notify();
  }

  void clearUserMessage() {
    _userMessage = null;
  }

  void showMessage(String message) {
    _userMessage = message;
    _notify();
  }

  /// Picks the message for the current language.
  String msg(String mr, String hi, String en) {
    switch (_language) {
      case AppLanguage.marathi:
        return mr;
      case AppLanguage.hindi:
        return hi;
      case AppLanguage.english:
        return en;
    }
  }

  // ------------------------------------------------------- authentication

  /// Signs a member in against the `residents` collection in Firestore. The
  /// identifier is looked up as a flat number first, then as a mobile number.
  void login(String societyCode, String flatOrMobile, String pin,
      {bool rememberMe = true, required ResultCallback onResult}) {
    _safeLaunch(() async {
      final code = societyCode.trim();
      final identifier = flatOrMobile.trim();
      final enteredPin = pin.trim();

      if (code.isEmpty || identifier.isEmpty || enteredPin.isEmpty) {
        onResult(false, msg('कृपया सर्व माहिती भरा.', 'कृपया सभी जानकारी भरें।', 'Please fill in every field.'));
        return;
      }

      final society = await _repository.findSocietyByCode(code);
      if (society == null) {
        onResult(
            false,
            msg(
              "हा सोसायटी कोड सापडला नाही. कोड तपासा किंवा 'नवीन सोसायटी नोंदणी' करा.",
              "यह सोसायटी कोड नहीं मिला। कोड जांचें या 'नई सोसायटी पंजीकरण' करें।",
              "No society found with that code. Check the code, or use 'Register New Society'.",
            ));
        return;
      }

      // Scope every following read and write to this society.
      _repository.setActiveSociety(society.societyCode);

      final resident = await _repository.getResidentByFlat(identifier, societyCode: society.societyCode) ??
          await _repository.getResidentByMobile(identifier, societyCode: society.societyCode);

      if (resident == null) {
        _repository.clearActiveSociety();
        onResult(
            false,
            msg(
              'हा फ्लॅट किंवा मोबाईल क्रमांक ${society.name} मध्ये नोंदणीकृत नाही!',
              'यह फ्लैट या मोबाइल नंबर ${society.name} में पंजीकृत नहीं है!',
              'That unit or mobile number is not registered in ${society.name}.',
            ));
        return;
      }

      // A staff member who has been deactivated keeps their record but is not
      // allowed back in.
      if (resident.role.isStaff && !resident.isActive) {
        _repository.clearActiveSociety();
        onResult(
            false,
            msg(
              'हे कर्मचारी खाते बंद केले आहे. अध्यक्षांशी संपर्क साधा.',
              'यह स्टाफ खाता निष्क्रिय है। अध्यक्ष से संपर्क करें।',
              'This staff account has been deactivated. Contact your chairman.',
            ));
        return;
      }

      if (resident.pin.trim().isEmpty) {
        _repository.clearActiveSociety();
        onResult(
            false,
            msg(
              'या सभासदासाठी पिन सेट केलेला नाही. समितीकडून पिन सेट करून घ्या.',
              'इस सदस्य के लिए पिन सेट नहीं है। समिति से पिन सेट करवाएं।',
              'No PIN is set for this member yet. Ask the committee to set one.',
            ));
        return;
      }

      if (resident.pin != enteredPin) {
        _repository.clearActiveSociety();
        onResult(false, msg('चुकीचा पिन!', 'गलत पिन!', 'Incorrect PIN.'));
        return;
      }

      // Remember Me: the code and the identifier only, never the PIN.
      if (rememberMe) {
        await _sessionStore.save(society.societyCode, identifier);
      } else {
        await _sessionStore.clear();
      }

      _selectedUnitNumber = null;
      _currentUser = resident;
      _selectedTab = _landingTabFor(resident);
      _notify();
      onResult(true, msg('यशस्वी प्रवेश!', 'सफल लॉगिन!', 'Login successful!'));
    });
  }

  /// Signs a remembered member back in when the app is reopened. The member is
  /// re-read from Firestore every time rather than trusted from the device.
  Future<void> _restoreSession() async {
    final saved = await _sessionStore.read();
    if (saved == null) return;
    _isRestoringSession = true;
    _notify();
    try {
      await _safeLaunch(() async {
        final society = await _repository.findSocietyByCode(saved.societyCode);
        if (society == null) {
          await _sessionStore.clear();
          return;
        }
        _repository.setActiveSociety(society.societyCode);

        final resident = await _repository.getResidentByFlat(saved.identifier, societyCode: society.societyCode) ??
            await _repository.getResidentByMobile(saved.identifier, societyCode: society.societyCode);

        if (resident == null || resident.pin.trim().isEmpty || (resident.role.isStaff && !resident.isActive)) {
          _repository.clearActiveSociety();
          await _sessionStore.clear();
          return;
        }

        _currentUser = resident;
        _selectedTab = _landingTabFor(resident);
      });
    } finally {
      _isRestoringSession = false;
      _notify();
    }
  }

  /// Where a role lands after signing in - always its first allowed screen.
  NavDestination _landingTabFor(Resident member) {
    final role = member.role;
    if (role == UserRole.RESIDENT || role == UserRole.TENANT) return NavDestination.RESIDENT_PORTAL;
    final list = destinationsForMember(member, _roles);
    return list.isNotEmpty ? list.first : NavDestination.COMMUNITY;
  }

  /// Guards tab switches so a role can never land on a screen it cannot open.
  bool canOpen(NavDestination destination) {
    final role = _currentUser?.role;
    if (role == null) return false;
    if (destinations.contains(destination)) return true;
    // My Unit is every member's own, and the gate terminal is reachable from
    // the committee's dashboard even though it is not a tab of theirs.
    return destination == NavDestination.RESIDENT_PORTAL ||
        (destination == NavDestination.GATE && _committeeRoles.contains(role));
  }

  void logout() {
    _repository.clearActiveSociety();
    _sessionStore.clear();
    _selectedUnitNumber = null;
    _currentUser = null;
    _selectedTab = NavDestination.DASHBOARD;
    _notify();
  }

  // ---------------------------------------- Forgot PIN (OTP on the mobile)

  /// Step one of a PIN reset: confirm the mobile number really is registered
  /// against a flat in that society, before any OTP is spent on it.
  void findMemberForPinReset(String societyCode, String mobile,
      {required void Function(Resident? resident, String codeOrMessage) onResult}) {
    _safeLaunch(() async {
      final code = societyCode.trim();
      final number = mobile.trim();
      if (code.isEmpty || number.length < 10) {
        onResult(
            null,
            msg(
              'सोसायटी कोड व नोंदणीकृत १० अंकी मोबाईल क्रमांक टाका.',
              'सोसायटी कोड और पंजीकृत 10 अंकों का मोबाइल नंबर डालें।',
              'Enter the society code and the registered 10-digit mobile number.',
            ));
        return;
      }

      final society = await _repository.findSocietyByCode(code);
      if (society == null) {
        onResult(null, msg('हा सोसायटी कोड सापडला नाही.', 'यह सोसायटी कोड नहीं मिला।', 'No society found with that code.'));
        return;
      }

      final resident = await _repository.getResidentByMobile(number, societyCode: society.societyCode);
      if (resident == null) {
        onResult(
            null,
            msg(
              'हा मोबाईल क्रमांक ${society.name} मध्ये नोंदणीकृत नाही. अध्यक्षांशी संपर्क साधा.',
              'यह मोबाइल नंबर ${society.name} में पंजीकृत नहीं है। अध्यक्ष से संपर्क करें।',
              'That mobile number is not registered in ${society.name}. Contact your chairman.',
            ));
        return;
      }

      onResult(resident, society.societyCode);
    });
  }

  /// Step two: the OTP is already verified, so write the new 4-digit PIN.
  void resetPinAfterOtp(String societyCode, Resident resident, String newPin, {required ResultCallback onResult}) {
    _safeLaunch(() async {
      final pin = newPin.trim();
      if (!_isFourDigitPin(pin)) {
        onResult(false, msg('पिन नेमका ४ अंकी असावा.', 'पिन ठीक 4 अंकों का होना चाहिए।', 'The PIN must be exactly 4 digits.'));
        return;
      }
      try {
        await _repository.resetResidentPin(societyCode, resident, pin);
        onResult(
            true,
            msg(
              'नवीन पिन सेट झाला! आता ${resident.flatNumber} व नवीन पिनने लॉगिन करा.',
              'नया पिन सेट हो गया! अब ${resident.flatNumber} और नए पिन से लॉगिन करें।',
              'New PIN set. Log in with ${resident.flatNumber} and your new PIN.',
            ));
      } catch (e) {
        debugPrint('$_tag: PIN reset failed: $e');
        onResult(false, msg('पिन बदलता आला नाही. पुन्हा प्रयत्न करा.', 'पिन नहीं बदल सका। दोबारा प्रयास करें।', 'Could not change the PIN. Please try again.'));
      }
    });
  }

  // ------------------------------------------------ Society registration

  /// Creates the society master document and its first member (the chairman)
  /// in Firestore. The chairman is signed in by [completeRegistrationSignIn]
  /// once the wizard's confirmation step has been acknowledged.
  void registerNewSociety(SocietyRegistrationRequest request, {ResultCallback onResult = _noop}) {
    _safeLaunch(() async {
      final societyCode = request.code.trim().toUpperCase();
      final flatNumber = request.chairmanFlat.trim().toUpperCase();
      final pin = request.chairmanPin.trim();

      if (request.name.trim().isEmpty ||
          societyCode.isEmpty ||
          request.chairmanName.trim().isEmpty ||
          flatNumber.isEmpty ||
          request.chairmanPhone.trim().isEmpty ||
          pin.isEmpty) {
        onResult(false, msg('कृपया सर्व आवश्यक माहिती भरा.', 'कृपया सभी आवश्यक जानकारी भरें।', 'Please fill in every required field.'));
        return;
      }

      if (!_isFourDigitPin(pin)) {
        onResult(false, msg('लॉगिन पिन नेमका ४ अंकी असावा.', 'लॉगिन पिन ठीक 4 अंकों का होना चाहिए।', 'The login PIN must be exactly 4 digits.'));
        return;
      }

      // The login code is what tells societies apart, so it has to be unique.
      final clash = await _repository.findSocietyByCode(societyCode);
      if (clash != null) {
        onResult(
            false,
            msg(
              "कोड $societyCode आधीच '${clash.name}' साठी वापरला जातो. दुसरा कोड निवडा.",
              "कोड $societyCode पहले से '${clash.name}' के लिए उपयोग में है। दूसरा कोड चुनें।",
              "Code $societyCode is already used by '${clash.name}'. Please pick a different code.",
            ));
        return;
      }

      final wingPlans = request.wingPlans.where((p) => p.totalUnits > 0).toList();
      final generatedUnits = [for (final plan in wingPlans) ..._toVacantUnits(plan)];

      final newSoc = SocietyInfo(
        name: request.name.trim(),
        registrationNumber: request.regNumber.trim(),
        societyCode: societyCode,
        address: request.address.trim(),
        totalWings: request.wingNames.length > wingPlans.length ? request.wingNames.length : wingPlans.length,
        totalFlats: generatedUnits.length,
        bankAccountName: request.bankName.trim(),
        bankAccountNumber: request.bankAccount.trim(),
        ifscCode: request.ifsc.trim().toUpperCase(),
        upiId: request.upiId.trim(),
        paymentQrBase64: request.paymentQrBase64,
        currentBalanceCash: 0.0,
        currentBalanceBank: request.initialBankBalance,
        tariff1BHK: request.tariffFor('1 BHK', 1500.0),
        tariff2BHK: request.tariffFor('2 BHK', 2200.0),
        tariff3BHK: request.tariffFor('3 BHK', 3000.0),
        tariffPenthouse: request.tariffFor('Penthouse', request.tariffFor('3 BHK', 3000.0) * 1.5),
        tariffShop: request.tariffFor('Shop', 3500.0),
        logoBase64: request.logoBase64,
        country: request.country.trim(),
        state: request.state.trim(),
        city: request.city.trim(),
        pinCode: request.pinCode.trim(),
        contactNumber: request.contactNumber.trim(),
        email: request.email.trim(),
        wingNames: request.wingNames,
        wingPlans: wingPlans,
        unitTypes: request.unitTypes,
        billingStartMonth: request.billingStartMonth.trim(),
      );
      await _repository.insertOrUpdateSociety(newSoc);
      _repository.setActiveSociety(societyCode);

      // Floor inventory first: the flats land in the Resident Directory as
      // vacant units, ready for the committee to fill in owners against.
      await _repository.insertVacantUnits(societyCode, generatedUnits);

      final chairman = Resident(
        flatNumber: flatNumber,
        wing: _wingOf(flatNumber, request.wingNames),
        ownerName: request.chairmanName.trim(),
        contactNumber: request.chairmanPhone.trim(),
        email: request.chairmanEmail.trim(),
        role: UserRole.CHAIRMAN,
        pin: pin,
        flatType: FlatType.THREE_BHK,
        balanceDue: 0.0,
      );
      // Written after the vacant units so the chairman's own flat keeps his
      // details even when floor inventory generated that same number.
      await _repository.insertResident(chairman, societyCode: societyCode);

      // The registration notice to head office - never allowed to fail the
      // registration.
      try {
        await _queueRegistrationEmail(newSoc, chairman);
      } catch (e) {
        debugPrint('$_tag: registration email not queued: $e');
      }

      // A standing record of the login details inside the society itself.
      try {
        await _repository.insertNotice(
          Notice(
            title: 'Society registered on SocietySarv',
            content: registrationConfirmationText(newSoc, chairman),
            publishedDate: todayLabel(),
            isPinned: true,
            category: NoticeCategory.GENERAL,
            authorRole: 'SocietySarv',
          ),
          societyCode: societyCode,
        );
      } catch (e) {
        debugPrint('$_tag: confirmation notice failed: $e');
      }

      // The chairman is not signed in yet on purpose: the wizard's
      // Confirmation step needs to stay on screen long enough to hand the
      // login details over.
      _pendingRegistration = PendingRegistration(
        society: newSoc,
        chairman: await _repository.getResidentByFlat(flatNumber, societyCode: societyCode) ?? chairman,
      );
      _notify();

      final unitsNote = generatedUnits.isEmpty ? '' : ' ${generatedUnits.length} units created.';
      onResult(
          true,
          msg(
            'सोसायटी नोंदणी यशस्वी! लॉगिनसाठी कोड $societyCode व फ्लॅट $flatNumber वापरा.$unitsNote',
            'सोसायटी पंजीकरण सफल! लॉगिन के लिए कोड $societyCode और फ्लैट $flatNumber उपयोग करें।$unitsNote',
            'Society registered! Log in with code $societyCode and flat $flatNumber.$unitsNote',
          ));
    });
  }

  /// Signs the chairman in once they have acknowledged the confirmation screen.
  void completeRegistrationSignIn() {
    final pending = _pendingRegistration;
    if (pending == null) return;
    _sessionStore.save(pending.society.societyCode, pending.chairman.flatNumber);
    _selectedUnitNumber = null;
    _currentUser = pending.chairman;
    _selectedTab = NavDestination.DASHBOARD;
    _pendingRegistration = null;
    _notify();
  }

  void clearPendingRegistration() {
    _pendingRegistration = null;
    _notify();
  }

  /// Wing a flat belongs to: the configured wing it is prefixed with, else its first token.
  String _wingOf(String flatNumber, List<String> wingNames) {
    for (final w in wingNames) {
      if (flatNumber.toUpperCase().startsWith('${w.toUpperCase()}-')) return w;
    }
    final head = flatNumber.split('-').first;
    final short = head.length > 2 ? head.substring(0, 2) : head;
    return short.trim().isEmpty ? 'A' : short;
  }

  /// Expands one wing into the vacant units it plans for.
  List<Resident> _toVacantUnits(WingPlan plan) {
    final numbers = plan.generateUnitNumbers();
    final types = plan.unitTypesInOrder();
    return [
      for (var i = 0; i < numbers.length; i++)
        Resident(
          flatNumber: numbers[i],
          wing: plan.name,
          ownerName: '',
          contactNumber: '',
          role: UserRole.RESIDENT,
          pin: '',
          unitType: i < types.length ? types[i] : '',
          flatType: flatTypeFor(i < types.length ? types[i] : ''),
          balanceDue: 0.0,
        ),
    ];
  }

  /// Best-effort mapping of a free-text unit type onto the built-in slabs.
  static FlatType flatTypeFor(String unitTypeName) {
    final name = unitTypeName.toLowerCase().replaceAll(' ', '');
    if (name.contains('1bhk') || name.contains('1rk')) return FlatType.ONE_BHK;
    if (name.contains('3bhk')) return FlatType.THREE_BHK;
    if (name.contains('penthouse')) return FlatType.PENTHOUSE;
    if (name.contains('shop') || name.contains('office') || name.contains('commercial')) {
      return FlatType.COMMERCIAL_SHOP;
    }
    return FlatType.TWO_BHK;
  }

  /// Sends head office the details of a society that has just registered,
  /// queued into the `mail` collection for the Trigger Email worker.
  Future<void> _queueRegistrationEmail(SocietyInfo society, Resident chairman) async {
    final registeredOn = formatDate(DateTime.now(), 'dd MMM yyyy');
    final subject = 'New Society Registered - ${society.name}';
    String orDash(String v) => v.trim().isEmpty ? '-' : v;
    final address = [society.address, society.city, society.state, society.pinCode]
        .where((s) => s.trim().isNotEmpty)
        .join(', ');

    final details = <(String, String)>[
      ('Society Name', society.name),
      ('Society Code', society.societyCode),
      ('Chairman Name', chairman.ownerName),
      ('Chairman Mobile Number', chairman.contactNumber),
      ('Registration Date', registeredOn),
      ('Registration No.', orDash(society.registrationNumber)),
      ('Address', orDash(address)),
      ('Contact', orDash(society.contactNumber)),
      ('Email', orDash(society.email)),
      ('Wings', society.wingNames.isEmpty ? society.totalWings.toString() : society.wingNames.join(', ')),
      ('Units Created', society.totalFlats.toString()),
    ];

    final text = StringBuffer()
      ..writeln('Dear Team,')
      ..writeln()
      ..writeln('A new society has been successfully registered in the system.')
      ..writeln()
      ..writeln('Society Details:');
    for (final (label, value) in details) {
      text.writeln('$label: $value');
    }
    text
      ..writeln()
      ..writeln('Please find the complete society registration details in the system.')
      ..writeln()
      ..writeln('Regards,')
      ..write('MultiSarv System');

    final html = StringBuffer()
      ..write('<p>Dear Team,</p>')
      ..write('<p>A new society has been successfully registered in the system.</p>')
      ..write('<p><strong>Society Details:</strong></p><ul>');
    for (final (label, value) in details) {
      html.write('<li><strong>$label:</strong> ${_escapeHtml(value)}</li>');
    }
    html
      ..write('</ul>')
      ..write('<p>Please find the complete society registration details in the system.</p>')
      ..write('<p>Regards,<br/>MultiSarv System</p>');

    await _repository.queueSocietyRegistrationEmail(
      societyCode: society.societyCode,
      from: _registrationEmailFrom,
      to: _registrationEmailTo,
      subject: subject,
      bodyText: text.toString(),
      bodyHtml: html.toString(),
    );
  }

  String _escapeHtml(String value) => value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');

  /// The confirmation the chairman receives - also what the "Send to Chairman"
  /// buttons on the success screen put into the SMS / email body.
  String registrationConfirmationText(SocietyInfo society, Resident chairman) {
    final b = StringBuffer()
      ..writeln('SocietySarv - Society Registered')
      ..writeln('--------------------------------')
      ..writeln('Society : ${society.name}');
    if (society.registrationNumber.trim().isNotEmpty) b.writeln('Reg. No : ${society.registrationNumber}');
    b
      ..writeln('Login Code : ${society.societyCode}')
      ..writeln('Chairman : ${chairman.ownerName} (Unit ${chairman.flatNumber})')
      ..writeln('Mobile : ${chairman.contactNumber}')
      ..writeln('Wings : ${society.wingNames.isEmpty ? society.totalWings.toString() : society.wingNames.join(', ')}')
      ..writeln('Units created : ${society.totalFlats}')
      ..writeln('--------------------------------')
      ..writeln('Log in with:')
      ..writeln('  Society Login Code : ${society.societyCode}')
      ..writeln('  Unit / Mobile      : ${chairman.flatNumber}')
      ..writeln('  4-Digit Login PIN  : the PIN set during registration')
      ..writeln('Keep these details safe.');
    return b.toString();
  }

  // --------------------------------------------- Maintenance collection

  String _newReceiptNumber() => 'REC-${formatDate(DateTime.now(), 'yyyy-MM')}-${randomBetween(1000, 9999)}';

  String _newBillNumber() => 'BILL-${formatDate(DateTime.now(), 'yyMM')}-${randomBetween(100, 999)}';

  /// Receipts a payment against one bill. The bill takes only what it is owed;
  /// anything over that is held as the unit's advance credit.
  void recordMaintenancePayment(MaintenanceBill bill, double amount, PaymentMode mode, String ref, String dateStr,
      {String screenshotBase64 = ''}) {
    _safeLaunch(() async {
      final now = todayLabel();
      final receiptNo = _newReceiptNumber();
      final paid = amount < 0 ? 0.0 : amount;
      final appliedToBill = roundToPaise(paid < bill.pendingAmount ? paid : bill.pendingAmount);
      final excess = roundToPaise(paid - appliedToBill);
      final paidSoFar = roundToPaise(bill.amountPaid + appliedToBill);
      await _repository.updateBill(bill.copyWith(
        amountPaid: paidSoFar,
        totalAmount: bill.payableTotal,
        status: paidSoFar >= bill.payableTotal - 0.009 ? PaymentStatus.PAID : PaymentStatus.PARTIAL,
        paidDate: dateStr.trim().isEmpty ? now : dateStr,
        paymentMode: mode,
        transactionRef: ref,
        receiptNumber: receiptNo,
        receiptImageBase64: screenshotBase64,
      ));

      // Update resident dues, and park any overpayment as advance credit.
      final res = await _repository.getResidentByFlat(bill.flatNumber);
      if (res != null) {
        final due = res.balanceDue - appliedToBill;
        await _repository.updateResident(res.copyWith(
          balanceDue: due < 0 ? 0.0 : due,
          advanceBalance: roundToPaise(res.advanceBalance + excess),
          advanceMode: excess > 0.009 ? mode : res.advanceMode,
        ));
      }

      final carried = excess > 0.009
          ? msg(
              ' ₹${fmt2(excess)} जास्तीची रक्कम पुढील बिलात वजा होईल.',
              ' ₹${fmt2(excess)} अतिरिक्त राशि अगले बिल में समायोजित होगी।',
              ' ₹${fmt2(excess)} extra carried forward to the next bill.',
            )
          : '';
      showMessage(msg('पावती क्र. $receiptNo तयार झाली!$carried', 'रसीद क्र. $receiptNo तैयार हुई!$carried',
          'Receipt #$receiptNo generated.$carried'));
    });
  }

  /// Receipts one payment against however many months it covers: spread over
  /// the unit's pending bills oldest month first, one receipt number, the
  /// leftover as advance credit.
  void recordBulkPayment(String flatNumber, double amount, PaymentMode mode, String ref, String dateStr,
      {String screenshotBase64 = '', ResultCallback onResult = _noop}) {
    if (!canEditAccounts) {
      onResult(false, _accountsReadOnlyMessage());
      return;
    }
    if (amount <= 0.0) {
      onResult(false, msg('रक्कम शून्यापेक्षा जास्त असावी.', 'राशि शून्य से अधिक होनी चाहिए।', 'Enter an amount greater than zero.'));
      return;
    }
    final pending = pendingBillsFor(flatNumber);
    if (pending.isEmpty) {
      onResult(
          false,
          msg('$flatNumber ची सर्व बिले आधीच भरली आहेत.', '$flatNumber के सभी बिल पहले ही भरे गए हैं।',
              '$flatNumber has no pending bills to settle.'));
      return;
    }
    _safeLaunch(() async {
      final receiptNo = _newReceiptNumber();
      final paidOn = dateStr.trim().isEmpty ? todayLabel() : dateStr;
      final allocation = allocatePayment(pending, amount);

      for (final line in allocation.touched) {
        final paidSoFar = roundToPaise(line.bill.amountPaid + line.applied);
        await _repository.updateBill(line.bill.copyWith(
          amountPaid: paidSoFar,
          totalAmount: line.bill.payableTotal,
          status: line.isSettled ? PaymentStatus.PAID : PaymentStatus.PARTIAL,
          paidDate: paidOn,
          paymentMode: mode,
          transactionRef: ref,
          receiptNumber: receiptNo,
          receiptImageBase64: screenshotBase64,
        ));
      }

      final res = await _repository.getResidentByFlat(flatNumber);
      if (res != null) {
        final due = res.balanceDue - allocation.applied;
        await _repository.updateResident(res.copyWith(
          balanceDue: due < 0 ? 0.0 : due,
          advanceBalance: roundToPaise(res.advanceBalance + allocation.excess),
          advanceMode: allocation.excess > 0.009 ? mode : res.advanceMode,
        ));
      }

      final monthsCovered = allocation.touched.length;
      final stillOwed = roundToPaise(allocation.allocations.fold(0.0, (s, a) => s + a.pendingAfter));
      final tail = StringBuffer();
      if (stillOwed > 0.009) {
        tail.write(msg(' ₹${fmt2(stillOwed)} अजून थकीत आहे.', ' ₹${fmt2(stillOwed)} अब भी बकाया है।',
            ' ₹${fmt2(stillOwed)} still pending.'));
      }
      if (allocation.excess > 0.009) {
        final x = fmt2(allocation.excess);
        tail.write(msg(' ₹$x पुढील बिलात वजा होईल.', ' ₹$x अगले बिल में समायोजित होगी।', ' ₹$x carried forward to the next bill.'));
      }
      final applied = fmt2(allocation.applied);
      onResult(
          true,
          msg(
            'पावती $receiptNo - $monthsCovered महिन्यांसाठी ₹$applied नोंदवले.$tail',
            'रसीद $receiptNo - $monthsCovered महीनों के लिए ₹$applied दर्ज किया।$tail',
            'Receipt $receiptNo - ₹$applied recorded across $monthsCovered month(s).$tail',
          ));
    });
  }

  /// A unit's bills that still owe something, oldest month first.
  List<MaintenanceBill> pendingBillsFor(String flatNumber) => oldestMonthFirst(
      _bills.where((b) => _eqIgnoreCase(b.flatNumber, flatNumber) && b.pendingAmount > 0.009).toList());

  /// Advance credit sitting against a unit, from an earlier overpayment.
  double advanceCreditFor(String flatNumber) {
    for (final r in _residents) {
      if (_eqIgnoreCase(r.flatNumber, flatNumber)) return r.advanceBalance < 0 ? 0.0 : r.advanceBalance;
    }
    return 0.0;
  }

  /// What one unit owes: arrears, this month, and the credit set against them.
  UnitDues duesFor(String flatNumber) => unitDues(
        _bills.where((b) => _eqIgnoreCase(b.flatNumber, flatNumber)).toList(),
        advanceCreditFor(flatNumber),
      );

  /// Corrects a receipt that has already been taken. The amount replaces what
  /// was recorded rather than adding to it.
  void updateRecordedPayment(MaintenanceBill bill, double amount, PaymentMode mode, String ref, String dateStr,
      String screenshotBase64,
      {ResultCallback onResult = _noop}) {
    if (!canEditAccounts) {
      onResult(false, _accountsReadOnlyMessage());
      return;
    }
    if (amount <= 0.0) {
      onResult(false, msg('रक्कम शून्यापेक्षा जास्त असावी.', 'राशि शून्य से अधिक होनी चाहिए।', 'Enter an amount greater than zero.'));
      return;
    }
    _safeLaunch(() async {
      // The bill takes only what it charged; an overshoot carries forward as credit.
      final onBill = roundToPaise(amount < bill.payableTotal ? amount : bill.payableTotal);
      final excess = roundToPaise(amount - onBill);
      await _repository.updateBill(bill.copyWith(
        amountPaid: onBill,
        totalAmount: bill.payableTotal,
        status: onBill >= bill.payableTotal - 0.009 ? PaymentStatus.PAID : PaymentStatus.PARTIAL,
        paidDate: dateStr.trim().isEmpty ? bill.paidDate : dateStr,
        paymentMode: mode,
        transactionRef: ref,
        receiptImageBase64: screenshotBase64,
      ));

      final delta = onBill - bill.amountPaid;
      final res = await _repository.getResidentByFlat(bill.flatNumber);
      if (res != null && (delta != 0.0 || excess > 0.009)) {
        final due = res.balanceDue - delta;
        await _repository.updateResident(res.copyWith(
          balanceDue: due < 0 ? 0.0 : due,
          advanceBalance: roundToPaise(res.advanceBalance + excess),
          advanceMode: excess > 0.009 ? mode : res.advanceMode,
        ));
      }
      onResult(
          true,
          msg('पावती ${bill.receiptNumber} अद्ययावत केली!', 'रसीद ${bill.receiptNumber} अपडेट की गई!',
              'Receipt ${bill.receiptNumber} updated.'));
    });
  }

  /// Withdraws a receipt, putting the bill back to unpaid.
  void deleteReceipt(MaintenanceBill bill, {ResultCallback onResult = _noop}) {
    if (!canEditAccounts) {
      onResult(false, _accountsReadOnlyMessage());
      return;
    }
    if (bill.amountPaid <= 0.0) {
      onResult(false, msg('या बिलावर पावती नाही.', 'इस बिल पर रसीद नहीं है।', 'There is no receipt against this bill.'));
      return;
    }
    _safeLaunch(() async {
      final reversed = bill.amountPaid;
      await _repository.updateBill(bill.copyWith(
        amountPaid: 0.0,
        status: PaymentStatus.UNPAID,
        paidDate: '',
        paymentMode: null,
        transactionRef: '',
        receiptNumber: '',
        receiptImageBase64: '',
      ));
      final res = await _repository.getResidentByFlat(bill.flatNumber);
      if (res != null) {
        await _repository.updateResident(res.copyWith(balanceDue: res.balanceDue + reversed));
      }
      onResult(true, msg('पावती रद्द केली. बिल पुन्हा थकीत झाले.', 'रसीद हटाई गई। बिल फिर से बकाया है।', 'Receipt deleted. The bill is unpaid again.'));
    });
  }

  /// Raises one month's bill for a unit. Any advance the unit is holding is
  /// set against the new bill as it is created.
  void generateBill(String flatNumber, String residentName, String month, double baseAmount, double water,
      double sinking, double repair, double lateFee, String dueDate) {
    _safeLaunch(() async {
      final total = baseAmount + water + sinking + repair + lateFee;
      final res = await _repository.getResidentByFlat(flatNumber);
      final credit = (res?.advanceBalance ?? 0.0) < 0 ? 0.0 : (res?.advanceBalance ?? 0.0);
      final fromAdvance = roundToPaise(credit < total ? credit : total);

      await _repository.insertBill(MaintenanceBill(
        flatNumber: flatNumber,
        residentName: residentName,
        billMonth: month,
        baseAmount: baseAmount,
        waterCharges: water,
        sinkingFund: sinking,
        repairFund: repair,
        lateFeeAmount: lateFee,
        totalAmount: total,
        amountPaid: fromAdvance,
        status: fromAdvance <= 0.009
            ? PaymentStatus.UNPAID
            : (fromAdvance >= total - 0.009 ? PaymentStatus.PAID : PaymentStatus.PARTIAL),
        dueDate: dueDate,
        paidDate: fromAdvance > 0.009 ? todayLabel() : '',
        paymentMode: fromAdvance > 0.009 ? res?.advanceMode : null,
        transactionRef: fromAdvance > 0.009 ? advanceAdjustmentRef : '',
        receiptNumber: _newBillNumber(),
      ));

      if (res != null) {
        final due = res.balanceDue + total - fromAdvance;
        await _repository.updateResident(res.copyWith(
          balanceDue: due < 0 ? 0.0 : due,
          advanceBalance: roundToPaise(credit - fromAdvance),
        ));
      }

      final adjusted = fromAdvance > 0.009
          ? msg(
              ' मागील ₹${fmt2(fromAdvance)} जमा रक्कम वजा केली.',
              ' पिछली ₹${fmt2(fromAdvance)} जमा राशि समायोजित की।',
              ' ₹${fmt2(fromAdvance)} of advance credit adjusted against it.',
            )
          : '';
      showMessage(msg('$flatNumber साठी मासिक बिल तयार केले!$adjusted', '$flatNumber के लिए मासिक बिल बनाया!$adjusted',
          'Maintenance bill created for $flatNumber.$adjusted'));
    });
  }

  /// Every bill that ought to exist but does not: each billable unit, for each
  /// month from the billing start up to now. Vacant flats are not counted.
  List<MissingBill> pendingBillRun() {
    final society = _societyInfo;
    if (society == null) return const [];
    final months = billingMonths(society);
    if (months.isEmpty) return const [];

    final billable = _residents.where((r) => !r.role.isStaff && r.ownerName.trim().isNotEmpty);
    final alreadyBilled = _bills.map((b) => '${b.flatNumber.toUpperCase()}|${b.billMonth.toLowerCase()}').toSet();

    return [
      for (final unit in billable)
        for (final month in months)
          if (!alreadyBilled.contains('${unit.flatNumber.toUpperCase()}|${month.toLowerCase()}'))
            MissingBill(unit, month),
    ];
  }

  /// Raises every bill [pendingBillRun] found, in one go. Existing bills are
  /// never touched, so running this twice is harmless.
  void generateMissingBills({ResultCallback onResult = _noop}) {
    if (!canEditAccounts) {
      onResult(false, _accountsReadOnlyMessage());
      return;
    }
    final society = _societyInfo;
    if (society == null) {
      onResult(false, 'Society not loaded yet.');
      return;
    }
    final outstanding = pendingBillRun();
    if (outstanding.isEmpty) {
      onResult(false, msg('सर्व युनिट्सची बिले आधीच तयार आहेत.', 'सभी इकाइयों के बिल पहले से तैयार हैं।',
          'Every unit is already billed for every month.'));
      return;
    }
    _safeLaunch(() async {
      final duesAdded = <String, double>{};
      var raised = 0;
      // Advance credit is spent on the oldest month billed, then carries on.
      final creditLeft = <String, double>{};
      final now = todayLabel();

      final ordered = [...outstanding]..sort((a, b) => billMonthKey(a.month).compareTo(billMonthKey(b.month)));
      for (final missing in ordered) {
        final maintenance = society.monthlyTariffFor(missing.unit);
        if (maintenance <= 0.0) continue;
        final split = society.splitMaintenance(maintenance);
        final flat = missing.unit.flatNumber;
        final credit = creditLeft.putIfAbsent(
            flat, () => missing.unit.advanceBalance < 0 ? 0.0 : missing.unit.advanceBalance);
        final fromAdvance = roundToPaise(credit < split.total ? credit : split.total);
        creditLeft[flat] = roundToPaise(credit - fromAdvance);

        await _repository.insertBill(MaintenanceBill(
          flatNumber: flat,
          residentName: missing.unit.ownerName,
          billMonth: missing.month,
          baseAmount: split.other,
          waterCharges: split.water,
          sinkingFund: split.sinking,
          repairFund: split.repair,
          totalAmount: split.total,
          amountPaid: fromAdvance,
          status: fromAdvance <= 0.009
              ? PaymentStatus.UNPAID
              : (fromAdvance >= split.total - 0.009 ? PaymentStatus.PAID : PaymentStatus.PARTIAL),
          dueDate: _dueDateFor(missing.month),
          paidDate: fromAdvance > 0.009 ? now : '',
          paymentMode: fromAdvance > 0.009 ? missing.unit.advanceMode : null,
          transactionRef: fromAdvance > 0.009 ? advanceAdjustmentRef : '',
          receiptNumber: _newBillNumber(),
        ));
        duesAdded[flat] = (duesAdded[flat] ?? 0.0) + split.total - fromAdvance;
        raised++;
      }

      for (final entry in duesAdded.entries) {
        final res = await _repository.getResidentByFlat(entry.key);
        if (res != null) {
          final due = res.balanceDue + entry.value;
          await _repository.updateResident(res.copyWith(
            balanceDue: due < 0 ? 0.0 : due,
            advanceBalance: creditLeft[entry.key] ?? res.advanceBalance,
          ));
        }
      }

      final skipped = outstanding.length - raised;
      final note = skipped > 0
          ? msg(' $skipped युनिट्सना दर नसल्याने वगळले.', ' $skipped इकाइयां दर न होने से छोड़ी गईं।',
              ' $skipped skipped - no tariff set for their unit type.')
          : '';
      onResult(true, msg('$raised बिले तयार केली!$note', '$raised बिल तैयार किए गए!$note', 'Raised $raised bill(s).$note'));
    });
  }

  /// The 10th of the billed month, the usual society due date.
  String _dueDateFor(String billMonth) {
    final parsed = parseMonthYear(billMonth);
    if (parsed == null) return billMonth;
    return formatDate(DateTime(parsed.year, parsed.month, 10), 'dd MMM yyyy');
  }

  /// Rewrites an existing bill's line items. Payments already recorded are
  /// left alone; the member's balance moves by the difference.
  void updateBill(MaintenanceBill original, String month, double baseAmount, double water, double sinking,
      double repair, double lateFee, String dueDate,
      {ResultCallback onResult = _noop}) {
    if (!canEditAccounts) {
      onResult(false, _accountsReadOnlyMessage());
      return;
    }
    _safeLaunch(() async {
      final payable = baseAmount + water + sinking + repair + lateFee;
      await _repository.updateBill(original.copyWith(
        billMonth: month,
        baseAmount: baseAmount,
        waterCharges: water,
        sinkingFund: sinking,
        repairFund: repair,
        parkingCharges: 0.0,
        lateFeeAmount: lateFee,
        totalAmount: payable,
        dueDate: dueDate,
        status: original.amountPaid <= 0.0
            ? PaymentStatus.UNPAID
            : (original.amountPaid >= payable - 0.009 ? PaymentStatus.PAID : PaymentStatus.PARTIAL),
      ));

      final delta = payable - original.payableTotal;
      final res = await _repository.getResidentByFlat(original.flatNumber);
      if (res != null && delta != 0.0) {
        final due = res.balanceDue + delta;
        await _repository.updateResident(res.copyWith(balanceDue: due < 0 ? 0.0 : due));
      }
      onResult(
          true,
          msg('${original.flatNumber} चे $month चे बिल अद्ययावत केले!', '${original.flatNumber} का $month का बिल अपडेट किया गया!',
              'Bill for ${original.flatNumber} ($month) updated.'));
    });
  }

  /// Removes a bill and takes what it charged back off the member's dues.
  void deleteBill(MaintenanceBill bill, {ResultCallback onResult = _noop}) {
    if (!canEditAccounts) {
      onResult(false, _accountsReadOnlyMessage());
      return;
    }
    // A paid bill has a receipt against it; withdraw the receipt first.
    if (bill.derivedStatus == PaymentStatus.PAID) {
      onResult(
          false,
          msg('भरलेले बिल हटवता येत नाही. आधी पावती रद्द करा.', 'भुगतान किया गया बिल नहीं हटा सकते। पहले रसीद हटाएं।',
              'A paid bill cannot be deleted. Delete its receipt first.'));
      return;
    }
    _safeLaunch(() async {
      await _repository.deleteBill(bill);
      final res = await _repository.getResidentByFlat(bill.flatNumber);
      if (res != null) {
        final owed = bill.payableTotal - bill.amountPaid;
        final due = res.balanceDue - (owed < 0 ? 0.0 : owed);
        await _repository.updateResident(res.copyWith(balanceDue: due < 0 ? 0.0 : due));
      }
      onResult(true, msg('बिल हटवले!', 'बिल हटाया गया!', 'Bill deleted.'));
    });
  }

  // ------------------------------------------------------- Expense logger

  /// Cash plus bank - what the society can actually pay an expense out of.
  double get availableFunds {
    final f = financialOverview;
    return f.cashBalance + f.bankBalance;
  }

  /// Funds sitting in whichever account the expense would be paid from.
  double fundsFor(PaymentMode mode) {
    final f = financialOverview;
    return mode == PaymentMode.CASH ? f.cashBalance : f.bankBalance;
  }

  String _insufficientFundsMessage() => msg(
        'पुरेशी शिल्लक नाही. हा खर्च नोंदवण्यापूर्वी कृपया निधी जमा करा.',
        'अपर्याप्त शेष राशि। यह व्यय दर्ज करने से पहले कृपया धनराशि जोड़ें।',
        'Insufficient balance. Please add funds before recording this expense.',
      );

  void addExpense(String title, ExpenseCategory category, double amount, String date, String vendor,
      String vendorPhone, PaymentMode mode, String notes,
      {ResultCallback onResult = _noop}) {
    if (!canEditAccounts) {
      onResult(false, _accountsReadOnlyMessage());
      return;
    }
    // The society cannot spend money it does not hold.
    if (amount > availableFunds) {
      onResult(false, _insufficientFundsMessage());
      return;
    }
    _safeLaunch(() async {
      final expNo = 'EXP-${formatDate(DateTime.now(), 'yyyy')}-${randomBetween(100, 999)}';
      await _repository.insertExpense(ExpenseRecord(
        title: title,
        category: category,
        amount: amount,
        date: date.trim().isEmpty ? todayLabel() : date,
        vendorName: vendor,
        vendorContact: vendorPhone,
        paymentMode: mode,
        approvedBy: _currentUser?.role.name ?? 'Secretary',
        notes: notes,
        receiptNumber: expNo,
        status: PaymentStatus.PAID,
      ));
      final done = _language == AppLanguage.marathi ? '₹$amount खर्च नोंदवला ($expNo)' : 'Recorded expense ₹$amount ($expNo)';
      showMessage(done);
      onResult(true, done);
    });
  }

  /// Rewrites an expense that was already recorded. The overdraft guard allows
  /// for the money this expense already took out of the treasury.
  void updateExpense(ExpenseRecord previous, String title, ExpenseCategory category, double amount, String date,
      String vendor, String vendorPhone, PaymentMode mode, String notes,
      {ResultCallback onResult = _noop}) {
    if (!canEditAccounts) {
      onResult(false, _accountsReadOnlyMessage());
      return;
    }
    if (amount <= 0.0) {
      onResult(false, msg('रक्कम शून्यापेक्षा जास्त असावी.', 'राशि शून्य से अधिक होनी चाहिए।', 'Enter an amount greater than zero.'));
      return;
    }
    if (amount > availableFunds + previous.amount) {
      onResult(false, _insufficientFundsMessage());
      return;
    }
    _safeLaunch(() async {
      await _repository.updateExpense(
        previous,
        previous.copyWith(
          title: title.trim().isEmpty ? category.labelEn : title,
          category: category,
          amount: amount,
          date: date.trim().isEmpty ? previous.date : date,
          vendorName: vendor,
          vendorContact: vendorPhone,
          paymentMode: mode,
          notes: notes,
        ),
      );
      onResult(
          true,
          msg('खर्च अद्ययावत केला (${previous.receiptNumber}).', 'व्यय अपडेट किया गया (${previous.receiptNumber})।',
              'Expense ${previous.receiptNumber} updated.'));
    });
  }

  /// Deletes an expense; the balance follows on its own.
  void deleteExpense(ExpenseRecord expense, {ResultCallback onResult = _noop}) {
    if (!canEditAccounts) {
      onResult(false, _accountsReadOnlyMessage());
      return;
    }
    _safeLaunch(() async {
      await _repository.deleteExpense(expense);
      onResult(true, msg('खर्च हटवला!', 'व्यय हटाया गया!', 'Expense removed.'));
    });
  }

  String _accountsReadOnlyMessage() => msg(
        'हिशोबात बदल फक्त अध्यक्ष व खजिनदार करू शकतात.',
        'खातों में बदलाव केवल अध्यक्ष और कोषाध्यक्ष कर सकते हैं।',
        'Only the Chairman and Treasurer can edit the accounts.',
      );

  // ------------------------------------------------- Non-fee revenue

  void addNonFeeRevenue(String title, RevenueSource category, double amount, String date, String payer,
      String flatNumber, PaymentMode mode, String notes) {
    if (!canEditAccounts) {
      showMessage(_accountsReadOnlyMessage());
      return;
    }
    _safeLaunch(() async {
      final recNo = 'NFR-${formatDate(DateTime.now(), 'yyyy')}-${randomBetween(100, 999)}';
      await _repository.insertRevenue(NonFeeRevenue(
        title: title,
        sourceCategory: category,
        amount: amount,
        date: date.trim().isEmpty ? todayLabel() : date,
        payerName: payer,
        flatNumber: flatNumber,
        paymentMode: mode,
        receiptNumber: recNo,
        notes: notes,
      ));
      showMessage(_language == AppLanguage.marathi
          ? '₹$amount इतर महसूल नोंदवला ($recNo)'
          : 'Recorded non-fee revenue ₹$amount ($recNo)');
    });
  }

  // ------------------------------------------------ Resident management

  void addResident({
    required String flatNumber,
    required String wing,
    required String ownerName,
    String coApplicantName = '',
    required String tenantName,
    required String tenantPhone,
    required String rentAgreementBase64,
    required String phone,
    required String email,
    required UserRole role,
    required int familyCount,
    required int twoWheelers,
    required int fourWheelers,
    required String twoWheelerNo,
    required String fourWheelerNo,
    required String parkingSlot,
    required ParkingType parkingType,
    required String unitType,
    required FlatType flatType,
    required int sqft,
    required String pin,
    ResultCallback onResult = _noop,
  }) {
    _safeLaunch(() async {
      final flat = flatNumber.trim().toUpperCase();
      if (pin.trim().isNotEmpty && !_isFourDigitPin(pin.trim())) {
        onResult(false, msg('लॉगिन पिन नेमका ४ अंकी असावा.', 'लॉगिन पिन ठीक 4 अंकों का होना चाहिए।', 'The login PIN must be exactly 4 digits.'));
        return;
      }
      // A bay can only be allotted once.
      if (parkingType != ParkingType.NONE && parkingSlot.trim().isNotEmpty) {
        final all = await _repository.getAllResidentsSync();
        final clash = all.where((o) => _eqIgnoreCase(o.parkingSlot, parkingSlot.trim()) && !_eqIgnoreCase(o.flatNumber, flat));
        if (clash.isNotEmpty) {
          final c = clash.first;
          onResult(
              false,
              msg('पार्किंग $parkingSlot आधीच ${c.flatNumber} ला दिले आहे.', 'पार्किंग $parkingSlot पहले से ${c.flatNumber} को दिया गया है।',
                  'Parking $parkingSlot is already allotted to ${c.flatNumber}.'));
          return;
        }
      }
      if (flat.isEmpty || ownerName.trim().isEmpty || phone.trim().isEmpty || pin.trim().isEmpty) {
        onResult(
            false,
            msg('फ्लॅट क्रमांक, नाव, मोबाईल व पिन आवश्यक आहे.', 'फ्लैट नंबर, नाम, मोबाइल और पिन आवश्यक है।',
                'Unit number, name, mobile and PIN are all required.'));
        return;
      }
      // A flat generated by the Floor Inventory step exists but has no owner
      // yet - creating a resident fills that vacant unit in.
      final existing = await _repository.getResidentByFlat(flat);
      if (existing != null && existing.ownerName.trim().isNotEmpty) {
        onResult(false, msg('फ्लॅट $flat आधीच नोंदणीकृत आहे.', 'फ्लैट $flat पहले से पंजीकृत है।', 'Unit $flat is already registered.'));
        return;
      }
      final hasTenant = tenantName.trim().isNotEmpty;
      final wingName = wing.trim().isEmpty
          ? (flat.split('-').first.isEmpty ? '' : flat.split('-').first.substring(0, 1))
          : wing.trim();
      await _repository.insertResident(Resident(
        docId: existing?.docId ?? '',
        id: existing?.id ?? 0,
        flatNumber: flat,
        wing: wingName,
        ownerName: ownerName.trim(),
        coApplicantName: coApplicantName.trim(),
        tenantName: tenantName.trim(),
        tenantContact: hasTenant ? tenantPhone.trim() : '',
        rentAgreementBase64: hasTenant ? rentAgreementBase64 : '',
        contactNumber: phone.trim(),
        email: email.trim(),
        role: role,
        pin: pin.trim(),
        familyMemberCount: familyCount,
        vehicleTwoWheelerCount: twoWheelers,
        vehicleFourWheelerCount: fourWheelers,
        twoWheelerNumber: twoWheelerNo,
        fourWheelerNumber: fourWheelerNo,
        parkingSlot: parkingType == ParkingType.NONE ? '' : parkingSlot,
        parkingType: parkingType,
        unitType: unitType.trim(),
        // Kept in step with the chosen unit type for older screens.
        flatType: unitType.trim().isEmpty ? flatType : flatTypeFor(unitType),
        squareFeet: sqft,
        balanceDue: 0.0,
        isOwnerLiving: !hasTenant,
      ));
      onResult(
          true,
          msg('सभासद $flat जोडले! ते या फ्लॅट क्रमांक व पिनने लॉगिन करू शकतात.', 'सदस्य $flat जोड़ा गया! वे इस फ्लैट नंबर और पिन से लॉगिन कर सकते हैं।',
              'Member $flat added. They can log in with this unit number and PIN.'));
    });
  }

  /// Renames a wing on every unit that carries it.
  void renameWingOnUnits(String from, String to, {ResultCallback onResult = _noop}) {
    if (!canEditSociety) {
      onResult(false, msg('फक्त अध्यक्षच विंग बदलू शकतात.', 'केवल अध्यक्ष ही विंग बदल सकते हैं।', 'Only the Chairman can rename a wing.'));
      return;
    }
    final oldName = from.trim();
    final newName = to.trim();
    if (oldName.isEmpty || newName.isEmpty || _eqIgnoreCase(oldName, newName)) {
      onResult(false, 'Enter a different wing name.');
      return;
    }
    _safeLaunch(() async {
      final affected = (await _repository.getAllResidentsSync()).where((r) => _eqIgnoreCase(r.wing, oldName)).toList();
      for (final unit in affected) {
        await _repository.updateResident(unit.copyWith(wing: newName));
      }
      onResult(
          true,
          msg('विंग $oldName चे नाव $newName केले (${affected.length} सदनिका).', 'विंग $oldName का नाम $newName किया (${affected.length} इकाइयां)।',
              'Wing $oldName renamed to $newName on ${affected.length} unit(s).'));
    });
  }

  WingContents wingContents(String wing) {
    final units = _residents.where((r) => _eqIgnoreCase(r.wing, wing.trim())).toList();
    final unitNumbers = units.map((u) => u.flatNumber.toUpperCase()).toSet();
    return WingContents(
      units: units.length,
      occupied: units.where((u) => u.ownerName.trim().isNotEmpty).length,
      bills: _bills.where((b) => unitNumbers.contains(b.flatNumber.toUpperCase())).length,
    );
  }

  /// Deletes a wing, every unit on it, and the bills raised against them.
  void deleteWingWithUnits(String wing, {ResultCallback onResult = _noop}) {
    if (!canEditSociety) {
      onResult(false, msg('फक्त अध्यक्षच विंग हटवू शकतात.', 'केवल अध्यक्ष ही विंग हटा सकते हैं।', 'Only the Chairman can delete a wing.'));
      return;
    }
    final name = wing.trim();
    if (name.isEmpty) {
      onResult(false, 'Pick a wing to delete.');
      return;
    }
    _safeLaunch(() async {
      final units = (await _repository.getAllResidentsSync()).where((r) => _eqIgnoreCase(r.wing, name)).toList();
      final unitNumbers = units.map((u) => u.flatNumber.toUpperCase()).toSet();

      for (final b in _bills.where((b) => unitNumbers.contains(b.flatNumber.toUpperCase())).toList()) {
        await _repository.deleteBill(b);
      }
      for (final u in units) {
        await _repository.deleteResident(u);
      }

      // Take the wing off the society's own list as well.
      final soc = await _repository.getSocietyInfoSync();
      if (soc != null) {
        final keptNames = soc.wingNames.where((w) => !_eqIgnoreCase(w, name)).toList();
        final keptPlans = soc.wingPlans.where((p) => !_eqIgnoreCase(p.name, name)).toList();
        final flats = soc.totalFlats - units.length;
        await _repository.updateSociety(soc.copyWith(
          wingNames: keptNames,
          wingPlans: keptPlans,
          totalWings: keptNames.length > keptPlans.length ? keptNames.length : keptPlans.length,
          totalFlats: flats < 0 ? 0 : flats,
        ));
      }

      onResult(
          true,
          msg('विंग $name व त्यातील ${units.length} सदनिका हटवल्या.', 'विंग $name और उसकी ${units.length} इकाइयां हटाई गईं।',
              'Deleted wing $name and its ${units.length} unit(s).'));
    });
  }

  /// Deletes a unit that nobody lives in. Refused for an occupied unit.
  void deleteVacantUnit(Resident unit, {ResultCallback onResult = _noop}) {
    if (!canEditSociety) {
      onResult(false, msg('फक्त अध्यक्षच सदनिका हटवू शकतात.', 'केवल अध्यक्ष ही इकाई हटा सकते हैं।', 'Only the Chairman can delete a unit.'));
      return;
    }
    if (unit.ownerName.trim().isNotEmpty) {
      onResult(false, msg('या सदनिकेत सभासद आहे. आधी सभासद काढा.', 'इस इकाई में सदस्य है। पहले सदस्य हटाएं।', 'That unit has a member in it. Remove the member first.'));
      return;
    }
    _safeLaunch(() async {
      await _repository.deleteResident(unit);
      onResult(true, msg('सदनिका ${unit.flatNumber} हटवली.', 'इकाई ${unit.flatNumber} हटाई गई।', 'Unit ${unit.flatNumber} deleted.'));
    });
  }

  /// Takes the member off a unit, leaving the unit itself as a vacant one.
  void removeMemberFromUnit(Resident member, {ResultCallback onResult = _noop}) {
    if (!canEditSociety) {
      onResult(false, msg('फक्त अध्यक्षच सभासद काढू शकतात.', 'केवल अध्यक्ष ही सदस्य हटा सकते हैं।', 'Only the Chairman can remove a member.'));
      return;
    }
    _safeLaunch(() async {
      await _repository.updateResident(member.copyWith(
        ownerName: '',
        tenantName: '',
        contactNumber: '',
        email: '',
        pin: '',
        photoBase64: '',
        familyMemberCount: 1,
        parkingSlot: '',
        parkingType: ParkingType.NONE,
        twoWheelerNumber: '',
        fourWheelerNumber: '',
        vehicleTwoWheelerCount: 0,
        vehicleFourWheelerCount: 0,
        isOwnerLiving: true,
      ));
      onResult(
          true,
          msg('${member.flatNumber} मधील सभासद काढला. सदनिका रिक्त झाली.', '${member.flatNumber} से सदस्य हटाया गया। इकाई रिक्त है।',
              'Member removed from ${member.flatNumber}. The unit is now vacant.'));
    });
  }

  void updateResident(Resident resident, {ResultCallback onResult = _noop}) {
    _safeLaunch(() async {
      // A bay can only be allotted once.
      if (resident.parkingType != ParkingType.NONE && resident.parkingSlot.trim().isNotEmpty) {
        final all = await _repository.getAllResidentsSync();
        final clash = all.where((o) =>
            _eqIgnoreCase(o.parkingSlot.trim(), resident.parkingSlot.trim()) &&
            !_eqIgnoreCase(o.flatNumber, resident.flatNumber));
        if (clash.isNotEmpty) {
          final c = clash.first;
          final message = msg(
              'पार्किंग ${resident.parkingSlot} आधीच ${c.flatNumber} ला दिले आहे.',
              'पार्किंग ${resident.parkingSlot} पहले से ${c.flatNumber} को दिया गया है।',
              'Parking ${resident.parkingSlot} is already allotted to ${c.flatNumber}.');
          showMessage(message);
          onResult(false, message);
          return;
        }
      }
      await _repository.updateResident(resident);
      final done = msg('माहिती अद्ययावत केली!', 'जानकारी अपडेट की गई!', 'Resident details updated!');
      showMessage(done);
      onResult(true, done);
    });
  }

  /// Saves how a month's maintenance is divided between the society's funds.
  /// The four shares must add up to 100%.
  void updateMaintenanceCharges(double waterPercent, double sinkingPercent, double repairPercent, double otherPercent,
      {ResultCallback onResult = _noop}) {
    if (!canEditAccounts) {
      onResult(false, _accountsReadOnlyMessage());
      return;
    }
    final shares = [waterPercent, sinkingPercent, repairPercent, otherPercent];
    if (shares.any((s) => s < 0.0)) {
      onResult(false, msg('टक्केवारी ऋण असू शकत नाही.', 'प्रतिशत ऋणात्मक नहीं हो सकता।', 'A percentage cannot be negative.'));
      return;
    }
    final total = shares.fold(0.0, (a, b) => a + b);
    if ((total - 100.0).abs() >= 0.01) {
      final t = plain2(total);
      onResult(
          false,
          msg('एकूण टक्केवारी १००% असावी. सध्या ती $t% आहे.', 'कुल प्रतिशत 100% होना चाहिए। अभी यह $t% है।',
              'The percentages must add up to 100%. They currently add up to $t%.'));
      return;
    }
    _safeLaunch(() async {
      final soc = await _repository.getSocietyInfoSync();
      if (soc == null) {
        onResult(false, 'Society not loaded yet.');
        return;
      }
      await _repository.updateSociety(soc.copyWith(
        waterChargesPercent: waterPercent,
        sinkingFundPercent: sinkingPercent,
        repairFundPercent: repairPercent,
        otherMaintenancePercent: otherPercent,
      ));
      onResult(
          true,
          msg('देखभाल शुल्क रचना जतन केली! पुढील बिलांना ती लागू होईल.', 'रखरखाव शुल्क संरचना सहेजी गई! अगले बिलों पर लागू होगी।',
              'Maintenance charge split saved. It applies to bills raised from now on.'));
    });
  }

  void updateLateFeeRules(double percent, int graceDays) {
    _safeLaunch(() async {
      final soc = await _repository.getSocietyInfoSync();
      if (soc != null) {
        await _repository.updateSociety(soc.copyWith(lateFeePercentPerMonth: percent, lateFeeGraceDays: graceDays));
        showMessage(_language == AppLanguage.marathi ? 'व्याज व दंड नियम अद्ययावत केले!' : 'Late fee rules updated!');
      }
    });
  }

  // ------------------------------------------------------ Gate & visitor

  void checkInVisitor(String name, String phone, String flatNumber, VisitorPurpose purpose, String vehicleNo,
      String passcode) {
    _safeLaunch(() async {
      await _repository.insertVisitor(VisitorLog(
        visitorName: name,
        phone: phone,
        flatNumber: flatNumber,
        purpose: purpose,
        vehicleNumber: vehicleNo,
        entryTime: nowTimeLabel(),
        status: VisitorStatus.INSIDE,
        passcode: passcode.trim().isEmpty ? randomBetween(100000, 999999).toString() : passcode,
        guardName: _currentUser?.ownerName ?? 'Gate Guard',
      ));
      showMessage(_language == AppLanguage.marathi ? 'अभ्यागत नोंद केली (फ्लॅट $flatNumber)' : 'Visitor checked in for $flatNumber');
    });
  }

  /// The member at the unit says a visitor is expected, or is not. Only the
  /// unit the visitor was logged against (or the committee) can answer.
  void answerVisitor(VisitorLog visitor, bool isApproved, {ResultCallback onResult = _noop}) {
    final user = _currentUser;
    final ownsUnit = myUnits.any((u) => _eqIgnoreCase(u.flatNumber, visitor.flatNumber)) ||
        _eqIgnoreCase(user?.flatNumber, visitor.flatNumber);
    final isCommittee = user?.role == UserRole.CHAIRMAN || user?.role == UserRole.SECRETARY;
    if (!ownsUnit && !isCommittee) {
      onResult(false, msg('हा अभ्यागत तुमच्या सदनिकेसाठी नाही.', 'यह आगंतुक आपकी इकाई के लिए नहीं है।', 'That visitor is not for your unit.'));
      return;
    }
    _safeLaunch(() async {
      await _repository.updateVisitor(visitor.copyWith(status: isApproved ? VisitorStatus.INSIDE : VisitorStatus.REJECTED));
      onResult(
          true,
          isApproved
              ? msg('${visitor.visitorName} ना परवानगी दिली.', '${visitor.visitorName} को अनुमति दी गई।', 'Approved ${visitor.visitorName}.')
              : msg('${visitor.visitorName} ना नकार दिला.', '${visitor.visitorName} को अस्वीकार किया।', 'Turned ${visitor.visitorName} away.'));
    });
  }

  void markVisitorExit(VisitorLog visitor) {
    _safeLaunch(() async {
      await _repository.markVisitorExit(nowTimeLabel(), visitor);
      showMessage(_language == AppLanguage.marathi ? '${visitor.visitorName} बाहेर पडले' : 'Marked exit for ${visitor.visitorName}');
    });
  }

  void generateVisitorPasscode(String visitorName, String phone, String flatNumber, VisitorPurpose purpose,
      {required void Function(String code) onGenerated}) {
    _safeLaunch(() async {
      final code = randomBetween(100000, 999999).toString();
      await _repository.insertVisitor(VisitorLog(
        visitorName: visitorName,
        phone: phone,
        flatNumber: flatNumber,
        purpose: purpose,
        entryTime: nowTimeLabel(),
        status: VisitorStatus.PRE_APPROVED,
        passcode: code,
      ));
      onGenerated(code);
      showMessage(_language == AppLanguage.marathi ? '६-अंकी पासकोड तयार झाला: $code' : 'Generated 6-digit passcode: $code');
    });
  }

  void verifyPasscode(String code, {required void Function(VisitorLog? log) onResult}) {
    _safeLaunch(() async {
      onResult(await _repository.verifyVisitorPasscode(code.trim()));
    });
  }

  // ---------------------------------------------------- Community notices

  void postNotice(String title, String content, bool isPinned, NoticeCategory category) {
    _safeLaunch(() async {
      await _repository.insertNotice(Notice(
        title: title,
        content: content,
        publishedDate: todayLabel(),
        isPinned: isPinned,
        category: category,
        authorRole: _currentUser?.role.name ?? 'Managing Committee',
      ));
      showMessage(_language == AppLanguage.marathi ? 'नवीन सूचना फलकावर प्रसिद्ध केली!' : 'Notice published to board!');
    });
  }

  void deleteNotice(Notice notice) {
    _safeLaunch(() async {
      await _repository.deleteNotice(notice);
      showMessage(_language == AppLanguage.marathi ? 'सूचना हटवली!' : 'Notice removed!');
    });
  }

  // ---------------------------------------------- Helpdesk & complaints

  void fileComplaint(ComplaintCategory category, String description, String flatNumber, String residentName) {
    _safeLaunch(() async {
      await _repository.insertComplaint(Complaint(
        flatNumber: flatNumber,
        residentName: residentName,
        category: category,
        description: description,
        filedDate: todayLabel(),
        status: TicketStatus.OPEN,
      ));
      showMessage(_language == AppLanguage.marathi
          ? 'तक्रार दाखल केली! समिती लवकरच कार्यवाही करेल.'
          : 'Complaint ticket raised successfully!');
    });
  }

  /// A member correcting their own complaint, while it is still open.
  void editMyComplaint(Complaint complaint, ComplaintCategory category, String description,
      {ResultCallback onResult = _noop}) {
    final refusal = _complaintRefusal(complaint);
    if (refusal != null) {
      onResult(false, refusal);
      return;
    }
    if (description.trim().isEmpty) {
      onResult(false, msg('तपशील लिहा.', 'विवरण लिखें।', 'Write what the problem is.'));
      return;
    }
    _safeLaunch(() async {
      await _repository.updateComplaint(complaint.copyWith(category: category, description: description.trim()));
      onResult(true, msg('तक्रार अद्ययावत केली!', 'शिकायत अपडेट की गई!', 'Complaint updated.'));
    });
  }

  /// A member withdrawing their own complaint, while it is still open.
  void deleteMyComplaint(Complaint complaint, {ResultCallback onResult = _noop}) {
    final refusal = _complaintRefusal(complaint);
    if (refusal != null) {
      onResult(false, refusal);
      return;
    }
    _safeLaunch(() async {
      await _repository.deleteComplaint(complaint);
      onResult(true, msg('तक्रार मागे घेतली.', 'शिकायत वापस ली गई।', 'Complaint withdrawn.'));
    });
  }

  /// Why this member may not touch this complaint, or null when they may.
  String? _complaintRefusal(Complaint complaint) {
    final mine = myUnits.any((u) => _eqIgnoreCase(u.flatNumber, complaint.flatNumber)) ||
        _eqIgnoreCase(_currentUser?.flatNumber, complaint.flatNumber);
    if (!mine) {
      return msg('ही तक्रार तुमच्या सदनिकेची नाही.', 'यह शिकायत आपकी इकाई की नहीं है।', 'That complaint was not raised by your unit.');
    }
    if (complaint.status != TicketStatus.OPEN) {
      return msg(
          'समितीने ही तक्रार हाती घेतली आहे, आता बदलता येणार नाही.',
          'समिति ने यह शिकायत ले ली है, अब बदली नहीं जा सकती।',
          'The committee has already picked this up, so it can no longer be changed.');
    }
    return null;
  }

  void updateComplaintStatus(Complaint complaint, TicketStatus newStatus, String notes) {
    _safeLaunch(() async {
      await _repository.updateComplaintStatus(complaint, newStatus, notes);
      showMessage(_language == AppLanguage.marathi ? 'तक्रार स्थिती अद्ययावत केली!' : 'Ticket status updated!');
    });
  }

  // ------------------------------------------------------- Society staff

  /// Parking bays that are free, plus whichever bay [keepFor] already holds.
  List<String> availableParking(ParkingType type, {String keepFor = ''}) {
    final society = _societyInfo;
    if (society == null) return const [];
    final taken = _residents
        .where((r) => !_eqIgnoreCase(r.flatNumber, keepFor))
        .map((r) => r.parkingSlot.trim().toUpperCase())
        .where((s) => s.isNotEmpty)
        .toSet();
    return society.parkingNumbers(type).where((n) => !taken.contains(n.toUpperCase())).toList();
  }

  /// Creates or updates a staff member. The staff code doubles as their login id.
  void saveStaff({
    required String staffCode,
    required String name,
    required UserRole role,
    required String designation,
    required String phone,
    required String email,
    required String pin,
    required bool isActive,
    Resident? existing,
    SocietyRole? customRole,
    ResultCallback onResult = _noop,
  }) {
    if (!canEditSociety) {
      onResult(false, msg('फक्त अध्यक्षच कर्मचारी व्यवस्थापित करू शकतात.', 'केवल अध्यक्ष ही कर्मचारी प्रबंधित कर सकते हैं।', 'Only the Chairman can manage society staff.'));
      return;
    }
    _safeLaunch(() async {
      final code = staffCode.trim().toUpperCase();
      final trimmedPin = pin.trim();
      if (code.isEmpty || name.trim().isEmpty || phone.trim().isEmpty) {
        onResult(false, msg('कर्मचारी कोड, नाव व मोबाईल आवश्यक आहे.', 'स्टाफ कोड, नाम और मोबाइल आवश्यक है।', 'Staff code, name and mobile number are all required.'));
        return;
      }
      if (!_isFourDigitPin(trimmedPin)) {
        onResult(false, msg('लॉगिन पिन नेमका ४ अंकी असावा.', 'लॉगिन पिन ठीक 4 अंकों का होना चाहिए।', 'The login PIN must be exactly 4 digits.'));
        return;
      }
      // A staff code shares the residents collection with flat numbers, so it
      // must not collide with a real unit.
      final clash = await _repository.getResidentByFlat(code);
      if (clash != null && !clash.role.isStaff && existing == null) {
        onResult(
            false,
            msg('$code हा फ्लॅट क्रमांक आधीच वापरात आहे. दुसरा कर्मचारी कोड निवडा.', '$code पहले से एक फ्लैट नंबर है। दूसरा स्टाफ कोड चुनें।',
                '$code is already a unit number. Pick a different staff code.'));
        return;
      }

      if (customRole != null && !customRole.isActive && customRole.docId != existing?.customRoleId) {
        onResult(false, roleRefusalMessage(RoleChangeRefusal.ROLE_UNAVAILABLE));
        return;
      }
      final newRole = customRole?.baseRole ?? role;
      final saved = Resident(
        docId: existing?.docId ?? '',
        id: existing?.id ?? 0,
        flatNumber: code,
        wing: 'STAFF',
        ownerName: name.trim(),
        contactNumber: phone.trim(),
        email: email.trim(),
        role: newRole,
        pin: trimmedPin,
        staffDesignation: designation.trim(),
        isActive: isActive,
        flatType: FlatType.COMMERCIAL_SHOP,
        balanceDue: 0.0,
        customRoleId: customRole?.docId ?? '',
        customRoleName: customRole?.name.trim() ?? '',
        photoBase64: existing?.photoBase64 ?? '',
      );
      await _repository.insertResident(saved);
      // A different role for an existing staff member goes on record.
      if (existing != null && (existing.role != saved.role || existing.customRoleId != saved.customRoleId)) {
        await _repository.recordRoleChange(_roleChangeEntry(existing, saved.role, customRole));
      }
      onResult(
          true,
          msg('कर्मचारी $code जतन केला. ते या कोड व पिनने लॉगिन करू शकतात.', 'स्टाफ $code सहेजा गया। वे इस कोड और पिन से लॉगिन कर सकते हैं।',
              'Staff $code saved. They can sign in with this code and PIN.'));
    });
  }

  void deleteStaff(Resident member, {ResultCallback onResult = _noop}) {
    if (!canEditSociety) {
      onResult(false, msg('फक्त अध्यक्षच कर्मचारी व्यवस्थापित करू शकतात.', 'केवल अध्यक्ष ही कर्मचारी प्रबंधित कर सकते हैं।', 'Only the Chairman can manage society staff.'));
      return;
    }
    _safeLaunch(() async {
      await _repository.deleteResident(member);
      onResult(true, msg('कर्मचारी हटवला!', 'स्टाफ हटाया गया!', 'Staff member removed.'));
    });
  }

  // ------------------------------------------------------------- Roles

  String get _actorLabel {
    final user = _currentUser;
    if (user == null) return '';
    return '${user.ownerName.trim()} (${user.flatNumber})';
  }

  RoleChange _roleChangeEntry(Resident member, UserRole newRole, SocietyRole? customRole) => RoleChange(
        flatNumber: member.flatNumber,
        memberName: member.ownerName,
        fromRole: member.role,
        toRole: newRole,
        fromCustomRoleName: member.customRoleName,
        toCustomRoleName: customRole?.name.trim() ?? '',
        changedBy: _actorLabel,
      );

  /// The words for why a role change was refused, in the current language.
  String roleRefusalMessage(RoleChangeRefusal reason) {
    switch (reason) {
      case RoleChangeRefusal.NOT_CHAIRMAN:
        return msg(
          'फक्त अध्यक्षच भूमिका बदलू शकतात.',
          'केवल अध्यक्ष ही भूमिका बदल सकते हैं।',
          'Only the Chairman can change roles.',
        );
      case RoleChangeRefusal.WRONG_KIND:
        return msg(
          'सभासद व कर्मचारी यांच्या भूमिका एकमेकांत बदलता येत नाहीत.',
          'सदस्य और स्टाफ की भूमिकाएं आपस में नहीं बदली जा सकतीं।',
          'A member cannot be given a staff role, or a staff member a committee role.',
        );
      case RoleChangeRefusal.VACANT_UNIT:
        return msg(
          'रिकाम्या सदनिकेला पद देता येत नाही.',
          'खाली इकाई को पद नहीं दिया जा सकता।',
          'A vacant unit cannot hold a committee post.',
        );
      case RoleChangeRefusal.CHAIRMAN_HANDOVER_ONLY:
        return msg(
          'अध्यक्षपद फक्त हस्तांतरित करता येते - "अध्यक्षपद सोपवा" वापरा.',
          'अध्यक्ष पद केवल सौंपा जा सकता है - "अध्यक्ष पद सौंपें" का उपयोग करें।',
          'The Chairman\'s post can only be handed over - use "Hand over Chairman".',
        );
      case RoleChangeRefusal.ROLE_UNAVAILABLE:
        return msg('ही भूमिका आता उपलब्ध नाही.', 'यह भूमिका अब उपलब्ध नहीं है।', 'That role is no longer available.');
      case RoleChangeRefusal.UNCHANGED:
        return msg('ही भूमिका आधीच आहे.', 'यह भूमिका पहले से है।', 'They already hold that role.');
    }
  }

  /// Changes one member's or staff member's role on their existing record.
  void changeMemberRole(Resident member, UserRole newRole, {SocietyRole? customRole, ResultCallback onResult = _noop}) {
    final refusal = checkRoleChange(actor: _currentUser, target: member, newRole: newRole, customRole: customRole);
    if (refusal != null) {
      onResult(false, roleRefusalMessage(refusal));
      return;
    }
    final role = customRole?.baseRole ?? newRole;
    _safeLaunch(() async {
      await _repository.changeMemberRole(member, role, customRole, _roleChangeEntry(member, role, customRole));
      onResult(true, msg('भूमिका बदलली.', 'भूमिका बदली गई।', 'Role updated.'));
    });
  }

  /// The Chairman gives the post to [successor] and takes [myNewRole].
  void handOverChairman(Resident successor, UserRole myNewRole, {ResultCallback onResult = _noop}) {
    final me = _currentUser;
    final refusal = checkChairmanHandover(actor: me, target: successor, actorNewRole: myNewRole);
    if (refusal != null || me == null) {
      onResult(false, roleRefusalMessage(refusal ?? RoleChangeRefusal.NOT_CHAIRMAN));
      return;
    }
    _safeLaunch(() async {
      await _repository.handOverChairman(
        from: me,
        fromNewRole: myNewRole,
        to: successor,
        fromEntry: _roleChangeEntry(me, myNewRole, null),
        toEntry: _roleChangeEntry(successor, UserRole.CHAIRMAN, null),
      );
      onResult(
        true,
        msg(
          'अध्यक्षपद ${successor.ownerName} यांना सोपवले.',
          'अध्यक्ष पद ${successor.ownerName} को सौंपा गया।',
          'The Chairman\'s post was handed over to ${successor.ownerName}.',
        ),
      );
    });
  }

  /// Creates or edits a society staff role.
  void saveRole(SocietyRole role, {ResultCallback onResult = _noop}) {
    if (!canManageRoles) {
      onResult(false, roleRefusalMessage(RoleChangeRefusal.NOT_CHAIRMAN));
      return;
    }
    final name = role.name.trim();
    if (name.isEmpty) {
      onResult(false, msg('भूमिकेचे नाव लिहा.', 'भूमिका का नाम लिखें।', 'Enter a name for the role.'));
      return;
    }
    if (!role.baseRole.isStaff) {
      onResult(false, roleRefusalMessage(RoleChangeRefusal.WRONG_KIND));
      return;
    }
    final clash = _roles.any((r) => r.docId != role.docId && _eqIgnoreCase(r.name.trim(), name));
    final builtIn = UserRole.values.any((r) => _eqIgnoreCase(r.name.replaceAll('_', ' '), name));
    if (clash || builtIn) {
      onResult(
        false,
        msg('या नावाची भूमिका आधीच आहे.', 'इस नाम की भूमिका पहले से है।', 'A role with that name already exists.'),
      );
      return;
    }
    final holders =
        role.docId.isEmpty ? const <Resident>[] : _residents.where((r) => r.customRoleId == role.docId).toList();
    _safeLaunch(() async {
      await _repository.saveRole(
        role.copyWith(name: name, menus: normaliseRoleMenus(role.menus)),
        holders: holders,
      );
      onResult(true, msg('भूमिका जतन केली.', 'भूमिका सहेजी गई।', 'Role saved.'));
    });
  }

  /// Deletes a staff role nobody holds any more.
  void deleteRole(SocietyRole role, {ResultCallback onResult = _noop}) {
    if (!canManageRoles) {
      onResult(false, roleRefusalMessage(RoleChangeRefusal.NOT_CHAIRMAN));
      return;
    }
    final held = _residents.where((r) => r.customRoleId == role.docId).length;
    if (held > 0) {
      onResult(
        false,
        msg(
          '$held कर्मचाऱ्यांकडे ही भूमिका आहे. आधी त्यांची भूमिका बदला.',
          '$held स्टाफ के पास यह भूमिका है। पहले उनकी भूमिका बदलें।',
          '$held staff member(s) still hold this role. Change their role first.',
        ),
      );
      return;
    }
    _safeLaunch(() async {
      await _repository.deleteRole(role);
      onResult(true, msg('भूमिका हटवली.', 'भूमिका हटाई गई।', 'Role deleted.'));
    });
  }

  // ------------------------------------------------------ Subscription

  /// Asks MultiSarv to activate [plan] for the society.
  void requestSubscription(SubscriptionPlan plan, String note, {ResultCallback onResult = _noop}) {
    final user = _currentUser;
    if (!canRequestSubscription || user == null) {
      onResult(
        false,
        msg(
          'फक्त अध्यक्षच प्लॅनची विनंती करू शकतात.',
          'केवल अध्यक्ष ही प्लान का अनुरोध कर सकते हैं।',
          'Only the Chairman can request a plan.',
        ),
      );
      return;
    }
    if (pendingSubscriptionRequest != null) {
      onResult(
        false,
        msg(
          'एक विनंती आधीच प्रलंबित आहे.',
          'एक अनुरोध पहले से लंबित है।',
          'A request is already waiting on MultiSarv.',
        ),
      );
      return;
    }
    _safeLaunch(() async {
      await _repository.insertSubscriptionRequest(
        SubscriptionRequest(
          planId: plan.id,
          planName: plan.name,
          requestedBy: user.ownerName.trim(),
          requestedFlat: user.flatNumber,
          contactNumber: user.contactNumber,
          note: note,
        ),
      );
      onResult(
        true,
        msg(
          'विनंती पाठवली. MultiSarv टीम लवकरच संपर्क करेल.',
          'अनुरोध भेजा गया। MultiSarv टीम जल्द संपर्क करेगी।',
          'Request sent. The MultiSarv team will be in touch shortly.',
        ),
      );
    });
  }

  // ------------------------------------------------------------ Profiles

  /// Saves the Society Profile in full. New wings/floors are expanded into
  /// vacant units; existing units are never touched.
  void saveSocietyConfiguration(SocietyInfo updated, {ResultCallback onResult = _noop}) {
    if (!canEditSociety) {
      onResult(false, msg('फक्त अध्यक्षच संस्थेची माहिती बदलू शकतात.', 'केवल अध्यक्ष ही सोसायटी की जानकारी बदल सकते हैं।', 'Only the Chairman can edit society details.'));
      return;
    }
    _safeLaunch(() async {
      final current = await _repository.getSocietyInfoSync();
      if (current == null) {
        onResult(false, 'Society not loaded yet.');
        return;
      }

      // An empty section of the form means "leave this alone", never "wipe it".
      var wingPlans = updated.wingPlans.where((p) => p.totalUnits > 0).toList();
      if (wingPlans.isEmpty) wingPlans = current.wingPlans;
      var wingNames = updated.wingNames;
      if (wingNames.isEmpty) wingNames = wingPlans.map((p) => p.name).toList();
      if (wingNames.isEmpty) wingNames = current.wingNames;
      final newUnits = [for (final plan in wingPlans) ..._toVacantUnits(plan)];

      // A type that already carries a tariff keeps it; only new types come in at zero.
      var mergedUnitTypes = updated.unitTypes.where((t) => t.name.trim().isNotEmpty).map((type) {
        if (type.monthlyTariff > 0.0) return type;
        final known = current.unitTypes.where((t) => _eqIgnoreCase(t.name, type.name));
        return type.copyWith(monthlyTariff: known.isEmpty ? 0.0 : known.first.monthlyTariff);
      }).toList();
      if (mergedUnitTypes.isEmpty) mergedUnitTypes = current.unitTypes;

      final totalWings = [wingNames.length, wingPlans.length, current.totalWings].reduce((a, b) => a > b ? a : b);
      await _repository.updateSociety(updated.copyWith(
        // The login code, the document identity and the treasury balances
        // belong to the ledger, not to this form.
        societyCode: current.societyCode,
        docId: current.docId,
        id: current.id,
        wingNames: wingNames,
        wingPlans: wingPlans,
        totalWings: totalWings,
        totalFlats: newUnits.length > current.totalFlats ? newUnits.length : current.totalFlats,
        unitTypes: mergedUnitTypes,
        tariff1BHK: _tariffOf(mergedUnitTypes, '1 BHK', current.tariff1BHK),
        tariff2BHK: _tariffOf(mergedUnitTypes, '2 BHK', current.tariff2BHK),
        tariff3BHK: _tariffOf(mergedUnitTypes, '3 BHK', current.tariff3BHK),
        tariffPenthouse: _tariffOf(mergedUnitTypes, 'Penthouse', current.tariffPenthouse),
        tariffShop: _tariffOf(mergedUnitTypes, 'Shop', current.tariffShop),
      ));

      if (newUnits.isNotEmpty) {
        await _repository.insertVacantUnits(current.societyCode, newUnits);
      }

      onResult(true, msg('संस्थेची माहिती जतन केली!', 'सोसायटी जानकारी सहेजी गई!', 'Society profile saved.'));
    });
  }

  /// Saves the society's unit types and their monthly tariffs from the Tariff
  /// screen, keeping the fixed BHK slabs in step.
  void updateUnitTypeTariffs(List<UnitTypeConfig> types, {ResultCallback onResult = _noop}) {
    if (!canEditSociety && !canEditAccounts) {
      onResult(false, _accountsReadOnlyMessage());
      return;
    }
    _safeLaunch(() async {
      final soc = await _repository.getSocietyInfoSync();
      if (soc == null) {
        onResult(false, 'Society not loaded yet.');
        return;
      }
      final cleaned = types.where((t) => t.name.trim().isNotEmpty).toList();
      await _repository.updateSociety(soc.copyWith(
        unitTypes: cleaned,
        tariff1BHK: _tariffOf(cleaned, '1 BHK', soc.tariff1BHK),
        tariff2BHK: _tariffOf(cleaned, '2 BHK', soc.tariff2BHK),
        tariff3BHK: _tariffOf(cleaned, '3 BHK', soc.tariff3BHK),
        tariffPenthouse: _tariffOf(cleaned, 'Penthouse', soc.tariffPenthouse),
        tariffShop: _tariffOf(cleaned, 'Shop', soc.tariffShop),
      ));
      onResult(true, msg('मासिक दरपत्रक अद्ययावत केले!', 'मासिक दर सूची अपडेट की गई!', 'Tariff slabs updated successfully!'));
    });
  }

  /// Updates the signed-in user's own profile. A member may only fill in what
  /// the society never recorded; committee and staff edit freely.
  void updateMyProfile({
    required String ownerName,
    required String phone,
    required String email,
    required String photoBase64,
    String coApplicantName = '',
    ResultCallback onResult = _noop,
  }) {
    final current = _currentUser;
    if (current == null) {
      onResult(false, 'Not signed in.');
      return;
    }
    _safeLaunch(() async {
      final updated = current.withProfileEdits(
        ownerName: ownerName,
        coApplicantName: coApplicantName,
        contactNumber: phone,
        email: email,
        photoBase64: photoBase64,
        addOnly: current.role.isMember,
      );

      if (updated.sameProfileAs(current)) {
        onResult(true, msg('भरण्यासारखे काही नवीन नव्हते.', 'भरने के लिए कुछ नया नहीं था।', 'There was nothing new to fill in.'));
        return;
      }

      await _repository.updateResident(updated);
      _currentUser = updated;
      _notify();
      onResult(true, msg('प्रोफाइल जतन केले!', 'प्रोफ़ाइल सहेजी गई!', 'Profile saved.'));
    });
  }

  // ---------------------------------------- Emergency contacts (chairman)

  void saveEmergencyContact(EmergencyContact contact, {ResultCallback onResult = _noop}) {
    if (!canEditSociety) {
      onResult(false, msg('फक्त अध्यक्षच तातडीचे संपर्क व्यवस्थापित करू शकतात.', 'केवल अध्यक्ष ही आपातकालीन संपर्क प्रबंधित कर सकते हैं।', 'Only the Chairman can manage emergency contacts.'));
      return;
    }
    if (contact.titleEn.trim().isEmpty) {
      onResult(false, msg('संपर्काचे नाव लिहा.', 'संपर्क का नाम लिखें।', 'Enter a name for the contact.'));
      return;
    }
    // An emergency number that cannot be dialled is the one thing this list
    // must never hold.
    final digits = contact.phoneNumber.replaceAll(RegExp(r'[^0-9]'), '');
    if (digits.length < EMERGENCY_PHONE_MIN_DIGITS || digits.length > EMERGENCY_PHONE_MAX_DIGITS) {
      onResult(false, msg('वैध दूरध्वनी क्रमांक टाका (३ ते १५ अंक).', 'मान्य फोन नंबर डालें (3 से 15 अंक)।', 'Enter a valid phone number - between 3 and 15 digits.'));
      return;
    }
    _safeLaunch(() async {
      await _repository.saveEmergencyContact(contact);
      onResult(true, msg('संपर्क जतन केला!', 'संपर्क सहेजा गया!', 'Contact saved.'));
    });
  }

  void deleteEmergencyContact(EmergencyContact contact, {ResultCallback onResult = _noop}) {
    if (!canEditSociety) {
      onResult(false, msg('फक्त अध्यक्षच तातडीचे संपर्क व्यवस्थापित करू शकतात.', 'केवल अध्यक्ष ही आपातकालीन संपर्क प्रबंधित कर सकते हैं।', 'Only the Chairman can manage emergency contacts.'));
      return;
    }
    _safeLaunch(() async {
      await _repository.deleteEmergencyContact(contact);
      onResult(true, msg('संपर्क हटवला!', 'संपर्क हटाया गया!', 'Contact removed.'));
    });
  }

  void updatePin(String newPin) {
    _safeLaunch(() async {
      final cur = _currentUser;
      if (cur != null) {
        await _repository.updateResidentPin(cur.flatNumber, newPin);
        _currentUser = cur.copyWith(pin: newPin);
        showMessage(_language == AppLanguage.marathi ? 'सुरक्षा पिन बदलला!' : 'Security PIN changed successfully!');
      }
    });
  }

  // ------------------------------------------------- Export summary text

  /// Printable / shareable balance sheet & accounting summary.
  String generateExportSummary() {
    final soc = _societyInfo;
    final fin = financialOverview;
    final dateStr = formatDate(DateTime.now(), 'dd MMMM yyyy, hh:mm a');
    final months = billingMonths(soc);

    final b = StringBuffer()
      ..writeln('=====================================================')
      ..writeln('       ${soc?.name ?? 'Housing Society'}')
      ..writeln('       Registration No: ${soc?.registrationNumber ?? 'N/A'}')
      ..writeln('       Address: ${soc?.address ?? 'N/A'}')
      ..writeln('=====================================================')
      ..writeln('GENERAL BALANCE SHEET & FINANCIAL STATEMENT')
      ..writeln('Generated on: $dateStr')
      // The period the society actually bills for, not a fixed year.
      ..writeln('Billing Period: ${months.isEmpty ? '-' : months.last} to ${months.isEmpty ? '-' : months.first}')
      ..writeln('-----------------------------------------------------')
      ..writeln('TREASURY & BALANCES:')
      ..writeln('  • Net Treasury Fund   : ₹${fmt2(fin.netTreasuryBalance)}')
      ..writeln('  • Bank Balance        : ₹${fmt2(fin.bankBalance)} (${soc?.bankAccountName ?? 'Bank'})')
      ..writeln('  • Cash in Hand        : ₹${fmt2(fin.cashBalance)}')
      ..writeln('  • Total Outstanding   : ₹${fmt2(fin.totalOutstandingDues)}')
      ..writeln('-----------------------------------------------------')
      ..writeln('INCOME SUMMARY:')
      ..writeln('  • Maintenance Receipts: ₹${fmt2(fin.totalMaintenanceCollected)}')
      ..writeln('  • Non-Fee Revenues    : ₹${fmt2(fin.totalNonFeeRevenue)}')
      ..writeln('  • Gross Total Income  : ₹${fmt2(fin.totalGrossIncome)}')
      ..writeln('-----------------------------------------------------')
      ..writeln('EXPENDITURE SUMMARY:')
      ..writeln('  • Total Operational Exp: ₹${fmt2(fin.totalOperatingExpenses)}')
      ..writeln('  • NET SURPLUS / (DEF)  : ₹${fmt2(fin.netSurplus)}')
      ..writeln('-----------------------------------------------------')
      ..writeln('TOP OPERATING EXPENSES:');
    for (final exp in _expenses.take(5)) {
      b.writeln('  - [${exp.date}] ${exp.category.name}: ₹${fmt2(exp.amount)} (${exp.title})');
    }
    b
      ..writeln('-----------------------------------------------------')
      ..writeln('MEMBER DUES SUMMARY:')
      ..writeln('  - Total Flats: ${soc?.totalFlats ?? 0} | Paid: ${fin.paidFlatsCount} | Pending: ${fin.pendingFlatsCount}')
      ..writeln('=====================================================')
      ..writeln('Verified & Certified by:')
      ..writeln('Hon. Chairman / Hon. Secretary / Hon. Treasurer')
      ..writeln('SocietySarv Digital Accounting Management System');
    return b.toString();
  }
}
