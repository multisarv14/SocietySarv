package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.entity.UserRole
import com.example.data.localization.AppLanguage
import com.example.data.localization.AppStrings
import com.example.ui.components.SocietyTopAppBar
import com.example.ui.screens.AccountingScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CommunityScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GateSecurityScreen
import com.example.ui.screens.MaintenanceScreen
import com.example.ui.screens.ResidentPortalScreen
import com.example.ui.screens.ResidentsScreen
import com.example.ui.screens.SocietyProfileDialog
import com.example.ui.screens.SocietyProfileEditorScreen
import com.example.ui.screens.UserProfileDialog
import com.example.ui.theme.SocietySarvTheme
import com.example.ui.theme.TealPrimary
import com.example.viewmodel.NavDestination
import com.example.viewmodel.destinationsFor
import com.example.viewmodel.SocietyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SocietySarvTheme {
                SocietySarvApp()
            }
        }
    }
}

@Composable
fun SocietySarvApp(viewModel: SocietyViewModel = viewModel()) {
    val currentUser by viewModel.currentUser.collectAsState()
    val societyInfo by viewModel.societyInfo.collectAsState()
    val language by viewModel.language.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val financialOverview by viewModel.financialOverview.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val isRestoringSession by viewModel.isRestoringSession.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var isSocietyProfileOpen by remember { mutableStateOf(false) }
    var isSocietyEditorOpen by remember { mutableStateOf(false) }
    var isUserProfileOpen by remember { mutableStateOf(false) }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    if (isSocietyProfileOpen) {
        SocietyProfileDialog(
            society = societyInfo,
            currentUser = currentUser,
            language = language,
            onDismiss = { isSocietyProfileOpen = false },
            onEdit = { isSocietyProfileOpen = false; isSocietyEditorOpen = true }
        )
    }

    if (isUserProfileOpen) {
        UserProfileDialog(
            viewModel = viewModel,
            currentUser = currentUser,
            language = language,
            onDismiss = { isUserProfileOpen = false },
            onLogout = { viewModel.logout() }
        )
    }

    if (currentUser == null && isRestoringSession) {
        // A remembered member is being signed back in - show a spinner rather
        // than flashing the login form at somebody who never has to see it.
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = TealPrimary,
                modifier = Modifier.testTag("session_restore_spinner")
            )
        }
    } else if (currentUser == null) {
        AuthScreen(
            viewModel = viewModel,
            societyInfo = societyInfo,
            language = language,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Scaffold(
            topBar = {
                SocietyTopAppBar(
                    societyInfo = societyInfo,
                    currentUser = currentUser,
                    currentLanguage = language,
                    onToggleLanguage = { viewModel.toggleLanguage() },
                    onSelectLanguage = { viewModel.setLanguage(it) },
                    onOpenSocietyProfile = { isSocietyProfileOpen = true },
                    onOpenUserProfile = { isUserProfileOpen = true }
                )
            },
            bottomBar = {
                SocietyBottomNavigation(
                    selectedTab = selectedTab,
                    userRole = currentUser?.role ?: UserRole.RESIDENT,
                    language = language,
                    onSelectTab = { viewModel.selectTab(it) }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    /*
                     * The scaffold insets the content for the top and bottom
                     * bars, but not for the keyboard - so a form near the
                     * bottom of a tab (the gate check-in, the complaint box)
                     * had its fields typed into from behind the keyboard.
                     */
                    .imePadding()
            ) {
                when (selectedTab) {
                    NavDestination.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        financial = financialOverview,
                        society = societyInfo,
                        currentUser = currentUser,
                        language = language,
                        onNavigate = { viewModel.selectTab(it) }
                    )
                    NavDestination.ACCOUNTS -> AccountingScreen(
                        viewModel = viewModel,
                        financial = financialOverview,
                        society = societyInfo,
                        language = language
                    )
                    NavDestination.MAINTENANCE -> MaintenanceScreen(
                        viewModel = viewModel,
                        society = societyInfo,
                        language = language
                    )
                    NavDestination.RESIDENTS -> ResidentsScreen(
                        viewModel = viewModel,
                        society = societyInfo,
                        language = language
                    )
                    NavDestination.GATE -> GateSecurityScreen(
                        viewModel = viewModel,
                        currentUser = currentUser,
                        language = language
                    )
                    NavDestination.COMMUNITY -> CommunityScreen(
                        viewModel = viewModel,
                        currentUser = currentUser,
                        language = language
                    )
                    NavDestination.RESIDENT_PORTAL -> ResidentPortalScreen(
                        viewModel = viewModel,
                        currentUser = currentUser,
                        society = societyInfo,
                        language = language
                    )
                }
            }
        }
    }

    // The Society Profile editing wizard, drawn over everything else. It is a
    // screen rather than a dialog so its buttons honour the gesture bar, and it
    // is emitted last so it paints on top of the scaffold beneath it.
    val editableSociety = societyInfo
    if (isSocietyEditorOpen && editableSociety != null && currentUser != null) {
        SocietyProfileEditorScreen(
            viewModel = viewModel,
            society = editableSociety,
            language = language,
            onClose = { isSocietyEditorOpen = false }
        )
    }
}

@Composable
fun SocietyBottomNavigation(
    selectedTab: NavDestination,
    userRole: UserRole,
    language: AppLanguage,
    onSelectTab: (NavDestination) -> Unit
) {
    // The role decides the whole bar - Residents is the Chairman's, Security is
    // the security team's, and nobody sees a tab they cannot open.
    val destinations = destinationsFor(userRole)

    NavigationBar(tonalElevation = 6.dp) {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = selectedTab == destination,
                onClick = { onSelectTab(destination) },
                icon = { Icon(destination.icon(), contentDescription = destination.name) },
                label = {
                    Text(
                        text = AppStrings.get(destination.labelKey(), language),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                },
                modifier = Modifier.testTag(destination.testTag())
            )
        }
    }
}

private fun NavDestination.icon(): ImageVector = when (this) {
    NavDestination.DASHBOARD -> Icons.Default.Dashboard
    NavDestination.ACCOUNTS -> Icons.Default.AccountBalance
    NavDestination.MAINTENANCE -> Icons.Default.ReceiptLong
    NavDestination.RESIDENTS -> Icons.Default.Groups
    NavDestination.GATE -> Icons.Default.Security
    NavDestination.COMMUNITY -> Icons.Default.Forum
    NavDestination.RESIDENT_PORTAL -> Icons.Default.Home
}

private fun NavDestination.labelKey(): String = when (this) {
    NavDestination.DASHBOARD -> "nav_dashboard"
    NavDestination.ACCOUNTS -> "nav_accounts"
    NavDestination.MAINTENANCE -> "nav_maintenance"
    NavDestination.RESIDENTS -> "nav_residents"
    NavDestination.GATE -> "nav_gate"
    NavDestination.COMMUNITY -> "nav_community"
    NavDestination.RESIDENT_PORTAL -> "my_unit_portal"
}

private fun NavDestination.testTag(): String = when (this) {
    NavDestination.DASHBOARD -> "nav_dashboard"
    NavDestination.ACCOUNTS -> "nav_accounts"
    NavDestination.MAINTENANCE -> "nav_maintenance"
    NavDestination.RESIDENTS -> "nav_residents"
    NavDestination.GATE -> "nav_gate"
    NavDestination.COMMUNITY -> "nav_community"
    NavDestination.RESIDENT_PORTAL -> "nav_my_unit"
}
