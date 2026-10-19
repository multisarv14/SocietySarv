// Port of ui/screens/ProfileScreens.kt.
//
// Public API:
//   Future<void> showSocietyProfileDialog(BuildContext context, {required VoidCallback onEdit})
//   class SocietyProfileDialog ({Key? key, required VoidCallback onDismiss, required VoidCallback onEdit})
//   class SocietyProfileEditorScreen ({Key? key, required VoidCallback onClose})
//   Future<void> showUserProfileDialog(BuildContext context)
//   class UserProfileDialog ({Key? key, required VoidCallback onDismiss, required VoidCallback onLogout})
//   class ProfileSection ({Key? key, required String title, required Widget child})

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../data/models.dart';
import '../../l10n/app_strings.dart';
import '../../l10n/location_data.dart';
import '../../state/society_view_model.dart';
import '../../util/format.dart';
import '../../util/image_blob.dart';
import '../components/common_components.dart';
import '../theme/app_colors.dart';
import 'auth_screen.dart';

String _digitsOnly(String s, int max) {
  final d = s.replaceAll(RegExp(r'[^0-9]'), '');
  return d.length > max ? d.substring(0, max) : d;
}

String _unitTypesText(List<UnitTypeConfig> types) => types
    .where((t) => t.name.trim().isNotEmpty)
    .map((t) => t.monthlyTariff > 0 ? '${t.name} (₹${t.monthlyTariff.toInt()})' : t.name)
    .join(', ');

String _orDash(String s) => s.trim().isEmpty ? '-' : s;

// ---------------------------------------------------------- Society Profile

/// Society Profile, opened by tapping the society logo in the top bar.
///
/// Everyone can read it; only the Chairman gets the Edit button, and the
/// ViewModel refuses the write for anyone else regardless.
///
/// [onEdit] opens the editing wizard. It is hosted by the caller as a
/// full-screen screen rather than nested in a dialog window, so the Cancel /
/// Save row is never laid out under the gesture bar.
Future<void> showSocietyProfileDialog(BuildContext context, {required VoidCallback onEdit}) async {
  final vm = context.read<SocietyViewModel>();
  if (vm.societyInfo == null) return;
  await showAppDialog<void>(
    context: context,
    builder: (dialogContext) => SocietyProfileDialog(
      onDismiss: () => Navigator.of(dialogContext).pop(),
      onEdit: () {
        Navigator.of(dialogContext).pop();
        onEdit();
      },
    ),
  );
}

class SocietyProfileDialog extends StatelessWidget {
  const SocietyProfileDialog({super.key, required this.onDismiss, required this.onEdit});

  final VoidCallback onDismiss;
  final VoidCallback onEdit;

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final society = vm.societyInfo;
    final language = vm.language;
    final tt = Theme.of(context).textTheme;
    final cs = Theme.of(context).colorScheme;
    if (society == null) {
      WidgetsBinding.instance.addPostFrameCallback((_) => onDismiss());
      return const SizedBox.shrink();
    }
    final canEdit = vm.currentUser?.role == UserRole.CHAIRMAN;
    const divider = Divider(height: 16);

    return AlertDialog(
      insetPadding: dialogInsetPadding(context),
      title: Row(
        children: [
          Expanded(
            child: Text(tr('society_profile', language), style: tt.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
          ),
          if (canEdit)
            TextButton.icon(
              key: const Key('edit_society_profile_btn'),
              onPressed: onEdit,
              icon: const Icon(Icons.edit, color: AppColors.tealPrimary),
              label: Text(tr('edit_profile', language), style: const TextStyle(color: AppColors.tealPrimary)),
            ),
        ],
      ),
      content: SizedBox(
        width: double.maxFinite,
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                children: [
                  SocietyLogoMark(logoBase64: society.logoBase64, size: 56),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(society.name, style: tt.titleSmall?.copyWith(fontWeight: FontWeight.bold)),
                        Text(
                          'Login Code: ${society.societyCode}',
                          style: tt.bodySmall?.copyWith(color: AppColors.tealPrimary),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const Divider(height: 20),
              _ProfileRow('Registration No.', society.registrationNumber),
              _ProfileRow(
                'Location',
                [society.city, society.state, society.country].where((s) => s.trim().isNotEmpty).join(', '),
              ),
              _ProfileRow('Address', society.address),
              _ProfileRow('PIN Code', society.pinCode),
              _ProfileRow('Contact', society.contactNumber),
              _ProfileRow('Email', society.email),
              divider,
              _ProfileRow(
                'Wings',
                society.wingNames.isEmpty ? society.totalWings.toString() : society.wingNames.join(', '),
              ),
              _ProfileRow('Total Units', society.totalFlats.toString()),
              for (final plan in society.wingPlans)
                _ProfileRow(
                  'Wing ${plan.name}',
                  '${plan.totalFloors} floors • ${plan.totalUnits} units'
                      '${plan.unitCounts.isEmpty ? '' : ' (${plan.unitCounts.map((c) => '${c.unitType} × ${c.quantity}').join(', ')})'}',
                ),
              _ProfileRow('Unit Types', _unitTypesText(society.unitTypes)),
              _ProfileRow(
                'Billing Starts',
                society.billingStartMonth.trim().isEmpty ? 'Not set' : society.billingStartMonth,
              ),
              divider,
              _ProfileRow('Bank', society.bankAccountName),
              _ProfileRow('Account No.', society.bankAccountNumber),
              _ProfileRow('IFSC', society.ifscCode),
              if (!canEdit) ...[
                const SizedBox(height: 10),
                Text(
                  tr('chairman_only_edit', language),
                  key: const Key('chairman_only_note'),
                  style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
                ),
              ],
            ],
          ),
        ),
      ),
      actions: [
        AppButton.filled(
          style: FilledButton.styleFrom(backgroundColor: AppColors.tealPrimary),
          onPressed: onDismiss,
          child: Text(tr('close', language)),
        ),
      ],
    );
  }
}

// ------------------------------------------------- Society Profile editor

/// Society Details, Unit Details, Floor Inventory, then Parking & Review.
const _editorSteps = 4;

/// The society's registration wizard, reopened for editing.
///
/// Same shape as registration - one step at a time, a progress bar, Back and
/// Save & Continue - and the same fields: society details with the billing start
/// month, wings, floor inventory, unit types, and parking bays. Each step is
/// blocked until it is valid, exactly as registration blocks it, so a
/// half-filled society can never be saved.
///
/// Office Bearers is the one registration step missing: the chairman already
/// exists and is edited under Residents > Members. The login code is shown but
/// not editable - members sign in against it.
class SocietyProfileEditorScreen extends StatelessWidget {
  const SocietyProfileEditorScreen({super.key, required this.onClose});

  final VoidCallback onClose;

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final society = vm.societyInfo;
    if (society == null) return const SizedBox.shrink();
    return _SocietyRegistrationEditor(
      society: society,
      members: vm.members,
      language: vm.language,
      onRenameWingOnUnits: (from, to) =>
          vm.renameWingOnUnits(from, to, onResult: (_, message) => vm.showMessage(message)),
      onDeleteWingWithUnits: (wing) =>
          vm.deleteWingWithUnits(wing, onResult: (_, message) => vm.showMessage(message)),
      onCancel: onClose,
      onSave: (updated, onError) {
        vm.saveSocietyConfiguration(updated, onResult: (ok, message) {
          if (ok) {
            vm.showMessage(message);
            onClose();
          } else {
            onError(message);
          }
        });
      },
    );
  }
}

class _SocietyRegistrationEditor extends StatefulWidget {
  const _SocietyRegistrationEditor({
    required this.society,
    required this.members,
    required this.language,
    required this.onRenameWingOnUnits,
    required this.onDeleteWingWithUnits,
    required this.onCancel,
    required this.onSave,
  });

  final SocietyInfo society;
  final List<Resident> members;
  final AppLanguage language;
  final void Function(String from, String to) onRenameWingOnUnits;
  final ValueChanged<String> onDeleteWingWithUnits;
  final VoidCallback onCancel;
  final void Function(SocietyInfo updated, ValueChanged<String> onError) onSave;

  @override
  State<_SocietyRegistrationEditor> createState() => _SocietyRegistrationEditorState();
}

class _SocietyRegistrationEditorState extends State<_SocietyRegistrationEditor> {
  int _step = 1;
  String? _errorMessage;

  // 1. Society Details
  late String _name;
  late String _regNumber;
  late String _country;
  late String _state;
  late String _city;
  late String _address;
  late String _pinCode;
  late String _phone;
  late String _email;
  late String _logoBase64;
  late String _bankName;
  late String _bankAccount;
  late String _ifsc;
  late String _upiId;
  late String _paymentQrBase64;
  late String _billingStartMonth;

  // 2. Unit Details
  late String _totalWings;
  late String _wingNamesRaw;

  // 3. Floor Inventory
  late List<WingPlan> _wingPlans;

  // 4. Parking & Review
  // Opening balances: what the society held before it started keeping books
  // here. Everything after that is worked out from the bills and vouchers.
  late String _openingBank;
  late String _openingCash;
  late String _coveredParking;
  late String _openParking;

  // Stray wings the chairman has dismissed in this sitting. They carry no
  // units, so there is nothing to write - they just stop being listed.
  Set<String> _forgottenWings = {};

  @override
  void initState() {
    super.initState();
    _loadFrom(widget.society);
  }

  @override
  void didUpdateWidget(covariant _SocietyRegistrationEditor oldWidget) {
    super.didUpdateWidget(oldWidget);
    // Kotlin keyed every field on the society: a fresh copy from Firestore
    // starts the form over from it.
    if (!identical(oldWidget.society, widget.society)) _loadFrom(widget.society);
  }

  void _loadFrom(SocietyInfo s) {
    _name = s.name;
    _regNumber = s.registrationNumber;
    _country = s.country.trim().isEmpty ? LocationData.defaultCountry : s.country;
    _state = s.state;
    _city = s.city;
    _address = s.address;
    _pinCode = s.pinCode;
    _phone = s.contactNumber;
    _email = s.email;
    _logoBase64 = s.logoBase64;
    _bankName = s.bankAccountName;
    _bankAccount = s.bankAccountNumber;
    _ifsc = s.ifscCode;
    _upiId = s.upiId;
    _paymentQrBase64 = s.paymentQrBase64;
    _billingStartMonth = s.billingStartMonth.trim().isEmpty ? currentMonthLabel() : s.billingStartMonth;
    _totalWings = s.wingNames.isNotEmpty
        ? s.wingNames.length.toString()
        : (s.totalWings > 0 ? s.totalWings.toString() : '');
    _wingNamesRaw = s.wingNames.join(', ');
    _wingPlans = s.wingPlans;
    _openingBank = s.currentBalanceBank != 0.0 ? s.currentBalanceBank.toInt().toString() : '';
    _openingCash = s.currentBalanceCash != 0.0 ? s.currentBalanceCash.toInt().toString() : '';
    _coveredParking = s.coveredParkingCount > 0 ? s.coveredParkingCount.toString() : '';
    _openParking = s.openParkingCount > 0 ? s.openParkingCount.toString() : '';
    _forgottenWings = {};
  }

  void _set(VoidCallback fn) => setState(fn);

  /// The same gate registration uses, step for step.
  String? _stepError(List<String> wingNames, List<UnitTypeConfig> unitTypes) {
    final wingCount = int.tryParse(_totalWings) ?? 0;
    switch (_step) {
      case 1:
        if (_name.trim().isEmpty) return 'Enter the society name.';
        if (_pinCode.trim().isNotEmpty && _pinCode.length != 6) return 'PIN code must be exactly 6 digits.';
        if (_email.trim().isNotEmpty && !_email.contains('@')) return 'Enter a valid society email id.';
        return null;
      /*
       * Editing is not registering.
       *
       * A society registered before floor inventory existed has no wing names
       * and no plans stored, and it must still be able to correct a phone
       * number without being forced to re-enter its whole structure. So these
       * two steps only object to a half-filled answer - never to an empty one,
       * which simply means "leave this as it is".
       */
      case 2:
        // Nothing typed and nothing ever stored: this society predates wing
        // names, and correcting a phone number must not demand them.
        if (_wingNamesRaw.trim().isEmpty && widget.society.wingNames.isEmpty) return null;
        if (wingCount <= 0) return 'Enter how many wings the society has.';
        if (wingNames.isEmpty) return 'Enter the wing names, separated by commas.';
        if (wingNames.length != wingCount) {
          return 'You entered ${wingNames.length} wing name(s) for $wingCount wing(s).';
        }
        return null;
      case 3:
        if (unitTypes.isNotEmpty && !unitTypes.any((t) => t.name.trim().isNotEmpty)) {
          return 'Give every unit type a name, or remove the blank ones.';
        }
        return null;
      default:
        return null;
    }
  }

  @override
  Widget build(BuildContext context) {
    final society = widget.society;
    final members = widget.members;
    final language = widget.language;
    final tt = Theme.of(context).textTheme;
    final cs = Theme.of(context).colorScheme;

    final wingNames = parseWingNames(_wingNamesRaw);
    // Unit types follow the wings they were entered against; their tariffs are
    // set under Maintenance > Tariff, not here.
    final unitTypes = unitTypesOf(_wingPlans);
    // Wings the units carry that the society never listed - invisible until
    // now, which is why they could not be tidied up.
    final seen = <String>{};
    final strayWings = <String>[];
    for (final m in members) {
      final w = m.wing.trim();
      if (w.isEmpty || w.toUpperCase() == 'STAFF') continue;
      if (!seen.add(w.toLowerCase())) continue;
      if (wingNames.any((n) => n.toLowerCase() == w.toLowerCase())) continue;
      strayWings.add(w);
    }
    strayWings.sort();
    final plannedUnits = _wingPlans.fold<int>(0, (s, p) => s + p.totalUnits);
    final stepError = _stepError(wingNames, unitTypes);

    final Widget stepBody;
    switch (_step) {
      case 1:
        stepBody = SocietyDetailsStep(
          language: language,
          societyName: _name,
          onSocietyName: (v) => _set(() => _name = v),
          regNumber: _regNumber,
          onRegNumber: (v) => _set(() => _regNumber = v),
          societyCode: society.societyCode,
          onSocietyCode: (_) {},
          country: _country,
          onCountry: (v) => _set(() {
            _country = v;
            _state = '';
            _city = '';
          }),
          state: _state,
          onState: (v) => _set(() {
            _state = v;
            _city = '';
          }),
          city: _city,
          onCity: (v) => _set(() => _city = v),
          address: _address,
          onAddress: (v) => _set(() => _address = v),
          pinCode: _pinCode,
          onPinCode: (v) => _set(() => _pinCode = _digitsOnly(v, 6)),
          phone: _phone,
          onPhone: (v) => _set(() => _phone = v),
          email: _email,
          onEmail: (v) => _set(() => _email = v),
          logoBase64: _logoBase64,
          onLogo: (v) => _set(() => _logoBase64 = v),
          bankName: _bankName,
          onBankName: (v) => _set(() => _bankName = v),
          bankAccount: _bankAccount,
          onBankAccount: (v) => _set(() => _bankAccount = v),
          ifscCode: _ifsc,
          onIfsc: (v) => _set(() => _ifsc = v),
          upiId: _upiId,
          onUpiId: (v) => _set(() => _upiId = v),
          paymentQrBase64: _paymentQrBase64,
          onPaymentQr: (v) => _set(() => _paymentQrBase64 = v),
          initialBankBalance: '',
          onInitialBalance: (_) {},
          billingStartMonth: _billingStartMonth,
          onBillingStartMonth: (v) => _set(() => _billingStartMonth = v),
          codeEditable: false,
          showOpeningBalance: false,
        );
      case 2:
        stepBody = UnitDetailsStep(
          language: language,
          totalWings: _totalWings,
          onTotalWings: (v) => _set(() => _totalWings = v),
          wingNamesRaw: _wingNamesRaw,
          onWingNamesRaw: (v) => _set(() => _wingNamesRaw = v),
          parsedWingNames: wingNames,
          strayWings: strayWings.where((w) => !_forgottenWings.contains(w)).toList(),
          unitCountFor: (wing) => members.where((m) => m.wing.toLowerCase() == wing.toLowerCase()).length,
          onRenameUnits: widget.onRenameWingOnUnits,
          onDeleteStrayWing: (w) => _set(() => _forgottenWings = {..._forgottenWings, w}),
          onDeleteWingWithUnits: widget.onDeleteWingWithUnits,
        );
      case 3:
        stepBody = FloorInventoryStep(
          language: language,
          wingNames: wingNames,
          wingPlans: _wingPlans,
          onWingPlans: (v) => _set(() => _wingPlans = v),
        );
      default:
        stepBody = _SocietyParkingReviewStep(
          society: society,
          wingNames: wingNames,
          wingPlans: _wingPlans,
          unitTypes: unitTypes,
          plannedUnits: plannedUnits,
          billingStartMonth: _billingStartMonth,
          coveredParking: _coveredParking,
          onCoveredParking: (v) => _set(() => _coveredParking = v),
          openParking: _openParking,
          onOpenParking: (v) => _set(() => _openParking = v),
          openingBank: _openingBank,
          onOpeningBank: (v) => _set(() => _openingBank = v),
          openingCash: _openingCash,
          onOpeningCash: (v) => _set(() => _openingCash = v),
        );
    }

    final blocker = _errorMessage ?? stepError;

    // Laid out exactly like the registration wizard - a plain full-screen
    // surface, not a dialog window - so the Cancel / Save row sits above the
    // gesture bar instead of being clipped off the bottom of the display.
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop) widget.onCancel();
      },
      child: Scaffold(
        backgroundColor: cs.surface,
        resizeToAvoidBottomInset: true,
        body: SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Row(
                  children: [
                    IconButton(
                      key: const Key('society_editor_back_btn'),
                      tooltip: 'Back',
                      onPressed: () {
                        if (_step > 1) {
                          setState(() => _step--);
                        } else {
                          widget.onCancel();
                        }
                      },
                      icon: const Icon(Icons.arrow_back),
                    ),
                    Expanded(
                      child: Text(
                        tr('society_profile', language),
                        style: tt.titleLarge?.copyWith(fontWeight: FontWeight.w800),
                      ),
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                      decoration: BoxDecoration(
                        color: AppColors.tealContainer,
                        borderRadius: BorderRadius.circular(20),
                      ),
                      child: Text(
                        '$_step / $_editorSteps',
                        style: tt.labelMedium?.copyWith(fontWeight: FontWeight.bold, color: AppColors.tealPrimary),
                      ),
                    ),
                  ],
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 8),
                  child: AnimatedProgressBar(value: _step / _editorSteps, color: AppColors.tealPrimary, height: 6),
                ),
                Expanded(
                  child: FadeThroughSwitcher(
                    child: SingleChildScrollView(
                      key: Key('society_editor_step_$_step'),
                      padding: const EdgeInsets.symmetric(vertical: 8),
                      child: stepBody,
                    ),
                  ),
                ),
                if (blocker != null)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 4),
                    child: Text(
                      blocker,
                      key: const Key('society_editor_error'),
                      style: tt.bodySmall?.copyWith(color: cs.error),
                    ),
                  ),
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 8),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      OutlinedButton(
                        key: const Key('society_editor_cancel_btn'),
                        onPressed: widget.onCancel,
                        child: Text(tr('cancel', language)),
                      ),
                      AppButton.filled(
                        key: const Key('save_society_profile_btn'),
                        style: FilledButton.styleFrom(backgroundColor: AppColors.tealPrimary),
                        onPressed: stepError != null
                            ? null
                            : () {
                                setState(() => _errorMessage = null);
                                if (_step < _editorSteps) {
                                  setState(() => _step++);
                                  return;
                                }
                                widget.onSave(
                                  society.copyWith(
                                    name: _name.trim(),
                                    registrationNumber: _regNumber.trim(),
                                    country: _country,
                                    state: _state,
                                    city: _city,
                                    address: _address.trim(),
                                    pinCode: _pinCode,
                                    contactNumber: _phone.trim(),
                                    email: _email.trim(),
                                    logoBase64: _logoBase64,
                                    bankAccountName: _bankName.trim(),
                                    bankAccountNumber: _bankAccount.trim(),
                                    ifscCode: _ifsc.trim().toUpperCase(),
                                    upiId: _upiId.trim(),
                                    paymentQrBase64: _paymentQrBase64,
                                    wingNames: wingNames,
                                    wingPlans: _wingPlans,
                                    unitTypes: unitTypes,
                                    coveredParkingCount: int.tryParse(_coveredParking) ?? 0,
                                    openParkingCount: int.tryParse(_openParking) ?? 0,
                                    currentBalanceBank: double.tryParse(_openingBank) ?? 0.0,
                                    currentBalanceCash: double.tryParse(_openingCash) ?? 0.0,
                                    billingStartMonth: _billingStartMonth.trim(),
                                  ),
                                  (message) {
                                    if (mounted) setState(() => _errorMessage = message);
                                  },
                                );
                              },
                        child: Text(
                          _step < _editorSteps ? tr('save_continue', language) : tr('save_profile', language),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// The editor's last step: how many parking bays the society has, and a summary
/// of everything the earlier steps are about to save.
class _SocietyParkingReviewStep extends StatelessWidget {
  const _SocietyParkingReviewStep({
    required this.society,
    required this.wingNames,
    required this.wingPlans,
    required this.unitTypes,
    required this.plannedUnits,
    required this.billingStartMonth,
    required this.coveredParking,
    required this.onCoveredParking,
    required this.openParking,
    required this.onOpenParking,
    required this.openingBank,
    required this.onOpeningBank,
    required this.openingCash,
    required this.onOpeningCash,
  });

  final SocietyInfo society;
  final List<String> wingNames;
  final List<WingPlan> wingPlans;
  final List<UnitTypeConfig> unitTypes;
  final int plannedUnits;
  final String billingStartMonth;
  final String coveredParking;
  final ValueChanged<String> onCoveredParking;
  final String openParking;
  final ValueChanged<String> onOpenParking;
  final String openingBank;
  final ValueChanged<String> onOpeningBank;
  final String openingCash;
  final ValueChanged<String> onOpeningCash;

  @override
  Widget build(BuildContext context) {
    final tt = Theme.of(context).textTheme;
    final cs = Theme.of(context).colorScheme;
    String amountOnly(String v) => v.replaceAll(RegExp(r'[^0-9.]'), '');

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const StepTitle('4. Parking & Review'),
        const SubHeading('Parking Bays'),
        Text(
          'How many bays of each kind the society has. Left blank, one bay '
          'per unit is offered.',
          style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
        ),
        const SizedBox(height: 8),
        Row(
          children: [
            Expanded(
              child: StateTextField(
                key: const Key('society_editor_covered_parking'),
                value: coveredParking,
                onChanged: (v) => onCoveredParking(_digitsOnly(v, 4)),
                label: 'Covered Bays',
                keyboardType: TextInputType.number,
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: StateTextField(
                key: const Key('society_editor_open_parking'),
                value: openParking,
                onChanged: (v) => onOpenParking(_digitsOnly(v, 4)),
                label: 'Open Bays',
                keyboardType: TextInputType.number,
              ),
            ),
          ],
        ),
        const SizedBox(height: 18),
        const SubHeading('Opening Balance'),
        Text(
          'What the society held before it started keeping books here. '
          'Everything after that is added up from its own bills and vouchers, '
          'so leave these at zero unless you are carrying a balance forward.',
          style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
        ),
        const SizedBox(height: 8),
        Row(
          children: [
            Expanded(
              child: StateTextField(
                key: const Key('society_editor_opening_bank'),
                value: openingBank,
                onChanged: (v) => onOpeningBank(amountOnly(v)),
                label: 'Opening Bank (₹)',
                placeholder: '0',
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: StateTextField(
                key: const Key('society_editor_opening_cash'),
                value: openingCash,
                onChanged: (v) => onOpeningCash(amountOnly(v)),
                label: 'Opening Cash (₹)',
                placeholder: '0',
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
              ),
            ),
          ],
        ),
        const SizedBox(height: 18),
        const SubHeading('Review'),
        const SizedBox(height: 6),
        Card(
          color: AppColors.surfaceVariant,
          elevation: 0,
          margin: EdgeInsets.zero,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                _ProfileRow('Login Code', society.societyCode),
                _ProfileRow('Wings', _orDash(wingNames.join(', '))),
                for (final plan in wingPlans)
                  _ProfileRow('Wing ${plan.name}', '${plan.totalFloors} floors • ${plan.totalUnits} units'),
                _ProfileRow('Units Planned', plannedUnits.toString()),
                _ProfileRow('Unit Types', _orDash(_unitTypesText(unitTypes))),
                _ProfileRow('Billing Starts', _orDash(billingStartMonth)),
              ],
            ),
          ),
        ),
        const SizedBox(height: 10),
        Text(
          'Saving creates any new wing or floor as vacant units. Units that '
          'already exist are never overwritten.',
          style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
        ),
      ],
    );
  }
}

// ------------------------------------------------------------ User Profile

/// The signed-in member's own profile, opened from their photo in the top bar.
/// Logout lives at the bottom of this sheet rather than in the top bar.
Future<void> showUserProfileDialog(BuildContext context) async {
  final vm = context.read<SocietyViewModel>();
  if (vm.currentUser == null) return;
  var logout = false;
  await showAppDialog<void>(
    context: context,
    builder: (dialogContext) => UserProfileDialog(
      onDismiss: () => Navigator.of(dialogContext).pop(),
      onLogout: () => logout = true,
    ),
  );
  if (logout) vm.logout();
}

class UserProfileDialog extends StatefulWidget {
  const UserProfileDialog({super.key, required this.onDismiss, required this.onLogout});

  final VoidCallback onDismiss;
  final VoidCallback onLogout;

  @override
  State<UserProfileDialog> createState() => _UserProfileDialogState();
}

class _UserProfileDialogState extends State<UserProfileDialog> {
  Resident? _loadedFor;
  bool _isEditing = false;
  String _name = '';
  String _coApplicant = '';
  String _phone = '';
  String _email = '';
  String _photoBase64 = '';
  String _newPin = '';
  String _confirmPin = '';
  String? _error;

  void _loadFrom(Resident user) {
    _loadedFor = user;
    _name = user.ownerName;
    _coApplicant = user.coApplicantName;
    _phone = user.contactNumber;
    _email = user.email;
    _photoBase64 = user.photoBase64;
  }

  void _save(SocietyViewModel vm) {
    if (_newPin.trim().isNotEmpty) {
      if (_newPin.length != 4) {
        setState(() => _error = 'The PIN must be exactly 4 digits.');
        return;
      }
      if (_newPin != _confirmPin) {
        setState(() => _error = 'The two PINs do not match.');
        return;
      }
    }
    final pin = _newPin;
    vm.updateMyProfile(
      ownerName: _name,
      phone: _phone,
      email: _email,
      photoBase64: _photoBase64,
      coApplicantName: _coApplicant,
      onResult: (ok, message) {
        if (!ok) {
          if (mounted) setState(() => _error = message);
          return;
        }
        if (pin.length == 4) {
          vm.updatePin(pin);
        } else {
          vm.showMessage(message);
        }
        if (mounted) {
          setState(() => _isEditing = false);
          widget.onDismiss();
        }
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final currentUser = vm.currentUser;
    final language = vm.language;
    final tt = Theme.of(context).textTheme;
    final cs = Theme.of(context).colorScheme;
    if (currentUser == null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) widget.onDismiss();
      });
      return const SizedBox.shrink();
    }
    // Kotlin keyed the form fields on the user record.
    if (!identical(_loadedFor, currentUser)) _loadFrom(currentUser);

    /*
     * A member fills in blanks; they do not rewrite the register.
     *
     * Their name, mobile number and the rest are the society's own record of
     * who lives where, so anything already on file is shown locked and only
     * what is genuinely missing can be typed into. The committee and the staff
     * edit their own details as before.
     */
    final isMemberProfile = currentUser.role == UserRole.RESIDENT || currentUser.role == UserRole.TENANT;
    final missingFields = [
      currentUser.ownerName,
      currentUser.coApplicantName,
      currentUser.contactNumber,
      currentUser.email,
      currentUser.photoBase64,
    ].where((s) => s.trim().isEmpty).length;
    final hasSomethingToAdd = !isMemberProfile || missingFields > 0;
    bool locked(String existing) => isMemberProfile && existing.trim().isNotEmpty;
    final roleLabel = getRoleLabel(currentUser.role, language);

    return AlertDialog(
      insetPadding: dialogInsetPadding(context),
      title: Row(
        children: [
          Expanded(
            child: Text(tr('user_profile', language), style: tt.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
          ),
          if (!_isEditing && hasSomethingToAdd)
            TextButton.icon(
              key: const Key('edit_user_profile_btn'),
              onPressed: () => setState(() => _isEditing = true),
              icon: const Icon(Icons.edit, color: AppColors.tealPrimary),
              label: Text(
                isMemberProfile ? tr('add_missing_details', language) : tr('edit_profile', language),
                style: const TextStyle(color: AppColors.tealPrimary),
              ),
            ),
        ],
      ),
      content: SizedBox(
        width: double.maxFinite,
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                children: [
                  Container(
                    decoration: const BoxDecoration(color: AppColors.tealPrimary, shape: BoxShape.circle),
                    child: MemberAvatar(photoBase64: _photoBase64, name: currentUser.ownerName, size: 60),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          _isEditing ? _name : currentUser.ownerName,
                          style: tt.titleSmall?.copyWith(fontWeight: FontWeight.bold),
                        ),
                        Text(
                          '${currentUser.flatNumber} • $roleLabel',
                          style: tt.bodySmall?.copyWith(color: AppColors.tealPrimary),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              // A photo already on file stays; a missing one can be added.
              // The chairman sees whatever is here in the member directory.
              if (_isEditing && !locked(currentUser.photoBase64)) ...[
                const SizedBox(height: 8),
                _LogoPickerButton(
                  label: _photoBase64.trim().isEmpty ? 'Upload Photo' : 'Change Photo',
                  onPicked: (v) => setState(() => _photoBase64 = v),
                  onCleared: () => setState(() => _photoBase64 = ''),
                  showClear: _photoBase64.trim().isNotEmpty,
                ),
              ],
              const Divider(height: 20),
              if (_isEditing) ...[
                if (isMemberProfile)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: Text(
                      tr('add_missing_only_note', language),
                      key: const Key('profile_add_only_note'),
                      style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
                    ),
                  ),
                _ProfileField(
                  label: 'Full Name',
                  value: _name,
                  enabled: !locked(currentUser.ownerName),
                  onChanged: (v) => setState(() => _name = v),
                ),
                _ProfileField(
                  label: 'Co-Applicant Name',
                  value: _coApplicant,
                  enabled: !locked(currentUser.coApplicantName),
                  onChanged: (v) => setState(() => _coApplicant = v),
                ),
                _ProfileField(
                  label: 'Mobile Number',
                  value: _phone,
                  keyboardType: TextInputType.phone,
                  enabled: !locked(currentUser.contactNumber),
                  onChanged: (v) => setState(() => _phone = _digitsOnly(v, 10)),
                ),
                _ProfileField(
                  label: 'Email',
                  value: _email,
                  keyboardType: TextInputType.emailAddress,
                  enabled: !locked(currentUser.email),
                  onChanged: (v) => setState(() => _email = v),
                ),
                const Divider(height: 16),
                Text('Change Login PIN (optional)', style: tt.titleSmall?.copyWith(fontWeight: FontWeight.bold)),
                const SizedBox(height: 6),
                _ProfileField(
                  label: 'New 4-Digit PIN',
                  value: _newPin,
                  keyboardType: TextInputType.number,
                  isPassword: true,
                  onChanged: (v) => setState(() => _newPin = _digitsOnly(v, 4)),
                ),
                _ProfileField(
                  label: 'Confirm New PIN',
                  value: _confirmPin,
                  keyboardType: TextInputType.number,
                  isPassword: true,
                  isError: _confirmPin.isNotEmpty && _confirmPin != _newPin,
                  onChanged: (v) => setState(() => _confirmPin = _digitsOnly(v, 4)),
                ),
              ] else ...[
                _ProfileRow('Unit No.', currentUser.flatNumber),
                _ProfileRow('Wing', currentUser.wing),
                _ProfileRow('Role', roleLabel),
                _ProfileRow('Co-Applicant', currentUser.coApplicantName),
                _ProfileRow('Mobile', currentUser.contactNumber),
                _ProfileRow('Email', currentUser.email),
                if (currentUser.tenantName.trim().isNotEmpty) _ProfileRow('Tenant', currentUser.tenantName),
                // The society's own unit type is what the unit was created and
                // is billed as. flatType is only the nearest built-in slab, so
                // showing it here reported the wrong type for any society whose
                // types are its own ("Shop", "Office").
                _ProfileRow(
                  'Unit Type',
                  currentUser.unitType.trim().isEmpty ? currentUser.flatType.displayName : currentUser.unitType,
                ),
                _ProfileRow('Parking Slot', _orDash(currentUser.parkingSlot)),
                _ProfileRow('Outstanding Dues', rupees(currentUser.balanceDue)),
              ],
              if (_error != null) ...[
                const SizedBox(height: 8),
                Text(_error!, style: tt.bodySmall?.copyWith(color: cs.error)),
              ],
              // Logout now belongs to the profile, not the top bar.
              const SizedBox(height: 14),
              const Divider(height: 1),
              const SizedBox(height: 10),
              OutlinedButton.icon(
                key: const Key('profile_logout_btn'),
                style: OutlinedButton.styleFrom(
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                  side: const BorderSide(color: AppColors.statusOverdue),
                ),
                onPressed: () {
                  widget.onLogout();
                  widget.onDismiss();
                },
                icon: const Icon(Icons.logout, color: AppColors.statusOverdue),
                label: Text(tr('logout', language), style: const TextStyle(color: AppColors.statusOverdue)),
              ),
            ],
          ),
        ),
      ),
      actions: _isEditing
          ? [
              OutlinedButton(
                onPressed: () => setState(() {
                  _isEditing = false;
                  _error = null;
                }),
                child: Text(tr('cancel', language)),
              ),
              AppButton.filled(
                key: const Key('save_user_profile_btn'),
                style: FilledButton.styleFrom(backgroundColor: AppColors.tealPrimary),
                onPressed: () => _save(vm),
                child: Text(tr('save_profile', language)),
              ),
            ]
          : [
              AppButton.filled(
                style: FilledButton.styleFrom(backgroundColor: AppColors.tealPrimary),
                onPressed: widget.onDismiss,
                child: Text(tr('close', language)),
              ),
            ],
    );
  }
}

// ------------------------------------------------------------------ helpers

class _ProfileRow extends StatelessWidget {
  const _ProfileRow(this.label, this.value);

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    if (value.trim().isEmpty) return const SizedBox.shrink();
    final tt = Theme.of(context).textTheme;
    final cs = Theme.of(context).colorScheme;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant)),
          const SizedBox(width: 12),
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.end,
              style: tt.bodySmall?.copyWith(fontWeight: FontWeight.w600),
            ),
          ),
        ],
      ),
    );
  }
}

class _ProfileField extends StatelessWidget {
  const _ProfileField({
    required this.label,
    required this.value,
    required this.onChanged,
    this.keyboardType,
    this.isPassword = false,
    this.isError = false,
    this.enabled = true,
  });

  final String label;
  final String value;
  final ValueChanged<String> onChanged;
  final TextInputType? keyboardType;
  final bool isPassword;
  final bool isError;

  /// False for a detail the society has already recorded. The field is still
  /// shown, with what is on file in it - a member needs to see their own
  /// record - but it is read-only and says so.
  final bool enabled;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: StateTextField(
        value: value,
        onChanged: onChanged,
        label: label,
        readOnly: !enabled,
        isError: isError,
        supportingText: enabled ? null : 'Already on record with the society.',
        obscureText: isPassword,
        leadingIcon: isPassword ? const Icon(Icons.lock, color: AppColors.tealPrimary) : null,
        keyboardType: keyboardType,
      ),
    );
  }
}

class _LogoPickerButton extends StatefulWidget {
  const _LogoPickerButton({
    required this.label,
    required this.onPicked,
    required this.onCleared,
    required this.showClear,
  });

  final String label;
  final ValueChanged<String> onPicked;
  final VoidCallback onCleared;
  final bool showClear;

  @override
  State<_LogoPickerButton> createState() => _LogoPickerButtonState();
}

class _LogoPickerButtonState extends State<_LogoPickerButton> {
  String? _pickError;

  Future<void> _pick() async {
    try {
      final encoded = await pickImageAsBase64(context, source: ImageSource.gallery);
      if (encoded == null || !mounted) return;
      widget.onPicked(encoded);
      setState(() => _pickError = null);
    } catch (_) {
      if (mounted) setState(() => _pickError = 'Could not read that image. Pick a JPG or PNG.');
    }
  }

  @override
  Widget build(BuildContext context) {
    final tt = Theme.of(context).textTheme;
    final cs = Theme.of(context).colorScheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            OutlinedButton.icon(
              onPressed: _pick,
              style: OutlinedButton.styleFrom(
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                side: const BorderSide(color: AppColors.tealPrimary),
              ),
              icon: const Icon(Icons.add_photo_alternate, color: AppColors.tealPrimary),
              label: Text(widget.label, style: const TextStyle(color: AppColors.tealPrimary)),
            ),
            if (widget.showClear)
              TextButton(
                onPressed: widget.onCleared,
                child: Text('Remove', style: TextStyle(color: cs.error)),
              ),
          ],
        ),
        if (_pickError != null) Text(_pickError!, style: tt.bodySmall?.copyWith(color: cs.error)),
      ],
    );
  }
}

/// Card wrapper used for the read-only blocks in both profiles.
class ProfileSection extends StatelessWidget {
  const ProfileSection({super.key, required this.title, required this.child});

  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final tt = Theme.of(context).textTheme;
    return Card(
      color: AppColors.surfaceVariant,
      elevation: 0,
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(title, style: tt.titleSmall?.copyWith(fontWeight: FontWeight.bold)),
            const SizedBox(height: 6),
            child,
          ],
        ),
      ),
    );
  }
}
