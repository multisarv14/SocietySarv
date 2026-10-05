// Number and date formatting shared by every screen, replacing Kotlin's
// String.format("%,.2f") and SimpleDateFormat calls.

import 'dart:math';

import 'package:intl/intl.dart';

final NumberFormat _grouped2 = NumberFormat('#,##0.00', 'en_US');
final NumberFormat _grouped0 = NumberFormat('#,##0', 'en_US');

/// Kotlin "%,.2f" - e.g. 12,345.50
String fmt2(double value) => _grouped2.format(value);

/// Kotlin "%,.0f" - e.g. 12,346
String fmt0(double value) => _grouped0.format(value);

/// Kotlin "%.2f" - no grouping.
String plain2(double value) => value.toStringAsFixed(2);

/// "₹12,345.50"
String rupees(double value) => '₹${fmt2(value)}';

/// "₹12,346"
String rupees0(double value) => '₹${fmt0(value)}';

/// A number field's text back to a double, tolerating commas and blanks.
double parseAmount(String raw) => double.tryParse(raw.replaceAll(',', '').trim()) ?? 0.0;

/// A number field's text back to an int, tolerating blanks.
int parseCount(String raw) => int.tryParse(raw.trim()) ?? 0;

/// "dd MMM yyyy" - the date format every record in Firestore uses.
String todayLabel() => DateFormat('dd MMM yyyy', 'en_US').format(DateTime.now());

String formatDate(DateTime d, String pattern) => DateFormat(pattern, 'en_US').format(d);

/// "hh:mm a" - visitor entry / exit times.
String nowTimeLabel() => DateFormat('hh:mm a', 'en_US').format(DateTime.now());

/// "August 2026" for the current month.
String currentMonthLabel() => DateFormat('MMMM yyyy', 'en_US').format(DateTime.now());

final Random _random = Random();

/// Random int in [from, until) - Kotlin's Random.nextInt(from, until).
int randomBetween(int from, int until) => from + _random.nextInt(until - from);
