// Port of util/UpiQr.kt.
//
// Public API:
//   class UpiQr
//     static String? paymentUri(String vpa, String payeeName, double amount, String transactionNote)
//     static String encode(String value)            // RFC 3986 query-value percent-encoding
//   class UpiQrImage extends StatelessWidget
//     UpiQrImage({Key? key, required String data, double size = 180})
//
// Turning a payment request into something a UPI app will actually open:
// building the `upi://pay` address correctly, and painting a scannable symbol
// for it (rendered with qr_flutter instead of the hand-written Kotlin encoder).

import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:qr_flutter/qr_flutter.dart';

class UpiQr {
  UpiQr._();

  /// Light modules around the symbol; without them scanners refuse to lock on.
  static const int quietZoneModules = 4;

  /// Builds the NPCI payment address:
  /// `upi://pay?pa=<vpa>&pn=<name>&am=<amount>&cu=INR&tn=<note>`.
  ///
  /// Returns null when the society has no UPI id - there is nothing real to
  /// pay into, and a QR built from an invented address takes a member's money
  /// nowhere.
  ///
  /// The amount is written only when it is positive, so a QR for a fully
  /// settled bill opens the payee rather than a ₹0.00 request the UPI apps
  /// reject outright.
  static String? paymentUri(
    String vpa,
    String payeeName,
    double amount,
    String transactionNote,
  ) {
    final cleanVpa = vpa.trim();
    if (cleanVpa.isEmpty) return null;

    final sb = StringBuffer()
      ..write('upi://pay?pa=')
      ..write(encode(cleanVpa))
      ..write('&pn=');
    final name = payeeName.trim();
    sb.write(encode(name.isEmpty ? 'Housing Society' : name));
    if (amount > 0.0) {
      sb
        ..write('&am=')
        ..write(amount.toStringAsFixed(2));
    }
    sb.write('&cu=INR');
    final note = transactionNote.trim();
    if (note.isNotEmpty) {
      sb
        ..write('&tn=')
        ..write(encode(note));
    }
    return sb.toString();
  }

  /// Percent-encoding for a URI query value.
  ///
  /// Form encoding writes a space as `+` and escapes characters differently,
  /// which is enough to turn a valid VPA into one no bank resolves. This keeps
  /// the unreserved set and percent-encodes everything else, space included.
  static String encode(String value) {
    final sb = StringBuffer();
    for (final code in utf8.encode(value)) {
      final isUnreserved =
          code < 128 &&
          ((code >= 0x41 && code <= 0x5A) || // A-Z
              (code >= 0x61 && code <= 0x7A) || // a-z
              (code >= 0x30 && code <= 0x39) || // 0-9
              code == 0x2D || // -
              code == 0x5F || // _
              code == 0x2E || // .
              code == 0x7E); // ~
      if (isUnreserved) {
        sb.writeCharCode(code);
      } else {
        sb
          ..write('%')
          ..write(code.toRadixString(16).toUpperCase().padLeft(2, '0'));
      }
    }
    return sb.toString();
  }
}

/// A scannable QR symbol for [data] on a white background with a quiet zone,
/// error-correction level M, [size] logical pixels square.
class UpiQrImage extends StatelessWidget {
  const UpiQrImage({super.key, required this.data, this.size = 180});

  final String data;
  final double size;

  @override
  Widget build(BuildContext context) {
    // Fixed-size box: QrImageView is built on a LayoutBuilder, which cannot
    // answer the intrinsic-size queries an AlertDialog makes.
    return SizedBox(
      width: size,
      height: size,
      child: QrImageView(
        data: data,
        version: QrVersions.auto,
        size: size,
        backgroundColor: Colors.white,
        errorCorrectionLevel: QrErrorCorrectLevel.M,
        // Roughly four modules of quiet zone for a typical UPI payload.
        padding: EdgeInsets.all(size * 0.06),
        eyeStyle: const QrEyeStyle(
          eyeShape: QrEyeShape.square,
          color: Colors.black,
        ),
        dataModuleStyle: const QrDataModuleStyle(
          dataModuleShape: QrDataModuleShape.square,
          color: Colors.black,
        ),
        semanticsLabel: 'UPI QR Code',
      ),
    );
  }
}
