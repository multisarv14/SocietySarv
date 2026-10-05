// Shared drawing helpers for the receipt and ledger PDFs.
//
// Public API:
//   enum PdfAlign { left, center, right }
//   class PdfCanvas
//     PdfCanvas(PdfDocument document, PdfPage page)
//     static const double pageWidth / pageHeight     // A4 at 72dpi: 595 x 842
//     void text(String s, double x, double baselineY, {double size = 10, PdfColor color, bool bold = false, PdfAlign align = PdfAlign.left})
//     void line(double x1, double y1, double x2, double y2, {double width = 1, PdfColor color})
//     void fillRect(double left, double top, double right, double bottom, PdfColor color)
//     void image(PdfImage image, double left, double top, double width, double height)
//   String pdfSafeText(String s)
//   Future<File> writeReportPdf(Uint8List bytes, String fileName)   // cacheDir/receipts/<fileName>
//   Future<String?> sharePdfFile(File file, {String? subject, String? text})
//
// The Kotlin PDFs were drawn on an android.graphics.Canvas with a top-left
// origin and text drawn at its baseline. This wraps the pdf package's
// low-level PdfGraphics (bottom-left origin) so the ported layout code can use
// the same coordinates as the Kotlin source line for line.

import 'dart:io';
import 'dart:typed_data';

import 'package:path_provider/path_provider.dart';
import 'package:pdf/pdf.dart';
import 'package:share_plus/share_plus.dart';

enum PdfAlign { left, center, right }

class PdfCanvas {
  PdfCanvas(PdfDocument document, this.page)
      : _g = page.getGraphics(),
        _regular = PdfFont.helvetica(document),
        _bold = PdfFont.helveticaBold(document);

  static const double pageWidth = 595;
  static const double pageHeight = 842;

  final PdfPage page;
  final PdfGraphics _g;
  final PdfFont _regular;
  final PdfFont _bold;

  double _flip(double y) => pageHeight - y;

  void text(
    String s,
    double x,
    double baselineY, {
    double size = 10,
    PdfColor color = PdfColors.black,
    bool bold = false,
    PdfAlign align = PdfAlign.left,
  }) {
    final safe = pdfSafeText(s);
    if (safe.isEmpty) return;
    final font = bold ? _bold : _regular;
    final width = font.stringMetrics(safe).advanceWidth * size;
    final left = switch (align) {
      PdfAlign.left => x,
      PdfAlign.center => x - width / 2,
      PdfAlign.right => x - width,
    };
    _g
      ..setFillColor(color)
      ..drawString(font, size, safe, left, _flip(baselineY));
  }

  void line(
    double x1,
    double y1,
    double x2,
    double y2, {
    double width = 1,
    PdfColor color = PdfColors.black,
  }) {
    _g
      ..setStrokeColor(color)
      ..setLineWidth(width)
      ..moveTo(x1, _flip(y1))
      ..lineTo(x2, _flip(y2))
      ..strokePath();
  }

  void fillRect(double left, double top, double right, double bottom, PdfColor color) {
    _g
      ..setFillColor(color)
      ..drawRect(left, _flip(bottom), right - left, bottom - top)
      ..fillPath();
  }

  void image(PdfImage image, double left, double top, double width, double height) {
    _g.drawImage(image, left, _flip(top + height), width, height);
  }
}

/// The built-in PDF fonts only cover Latin-1. The rupee sign becomes "Rs.",
/// the bullet a middle dot, and anything else outside Latin-1 (for example a
/// Devanagari name) a "?" rather than failing the whole document.
String pdfSafeText(String s) {
  final replaced = s.replaceAll('₹', 'Rs.').replaceAll('•', '·').replaceAll('–', '-').replaceAll('—', '-');
  final sb = StringBuffer();
  for (final rune in replaced.runes) {
    sb.writeCharCode(rune <= 0xFF ? rune : 0x3F);
  }
  return sb.toString();
}

/// Writes [bytes] into `<temporary dir>/receipts/<fileName>` and returns the file.
Future<File> writeReportPdf(Uint8List bytes, String fileName) async {
  final tmp = await getTemporaryDirectory();
  final dir = Directory('${tmp.path}/receipts');
  if (!dir.existsSync()) dir.createSync(recursive: true);
  final file = File('${dir.path}/$fileName');
  await file.writeAsBytes(bytes, flush: true);
  return file;
}

/// Opens the system share sheet for a PDF file. Returns null on success or an
/// error message when nothing could share it.
Future<String?> sharePdfFile(File file, {String? subject, String? text}) async {
  try {
    await SharePlus.instance.share(
      ShareParams(
        files: [XFile(file.path, mimeType: 'application/pdf')],
        subject: subject,
        text: text,
      ),
    );
    return null;
  } catch (_) {
    return 'No app available to share the PDF.';
  }
}
