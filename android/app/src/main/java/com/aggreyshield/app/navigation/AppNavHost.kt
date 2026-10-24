package com.aggreyshield.app.navigation

import androidx.navigation.navDeepLink
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aggreyshield.app.core.common.OneShotEvent
import com.aggreyshield.app.core.security.SessionState
import com.aggreyshield.app.core.ui.components.ObserveEvents
import com.aggreyshield.app.core.ui.components.OfflineBanner
import com.aggreyshield.app.presentation.about.AboutScreen
import com.aggreyshield.app.presentation.alerts.AlertsScreen
import com.aggreyshield.app.presentation.alerts.AlertsViewModel
import com.aggreyshield.app.presentation.auth.ForgotPasswordScreen
import com.aggreyshield.app.presentation.auth.ForgotPasswordViewModel
import com.aggreyshield.app.presentation.auth.LoginScreen
import com.aggreyshield.app.presentation.auth.LoginViewModel
import com.aggreyshield.app.presentation.auth.RegisterScreen
import com.aggreyshield.app.presentation.auth.RegisterViewModel
import com.aggreyshield.app.presentation.auth.ResetPasswordScreen
import com.aggreyshield.app.presentation.auth.ResetPasswordViewModel
import com.aggreyshield.app.presentation.auth.VerifyEmailScreen
import com.aggreyshield.app.presentation.auth.VerifyEmailViewModel
import com.aggreyshield.app.presentation.dashboard.DashboardScreen
import com.aggreyshield.app.presentation.dashboard.DashboardViewModel
import com.aggreyshield.app.presentation.dashboard.SecurityScoreScreen
import com.aggreyshield.app.presentation.dashboard.SecurityScoreViewModel
import com.aggreyshield.app.presentation.incidents.IncidentDetailScreen
import com.aggreyshield.app.presentation.incidents.IncidentDetailViewModel
import com.aggreyshield.app.presentation.incidents.IncidentListScreen
import com.aggreyshield.app.presentation.incidents.IncidentListViewModel
import com.aggreyshield.app.presentation.incidents.IncidentReportScreen
import com.aggreyshield.app.presentation.incidents.IncidentReportViewModel
import com.aggreyshield.app.presentation.logout.LogoutConfirmationScreen
import com.aggreyshield.app.presentation.logout.LogoutViewModel
import com.aggreyshield.app.presentation.notifications.NotificationsScreen
import com.aggreyshield.app.presentation.notifications.NotificationsViewModel
import com.aggreyshield.app.presentation.onboarding.OnboardingScreen
import com.aggreyshield.app.presentation.onboarding.OnboardingViewModel
import com.aggreyshield.app.presentation.organization.OrganizationMembersScreen
import com.aggreyshield.app.presentation.organization.OrganizationMembersViewModel
import com.aggreyshield.app.presentation.organization.OrganizationScreen
import com.aggreyshield.app.presentation.organization.OrganizationViewModel
import com.aggreyshield.app.presentation.profile.AccountSecurityScreen
import com.aggreyshield.app.presentation.profile.AccountSecurityViewModel
import com.aggreyshield.app.presentation.profile.ChangePasswordScreen
import com.aggreyshield.app.presentation.profile.ChangePasswordViewModel
import com.aggreyshield.app.presentation.profile.MfaSetupScreen
import com.aggreyshield.app.presentation.profile.MfaSetupViewModel
import com.aggreyshield.app.presentation.profile.ProfileScreen
import com.aggreyshield.app.presentation.profile.ProfileViewModel
import com.aggreyshield.app.presentation.scanner.MessageAnalyzerScreen
import com.aggreyshield.app.presentation.scanner.MessageAnalyzerViewModel
import com.aggreyshield.app.presentation.scanner.ScannerHubScreen
import com.aggreyshield.app.presentation.scanner.ScannerHubViewModel
import com.aggreyshield.app.presentation.scanner.UrlScannerScreen
import com.aggreyshield.app.presentation.scanner.UrlScannerViewModel
import com.aggreyshield.app.presentation.settings.SettingsScreen
import com.aggreyshield.app.presentation.settings.SettingsViewModel
import com.aggreyshield.app.presentation.splash.SplashScreen
import com.aggreyshield.app.presentation.splash.SplashViewModel
import com.aggreyshield.app.presentation.threatintel.ThreatIntelScreen
import com.aggreyshield.app.presentation.threatintel.ThreatIntelViewModel
import com.aggreyshield.app.presentation.training.CourseDetailScreen
import com.aggreyshield.app.presentation.training.CourseDetailViewModel
import com.aggreyshield.app.presentation.training.TrainingListScreen
import com.aggreyshield.app.presentation.training.TrainingListViewModel
import com.aggreyshield.app.presentation.training.TrainingResultsScreen
import com.aggreyshield.app.presentation.training.TrainingResultsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Routes a one-shot [OneShotEvent] into a real navigation action or a snackbar message. */
private fun handleEvent(
    event: OneShotEvent,
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
) {
    when (event) {
        is OneShotEvent.Navigate -> navController.navigate(event.route) {
            if (event.popUpToRoute != null) {
                popUpTo(event.popUpToRoute) { inclusive = event.inclusive }
            }
            launchSingleTop = true
        }
        OneShotEvent.NavigateBack -> navController.popBackStack()
        is OneShotEvent.ShowMessage -> scope.launch { snackbarHostState.showSnackbar(event.message) }
    }
}

/**
 * The whole app: session-aware navigation, the persistent offline banner, and the bottom
 * navigation bar shown only on the five primary authenticated sections.
 */
@Composable
fun AggreyShieldApp() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val rootViewModel: RootViewModel = hiltViewModel()
    val sessionState by rootViewModel.sessionState.collectAsStateWithLifecycle()
    val isOnline by rootViewModel.isOnline.collectAsStateWithLifecycle()

    var previousSessionState by remember { androidx.compose.runtime.mutableStateOf(SessionState.UNKNOWN) }
    androidx.compose.runtime.LaunchedEffect(sessionState) {
        val previous = previousSessionState
        if (previous == SessionState.AUTHENTICATED && sessionState == SessionState.UNAUTHENTICATED) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        } else if (
            previous != SessionState.UNKNOWN &&
            previous == SessionState.UNAUTHENTICATED &&
            sessionState == SessionState.AUTHENTICATED
        ) {
            navController.navigate(Routes.DASHBOARD) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
        previousSessionState = sessionState
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = BOTTOM_NAV_ITEMS.any { it.route == currentRoute }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BOTTOM_NAV_ITEMS.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { androidx.compose.material3.Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(padding)) {
            OfflineBanner(visible = !isOnline)

            NavHost(navController = navController, startDestination = Routes.SPLASH) {

                composable(Routes.SPLASH) {
                    val vm: SplashViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    SplashScreen(viewModel = vm)
                }

                composable(Routes.ONBOARDING) {
                    val vm: OnboardingViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    OnboardingScreen(viewModel = vm)
                }

                composable(Routes.LOGIN) {
                    val vm: LoginViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    LoginScreen(
                        viewModel = vm,
                        onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                        onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
                    )
                }

                composable(Routes.REGISTER) {
                    val vm: RegisterViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    RegisterScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }

                composable(
                    Routes.VERIFY_EMAIL_PATTERN,
                    arguments = listOf(navArgument(Routes.ARG_EMAIL) { type = NavType.StringType }),
                ) {
                    val vm: VerifyEmailViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    VerifyEmailScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }

                composable(Routes.FORGOT_PASSWORD) {
                    val vm: ForgotPasswordViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    ForgotPasswordScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }

                composable(
                    Routes.RESET_PASSWORD_PATTERN,
                    arguments = listOf(
                        navArgument(Routes.ARG_RESET_TOKEN) {
                            type = NavType.StringType
                        }
                    ),
                    deepLinks = listOf(
                        navDeepLink {
                            uriPattern = "aggreyshield://reset-password?token={token}"
                        }
                    ),

                ) {
                    val vm: ResetPasswordViewModel = hiltViewModel()

                    ObserveEvents(vm.events) {
                        handleEvent(
                            it,
                            navController,
                            snackbarHostState,
                            scope,
                        )
                    }

                    ResetPasswordScreen(
                        viewModel = vm,
                        onBack = {
                            navController.popBackStack()
                        },
                    )
                }

                composable(Routes.DASHBOARD) {
                    val vm: DashboardViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    DashboardScreen(viewModel = vm)
                }

                composable(Routes.SECURITY_SCORE) {
                    val vm: SecurityScoreViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    SecurityScoreScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }

                composable(Routes.SCANNER_HUB) {
                    val vm: ScannerHubViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    ScannerHubScreen(viewModel = vm)
                }

                composable(Routes.URL_SCANNER) {
                    val vm: UrlScannerViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    UrlScannerScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }

                composable(Routes.MESSAGE_ANALYZER) {
                    val vm: MessageAnalyzerViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    MessageAnalyzerScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }

                composable(Routes.INCIDENT_LIST) {
                    val vm: IncidentListViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    IncidentListScreen(viewModel = vm)
                }

                composable(Routes.INCIDENT_REPORT) {
                    val vm: IncidentReportViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    IncidentReportScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }

                composable(
                    Routes.INCIDENT_DETAIL_PATTERN,
                    arguments = listOf(navArgument(Routes.ARG_INCIDENT_ID) { type = NavType.StringType }),
                ) {
                    val vm: IncidentDetailViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    IncidentDetailScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }

                composable(Routes.ALERTS) {
                    val vm: AlertsViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    AlertsScreen(viewModel = vm)
                }

                composable(Routes.THREAT_INTEL) {
                    val vm: ThreatIntelViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    ThreatIntelScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }

                composable(Routes.TRAINING_LIST) {
                    val vm: TrainingListViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    TrainingListScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }

                composable(
                    Routes.COURSE_DETAIL_PATTERN,
                    arguments = listOf(navArgument(Routes.ARG_COURSE_ID) { type = NavType.StringType }),
                ) {
                    val vm: CourseDetailViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    CourseDetailScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }

                composable(Routes.TRAINING_RESULTS) {
                    val vm: TrainingResultsViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    TrainingResultsScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }

                composable(Routes.NOTIFICATIONS) {
                    val vm: NotificationsViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    NotificationsScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }

                composable(Routes.PROFILE) {
                    val vm: ProfileViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    ProfileScreen(viewModel = vm)
                }

                composable(Routes.ACCOUNT_SECURITY) {
                    val vm: AccountSecurityViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    AccountSecurityScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }

                composable(Routes.CHANGE_PASSWORD) {
                    val vm: ChangePasswordViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    ChangePasswordScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }

                composable(Routes.MFA_SETUP) {
                    val vm: MfaSetupViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    MfaSetupScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }

                composable(Routes.ORGANIZATION) {
                    val vm: OrganizationViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    OrganizationScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }

                composable(Routes.ORGANIZATION_MEMBERS) {
                    val vm: OrganizationMembersViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    OrganizationMembersScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }

                composable(Routes.SETTINGS) {
                    val vm: SettingsViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    SettingsScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }

                composable(Routes.ABOUT) {
                    AboutScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.LOGOUT_CONFIRM) {
                    val vm: LogoutViewModel = hiltViewModel()
                    ObserveEvents(vm.events) { handleEvent(it, navController, snackbarHostState, scope) }
                    LogoutConfirmationScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
