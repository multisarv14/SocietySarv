// Port of FinancialYearTest.kt.
//
// A society's financial year runs April to March, so January belongs to the
// year that started the previous April - not to the calendar year it falls in.

import 'package:flutter_test/flutter_test.dart';
import 'package:societysarv/data/models.dart';

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

  test('april starts a new financial year', () {
    expect(financialYearOf('April 2026'), 'FY 2026-2027');
    expect(financialYearOf('December 2026'), 'FY 2026-2027');
  });

  test('january to march belong to the year that started last april', () {
    expect(financialYearOf('January 2026'), 'FY 2025-2026');
    expect(financialYearOf('March 2026'), 'FY 2025-2026');
  });

  test('march and april either side of the boundary differ', () {
    expect(financialYearOf('March 2026'), 'FY 2025-2026');
    expect(financialYearOf('April 2026'), 'FY 2026-2027');
  });

  test('an unparseable month yields nothing rather than a wrong year', () {
    expect(financialYearOf('not a month'), '');
    expect(financialYearOf(''), '');
  });

  test('a society billing across a boundary offers both years', () {
    final society = _society(billingStartMonth: 'January 2026');

    final years = financialYears(society);

    expect(years, contains('FY 2025-2026'), reason: 'expected FY 2025-2026 in $years');
    expect(years.length, greaterThanOrEqualTo(1), reason: 'expected more than one year in $years');
    expect(years.toSet().toList(), years);
  });
}
