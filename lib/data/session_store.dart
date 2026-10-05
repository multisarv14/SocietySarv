import 'package:shared_preferences/shared_preferences.dart';

/// A remembered session: the society code and login identifier, never the PIN.
class SavedSession {
  const SavedSession(this.societyCode, this.identifier);
  final String societyCode;
  final String identifier;
}

/// Remembers who was signed in so the app does not ask for a society code,
/// unit number and PIN every time it is opened. On the next launch the member
/// is looked up in Firestore again, so a removed or deactivated account does not
/// stay signed in on a stale device.
class SessionStore {
  static const _keySociety = 'society_code';
  static const _keyIdentifier = 'login_identifier';

  Future<void> save(String societyCode, String identifier) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_keySociety, societyCode.trim().toUpperCase());
    await prefs.setString(_keyIdentifier, identifier.trim());
  }

  Future<SavedSession?> read() async {
    final prefs = await SharedPreferences.getInstance();
    final society = prefs.getString(_keySociety)?.trim() ?? '';
    final identifier = prefs.getString(_keyIdentifier)?.trim() ?? '';
    if (society.isEmpty || identifier.isEmpty) return null;
    return SavedSession(society, identifier);
  }

  Future<void> clear() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_keySociety);
    await prefs.remove(_keyIdentifier);
  }
}
