// Port of MemberProfileEditTest.kt.
//
// What a member may change about themselves.
//
// The rule the committee asked for is narrow and worth pinning down: a member
// fills in what the society never recorded, and touches nothing else. Everyone
// else editing their own profile is unaffected.

import 'package:flutter_test/flutter_test.dart';
import 'package:societysarv/data/models.dart';

Resident _member({
  UserRole role = UserRole.RESIDENT,
  String ownerName = 'Sunil Patil',
  String coApplicantName = '',
  String contactNumber = '',
  String email = '',
  String photoBase64 = '',
}) =>
    Resident(
      docId: 'A-202',
      flatNumber: 'A-202',
      wing: 'A',
      ownerName: ownerName,
      coApplicantName: coApplicantName,
      contactNumber: contactNumber,
      email: email,
      photoBase64: photoBase64,
      role: role,
    );

/// Positional shorthand matching the Kotlin call sites.
Resident _edit(Resident r, String owner, String co, String contact, String email, String photo,
        {required bool addOnly}) =>
    r.withProfileEdits(
      ownerName: owner,
      coApplicantName: co,
      contactNumber: contact,
      email: email,
      photoBase64: photo,
      addOnly: addOnly,
    );

void main() {
  test('a member cannot overwrite a detail the society already holds', () {
    final onFile = _member(ownerName: 'Sunil Patil', contactNumber: '9876543210');

    final edited = onFile.withProfileEdits(
      ownerName: 'Somebody Else',
      coApplicantName: '',
      contactNumber: '9000000000',
      email: '',
      photoBase64: '',
      addOnly: true,
    );

    expect(edited.ownerName, 'Sunil Patil');
    expect(edited.contactNumber, '9876543210');
  });

  test('a member can fill in what is missing', () {
    final onFile = _member(ownerName: 'Sunil Patil', email: '');

    final edited = onFile.withProfileEdits(
      ownerName: 'Sunil Patil',
      coApplicantName: 'Asha Patil',
      contactNumber: '',
      email: 'sunil@example.com',
      photoBase64: '',
      addOnly: true,
    );

    expect(edited.email, 'sunil@example.com');
    expect(edited.coApplicantName, 'Asha Patil');
  });

  test('a co-applicant already on record cannot be renamed by the member', () {
    final onFile = _member(coApplicantName: 'Asha Patil');

    final edited = onFile.withProfileEdits(
      ownerName: '',
      coApplicantName: 'Someone Else',
      contactNumber: '',
      email: '',
      photoBase64: '',
      addOnly: true,
    );

    expect(edited.coApplicantName, 'Asha Patil');
  });

  test('a photo already uploaded is not replaced, and a missing one can be added', () {
    final withPhoto = _member(photoBase64: 'existing-photo');
    final withoutPhoto = _member(photoBase64: '');

    expect(_edit(withPhoto, '', '', '', '', 'new-photo', addOnly: true).photoBase64, 'existing-photo');
    expect(_edit(withoutPhoto, '', '', '', '', 'new-photo', addOnly: true).photoBase64, 'new-photo');
  });

  test('a member clearing a field they filled in cannot blank the register', () {
    final onFile = _member(email: 'sunil@example.com');

    final edited = _edit(onFile, '', '', '', '', '', addOnly: true);

    expect(edited.email, 'sunil@example.com');
  });

  test('the name is never blanked, even for somebody editing freely', () {
    final onFile = _member(ownerName: 'Sunil Patil');

    final edited = _edit(onFile, '   ', '', '9876543210', '', '', addOnly: false);

    expect(edited.ownerName, 'Sunil Patil');
  });

  test('the committee and the staff still edit their own details freely', () {
    final chairman = _member(role: UserRole.CHAIRMAN, ownerName: 'Sunil Patil', contactNumber: '9876543210');

    final edited = chairman.withProfileEdits(
      ownerName: 'Sunil M Patil',
      coApplicantName: '',
      contactNumber: '9000000000',
      email: 'chair@example.com',
      photoBase64: '',
      addOnly: chairman.role.isMember,
    );

    expect(edited.ownerName, 'Sunil M Patil');
    expect(edited.contactNumber, '9000000000');
    expect(edited.email, 'chair@example.com');
  });

  test('whitespace around a newly added detail is trimmed off', () {
    final onFile = _member(email: '');

    final edited = _edit(onFile, '', '', '', '  sunil@example.com  ', '', addOnly: true);

    expect(edited.email, 'sunil@example.com');
  });

  test('a member whose record is complete changes nothing at all', () {
    final onFile = _member(
      ownerName: 'Sunil Patil',
      coApplicantName: 'Asha Patil',
      contactNumber: '9876543210',
      email: 'sunil@example.com',
      photoBase64: 'photo',
    );

    final edited = _edit(onFile, 'A', 'B', 'C', 'D', 'E', addOnly: true);

    // Kotlin compared data classes with assertEquals; Dart's Resident has no
    // value ==, so compare the profile fields plus the identity fields.
    expect(edited.sameProfileAs(onFile), isTrue);
    expect(edited.docId, onFile.docId);
    expect(edited.flatNumber, onFile.flatNumber);
    expect(edited.wing, onFile.wing);
    expect(edited.role, onFile.role);
  });

  test('residents and tenants are members, the committee and staff are not', () {
    expect(UserRole.RESIDENT.isMember, isTrue);
    expect(UserRole.TENANT.isMember, isTrue);
    expect(UserRole.CHAIRMAN.isMember, isFalse);
    expect(UserRole.TREASURER.isMember, isFalse);
    expect(UserRole.GUARD.isMember, isFalse);
  });
}
