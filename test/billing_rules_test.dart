// Port of BillingRulesTest.kt.
//
// The two pieces of billing arithmetic the whole app leans on: how a wing
// expands into unit numbers, and which months a society can be billed for.

import 'package:flutter_test/flutter_test.dart';
import 'package:intl/intl.dart';
import 'package:societysarv/data/models.dart';

String _monthsFromNow(int offset) {
  final now = DateTime.now();
  return DateFormat('MMMM yyyy', 'en_US').format(DateTime(now.year, now.month + offset, 1));
}

SocietyInfo _society({required String billingStartMonth}) => SocietyInfo(
      name: 'Test Society',
      registrationNumber: 'REG-1',
      societyCode: 'TS001',
      address: '',
      totalWings: 1,
      totalFlats: 0,
      bankAccountName: '',
      bankAccountNumber: '',
      ifscCode: '',
      currentBalanceCash: 0.0,
      currentBalanceBank: 0.0,
      billingStartMonth: billingStartMonth,
    );

void main() {
  // The app does this in main() before any month label is parsed or formatted.

  test('a wing spreads its units evenly over its floors', () {
    const plan = WingPlan(
      name: 'A',
      totalFloors: 2,
      unitCounts: [WingUnitCount(unitType: '1 BHK', quantity: 3), WingUnitCount(unitType: '2 BHK', quantity: 3)],
    );

    expect(plan.totalUnits, 6);
    expect(plan.generateUnitNumbers(), ['A-101', 'A-102', 'A-103', 'A-201', 'A-202', 'A-203']);
  });

  test('unit types line up one for one with the generated numbers', () {
    const plan = WingPlan(
      name: 'A',
      totalFloors: 2,
      unitCounts: [WingUnitCount(unitType: '1 BHK', quantity: 3), WingUnitCount(unitType: '2 BHK', quantity: 1)],
    );

    final numbers = plan.generateUnitNumbers();
    final types = plan.unitTypesInOrder();

    expect(types.length, numbers.length);
    expect(types, ['1 BHK', '1 BHK', '1 BHK', '2 BHK']);
  });

  test('a remainder lands on the last floor rather than being lost', () {
    const plan = WingPlan(name: 'B', totalFloors: 3, unitCounts: [WingUnitCount(unitType: '1 BHK', quantity: 13)]);

    final numbers = plan.generateUnitNumbers();

    expect(numbers.length, 13);
    expect(numbers.first, 'B-101');
    expect(numbers.last, 'B-303');
  });

  test('a wing with no floors entered still creates its units on one floor', () {
    const plan = WingPlan(name: 'S', totalFloors: 0, unitCounts: [WingUnitCount(unitType: 'Shop', quantity: 4)]);

    expect(plan.totalUnits, 4);
    expect(plan.generateUnitNumbers(), ['S-101', 'S-102', 'S-103', 'S-104']);
  });

  test('a wing with no unit types creates nothing', () {
    const plan = WingPlan(name: 'C', totalFloors: 5);

    expect(plan.totalUnits, 0);
    expect(plan.generateUnitNumbers(), isEmpty);
  });

  test('billing months run from the configured start month to this month', () {
    final threeMonthsAgo = _monthsFromNow(-3);
    final society = _society(billingStartMonth: threeMonthsAgo);

    final months = billingMonths(society);

    expect(months.length, 4);
    expect(months.first, _monthsFromNow(0));
    expect(months.last, threeMonthsAgo);
  });

  test('a start month in the future yields only that month', () {
    final nextMonth = _monthsFromNow(1);

    expect(billingMonths(_society(billingStartMonth: nextMonth)), [nextMonth]);
  });

  test('a society with no start month falls back to the last twelve months', () {
    final months = billingMonths(_society(billingStartMonth: ''));

    expect(months.length, 12);
    expect(months.first, _monthsFromNow(0));
    expect(months.last, _monthsFromNow(-11));
  });

  test('an unparseable start month falls back rather than producing nothing', () {
    final months = billingMonths(_society(billingStartMonth: 'not a month'));

    expect(months, isNotEmpty);
    expect(months.first, _monthsFromNow(0));
  });
}
