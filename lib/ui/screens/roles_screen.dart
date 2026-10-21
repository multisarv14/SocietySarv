// Member roles - the Chairman's tools for who does what in the society.
//
// Roles tab : the society's own staff roles (name, base role, the screens they
//             open) and the audit trail of every role change
// Change role: a member's committee post, or a staff member's role, changed on
//             their existing record; the Chairman's post is only handed over
//
// Public API:
//   class RolesTab ({Key? key})
//   Future<void> showChangeRoleDialog(BuildContext context, Resident member)
//   String roleAccessSummary(List<NavDestination> screens, AppLanguage language)

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../data/models.dart';
import '../../l10n/app_strings.dart';
import '../../state/society_view_model.dart';
import '../../util/format.dart';
import '../components/common_components.dart';
import '../theme/app_colors.dart';

String _screenLabel(String key, AppLanguage language) {
  switch (key) {
    case 'DASHBOARD':
      return tr('nav_dashboard', language);
    case 'ACCOUNTS':
      return tr('nav_accounts', language);
    case 'MAINTENANCE':
      return tr('nav_maintenance', language);
    case 'GATE':
      return tr('nav_gate', language);
    case 'COMMUNITY':
      return tr('nav_community', language);
    default:
      return key;
  }
}

/// "Home, Accounts and Community" for the screens a role opens.
String roleAccessSummary(List<NavDestination> screens, AppLanguage language) {
  final names = screens.map((d) => _screenLabel(d.name, language)).toList();
  if (names.isEmpty) return '-';
  if (names.length == 1) return names.first;
  return '${names.sublist(0, names.length - 1).join(', ')} & ${names.last}';
}

// ------------------------------------------------------------------ Roles tab

class RolesTab extends StatelessWidget {
  const RolesTab({super.key});

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final theme = Theme.of(context);
    final muted = theme.colorScheme.onSurfaceVariant;
    final history = vm.roleHistory;

    final children = <Widget>[
      Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  vm.msg('कर्मचारी भूमिका', 'स्टाफ भूमिकाएं', 'Staff roles'),
                  style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                ),
                Text(
                  vm.msg('सोसायटीच्या गरजेनुसार नवीन भूमिका बनवा.', 'सोसायटी की जरूरत के अनुसार नई भूमिका बनाएं।',
                      'Create roles to fit how your society works.'),
                  style: theme.textTheme.bodySmall?.copyWith(color: muted),
                ),
              ],
            ),
          ),
          if (vm.canManageRoles)
            AppButton.filled(
              key: const Key('add_role_btn'),
              style: FilledButton.styleFrom(
                backgroundColor: AppColors.tealPrimary,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
              ),
              onPressed: () =>
                  showAppDialog<void>(context: context, builder: (_) => const _RoleEditorDialog(existing: null)),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.add, size: 16),
                  const SizedBox(width: 4),
                  Text(vm.msg('भूमिका जोडा', 'भूमिका जोड़ें', 'Add role'),
                      style: theme.textTheme.labelSmall?.copyWith(color: Colors.white)),
                ],
              ),
            ),
        ],
      ),
      if (vm.roles.isEmpty)
        Text(
          vm.msg(
              'अजून स्वतःची भूमिका नाही. अंगभूत भूमिका (रक्षक, लेखापाल...) वापरता येतात.',
              'अभी कोई अपनी भूमिका नहीं। अंतर्निहित भूमिकाएं (गार्ड, लेखाकार...) उपलब्ध हैं।',
              'No society roles yet. The built-in ones (Guard, Accountant, ...) are always available.'),
          style: theme.textTheme.bodySmall?.copyWith(color: muted),
        ),
      for (final role in vm.roles) _RoleCard(role: role),
      Padding(
        padding: const EdgeInsets.only(top: 8),
        child: Text(
          vm.msg('भूमिका बदलांचा इतिहास', 'भूमिका परिवर्तन इतिहास', 'Role change history'),
          style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
        ),
      ),
      if (history.isEmpty)
        Text(vm.msg('अजून कोणताही बदल नाही.', 'अभी कोई बदलाव नहीं।', 'No role changes yet.'),
            style: theme.textTheme.bodySmall?.copyWith(color: muted)),
      for (final change in history) _HistoryRow(change: change),
    ];

    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: children.length,
      separatorBuilder: (_, _) => const SizedBox(height: 12),
      itemBuilder: (_, i) => AppEntrance(index: i, child: children[i]),
    );
  }
}

class _RoleCard extends StatelessWidget {
  const _RoleCard({required this.role});

  final SocietyRole role;

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final language = vm.language;
    final theme = Theme.of(context);
    final muted = theme.colorScheme.onSurfaceVariant;
    final holders = vm.residents.where((r) => r.customRoleId == role.docId).length;
    final screens = [
      for (final key in role.menus)
        for (final d in NavDestination.values)
          if (d.name == key) d,
    ];

    return Card(
      key: Key('role_card_${role.docId}'),
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
            const Icon(Icons.badge_outlined, color: AppColors.tealPrimary),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(role.name, style: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold)),
                  Text(
                    '${vm.msg('आधार', 'आधार', 'Based on')}: ${getRoleLabel(role.baseRole, language)} • '
                    '${vm.msg('$holders व्यक्ती', '$holders व्यक्ति', '$holders holder(s)')}',
                    style: theme.textTheme.bodySmall?.copyWith(color: AppColors.tealPrimary),
                  ),
                  Text(roleAccessSummary(screens, language), style: theme.textTheme.bodySmall?.copyWith(color: muted)),
                  if (role.description.trim().isNotEmpty)
                    Text(role.description, style: theme.textTheme.bodySmall?.copyWith(color: muted)),
                ],
              ),
            ),
            if (!role.isActive)
              Text(
                vm.msg('बंद', 'बंद', 'Retired'),
                style:
                    theme.textTheme.labelSmall?.copyWith(color: AppColors.statusOverdue, fontWeight: FontWeight.bold),
              ),
            if (vm.canManageRoles)
              IconButton(
                key: Key('edit_role_${role.docId}'),
                tooltip: 'Edit role',
                icon: const Icon(Icons.edit, color: AppColors.tealPrimary),
                onPressed: () =>
                    showAppDialog<void>(context: context, builder: (_) => _RoleEditorDialog(existing: role)),
              ),
          ],
        ),
      ),
    );
  }
}

class _HistoryRow extends StatelessWidget {
  const _HistoryRow({required this.change});

  final RoleChange change;

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final language = vm.language;
    final theme = Theme.of(context);
    String label(UserRole role, String custom) => custom.trim().isNotEmpty ? custom : getRoleLabel(role, language);
    final when = change.at == null ? '' : formatDate(change.at!.toLocal(), 'dd MMM yyyy, hh:mm a');
    final by = change.source == 'core' ? 'MultiSarv (${change.changedBy})' : change.changedBy;

    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Padding(
          padding: EdgeInsets.only(top: 2),
          child: Icon(Icons.history, size: 18, color: AppColors.textMuted),
        ),
        const SizedBox(width: 8),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '${change.flatNumber} • ${change.memberName}: '
                '${label(change.fromRole, change.fromCustomRoleName)} → ${label(change.toRole, change.toCustomRoleName)}',
                style: theme.textTheme.bodyMedium,
              ),
              Text(
                [if (by.trim().isNotEmpty) by, if (when.isNotEmpty) when].join(' • '),
                style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

// --------------------------------------------------------------- role editor

class _RoleEditorDialog extends StatefulWidget {
  const _RoleEditorDialog({required this.existing});

  final SocietyRole? existing;

  @override
  State<_RoleEditorDialog> createState() => _RoleEditorDialogState();
}

class _RoleEditorDialogState extends State<_RoleEditorDialog> {
  late final TextEditingController _name = TextEditingController(text: widget.existing?.name ?? '');
  late final TextEditingController _description = TextEditingController(text: widget.existing?.description ?? '');
  late UserRole _base = widget.existing?.baseRole ?? UserRole.MANAGER;
  late final Set<String> _menus = {...(widget.existing?.menus ?? _defaultMenus(UserRole.MANAGER))};
  late bool _isActive = widget.existing?.isActive ?? true;
  String? _error;

  static List<String> _defaultMenus(UserRole base) => destinationsFor(base).map((d) => d.name).toList();

  @override
  void dispose() {
    _name.dispose();
    _description.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final language = vm.language;
    final theme = Theme.of(context);
    final existing = widget.existing;
    final muted = theme.colorScheme.onSurfaceVariant;

    void done(bool ok, String message) {
      if (ok) {
        vm.showMessage(message);
        if (mounted) Navigator.of(context).pop();
      } else if (mounted) {
        setState(() => _error = message);
      }
    }

    return AlertDialog(
      insetPadding: dialogInsetPadding(context),
      title: Text(
        existing == null
            ? vm.msg('नवीन भूमिका', 'नई भूमिका', 'New role')
            : vm.msg('भूमिका बदला', 'भूमिका संपादित करें', 'Edit role'),
        style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
      ),
      content: SizedBox(
        width: double.maxFinite,
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              TextField(
                key: const Key('role_name_input'),
                controller: _name,
                decoration: InputDecoration(
                  labelText: vm.msg('भूमिकेचे नाव', 'भूमिका का नाम', 'Role name'),
                  hintText: vm.msg('उदा. लिफ्ट ऑपरेटर', 'जैसे लिफ्ट ऑपरेटर', 'e.g. Lift Operator'),
                ),
              ),
              const SizedBox(height: 8),
              TextField(
                controller: _description,
                decoration:
                    InputDecoration(labelText: vm.msg('वर्णन (ऐच्छिक)', 'विवरण (वैकल्पिक)', 'Description (optional)')),
              ),
              const SizedBox(height: 8),
              SearchableDropdownField(
                label: vm.msg('यावर आधारित', 'इस पर आधारित', 'Based on'),
                value: getRoleLabel(_base, language),
                options: STAFF_ROLES.map((r) => getRoleLabel(r, language)).toList(),
                onValueChange: (picked) => setState(() {
                  final next = STAFF_ROLES.firstWhere((r) => getRoleLabel(r, language) == picked, orElse: () => _base);
                  // A new role starts from what its base role opens.
                  if (existing == null && next != _base) {
                    _menus
                      ..clear()
                      ..addAll(_defaultMenus(next));
                  }
                  _base = next;
                }),
                allowFreeText: false,
                testTag: 'role_base_dropdown',
              ),
              const SizedBox(height: 10),
              Text(vm.msg('ही भूमिका काय उघडू शकते', 'यह भूमिका क्या खोल सकती है', 'Screens this role opens'),
                  style: theme.textTheme.labelLarge?.copyWith(fontWeight: FontWeight.bold)),
              for (final key in ROLE_MENU_KEYS)
                CheckboxListTile(
                  key: Key('role_menu_$key'),
                  dense: true,
                  contentPadding: EdgeInsets.zero,
                  value: _menus.contains(key),
                  title: Text(_screenLabel(key, language)),
                  onChanged: (v) => setState(() => v == true ? _menus.add(key) : _menus.remove(key)),
                ),
              Text(
                vm.msg(
                    'पानांमधील कृती (उदा. खर्च नोंदवणे) आधार भूमिकेनुसार ठरतात.',
                    'पेज के अंदर की कार्रवाइयां (जैसे खर्च दर्ज करना) आधार भूमिका से तय होती हैं।',
                    'Actions inside a screen (e.g. recording expenses) follow the base role.'),
                style: theme.textTheme.bodySmall?.copyWith(color: muted),
              ),
              if (existing != null)
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  value: _isActive,
                  onChanged: (v) => setState(() => _isActive = v),
                  title: Text(
                      vm.msg('नवीन नियुक्तीसाठी उपलब्ध', 'नई नियुक्ति के लिए उपलब्ध', 'Available for new assignments')),
                ),
              if (_error != null) ...[
                const SizedBox(height: 6),
                Text(_error!, style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.error)),
              ],
            ],
          ),
        ),
      ),
      actions: [
        if (existing != null)
          TextButton(
            key: const Key('delete_role_btn'),
            onPressed: () => vm.deleteRole(existing, onResult: done),
            child: Text(tr('delete', language), style: const TextStyle(color: AppColors.statusOverdue)),
          ),
        TextButton(onPressed: () => Navigator.of(context).pop(), child: Text(tr('cancel', language))),
        AppButton.filled(
          key: const Key('save_role_btn'),
          style: FilledButton.styleFrom(backgroundColor: AppColors.tealPrimary),
          onPressed: () => vm.saveRole(
            SocietyRole(
              docId: existing?.docId ?? '',
              name: _name.text,
              description: _description.text,
              baseRole: _base,
              menus: ROLE_MENU_KEYS.where(_menus.contains).toList(),
              isActive: _isActive,
            ),
            onResult: done,
          ),
          child: Text(tr('save', language)),
        ),
      ],
    );
  }
}

// --------------------------------------------------------------- change role

Future<void> showChangeRoleDialog(BuildContext context, Resident member) =>
    showAppDialog<void>(context: context, builder: (_) => _ChangeRoleDialog(member: member));

/// What a picker row stands for: a built-in role, or a society staff role.
class _RoleChoice {
  const _RoleChoice(this.role, [this.custom]);
  final UserRole role;
  final SocietyRole? custom;
}

class _ChangeRoleDialog extends StatefulWidget {
  const _ChangeRoleDialog({required this.member});

  final Resident member;

  @override
  State<_ChangeRoleDialog> createState() => _ChangeRoleDialogState();
}

class _ChangeRoleDialogState extends State<_ChangeRoleDialog> {
  late String _picked;

  /// What the outgoing Chairman becomes when handing the post over.
  UserRole _myNewRole = UserRole.COMMITTEE;
  String? _error;

  @override
  void initState() {
    super.initState();
    _picked = '';
  }

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final language = vm.language;
    final theme = Theme.of(context);
    final member = widget.member;
    final muted = theme.colorScheme.onSurfaceVariant;

    final choices = <String, _RoleChoice>{
      for (final r in member.role.isStaff ? STAFF_ROLES : MEMBER_ROLES) getRoleLabel(r, language): _RoleChoice(r),
    };
    if (member.role.isStaff) {
      for (final r in vm.roles.where((r) => r.isActive || r.docId == member.customRoleId)) {
        // A society role named like a built-in one still gets its own row.
        final key = choices.containsKey(r.name) ? '${r.name} (${vm.msg('सोसायटी', 'सोसायटी', 'society')})' : r.name;
        choices[key] = _RoleChoice(r.baseRole, r);
      }
    }
    final current = memberRoleLabel(member, language);
    if (_picked.isEmpty || !choices.containsKey(_picked)) {
      _picked = choices.containsKey(current) ? current : choices.keys.first;
    }
    final choice = choices[_picked]!;
    final isHandover = !member.role.isStaff && choice.role == UserRole.CHAIRMAN && member.role != UserRole.CHAIRMAN;
    final myRoles = MEMBER_ROLES.where((r) => r != UserRole.CHAIRMAN).toList();

    void done(bool ok, String message) {
      if (ok) {
        vm.showMessage(message);
        if (mounted) Navigator.of(context).pop();
      } else if (mounted) {
        setState(() => _error = message);
      }
    }

    return AlertDialog(
      insetPadding: dialogInsetPadding(context),
      title: Text(vm.msg('भूमिका बदला', 'भूमिका बदलें', 'Change role'),
          style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
      content: SizedBox(
        width: double.maxFinite,
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text('${member.flatNumber} • ${member.ownerName}', style: theme.textTheme.titleSmall),
              Text('${vm.msg('सध्याची भूमिका', 'वर्तमान भूमिका', 'Current role')}: $current',
                  style: theme.textTheme.bodySmall?.copyWith(color: muted)),
              const SizedBox(height: 10),
              SearchableDropdownField(
                label: vm.msg('नवीन भूमिका', 'नई भूमिका', 'New role'),
                value: _picked,
                options: choices.keys.toList(),
                onValueChange: (v) => setState(() {
                  _picked = v;
                  _error = null;
                }),
                allowFreeText: false,
                testTag: 'change_role_dropdown',
              ),
              if (member.role == UserRole.CHAIRMAN && !isHandover)
                Padding(
                  padding: const EdgeInsets.only(top: 8),
                  child: Text(
                    vm.msg(
                        'अध्यक्षांची भूमिका बदलण्यासाठी दुसऱ्या सभासदाला अध्यक्ष करा.',
                        'अध्यक्ष की भूमिका बदलने के लिए किसी दूसरे सदस्य को अध्यक्ष बनाएं।',
                        'To change the Chairman\'s role, make another member Chairman instead.'),
                    style: theme.textTheme.bodySmall?.copyWith(color: muted),
                  ),
                ),
              if (isHandover) ...[
                const SizedBox(height: 10),
                Container(
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(color: AppColors.warningContainer, borderRadius: BorderRadius.circular(10)),
                  child: Text(
                    vm.msg(
                      'अध्यक्षपद ${member.ownerName} यांना सोपवले जाईल. तुमचा अध्यक्ष म्हणून प्रवेश लगेच बंद होईल.',
                      'अध्यक्ष पद ${member.ownerName} को सौंपा जाएगा। अध्यक्ष के रूप में आपकी पहुंच तुरंत बंद होगी।',
                      'The Chairman\'s post passes to ${member.ownerName}. Your Chairman access ends straight away.',
                    ),
                    style: theme.textTheme.bodySmall?.copyWith(color: AppColors.goldOnTertiaryContainer),
                  ),
                ),
                const SizedBox(height: 8),
                SearchableDropdownField(
                  label: vm.msg('तुमची नवीन भूमिका', 'आपकी नई भूमिका', 'Your new role'),
                  value: getRoleLabel(_myNewRole, language),
                  options: myRoles.map((r) => getRoleLabel(r, language)).toList(),
                  onValueChange: (v) => setState(() {
                    _myNewRole = myRoles.firstWhere((r) => getRoleLabel(r, language) == v, orElse: () => _myNewRole);
                  }),
                  allowFreeText: false,
                  testTag: 'handover_my_role_dropdown',
                ),
              ],
              if (member.role.isStaff) ...[
                const SizedBox(height: 8),
                Text(
                  '${vm.msg('प्रवेश', 'पहुंच', 'Access')}: ${roleAccessSummary(choice.custom == null ? destinationsFor(choice.role) : destinationsForMember(member.copyWith(customRoleId: choice.custom!.docId), vm.roles), language)}',
                  style: theme.textTheme.bodySmall?.copyWith(color: muted),
                ),
              ],
              if (_error != null) ...[
                const SizedBox(height: 8),
                Text(_error!, style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.error)),
              ],
            ],
          ),
        ),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.of(context).pop(), child: Text(tr('cancel', language))),
        AppButton.filled(
          key: const Key('change_role_submit'),
          style: FilledButton.styleFrom(backgroundColor: isHandover ? AppColors.warningAmber : AppColors.tealPrimary),
          onPressed: () => isHandover
              ? vm.handOverChairman(member, _myNewRole, onResult: done)
              : vm.changeMemberRole(member, choice.role, customRole: choice.custom, onResult: done),
          child: Text(
              isHandover ? vm.msg('अध्यक्षपद सोपवा', 'अध्यक्ष पद सौंपें', 'Hand over Chairman') : tr('save', language)),
        ),
      ],
    );
  }
}
