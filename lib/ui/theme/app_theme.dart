import 'package:flutter/cupertino.dart' show CupertinoPageTransitionsBuilder;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'app_colors.dart';

/// Light Material 3 theme matching the Compose `SocietySarvTheme`, tuned for
/// a clean production look: soft rounded surfaces, filled inputs, pill tabs
/// and consistent motion for page transitions.
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
    surfaceContainerHigh: AppColors.surfaceVariant,
    surfaceContainer: AppColors.surfaceBg,
    surfaceContainerLow: AppColors.surfaceCard,
    surfaceContainerLowest: AppColors.surfaceCard,
    surfaceTint: Colors.transparent,
    onSurfaceVariant: AppColors.textSecondary,
    outline: AppColors.slateOutline,
    outlineVariant: AppColors.borderLight,
    error: AppColors.dangerRed,
    onError: AppColors.onPrimary,
    errorContainer: AppColors.dangerContainer,
    onErrorContainer: AppColors.dangerRed,
    shadow: Color(0xFF0F172A),
  );

  final base = ThemeData(useMaterial3: true, colorScheme: scheme);
  final text = base.textTheme
      .copyWith(
        headlineLarge: base.textTheme.headlineLarge?.copyWith(fontWeight: FontWeight.w800, letterSpacing: -0.5),
        headlineMedium: base.textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w800, letterSpacing: -0.4),
        headlineSmall: base.textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w700, letterSpacing: -0.3),
        titleLarge: base.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700, letterSpacing: -0.2),
        titleMedium: base.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600),
        titleSmall: base.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600),
        bodyLarge: const TextStyle(fontSize: 16, height: 1.5, letterSpacing: 0.2),
        bodyMedium: base.textTheme.bodyMedium?.copyWith(height: 1.4),
        labelLarge: base.textTheme.labelLarge?.copyWith(fontWeight: FontWeight.w600, letterSpacing: 0.2),
      )
      .apply(bodyColor: AppColors.textPrimary, displayColor: AppColors.textPrimary);

  final radius12 = BorderRadius.circular(12);
  final radius14 = BorderRadius.circular(14);
  OutlineInputBorder inputBorder(Color color, [double width = 1]) => OutlineInputBorder(
    borderRadius: radius14,
    borderSide: BorderSide(color: color, width: width),
  );

  const buttonPadding = EdgeInsets.symmetric(horizontal: 20, vertical: 12);
  const buttonText = TextStyle(fontWeight: FontWeight.w700, fontSize: 14.5, letterSpacing: 0.2);

  return base.copyWith(
    scaffoldBackgroundColor: AppColors.surfaceBg,
    textTheme: text,
    splashFactory: InkSparkle.splashFactory,
    visualDensity: VisualDensity.standard,
    pageTransitionsTheme: const PageTransitionsTheme(
      builders: {
        TargetPlatform.android: FadeForwardsPageTransitionsBuilder(),
        TargetPlatform.iOS: CupertinoPageTransitionsBuilder(),
        TargetPlatform.macOS: CupertinoPageTransitionsBuilder(),
      },
    ),
    appBarTheme: const AppBarTheme(
      backgroundColor: AppColors.tealPrimary,
      foregroundColor: Colors.white,
      elevation: 0,
      scrolledUnderElevation: 0,
      centerTitle: false,
      systemOverlayStyle: SystemUiOverlayStyle.light,
    ),
    cardTheme: CardThemeData(
      color: AppColors.surfaceCard,
      surfaceTintColor: Colors.transparent,
      shadowColor: const Color(0x260F172A),
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(18),
        side: const BorderSide(color: AppColors.borderLight),
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: AppColors.surfaceBg,
      border: inputBorder(AppColors.borderLight),
      enabledBorder: inputBorder(AppColors.borderLight),
      focusedBorder: inputBorder(AppColors.tealPrimary, 1.8),
      errorBorder: inputBorder(AppColors.dangerRed),
      focusedErrorBorder: inputBorder(AppColors.dangerRed, 1.8),
      disabledBorder: inputBorder(AppColors.surfaceVariant),
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
      labelStyle: const TextStyle(color: AppColors.textSecondary),
      floatingLabelStyle: WidgetStateTextStyle.resolveWith(
        (states) => TextStyle(
          color: states.contains(WidgetState.error)
              ? AppColors.dangerRed
              : states.contains(WidgetState.focused)
              ? AppColors.tealPrimary
              : AppColors.textSecondary,
          fontWeight: FontWeight.w600,
        ),
      ),
      hintStyle: const TextStyle(color: AppColors.textMuted),
      prefixIconColor: WidgetStateColor.resolveWith(
        (states) => states.contains(WidgetState.focused) ? AppColors.tealPrimary : AppColors.textSecondary,
      ),
      suffixIconColor: AppColors.textSecondary,
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        backgroundColor: AppColors.tealPrimary,
        foregroundColor: Colors.white,
        disabledBackgroundColor: AppColors.surfaceVariant,
        minimumSize: const Size(64, 46),
        padding: buttonPadding,
        shape: RoundedRectangleBorder(borderRadius: radius14),
        textStyle: buttonText,
        elevation: 0,
      ),
    ),
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: AppColors.tealPrimary,
        foregroundColor: Colors.white,
        minimumSize: const Size(64, 46),
        padding: buttonPadding,
        shape: RoundedRectangleBorder(borderRadius: radius14),
        textStyle: buttonText,
        elevation: 1,
        shadowColor: const Color(0x4000695C),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        foregroundColor: AppColors.tealPrimary,
        minimumSize: const Size(64, 46),
        padding: buttonPadding,
        shape: RoundedRectangleBorder(borderRadius: radius14),
        side: const BorderSide(color: AppColors.slateOutline),
        textStyle: buttonText,
      ),
    ),
    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(
        foregroundColor: AppColors.tealPrimary,
        shape: RoundedRectangleBorder(borderRadius: radius12),
        textStyle: buttonText,
      ),
    ),
    iconButtonTheme: IconButtonThemeData(
      style: IconButton.styleFrom(shape: RoundedRectangleBorder(borderRadius: radius12)),
    ),
    floatingActionButtonTheme: FloatingActionButtonThemeData(
      backgroundColor: AppColors.tealPrimary,
      foregroundColor: Colors.white,
      elevation: 3,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
    ),
    dialogTheme: DialogThemeData(
      backgroundColor: AppColors.surfaceCard,
      surfaceTintColor: Colors.transparent,
      elevation: 12,
      shadowColor: const Color(0x400F172A),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      titleTextStyle: text.titleLarge?.copyWith(color: AppColors.textPrimary),
      contentTextStyle: text.bodyMedium?.copyWith(color: AppColors.textSecondary),
    ),
    bottomSheetTheme: const BottomSheetThemeData(
      backgroundColor: AppColors.surfaceCard,
      surfaceTintColor: Colors.transparent,
      showDragHandle: true,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(28))),
    ),
    popupMenuTheme: PopupMenuThemeData(
      color: AppColors.surfaceCard,
      surfaceTintColor: Colors.transparent,
      elevation: 8,
      shadowColor: const Color(0x330F172A),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
    ),
    menuTheme: MenuThemeData(
      style: MenuStyle(
        backgroundColor: const WidgetStatePropertyAll(AppColors.surfaceCard),
        surfaceTintColor: const WidgetStatePropertyAll(Colors.transparent),
        shape: WidgetStatePropertyAll(RoundedRectangleBorder(borderRadius: radius14)),
        elevation: const WidgetStatePropertyAll(8),
      ),
    ),
    navigationBarTheme: NavigationBarThemeData(
      backgroundColor: AppColors.surfaceCard,
      surfaceTintColor: Colors.transparent,
      elevation: 0,
      height: 70,
      indicatorColor: AppColors.tealContainer,
      indicatorShape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      labelTextStyle: WidgetStateProperty.resolveWith(
        (states) => TextStyle(
          fontSize: 11.5,
          fontWeight: states.contains(WidgetState.selected) ? FontWeight.w700 : FontWeight.w500,
          color: states.contains(WidgetState.selected) ? AppColors.tealPrimary : AppColors.textSecondary,
        ),
      ),
      iconTheme: WidgetStateProperty.resolveWith(
        (states) => IconThemeData(
          size: 24,
          color: states.contains(WidgetState.selected) ? AppColors.tealPrimary : AppColors.textSecondary,
        ),
      ),
    ),
    tabBarTheme: const TabBarThemeData(
      indicator: _PillTabIndicator(color: AppColors.tealContainer),
      indicatorSize: TabBarIndicatorSize.tab,
      dividerColor: AppColors.borderLight,
      labelColor: AppColors.tealPrimary,
      unselectedLabelColor: AppColors.textSecondary,
      labelPadding: EdgeInsets.symmetric(horizontal: 18),
      splashBorderRadius: BorderRadius.all(Radius.circular(12)),
      overlayColor: WidgetStatePropertyAll(Color(0x1400695C)),
    ),
    chipTheme: ChipThemeData(
      backgroundColor: AppColors.surfaceCard,
      selectedColor: AppColors.tealContainer,
      side: const BorderSide(color: AppColors.borderLight),
      shape: RoundedRectangleBorder(borderRadius: radius12),
      labelStyle: const TextStyle(fontWeight: FontWeight.w600, color: AppColors.textPrimary),
      checkmarkColor: AppColors.tealPrimary,
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 4),
    ),
    segmentedButtonTheme: SegmentedButtonThemeData(
      style: SegmentedButton.styleFrom(
        selectedBackgroundColor: AppColors.tealContainer,
        selectedForegroundColor: AppColors.tealDark,
        side: const BorderSide(color: AppColors.borderLight),
        shape: RoundedRectangleBorder(borderRadius: radius12),
      ),
    ),
    checkboxTheme: CheckboxThemeData(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(5)),
      side: const BorderSide(color: AppColors.slateOutline, width: 1.6),
    ),
    switchTheme: SwitchThemeData(
      thumbColor: WidgetStateProperty.resolveWith(
        (s) => s.contains(WidgetState.selected) ? Colors.white : AppColors.slateOutline,
      ),
      trackColor: WidgetStateProperty.resolveWith(
        (s) => s.contains(WidgetState.selected) ? AppColors.tealPrimary : AppColors.surfaceVariant,
      ),
      trackOutlineColor: WidgetStateProperty.resolveWith(
        (s) => s.contains(WidgetState.selected) ? Colors.transparent : AppColors.slateOutline,
      ),
    ),
    radioTheme: const RadioThemeData(fillColor: WidgetStatePropertyAll(AppColors.tealPrimary)),
    progressIndicatorTheme: const ProgressIndicatorThemeData(
      color: AppColors.tealPrimary,
      linearTrackColor: AppColors.surfaceVariant,
      circularTrackColor: AppColors.tealContainer,
    ),
    listTileTheme: ListTileThemeData(
      shape: RoundedRectangleBorder(borderRadius: radius12),
      iconColor: AppColors.textSecondary,
      titleTextStyle: text.titleSmall?.copyWith(color: AppColors.textPrimary),
      subtitleTextStyle: text.bodySmall?.copyWith(color: AppColors.textSecondary),
    ),
    dividerTheme: const DividerThemeData(color: AppColors.borderLight, thickness: 1, space: 1),
    snackBarTheme: SnackBarThemeData(
      behavior: SnackBarBehavior.floating,
      backgroundColor: AppColors.textPrimary,
      contentTextStyle: const TextStyle(color: Colors.white, fontWeight: FontWeight.w500),
      actionTextColor: AppColors.mintSecondaryContainer,
      elevation: 6,
      insetPadding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
      shape: RoundedRectangleBorder(borderRadius: radius14),
    ),
    tooltipTheme: TooltipThemeData(
      decoration: BoxDecoration(color: AppColors.textPrimary, borderRadius: BorderRadius.circular(8)),
      textStyle: const TextStyle(color: Colors.white, fontSize: 12),
    ),
    datePickerTheme: DatePickerThemeData(
      backgroundColor: AppColors.surfaceCard,
      surfaceTintColor: Colors.transparent,
      headerBackgroundColor: AppColors.tealPrimary,
      headerForegroundColor: Colors.white,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
    ),
    timePickerTheme: TimePickerThemeData(
      backgroundColor: AppColors.surfaceCard,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
    ),
    expansionTileTheme: ExpansionTileThemeData(
      shape: RoundedRectangleBorder(borderRadius: radius14),
      collapsedShape: RoundedRectangleBorder(borderRadius: radius14),
      iconColor: AppColors.tealPrimary,
    ),
  );
}

/// Rounded "pill" behind the selected tab, inset from the tab's edges.
class _PillTabIndicator extends Decoration {
  const _PillTabIndicator({required this.color});

  final Color color;

  @override
  BoxPainter createBoxPainter([VoidCallback? onChanged]) => _PillPainter(color);
}

class _PillPainter extends BoxPainter {
  _PillPainter(this.color);

  final Color color;

  @override
  void paint(Canvas canvas, Offset offset, ImageConfiguration configuration) {
    final size = configuration.size;
    if (size == null) return;
    final rect = offset & size;
    final pill = Rect.fromLTRB(rect.left + 4, rect.top + 7, rect.right - 4, rect.bottom - 7);
    canvas.drawRRect(RRect.fromRectAndRadius(pill, const Radius.circular(12)), Paint()..color = color);
  }
}
