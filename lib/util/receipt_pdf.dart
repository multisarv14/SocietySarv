// Port of util/ReceiptPdf.kt (+ the AmountWords object in it).
//
// Public API:
//   Future<Uint8List> buildReceiptPdf(MaintenanceBill bill, SocietyInfo? society)
//   class ReceiptPdf
//     static Future<Uint8List> build(MaintenanceBill bill, SocietyInfo? society)
//     static String fileName(MaintenanceBill bill)                 // SocietySarv-Receipt-<no>.pdf
//     static Future<File?> generate(MaintenanceBill bill, SocietyInfo? society)
//     static Future<String?> share(MaintenanceBill bill, SocietyInfo? society)   // null = ok, else message
//     static Future<ReceiptSaveResult> saveToDownloads(MaintenanceBill bill, SocietyInfo? society)
//     static Future<String?> view(MaintenanceBill bill, SocietyInfo? society)    // print/preview sheet
//   class ReceiptSaveResult { bool isSaved; String message; Uri? uri; }
//   class AmountWords
//     static String rupees(double amount)   // "Rupees One Thousand Two Hundred Only"
//
// Renders a maintenance payment receipt as a real PDF, laid out the way a
// housing society's receipt book is: society letterhead with its logo, the
// receipt number and date, a particulars table that adds up to the bill total,
// what was actually paid against it, and what is still outstanding.
//
// The built-in PDF fonts have no rupee glyph and no Devanagari, so amounts are
// headed "Rs." and the PDF text stays in English, as the Kotlin PDFs did.

import 'dart:io';
import 'dart:typed_data';

import 'package:file_picker/file_picker.dart';
import 'package:pdf/pdf.dart';
import 'package:printing/printing.dart';

import '../data/models.dart';
import 'format.dart';
import 'image_blob.dart';
import 'pdf_canvas.dart';

Future<Uint8List> buildReceiptPdf(MaintenanceBill bill, SocietyInfo? society) =>
    ReceiptPdf.build(bill, society);

/// Where a receipt ended up, and what to tell the member about it.
class ReceiptSaveResult {
  const ReceiptSaveResult(this.isSaved, this.message, [this.uri]);
  final bool isSaved;
  final String message;
  final Uri? uri;
}

class ReceiptPdf {
  ReceiptPdf._();

  static const double _margin = 40;
  static const double _pageWidth = PdfCanvas.pageWidth;
  static const double _pageHeight = PdfCanvas.pageHeight;

  static const PdfColor _teal = PdfColor.fromInt(0xFF027C75);
  static const PdfColor _ink = PdfColor.fromInt(0xFF111827);
  static const PdfColor _muted = PdfColor.fromInt(0xFF6B7280);
  static const PdfColor _rule = PdfColor.fromInt(0xFFD1D5DB);
  static const PdfColor _tableFill = PdfColor.fromInt(0xFFF0F7F6);
  static const PdfColor _red = PdfColor.fromInt(0xFFB91C1C);
  static const PdfColor _green = PdfColor.fromInt(0xFF15803D);

  static String fileName(MaintenanceBill bill) {
    final number = bill.receiptNumber.trim().isEmpty ? 'RECEIPT' : bill.receiptNumber;
    final safeNumber = number.replaceAll(RegExp(r'[^A-Za-z0-9-]'), '-');
    return 'SocietySarv-Receipt-$safeNumber.pdf';
  }

  /// Writes the receipt and returns the file, or null if it could not be written.
  static Future<File?> generate(MaintenanceBill bill, SocietyInfo? society) async {
    try {
      final bytes = await build(bill, society);
      return await writeReportPdf(bytes, fileName(bill));
    } catch (_) {
      return null;
    }
  }

  /// Generates the receipt and opens the system share sheet for it.
  static Future<String?> share(MaintenanceBill bill, SocietyInfo? society) async {
    final file = await generate(bill, society);
    if (file == null) return 'Could not create the receipt PDF.';
    final failure = await sharePdfFile(
      file,
      subject: '${society?.name ?? ''} - Payment Receipt ${bill.receiptNumber}'.trim(),
      text: 'Receipt ${bill.receiptNumber} for ${bill.flatNumber} (${bill.billMonth}).',
    );
    return failure == null ? null : 'No app available to share the receipt.';
  }

  /// Saves the receipt where the member chooses (the system "save as" sheet,
  /// which defaults to Downloads) and says where it went, or why it could not.
  static Future<ReceiptSaveResult> saveToDownloads(MaintenanceBill bill, SocietyInfo? society) async {
    final Uint8List bytes;
    try {
      bytes = await build(bill, society);
    } catch (_) {
      return const ReceiptSaveResult(false, 'Could not create the receipt PDF.');
    }
    final name = fileName(bill);
    try {
      final uri = await FilePicker.saveFile(
        fileName: name,
        bytes: bytes,
        mimeType: 'application/pdf',
        type: FileType.custom,
        allowedExtensions: const ['pdf'],
      );
      if (uri == null) return const ReceiptSaveResult(false, 'Receipt not saved.');
      return ReceiptSaveResult(true, 'Saved to Downloads as $name', uri);
    } catch (_) {
      return const ReceiptSaveResult(false, 'Could not save to Downloads.');
    }
  }

  /// Opens the receipt in the system PDF preview (from where it can be printed
  /// or saved). Returns null when it opened, or a message otherwise.
  static Future<String?> view(MaintenanceBill bill, SocietyInfo? society) async {
    final Uint8List bytes;
    try {
      bytes = await build(bill, society);
    } catch (_) {
      return 'Could not create the receipt PDF.';
    }
    try {
      await Printing.layoutPdf(
        onLayout: (_) async => bytes,
        name: fileName(bill),
        format: PdfPageFormat.a4,
      );
      return null;
    } catch (_) {
      return 'No app on this phone can open a PDF. Use Share instead.';
    }
  }

  // ------------------------------------------------------------- rendering

  static Future<Uint8List> build(MaintenanceBill bill, SocietyInfo? society) async {
    final document = PdfDocument();
    final page = PdfPage(document, pageFormat: PdfPageFormat.a4);
    final c = PdfCanvas(document, page);
    _drawReceipt(document, c, bill, society);
    return document.save();
  }

  static void _drawReceipt(PdfDocument document, PdfCanvas c, MaintenanceBill bill, SocietyInfo? society) {
    void title(String s, double x, double y) => c.text(s, x, y, size: 16, color: _ink, bold: true);
    void heading(String s, double x, double y) => c.text(s, x, y, size: 13, color: _teal, bold: true);
    void body(String s, double x, double y, {PdfAlign align = PdfAlign.left}) =>
        c.text(s, x, y, size: 10, color: _ink, align: align);
    void bodyBold(String s, double x, double y, {PdfAlign align = PdfAlign.left}) =>
        c.text(s, x, y, size: 10, color: _ink, bold: true, align: align);
    void small(String s, double x, double y, {PdfAlign align = PdfAlign.left}) =>
        c.text(s, x, y, size: 8.5, color: _muted, align: align);

    var y = _margin;

    // --- letterhead -----------------------------------------------------
    final logo = societyLogo(document, society);
    if (logo != null) c.image(logo, _margin, y, 58, 58);
    final textLeft = _margin + (logo != null ? 70 : 0);

    final name = society?.name ?? '';
    title(name.trim().isEmpty ? 'Housing Society' : name, textLeft, y + 16);
    var headerY = y + 32;
    final reg = society?.registrationNumber ?? '';
    if (reg.trim().isNotEmpty) {
      small('Reg. No: $reg', textLeft, headerY);
      headerY += 12;
    }
    final where = [society?.address, society?.city, society?.state, society?.pinCode]
        .whereType<String>()
        .where((s) => s.trim().isNotEmpty)
        .join(', ');
    if (where.trim().isNotEmpty) {
      small(where, textLeft, headerY);
      headerY += 12;
    }
    final reach = [
      if ((society?.contactNumber ?? '').trim().isNotEmpty) 'Tel: ${society!.contactNumber}',
      if ((society?.email ?? '').trim().isNotEmpty) society!.email,
    ].join('  •  ');
    if (reach.trim().isNotEmpty) {
      small(reach, textLeft, headerY);
      headerY += 12;
    }

    y = headerY > y + 66 ? headerY : y + 66;
    c.line(_margin, y, _pageWidth - _margin, y, width: 2, color: _teal);
    y += 26;

    // --- receipt title & meta -------------------------------------------
    c.text('MAINTENANCE PAYMENT RECEIPT', _pageWidth / 2, y,
        size: 13, color: _teal, bold: true, align: PdfAlign.center);
    y += 24;

    final paidOn = bill.paidDate.trim().isEmpty ? todayLabel() : bill.paidDate;
    final receiptNo = bill.receiptNumber.trim().isEmpty ? '-' : bill.receiptNumber;
    bodyBold('Receipt No: $receiptNo', _margin, y);
    bodyBold('Date: $paidOn', _pageWidth - _margin, y, align: PdfAlign.right);
    y += 22;

    small('Received with thanks from:', _margin, y);
    y += 15;
    bodyBold('${bill.residentName}  (Unit ${bill.flatNumber})', _margin, y);
    y += 15;
    body('Towards maintenance for: ${bill.billMonth}', _margin, y);
    y += 22;

    // --- particulars table ----------------------------------------------
    const amountX = _pageWidth - _margin;
    c.fillRect(_margin, y - 13, _pageWidth - _margin, y + 6, _tableFill);
    c.text('PARTICULARS', _margin + 8, y, size: 9, color: _teal, bold: true);
    c.text('AMOUNT (₹)', amountX - 8, y, size: 9, color: _teal, bold: true, align: PdfAlign.right);
    y += 20;

    final lines = <(String, double)>[
      ('Other / Common Maintenance', bill.baseAmount),
      if (bill.waterCharges > 0) ('Water Charges', bill.waterCharges),
      if (bill.sinkingFund > 0) ('Sinking Fund', bill.sinkingFund),
      if (bill.repairFund > 0) ('Repair & Maintenance Fund', bill.repairFund),
      if (bill.parkingCharges > 0) ('Parking Charges', bill.parkingCharges),
      if (bill.nonOccupancyCharges > 0) ('Non-Occupancy Charges', bill.nonOccupancyCharges),
      if (bill.lateFeeAmount > 0) ('Late Payment Fee', bill.lateFeeAmount),
    ];
    for (final (label, amount) in lines) {
      body(label, _margin + 8, y);
      body(fmt2(amount), amountX - 8, y, align: PdfAlign.right);
      y += 16;
    }

    y += 4;
    c.line(_margin, y, _pageWidth - _margin, y, width: 1, color: _rule);
    y += 18;
    bodyBold('Total Bill Amount', _margin + 8, y);
    bodyBold(fmt2(bill.totalAmount), amountX - 8, y, align: PdfAlign.right);
    y += 18;
    bodyBold('Amount Paid', _margin + 8, y);
    bodyBold(fmt2(bill.amountPaid), amountX - 8, y, align: PdfAlign.right);
    y += 18;

    final diff = bill.totalAmount - bill.amountPaid;
    final outstanding = diff < 0 ? 0.0 : diff;
    bodyBold('Balance Outstanding', _margin + 8, y);
    c.text(fmt2(outstanding), amountX - 8, y,
        size: 10, color: outstanding > 0 ? _red : _green, bold: true, align: PdfAlign.right);
    y += 10;
    c.line(_margin, y, _pageWidth - _margin, y, width: 1, color: _rule);
    y += 24;

    body('Amount in words: ${AmountWords.rupees(bill.amountPaid)}', _margin, y);
    y += 22;

    // --- payment details -------------------------------------------------
    heading('PAYMENT DETAILS', _margin, y);
    y += 16;
    body('Mode: ${bill.paymentMode?.labelEn ?? '-'}', _margin, y);
    y += 14;
    body('Reference / Txn No: ${bill.transactionRef.trim().isEmpty ? '-' : bill.transactionRef}', _margin, y);
    y += 14;
    body('Bill Due Date: ${bill.dueDate.trim().isEmpty ? '-' : bill.dueDate}', _margin, y);
    y += 14;
    body('Status: ${bill.status.name}', _margin, y);

    // --- signature & footer ----------------------------------------------
    const signY = _pageHeight - _margin - 70;
    c.line(_pageWidth - _margin - 160, signY, _pageWidth - _margin, signY, width: 1, color: _rule);
    small('Authorised Signatory', _pageWidth - _margin, signY + 14, align: PdfAlign.right);
    small('Hon. Treasurer / Secretary', _pageWidth - _margin, signY + 26, align: PdfAlign.right);

    c.text(
      'This is a computer-generated receipt from SocietySarv and does not require a physical signature.',
      _pageWidth / 2,
      _pageHeight - _margin,
      size: 7.5,
      color: _muted,
      align: PdfAlign.center,
    );
  }
}

/// The society's own logo when it uploaded one. (The Kotlin app fell back to
/// the launcher icon; the Flutter app bundles no image asset, so no logo is
/// drawn and the letterhead text starts at the margin.)
PdfImage? societyLogo(PdfDocument document, SocietyInfo? society) {
  final bytes = ImageBlob.toBytes(society?.logoBase64);
  if (bytes == null) return null;
  try {
    return PdfImage.file(document, bytes: bytes);
  } catch (_) {
    return null;
  }
}

/// Rupee amounts spelled out, the way a receipt book writes them.
class AmountWords {
  AmountWords._();

  static const List<String> _ones = [
    '', 'One', 'Two', 'Three', 'Four', 'Five', 'Six', 'Seven', 'Eight', 'Nine', 'Ten',
    'Eleven', 'Twelve', 'Thirteen', 'Fourteen', 'Fifteen', 'Sixteen', 'Seventeen',
    'Eighteen', 'Nineteen',
  ];
  static const List<String> _tens = [
    '', '', 'Twenty', 'Thirty', 'Forty', 'Fifty', 'Sixty', 'Seventy', 'Eighty', 'Ninety',
  ];

  static String rupees(double amount) {
    final whole = amount.truncate();
    // Kotlin Math.round: half rounds up (towards +infinity).
    final paise = ((amount - whole) * 100 + 0.5).floor();
    final rupeePart = whole == 0 ? 'Zero' : _indian(whole);
    if (paise > 0) {
      return 'Rupees $rupeePart and ${_indian(paise)} Paise Only';
    }
    return 'Rupees $rupeePart Only';
  }

  /// Crore / lakh / thousand grouping, not the western million system.
  static String _indian(int value) {
    if (value == 0) return '';
    final parts = <String>[];
    var left = value;

    final crore = left ~/ 10000000;
    left %= 10000000;
    final lakh = left ~/ 100000;
    left %= 100000;
    final thousand = left ~/ 1000;
    left %= 1000;
    final hundred = left ~/ 100;
    left %= 100;

    if (crore > 0) parts.add('${_indian(crore)} Crore');
    if (lakh > 0) parts.add('${_indian(lakh)} Lakh');
    if (thousand > 0) parts.add('${_indian(thousand)} Thousand');
    if (hundred > 0) parts.add('${_ones[hundred]} Hundred');
    if (left > 0) parts.add(_underHundred(left));
    return parts.join(' ');
  }

  static String _underHundred(int value) {
    if (value < 20) return _ones[value];
    if (value % 10 == 0) return _tens[value ~/ 10];
    return '${_tens[value ~/ 10]} ${_ones[value % 10]}';
  }
}
