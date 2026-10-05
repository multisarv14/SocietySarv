import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/foundation.dart';

/// Single entry point to the Cloud Firestore backend used by SocietySarv.
///
/// Project  : multisarv-mobileapps
/// Database : societysarv-app-db  (a named database, NOT "(default)" -- the
///            default database belongs to another app in the same project)
class FirebaseModule {
  FirebaseModule._();

  static const databaseId = 'societysarv-app-db';

  static FirebaseFirestore? _firestore;

  static FirebaseFirestore get firestore {
    return _firestore ??= FirebaseFirestore.instanceFor(app: Firebase.app(), databaseId: databaseId)
      // Offline persistence keeps the society data readable without a network
      // connection and re-syncs writes when it comes back.
      ..settings = const Settings(persistenceEnabled: true);
  }

  /// Signs the device in anonymously so Firestore rules can require an
  /// authenticated identity. A no-op if the Anonymous provider is not enabled.
  static Future<bool> ensureSignedIn() async {
    final auth = FirebaseAuth.instance;
    if (auth.currentUser != null) return true;
    try {
      final result = await auth.signInAnonymously();
      debugPrint('Anonymous sign-in ok: ${result.user?.uid}');
      return true;
    } catch (e) {
      // ADMIN_ONLY_OPERATION => the Anonymous provider is still off in the
      // console. Firestore rules currently allow unauthenticated access.
      debugPrint('Anonymous sign-in unavailable: $e');
      return false;
    }
  }
}

/// Firestore layout. Every society is its own document under [societies],
/// keyed by its login code, and all of its data lives in subcollections.
class Collections {
  Collections._();

  static const societies = 'societies';
  static const residents = 'residents';
  static const maintenanceBills = 'maintenance_bills';
  static const expenseRecords = 'expense_records';
  static const nonFeeRevenues = 'non_fee_revenues';
  static const visitorLogs = 'visitor_logs';
  static const notices = 'notices';
  static const complaints = 'complaints';
  static const emergencyContacts = 'emergency_contacts';
  static const counters = 'counters';

  /// Outgoing email, queued for the "Trigger Email from Firestore" worker.
  static const mail = 'mail';
}
