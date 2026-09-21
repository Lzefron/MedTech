package com.medtrack.app.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.medtrack.app.data.repository.*
import com.medtrack.app.ui.screens.emergency.*
import com.medtrack.app.ui.screens.home.*
import com.medtrack.app.ui.screens.medicines.*
import com.medtrack.app.ui.screens.profile.*
import com.medtrack.app.ui.screens.report.*
import com.medtrack.app.ui.screens.trends.*

@Composable
fun MedTrackNavGraph(
    profileRepository: ProfileRepository,
    labReportRepository: LabReportRepository,
    medicineRepository: MedicineRepository,
    sessionVersion: Int = 0
) {
    val navController = rememberNavController()
    val home: HomeViewModel = viewModel { HomeViewModel(profileRepository, labReportRepository, medicineRepository) }
    val profiles: ProfileViewModel = viewModel { ProfileViewModel(profileRepository) }
    LaunchedEffect(sessionVersion) {
        if (sessionVersion > 0) {
            home.clearSelection()
            navController.navigate(Screen.ProfileLauncher.route) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    val selected by home.selectedProfileId.collectAsState()
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val tabs = listOf(Screen.Home, Screen.Trends, Screen.Medicines, Screen.Emergency)
    val icons = listOf(Icons.Default.FolderShared, Icons.Default.ShowChart, Icons.Default.Medication, Icons.Default.LocalHospital)
    val labels = listOf("Records", "Trends", "Medicines", "Emergency")

    // Keep NavHost mounted while its initial route is established.
    // Selection is deliberately not saved across process death.
    LaunchedEffect(route, selected) {
        if (route != null && route != Screen.ProfileLauncher.route && selected == null) {
            navController.navigate(Screen.ProfileLauncher.route) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
        if (route == Screen.ProfileLauncher.route) home.clearSelection()
    }

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = {
        if (selected != null && route in tabs.map { it.route }) {
            NavigationBar {
                tabs.forEachIndexed { index, screen ->
                    NavigationBarItem(selected = route == screen.route, icon = { Icon(icons[index], null) },
                        label = { Text(labels[index]) }, onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Home.route) { inclusive = screen == Screen.Home }
                                launchSingleTop = true
                            }
                        })
                }
            }
        }
    }) { padding ->
        NavHost(navController, startDestination = Screen.ProfileLauncher.route, modifier = Modifier.padding(padding)) {
            composable(Screen.ProfileLauncher.route) {
                ProfileLauncherScreen(profiles) { id ->
                    home.selectProfile(id)
                    navController.navigate(Screen.Home.route) { launchSingleTop = true }
                }
            }
            composable(Screen.Home.route) {
                if (selected != null) HomeScreen(home,
                    onNavigateToScan = { navController.navigate(Screen.ReportScan.route) },
                    onNavigateToManual = { navController.navigate(Screen.ManualEntry.route) },
                    onNavigateToReportDetail = { navController.navigate(Screen.ReportDetail.createRoute(it)) },
                    onNavigateToLauncher = { navController.popBackStack(Screen.ProfileLauncher.route, false) },
                    onNavigateToEmergency = { navController.navigate(Screen.Emergency.route) })
            }
            composable(Screen.Trends.route) {
                selected?.let { id ->
                    val vm: TrendsViewModel = viewModel(key = "trends-" + id) { TrendsViewModel(labReportRepository) }
                    TrendsScreen(vm, id)
                }
            }
            composable(Screen.Medicines.route) {
                selected?.let { id ->
                    val vm: MedicineViewModel = viewModel(key = "medicines-" + id) { MedicineViewModel(medicineRepository) }
                    MedicineScreen(vm, id)
                }
            }
            composable(Screen.Emergency.route) {
                selected?.let { id ->
                    val vm: EmergencyViewModel = viewModel(key = "emergency-" + id) { EmergencyViewModel(profileRepository, labReportRepository, medicineRepository) }
                    EmergencyScreen(vm, id)
                }
            }
            composable(Screen.ReportScan.route) {
                selected?.let { id ->
                    val vm: ReportViewModel = viewModel { ReportViewModel(labReportRepository) }
                    ReportScanScreen(vm, id, onNavigateBack = { navController.popBackStack() },
                        onManualEntry = { navController.navigate(Screen.ManualEntry.route) })
                }
            }
            composable(Screen.ManualEntry.route) {
                selected?.let { id ->
                    val vm: ReportViewModel = viewModel { ReportViewModel(labReportRepository) }
                    ManualEntryScreen(vm, id) { navController.popBackStack() }
                }
            }
            composable(Screen.ReportDetail.route, arguments = listOf(navArgument("reportId") { type = NavType.LongType })) { detail ->
                selected?.let { id ->
                    ReportDetailScreen(detail.arguments?.getLong("reportId") ?: 0L, labReportRepository,
                        profileId = id, onNavigateBack = { navController.popBackStack() })
                }
            }
        }
    }
}
