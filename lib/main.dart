import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:provider/provider.dart';

import 'data/models.dart';
import 'l10n/app_strings.dart';
import 'state/society_view_model.dart';
import 'ui/components/common_components.dart';
import 'ui/screens/accounting_screen.dart';
import 'ui/screens/auth_screen.dart';
import 'ui/screens/community_screen.dart';
import 'ui/screens/dashboard_screen.dart';
import 'ui/screens/gate_security_screen.dart';
import 'ui/screens/maintenance_screen.dart';
import 'ui/screens/profile_screens.dart';
import 'ui/screens/resident_portal_screen.dart';
import 'ui/screens/residents_screen.dart';
import 'ui/theme/app_colors.dart';
import 'ui/theme/app_theme.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  // On Android the options come from android/app/google-services.json.
  await Firebase.initializeApp();
  await initializeDateFormatting('en_US');
  SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
  runApp(ChangeNotifierProvider(create: (_) => SocietyViewModel(), child: const SocietySarvApp()));
}

class SocietySarvApp extends StatelessWidget {
  const SocietySarvApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'SocietySarv',
      debugShowCheckedModeBanner: false,
      theme: buildAppTheme(),
      // Every list can be pulled down to refresh, even when it is short.
      scrollBehavior: const _AppScrollBehavior(),
      home: const AppShell(),
    );
  }
}

/// The root of the signed-in / signed-out app, port of `SocietySarvApp()` in
/// MainActivity.kt.
class AppShell extends StatefulWidget {
  const AppShell({super.key});

  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> {
  final _messengerKey = GlobalKey<ScaffoldMessengerState>();
  bool _isSocietyEditorOpen = false;

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final currentUser = vm.currentUser;
    final language = vm.language;

    // Surface one-shot user messages as a snackbar, then clear them.
    final message = vm.userMessage;
    if (message != null) {
      vm.clearUserMessage();
      WidgetsBinding.instance.addPostFrameCallback((_) {
        _messengerKey.currentState
          ?..hideCurrentSnackBar()
          ..showSnackBar(SnackBar(content: Text(message)));
      });
    }

    final Widget body;
    if (currentUser == null && vm.isRestoringSession) {
      // A remembered member is being signed back in - show a spinner rather
      // than flashing the login form at somebody who never has to see it.
      body = const _SplashScreen(key: ValueKey('splash'));
    } else if (currentUser == null) {
      _isSocietyEditorOpen = false;
      body = const AuthScreen(key: ValueKey('auth'));
    } else {
      final society = vm.societyInfo;
      body = Stack(
        key: const ValueKey('shell'),
        children: [
          Scaffold(
            appBar: SocietyTopAppBar(
              societyInfo: society,
              currentUser: currentUser,
              currentLanguage: language,
              onToggleLanguage: vm.toggleLanguage,
              onSelectLanguage: vm.setLanguage,
              onOpenSocietyProfile: () =>
                  showSocietyProfileDialog(context, onEdit: () => setState(() => _isSocietyEditorOpen = true)),
              onOpenUserProfile: () => showUserProfileDialog(context),
            ),
            bottomNavigationBar: SocietyBottomNavigation(
              selectedTab: vm.selectedTab,
              userRole: currentUser.role,
              language: language,
              onSelectTab: vm.selectTab,
            ),
            body: SafeArea(
              top: false,
              bottom: false,
              child: Column(
                children: [
                  const BusyProgressBar(),
                  Expanded(
                    child: RefreshIndicator(
                      key: const Key('pull_to_refresh'),
                      color: AppColors.tealPrimary,
                      backgroundColor: AppColors.surfaceCard,
                      strokeWidth: 2.6,
                      displacement: 24,
                      onRefresh: vm.refresh,
                      child: FadeThroughSwitcher(
                        child: vm.isInitialLoading
                            ? const SkeletonPage(key: ValueKey('skeleton'))
                            : KeyedSubtree(key: ValueKey(vm.selectedTab), child: _tabBody(vm.selectedTab)),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
          // The Society Profile editing wizard, drawn over everything else.
          Positioned.fill(
            child: IgnorePointer(
              ignoring: !(_isSocietyEditorOpen && society != null),
              child: AnimatedSwitcher(
                duration: AppMotion.medium,
                switchInCurve: AppMotion.emphasized,
                switchOutCurve: Curves.easeIn,
                transitionBuilder: (child, animation) => FadeTransition(
                  opacity: animation,
                  child: SlideTransition(
                    position: Tween<Offset>(begin: const Offset(0, 0.06), end: Offset.zero).animate(animation),
                    child: child,
                  ),
                ),
                child: _isSocietyEditorOpen && society != null
                    ? SocietyProfileEditorScreen(
                        key: const ValueKey('society_editor'),
                        onClose: () => setState(() => _isSocietyEditorOpen = false),
                      )
                    : const SizedBox.shrink(),
              ),
            ),
          ),
        ],
      );
    }

    return ScaffoldMessenger(
      key: _messengerKey,
      child: AnimatedSwitcher(
        duration: AppMotion.slow,
        switchInCurve: AppMotion.emphasized,
        switchOutCurve: Curves.easeIn,
        transitionBuilder: (child, animation) => FadeTransition(
          opacity: animation,
          child: ScaleTransition(scale: Tween<double>(begin: 0.98, end: 1).animate(animation), child: child),
        ),
        child: body,
      ),
    );
  }

  Widget _tabBody(NavDestination tab) {
    switch (tab) {
      case NavDestination.DASHBOARD:
        return const DashboardScreen();
      case NavDestination.ACCOUNTS:
        return const AccountingScreen();
      case NavDestination.MAINTENANCE:
        return const MaintenanceScreen();
      case NavDestination.RESIDENTS:
        return const ResidentsScreen();
      case NavDestination.GATE:
        return const GateSecurityScreen();
      case NavDestination.COMMUNITY:
        return const CommunityScreen();
      case NavDestination.RESIDENT_PORTAL:
        return const ResidentPortalScreen();
    }
  }
}

/// The role decides the whole bar - Residents is the Chairman's, Security is
/// the security team's, and nobody sees a tab they cannot open.
class SocietyBottomNavigation extends StatelessWidget {
  const SocietyBottomNavigation({
    super.key,
    required this.selectedTab,
    required this.userRole,
    required this.language,
    required this.onSelectTab,
  });

  final NavDestination selectedTab;
  final UserRole userRole;
  final AppLanguage language;
  final ValueChanged<NavDestination> onSelectTab;

  @override
  Widget build(BuildContext context) {
    final destinations = destinationsFor(userRole);
    // A tab the role cannot open (e.g. Gate reached from the dashboard) still
    // renders its screen, but no bar item is highlighted for it.
    final index = destinations.indexOf(selectedTab);
    final Widget bar;
    if (destinations.length < 2) {
      // NavigationBar needs at least two destinations; a single-screen role
      // (housekeeping) just shows its one label.
      final d = destinations.isEmpty ? NavDestination.COMMUNITY : destinations.first;
      bar = NavigationBar(
        selectedIndex: 0,
        destinations: [_item(d), _item(d, hidden: true)],
        onDestinationSelected: (_) => onSelectTab(d),
      );
    } else {
      bar = NavigationBar(
        selectedIndex: index < 0 ? 0 : index,
        indicatorColor: index < 0 ? Colors.transparent : null,
        labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
        animationDuration: const Duration(milliseconds: 450),
        onDestinationSelected: (i) => onSelectTab(destinations[i]),
        destinations: [for (final d in destinations) _item(d)],
      );
    }
    // A soft shadow lifts the bar off the page instead of a hard divider.
    return DecoratedBox(
      decoration: const BoxDecoration(
        color: AppColors.surfaceCard,
        border: Border(top: BorderSide(color: AppColors.borderLight)),
        boxShadow: [BoxShadow(color: Color(0x140F172A), blurRadius: 16, offset: Offset(0, -4))],
      ),
      child: bar,
    );
  }

  Widget _item(NavDestination d, {bool hidden = false}) {
    if (hidden) {
      return const NavigationDestination(icon: SizedBox.shrink(), label: '', enabled: false);
    }
    final (outlined, filled) = _icons(d);
    return NavigationDestination(
      key: Key(_testTag(d)),
      icon: Icon(outlined),
      selectedIcon: _BouncyIcon(icon: filled),
      label: AppStrings.get(_labelKey(d), language),
    );
  }

  static (IconData, IconData) _icons(NavDestination d) {
    switch (d) {
      case NavDestination.DASHBOARD:
        return (Icons.space_dashboard_outlined, Icons.space_dashboard_rounded);
      case NavDestination.ACCOUNTS:
        return (Icons.account_balance_outlined, Icons.account_balance_rounded);
      case NavDestination.MAINTENANCE:
        return (Icons.receipt_long_outlined, Icons.receipt_long_rounded);
      case NavDestination.RESIDENTS:
        return (Icons.groups_outlined, Icons.groups_rounded);
      case NavDestination.GATE:
        return (Icons.shield_outlined, Icons.shield_rounded);
      case NavDestination.COMMUNITY:
        return (Icons.forum_outlined, Icons.forum_rounded);
      case NavDestination.RESIDENT_PORTAL:
        return (Icons.home_outlined, Icons.home_rounded);
    }
  }

  static String _labelKey(NavDestination d) {
    switch (d) {
      case NavDestination.DASHBOARD:
        return 'nav_dashboard';
      case NavDestination.ACCOUNTS:
        return 'nav_accounts';
      case NavDestination.MAINTENANCE:
        return 'nav_maintenance';
      case NavDestination.RESIDENTS:
        return 'nav_residents';
      case NavDestination.GATE:
        return 'nav_gate';
      case NavDestination.COMMUNITY:
        return 'nav_community';
      case NavDestination.RESIDENT_PORTAL:
        return 'my_unit_portal';
    }
  }

  static String _testTag(NavDestination d) => d == NavDestination.RESIDENT_PORTAL ? 'nav_my_unit' : _labelKey(d);
}

/// The selected bottom-bar icon pops in with a small spring.
class _BouncyIcon extends StatelessWidget {
  const _BouncyIcon({required this.icon});

  final IconData icon;

  @override
  Widget build(BuildContext context) {
    return TweenAnimationBuilder<double>(
      tween: Tween<double>(begin: 0.7, end: 1),
      duration: const Duration(milliseconds: 420),
      curve: Curves.elasticOut,
      builder: (_, scale, child) => Transform.scale(scale: scale, child: child),
      child: Icon(icon),
    );
  }
}

/// Branded splash shown while a remembered member is signed back in.
class _SplashScreen extends StatefulWidget {
  const _SplashScreen({super.key});

  @override
  State<_SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<_SplashScreen> with SingleTickerProviderStateMixin {
  late final AnimationController _pulse = AnimationController(vsync: this, duration: const Duration(milliseconds: 1400))
    ..repeat(reverse: true);

  @override
  void dispose() {
    _pulse.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.light,
      child: Scaffold(
        body: Container(
          decoration: const BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [AppColors.tealDark, AppColors.tealPrimary, Color(0xFF26A69A)],
            ),
          ),
          alignment: Alignment.center,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              ScaleTransition(
                scale: Tween<double>(
                  begin: 0.94,
                  end: 1.04,
                ).animate(CurvedAnimation(parent: _pulse, curve: Curves.easeInOut)),
                child: Container(
                  width: 96,
                  height: 96,
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(28),
                    boxShadow: const [BoxShadow(color: Color(0x33000000), blurRadius: 24, offset: Offset(0, 10))],
                  ),
                  child: const Icon(Icons.apartment_rounded, color: AppColors.tealPrimary, size: 52),
                ),
              ),
              const SizedBox(height: 24),
              Text(
                'SocietySarv',
                style: Theme.of(
                  context,
                ).textTheme.headlineSmall?.copyWith(color: Colors.white, fontWeight: FontWeight.w800),
              ),
              const SizedBox(height: 28),
              const SizedBox(
                width: 28,
                height: 28,
                child: CircularProgressIndicator(
                  key: Key('session_restore_spinner'),
                  strokeWidth: 3,
                  color: Colors.white,
                  backgroundColor: Color(0x33FFFFFF),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _AppScrollBehavior extends MaterialScrollBehavior {
  const _AppScrollBehavior();

  @override
  ScrollPhysics getScrollPhysics(BuildContext context) =>
      AlwaysScrollableScrollPhysics(parent: super.getScrollPhysics(context));
}
