// Port of ui/components/UpiQrPaymentDialog.kt.
//
// Public API:
//   class UpiQrPaymentDialog extends StatelessWidget
//     UpiQrPaymentDialog({Key? key, required MaintenanceBill bill, required SocietyInfo? society,
//       required AppLanguage language, required VoidCallback onDismiss,
//       required VoidCallback onPaymentRecorded, UnitDues? dues})
//   Future<void> showUpiQrPaymentDialog(BuildContext context, {required MaintenanceBill bill,
//       required SocietyInfo? society, required AppLanguage language,
//       required VoidCallback onPaymentRecorded, UnitDues? dues})
//     // closes the dialog first, then calls onPaymentRecorded when "I have Paid" is tapped
//
// What a member is shown when they tap Pay by QR.
//
// The amount asked for is the unit's whole position - this month, the arrears
// before it, less any credit already paid in - rather than the one bill that
// was tapped, because that is the figure a member actually settles.
//
// The QR itself is either the one the chairman uploaded or one generated from
// the society's UPI id, in that order: a society whose bank issues its own QR
// needs that exact image, and only a society without one falls back to the
// generated code.

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:share_plus/share_plus.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../data/models.dart';
import '../../data/payment_allocation.dart';
import '../../l10n/app_strings.dart';
import '../../util/format.dart';
import '../../util/image_blob.dart';
import '../../util/upi_qr.dart';
import '../theme/app_colors.dart';
import 'common_components.dart' show dialogInsetPadding;

/// Opens [UpiQrPaymentDialog]. Close dismisses it; "I have Paid" dismisses it
/// and then calls [onPaymentRecorded].
Future<void> showUpiQrPaymentDialog(
  BuildContext context, {
  required MaintenanceBill bill,
  required SocietyInfo? society,
  required AppLanguage language,
  required VoidCallback onPaymentRecorded,
  UnitDues? dues,
}) {
  return showDialog<void>(
    context: context,
    builder: (dialogContext) => UpiQrPaymentDialog(
      bill: bill,
      society: society,
      language: language,
      dues: dues,
      onDismiss: () => Navigator.of(dialogContext).pop(),
      onPaymentRecorded: () {
        Navigator.of(dialogContext).pop();
        onPaymentRecorded();
      },
    ),
  );
}

class UpiQrPaymentDialog extends StatelessWidget {
  const UpiQrPaymentDialog({
    super.key,
    required this.bill,
    required this.society,
    required this.language,
    required this.onDismiss,
    required this.onPaymentRecorded,
    this.dues,
  });

  final MaintenanceBill bill;
  final SocietyInfo? society;
  final AppLanguage language;
  final VoidCallback onDismiss;
  final VoidCallback onPaymentRecorded;

  /// The unit's arrears, current month and advance credit. Left null, the
  /// dialog falls back to what this one bill still owes.
  final UnitDues? dues;

  bool get _mr => language == AppLanguage.marathi;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final textTheme = theme.textTheme;

    final thisBillPending = bill.pendingAmount;
    final dueAmount = dues?.netPayable ?? thisBillPending;
    final societyName = society?.name ?? 'Housing Society';
    // The society's real UPI id. Nothing is invented here - a QR built from a
    // made-up address takes the member's money nowhere.
    final societyVpa = (society?.upiId ?? '').trim();
    final uploadedQr = blobImageProvider(society?.paymentQrBase64);
    final transactionNote = 'Maintenance ${bill.flatNumber} ${bill.billMonth}';

    final upiUri = UpiQr.paymentUri(societyVpa, societyName, dueAmount, transactionNote);
    final hasQr = uploadedQr != null || upiUri != null;

    final Widget qrSection;
    if (!hasQr) {
      // No UPI id and no uploaded image means no QR. Showing one built from a
      // made-up address would send the money nowhere.
      qrSection = Container(
        key: const Key('upi_not_configured'),
        width: double.infinity,
        margin: const EdgeInsets.symmetric(vertical: 8),
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppColors.statusOverdueBg,
          borderRadius: BorderRadius.circular(16),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              _mr ? 'संस्थेचा UPI आयडी अजून सेट केलेला नाही' : 'No UPI ID set for the society yet',
              style: textTheme.titleSmall?.copyWith(
                fontWeight: FontWeight.bold,
                color: AppColors.statusOverdue,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              _mr
                  ? 'अध्यक्षांना Society Profile मध्ये UPI आयडी भरायला सांगा. तोपर्यंत बँक तपशिलाने भरा.'
                  : "Ask the Chairman to add the society's UPI ID, or upload the "
                      "society's own payment QR, under Society Profile. Until "
                      'then, pay using the bank details.',
              style: textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
          ],
        ),
      );
    } else {
      // QR Code Frame Container
      qrSection = Container(
        margin: const EdgeInsets.symmetric(vertical: 8),
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: AppColors.tealPrimary, width: 2),
          boxShadow: const [BoxShadow(color: Colors.black26, blurRadius: 4, offset: Offset(0, 2))],
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (uploadedQr != null)
              Image(
                key: const Key('uploaded_upi_qr'),
                image: uploadedQr,
                width: 180,
                height: 180,
                fit: BoxFit.contain,
                gaplessPlayback: true,
                semanticLabel: 'UPI QR Code',
              )
            else
              UpiQrImage(key: const Key('generated_upi_qr'), data: upiUri!, size: 180),
            const SizedBox(height: 6),
            Text(
              'Scan with any UPI App (GPay, PhonePe, Paytm, BHIM)',
              style: textTheme.labelSmall?.copyWith(color: const Color(0xFF444444)),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      );
    }

    final currentDues = dues;

    return AlertDialog(
      insetPadding: dialogInsetPadding(context),
      scrollable: true,
      title: Row(
        children: [
          Container(
            width: 36,
            height: 36,
            decoration: const BoxDecoration(color: AppColors.tealContainer, shape: BoxShape.circle),
            alignment: Alignment.center,
            child: const Icon(Icons.qr_code_2, color: AppColors.tealPrimary, size: 22),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  _mr
                      ? 'UPI QR कोड पेमेंट'
                      : language == AppLanguage.hindi
                          ? 'यूपीआई क्यूआर भुगतान'
                          : 'Instant UPI QR Payment',
                  style: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                ),
                Text(
                  'Unit ${bill.flatNumber} • ${bill.billMonth}',
                  style: textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
              ],
            ),
          ),
        ],
      ),
      content: SizedBox(
        width: double.maxFinite,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            qrSection,
            const SizedBox(height: 10),

            // Payment Info Details Card
            Card(
              margin: EdgeInsets.zero,
              color: AppColors.surfaceVariant,
              elevation: 1,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
                side: const BorderSide(color: AppColors.borderLight),
              ),
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  spacing: 6,
                  children: [
                    // The whole position, not just the bill that was tapped:
                    // what this month came to, what was owed before it, and
                    // the credit the member is already in. A member paying off
                    // arrears needs to see all three or the amount on the QR
                    // looks wrong to them.
                    _AmountRow(
                      label: _mr ? '${bill.billMonth} चे बिल' : '${bill.billMonth} bill',
                      amount: thisBillPending,
                    ),
                    if (currentDues != null && currentDues.previousPending > 0.009)
                      _AmountRow(
                        label: _mr ? 'मागील थकबाकी' : 'Previous pending',
                        amount: currentDues.previousPending,
                      ),
                    if (currentDues != null && currentDues.excessCredit > 0.009)
                      _AmountRow(
                        label: _mr ? 'जमा (जास्तीची) रक्कम' : 'Advance credit',
                        amount: -currentDues.excessCredit,
                        color: AppColors.statusPaid,
                      ),
                    const Padding(
                      padding: EdgeInsets.symmetric(vertical: 2),
                      child: Divider(height: 1),
                    ),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(
                          _mr ? 'एकूण भरणा रक्कम' : 'Total Payable',
                          style: textTheme.bodySmall?.copyWith(fontWeight: FontWeight.bold),
                        ),
                        Text(
                          rupees(dueAmount),
                          key: const Key('upi_total_payable'),
                          style: textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.bold,
                            color: AppColors.statusOverdue,
                          ),
                        ),
                      ],
                    ),
                    if (societyVpa.isNotEmpty) ...[
                      const Padding(
                        padding: EdgeInsets.symmetric(vertical: 2),
                        child: Divider(height: 1),
                      ),
                      Row(
                        children: [
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  _mr ? 'संस्था UPI आयडी' : 'Society UPI VPA',
                                  style: textTheme.labelSmall?.copyWith(
                                    color: theme.colorScheme.onSurfaceVariant,
                                  ),
                                ),
                                Text(
                                  societyVpa,
                                  style: textTheme.bodySmall?.copyWith(
                                    fontWeight: FontWeight.bold,
                                    color: AppColors.tealPrimary,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          SizedBox(
                            width: 32,
                            height: 32,
                            child: IconButton(
                              padding: EdgeInsets.zero,
                              tooltip: 'Copy UPI VPA',
                              onPressed: () => Clipboard.setData(ClipboardData(text: societyVpa)),
                              icon: const Icon(Icons.content_copy, color: AppColors.tealPrimary, size: 16),
                            ),
                          ),
                        ],
                      ),
                    ],
                  ],
                ),
              ),
            ),
            const SizedBox(height: 12),

            // Direct App Open Button & Share
            Row(
              spacing: 8,
              children: [
                // Only offered when there is a real address behind it. A
                // society that only uploaded an image has nothing for a UPI
                // app to open, and a dead button is worse than none.
                Expanded(
                  child: OutlinedButton(
                    key: const Key('open_upi_app_btn'),
                    style: OutlinedButton.styleFrom(
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                    ),
                    onPressed: upiUri == null
                        ? null
                        : () async {
                            try {
                              await launchUrl(Uri.parse(upiUri), mode: LaunchMode.externalApplication);
                            } catch (_) {
                              // No UPI app installed - nothing to open.
                            }
                          },
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(Icons.open_in_new, size: 16),
                        const SizedBox(width: 4),
                        Flexible(
                          child: Text(
                            _mr ? 'UPI ॲपने भरा' : 'Pay in App',
                            style: textTheme.labelSmall,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                // Given the same weight as Pay in App. Squeezed into a 48dp
                // square it had no room for its own icon.
                Expanded(
                  child: OutlinedButton(
                    key: const Key('share_upi_btn'),
                    style: OutlinedButton.styleFrom(
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                      padding: const EdgeInsets.all(8),
                    ),
                    onPressed: () async {
                      final text = StringBuffer()
                        ..write('Payment Request for ${bill.residentName} (${bill.flatNumber})\n')
                        ..write('Amount: ${rupees(dueAmount)}\n');
                      if (societyVpa.isNotEmpty) text.write('UPI ID: $societyVpa\n');
                      text.write('Note: $transactionNote');
                      if (upiUri != null) text.write('\nPay Link: $upiUri');
                      try {
                        await SharePlus.instance.share(
                          ShareParams(text: text.toString(), title: 'Share Payment QR Details'),
                        );
                      } catch (_) {
                        // Nothing to share with.
                      }
                    },
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(Icons.share, size: 16),
                        const SizedBox(width: 4),
                        Flexible(
                          child: Text(
                            _mr ? 'शेअर करा' : 'Share',
                            style: textTheme.labelSmall,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: onDismiss,
          child: Text(AppStrings.get('close', language)),
        ),
        FilledButton(
          key: const Key('record_after_upi_btn'),
          style: FilledButton.styleFrom(
            backgroundColor: AppColors.tealPrimary,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
          ),
          onPressed: onPaymentRecorded,
          child: Text(
            _mr ? 'पावती नोंदवा' : 'I have Paid',
            style: textTheme.labelSmall?.copyWith(color: Colors.white),
          ),
        ),
      ],
    );
  }
}

/// One line of the payable breakdown: a label on the left, a rupee figure right.
class _AmountRow extends StatelessWidget {
  const _AmountRow({required this.label, required this.amount, this.color});

  final String label;
  final double amount;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Flexible(child: Text(label, style: theme.textTheme.bodySmall)),
        Text(
          rupees(amount),
          style: theme.textTheme.bodySmall?.copyWith(
            fontWeight: FontWeight.w600,
            color: color ?? theme.colorScheme.onSurface,
          ),
        ),
      ],
    );
  }
}
