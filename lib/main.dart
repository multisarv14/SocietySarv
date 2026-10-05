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
  runApp(
    ChangeNotifierProvider(
      create: (_) => SocietyViewModel(),
      child: const SocietySarvApp(),
    ),
  );
}

class SocietySarvApp extends StatelessWidget {
  const SocietySarvApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'SocietySarv',
      debugShowCheckedModeBanner: false,
      theme: buildAppTheme(),
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
      body = const Scaffold(
        body: Center(
          child: CircularProgressIndicator(key: Key('session_restore_spinner'), color: AppColors.tealPrimary),
        ),
      );
    } else if (currentUser == null) {
      _isSocietyEditorOpen = false;
      body = const AuthScreen();
    } else {
      final society = vm.societyInfo;
      body = Stack(
        children: [
          Scaffold(
            appBar: SocietyTopAppBar(
              societyInfo: society,
              currentUser: currentUser,
              currentLanguage: language,
              onToggleLanguage: vm.toggleLanguage,
              onSelectLanguage: vm.setLanguage,
              onOpenSocietyProfile: () => showSocietyProfileDialog(
                context,
                onEdit: () => setState(() => _isSocietyEditorOpen = true),
              ),
              onOpenUserProfile: () => showUserProfileDialog(context),
            ),
            bottomNavigationBar: SocietyBottomNavigation(
              selectedTab: vm.selectedTab,
              userRole: currentUser.role,
              language: language,
              onSelectTab: vm.selectTab,
            ),
            body: SafeArea(top: false, bottom: false, child: _tabBody(vm.selectedTab)),
          ),
          // The Society Profile editing wizard, drawn over everything else.
          if (_isSocietyEditorOpen && society != null)
            Positioned.fill(
              child: SocietyProfileEditorScreen(
                onClose: () => setState(() => _isSocietyEditorOpen = false),
              ),
            ),
        ],
      );
    }

    return ScaffoldMessenger(key: _messengerKey, child: body);
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
    if (destinations.length < 2) {
      // NavigationBar needs at least two destinations; a single-screen role
      // (housekeeping) just shows its one label.
      final d = destinations.isEmpty ? NavDestination.COMMUNITY : destinations.first;
      return NavigationBar(
        selectedIndex: 0,
        elevation: 6,
        destinations: [_item(d), _item(d, hidden: true)],
        onDestinationSelected: (_) => onSelectTab(d),
      );
    }
    return NavigationBar(
      selectedIndex: index < 0 ? 0 : index,
      elevation: 6,
      indicatorColor: index < 0 ? Colors.transparent : null,
      labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
      onDestinationSelected: (i) => onSelectTab(destinations[i]),
      destinations: [for (final d in destinations) _item(d)],
    );
  }

  Widget _item(NavDestination d, {bool hidden = false}) {
    if (hidden) {
      return const NavigationDestination(icon: SizedBox.shrink(), label: '', enabled: false);
    }
    return NavigationDestination(
      key: Key(_testTag(d)),
      icon: Icon(_icon(d)),
      label: AppStrings.get(_labelKey(d), language),
    );
  }

  static IconData _icon(NavDestination d) {
    switch (d) {
      case NavDestination.DASHBOARD:
        return Icons.dashboard;
      case NavDestination.ACCOUNTS:
        return Icons.account_balance;
      case NavDestination.MAINTENANCE:
        return Icons.receipt_long;
      case NavDestination.RESIDENTS:
        return Icons.groups;
      case NavDestination.GATE:
        return Icons.security;
      case NavDestination.COMMUNITY:
        return Icons.forum;
      case NavDestination.RESIDENT_PORTAL:
        return Icons.home;
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

  static String _testTag(NavDestination d) =>
      d == NavDestination.RESIDENT_PORTAL ? 'nav_my_unit' : _labelKey(d);
}
