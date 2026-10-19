// Port of ui/screens/GateSecurityScreen.kt.
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../data/models.dart';
import '../../l10n/app_strings.dart';
import '../../state/society_view_model.dart';
import '../components/common_components.dart';
import '../theme/app_colors.dart';

class GateSecurityScreen extends StatefulWidget {
  const GateSecurityScreen({super.key});

  @override
  State<GateSecurityScreen> createState() => _GateSecurityScreenState();
}

class _GateSecurityScreenState extends State<GateSecurityScreen> with SingleTickerProviderStateMixin {
  late final TabController _tabs = TabController(length: 2, vsync: this)
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
      tr('gate_terminal_title', language),
      language == AppLanguage.marathi ? 'अभ्यागत इतिहास' : 'Visitor Log History',
    ];
    final selected = _tabs.index;

    return Container(
      color: theme.scaffoldBackgroundColor,
      child: Column(
        children: [
          Material(
            color: theme.colorScheme.surface,
            child: TabBar(
              controller: _tabs,
              isScrollable: true,
              tabAlignment: TabAlignment.start,
              padding: const EdgeInsets.symmetric(horizontal: 16),
              labelColor: AppColors.tealPrimary,
              unselectedLabelColor: theme.colorScheme.onSurfaceVariant,
              tabs: [
                for (var i = 0; i < subTabs.length; i++)
                  Tab(
                    key: Key('gate_subtab_$i'),
                    child: Text(
                      subTabs[i],
                      maxLines: 1,
                      style: theme.textTheme.labelLarge?.copyWith(
                        fontWeight: selected == i ? FontWeight.bold : FontWeight.w500,
                      ),
                    ),
                  ),
              ],
            ),
          ),
          Expanded(
            child: FadeThroughSwitcher(
              child: selected == 0
                  ? const LiveGateTerminalView(key: ValueKey('gate_live'))
                  : const VisitorHistoryView(key: ValueKey('gate_history')),
            ),
          ),
        ],
      ),
    );
  }
}

// 1. Live Gate Terminal View
class LiveGateTerminalView extends StatelessWidget {
  const LiveGateTerminalView({super.key});

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final language = vm.language;
    final activeVisitors = vm.activeVisitors;
    final residents = vm.residents;
    final theme = Theme.of(context);

    Future<void> openCheckIn() async {
      await showAppDialog<void>(
        context: context,
        builder: (_) => CheckInVisitorDialog(
          language: language,
          onSubmit: (name, phone, flat, purpose, vehicle, passcode) {
            context.read<SocietyViewModel>().checkInVisitor(name, phone, flat, purpose, vehicle, passcode);
          },
        ),
      );
    }

    Future<void> openVerify() async {
      await showAppDialog<void>(context: context, builder: (_) => VerifyPasscodeDialog(language: language));
    }

    final items = <Widget>[
      // Quick Security Actions
      Row(
        children: [
          Expanded(
            child: SizedBox(
              height: 48,
              child: AppButton.elevatedIcon(
                key: const Key('gate_checkin_btn'),
                onPressed: openCheckIn,
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.tealPrimary,
                  foregroundColor: Colors.white,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
                icon: const Icon(Icons.person),
                label: Text(tr('check_in_visitor', language), style: theme.textTheme.labelMedium?.copyWith(color: Colors.white)),
              ),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: SizedBox(
              height: 48,
              child: AppButton.elevatedIcon(
                key: const Key('gate_verify_passcode_btn'),
                onPressed: openVerify,
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.tealLight,
                  foregroundColor: Colors.white,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
                icon: const Icon(Icons.key),
                label: Text(tr('verify_passcode', language), style: theme.textTheme.labelMedium?.copyWith(color: Colors.white)),
              ),
            ),
          ),
        ],
      ),
      // Active Inside Header
      Text(
        language == AppLanguage.marathi
            ? 'सध्या आवारात असलेले अभ्यागत (${activeVisitors.length})'
            : 'Active Inside Premises (${activeVisitors.length})',
        style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
      ),
    ];

    if (activeVisitors.isEmpty) {
      items.add(EmptyState(
        icon: Icons.verified_user_rounded,
        color: AppColors.statusPaid,
        message: language == AppLanguage.marathi
            ? 'सध्या आवारात कोणताही बाहेरील अभ्यागत नाही.'
            : 'No active external visitors inside premises.',
      ));
    } else {
      for (final visitor in activeVisitors) {
        Resident? flatResident;
        for (final r in residents) {
          if (r.flatNumber == visitor.flatNumber) {
            flatResident = r;
            break;
          }
        }
        items.add(_ActiveVisitorCard(visitor: visitor, flatResident: flatResident, language: language));
      }
    }

    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: items.length,
      separatorBuilder: (_, _) => const SizedBox(height: 12),
      itemBuilder: (_, i) => AppEntrance(index: i, child: items[i]),
    );
  }
}

class _ActiveVisitorCard extends StatelessWidget {
  const _ActiveVisitorCard({required this.visitor, required this.flatResident, required this.language});

  final VisitorLog visitor;
  final Resident? flatResident;
  final AppLanguage language;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final unit = flatResident;
    return Card(
      margin: EdgeInsets.zero,
      elevation: 0,
      color: theme.colorScheme.surface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
        side: const BorderSide(color: AppColors.borderLight),
      ),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Row(
                    children: [
                      Container(
                        width: 40,
                        height: 40,
                        decoration: const BoxDecoration(color: AppColors.tealContainer, shape: BoxShape.circle),
                        alignment: Alignment.center,
                        child: Icon(getPurposeIcon(visitor.purpose), color: AppColors.tealPrimary, size: 22),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(visitor.visitorName,
                                style: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold)),
                            Text(
                              '${visitor.purpose.name} • ${visitor.flatNumber}',
                              style: theme.textTheme.bodySmall?.copyWith(color: AppColors.tealPrimary),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
                VisitorStatusBadge(status: visitor.status, language: language),
              ],
            ),
            const Divider(height: 20),
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'In: ${visitor.entryTime} • ${visitor.phone}',
                        style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                      ),
                      if (visitor.vehicleNumber.trim().isNotEmpty)
                        Text('Vehicle: ${visitor.vehicleNumber}', style: theme.textTheme.bodySmall),
                    ],
                  ),
                ),
                if (unit != null && unit.contactNumber.trim().isNotEmpty) ...[
                  SizedBox(
                    width: 36,
                    height: 36,
                    child: IconButton(
                      padding: EdgeInsets.zero,
                      style: IconButton.styleFrom(backgroundColor: AppColors.tealContainer),
                      tooltip: 'Call Unit',
                      onPressed: () => launchUrl(Uri.parse('tel:${unit.contactNumber}')),
                      icon: const Icon(Icons.phone, color: AppColors.tealPrimary, size: 18),
                    ),
                  ),
                  const SizedBox(width: 6),
                ],
                AppButton.elevatedIcon(
                  key: Key('mark_exit_btn_${visitor.id}'),
                  onPressed: () => context.read<SocietyViewModel>().markVisitorExit(visitor),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.statusOverdue,
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                  ),
                  icon: const Icon(Icons.exit_to_app, size: 16),
                  label: Text(tr('visitor_exited', language),
                      style: theme.textTheme.labelSmall?.copyWith(color: Colors.white)),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

// 2. Visitor History View
class VisitorHistoryView extends StatefulWidget {
  const VisitorHistoryView({super.key});

  @override
  State<VisitorHistoryView> createState() => _VisitorHistoryViewState();
}

class _VisitorHistoryViewState extends State<VisitorHistoryView> {
  String _searchQuery = '';

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final language = vm.language;
    final theme = Theme.of(context);
    final q = _searchQuery.toLowerCase();
    final filtered = vm.visitors
        .where((it) =>
            it.visitorName.toLowerCase().contains(q) ||
            it.flatNumber.toLowerCase().contains(q) ||
            it.phone.toLowerCase().contains(q))
        .toList();

    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: filtered.length + 1,
      separatorBuilder: (_, _) => const SizedBox(height: 10),
      itemBuilder: appEntrance((context, i) {
        if (i == 0) {
          return TextField(
            onChanged: (v) => setState(() => _searchQuery = v),
            decoration: const InputDecoration(
              labelText: 'Search by name, flat or phone',
              prefixIcon: Icon(Icons.search),
            ),
          );
        }
        final visitor = filtered[i - 1];
        return Card(
          margin: EdgeInsets.zero,
          elevation: 0,
          color: theme.colorScheme.surface,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
            side: const BorderSide(color: AppColors.borderLight),
          ),
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('${visitor.visitorName} • ${visitor.flatNumber}',
                          style: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold)),
                      Text(
                        '${visitor.purpose.name} • In: ${visitor.entryTime} | Out: ${visitor.exitTime.trim().isEmpty ? '--' : visitor.exitTime}',
                        style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                      ),
                    ],
                  ),
                ),
                VisitorStatusBadge(status: visitor.status, language: language),
              ],
            ),
          ),
        );
      }),
    );
  }
}

IconData getPurposeIcon(VisitorPurpose purpose) {
  switch (purpose) {
    case VisitorPurpose.DELIVERY:
      return Icons.delivery_dining;
    case VisitorPurpose.GUEST:
      return Icons.person;
    case VisitorPurpose.CAB:
      return Icons.local_taxi;
    case VisitorPurpose.SERVICE:
      return Icons.support_agent;
    case VisitorPurpose.OTHER:
      return Icons.security;
  }
}

typedef CheckInSubmit = void Function(
    String name, String phone, String flat, VisitorPurpose purpose, String vehicle, String passcode);

// Dialog: Check-In Visitor
class CheckInVisitorDialog extends StatefulWidget {
  const CheckInVisitorDialog({super.key, required this.language, required this.onSubmit});

  final AppLanguage language;

  /// Called on a valid submit; the dialog closes itself afterwards.
  final CheckInSubmit onSubmit;

  @override
  State<CheckInVisitorDialog> createState() => _CheckInVisitorDialogState();
}

class _CheckInVisitorDialogState extends State<CheckInVisitorDialog> {
  final _name = TextEditingController();
  final _phone = TextEditingController();
  final _flat = TextEditingController();
  final _vehicle = TextEditingController();
  VisitorPurpose _selectedPurpose = VisitorPurpose.DELIVERY;

  @override
  void dispose() {
    _name.dispose();
    _phone.dispose();
    _flat.dispose();
    _vehicle.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final language = widget.language;
    final theme = Theme.of(context);
    return AlertDialog(
      insetPadding: dialogInsetPadding(context),
      title: Text(tr('check_in_visitor', language),
          style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
      content: SizedBox(
        width: double.maxFinite,
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: _name,
                decoration: const InputDecoration(
                  labelText: 'Visitor Name (अभ्यागताचे नाव)',
                ),
              ),
              const SizedBox(height: 10),
              TextField(
                controller: _phone,
                keyboardType: TextInputType.phone,
                decoration: const InputDecoration(
                  labelText: 'Mobile Number (मोबाईल)',
                ),
              ),
              const SizedBox(height: 10),
              TextField(
                controller: _flat,
                decoration: const InputDecoration(
                  labelText: 'Visiting Unit (सदनिका क्र.)',
                  hintText: 'e.g. A-101',
                ),
              ),
              const SizedBox(height: 10),
              DropdownButtonFormField<VisitorPurpose>(
                initialValue: _selectedPurpose,
                isExpanded: true,
                decoration: const InputDecoration(
                  labelText: 'Purpose (उद्देश)',
                ),
                items: [
                  for (final pur in VisitorPurpose.values) DropdownMenuItem(value: pur, child: Text(pur.name)),
                ],
                onChanged: (v) {
                  if (v != null) setState(() => _selectedPurpose = v);
                },
              ),
              const SizedBox(height: 10),
              TextField(
                controller: _vehicle,
                decoration: const InputDecoration(
                  labelText: 'Vehicle Number (वाहनाचा क्र.)',
                  hintText: 'e.g. MH 12 AB 1234',
                ),
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: Text(tr('cancel', language)),
        ),
        AppButton.elevated(
          key: const Key('submit_checkin_btn'),
          style: ElevatedButton.styleFrom(backgroundColor: AppColors.tealPrimary, foregroundColor: Colors.white),
          onPressed: () {
            if (_name.text.trim().isNotEmpty && _flat.text.trim().isNotEmpty) {
              widget.onSubmit(_name.text, _phone.text, _flat.text, _selectedPurpose, _vehicle.text, '');
              Navigator.of(context).pop();
            }
          },
          child: Text(tr('submit', language)),
        ),
      ],
    );
  }
}

// Dialog: Verify Passcode
class VerifyPasscodeDialog extends StatefulWidget {
  const VerifyPasscodeDialog({super.key, required this.language});

  final AppLanguage language;

  @override
  State<VerifyPasscodeDialog> createState() => _VerifyPasscodeDialogState();
}

class _VerifyPasscodeDialogState extends State<VerifyPasscodeDialog> {
  final _codeInput = TextEditingController();
  VisitorLog? _verifiedLog;
  String? _errorMsg;

  @override
  void dispose() {
    _codeInput.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final language = widget.language;
    final theme = Theme.of(context);
    final verified = _verifiedLog;
    return AlertDialog(
      insetPadding: dialogInsetPadding(context),
      title: Text(tr('verify_passcode', language),
          style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
      content: SizedBox(
        width: double.maxFinite,
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(language == AppLanguage.marathi
                  ? 'रहिवाशाने दिलेला ६-अंकी पासकोड टाका:'
                  : 'Enter 6-digit passcode provided by resident:'),
              const SizedBox(height: 10),
              TextField(
                controller: _codeInput,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(labelText: '6-Digit Code'),
              ),
              if (_errorMsg != null) ...[
                const SizedBox(height: 10),
                Text(_errorMsg!, style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.error)),
              ],
              if (verified != null) ...[
                const SizedBox(height: 10),
                Card(
                  margin: EdgeInsets.zero,
                  elevation: 0,
                  color: AppColors.tealContainer,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                  child: Padding(
                    padding: const EdgeInsets.all(10),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('VALID PASSCODE ✅',
                            style: theme.textTheme.titleSmall
                                ?.copyWith(fontWeight: FontWeight.bold, color: AppColors.tealPrimary)),
                        Text('Visitor: ${verified.visitorName}'),
                        Text('Visiting Flat: ${verified.flatNumber}'),
                        Text('Purpose: ${verified.purpose.name}'),
                      ],
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: Text(tr('close', language)),
        ),
        AppButton.elevated(
          key: const Key('submit_verify_passcode_btn'),
          style: ElevatedButton.styleFrom(backgroundColor: AppColors.tealPrimary, foregroundColor: Colors.white),
          onPressed: () {
            final vm = context.read<SocietyViewModel>();
            final log = _verifiedLog;
            if (log != null) {
              vm.checkInVisitor(log.visitorName, log.phone, log.flatNumber, log.purpose, log.vehicleNumber, log.passcode);
              Navigator.of(context).pop();
            } else {
              vm.verifyPasscode(_codeInput.text, onResult: (found) {
                if (!mounted) return;
                setState(() {
                  if (found != null) {
                    _verifiedLog = found;
                    _errorMsg = null;
                  } else {
                    _errorMsg = language == AppLanguage.marathi ? 'अवैध किंवा कालबाह्य पासकोड!' : 'Invalid or expired passcode!';
                  }
                });
              });
            }
          },
          child: Text(verified != null ? tr('check_in_visitor', language) : 'Verify'),
        ),
      ],
    );
  }
}
