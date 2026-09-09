package com.medtrack.app.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Vault")
    object Trends : Screen("trends", "Trends")
    object Medicines : Screen("medicines", "Medicines")
    object Emergency : Screen("emergency", "Emergency")

    object ReportScan : Screen("report_scan", "Scan Report")
    object ManualEntry : Screen("manual_entry", "Add Record")
    object ReportDetail : Screen("report_detail/{reportId}", "Report Details") {
        fun createRoute(reportId: Long) = "report_detail/$reportId"
    }
    object ProfileManagement : Screen("profile_management", "Profiles")
}
