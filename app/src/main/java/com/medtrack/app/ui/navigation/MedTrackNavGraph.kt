package com.medtrack.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.medtrack.app.data.repository.LabReportRepository
import com.medtrack.app.data.repository.MedicineRepository
import com.medtrack.app.data.repository.ProfileRepository
import com.medtrack.app.ui.screens.emergency.EmergencyScreen
import com.medtrack.app.ui.screens.emergency.EmergencyViewModel
import com.medtrack.app.ui.screens.home.HomeScreen
import com.medtrack.app.ui.screens.home.HomeViewModel
import com.medtrack.app.ui.screens.medicines.MedicineScreen
import com.medtrack.app.ui.screens.medicines.MedicineViewModel
import com.medtrack.app.ui.screens.profile.ProfileManagementScreen
import com.medtrack.app.ui.screens.profile.ProfileViewModel
import com.medtrack.app.ui.screens.report.ManualEntryScreen
import com.medtrack.app.ui.screens.report.ReportDetailScreen
import com.medtrack.app.ui.screens.report.ReportScanScreen
import com.medtrack.app.ui.screens.report.ReportViewModel
import com.medtrack.app.ui.screens.trends.TrendsScreen
import com.medtrack.app.ui.screens.trends.TrendsViewModel

data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

@Composable
fun MedTrackNavGraph(
    profileRepository: ProfileRepository,
    labReportRepository: LabReportRepository,
    medicineRepository: MedicineRepository
) {
    val navController = rememberNavController()

    // viewModel() ties these to the nav graph's ViewModelStore: state survives
    // configuration changes and onCleared() cancels viewModelScope (the old
    // remember {} construction lost state on rotation and never cancelled).
    val homeViewModel: HomeViewModel = viewModel {
        HomeViewModel(profileRepository, labReportRepository, medicineRepository)
    }
    val reportViewModel: ReportViewModel = viewModel { ReportViewModel(labReportRepository) }
    val trendsViewModel: TrendsViewModel = viewModel { TrendsViewModel(labReportRepository) }
    val medicineViewModel: MedicineViewModel = viewModel { MedicineViewModel(medicineRepository) }
    val emergencyViewModel: EmergencyViewModel = viewModel {
        EmergencyViewModel(profileRepository, labReportRepository, medicineRepository)
    }
    val profileViewModel: ProfileViewModel = viewModel { ProfileViewModel(profileRepository) }

    val currentProfile by homeViewModel.currentProfile.collectAsState()
    // Null until the default profile loads; screens treat null as "loading"
    // rather than writing against a nonexistent profile id 1.
    val activeProfileId = currentProfile?.id

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Home, Icons.Default.FolderShared, "Vault"),
        BottomNavItem(Screen.Trends, Icons.Default.ShowChart, "Trends"),
        BottomNavItem(Screen.Medicines, Icons.Default.Medication, "Medicines"),
        BottomNavItem(Screen.Emergency, Icons.Default.LocalHospital, "Emergency")
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Trends.route,
        Screen.Medicines.route,
        Screen.Emergency.route
    )

    // Hold the UI until the profile (and its id) is loaded; previously a
    // hardcoded fallback id of 1 could send writes to a nonexistent profile.
    if (activeProfileId == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val profileId = activeProfileId

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToScan = { navController.navigate(Screen.ReportScan.route) },
                    onNavigateToManual = { navController.navigate(Screen.ManualEntry.route) },
                    onNavigateToReportDetail = { reportId ->
                        navController.navigate(Screen.ReportDetail.createRoute(reportId))
                    },
                    onNavigateToProfiles = { navController.navigate(Screen.ProfileManagement.route) },
                    onNavigateToEmergency = { navController.navigate(Screen.Emergency.route) }
                )
            }

            composable(Screen.Trends.route) {
                TrendsScreen(
                    viewModel = trendsViewModel,
                    profileId = profileId
                )
            }

            composable(Screen.Medicines.route) {
                MedicineScreen(
                    viewModel = medicineViewModel,
                    profileId = profileId
                )
            }

            composable(Screen.Emergency.route) {
                EmergencyScreen(
                    viewModel = emergencyViewModel,
                    profileId = profileId
                )
            }

            composable(Screen.ReportScan.route) {
                ReportScanScreen(
                    viewModel = reportViewModel,
                    profileId = profileId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ManualEntry.route) {
                ManualEntryScreen(
                    viewModel = reportViewModel,
                    profileId = profileId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ReportDetail.route,
                arguments = listOf(navArgument("reportId") { type = NavType.LongType })
            ) { backStackEntry ->
                val reportId = backStackEntry.arguments?.getLong("reportId") ?: 0L
                ReportDetailScreen(
                    reportId = reportId,
                    repository = labReportRepository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ProfileManagement.route) {
                ProfileManagementScreen(
                    viewModel = profileViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
