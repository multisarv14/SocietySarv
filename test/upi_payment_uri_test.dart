// Port of UpiPaymentUriTest.kt.
//
// The address a UPI app is handed when a member taps Pay by QR.
//
// The old builder used form encoding, which writes a space as `+` and is not
// what a URI query expects - enough to leave a payee name or note mangled in
// the app that opens it.

import 'package:flutter_test/flutter_test.dart';
import 'package:societysarv/util/upi_qr.dart';

void main() {
  test('a payment request carries the payee, the amount and the note', () {
    final uri = UpiQr.paymentUri('greenvalley@okhdfcbank', 'Green Valley CHS', 5500.0, 'Maintenance A-202 August 2026');

    expect(
      uri,
      'upi://pay?pa=greenvalley%40okhdfcbank&pn=Green%20Valley%20CHS'
      '&am=5500.00&cu=INR&tn=Maintenance%20A-202%20August%202026',
    );
  });

  test('spaces are percent-encoded, never turned into plus signs', () {
    final uri = UpiQr.paymentUri('a@b', 'Sai Krupa Society', 100.0, 'July dues')!;

    expect(uri, contains('pn=Sai%20Krupa%20Society'));
    expect(uri.contains('+'), isFalse);
  });

  test('a society with no UPI id gets no payment address at all', () {
    expect(UpiQr.paymentUri('', 'Green Valley CHS', 5500.0, 'dues'), isNull);
    expect(UpiQr.paymentUri('   ', 'Green Valley CHS', 5500.0, 'dues'), isNull);
  });

  test('a settled bill asks for no amount rather than for zero rupees', () {
    // UPI apps reject a ₹0.00 request outright, so the amount is left off
    // and the member simply lands on the society as payee.
    final uri = UpiQr.paymentUri('a@b', 'Society', 0.0, 'dues')!;

    expect(uri.contains('am='), isFalse);
    expect(uri, contains('cu=INR'));
  });

  test('paise are always written to two places', () {
    final uri = UpiQr.paymentUri('a@b', 'Society', 1234.5, 'dues')!;

    expect(uri, contains('am=1234.50'));
  });

  test('a name in Devanagari survives the round trip as UTF-8', () {
    final uri = UpiQr.paymentUri('a@b', 'सोसायटी', 100.0, 'dues')!;

    // UTF-8 for "सोसायटी", percent-encoded byte by byte.
    expect(uri, contains('pn=%E0%A4%B8%E0%A5%8B%E0%A4%B8%E0%A4%BE%E0%A4%AF%E0%A4%9F%E0%A5%80'));
  });

  test('an unnamed society and an empty note still produce a usable address', () {
    final uri = UpiQr.paymentUri('a@b', '', 100.0, '')!;

    expect(uri, 'upi://pay?pa=a%40b&pn=Housing%20Society&am=100.00&cu=INR');
  });

  test('surrounding whitespace on a UPI id is trimmed rather than encoded', () {
    final uri = UpiQr.paymentUri('  society@okaxis  ', 'Society', 100.0, 'dues')!;

    expect(uri, startsWith('upi://pay?pa=society%40okaxis&'));
  });
}
