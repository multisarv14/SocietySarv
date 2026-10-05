// Port of util/LedgerPdf.kt.
//
// Public API:
//   Future<Uint8List> buildLedgerPdf(Resident member, List<MaintenanceBill> bills, SocietyInfo? society)
//   class LedgerPdf
//     static Future<Uint8List> build(Resident member, List<MaintenanceBill> bills, SocietyInfo? society)
//     static String fileName(Resident member)                     // SocietySarv-Ledger-<unit>.pdf
//     static Future<File?> generate(Resident member, List<MaintenanceBill> bills, SocietyInfo? society)
//     static Future<String?> share(Resident member, List<MaintenanceBill> bills, SocietyInfo? society)
//     static Future<String?> view(Resident member, List<MaintenanceBill> bills, SocietyInfo? society)
//
// A member's ledger as a real PDF: the society letterhead, the member's unit,
// and one row per billed month - bill amount, late fee, total, paid and
// pending - closing with the totals the committee reads out at a meeting.
//
// Rolls onto a second page when a member has more months than one page holds,
// so a ledger is never silently cut short.

import 'dart:io';
import 'dart:typed_data';

import 'package:pdf/pdf.dart';
import 'package:printing/printing.dart';

import '../data/models.dart';
import 'format.dart';
import 'pdf_canvas.dart';
import 'receipt_pdf.dart' show societyLogo;

Future<Uint8List> buildLedgerPdf(Resident member, List<MaintenanceBill> bills, SocietyInfo? society) =>
    LedgerPdf.build(member, bills, society);

class LedgerPdf {
  LedgerPdf._();

  static const double _pageWidth = PdfCanvas.pageWidth;
  static const double _pageHeight = PdfCanvas.pageHeight;
  static const double _margin = 40;
  static const double _rowHeight = 18;

  static const PdfColor _teal = PdfColor.fromInt(0xFF027C75);
  static const PdfColor _ink = PdfColor.fromInt(0xFF111827);
  static const PdfColor _muted = PdfColor.fromInt(0xFF6B7280);
  static const PdfColor _rule = PdfColor.fromInt(0xFFD1D5DB);
  static const PdfColor _red = PdfColor.fromInt(0xFFB91C1C);
  static const PdfColor _green = PdfColor.fromInt(0xFF15803D);
  static const PdfColor _tableFill = PdfColor.fromInt(0xFFF0F7F6);

  static String fileName(Resident member) {
    final unit = member.flatNumber.trim().isEmpty ? 'UNIT' : member.flatNumber;
    return 'SocietySarv-Ledger-${unit.replaceAll(RegExp(r'[^A-Za-z0-9-]'), '-')}.pdf';
  }

  /// Writes the ledger and returns the file, or null if it could not be written.
  static Future<File?> generate(Resident member, List<MaintenanceBill> bills, SocietyInfo? society) async {
    try {
      final bytes = await build(member, bills, society);
      return await writeReportPdf(bytes, fileName(member));
    } catch (_) {
      return null;
    }
  }

  /// Generates the ledger and opens the system share sheet for it.
  static Future<String?> share(Resident member, List<MaintenanceBill> bills, SocietyInfo? society) async {
    final file = await generate(member, bills, society);
    if (file == null) return 'Could not create the ledger PDF.';
    final failure = await sharePdfFile(
      file,
      subject: '${society?.name ?? ''} - Member Ledger ${member.flatNumber}'.trim(),
      text: 'Month-wise maintenance ledger for ${member.flatNumber} (${member.ownerName}).',
    );
    return failure == null ? null : 'No app available to share the ledger.';
  }

  /// Opens the ledger in the system PDF preview / print sheet.
  static Future<String?> view(Resident member, List<MaintenanceBill> bills, SocietyInfo? society) async {
    final Uint8List bytes;
    try {
      bytes = await build(member, bills, society);
    } catch (_) {
      return 'Could not create the ledger PDF.';
    }
    try {
      await Printing.layoutPdf(onLayout: (_) async => bytes, name: fileName(member), format: PdfPageFormat.a4);
      return null;
    } catch (_) {
      return 'No app on this phone can open a PDF. Use Share instead.';
    }
  }

  static Future<Uint8List> build(Resident member, List<MaintenanceBill> bills, SocietyInfo? society) async {
    final document = PdfDocument();
    // Stable sort by month, like Kotlin's sortedBy.
    final indexed = bills.asMap().entries.toList()
      ..sort((a, b) {
        final c = _monthKey(a.value.billMonth).compareTo(_monthKey(b.value.billMonth));
        return c != 0 ? c : a.key.compareTo(b.key);
      });
    final ordered = indexed.map((e) => e.value).toList();
    final logo = societyLogo(document, society);

    var index = 0;
    var pageNumber = 1;
    do {
      final page = PdfPage(document, pageFormat: PdfPageFormat.a4);
      index = _drawPage(PdfCanvas(document, page), logo, member, ordered, society, index, pageNumber);
      pageNumber++;
    } while (index < ordered.length);

    return document.save();
  }

  // ------------------------------------------------------------- rendering

  /// Draws one page and returns the index of the first row that did not fit.
  static int _drawPage(
    PdfCanvas c,
    PdfImage? logo,
    Resident member,
    List<MaintenanceBill> bills,
    SocietyInfo? society,
    int from,
    int pageNumber,
  ) {
    void body(String s, double x, double y) => c.text(s, x, y, size: 9, color: _ink);
    void bodyBold(String s, double x, double y, {PdfAlign align = PdfAlign.left}) =>
        c.text(s, x, y, size: 9, color: _ink, bold: true, align: align);
    void small(String s, double x, double y, {PdfAlign align = PdfAlign.left}) =>
        c.text(s, x, y, size: 8.5, color: _muted, align: align);

    var y = _margin;

    if (pageNumber == 1) {
      if (logo != null) c.image(logo, _margin, y, 58, 58);
      final textLeft = _margin + (logo != null ? 70 : 0);
      final name = society?.name ?? '';
      c.text(name.trim().isEmpty ? 'Housing Society' : name, textLeft, y + 16, size: 16, color: _ink, bold: true);
      var headerY = y + 32;
      final reg = society?.registrationNumber ?? '';
      if (reg.trim().isNotEmpty) {
        small('Reg. No: $reg', textLeft, headerY);
        headerY += 12;
      }
      final where = [society?.address, society?.city, society?.state]
          .whereType<String>()
          .where((s) => s.trim().isNotEmpty)
          .join(', ');
      if (where.trim().isNotEmpty) small(where, textLeft, headerY);

      y = headerY > y + 66 ? headerY : y + 66;
      c.line(_margin, y, _pageWidth - _margin, y, width: 2, color: _teal);
      y += 26;

      c.text('MEMBER LEDGER - MONTH WISE', _pageWidth / 2, y,
          size: 13, color: _teal, bold: true, align: PdfAlign.center);
      y += 22;

      bodyBold('Unit: ${member.flatNumber}', _margin, y);
      bodyBold('Generated: ${todayLabel()}', _pageWidth - _margin, y, align: PdfAlign.right);
      y += 15;
      body('Member: ${member.ownerName}', _margin, y);
      y += 14;
      if (member.contactNumber.trim().isNotEmpty) {
        body('Contact: ${member.contactNumber}', _margin, y);
        y += 14;
      }
      y += 8;
    } else {
      bodyBold('Member Ledger - ${member.flatNumber} (continued)', _margin, y + 10);
      y += 30;
    }

    // Column layout: Month | Bill | Late Fee | Total | Paid | Pending
    const columns = <(double, PdfAlign)>[
      (_margin + 6, PdfAlign.left),
      (_margin + 160, PdfAlign.right),
      (_margin + 240, PdfAlign.right),
      (_margin + 330, PdfAlign.right),
      (_margin + 415, PdfAlign.right),
      (_pageWidth - _margin - 6, PdfAlign.right),
    ];
    const headers = ['MONTH', 'BILL (₹)', 'LATE FEE (₹)', 'TOTAL (₹)', 'PAID (₹)', 'PENDING (₹)'];

    c.fillRect(_margin, y - 12, _pageWidth - _margin, y + 6, _tableFill);
    for (var i = 0; i < headers.length; i++) {
      final (x, align) = columns[i];
      c.text(headers[i], x, y, size: 8, color: _teal, bold: true, align: align);
    }
    y += 20;

    const bottomLimit = _pageHeight - _margin - 120;
    var index = from;
    while (index < bills.length && y + _rowHeight < bottomLimit) {
      final bill = bills[index];
      final cells = [
        bill.billMonth,
        fmt2(bill.chargesSubtotal),
        fmt2(bill.lateFeeAmount),
        fmt2(bill.payableTotal),
        fmt2(bill.amountPaid),
        fmt2(bill.pendingAmount),
      ];
      for (var i = 0; i < cells.length; i++) {
        final (x, align) = columns[i];
        final color = i == 5 ? (bill.pendingAmount > 0.009 ? _red : _green) : _ink;
        c.text(cells[i], x, y, size: 9, color: color, align: align);
      }
      c.line(_margin, y + 5, _pageWidth - _margin, y + 5, width: 0.5, color: _rule);
      y += _rowHeight;
      index++;
    }

    // Totals go on the page that carries the last row.
    if (index >= bills.length) {
      double sum(double Function(MaintenanceBill) f) => bills.fold(0.0, (s, b) => s + f(b));
      final totalPending = sum((b) => b.pendingAmount);
      y += 8;
      c.line(_margin, y, _pageWidth - _margin, y, width: 1, color: _rule);
      y += 16;
      final totals = [
        'TOTAL',
        fmt2(sum((b) => b.chargesSubtotal)),
        fmt2(sum((b) => b.lateFeeAmount)),
        fmt2(sum((b) => b.payableTotal)),
        fmt2(sum((b) => b.amountPaid)),
        fmt2(totalPending),
      ];
      for (var i = 0; i < totals.length; i++) {
        final (x, align) = columns[i];
        c.text(totals[i], x, y, size: 9.5, color: _ink, bold: true, align: align);
      }
      y += 26;
      c.text('Outstanding as on date: ₹${fmt2(totalPending)}', _margin, y,
          size: 10, color: totalPending > 0.009 ? _red : _green, bold: true);

      const signY = _pageHeight - _margin - 60;
      c.line(_pageWidth - _margin - 160, signY, _pageWidth - _margin, signY, width: 1, color: _rule);
      small('Hon. Treasurer / Secretary', _pageWidth - _margin, signY + 14, align: PdfAlign.right);
    }

    c.text('Computer-generated ledger from SocietySarv - page $pageNumber', _pageWidth / 2, _pageHeight - _margin,
        size: 7.5, color: _muted, align: PdfAlign.center);
    return index;
  }

  static int _monthKey(String billMonth) => parseMonthYear(billMonth)?.millisecondsSinceEpoch ?? 0;
}
