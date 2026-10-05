// How money a member hands over is spread across what they owe.
//
// A society is paid in round figures, not bill-sized instalments: one member
// pays five months of arrears at once, another pays part, a third overpays.
// All three are the same operation - settle the oldest arrears first and keep
// whatever is left over as credit against the months still to be billed.

import 'models.dart';

/// The month a bill belongs to, as a sortable number. Unparseable months sort first.
int billMonthKey(String billMonth) => parseMonthYear(billMonth)?.millisecondsSinceEpoch ?? 0;

/// Bills in the order arrears are settled: oldest month first.
List<MaintenanceBill> oldestMonthFirst(List<MaintenanceBill> bills) {
  final sorted = [...bills];
  sorted.sort((a, b) {
    final byMonth = billMonthKey(a.billMonth).compareTo(billMonthKey(b.billMonth));
    return byMonth != 0 ? byMonth : a.id.compareTo(b.id);
  });
  return sorted;
}

/// One bill, and how much of a payment landed on it.
class BillAllocation {
  const BillAllocation(this.bill, this.applied);
  final MaintenanceBill bill;
  final double applied;

  /// The bill as it stands once [applied] has been recorded against it.
  double get pendingAfter {
    final p = bill.payableTotal - bill.amountPaid - applied;
    return roundToPaise(p < 0 ? 0.0 : p);
  }

  /// True when this payment clears the bill outright.
  bool get isSettled => pendingAfter <= 0.009;
}

/// The result of spreading one payment over a unit's bills.
class PaymentAllocation {
  const PaymentAllocation(this.allocations, this.excess);
  final List<BillAllocation> allocations;

  /// What the payment could not be used for - carried forward as credit.
  final double excess;

  /// What actually landed on bills, excess aside.
  double get applied => roundToPaise(allocations.fold(0.0, (s, a) => s + a.applied));

  /// Bills this payment touched at all.
  List<BillAllocation> get touched => allocations.where((a) => a.applied > 0.009).toList();

  /// Bills this payment cleared outright.
  int get settledCount => touched.where((a) => a.isSettled).length;

  /// Months this payment went against, oldest first, for the receipt note.
  List<String> get months => touched.map((a) => a.bill.billMonth).toList();
}

/// Spreads [amount] over [bills], oldest month first.
PaymentAllocation allocatePayment(List<MaintenanceBill> bills, double amount) {
  var remaining = roundToPaise(amount < 0 ? 0.0 : amount);
  final allocations = <BillAllocation>[];
  for (final bill in oldestMonthFirst(bills)) {
    final owed = bill.pendingAmount;
    final applied = (remaining <= 0.009 || owed <= 0.009) ? 0.0 : roundToPaise(remaining < owed ? remaining : owed);
    remaining = roundToPaise(remaining - applied);
    allocations.add(BillAllocation(bill, applied));
  }
  return PaymentAllocation(allocations, roundToPaise(remaining < 0 ? 0.0 : remaining));
}

/// What a unit owes, in the three parts a member is actually shown.
class UnitDues {
  const UnitDues({
    required this.previousPending,
    required this.currentBill,
    required this.currentPending,
    required this.excessCredit,
  });

  /// Everything still owed on bills before the latest billed month.
  final double previousPending;

  /// What the latest billed month came to, late fee included.
  final double currentBill;

  /// Still owed on the latest billed month alone.
  final double currentPending;

  /// Credit the member is already in, carried forward from an overpayment.
  final double excessCredit;

  /// Everything owed across every bill, before any credit is applied.
  double get grossOutstanding => roundToPaise(previousPending + currentPending);

  /// What the member is actually asked to pay: what is owed, less their credit.
  double get netPayable {
    final n = grossOutstanding - excessCredit;
    return roundToPaise(n < 0 ? 0.0 : n);
  }

  /// Credit left over once every outstanding bill is covered by it.
  double get creditInHand {
    final c = excessCredit - grossOutstanding;
    return roundToPaise(c < 0 ? 0.0 : c);
  }

  /// True when the member owes nothing after their credit is taken into account.
  bool get isSettled => netPayable <= 0.009;
}

/// Works [UnitDues] out from a unit's own bills and its carried-forward credit.
UnitDues unitDues(List<MaintenanceBill> bills, [double excessCredit = 0.0]) {
  final ordered = oldestMonthFirst(bills);
  final latest = ordered.isEmpty ? null : ordered.last;
  final earlier = ordered.isEmpty ? <MaintenanceBill>[] : ordered.sublist(0, ordered.length - 1);
  return UnitDues(
    previousPending: roundToPaise(earlier.fold(0.0, (s, b) => s + b.pendingAmount)),
    currentBill: roundToPaise(latest?.payableTotal ?? 0.0),
    currentPending: roundToPaise(latest?.pendingAmount ?? 0.0),
    excessCredit: roundToPaise(excessCredit < 0 ? 0.0 : excessCredit),
  );
}
