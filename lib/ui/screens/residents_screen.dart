// Port of ui/screens/ResidentsScreen.kt.
//
// Residents - the Chairman's own section, sitting right after Accounts.
//
// Members : the resident directory, and the only place a new resident is added
// Parking : who holds which bay, and which bays are still free
// Staff   : hiring, login credentials and access for society employees
// ignore_for_file: constant_identifier_names

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../../data/models.dart';
import '../../l10n/app_strings.dart';
import '../../state/society_view_model.dart';
import '../../util/format.dart';
import '../../util/image_blob.dart';
import '../components/common_components.dart';
import '../theme/app_colors.dart';

class ResidentsScreen extends StatefulWidget {
  const ResidentsScreen({super.key});

  @override
  State<ResidentsScreen> createState() => _ResidentsScreenState();
}

class _ResidentsScreenState extends State<ResidentsScreen> with SingleTickerProviderStateMixin {
  late final TabController _tabs = TabController(length: 3, vsync: this)
    ..addListener(() {
      if (mounted) setState(() {});
    });

  @override
  void dispose() {
    _tabs.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final language = vm.language;
    final theme = Theme.of(context);
    final subTabs = [
      tr('tab_members', language),
      tr('tab_parking', language),
      tr('tab_society_staff', language),
    ];
    final selected = _tabs.index;

    return ColoredBox(
      color: theme.scaffoldBackgroundColor,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Material(
            color: theme.colorScheme.surface,
            child: TabBar(
              controller: _tabs,
              isScrollable: true,
              tabAlignment: TabAlignment.start,
              padding: const EdgeInsets.symmetric(horizontal: 16),
              labelColor: AppColors.tealPrimary,
              unselectedLabelColor: AppColors.tealPrimary,
              indicatorColor: AppColors.tealPrimary,
              tabs: [
                for (var i = 0; i < subTabs.length; i++)
                  Tab(
                    key: Key('residents_subtab_$i'),
                    child: Text(
                      subTabs[i],
                      maxLines: 1,
                      style: theme.textTheme.labelLarge?.copyWith(
                        fontWeight: selected == i ? FontWeight.bold : FontWeight.w500,
                        color: AppColors.tealPrimary,
                      ),
                    ),
                  ),
              ],
            ),
          ),
          Expanded(
            child: switch (selected) {
              0 => const _MembersTab(),
              1 => const _ParkingTab(),
              _ => const _SocietyStaffTab(),
            },
          ),
        ],
      ),
    );
  }
}

// ----------------------------------------------------------------- 1. members

class _MembersTab extends StatefulWidget {
  const _MembersTab();

  @override
  State<_MembersTab> createState() => _MembersTabState();
}

class _MembersTabState extends State<_MembersTab> {
  String _wingFilter = ALL_WINGS;

  Future<void> _confirmDelete(BuildContext context, Resident member, AppLanguage language) {
    final theme = Theme.of(context);
    final isVacant = member.ownerName.trim().isEmpty;
    final String title;
    if (isVacant) {
      title = language == AppLanguage.marathi ? 'रिक्त सदनिका हटवायची?' : 'Delete this vacant unit?';
    } else if (language == AppLanguage.marathi) {
      title = 'सभासद हटवायचा?';
    } else {
      title = 'Remove this member?';
    }
    final String body = isVacant
        // Nobody lives here, so the unit itself can go.
        ? 'Unit ${member.flatNumber} has no member against it and will be '
            'removed from the society altogether. Its bills and history go with it.'
        : '${member.flatNumber} • ${member.ownerName}\n\n'
            "The unit itself stays in the directory - only the member's "
            'details and their login are removed.'
            '${member.balanceDue > 0 ? '\n\nThey still owe ${rupees(member.balanceDue)}.' : ''}';

    return showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        insetPadding: dialogInsetPadding(dialogContext),
        title: Text(title, style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
        content: Text(body, style: theme.textTheme.bodySmall),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: Text(tr('cancel', language)),
          ),
          FilledButton(
            key: const Key('confirm_delete_member_btn'),
            style: FilledButton.styleFrom(backgroundColor: AppColors.statusOverdue),
            onPressed: () {
              final vm = context.read<SocietyViewModel>();
              if (isVacant) {
                vm.deleteVacantUnit(member, onResult: (_, message) => vm.showMessage(message));
              } else {
                vm.removeMemberFromUnit(member, onResult: (_, message) => vm.showMessage(message));
              }
              Navigator.of(dialogContext).pop();
            },
            child: Text(language == AppLanguage.marathi ? 'हटवा' : 'Delete'),
          ),
        ],
      ),
    );
  }

  void _openResidentDialog(BuildContext context, Resident? existing) {
    showDialog<void>(
      context: context,
      builder: (_) => AddResidentDialog(existing: existing),
    );
  }

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final language = vm.language;
    final society = vm.societyInfo;
    final members = vm.members;
    final theme = Theme.of(context);
    final canDelete = vm.currentUser?.role == UserRole.CHAIRMAN;

    // Wing filter. The options come from the units themselves as well as the
    // society's configured wings, so a wing that only exists on residents -
    // because it was typed in before the wing list was set up - is still
    // reachable here.
    final wingOptions = wingsInUse(members, society);
    final visibleMembers = _wingFilter == ALL_WINGS
        ? members
        : members.where((m) => m.wing.toLowerCase() == _wingFilter.toLowerCase()).toList();

    final occupied = visibleMembers.where((m) => m.ownerName.trim().isNotEmpty).length;
    final vacant = visibleMembers.where((m) => m.ownerName.trim().isEmpty).length;

    final children = <Widget>[
      Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  tr('member_directory', language),
                  style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                ),
                Text(
                  '$occupied occupied • $vacant vacant',
                  style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
              ],
            ),
          ),
          FilledButton(
            key: const Key('add_resident_btn'),
            style: FilledButton.styleFrom(
              backgroundColor: AppColors.tealPrimary,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
            ),
            onPressed: () => _openResidentDialog(context, null),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.person_add, size: 16),
                const SizedBox(width: 4),
                Text(tr('add_member', language), style: theme.textTheme.labelSmall?.copyWith(color: Colors.white)),
              ],
            ),
          ),
        ],
      ),
      if (wingOptions.length > 1)
        SearchableDropdownField(
          label: 'Wing',
          value: _wingFilter,
          options: wingOptions,
          onValueChange: (v) => setState(() => _wingFilter = v),
          allowFreeText: false,
          testTag: 'members_wing_filter',
        ),
      for (final member in visibleMembers)
        _memberCard(context, member, theme, canDelete, language),
    ];

    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: children.length,
      separatorBuilder: (_, _) => const SizedBox(height: 12),
      itemBuilder: (_, i) => children[i],
    );
  }

  Widget _memberCard(BuildContext context, Resident member, ThemeData theme, bool canDelete, AppLanguage language) {
    final isVacant = member.ownerName.trim().isEmpty;
    final small = theme.textTheme.bodySmall;
    final muted = theme.colorScheme.onSurfaceVariant;
    final wingAndType = [
      if (member.wing.trim().isNotEmpty) 'Wing ${member.wing}',
      member.unitType.trim().isNotEmpty ? member.unitType : 'Unit type not set',
    ].join(' • ');

    return Card(
      margin: EdgeInsets.zero,
      color: theme.colorScheme.surface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
        side: const BorderSide(color: AppColors.borderLight),
      ),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Row(
          children: [
            // The member's own photo, once they have uploaded one in their
            // profile. A vacant unit has nobody to show.
            if (!isVacant) ...[
              Material(
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)),
                color: AppColors.tealPrimary,
                clipBehavior: Clip.antiAlias,
                child: MemberAvatar(
                  key: Key('member_photo_${member.flatNumber}'),
                  photoBase64: member.photoBase64,
                  name: member.ownerName,
                  size: 40,
                ),
              ),
              const SizedBox(width: 12),
            ],
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '${member.flatNumber} • ${isVacant ? 'Vacant unit' : member.ownerName}',
                    style: theme.textTheme.titleSmall?.copyWith(
                      fontWeight: FontWeight.bold,
                      color: isVacant ? muted : theme.colorScheme.onSurface,
                    ),
                  ),
                  if (member.coApplicantName.trim().isNotEmpty)
                    Text('Co-applicant: ${member.coApplicantName}', style: small?.copyWith(color: muted)),
                  if (member.tenantName.trim().isNotEmpty)
                    Text('Tenant: ${member.tenantName}', style: small?.copyWith(color: AppColors.tealPrimary)),
                  // Wing and unit type on the card - the two things that
                  // decide which tariff this unit is billed.
                  Text(
                    wingAndType,
                    style: small?.copyWith(
                      color: member.unitType.trim().isEmpty ? AppColors.statusOverdue : AppColors.tealPrimary,
                    ),
                  ),
                  if (!isVacant) ...[
                    Text(
                      '${member.contactNumber} • ${member.familyMemberCount} family member(s)',
                      style: small?.copyWith(color: muted),
                    ),
                    Text(
                      member.parkingType == ParkingType.NONE
                          ? 'No parking'
                          : '${member.parkingType.labelEn} • ${member.parkingSlot.trim().isEmpty ? 'unallotted' : member.parkingSlot}',
                      style: small?.copyWith(color: muted),
                    ),
                  ],
                ],
              ),
            ),
            IconButton(
              key: Key('edit_member_${member.flatNumber}'),
              onPressed: () => _openResidentDialog(context, member),
              tooltip: 'Edit member',
              icon: const Icon(Icons.edit, color: AppColors.tealPrimary),
            ),
            // Only the Chairman can turf a member out, and only after
            // confirming it.
            if (canDelete)
              IconButton(
                key: Key('delete_member_${member.flatNumber}'),
                onPressed: () => _confirmDelete(context, member, language),
                tooltip: isVacant ? 'Delete vacant unit' : 'Remove member',
                icon: const Icon(Icons.delete, color: AppColors.statusOverdue),
              ),
          ],
        ),
      ),
    );
  }
}

/// Add (or fill in) one resident against a unit.
///
/// The unit comes from a dropdown of what the society actually has, and parking
/// is picked in two steps: the type first, then a bay number - and only bays
/// that are still free are offered.
class AddResidentDialog extends StatefulWidget {
  const AddResidentDialog({super.key, required this.existing});

  final Resident? existing;

  @override
  State<AddResidentDialog> createState() => _AddResidentDialogState();
}

class _AddResidentDialogState extends State<AddResidentDialog> {
  late String _unitNumber;
  late final TextEditingController _ownerName;
  late final TextEditingController _coApplicantName;
  late final TextEditingController _tenantName;
  late final TextEditingController _familyCount;
  late final TextEditingController _phone;
  late final TextEditingController _pin;
  late final TextEditingController _email;
  late final TextEditingController _tenantContact;
  late ParkingType _parkingType;
  late String _parkingSlot;
  late String _unitType;
  late String _wing;
  late String _rentAgreement;
  String? _error;

  @override
  void initState() {
    super.initState();
    final existing = widget.existing;
    final members = context.read<SocietyViewModel>().members;
    final unitNumbers = _sortedUnitNumbers(members);
    final existingFlat = existing?.flatNumber ?? '';
    _unitNumber = existingFlat.trim().isNotEmpty ? existingFlat : (unitNumbers.isEmpty ? '' : unitNumbers.first);
    _ownerName = TextEditingController(text: existing?.ownerName ?? '');
    _coApplicantName = TextEditingController(text: existing?.coApplicantName ?? '');
    _tenantName = TextEditingController(text: existing?.tenantName ?? '');
    _familyCount = TextEditingController(text: (existing?.familyMemberCount ?? 1).toString());
    _phone = TextEditingController(text: existing?.contactNumber ?? '');
    _pin = TextEditingController(text: existing?.pin ?? '');
    _email = TextEditingController(text: existing?.email ?? '');
    _tenantContact = TextEditingController(text: existing?.tenantContact ?? '');
    _parkingType = existing?.parkingType ?? ParkingType.NONE;
    _parkingSlot = existing?.parkingSlot ?? '';
    _unitType = existing?.unitType ?? '';
    _wing = existing?.wing ?? '';
    _rentAgreement = existing?.rentAgreementBase64 ?? '';
    _pullUpUnit(members);
  }

  @override
  void dispose() {
    for (final c in [_ownerName, _coApplicantName, _tenantName, _familyCount, _phone, _pin, _email, _tenantContact]) {
      c.dispose();
    }
    super.dispose();
  }

  static List<String> _sortedUnitNumbers(List<Resident> members) => members.map((m) => m.flatNumber).toList()..sort();

  /// Picking a unit that is already filled in pulls its details up so the
  /// sheet doubles as an edit form.
  void _pullUpUnit(List<Resident> members) {
    if (widget.existing != null) return;
    Resident? match;
    for (final m in members) {
      if (m.flatNumber.toLowerCase() == _unitNumber.toLowerCase()) {
        match = m;
        break;
      }
    }
    if (match != null && match.ownerName.trim().isNotEmpty) {
      _ownerName.text = match.ownerName;
      _coApplicantName.text = match.coApplicantName;
      _tenantName.text = match.tenantName;
      _familyCount.text = match.familyMemberCount.toString();
      _phone.text = match.contactNumber;
      _email.text = match.email;
      _pin.text = match.pin;
      _parkingType = match.parkingType;
      _parkingSlot = match.parkingSlot;
      _tenantContact.text = match.tenantContact;
      _rentAgreement = match.rentAgreementBase64;
    }
    // A vacant unit still knows what type it was created as.
    if (match != null && match.unitType.trim().isNotEmpty) _unitType = match.unitType;
    if (match != null && match.wing.trim().isNotEmpty) _wing = match.wing;
  }

  void _submit(SocietyViewModel vm, List<Resident> members) {
    final unitNumber = _unitNumber;
    final ownerName = _ownerName.text;
    final phone = _phone.text;
    final pin = _pin.text;
    final tenantName = _tenantName.text;
    if (unitNumber.trim().isEmpty || ownerName.trim().isEmpty || phone.trim().isEmpty) {
      setState(() => _error = 'Unit number, owner name and mobile number are all required.');
      return;
    }
    if (pin.length != 4) {
      setState(() => _error = 'The login PIN must be exactly 4 digits.');
      return;
    }
    if (_parkingType != ParkingType.NONE && _parkingSlot.trim().isEmpty) {
      setState(() => _error = 'Pick a parking number, or choose No Parking.');
      return;
    }
    Resident? existingMember;
    for (final m in members) {
      if (m.flatNumber.toLowerCase() == unitNumber.toLowerCase()) {
        existingMember = m;
        break;
      }
    }
    final familyCount = int.tryParse(_familyCount.text) ?? 1;
    final noTenant = tenantName.trim().isEmpty;
    if (existingMember != null && existingMember.ownerName.trim().isNotEmpty) {
      // Already occupied: update in place rather than refuse.
      vm.updateResident(existingMember.copyWith(
        ownerName: ownerName.trim(),
        coApplicantName: _coApplicantName.text.trim(),
        tenantName: tenantName.trim(),
        familyMemberCount: familyCount,
        contactNumber: phone.trim(),
        email: _email.text.trim(),
        pin: pin,
        parkingType: _parkingType,
        parkingSlot: _parkingType == ParkingType.NONE ? '' : _parkingSlot,
        unitType: _unitType.trim(),
        wing: _wing.trim(),
        tenantContact: noTenant ? '' : _tenantContact.text.trim(),
        rentAgreementBase64: noTenant ? '' : _rentAgreement,
        isOwnerLiving: noTenant,
      ));
      Navigator.of(context).pop();
    } else {
      vm.addResident(
        flatNumber: unitNumber,
        wing: _wing.trim().isEmpty ? (existingMember?.wing ?? '') : _wing.trim(),
        ownerName: ownerName,
        coApplicantName: _coApplicantName.text,
        tenantName: tenantName,
        tenantPhone: _tenantContact.text,
        rentAgreementBase64: _rentAgreement,
        phone: phone,
        email: _email.text,
        role: UserRole.RESIDENT,
        familyCount: familyCount,
        twoWheelers: 0,
        fourWheelers: 0,
        twoWheelerNo: '',
        fourWheelerNo: '',
        parkingSlot: _parkingSlot,
        parkingType: _parkingType,
        unitType: _unitType,
        flatType: existingMember?.flatType ?? FlatType.TWO_BHK,
        sqft: existingMember?.squareFeet ?? 0,
        pin: pin,
        onResult: (ok, message) {
          if (ok) {
            vm.showMessage(message);
            if (mounted) Navigator.of(context).pop();
          } else if (mounted) {
            setState(() => _error = message);
          }
        },
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final language = vm.language;
    final society = vm.societyInfo;
    final members = vm.members;
    final theme = Theme.of(context);
    final unitNumbers = _sortedUnitNumbers(members);

    // The society's own unit types. Which one a unit is decides the monthly
    // tariff it is billed, so it is asked for here rather than guessed.
    final unitTypeOptions = society?.unitTypeNames() ?? const <String>[];
    // Every wing the society has, so a unit can be moved onto the right one
    // rather than a new spelling being invented for it.
    final wingChoices = wingsInUse(members, society).where((w) => w != ALL_WINGS).toList();
    final freeBays = vm.availableParking(_parkingType, keepFor: _unitNumber);
    final pinText = _pin.text;
    final pinBad = pinText.isNotEmpty && pinText.length != 4;
    final muted = theme.colorScheme.onSurfaceVariant;

    return AlertDialog(
      insetPadding: dialogInsetPadding(context),
      title: Text(
        tr('add_member', language),
        style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
      ),
      content: SizedBox(
        width: double.maxFinite,
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: _spaced(8, [
              SearchableDropdownField(
                label: tr('unit_number_label', language),
                value: _unitNumber,
                options: unitNumbers,
                onValueChange: (v) => setState(() {
                  final changed = v != _unitNumber;
                  _unitNumber = v;
                  _error = null;
                  if (changed) _pullUpUnit(members);
                }),
                allowFreeText: unitNumbers.isEmpty,
                testTag: 'resident_unit_dropdown',
              ),
              SearchableDropdownField(
                label: 'Wing',
                value: _wing,
                options: wingChoices,
                onValueChange: (v) => setState(() => _wing = v),
                // A society that has no wings yet can still type one in.
                allowFreeText: wingChoices.isEmpty,
                testTag: 'resident_wing_dropdown',
              ),
              SearchableDropdownField(
                label: 'Unit Type${_unitType.trim().isEmpty ? ' *' : ''}',
                value: _unitType,
                options: unitTypeOptions,
                onValueChange: (v) => setState(() {
                  _unitType = v;
                  _error = null;
                }),
                // A society that never named its types can still type one in.
                allowFreeText: unitTypeOptions.isEmpty,
                testTag: 'resident_unit_type_dropdown',
              ),
              if (_unitType.trim().isEmpty)
                Text(
                  'Pick the unit type - the monthly tariff is charged against it.',
                  style: theme.textTheme.bodySmall?.copyWith(color: muted),
                ),
              TextField(
                key: const Key('resident_owner_input'),
                controller: _ownerName,
                decoration: const InputDecoration(labelText: 'Owner Full Name'),
              ),
              // The second name on the unit's paperwork. Members can fill this
              // in themselves from their own profile when it is blank.
              TextField(
                key: const Key('resident_co_applicant_input'),
                controller: _coApplicantName,
                decoration: const InputDecoration(labelText: 'Co-Applicant Name (optional)'),
              ),
              TextField(
                controller: _tenantName,
                onChanged: (_) => setState(() {}),
                decoration: const InputDecoration(labelText: 'Tenant Name (optional)'),
              ),
              // A let flat has a tenant to reach and an agreement behind it, so
              // those are asked for only once a tenant is actually named.
              if (_tenantName.text.trim().isNotEmpty) ...[
                TextField(
                  key: const Key('resident_tenant_phone_input'),
                  controller: _tenantContact,
                  keyboardType: TextInputType.phone,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly, LengthLimitingTextInputFormatter(10)],
                  decoration: const InputDecoration(labelText: 'Tenant Mobile Number'),
                ),
                RentAgreementPicker(
                  agreementBase64: _rentAgreement,
                  onAgreement: (v) => setState(() => _rentAgreement = v),
                ),
              ],
              TextField(
                key: const Key('resident_family_input'),
                controller: _familyCount,
                keyboardType: TextInputType.number,
                inputFormatters: [FilteringTextInputFormatter.digitsOnly, LengthLimitingTextInputFormatter(2)],
                decoration: const InputDecoration(labelText: 'Total Number of Family Members'),
              ),
              TextField(
                key: const Key('resident_phone_input'),
                controller: _phone,
                keyboardType: TextInputType.phone,
                inputFormatters: [FilteringTextInputFormatter.digitsOnly, LengthLimitingTextInputFormatter(10)],
                decoration: InputDecoration(labelText: tr('mobile_number', language)),
              ),
              TextField(
                key: const Key('resident_pin_input'),
                controller: _pin,
                obscureText: true,
                keyboardType: TextInputType.number,
                onChanged: (_) => setState(() {}),
                inputFormatters: [FilteringTextInputFormatter.digitsOnly, LengthLimitingTextInputFormatter(4)],
                decoration: InputDecoration(
                  labelText: 'Login PIN',
                  helperText: pinBad ? null : '${pinText.length}/4 digits',
                  errorText: pinBad ? '${pinText.length}/4 digits' : null,
                ),
              ),
              TextField(
                controller: _email,
                keyboardType: TextInputType.emailAddress,
                decoration: const InputDecoration(labelText: 'Email ID'),
              ),
              const Padding(padding: EdgeInsets.symmetric(vertical: 4), child: Divider(height: 1)),
              Text(
                tr('parking_details', language),
                style: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
              ),
              SearchableDropdownField(
                label: tr('parking_type', language),
                value: _parkingType.labelEn,
                options: ParkingType.values.map((t) => t.labelEn).toList(),
                onValueChange: (picked) => setState(() {
                  _parkingType = ParkingType.values.firstWhere(
                    (t) => t.labelEn == picked,
                    orElse: () => ParkingType.NONE,
                  );
                  _parkingSlot = '';
                }),
                allowFreeText: false,
                testTag: 'parking_type_dropdown',
              ),
              // Only shown once a real parking type is chosen.
              if (_parkingType != ParkingType.NONE) ...[
                // The generated bay numbers (C-01, O-01 ...) are a convenience,
                // not a rule - a society whose bays are painted "B2-14" or
                // "Stilt 3" has to be able to type that in rather than being
                // forced onto a number it never used.
                SearchableDropdownField(
                  label: tr('parking_list', language),
                  value: _parkingSlot,
                  options: freeBays,
                  onValueChange: (v) => setState(() => _parkingSlot = v),
                  allowFreeText: true,
                  testTag: 'parking_list_dropdown',
                ),
                Text(
                  '${freeBays.length} free ${_parkingType.labelEn.toLowerCase()} bay(s) suggested - '
                  'or type your own bay number.',
                  style: theme.textTheme.bodySmall?.copyWith(color: muted),
                ),
              ],
              if (_error != null)
                Text(_error!, style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.error)),
            ]),
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: Text(tr('cancel', language)),
        ),
        FilledButton(
          key: const Key('resident_submit_btn'),
          style: FilledButton.styleFrom(backgroundColor: AppColors.tealPrimary),
          onPressed: () => _submit(vm, members),
          child: Text(tr('submit', language)),
        ),
      ],
    );
  }
}

// ----------------------------------------------------------------- 2. parking

/// Every bay in the society: its number, type, the unit holding it and who that is.
class _ParkingTab extends StatelessWidget {
  const _ParkingTab();

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final language = vm.language;
    final society = vm.societyInfo;
    final members = vm.members;
    final theme = Theme.of(context);
    final muted = theme.colorScheme.onSurfaceVariant;

    // Bay -> holder, built once from the directory.
    final allottedBy = <String, Resident>{
      for (final m in members.where((m) => m.parkingSlot.trim().isNotEmpty)) m.parkingSlot.trim().toUpperCase(): m,
    };
    final allBays = <(String, ParkingType)>[
      for (final b in society?.parkingNumbers(ParkingType.COVERED) ?? const <String>[]) (b, ParkingType.COVERED),
      for (final b in society?.parkingNumbers(ParkingType.OPEN) ?? const <String>[]) (b, ParkingType.OPEN),
    ];
    final allottedCount = allBays.where((e) => allottedBy.containsKey(e.$1.toUpperCase())).length;
    final headerStyle = theme.textTheme.labelSmall?.copyWith(fontWeight: FontWeight.bold);

    final children = <Widget>[
      Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(tr('tab_parking', language), style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
          Text(
            '$allottedCount of ${allBays.length} bays allotted',
            style: theme.textTheme.bodySmall?.copyWith(color: muted),
          ),
          const SizedBox(height: 8),
          // Column headers for the list below.
          Container(
            decoration: BoxDecoration(
              color: AppColors.tealContainer,
              borderRadius: BorderRadius.circular(8),
            ),
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
            child: Row(
              children: [
                Expanded(flex: 11, child: Text('Parking No.', style: headerStyle)),
                Expanded(flex: 11, child: Text('Type', style: headerStyle)),
                Expanded(flex: 10, child: Text('Unit', style: headerStyle)),
                Expanded(flex: 16, child: Text('Owner', style: headerStyle)),
              ],
            ),
          ),
        ],
      ),
      if (allBays.isEmpty)
        Text(
          'No parking bays configured yet. Set the covered / open counts in the Society Profile.',
          style: theme.textTheme.bodySmall?.copyWith(color: muted),
        ),
      for (final (bay, type) in allBays) _bayRow(theme, bay, type, allottedBy[bay.toUpperCase()], language),
    ];

    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: children.length,
      separatorBuilder: (_, _) => const SizedBox(height: 10),
      itemBuilder: (_, i) => children[i],
    );
  }

  Widget _bayRow(ThemeData theme, String bay, ParkingType type, Resident? holder, AppLanguage language) {
    final small = theme.textTheme.bodySmall;
    return Card(
      key: Key('parking_row_$bay'),
      margin: EdgeInsets.zero,
      color: holder == null ? AppColors.surfaceVariant : theme.colorScheme.surface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(10),
        side: const BorderSide(color: AppColors.borderLight),
      ),
      child: Padding(
        padding: const EdgeInsets.all(10),
        child: Row(
          children: [
            Expanded(
              flex: 11,
              child: Row(
                children: [
                  Icon(
                    Icons.directions_car,
                    size: 16,
                    color: holder == null ? theme.colorScheme.onSurfaceVariant : AppColors.tealPrimary,
                  ),
                  const SizedBox(width: 6),
                  Flexible(child: Text(bay, style: small?.copyWith(fontWeight: FontWeight.bold))),
                ],
              ),
            ),
            Expanded(
              flex: 11,
              child: Text(language == AppLanguage.marathi ? type.labelMr : type.labelEn, style: small),
            ),
            Expanded(flex: 10, child: Text(holder?.flatNumber ?? '-', style: small)),
            Expanded(
              flex: 16,
              child: Text(
                holder?.ownerName ?? 'Available',
                style: small?.copyWith(color: holder == null ? AppColors.statusPaid : theme.colorScheme.onSurface),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ----------------------------------------------------------- 3. society staff

class _SocietyStaffTab extends StatelessWidget {
  const _SocietyStaffTab();

  void _openStaffDialog(BuildContext context, Resident? existing) {
    showDialog<void>(context: context, builder: (_) => StaffDialog(existing: existing));
  }

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final language = vm.language;
    final staff = vm.staff;
    final theme = Theme.of(context);
    final muted = theme.colorScheme.onSurfaceVariant;

    final children = <Widget>[
      Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  tr('tab_society_staff', language),
                  style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                ),
                Text(
                  'Staff sign in with their staff code and PIN.',
                  style: theme.textTheme.bodySmall?.copyWith(color: muted),
                ),
              ],
            ),
          ),
          FilledButton(
            key: const Key('add_staff_btn'),
            style: FilledButton.styleFrom(
              backgroundColor: AppColors.tealPrimary,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
            ),
            onPressed: () => _openStaffDialog(context, null),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.add, size: 16),
                const SizedBox(width: 4),
                Text(tr('add_staff', language), style: theme.textTheme.labelSmall?.copyWith(color: Colors.white)),
              ],
            ),
          ),
        ],
      ),
      for (final member in staff)
        Card(
          margin: EdgeInsets.zero,
          color: theme.colorScheme.surface,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14),
            side: const BorderSide(color: AppColors.borderLight),
          ),
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Row(
              children: [
                Material(
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
                  color: AppColors.tealPrimary,
                  clipBehavior: Clip.antiAlias,
                  child: MemberAvatar(photoBase64: member.photoBase64, name: member.ownerName, size: 44),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(member.ownerName, style: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold)),
                      Text(
                        getRoleLabel(member.role, language) +
                            (member.staffDesignation.trim().isNotEmpty ? ' • ${member.staffDesignation}' : ''),
                        style: theme.textTheme.bodySmall?.copyWith(color: AppColors.tealPrimary),
                      ),
                      Text(
                        'Login: ${member.flatNumber} • ${member.contactNumber}',
                        style: theme.textTheme.bodySmall?.copyWith(color: muted),
                      ),
                    ],
                  ),
                ),
                Text(
                  member.isActive ? 'Active' : 'Disabled',
                  style: theme.textTheme.labelSmall?.copyWith(
                    fontWeight: FontWeight.bold,
                    color: member.isActive ? AppColors.statusPaid : AppColors.statusOverdue,
                  ),
                ),
                IconButton(
                  key: Key('edit_staff_${member.flatNumber}'),
                  onPressed: () => _openStaffDialog(context, member),
                  tooltip: 'Edit staff',
                  icon: const Icon(Icons.edit, color: AppColors.tealPrimary),
                ),
              ],
            ),
          ),
        ),
    ];

    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: children.length,
      separatorBuilder: (_, _) => const SizedBox(height: 12),
      itemBuilder: (_, i) => children[i],
    );
  }
}

/// Create or update one staff member, including the credentials they sign in with.
class StaffDialog extends StatefulWidget {
  const StaffDialog({super.key, required this.existing});

  final Resident? existing;

  @override
  State<StaffDialog> createState() => _StaffDialogState();
}

class _StaffDialogState extends State<StaffDialog> {
  late final TextEditingController _staffCode = TextEditingController(text: widget.existing?.flatNumber ?? '');
  late final TextEditingController _name = TextEditingController(text: widget.existing?.ownerName ?? '');
  late UserRole _role = widget.existing?.role ?? UserRole.GUARD;
  late final TextEditingController _designation =
      TextEditingController(text: widget.existing?.staffDesignation ?? '');
  late final TextEditingController _phone = TextEditingController(text: widget.existing?.contactNumber ?? '');
  late final TextEditingController _email = TextEditingController(text: widget.existing?.email ?? '');
  late final TextEditingController _pin = TextEditingController(text: widget.existing?.pin ?? '');
  late bool _isActive = widget.existing?.isActive ?? true;
  String? _error;

  static final _codeChar = RegExp(r'[\p{L}\p{N}-]', unicode: true);

  @override
  void dispose() {
    for (final c in [_staffCode, _name, _designation, _phone, _email, _pin]) {
      c.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final language = vm.language;
    final theme = Theme.of(context);
    final existing = widget.existing;
    final muted = theme.colorScheme.onSurfaceVariant;
    final pinText = _pin.text;
    final pinBad = pinText.isNotEmpty && pinText.length != 4;

    return AlertDialog(
      insetPadding: dialogInsetPadding(context),
      title: Text(
        existing == null ? tr('add_staff', language) : tr('tab_society_staff', language),
        style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
      ),
      content: SizedBox(
        width: double.maxFinite,
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: _spaced(8, [
              TextField(
                key: const Key('staff_code_input'),
                controller: _staffCode,
                enabled: existing == null,
                inputFormatters: [
                  TextInputFormatter.withFunction((oldValue, newValue) {
                    final cleaned = newValue.text.toUpperCase().split('').where(_codeChar.hasMatch).join();
                    return TextEditingValue(
                      text: cleaned,
                      selection: TextSelection.collapsed(offset: cleaned.length),
                    );
                  }),
                ],
                decoration: const InputDecoration(
                  labelText: 'Staff Login Code',
                  hintText: 'e.g. GATE-01',
                  helperText: 'This is what the staff member signs in with.',
                ),
              ),
              TextField(
                key: const Key('staff_name_input'),
                controller: _name,
                decoration: const InputDecoration(labelText: 'Full Name'),
              ),
              SearchableDropdownField(
                label: 'Staff Role',
                value: getRoleLabel(_role, language),
                options: STAFF_ROLES.map((r) => getRoleLabel(r, language)).toList(),
                onValueChange: (picked) => setState(() {
                  _role = STAFF_ROLES.firstWhere(
                    (r) => getRoleLabel(r, language) == picked,
                    orElse: () => UserRole.GUARD,
                  );
                }),
                allowFreeText: false,
                testTag: 'staff_role_dropdown',
              ),
              TextField(
                controller: _designation,
                decoration: const InputDecoration(
                  labelText: 'Designation (optional)',
                  hintText: 'e.g. Night Shift Guard',
                ),
              ),
              TextField(
                key: const Key('staff_phone_input'),
                controller: _phone,
                keyboardType: TextInputType.phone,
                inputFormatters: [FilteringTextInputFormatter.digitsOnly, LengthLimitingTextInputFormatter(10)],
                decoration: InputDecoration(labelText: tr('mobile_number', language)),
              ),
              TextField(
                controller: _email,
                keyboardType: TextInputType.emailAddress,
                decoration: const InputDecoration(labelText: 'Email ID (optional)'),
              ),
              TextField(
                key: const Key('staff_pin_input'),
                controller: _pin,
                obscureText: true,
                keyboardType: TextInputType.number,
                onChanged: (_) => setState(() {}),
                inputFormatters: [FilteringTextInputFormatter.digitsOnly, LengthLimitingTextInputFormatter(4)],
                decoration: InputDecoration(
                  labelText: 'Login PIN',
                  helperText: pinBad ? null : '${pinText.length}/4 digits',
                  errorText: pinBad ? '${pinText.length}/4 digits' : null,
                ),
              ),
              Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Login enabled', style: theme.textTheme.bodyMedium),
                        Text(
                          'Turn off to stop this staff member signing in.',
                          style: theme.textTheme.bodySmall?.copyWith(color: muted),
                        ),
                      ],
                    ),
                  ),
                  Switch(
                    key: const Key('staff_active_switch'),
                    value: _isActive,
                    onChanged: (v) => setState(() => _isActive = v),
                  ),
                ],
              ),
              Card(
                margin: EdgeInsets.zero,
                color: AppColors.surfaceVariant,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                child: Padding(
                  padding: const EdgeInsets.all(10),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Access for this role',
                        style: theme.textTheme.labelMedium?.copyWith(fontWeight: FontWeight.bold),
                      ),
                      Text(_accessSummary(_role), style: theme.textTheme.bodySmall?.copyWith(color: muted)),
                    ],
                  ),
                ),
              ),
              if (_error != null)
                Text(_error!, style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.error)),
            ]),
          ),
        ),
      ),
      actions: [
        if (existing != null)
          TextButton(
            onPressed: () {
              vm.deleteStaff(existing, onResult: (_, message) => vm.showMessage(message));
              Navigator.of(context).pop();
            },
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.delete, color: AppColors.statusOverdue),
                const SizedBox(width: 4),
                Text(tr('delete', language), style: const TextStyle(color: AppColors.statusOverdue)),
              ],
            ),
          ),
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: Text(tr('cancel', language)),
        ),
        FilledButton(
          key: const Key('staff_submit_btn'),
          style: FilledButton.styleFrom(backgroundColor: AppColors.tealPrimary),
          onPressed: () {
            vm.saveStaff(
              staffCode: _staffCode.text,
              name: _name.text,
              role: _role,
              designation: _designation.text,
              phone: _phone.text,
              email: _email.text,
              pin: _pin.text,
              isActive: _isActive,
              existing: existing,
              onResult: (ok, message) {
                if (ok) {
                  vm.showMessage(message);
                  if (mounted) Navigator.of(context).pop();
                } else if (mounted) {
                  setState(() => _error = message);
                }
              },
            );
          },
          child: Text(tr('submit', language)),
        ),
      ],
    );
  }
}

/// The "show everything" row at the top of a wing picker.
const String ALL_WINGS = 'All Wings';

/// Every wing the society actually has: the ones it configured, plus any that
/// only exist on a unit because they were typed in before the wing list did.
/// Without the second half a stray wing is invisible and cannot be cleaned up.
List<String> wingsInUse(List<Resident> members, SocietyInfo? society) {
  final configured = society?.wingNames ?? const <String>[];
  final seen = <String>{};
  final known = <String>[];
  for (final raw in [...configured, ...members.map((m) => m.wing)]) {
    final w = raw.trim();
    if (w.isEmpty || w.toLowerCase() == 'staff') continue;
    if (seen.add(w.toLowerCase())) known.add(w);
  }
  known.sort();
  return [ALL_WINGS, ...known];
}

/// Picks a rent agreement PDF and keeps it as a Base64 blob on the unit.
///
/// Firestore caps a document at 1 MiB and Base64 costs a third on top, so a
/// large scan is refused here rather than failing on write once the committee
/// believes it is filed.
class RentAgreementPicker extends StatefulWidget {
  const RentAgreementPicker({super.key, required this.agreementBase64, required this.onAgreement});

  final String agreementBase64;
  final ValueChanged<String> onAgreement;

  @override
  State<RentAgreementPicker> createState() => _RentAgreementPickerState();
}

class _RentAgreementPickerState extends State<RentAgreementPicker> {
  String? _error;

  Future<void> _pick() async {
    try {
      final picked = await pickFileAsBase64(allowedExtensions: const ['pdf']);
      if (picked == null || !mounted) return;
      if (picked.size > RENT_AGREEMENT_MAX_BYTES) {
        final mb = (picked.size / (1024.0 * 1024.0)).toStringAsFixed(1);
        final limit = (RENT_AGREEMENT_MAX_BYTES / (1024.0 * 1024.0)).toStringAsFixed(1);
        setState(() => _error = 'That file is $mb MB. The limit is $limit MB - upload a smaller scan.');
      } else {
        widget.onAgreement(picked.base64);
        setState(() => _error = null);
      }
    } catch (_) {
      if (mounted) setState(() => _error = 'Could not read that file. Pick a PDF.');
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final hasAgreement = widget.agreementBase64.trim().isNotEmpty;
    final labelStyle = theme.textTheme.labelMedium;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            OutlinedButton(
              key: const Key('rent_agreement_picker_btn'),
              onPressed: _pick,
              style: OutlinedButton.styleFrom(
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                side: const BorderSide(color: AppColors.tealPrimary),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.attach_file, color: AppColors.tealPrimary, size: 16),
                  const SizedBox(width: 6),
                  Text(
                    hasAgreement ? 'Replace Agreement' : 'Upload Rent Agreement',
                    style: labelStyle?.copyWith(color: AppColors.tealPrimary),
                  ),
                ],
              ),
            ),
            if (hasAgreement)
              TextButton(
                onPressed: () {
                  widget.onAgreement('');
                  setState(() => _error = null);
                },
                child: Text('Remove', style: labelStyle?.copyWith(color: AppColors.statusOverdue)),
              ),
          ],
        ),
        if (hasAgreement)
          Text(
            'Agreement on file (${((widget.agreementBase64.length * 3 ~/ 4) / 1024.0).toStringAsFixed(0)} KB)',
            style: theme.textTheme.bodySmall?.copyWith(color: AppColors.statusPaid),
          ),
        if (_error != null)
          Text(
            _error!,
            key: const Key('rent_agreement_error'),
            style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.error),
          ),
      ],
    );
  }
}

/// Plain-English version of what [destinationsFor] grants a staff role.
String _accessSummary(UserRole role) {
  switch (role) {
    case UserRole.GUARD:
      return 'Security (gate terminal) and Community only.';
    case UserRole.ACCOUNTANT:
      return 'Home, Accounts, Bills and Community.';
    case UserRole.MANAGER:
      return 'Home, Accounts, Bills and Community.';
    case UserRole.HOUSEKEEPING:
    case UserRole.MAINTENANCE_STAFF:
      return 'Community only - notices and the complaints they attend.';
    default:
      return 'Community only.';
  }
}

/// Kotlin `Arrangement.spacedBy(gap)` for a Column's children.
List<Widget> _spaced(double gap, List<Widget> children) => [
      for (var i = 0; i < children.length; i++) ...[
        if (i > 0) SizedBox(height: gap),
        children[i],
      ],
    ];
