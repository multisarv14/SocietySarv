import 'package:flutter/material.dart';

import 'app_colors.dart';

/// Light Material 3 theme matching the Compose `SocietySarvTheme`.
ThemeData buildAppTheme() {
  const scheme = ColorScheme(
    brightness: Brightness.light,
    primary: AppColors.tealPrimary,
    onPrimary: AppColors.onPrimary,
    primaryContainer: AppColors.tealContainer,
    onPrimaryContainer: AppColors.onTealContainer,
    secondary: AppColors.tealLight,
    onSecondary: AppColors.onPrimary,
    secondaryContainer: AppColors.mintSecondaryContainer,
    onSecondaryContainer: AppColors.mintOnSecondaryContainer,
    tertiary: AppColors.warningAmber,
    onTertiary: AppColors.onPrimary,
    tertiaryContainer: AppColors.warningContainer,
    onTertiaryContainer: AppColors.goldOnTertiaryContainer,
    surface: AppColors.surfaceCard,
    onSurface: AppColors.textPrimary,
    surfaceContainerHighest: AppColors.surfaceVariant,
    onSurfaceVariant: AppColors.textSecondary,
    outline: AppColors.slateOutline,
    outlineVariant: AppColors.borderLight,
    error: AppColors.dangerRed,
    onError: AppColors.onPrimary,
    errorContainer: AppColors.dangerContainer,
    onErrorContainer: AppColors.dangerRed,
  );

  return ThemeData(
    useMaterial3: true,
    colorScheme: scheme,
    scaffoldBackgroundColor: AppColors.surfaceBg,
    textTheme: const TextTheme(
      bodyLarge: TextStyle(fontSize: 16, height: 1.5, letterSpacing: 0.5),
    ),
    inputDecorationTheme: InputDecorationTheme(
      border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
      contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
    ),
    cardTheme: CardThemeData(
      color: AppColors.surfaceCard,
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: const BorderSide(color: AppColors.borderLight),
      ),
    ),
    snackBarTheme: const SnackBarThemeData(behavior: SnackBarBehavior.floating),
  );
}
