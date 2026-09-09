package com.medtrack.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

    val homeViewModel = remember { HomeViewModel(profileRepository, labReportRepository, medicineRepository) }
    val reportViewModel = remember { ReportViewModel(labReportRepository) }
    val trendsViewModel = remember { TrendsViewModel(labReportRepository) }
    val medicineViewModel = remember { MedicineViewModel(medicineRepository) }
    val emergencyViewModel = remember { EmergencyViewModel(profileRepository, labReportRepository, medicineRepository) }
    val profileViewModel = remember { ProfileViewModel(profileRepository) }

    val currentProfile by homeViewModel.currentProfile.collectAsState()
    val activeProfileId = currentProfile?.id ?: 1L

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
                    profileId = activeProfileId
                )
            }

            composable(Screen.Medicines.route) {
                MedicineScreen(
                    viewModel = medicineViewModel,
                    profileId = activeProfileId
                )
            }

            composable(Screen.Emergency.route) {
                EmergencyScreen(
                    viewModel = emergencyViewModel,
                    profileId = activeProfileId
                )
            }

            composable(Screen.ReportScan.route) {
                ReportScanScreen(
                    viewModel = reportViewModel,
                    profileId = activeProfileId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ManualEntry.route) {
                ManualEntryScreen(
                    viewModel = reportViewModel,
                    profileId = activeProfileId,
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
