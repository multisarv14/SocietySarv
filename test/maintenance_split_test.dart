// Port of MaintenanceSplitTest.kt (MaintenanceSplitTest + UnitTariffTest).

import 'package:flutter_test/flutter_test.dart';
import 'package:societysarv/data/models.dart';

const _eps = 0.001;

SocietyInfo _society({
  double water = 10.0,
  double sinking = 5.0,
  double repair = 20.0,
  double other = 65.0,
  List<UnitTypeConfig> types = const [],
}) =>
    SocietyInfo(
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
      waterChargesPercent: water,
      sinkingFundPercent: sinking,
      repairFundPercent: repair,
      otherMaintenancePercent: other,
      unitTypes: types,
    );

Resident _unit({required String unitType, FlatType flatType = FlatType.TWO_BHK}) => Resident(
      flatNumber: 'A-101',
      wing: 'A',
      ownerName: 'Test Member',
      contactNumber: '9999999999',
      unitType: unitType,
      flatType: flatType,
    );

void main() {
  // How a month's maintenance is divided between the society's funds. The shares
  // are of the amount, not additions to it, so they must always add back up to it.
  group('MaintenanceSplitTest', () {
    test('the configured example splits exactly as specified', () {
      final split = _society(water: 10.0, sinking: 5.0, repair: 20.0, other: 65.0).splitMaintenance(1100.0);

      expect(split.water, closeTo(110.0, _eps));
      expect(split.sinking, closeTo(55.0, _eps));
      expect(split.repair, closeTo(220.0, _eps));
      expect(split.other, closeTo(715.0, _eps));
      expect(split.total, closeTo(1100.0, _eps));
    });

    test('rounding leftovers land on other so the total still matches', () {
      // 33.33% of 1000 is 333.30, which does not divide cleanly.
      final split = _society(water: 10.0, sinking: 5.0, repair: 33.33, other: 51.67).splitMaintenance(1000.0);

      expect(split.water, closeTo(100.0, _eps));
      expect(split.sinking, closeTo(50.0, _eps));
      expect(split.repair, closeTo(333.3, _eps));
      expect(split.total, closeTo(1000.0, _eps));
    });

    test('every share is rounded to paise', () {
      final split = _society(water: 33.333, sinking: 33.333, repair: 33.334, other: 0.0).splitMaintenance(100.0);

      for (final amount in [split.water, split.sinking, split.repair, split.other]) {
        // Kotlin's Math.round(x) is floor(x + 0.5).
        expect(amount, (amount * 100 + 0.5).floorToDouble() / 100.0);
      }
      expect(split.total, closeTo(100.0, _eps));
    });

    test('a zero amount splits into nothing', () {
      final split = _society(water: 10.0, sinking: 5.0, repair: 20.0, other: 65.0).splitMaintenance(0.0);

      expect(split.total, closeTo(0.0, _eps));
    });

    test('percentages that add up to a hundred are balanced', () {
      final balanced = _society(water: 10.0, sinking: 5.0, repair: 20.0, other: 65.0);

      expect(balanced.maintenancePercentTotal, closeTo(100.0, _eps));
      expect(balanced.hasBalancedMaintenancePercents, isTrue);
    });

    test('percentages that do not add up to a hundred are flagged', () {
      final lopsided = _society(water: 10.0, sinking: 5.0, repair: 20.0, other: 50.0);

      expect(lopsided.maintenancePercentTotal, closeTo(85.0, _eps));
      expect(lopsided.hasBalancedMaintenancePercents, isFalse);
    });
  });

  // Which tariff a unit is charged. The society's own unit type is what the
  // committee prices, so it has to win over the built-in slabs.
  group('UnitTariffTest', () {
    test("the unit's own type is what it is charged", () {
      final society = _society(types: const [
        UnitTypeConfig(name: '1 BHK', monthlyTariff: 900.0),
        UnitTypeConfig(name: '2 BHK', monthlyTariff: 1400.0),
      ]);

      expect(society.monthlyTariffFor(_unit(unitType: '1 BHK')), closeTo(900.0, _eps));
      expect(society.monthlyTariffFor(_unit(unitType: '2 BHK')), closeTo(1400.0, _eps));
    });

    test('the type is matched whatever the case', () {
      final society = _society(types: const [UnitTypeConfig(name: 'Shop', monthlyTariff: 3000.0)]);

      expect(society.monthlyTariffFor(_unit(unitType: 'shop')), closeTo(3000.0, _eps));
    });

    test('a unit with no type falls back to its slab rather than nothing', () {
      final society = _society(types: const [UnitTypeConfig(name: '1 BHK', monthlyTariff: 900.0)]);

      final charged = society.monthlyTariffFor(_unit(unitType: '', flatType: FlatType.TWO_BHK));

      expect(charged, closeTo(2200.0, _eps));
    });

    test('a type with no tariff yet falls back rather than billing zero', () {
      final society = _society(types: const [UnitTypeConfig(name: 'Office', monthlyTariff: 0.0)]);

      final charged = society.monthlyTariffFor(_unit(unitType: 'Office', flatType: FlatType.TWO_BHK));

      expect(charged, closeTo(2200.0, _eps));
    });
  });
}
