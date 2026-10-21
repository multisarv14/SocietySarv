// Subscription status and member-role rules.
//
// Which state a society's plan is in on a given day, which plans are offered,
// who may change whose role, how the Chairman's post is handed over, and what
// a society staff role opens.

import 'package:flutter_test/flutter_test.dart';
import 'package:societysarv/data/models.dart';
import 'package:societysarv/state/society_view_model.dart';

Resident _member(String flat, UserRole role, {String name = 'Someone', String customRoleId = ''}) => Resident(
      docId: flat,
      flatNumber: flat,
      wing: 'A',
      ownerName: name,
      contactNumber: '9876543210',
      role: role,
      customRoleId: customRoleId,
    );

SocietySubscription _plan(String expiry) => SocietySubscription(
      planId: 'basic',
      planName: 'Basic',
      startDate: '2026-01-01',
      expiryDate: expiry,
    );

void main() {
  final today = DateTime(2026, 10, 21, 18, 30);

  group('subscription status', () {
    test('a society with no plan record is unassigned', () {
      expect(subscriptionStatusOf(null, today), SubscriptionStatus.NONE);
      expect(subscriptionStatusOf(_plan(''), today), SubscriptionStatus.NONE);
    });

    test('a plan with plenty of time left is active', () {
      expect(subscriptionStatusOf(_plan('2027-06-30'), today), SubscriptionStatus.ACTIVE);
    });

    test('a plan ending within the warning window is expiring soon', () {
      expect(subscriptionStatusOf(_plan('2026-11-05'), today), SubscriptionStatus.EXPIRING_SOON);
    });

    test('the expiry day itself is still covered, the day after is not', () {
      expect(subscriptionStatusOf(_plan('2026-10-21'), today), SubscriptionStatus.EXPIRING_SOON);
      expect(subscriptionDaysLeft(_plan('2026-10-21'), today), 0);
      expect(subscriptionStatusOf(_plan('2026-10-20'), today), SubscriptionStatus.EXPIRED);
    });

    test('only active plans are offered, in their set order', () {
      const plans = [
        SubscriptionPlan(id: 'pro', name: 'Pro', price: 9999, durationMonths: 12, sortOrder: 2),
        SubscriptionPlan(id: 'old', name: 'Old', price: 100, durationMonths: 12, isActive: false),
        SubscriptionPlan(id: 'basic', name: 'Basic', price: 4999, durationMonths: 12, sortOrder: 1),
      ];
      expect(offeredPlans(plans).map((p) => p.id), ['basic', 'pro']);
    });
  });

  group('role changes', () {
    final chairman = _member('A-101', UserRole.CHAIRMAN, name: 'Chair');
    final resident = _member('A-102', UserRole.RESIDENT);
    final guard = _member('GATE-01', UserRole.GUARD);

    test('the Chairman can give a member a committee post', () {
      expect(checkRoleChange(actor: chairman, target: resident, newRole: UserRole.SECRETARY), isNull);
    });

    test('nobody but the Chairman can change roles', () {
      final secretary = _member('A-103', UserRole.SECRETARY);
      expect(checkRoleChange(actor: secretary, target: resident, newRole: UserRole.TREASURER),
          RoleChangeRefusal.NOT_CHAIRMAN);
      expect(
          checkRoleChange(actor: null, target: resident, newRole: UserRole.TREASURER), RoleChangeRefusal.NOT_CHAIRMAN);
    });

    test('members and staff never swap kinds, so no person is duplicated', () {
      expect(checkRoleChange(actor: chairman, target: resident, newRole: UserRole.GUARD), RoleChangeRefusal.WRONG_KIND);
      expect(
          checkRoleChange(actor: chairman, target: guard, newRole: UserRole.SECRETARY), RoleChangeRefusal.WRONG_KIND);
    });

    test('a vacant unit cannot hold a committee post', () {
      final vacant = _member('A-104', UserRole.RESIDENT, name: '');
      expect(
          checkRoleChange(actor: chairman, target: vacant, newRole: UserRole.COMMITTEE), RoleChangeRefusal.VACANT_UNIT);
    });

    test('the Chairman\'s post is never simply given or taken away', () {
      expect(checkRoleChange(actor: chairman, target: resident, newRole: UserRole.CHAIRMAN),
          RoleChangeRefusal.CHAIRMAN_HANDOVER_ONLY);
      expect(checkRoleChange(actor: chairman, target: chairman, newRole: UserRole.RESIDENT),
          RoleChangeRefusal.CHAIRMAN_HANDOVER_ONLY);
    });

    test('a staff member can move to a society role, but not to a retired one', () {
      const liftman = SocietyRole(docId: 'r1', name: 'Lift Operator', baseRole: UserRole.MAINTENANCE_STAFF);
      expect(checkRoleChange(actor: chairman, target: guard, newRole: UserRole.GUARD, customRole: liftman), isNull);
      expect(
          checkRoleChange(
              actor: chairman, target: guard, newRole: UserRole.GUARD, customRole: liftman.copyWith(isActive: false)),
          RoleChangeRefusal.ROLE_UNAVAILABLE);
    });

    test('picking the role someone already holds changes nothing', () {
      expect(checkRoleChange(actor: chairman, target: guard, newRole: UserRole.GUARD), RoleChangeRefusal.UNCHANGED);
    });
  });

  group('Chairman handover', () {
    final chairman = _member('A-101', UserRole.CHAIRMAN, name: 'Chair');
    final resident = _member('A-102', UserRole.RESIDENT);

    test('the Chairman hands over to a member and steps down to another post', () {
      expect(checkChairmanHandover(actor: chairman, target: resident, actorNewRole: UserRole.COMMITTEE), isNull);
    });

    test('the outgoing Chairman must take a different, member role', () {
      expect(checkChairmanHandover(actor: chairman, target: resident, actorNewRole: UserRole.CHAIRMAN),
          RoleChangeRefusal.CHAIRMAN_HANDOVER_ONLY);
      expect(checkChairmanHandover(actor: chairman, target: resident, actorNewRole: UserRole.GUARD),
          RoleChangeRefusal.WRONG_KIND);
    });

    test('the post cannot go to staff, a vacant unit, or the Chairman themself', () {
      expect(
          checkChairmanHandover(
              actor: chairman, target: _member('G-1', UserRole.GUARD), actorNewRole: UserRole.RESIDENT),
          RoleChangeRefusal.WRONG_KIND);
      expect(
          checkChairmanHandover(
              actor: chairman, target: _member('A-109', UserRole.RESIDENT, name: ''), actorNewRole: UserRole.RESIDENT),
          RoleChangeRefusal.VACANT_UNIT);
      expect(checkChairmanHandover(actor: chairman, target: chairman, actorNewRole: UserRole.RESIDENT),
          RoleChangeRefusal.UNCHANGED);
    });

    test('only the sitting Chairman can hand over', () {
      final treasurer = _member('A-105', UserRole.TREASURER);
      expect(checkChairmanHandover(actor: treasurer, target: resident, actorNewRole: UserRole.RESIDENT),
          RoleChangeRefusal.NOT_CHAIRMAN);
    });
  });

  group('screens a role opens', () {
    const supervisor = SocietyRole(
      docId: 'sup',
      name: 'Supervisor',
      baseRole: UserRole.HOUSEKEEPING,
      menus: ['COMMUNITY', 'DASHBOARD', 'GATE'],
    );

    test('a society role opens exactly its screens, in bottom-bar order', () {
      final member = _member('SUP-1', UserRole.HOUSEKEEPING, customRoleId: 'sup');
      expect(destinationsForMember(member, const [supervisor]),
          [NavDestination.DASHBOARD, NavDestination.GATE, NavDestination.COMMUNITY]);
    });

    test('a deleted society role falls back to the built-in role', () {
      final member = _member('SUP-1', UserRole.HOUSEKEEPING, customRoleId: 'gone');
      expect(destinationsForMember(member, const [supervisor]), destinationsFor(UserRole.HOUSEKEEPING));
    });

    test('built-in roles are unchanged', () {
      expect(destinationsForMember(_member('A-101', UserRole.CHAIRMAN), const []), destinationsFor(UserRole.CHAIRMAN));
    });

    test('unknown or empty screen lists never lock a role out completely', () {
      expect(normaliseRoleMenus(const ['RESIDENTS', 'nonsense']), ['COMMUNITY']);
      expect(normaliseRoleMenus(const ['gate', 'DASHBOARD']), ['DASHBOARD', 'GATE']);
    });
  });
}
