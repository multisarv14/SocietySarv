import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/foundation.dart';

/// Firebase phone-number OTP, used by the "Forgot PIN" flow on the login screen.
///
/// The member proves they still hold the mobile number registered against their
/// flat, and only then may they set a new login PIN.
///
/// The Phone provider has to be enabled in the Firebase console and the app's
/// SHA-1/SHA-256 fingerprints registered, or [sendOtp] reports a config error.
class PhoneOtpManager {
  PhoneOtpManager._();

  static const _timeout = Duration(seconds: 60);

  static String? _verificationId;
  static int? _resendToken;

  /// True once a code has been sent and is waiting to be entered.
  static bool get isAwaitingCode => _verificationId != null;

  /// Normalises a 10-digit Indian mobile number into E.164.
  static String toE164(String rawNumber, {String defaultCountryCode = '+91'}) {
    final digitsOnly = rawNumber.split('').where((c) => RegExp(r'[0-9+]').hasMatch(c)).join();
    if (digitsOnly.startsWith('+')) return digitsOnly;
    if (digitsOnly.length > 10) return '+$digitsOnly';
    return '$defaultCountryCode$digitsOnly';
  }

  static void reset() {
    _verificationId = null;
    _resendToken = null;
  }

  /// Sends an OTP to [phoneNumber]. [onAutoVerified] fires when Android reads
  /// the SMS itself; [onCodeSent] when the member has to type the code.
  static Future<void> sendOtp({
    required String phoneNumber,
    bool resend = false,
    required VoidCallback onCodeSent,
    required VoidCallback onAutoVerified,
    required void Function(String) onError,
  }) async {
    try {
      await FirebaseAuth.instance.verifyPhoneNumber(
        phoneNumber: toE164(phoneNumber),
        timeout: _timeout,
        forceResendingToken: resend ? _resendToken : null,
        verificationCompleted: (credential) async {
          debugPrint('OTP auto-verified');
          onAutoVerified();
        },
        verificationFailed: (e) {
          debugPrint('OTP send failed: $e');
          onError(_describe(e));
        },
        codeSent: (id, token) {
          _verificationId = id;
          _resendToken = token;
          onCodeSent();
        },
        codeAutoRetrievalTimeout: (_) {},
      );
    } catch (e) {
      onError(_describe(e));
    }
  }

  /// Checks the 6-digit code the member typed against the code that was sent.
  static Future<void> verifyCode(String code, void Function(bool ok, String? error) onResult) async {
    final id = _verificationId;
    if (id == null) {
      onResult(false, 'Request an OTP first.');
      return;
    }
    try {
      final credential = PhoneAuthProvider.credential(verificationId: id, smsCode: code.trim());
      await FirebaseAuth.instance.signInWithCredential(credential);
      reset();
      onResult(true, null);
    } catch (e) {
      debugPrint('OTP verify failed: $e');
      onResult(false, _describe(e));
    }
  }

  static String _describe(Object e) {
    final message = e is FirebaseAuthException ? (e.message ?? e.code) : e.toString();
    if (e is FirebaseAuthException) {
      if (e.code == 'invalid-verification-code' || e.code == 'invalid-credential' || e.code == 'session-expired') {
        return 'That OTP is incorrect or has expired. Please try again.';
      }
      if (e.code == 'too-many-requests') {
        return 'Too many OTP requests from this device. Please try again later.';
      }
    }
    final lower = message.toLowerCase();
    if (lower.contains('billing_not_enabled')) return 'Phone sign-in is not enabled for this Firebase project yet.';
    if (lower.contains('not authorized')) {
      return 'This app is not authorised for phone sign-in. Enable the Phone provider in Firebase.';
    }
    return message.isEmpty ? 'Could not send the OTP. Check your network and try again.' : message;
  }
}
