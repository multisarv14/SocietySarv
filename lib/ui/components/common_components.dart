// Port of ui/components/CommonComponents.kt.
//
// Public API:
//   class SocietyTopAppBar extends StatelessWidget implements PreferredSizeWidget
//     SocietyTopAppBar({Key? key, required SocietyInfo? societyInfo, required Resident? currentUser,
//       required AppLanguage currentLanguage, required VoidCallback onToggleLanguage,
//       ValueChanged<AppLanguage>? onSelectLanguage, VoidCallback? onOpenSocietyProfile,
//       VoidCallback? onOpenUserProfile})
//   class SocietyLogoMark  ({Key? key, required String? logoBase64, required double size})
//   class MemberAvatar     ({Key? key, required String? photoBase64, required String name, required double size, VoidCallback? onTap})
//   String getRoleLabel(UserRole role, AppLanguage language)
//   class FinancialKpiCard ({Key? key, required String title, required double amount, String? subtitle,
//                            required IconData icon, Color accentColor = AppColors.tealPrimary})
//   class SectionHeader    ({Key? key, required String title, String? subtitle, String? actionLabel,
//                            VoidCallback? onActionClick, String actionTestTag = 'section_action_btn'})
//   class StatusBadge        ({Key? key, required PaymentStatus status, required AppLanguage language})
//   class VisitorStatusBadge ({Key? key, required VisitorStatus status, required AppLanguage language})
//   class TicketStatusBadge  ({Key? key, required TicketStatus status, required AppLanguage language})
//   class ReceiptDialog    ({Key? key, required MaintenanceBill bill, required SocietyInfo? society,
//                            required AppLanguage language, required VoidCallback onDismiss})
//   Future<void> showReceiptDialog(BuildContext context, {required MaintenanceBill bill,
//                            required SocietyInfo? society, required AppLanguage language})
//   class ReceiptRow       ({Key? key, required String label, required String value, bool isBold = false})
//   EdgeInsets dialogInsetPadding(BuildContext context)   // Kotlin DialogWidth: 96% wide, off the safe area
//   class SearchableDropdownField ({Key? key, required String label, required String value,
//                            required List<String> options, required ValueChanged<String> onValueChange,
//                            bool allowFreeText = true, String testTag = ''})
//   class MonthYearPickerField ({Key? key, required String label, required String value,
//                            required ValueChanged<String> onValueChange, String? supportingText,
//                            int yearsBack = 10, int yearsForward = 1, String testTagPrefix = 'month_year'})
//   const List<String> MONTH_NAMES
// ignore_for_file: constant_identifier_names

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../data/models.dart';
import '../../l10n/app_strings.dart';
import '../../util/format.dart';
import '../../util/image_blob.dart';
import '../../util/receipt_pdf.dart';
import '../theme/app_colors.dart';
import 'app_loading.dart';
import 'app_motion.dart';

export 'app_loading.dart';
export 'app_motion.dart';

/// Top bar for every signed-in screen.
///
/// Three tap targets, and nothing else: the society logo opens the Society
/// Profile, the language globe switches language, and the member's photo opens
/// their own profile (which is where Logout now lives).
class SocietyTopAppBar extends StatelessWidget implements PreferredSizeWidget {
  const SocietyTopAppBar({
    super.key,
    required this.societyInfo,
    required this.currentUser,
    required this.currentLanguage,
    required this.onToggleLanguage,
    this.onSelectLanguage,
    this.onOpenSocietyProfile,
    this.onOpenUserProfile,
  });

  final SocietyInfo? societyInfo;
  final Resident? currentUser;
  final AppLanguage currentLanguage;
  final VoidCallback onToggleLanguage;
  final ValueChanged<AppLanguage>? onSelectLanguage;
  final VoidCallback? onOpenSocietyProfile;
  final VoidCallback? onOpenUserProfile;

  @override
  Size get preferredSize => const Size.fromHeight(60);

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final user = currentUser;
    final languageIcon = Icon(
      Icons.language,
      color: Colors.white,
      size: 24,
      semanticLabel: AppStrings.get('switch_language', currentLanguage),
    );

    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.light,
      child: DecoratedBox(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [AppColors.tealDark, AppColors.tealPrimary, Color(0xFF0E8A7B)],
          ),
          boxShadow: [BoxShadow(color: Color(0x3300695C), blurRadius: 12, offset: Offset(0, 3))],
        ),
        child: Material(
          type: MaterialType.transparency,
          child: SafeArea(
            bottom: false,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
              child: Row(
                children: [
                  Expanded(
                    child: InkWell(
                      key: const Key('society_logo_btn'),
                      onTap: onOpenSocietyProfile,
                      borderRadius: BorderRadius.circular(14),
                      child: Row(
                        children: [
                          Container(
                            padding: const EdgeInsets.all(2),
                            decoration: BoxDecoration(
                              borderRadius: BorderRadius.circular(13),
                              color: Colors.white.withValues(alpha: 0.35),
                            ),
                            child: SocietyLogoMark(logoBase64: societyInfo?.logoBase64, size: 40),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              mainAxisSize: MainAxisSize.min,
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  societyInfo?.name ?? AppStrings.get('app_title', currentLanguage),
                                  style: textTheme.titleMedium?.copyWith(
                                    fontWeight: FontWeight.bold,
                                    color: Colors.white,
                                  ),
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                                Text(
                                  user != null
                                      ? '${user.flatNumber} • ${getRoleLabel(user.role, currentLanguage)}'
                                      : AppStrings.get('app_tagline', currentLanguage),
                                  style: textTheme.bodySmall?.copyWith(color: Colors.white.withValues(alpha: 0.85)),
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  // Language switcher - icon only, no label.
                  if (onSelectLanguage != null)
                    PopupMenuButton<AppLanguage>(
                      key: const Key('language_toggle_btn'),
                      icon: languageIcon,
                      tooltip: AppStrings.get('switch_language', currentLanguage),
                      onSelected: onSelectLanguage,
                      itemBuilder: (_) => [
                        for (final lang in AppLanguage.values)
                          PopupMenuItem<AppLanguage>(
                            value: lang,
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Text(
                                  '${lang.displayName} (${lang.nativeName})',
                                  style: TextStyle(
                                    fontWeight: lang == currentLanguage ? FontWeight.bold : FontWeight.normal,
                                  ),
                                ),
                                if (lang == currentLanguage) ...[
                                  const SizedBox(width: 8),
                                  const Icon(Icons.check, color: AppColors.tealPrimary, size: 16),
                                ],
                              ],
                            ),
                          ),
                      ],
                    )
                  else
                    SizedBox(
                      width: 40,
                      height: 40,
                      child: IconButton(
                        key: const Key('language_toggle_btn'),
                        padding: EdgeInsets.zero,
                        onPressed: onToggleLanguage,
                        icon: languageIcon,
                      ),
                    ),
                  if (user != null) ...[
                    const SizedBox(width: 4),
                    Container(
                      padding: const EdgeInsets.all(2),
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        border: Border.all(color: Colors.white.withValues(alpha: 0.7), width: 1.5),
                      ),
                      child: MemberAvatar(
                        key: const Key('user_profile_btn'),
                        photoBase64: user.photoBase64,
                        name: user.ownerName,
                        size: 36,
                        onTap: onOpenUserProfile,
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// The society's uploaded logo, falling back to the SocietySarv mark.
class SocietyLogoMark extends StatelessWidget {
  const SocietyLogoMark({super.key, required this.logoBase64, required this.size});

  final String? logoBase64;
  final double size;

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(size / 4),
      child: Container(
        width: size,
        height: size,
        color: Colors.white,
        alignment: Alignment.center,
        child: BlobImage(
          base64: logoBase64,
          width: size,
          height: size,
          fit: BoxFit.cover,
          // The Android launcher foreground has no Flutter asset; the
          // closest built-in mark stands in for it.
          placeholder: Icon(
            Icons.apartment,
            color: AppColors.tealPrimary,
            size: size * 0.6,
            semanticLabel: 'SocietySarv logo',
          ),
        ),
      ),
    );
  }
}

/// Member photo, or their initial when no photo has been uploaded.
class MemberAvatar extends StatelessWidget {
  const MemberAvatar({super.key, required this.photoBase64, required this.name, required this.size, this.onTap});

  final String? photoBase64;
  final String name;
  final double size;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final trimmed = name.trim();
    final initial = trimmed.isEmpty ? '?' : String.fromCharCode(trimmed.runes.first).toUpperCase();
    final avatar = ClipOval(
      child: Container(
        width: size,
        height: size,
        color: Colors.white.withValues(alpha: 0.25),
        alignment: Alignment.center,
        child: BlobImage(
          base64: photoBase64,
          width: size,
          height: size,
          fit: BoxFit.cover,
          placeholder: Text(
            initial,
            style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold, color: Colors.white),
          ),
        ),
      ),
    );
    if (onTap == null) return avatar;
    return GestureDetector(onTap: onTap, child: avatar);
  }
}

String getRoleLabel(UserRole role, AppLanguage language) {
  switch (role) {
    case UserRole.CHAIRMAN:
      return AppStrings.get('role_chairman', language);
    case UserRole.SECRETARY:
      return AppStrings.get('role_secretary', language);
    case UserRole.TREASURER:
      return AppStrings.get('role_treasurer', language);
    case UserRole.COMMITTEE:
      return AppStrings.get('role_committee', language);
    case UserRole.RESIDENT:
      return AppStrings.get('role_resident', language);
    case UserRole.TENANT:
      return AppStrings.get('role_tenant', language);
    case UserRole.GUARD:
      return AppStrings.get('role_guard', language);
    case UserRole.HOUSEKEEPING:
      return AppStrings.get('role_housekeeping', language);
    case UserRole.MAINTENANCE_STAFF:
      return AppStrings.get('role_maintenance_staff', language);
    case UserRole.ACCOUNTANT:
      return AppStrings.get('role_accountant', language);
    case UserRole.MANAGER:
      return AppStrings.get('role_manager', language);
  }
}

class FinancialKpiCard extends StatelessWidget {
  const FinancialKpiCard({
    super.key,
    required this.title,
    required this.amount,
    this.subtitle,
    required this.icon,
    this.accentColor = AppColors.tealPrimary,
  });

  final String title;
  final double amount;
  final String? subtitle;
  final IconData icon;
  final Color accentColor;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final textTheme = theme.textTheme;
    return Container(
      clipBehavior: Clip.antiAlias,
      decoration: BoxDecoration(
        color: theme.colorScheme.surface,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.borderLight),
        boxShadow: const [BoxShadow(color: Color(0x0F0F172A), blurRadius: 16, offset: Offset(0, 6))],
      ),
      child: Stack(
        children: [
          // A soft tinted bubble in the corner gives each KPI its own colour.
          Positioned(
            right: -22,
            top: -22,
            child: Container(
              width: 76,
              height: 76,
              decoration: BoxDecoration(shape: BoxShape.circle, color: accentColor.withValues(alpha: 0.07)),
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  width: 38,
                  height: 38,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(12),
                    gradient: LinearGradient(
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                      colors: [accentColor.withValues(alpha: 0.18), accentColor.withValues(alpha: 0.08)],
                    ),
                  ),
                  alignment: Alignment.center,
                  child: Icon(icon, color: accentColor, size: 20),
                ),
                const SizedBox(height: 10),
                Text(
                  title,
                  style: textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                AnimatedAmount(
                  amount: amount,
                  format: rupees,
                  style: textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w800,
                    color: theme.colorScheme.onSurface,
                  ),
                  maxLines: 1,
                ),
                if (subtitle != null)
                  Text(
                    subtitle!,
                    style: textTheme.labelSmall?.copyWith(color: accentColor, fontWeight: FontWeight.w600),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class SectionHeader extends StatelessWidget {
  const SectionHeader({
    super.key,
    required this.title,
    this.subtitle,
    this.actionLabel,
    this.onActionClick,
    this.actionTestTag = 'section_action_btn',
  });

  final String title;
  final String? subtitle;
  final String? actionLabel;
  final VoidCallback? onActionClick;
  final String actionTestTag;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final textTheme = theme.textTheme;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                    color: theme.colorScheme.onSurface,
                  ),
                ),
                if (subtitle != null)
                  Text(subtitle!, style: textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
              ],
            ),
          ),
          if (actionLabel != null && onActionClick != null)
            TextButton(
              key: Key(actionTestTag),
              onPressed: onActionClick,
              child: Text(
                actionLabel!,
                style: textTheme.labelMedium?.copyWith(fontWeight: FontWeight.bold, color: AppColors.tealPrimary),
              ),
            ),
        ],
      ),
    );
  }
}

/// The rounded pill every status badge uses.
class _Badge extends StatelessWidget {
  const _Badge({required this.label, required this.background, required this.foreground});

  final String label;
  final Color background;
  final Color foreground;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: foreground.withValues(alpha: 0.18)),
      ),
      child: Text(
        label,
        style: Theme.of(context).textTheme.labelSmall?.copyWith(fontWeight: FontWeight.bold, color: foreground),
      ),
    );
  }
}

class StatusBadge extends StatelessWidget {
  const StatusBadge({super.key, required this.status, required this.language});

  final PaymentStatus status;
  final AppLanguage language;

  @override
  Widget build(BuildContext context) {
    final (bg, fg, key) = switch (status) {
      PaymentStatus.PAID => (AppColors.statusPaidBg, AppColors.statusPaid, 'status_paid'),
      PaymentStatus.UNPAID => (AppColors.statusOverdueBg, AppColors.statusOverdue, 'status_unpaid'),
      PaymentStatus.PARTIAL => (AppColors.statusPendingBg, AppColors.statusPending, 'status_partial'),
    };
    return _Badge(label: AppStrings.get(key, language), background: bg, foreground: fg);
  }
}

class VisitorStatusBadge extends StatelessWidget {
  const VisitorStatusBadge({super.key, required this.status, required this.language});

  final VisitorStatus status;
  final AppLanguage language;

  @override
  Widget build(BuildContext context) {
    final (bg, fg, key) = switch (status) {
      VisitorStatus.INSIDE => (AppColors.statusPaidBg, AppColors.statusPaid, 'visitor_inside'),
      VisitorStatus.EXITED => (
        AppColors.surfaceVariant,
        Theme.of(context).colorScheme.onSurfaceVariant,
        'visitor_exited',
      ),
      VisitorStatus.PRE_APPROVED => (AppColors.statusPendingBg, AppColors.statusPending, 'generate_passcode'),
      // A turned-away visitor is a gate outcome, not a payment one. This
      // used to borrow the billing string and read as "Unpaid".
      VisitorStatus.REJECTED => (AppColors.statusOverdueBg, AppColors.statusOverdue, 'visitor_rejected'),
    };
    return _Badge(label: AppStrings.get(key, language), background: bg, foreground: fg);
  }
}

class TicketStatusBadge extends StatelessWidget {
  const TicketStatusBadge({super.key, required this.status, required this.language});

  final TicketStatus status;
  final AppLanguage language;

  @override
  Widget build(BuildContext context) {
    final (bg, fg, key) = switch (status) {
      TicketStatus.OPEN => (AppColors.statusOverdueBg, AppColors.statusOverdue, 'status_open'),
      TicketStatus.IN_PROGRESS => (AppColors.statusPendingBg, AppColors.statusPending, 'status_in_progress'),
      TicketStatus.RESOLVED => (AppColors.statusPaidBg, AppColors.statusPaid, 'status_resolved'),
    };
    return _Badge(label: AppStrings.get(key, language), background: bg, foreground: fg);
  }
}

/// How wide a dialog sits on the screen, and how far it keeps off the edges
/// (Kotlin `DialogWidth` + `WideDialogProperties`): 96% of the screen width,
/// with 12dp breathing room above and below. Flutter's dialog already keeps
/// clear of the system bars and the keyboard.
EdgeInsets dialogInsetPadding(BuildContext context) {
  final width = MediaQuery.sizeOf(context).width;
  return EdgeInsets.symmetric(horizontal: width * 0.02, vertical: 12);
}

/// Opens [ReceiptDialog] and completes when it is closed.
Future<void> showReceiptDialog(
  BuildContext context, {
  required MaintenanceBill bill,
  required SocietyInfo? society,
  required AppLanguage language,
}) {
  return showAppDialog<void>(
    context: context,
    builder: (dialogContext) => ReceiptDialog(
      bill: bill,
      society: society,
      language: language,
      onDismiss: () => Navigator.of(dialogContext).pop(),
    ),
  );
}

/// On-screen preview of the payment receipt, with the two things a committee
/// actually needs from it: a proper PDF saved to the device, and a share sheet
/// to send it on to the member.
class ReceiptDialog extends StatefulWidget {
  const ReceiptDialog({
    super.key,
    required this.bill,
    required this.society,
    required this.language,
    required this.onDismiss,
  });

  final MaintenanceBill bill;
  final SocietyInfo? society;
  final AppLanguage language;
  final VoidCallback onDismiss;

  @override
  State<ReceiptDialog> createState() => _ReceiptDialogState();
}

class _ReceiptDialogState extends State<ReceiptDialog> {
  String? _status;
  bool _isError = false;

  void _report(String? message, bool isError) {
    if (!mounted) return;
    setState(() {
      _status = message;
      _isError = isError;
    });
  }

  @override
  Widget build(BuildContext context) {
    final bill = widget.bill;
    final society = widget.society;
    final language = widget.language;
    final theme = Theme.of(context);
    final textTheme = theme.textTheme;
    final reg = society?.registrationNumber ?? '';

    final buttonPadding = const EdgeInsets.symmetric(horizontal: 10, vertical: 8);
    final buttonShape = RoundedRectangleBorder(borderRadius: BorderRadius.circular(10));

    Widget buttonLabel(IconData icon, String text, Color color) => Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 18, color: color),
        const SizedBox(width: 6),
        Flexible(
          child: Text(
            text,
            style: textTheme.labelMedium?.copyWith(color: color),
            overflow: TextOverflow.ellipsis,
          ),
        ),
      ],
    );

    return AlertDialog(
      insetPadding: dialogInsetPadding(context),
      scrollable: true,
      title: Row(
        children: [
          Expanded(
            child: Text(
              '${AppStrings.get('receipt_generated', language)} #${bill.receiptNumber}',
              style: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
            ),
          ),
          const Icon(Icons.check_circle, color: AppColors.statusPaid),
        ],
      ),
      content: SizedBox(
        width: double.maxFinite,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Card(
              margin: EdgeInsets.zero,
              color: AppColors.surfaceVariant,
              elevation: 1,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  spacing: 6,
                  children: [
                    Row(
                      children: [
                        SocietyLogoMark(logoBase64: society?.logoBase64, size: 36),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                society?.name ?? '',
                                style: textTheme.titleSmall?.copyWith(
                                  fontWeight: FontWeight.bold,
                                  color: AppColors.tealPrimary,
                                ),
                              ),
                              Text('Reg No: ${reg.trim().isEmpty ? '-' : reg}', style: textTheme.bodySmall),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const Padding(padding: EdgeInsets.symmetric(vertical: 4), child: Divider(height: 1)),
                    ReceiptRow(label: 'Unit / Member', value: '${bill.flatNumber} - ${bill.residentName}'),
                    ReceiptRow(label: 'Month', value: bill.billMonth),
                    ReceiptRow(label: 'Bill Amount', value: rupees(bill.chargesSubtotal)),
                    ReceiptRow(label: 'Late Fee', value: rupees(bill.lateFeeAmount)),
                    ReceiptRow(label: 'Total Payable', value: rupees(bill.payableTotal)),
                    ReceiptRow(label: 'Amount Paid', value: rupees(bill.amountPaid), isBold: true),
                    ReceiptRow(label: 'Balance Outstanding', value: rupees(bill.pendingAmount)),
                    ReceiptRow(label: 'Payment Mode', value: bill.paymentMode?.labelEn ?? '-'),
                    ReceiptRow(
                      label: 'Reference No.',
                      value: bill.transactionRef.trim().isEmpty ? '-' : bill.transactionRef,
                    ),
                    ReceiptRow(label: 'Paid On', value: bill.paidDate.trim().isEmpty ? '-' : bill.paidDate),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 12),
            // View, download, share. Download writes where the member picks
            // (Downloads by default) rather than the app's cache, and View
            // opens the PDF in a reader.
            Row(
              spacing: 8,
              children: [
                Expanded(
                  child: OutlinedButton(
                    key: const Key('receipt_view_btn'),
                    style: OutlinedButton.styleFrom(
                      shape: buttonShape,
                      side: const BorderSide(color: AppColors.tealPrimary),
                      padding: buttonPadding,
                    ),
                    onPressed: () async {
                      final failure = await ReceiptPdf.view(bill, society);
                      _report(failure, failure != null);
                    },
                    child: buttonLabel(
                      Icons.visibility,
                      AppStrings.get('view_receipt', language),
                      AppColors.tealPrimary,
                    ),
                  ),
                ),
                Expanded(
                  child: OutlinedButton(
                    key: const Key('receipt_pdf_btn'),
                    style: OutlinedButton.styleFrom(
                      shape: buttonShape,
                      side: const BorderSide(color: AppColors.tealPrimary),
                      padding: buttonPadding,
                    ),
                    onPressed: () async {
                      final saved = await ReceiptPdf.saveToDownloads(bill, society);
                      _report(saved.message, !saved.isSaved);
                    },
                    child: buttonLabel(
                      Icons.picture_as_pdf,
                      AppStrings.get('download_receipt', language),
                      AppColors.tealPrimary,
                    ),
                  ),
                ),
                Expanded(
                  child: AppButton.filled(
                    key: const Key('receipt_share_btn'),
                    style: FilledButton.styleFrom(
                      backgroundColor: AppColors.tealPrimary,
                      shape: buttonShape,
                      padding: buttonPadding,
                    ),
                    onPressed: () async {
                      final failure = await ReceiptPdf.share(bill, society);
                      _report(failure, failure != null);
                    },
                    child: buttonLabel(Icons.share, AppStrings.get('share_receipt', language), Colors.white),
                  ),
                ),
              ],
            ),
            if (_status != null) ...[
              const SizedBox(height: 8),
              Text(
                _status!,
                style: textTheme.bodySmall?.copyWith(color: _isError ? theme.colorScheme.error : AppColors.statusPaid),
              ),
            ],
          ],
        ),
      ),
      actions: [
        AppButton.filled(
          style: FilledButton.styleFrom(backgroundColor: AppColors.tealPrimary),
          onPressed: widget.onDismiss,
          child: Text(AppStrings.get('close', language)),
        ),
      ],
    );
  }
}

class ReceiptRow extends StatelessWidget {
  const ReceiptRow({super.key, required this.label, required this.value, this.isBold = false});

  final String label;
  final String value;
  final bool isBold;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final bodySmall = theme.textTheme.bodySmall;
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
        const SizedBox(width: 8),
        Flexible(
          child: Text(
            value,
            textAlign: TextAlign.end,
            style: isBold
                ? bodySmall?.copyWith(fontWeight: FontWeight.bold, color: AppColors.statusPaid)
                : bodySmall?.copyWith(fontWeight: FontWeight.normal),
          ),
        ),
      ],
    );
  }
}

/// Far more rows than any real unit, month or city list will ever hold.
const int _maxDropdownOptions = 1000;

/// Dropdown that still lets the user type a value that is not on the list -
/// there is no list of Indian cities that is ever complete enough to block a
/// registration on.
class SearchableDropdownField extends StatefulWidget {
  const SearchableDropdownField({
    super.key,
    required this.label,
    required this.value,
    required this.options,
    required this.onValueChange,
    this.allowFreeText = true,
    this.testTag = '',
  });

  final String label;
  final String value;
  final List<String> options;
  final ValueChanged<String> onValueChange;
  final bool allowFreeText;
  final String testTag;

  @override
  State<SearchableDropdownField> createState() => _SearchableDropdownFieldState();
}

class _SearchableDropdownFieldState extends State<SearchableDropdownField> {
  late final TextEditingController _controller = TextEditingController(text: widget.value);
  bool _syncing = false;

  @override
  void initState() {
    super.initState();
    _controller.addListener(_onTextChanged);
  }

  @override
  void didUpdateWidget(covariant SearchableDropdownField oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (_controller.text != widget.value) {
      _syncing = true;
      _controller.value = TextEditingValue(
        text: widget.value,
        selection: TextSelection.collapsed(offset: widget.value.length),
      );
      _syncing = false;
    }
  }

  @override
  void dispose() {
    _controller.removeListener(_onTextChanged);
    _controller.dispose();
    super.dispose();
  }

  void _onTextChanged() {
    if (_syncing || !widget.allowFreeText) return;
    if (_controller.text != widget.value) widget.onValueChange(_controller.text);
  }

  /*
   * What the list is narrowed to.
   *
   * The typed value is a search term only while the field is a free-text one
   * *and* what is in it is not already one of the options. A pick-only field
   * (unit number, billing month, unit type) treats its value as a selection,
   * so choosing "August 2026" or "2 BHK" must never leave that the only row
   * the list will show afterwards.
   */
  List<DropdownMenuEntry<String>> _filter(List<DropdownMenuEntry<String>> entries, String text) {
    final typed = text.trim();
    final lower = typed.toLowerCase();
    final isSelection = entries.any((e) => e.value.toLowerCase() == lower);
    if (!widget.allowFreeText || typed.isEmpty || isSelection) return entries;
    return entries.where((e) => e.value.toLowerCase().contains(lower)).toList();
  }

  @override
  Widget build(BuildContext context) {
    final entries = [
      // Every option is listed - a society with 200 units has to be able to
      // reach its last unit - and the menu scrolls when it has to.
      for (final option in widget.options.take(_maxDropdownOptions))
        DropdownMenuEntry<String>(value: option, label: option),
    ];
    return DropdownMenu<String>(
      key: widget.testTag.isEmpty ? null : Key(widget.testTag),
      controller: _controller,
      label: Text(widget.label),
      dropdownMenuEntries: entries,
      expandedInsets: EdgeInsets.zero,
      menuHeight: 320,
      enableFilter: true,
      enableSearch: false,
      filterCallback: _filter,
      requestFocusOnTap: widget.allowFreeText,
      helperText: widget.allowFreeText && widget.options.isEmpty
          ? 'Type it in - no list available for this selection.'
          : null,
      onSelected: (picked) {
        if (picked != null) widget.onValueChange(picked);
      },
    );
  }
}

/// Picks a month and a year and hands back one "April 2026" string - the format
/// every bill month in the app is written in.
///
/// Used for the society's billing start month, which is what every billing month
/// list in the app is generated from.
class MonthYearPickerField extends StatelessWidget {
  const MonthYearPickerField({
    super.key,
    required this.label,
    required this.value,
    required this.onValueChange,
    this.supportingText,
    this.yearsBack = 10,
    this.yearsForward = 1,
    this.testTagPrefix = 'month_year',
  });

  final String label;
  final String value;
  final ValueChanged<String> onValueChange;
  final String? supportingText;
  final int yearsBack;
  final int yearsForward;
  final String testTagPrefix;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final currentYear = DateTime.now().year;
    final parts = value.trim().split(' ').where((p) => p.trim().isNotEmpty).toList();
    final month = parts.isNotEmpty ? parts[0] : '';
    final year = parts.length > 1 ? parts[1] : '';
    final years = [for (var y = currentYear + yearsForward; y >= currentYear - yearsBack; y--) y.toString()];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          label,
          style: theme.textTheme.labelMedium?.copyWith(
            fontWeight: FontWeight.bold,
            color: theme.colorScheme.onSurfaceVariant,
          ),
        ),
        const SizedBox(height: 4),
        Row(
          spacing: 8,
          children: [
            Expanded(
              flex: 13,
              child: SearchableDropdownField(
                label: 'Month',
                value: month,
                options: MONTH_NAMES,
                onValueChange: (picked) =>
                    onValueChange('$picked ${year.trim().isEmpty ? currentYear.toString() : year}'),
                allowFreeText: false,
                testTag: '${testTagPrefix}_month_dropdown',
              ),
            ),
            Expanded(
              flex: 10,
              child: SearchableDropdownField(
                label: 'Year',
                value: year,
                options: years,
                onValueChange: (picked) => onValueChange('${month.trim().isEmpty ? MONTH_NAMES.first : month} $picked'),
                allowFreeText: false,
                testTag: '${testTagPrefix}_year_dropdown',
              ),
            ),
          ],
        ),
        if (supportingText != null)
          Padding(
            padding: const EdgeInsets.only(top: 2),
            child: Text(
              supportingText!,
              style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
          ),
      ],
    );
  }
}

/// English month names, the same spelling every bill month is stored with.
const List<String> MONTH_NAMES = [
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
];
