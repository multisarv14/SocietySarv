package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.ui.screens.FileComplaintDialog
import com.example.ui.screens.GateSecurityScreen
import com.example.ui.screens.MaintenanceScreen
import com.example.ui.screens.ResidentPortalScreen
import com.example.ui.theme.SocietySarvTheme
import com.example.ui.theme.TealPrimary
import com.example.viewmodel.NavDestination
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

    val snackbarHostState = remember { SnackbarHostState() }
    var isHelpdeskDialogOpen by remember { mutableStateOf(false) }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // Resident Helpdesk Complaint Form Dialog (triggered via FAB)
    if (isHelpdeskDialogOpen) {
        FileComplaintDialog(
            currentUser = currentUser,
            language = language,
            onDismiss = { isHelpdeskDialogOpen = false },
            onSubmit = { cat, desc, flat, name ->
                viewModel.fileComplaint(cat, desc, flat, name)
                isHelpdeskDialogOpen = false
            }
        )
    }

    if (currentUser == null) {
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
                    onLogout = { viewModel.logout() },
                    onSwitchProfile = { viewModel.logout() }
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
            floatingActionButton = {
                // Floating Action Button (FAB) on main screen for resident helpdesk complaints
                if (selectedTab == NavDestination.DASHBOARD || selectedTab == NavDestination.COMMUNITY || selectedTab == NavDestination.RESIDENT_PORTAL) {
                    ExtendedFloatingActionButton(
                        onClick = { isHelpdeskDialogOpen = true },
                        containerColor = TealPrimary,
                        contentColor = Color.White,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = "Helpdesk",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        text = {
                            Text(
                                text = when (language) {
                                    AppLanguage.MARATHI -> "तक्रार दाखल करा"
                                    AppLanguage.HINDI -> "शिकायत दर्ज करें"
                                    AppLanguage.ENGLISH -> "Helpdesk Complaint"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.testTag("main_screen_helpdesk_fab")
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
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
}

@Composable
fun SocietyBottomNavigation(
    selectedTab: NavDestination,
    userRole: UserRole,
    language: AppLanguage,
    onSelectTab: (NavDestination) -> Unit
) {
    NavigationBar(
        tonalElevation = 6.dp
    ) {
        val isResidentOrTenant = userRole == UserRole.RESIDENT || userRole == UserRole.TENANT
        val isGuard = userRole == UserRole.GUARD

        if (isGuard) {
            NavigationBarItem(
                selected = selectedTab == NavDestination.GATE,
                onClick = { onSelectTab(NavDestination.GATE) },
                icon = { Icon(Icons.Default.Security, contentDescription = "Gate") },
                label = { Text(AppStrings.get("gate_security", language), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("nav_gate")
            )
            NavigationBarItem(
                selected = selectedTab == NavDestination.COMMUNITY,
                onClick = { onSelectTab(NavDestination.COMMUNITY) },
                icon = { Icon(Icons.Default.Forum, contentDescription = "Community") },
                label = { Text(AppStrings.get("community_forum", language), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("nav_community")
            )
            return@NavigationBar
        }

        NavigationBarItem(
            selected = selectedTab == NavDestination.DASHBOARD,
            onClick = { onSelectTab(NavDestination.DASHBOARD) },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
            label = { Text(AppStrings.get("nav_dashboard", language), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
            modifier = Modifier.testTag("nav_dashboard")
        )

        NavigationBarItem(
            selected = selectedTab == NavDestination.ACCOUNTS,
            onClick = { onSelectTab(NavDestination.ACCOUNTS) },
            icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Accounts") },
            label = { Text(AppStrings.get("nav_accounts", language), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
            modifier = Modifier.testTag("nav_accounts")
        )

        NavigationBarItem(
            selected = selectedTab == NavDestination.MAINTENANCE,
            onClick = { onSelectTab(NavDestination.MAINTENANCE) },
            icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Maintenance") },
            label = { Text(AppStrings.get("nav_maintenance", language), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
            modifier = Modifier.testTag("nav_maintenance")
        )

        NavigationBarItem(
            selected = selectedTab == NavDestination.GATE,
            onClick = { onSelectTab(NavDestination.GATE) },
            icon = { Icon(Icons.Default.Security, contentDescription = "Gate") },
            label = { Text(AppStrings.get("nav_gate", language), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
            modifier = Modifier.testTag("nav_gate")
        )

        NavigationBarItem(
            selected = selectedTab == NavDestination.COMMUNITY,
            onClick = { onSelectTab(NavDestination.COMMUNITY) },
            icon = { Icon(Icons.Default.Forum, contentDescription = "Community") },
            label = { Text(AppStrings.get("nav_community", language), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
            modifier = Modifier.testTag("nav_community")
        )

        if (isResidentOrTenant) {
            NavigationBarItem(
                selected = selectedTab == NavDestination.RESIDENT_PORTAL,
                onClick = { onSelectTab(NavDestination.RESIDENT_PORTAL) },
                icon = { Icon(Icons.Default.Home, contentDescription = "My Flat") },
                label = { Text(AppStrings.get("my_flat_portal", language), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("nav_my_flat")
            )
        }
    }
}
