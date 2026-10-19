// Port of ui/screens/CommunityScreen.kt.
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../data/models.dart';
import '../../l10n/app_strings.dart';
import '../../state/society_view_model.dart';
import '../components/common_components.dart';
import '../theme/app_colors.dart';

class CommunityScreen extends StatefulWidget {
  const CommunityScreen({super.key});

  @override
  State<CommunityScreen> createState() => _CommunityScreenState();
}

class _CommunityScreenState extends State<CommunityScreen> with SingleTickerProviderStateMixin {
  late final TabController _tabs = TabController(length: 4, vsync: this)
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
      tr('notice_board', language),
      tr('helpdesk_title', language),
      tr('emergency_title', language),
      tr('vehicle_directory', language),
    ];
    final selected = _tabs.index;

    final Widget body = switch (selected) {
      0 => const NoticeBoardView(),
      1 => const HelpdeskView(),
      2 => const EmergencyDirectoryView(),
      _ => const VehicleDirectoryView(),
    };

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
                    key: Key('community_subtab_$i'),
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
            child: FadeThroughSwitcher(child: KeyedSubtree(key: ValueKey('community_$selected'), child: body)),
          ),
        ],
      ),
    );
  }
}

// ------------------------------------------------------------ shared bits

ShapeBorder _cardShape(double radius, {Color border = AppColors.borderLight}) => RoundedRectangleBorder(
      borderRadius: BorderRadius.circular(radius),
      side: BorderSide(color: border),
    );

ButtonStyle _filledStyle(Color color, {double radius = 8}) => ElevatedButton.styleFrom(
      backgroundColor: color,
      foregroundColor: Colors.white,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(radius)),
    );

Widget _smallActionButton(BuildContext context, {required Key key, required String label, required VoidCallback onPressed}) {
  return AppButton.elevatedIcon(
    key: key,
    onPressed: onPressed,
    style: _filledStyle(AppColors.tealPrimary),
    icon: const Icon(Icons.add, size: 16),
    label: Text(label, style: Theme.of(context).textTheme.labelSmall?.copyWith(color: Colors.white)),
  );
}

Widget _spacedList(List<Widget> items, double spacing) => ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: items.length,
      separatorBuilder: (_, _) => SizedBox(height: spacing),
      itemBuilder: (_, i) => AppEntrance(index: i, child: items[i]),
    );

/// Read-only dropdown for an enum, standing in for Compose's ExposedDropdownMenuBox.
Widget _enumDropdown<T>({
  required T value,
  required List<T> values,
  required String label,
  required String Function(T) display,
  required ValueChanged<T> onChanged,
}) {
  return DropdownButtonFormField<T>(
    initialValue: value,
    isExpanded: true,
    decoration: InputDecoration(labelText: label),
    items: [for (final v in values) DropdownMenuItem<T>(value: v, child: Text(display(v)))],
    onChanged: (v) {
      if (v != null) onChanged(v);
    },
  );
}

// 1. Notice Board & Bye-Laws View
class NoticeBoardView extends StatelessWidget {
  const NoticeBoardView({super.key});

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final language = vm.language;
    final currentUser = vm.currentUser;
    final notices = vm.notices;
    final theme = Theme.of(context);
    final role = currentUser?.role;

    final canPost =
        role == UserRole.CHAIRMAN || role == UserRole.SECRETARY || role == UserRole.COMMITTEE || currentUser == null;
    final canDelete = role == UserRole.CHAIRMAN || role == UserRole.SECRETARY;

    final items = <Widget>[
      Row(
        children: [
          Expanded(
            child: Text(tr('notice_board', language),
                style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
          ),
          if (canPost)
            _smallActionButton(
              context,
              key: const Key('post_notice_btn'),
              label: tr('post_notice', language),
              onPressed: () => showAppDialog<void>(
                context: context,
                builder: (_) => PostNoticeDialog(
                  language: language,
                  onSubmit: (title, content, isPinned, cat) =>
                      context.read<SocietyViewModel>().postNotice(title, content, isPinned, cat),
                ),
              ),
            ),
        ],
      ),
      // Bye-Laws Card snippet
      Card(
        margin: EdgeInsets.zero,
        elevation: 0,
        color: AppColors.surfaceVariant,
        shape: _cardShape(14),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  const Icon(Icons.gavel, color: AppColors.tealPrimary, size: 20),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      language == AppLanguage.marathi
                          ? 'महाराष्ट्र सहकारी गृहनिर्माण उपविधी व नियम'
                          : 'Maharashtra Housing Society Bye-Laws & Rules',
                      style: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 6),
              Text(
                language == AppLanguage.marathi
                    ? '• शांतता वेळ: रात्री १० ते सकाळी ६\n• सदनिका अंतर्गत दुरुस्ती: सकाळी ९ ते संध्याकाळी ६ (रविवारी बंद)\n• भाडेकरू पोलीस व्हेरिफिकेशन व ना-हरकत अनिवार्य\n• पार्किंग जागेचा गैरवापर केल्यास प्रति दिन ₹५०० दंड'
                    : '• Silence Hours: 10:00 PM to 06:00 AM\n• Unit Renovation Works: 09:00 AM to 06:00 PM (No Sunday work)\n• Police Verification and Tenant NOC mandatory for sub-letting\n• Misuse of designated parking slot attracts ₹500/day fine',
                style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
            ],
          ),
        ),
      ),
      for (final notice in notices)
        Card(
          margin: EdgeInsets.zero,
          elevation: 0,
          color: notice.isPinned ? const Color(0xFFFFFBEB) : theme.colorScheme.surface,
          shape: _cardShape(14, border: notice.isPinned ? const Color(0xFFFDE68A) : AppColors.borderLight),
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
                          if (notice.isPinned) ...[
                            const Icon(Icons.push_pin, semanticLabel: 'Pinned', color: AppColors.warningAmber, size: 18),
                            const SizedBox(width: 6),
                          ],
                          Expanded(
                            child: Text(
                              notice.title,
                              style: theme.textTheme.titleSmall?.copyWith(
                                fontWeight: FontWeight.bold,
                                color: notice.isPinned ? const Color(0xFF92400E) : theme.colorScheme.onSurface,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                    if (canDelete)
                      SizedBox(
                        width: 28,
                        height: 28,
                        child: IconButton(
                          padding: EdgeInsets.zero,
                          tooltip: 'Delete',
                          onPressed: () => context.read<SocietyViewModel>().deleteNotice(notice),
                          icon: const Icon(Icons.delete, color: AppColors.statusOverdue, size: 16),
                        ),
                      ),
                  ],
                ),
                const SizedBox(height: 6),
                Text(
                  notice.content,
                  style: theme.textTheme.bodyMedium?.copyWith(
                    color: notice.isPinned ? const Color(0xFFB45309) : theme.colorScheme.onSurfaceVariant,
                  ),
                ),
                const Divider(height: 16),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Flexible(
                      child: Text('Published by: ${notice.authorRole}',
                          style: theme.textTheme.bodySmall?.copyWith(color: AppColors.tealPrimary)),
                    ),
                    Text(notice.publishedDate,
                        style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
                  ],
                ),
              ],
            ),
          ),
        ),
    ];

    return _spacedList(items, 12);
  }
}

// 2. Helpdesk & Complaints View
class HelpdeskView extends StatefulWidget {
  const HelpdeskView({super.key});

  @override
  State<HelpdeskView> createState() => _HelpdeskViewState();
}

class _HelpdeskViewState extends State<HelpdeskView> {
  bool _showAllUnits = false;

  void _confirmWithdraw(Complaint complaint, AppLanguage language) {
    final theme = Theme.of(context);
    showAppDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        insetPadding: dialogInsetPadding(dialogContext),
        title: Text(
          language == AppLanguage.marathi ? 'तक्रार मागे घ्यायची?' : 'Withdraw this complaint?',
          style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
        ),
        content: Text(complaint.description, style: theme.textTheme.bodySmall),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: Text(tr('cancel', language)),
          ),
          AppButton.elevated(
            key: const Key('confirm_delete_complaint_btn'),
            style: _filledStyle(AppColors.statusOverdue, radius: 20),
            onPressed: () {
              final vm = dialogContext.read<SocietyViewModel>();
              vm.deleteMyComplaint(complaint, onResult: (_, message) => vm.showMessage(message));
              Navigator.of(dialogContext).pop();
            },
            child: Text(language == AppLanguage.marathi ? 'मागे घ्या' : 'Withdraw'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final language = vm.language;
    final currentUser = vm.currentUser;
    final theme = Theme.of(context);
    /*
     * Only what this user is allowed to see. A complaint is between the unit
     * that raised it and the committee that has to act on it, so the committee
     * gets every ticket and a member gets their own units' and nobody else's -
     * Unit 304's leak was being shown to Unit 202.
     */
    final complaints = vm.visibleComplaints;
    final myUnits = vm.myUnits;
    final activeUnit = vm.activeUnit;
    final role = currentUser?.role;

    final activeUnitNumber = (activeUnit ?? currentUser)?.flatNumber ?? '';
    // Narrowed once more by the unit picker, for a member who holds several.
    final listedComplaints = myUnits.length > 1 && !_showAllUnits
        ? complaints.where((c) => c.flatNumber.toLowerCase() == activeUnitNumber.toLowerCase()).toList()
        : complaints;

    final isCommittee = role == UserRole.CHAIRMAN || role == UserRole.SECRETARY || role == UserRole.COMMITTEE;

    final items = <Widget>[
      Row(
        children: [
          Expanded(
            child: Text(tr('helpdesk_title', language),
                style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
          ),
          _smallActionButton(
            context,
            key: const Key('file_complaint_btn'),
            label: tr('file_complaint', language),
            onPressed: () => showAppDialog<void>(
              context: context,
              builder: (_) => FileComplaintDialog(
                currentUser: activeUnit ?? currentUser,
                myUnits: myUnits,
                language: language,
                onSubmit: (cat, desc, flat, name) =>
                    context.read<SocietyViewModel>().fileComplaint(cat, desc, flat, name),
              ),
            ),
          ),
        ],
      ),
      // A member who owns several units can narrow the list further still;
      // the committee, which sees every ticket, has nothing to narrow.
      if (myUnits.length > 1)
        Wrap(
          spacing: 6,
          children: [
            FilterChip(
              key: const Key('helpdesk_filter_my_unit'),
              selected: !_showAllUnits,
              onSelected: (_) => setState(() => _showAllUnits = false),
              label: Text('${tr('select_unit', language)}: $activeUnitNumber', style: theme.textTheme.labelSmall),
            ),
            FilterChip(
              key: const Key('helpdesk_filter_all'),
              selected: _showAllUnits,
              onSelected: (_) => setState(() => _showAllUnits = true),
              label: Text(tr('filter_all', language), style: theme.textTheme.labelSmall),
            ),
          ],
        ),
      for (final complaint in listedComplaints)
        _complaintCard(context, complaint, language, myUnits, isCommittee),
    ];

    return _spacedList(items, 12);
  }

  Widget _complaintCard(
      BuildContext context, Complaint complaint, AppLanguage language, List<Resident> myUnits, bool isCommittee) {
    final theme = Theme.of(context);
    /*
     * The member who raised it may correct or withdraw it, but only while it
     * is still open - once the committee has picked it up, editing it
     * underneath them would rewrite what they are working from.
     */
    final isMine = myUnits.any((u) => u.flatNumber.toLowerCase() == complaint.flatNumber.toLowerCase());
    return Card(
      margin: EdgeInsets.zero,
      elevation: 0,
      color: theme.colorScheme.surface,
      shape: _cardShape(14),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '${language == AppLanguage.marathi ? complaint.category.labelMr : complaint.category.labelEn} • ${complaint.flatNumber}',
                        style: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
                      ),
                      Text(
                        'By: ${complaint.residentName} • Filed: ${complaint.filedDate}',
                        style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                      ),
                    ],
                  ),
                ),
                TicketStatusBadge(status: complaint.status, language: language),
              ],
            ),
            const SizedBox(height: 8),
            Text(complaint.description, style: theme.textTheme.bodyMedium),
            if (complaint.resolutionNotes.trim().isNotEmpty) ...[
              const SizedBox(height: 6),
              Text('Resolution: ${complaint.resolutionNotes}',
                  style: theme.textTheme.bodySmall?.copyWith(color: AppColors.statusPaid)),
            ],
            if (isMine && complaint.status == TicketStatus.OPEN) ...[
              const Divider(height: 16),
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  TextButton.icon(
                    key: Key('edit_complaint_btn_${complaint.id}'),
                    onPressed: () => showAppDialog<void>(
                      context: context,
                      builder: (_) => EditComplaintDialog(complaint: complaint, language: language),
                    ),
                    icon: const Icon(Icons.edit, color: AppColors.tealPrimary, size: 16),
                    label: Text(tr('edit_profile', language),
                        style: theme.textTheme.labelMedium?.copyWith(color: AppColors.tealPrimary)),
                  ),
                  TextButton.icon(
                    key: Key('delete_complaint_btn_${complaint.id}'),
                    onPressed: () => _confirmWithdraw(complaint, language),
                    icon: const Icon(Icons.delete, color: AppColors.statusOverdue, size: 16),
                    label: Text(language == AppLanguage.marathi ? 'मागे घ्या' : 'Withdraw',
                        style: theme.textTheme.labelMedium?.copyWith(color: AppColors.statusOverdue)),
                  ),
                ],
              ),
            ],
            // Admin resolution controls
            if (isCommittee) ...[
              const Divider(height: 16),
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  if (complaint.status != TicketStatus.RESOLVED)
                    TextButton(
                      onPressed: () => context
                          .read<SocietyViewModel>()
                          .updateComplaintStatus(complaint, TicketStatus.RESOLVED, 'Resolved by Managing Committee'),
                      child: Text(
                        language == AppLanguage.marathi ? 'निवारण पूर्ण (Mark Resolved)' : 'Mark Resolved',
                        style: theme.textTheme.labelMedium
                            ?.copyWith(fontWeight: FontWeight.bold, color: AppColors.statusPaid),
                      ),
                    ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }
}

// 3. Emergency Contacts Directory
class EmergencyDirectoryView extends StatelessWidget {
  const EmergencyDirectoryView({super.key});

  void _openEditor(BuildContext context, EmergencyContact contact, AppLanguage language) {
    showAppDialog<void>(
      context: context,
      builder: (_) => EmergencyContactDialog(
        contact: contact,
        language: language,
        onSave: (updated) {
          final vm = context.read<SocietyViewModel>();
          vm.saveEmergencyContact(updated, onResult: (_, message) => vm.showMessage(message));
        },
        onDelete: contact.docId.trim().isEmpty
            ? null
            : () {
                final vm = context.read<SocietyViewModel>();
                vm.deleteEmergencyContact(contact, onResult: (_, message) => vm.showMessage(message));
              },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final language = vm.language;
    final theme = Theme.of(context);
    final canManage = vm.currentUser?.role == UserRole.CHAIRMAN;

    // Members get the numbers that are actually in service. The committee sees
    // the inactive ones too, so they can be switched back on.
    final contacts = canManage ? vm.emergencyContacts : vm.emergencyContacts.where((c) => c.isActive).toList();

    final items = <Widget>[
      Padding(
        padding: const EdgeInsets.only(bottom: 6),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(tr('emergency_title', language),
                      style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
                  Text(
                    language == AppLanguage.marathi ? '१-टॅप त्वरित आपत्कालीन कॉल सेवा' : '1-Tap Direct Emergency Calling Services',
                    style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                  ),
                ],
              ),
            ),
            // Only the Chairman can add or change these numbers.
            if (canManage)
              _smallActionButton(
                context,
                key: const Key('add_emergency_contact_btn'),
                label: tr('add_emergency_contact', language),
                onPressed: () => _openEditor(
                  context,
                  const EmergencyContact(titleEn: '', titleMr: '', name: '', phoneNumber: '', category: 'Society'),
                  language,
                ),
              ),
          ],
        ),
      ),
      for (final contact in contacts) _contactCard(context, contact, language, canManage),
    ];

    return _spacedList(items, 10);
  }

  Widget _contactCard(BuildContext context, EmergencyContact contact, AppLanguage language, bool canManage) {
    final theme = Theme.of(context);
    final cat = contact.category.toLowerCase();
    final isUrgent = cat.contains('police') || cat.contains('fire') || cat.contains('ambulance');
    return Card(
      margin: EdgeInsets.zero,
      elevation: 0,
      color: theme.colorScheme.surface,
      shape: _cardShape(14),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => launchUrl(Uri.parse('tel:${contact.phoneNumber}')),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Row(
            children: [
              Expanded(
                child: Row(
                  children: [
                    Container(
                      width: 42,
                      height: 42,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: isUrgent ? AppColors.statusOverdue.withValues(alpha: 0.15) : AppColors.tealContainer,
                      ),
                      alignment: Alignment.center,
                      child: Icon(
                        getEmergencyIcon(contact.category),
                        color: isUrgent ? AppColors.statusOverdue : AppColors.tealPrimary,
                        size: 22,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Wrap(
                            crossAxisAlignment: WrapCrossAlignment.center,
                            spacing: 6,
                            children: [
                              Text(
                                language == AppLanguage.marathi ? contact.titleMr : contact.titleEn,
                                style: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
                              ),
                              // Only the committee ever sees this - members
                              // are not shown inactive contacts at all.
                              if (!contact.isActive)
                                Container(
                                  key: const Key('emergency_inactive_chip'),
                                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                  decoration: BoxDecoration(
                                    color: AppColors.statusOverdueBg,
                                    borderRadius: BorderRadius.circular(6),
                                  ),
                                  child: Text(
                                    'Inactive',
                                    style: theme.textTheme.labelSmall
                                        ?.copyWith(fontWeight: FontWeight.bold, color: AppColors.statusOverdue),
                                  ),
                                ),
                            ],
                          ),
                          Text(
                            '${contact.name} • ${contact.phoneNumber}',
                            style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              if (canManage)
                IconButton(
                  key: Key('edit_emergency_${contact.docId}'),
                  tooltip: 'Edit contact',
                  onPressed: () => _openEditor(context, contact, language),
                  icon: const Icon(Icons.edit, color: AppColors.tealPrimary),
                ),
              Container(
                width: 38,
                height: 38,
                decoration: const BoxDecoration(color: AppColors.statusPaidBg, shape: BoxShape.circle),
                alignment: Alignment.center,
                child: const Icon(Icons.call, semanticLabel: 'Call', color: AppColors.statusPaid, size: 20),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// 4. Vehicle & Parking Directory View
class VehicleDirectoryView extends StatefulWidget {
  const VehicleDirectoryView({super.key});

  @override
  State<VehicleDirectoryView> createState() => _VehicleDirectoryViewState();
}

class _VehicleDirectoryViewState extends State<VehicleDirectoryView> {
  String _searchQuery = '';

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final currentUser = vm.currentUser;
    final theme = Theme.of(context);
    // Members only. A guard's or a sweeper's bay is not part of the residents'
    // parking list.
    final residents = vm.members;

    // A member sees who parks where, but not everybody's phone number - the
    // committee and the gate keep that. Members can still see their own.
    final isMember = currentUser?.role == UserRole.RESIDENT || currentUser?.role == UserRole.TENANT;

    final q = _searchQuery.toLowerCase();
    final filtered = residents
        .where((it) =>
            it.parkingSlot.toLowerCase().contains(q) ||
            it.flatNumber.toLowerCase().contains(q) ||
            it.twoWheelerNumber.toLowerCase().contains(q) ||
            it.fourWheelerNumber.toLowerCase().contains(q) ||
            it.ownerName.toLowerCase().contains(q))
        .toList();

    final items = <Widget>[
      TextField(
        onChanged: (v) => setState(() => _searchQuery = v),
        decoration: const InputDecoration(
          labelText: 'Search by plate number, flat or parking slot',
          prefixIcon: Icon(Icons.search),
        ),
      ),
      for (final resident in filtered)
        Card(
          margin: EdgeInsets.zero,
          elevation: 0,
          color: theme.colorScheme.surface,
          shape: _cardShape(14),
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('${resident.flatNumber} • ${resident.ownerName}',
                          style: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold)),
                      Text(
                        '🅿️ Slot: ${resident.parkingSlot.trim().isEmpty ? 'N/A' : resident.parkingSlot}',
                        style: theme.textTheme.bodySmall
                            ?.copyWith(fontWeight: FontWeight.bold, color: AppColors.tealPrimary),
                      ),
                      if (resident.fourWheelerNumber.trim().isNotEmpty)
                        Text('🚗 4W: ${resident.fourWheelerNumber}', style: theme.textTheme.bodySmall),
                      if (resident.twoWheelerNumber.trim().isNotEmpty)
                        Text('🛵 2W: ${resident.twoWheelerNumber}', style: theme.textTheme.bodySmall),
                    ],
                  ),
                ),
                if (!isMember || resident.flatNumber.toLowerCase() == (currentUser?.flatNumber.toLowerCase() ?? ''))
                  Container(
                    decoration: BoxDecoration(
                      color: AppColors.surfaceVariant,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    child: Text(
                      '📞 ${resident.contactNumber}',
                      key: Key('parking_contact_${resident.flatNumber}'),
                      style: theme.textTheme.labelSmall,
                    ),
                  ),
              ],
            ),
          ),
        ),
    ];

    return _spacedList(items, 10);
  }
}

IconData getEmergencyIcon(String category) {
  final lower = category.toLowerCase();
  if (lower.contains('police')) return Icons.local_police;
  if (lower.contains('fire')) return Icons.emergency;
  if (lower.contains('ambulance') || lower.contains('hospital')) return Icons.local_hospital;
  if (lower.contains('lift')) return Icons.security;
  if (lower.contains('gate') || lower.contains('security')) return Icons.security;
  return Icons.support_agent;
}

// Dialog: Post Notice
class PostNoticeDialog extends StatefulWidget {
  const PostNoticeDialog({super.key, required this.language, required this.onSubmit});

  final AppLanguage language;

  /// Called on a valid submit; the dialog closes itself afterwards.
  final void Function(String title, String content, bool isPinned, NoticeCategory cat) onSubmit;

  @override
  State<PostNoticeDialog> createState() => _PostNoticeDialogState();
}

class _PostNoticeDialogState extends State<PostNoticeDialog> {
  final _title = TextEditingController();
  final _content = TextEditingController();
  bool _isPinned = false;
  NoticeCategory _selectedCat = NoticeCategory.GENERAL;

  @override
  void dispose() {
    _title.dispose();
    _content.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final language = widget.language;
    final theme = Theme.of(context);
    return AlertDialog(
      insetPadding: dialogInsetPadding(context),
      title: Text(tr('post_notice', language), style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
      content: SizedBox(
        width: double.maxFinite,
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: _title,
                decoration: const InputDecoration(labelText: 'Notice Title (विषय)'),
              ),
              const SizedBox(height: 10),
              _enumDropdown<NoticeCategory>(
                value: _selectedCat,
                values: NoticeCategory.values,
                label: 'Category',
                display: (c) => c.name,
                onChanged: (c) => setState(() => _selectedCat = c),
              ),
              const SizedBox(height: 10),
              TextField(
                controller: _content,
                minLines: 3,
                maxLines: null,
                decoration: const InputDecoration(
                  labelText: 'Notice Details (सविस्तर माहिती)',
                ),
              ),
              const SizedBox(height: 10),
              Row(
                children: [
                  const Expanded(child: Text('Pin to top (महत्त्वाची सूचना)')),
                  Switch(value: _isPinned, onChanged: (v) => setState(() => _isPinned = v)),
                ],
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.of(context).pop(), child: Text(tr('cancel', language))),
        AppButton.elevated(
          style: _filledStyle(AppColors.tealPrimary, radius: 20),
          onPressed: () {
            if (_title.text.trim().isNotEmpty && _content.text.trim().isNotEmpty) {
              widget.onSubmit(_title.text, _content.text, _isPinned, _selectedCat);
              Navigator.of(context).pop();
            }
          },
          child: Text(tr('submit', language)),
        ),
      ],
    );
  }
}

/// A member correcting the complaint they raised, while it is still open.
class EditComplaintDialog extends StatefulWidget {
  const EditComplaintDialog({super.key, required this.complaint, required this.language});

  final Complaint complaint;
  final AppLanguage language;

  @override
  State<EditComplaintDialog> createState() => _EditComplaintDialogState();
}

class _EditComplaintDialogState extends State<EditComplaintDialog> {
  late ComplaintCategory _selectedCat = widget.complaint.category;
  late final TextEditingController _desc = TextEditingController(text: widget.complaint.description);
  String? _error;

  @override
  void dispose() {
    _desc.dispose();
    super.dispose();
  }

  void _save() {
    final vm = context.read<SocietyViewModel>();
    vm.editMyComplaint(widget.complaint, _selectedCat, _desc.text, onResult: (ok, message) {
      if (ok) {
        vm.showMessage(message);
        if (mounted) Navigator.of(context).pop();
      } else if (mounted) {
        setState(() => _error = message);
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final language = widget.language;
    final complaint = widget.complaint;
    final theme = Theme.of(context);
    String label(ComplaintCategory c) => language == AppLanguage.marathi ? c.labelMr : c.labelEn;
    return AlertDialog(
      insetPadding: dialogInsetPadding(context),
      title: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            language == AppLanguage.marathi ? 'तक्रार संपादित करा' : 'Edit Complaint',
            style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
          ),
          Text('${complaint.flatNumber} • ${complaint.filedDate}',
              style: theme.textTheme.bodySmall?.copyWith(color: AppColors.tealPrimary)),
        ],
      ),
      content: SizedBox(
        width: double.maxFinite,
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _enumDropdown<ComplaintCategory>(
                value: _selectedCat,
                values: ComplaintCategory.values,
                label: 'Issue Type (तक्रार प्रकार)',
                display: label,
                onChanged: (c) => setState(() => _selectedCat = c),
              ),
              const SizedBox(height: 10),
              TextField(
                key: const Key('edit_complaint_desc_input'),
                controller: _desc,
                minLines: 3,
                maxLines: null,
                onChanged: (_) {
                  if (_error != null) setState(() => _error = null);
                },
                decoration: const InputDecoration(
                  labelText: 'Complaint Details (तपशील)',
                ),
              ),
              if (_error != null) ...[
                const SizedBox(height: 10),
                Text(_error!, style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.error)),
              ],
            ],
          ),
        ),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.of(context).pop(), child: Text(tr('cancel', language))),
        AppButton.elevated(
          key: const Key('save_complaint_btn'),
          style: _filledStyle(AppColors.tealPrimary, radius: 20),
          onPressed: _save,
          child: Text(tr('save', language)),
        ),
      ],
    );
  }
}

// Dialog: File Complaint
class FileComplaintDialog extends StatefulWidget {
  const FileComplaintDialog({
    super.key,
    required this.currentUser,
    this.myUnits = const [],
    required this.language,
    required this.onSubmit,
  });

  final Resident? currentUser;

  /// Every unit the signed-in member holds. One unit fills the form in and
  /// locks it; several turn the unit number into a picker, and the resident
  /// name follows whichever unit is chosen.
  final List<Resident> myUnits;
  final AppLanguage language;

  /// Called on a valid submit; the dialog closes itself afterwards.
  final void Function(ComplaintCategory cat, String desc, String flat, String name) onSubmit;

  @override
  State<FileComplaintDialog> createState() => _FileComplaintDialogState();
}

class _FileComplaintDialogState extends State<FileComplaintDialog> {
  ComplaintCategory _selectedCat = ComplaintCategory.PLUMBING;
  final _desc = TextEditingController();
  // Filled in from the signed-in member - never typed by hand.
  late String _flat = widget.currentUser?.flatNumber ?? '';

  @override
  void dispose() {
    _desc.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final language = widget.language;
    final theme = Theme.of(context);
    Resident? selectedUnit = widget.currentUser;
    for (final u in widget.myUnits) {
      if (u.flatNumber.toLowerCase() == _flat.toLowerCase()) {
        selectedUnit = u;
        break;
      }
    }
    final owner = selectedUnit?.ownerName ?? '';
    final name = owner.trim().isEmpty ? 'Resident' : owner;
    String label(ComplaintCategory c) => language == AppLanguage.marathi ? c.labelMr : c.labelEn;

    return AlertDialog(
      insetPadding: dialogInsetPadding(context),
      title: Text(tr('file_complaint', language), style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
      content: SizedBox(
        width: double.maxFinite,
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              _enumDropdown<ComplaintCategory>(
                value: _selectedCat,
                values: ComplaintCategory.values,
                label: 'Issue Type (तक्रार प्रकार)',
                display: label,
                onChanged: (c) => setState(() => _selectedCat = c),
              ),
              const SizedBox(height: 10),
              // The unit and the name come from who is signed in. A member
              // with more than one unit chooses which one this is about.
              if (widget.myUnits.length > 1)
                SearchableDropdownField(
                  label: tr('unit_number', language),
                  value: _flat,
                  options: widget.myUnits.map((u) => u.flatNumber).toList(),
                  onValueChange: (v) => setState(() => _flat = v),
                  allowFreeText: false,
                  testTag: 'complaint_unit_dropdown',
                )
              else
                TextFormField(
                  key: const Key('complaint_unit_field'),
                  initialValue: _flat,
                  readOnly: true,
                  enabled: false,
                  decoration: InputDecoration(labelText: tr('unit_number', language)),
                ),
              const SizedBox(height: 10),
              // Re-keyed on the name so it follows the chosen unit.
              KeyedSubtree(
                key: ValueKey('resident_name_$name'),
                child: TextFormField(
                  key: const Key('complaint_resident_name_field'),
                  initialValue: name,
                  readOnly: true,
                  enabled: false,
                  decoration: const InputDecoration(labelText: 'Resident Name'),
                ),
              ),
              const SizedBox(height: 10),
              TextField(
                controller: _desc,
                minLines: 3,
                maxLines: null,
                decoration: const InputDecoration(
                  labelText: 'Complaint Details (तपशील)',
                ),
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.of(context).pop(), child: Text(tr('cancel', language))),
        AppButton.elevated(
          style: _filledStyle(AppColors.tealPrimary, radius: 20),
          onPressed: () {
            if (_desc.text.trim().isNotEmpty) {
              widget.onSubmit(_selectedCat, _desc.text, _flat, name);
              Navigator.of(context).pop();
            }
          },
          child: Text(tr('submit', language)),
        ),
      ],
    );
  }
}

const List<String> _emergencyCategories = [
  'Police', 'Fire', 'Ambulance', 'Hospital', 'Security', 'Plumber', //
  'Electrician', 'Lift Technician', 'Water Tanker', 'Society',
];

/// Add / edit / delete one emergency contact. Chairman-only entry point.
class EmergencyContactDialog extends StatefulWidget {
  const EmergencyContactDialog({
    super.key,
    required this.contact,
    required this.language,
    required this.onSave,
    required this.onDelete,
  });

  final EmergencyContact contact;
  final AppLanguage language;

  /// Called with the edited contact; the dialog closes itself afterwards.
  final ValueChanged<EmergencyContact> onSave;

  /// Null for a contact that has not been saved yet.
  final VoidCallback? onDelete;

  @override
  State<EmergencyContactDialog> createState() => _EmergencyContactDialogState();
}

class _EmergencyContactDialogState extends State<EmergencyContactDialog> {
  late final _titleEn = TextEditingController(text: widget.contact.titleEn);
  late final _titleMr = TextEditingController(text: widget.contact.titleMr);
  late final _name = TextEditingController(text: widget.contact.name);
  late final _phone = TextEditingController(text: widget.contact.phoneNumber);
  late String _category = widget.contact.category;
  late bool _isActive = widget.contact.isActive;
  String? _error;

  @override
  void initState() {
    super.initState();
    // The save button is enabled only while a title and a number are present.
    _titleEn.addListener(_refresh);
    _phone.addListener(_refresh);
  }

  void _refresh() => setState(() {});

  @override
  void dispose() {
    _titleEn.dispose();
    _titleMr.dispose();
    _name.dispose();
    _phone.dispose();
    super.dispose();
  }

  void _onPhoneChanged(String raw) {
    var cleaned = raw.split('').where((ch) => RegExp(r'[0-9+]').hasMatch(ch)).join();
    if (cleaned.length > 15) cleaned = cleaned.substring(0, 15);
    if (cleaned != raw) {
      _phone.value = TextEditingValue(text: cleaned, selection: TextSelection.collapsed(offset: cleaned.length));
    }
  }

  void _save() {
    // An emergency number that cannot be dialled is worse than none at all,
    // so the shape is checked before it is saved.
    final digits = _phone.text.replaceAll(RegExp(r'[^0-9]'), '');
    if (digits.length < EMERGENCY_PHONE_MIN_DIGITS || digits.length > EMERGENCY_PHONE_MAX_DIGITS) {
      setState(() => _error = 'Enter a valid phone number - 3 to 15 digits, '
          'so short codes like 100 and full mobile numbers both work.');
      return;
    }
    setState(() => _error = null);
    final titleEn = _titleEn.text.trim();
    final titleMr = _titleMr.text.trim();
    final category = _category.trim();
    widget.onSave(widget.contact.copyWith(
      titleEn: titleEn,
      titleMr: titleMr.isEmpty ? titleEn : titleMr,
      name: _name.text.trim(),
      phoneNumber: _phone.text.trim(),
      category: category.isEmpty ? 'Society' : category,
      isActive: _isActive,
    ));
    Navigator.of(context).pop();
  }

  @override
  Widget build(BuildContext context) {
    final language = widget.language;
    final theme = Theme.of(context);
    final canSave = _titleEn.text.trim().isNotEmpty && _phone.text.trim().isNotEmpty;
    return AlertDialog(
      insetPadding: dialogInsetPadding(context),
      title: Text(
        widget.contact.docId.trim().isEmpty ? tr('add_emergency_contact', language) : tr('emergency_title', language),
        style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
      ),
      content: SizedBox(
        width: double.maxFinite,
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              TextField(
                key: const Key('emergency_title_input'),
                controller: _titleEn,
                decoration: const InputDecoration(
                  labelText: 'Title (English)',
                  hintText: 'e.g. Society Plumber',
                ),
              ),
              const SizedBox(height: 8),
              TextField(
                controller: _titleMr,
                decoration: const InputDecoration(labelText: 'Title (मराठी)'),
              ),
              const SizedBox(height: 8),
              TextField(
                controller: _name,
                decoration: const InputDecoration(labelText: 'Contact Person / Agency'),
              ),
              const SizedBox(height: 8),
              TextField(
                key: const Key('emergency_phone_input'),
                controller: _phone,
                keyboardType: TextInputType.phone,
                onChanged: _onPhoneChanged,
                decoration: InputDecoration(labelText: tr('mobile_number', language)),
              ),
              const SizedBox(height: 8),
              SearchableDropdownField(
                label: 'Category',
                value: _category,
                options: _emergencyCategories,
                onValueChange: (v) => setState(() => _category = v),
              ),
              const SizedBox(height: 8),
              // Inactive contacts stay on file for the committee but are not
              // shown to members - a number that is out of service must not
              // sit on an emergency list.
              Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(_isActive ? 'Active' : 'Inactive',
                            style: theme.textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.bold)),
                        Text(
                          _isActive ? 'Shown to every member.' : 'Kept on file, hidden from members.',
                          style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                        ),
                      ],
                    ),
                  ),
                  Switch(
                    key: const Key('emergency_active_switch'),
                    value: _isActive,
                    onChanged: (v) => setState(() => _isActive = v),
                  ),
                ],
              ),
              if (_error != null) ...[
                const SizedBox(height: 8),
                Text(
                  _error!,
                  key: const Key('emergency_error_text'),
                  style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.error),
                ),
              ],
            ],
          ),
        ),
      ),
      actions: [
        if (widget.onDelete != null)
          TextButton(
            onPressed: () {
              widget.onDelete!();
              Navigator.of(context).pop();
            },
            child: Text(tr('delete', language), style: const TextStyle(color: AppColors.statusOverdue)),
          ),
        TextButton(onPressed: () => Navigator.of(context).pop(), child: Text(tr('cancel', language))),
        AppButton.elevated(
          key: const Key('save_emergency_contact_btn'),
          style: _filledStyle(AppColors.tealPrimary, radius: 20),
          onPressed: canSave ? _save : null,
          child: Text(tr('submit', language)),
        ),
      ],
    );
  }
}
