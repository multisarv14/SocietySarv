import 'package:flutter/material.dart';

// ==========================================
// SocietySarv — Material 3 colour system (ported from Color.kt)
// ==========================================

class AppColors {
  AppColors._();

  // 1. Primary & brand colours (deep teal & mint)
  static const tealPrimary = Color(0xFF00695C);
  static const tealDark = Color(0xFF004D40);
  static const tealLight = Color(0xFF00796B);
  static const tealContainer = Color(0xFFE0F2F1);
  static const onTealContainer = Color(0xFF004D40);

  // 2. Surface & background
  static const surfaceBg = Color(0xFFF8FAFC);
  static const surfaceCard = Color(0xFFFFFFFF);
  static const surfaceVariant = Color(0xFFF1F5F9);
  static const borderLight = Color(0xFFE2E8F0);
  static const borderFocus = Color(0xFF00796B);

  // 3. Typography
  static const textPrimary = Color(0xFF0F172A);
  static const textSecondary = Color(0xFF475569);
  static const textMuted = Color(0xFF94A3B8);

  // 4. Semantic & financial status
  static const successGreen = Color(0xFF16A34A);
  static const successContainer = Color(0xFFDCFCE7);
  static const dangerRed = Color(0xFFDC2626);
  static const dangerContainer = Color(0xFFFEE2E2);
  static const warningAmber = Color(0xFFD97706);
  static const warningContainer = Color(0xFFFEF3C7);
  static const accentBlue = Color(0xFF2563EB);
  static const accentBlueContainer = Color(0xFFDBEAFE);

  // Aliases used throughout the screens (same names as the Kotlin theme)
  static const onPrimary = Color(0xFFFFFFFF);
  static const mintSecondaryContainer = Color(0xFFB2DFDB);
  static const mintOnSecondaryContainer = Color(0xFF00363A);
  static const goldOnTertiaryContainer = Color(0xFF78350F);
  static const slateOutline = Color(0xFFCBD5E1);

  static const statusPaid = successGreen;
  static const statusPaidBg = successContainer;
  static const statusPending = warningAmber;
  static const statusPendingBg = warningContainer;
  static const statusOverdue = dangerRed;
  static const statusOverdueBg = dangerContainer;
  static const statusInfo = accentBlue;
  static const statusInfoBg = accentBlueContainer;
}
