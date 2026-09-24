package com.medtrack.app

import androidx.compose.ui.test.*
import androidx.activity.compose.setContent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.medtrack.app.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Run only in the dedicated review emulator: this test clears this app's local test data. */
@RunWith(AndroidJUnit4::class)
class ProfileFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = compose.activity.application as MedTrackApplication

    @Before fun reset() = runBlocking { withContext(Dispatchers.IO) { app.database.clearAllTables() } }
    @After fun cleanup() = runBlocking { withContext(Dispatchers.IO) { app.database.clearAllTables() } }

    private fun awaitText(text: String) {
        compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun closeKeyboard() {
        compose.runOnUiThread {
            val manager = compose.activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            manager.hideSoftInputFromWindow(compose.activity.window.decorView.windowToken, 0)
        }
        compose.waitUntil(5000) {
            androidx.core.view.ViewCompat.getRootWindowInsets(compose.activity.window.decorView)?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) != true
        }
        compose.waitForIdle()
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val directory = java.io.File(app.getExternalFilesDir(null), "qa").apply { mkdirs() }
        java.io.File(directory, name + ".png").outputStream().use {
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun scanFieldsAreEditableAndSaveExplainsReviewRequirement() {
        val vm = com.medtrack.app.ui.screens.report.ReportViewModel(app.labReportRepository)
        val profile = runBlocking { app.profileRepository.insertProfile(ProfileEntity(name = "Scan UI test", isDefault = true)) }
        val bitmap = android.graphics.Bitmap.createBitmap(1000, 400, android.graphics.Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(bitmap).apply {
            drawColor(android.graphics.Color.WHITE)
            val paint = android.graphics.Paint().apply { color = android.graphics.Color.BLACK; textSize = 32f }
            drawText("Report date: 18-Sep-2024", 20f, 60f, paint)
            drawText("Hemoglobin 14.2 g/dL 12-16", 20f, 150f, paint)
        }
        val file = java.io.File(app.cacheDir, "scan-ui.png")
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        compose.runOnUiThread {
            compose.activity.setContent {
                com.medtrack.app.ui.theme.MedTrackTheme {
                    com.medtrack.app.ui.screens.report.ReportScanScreen(vm, profile, onNavigateBack = {}, onManualEntry = {})
                }
            }
            vm.startOcrProcessing(app, android.net.Uri.fromFile(file))
        }
        compose.waitUntil(60000) { vm.ocrState.value is com.medtrack.app.ui.screens.report.OcrUiState.Success }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Result *"))
        compose.onNodeWithText("Result *").performScrollTo().assertTextContains("14.2").performTextReplacement("13.8")
        compose.onNodeWithText("Test name *").performScrollTo().assertTextContains("Hemoglobin")
        screenshot("07-editable-scan")
        compose.onNodeWithText("Save 1 results").performClick()
        compose.onNodeWithText("Compare the results with the original photo and confirm your review.").assertExists()
        compose.onNode(isToggleable()).performClick()
        compose.onNodeWithText("Save 1 results").performClick()
        compose.waitUntil(15000) { !vm.saving.value && runBlocking { app.labReportRepository.getAllRecordsForProfileSync(profile).size } == 1 }
        val saved = runBlocking { app.labReportRepository.getAllRecordsForProfileSync(profile).single() }
        assertEquals("13.8", saved.value); assertEquals("2024-09-18", saved.testDate)
        file.delete()
    }

    @Test fun freshInstallCreatesOnlyNamedProfileAndReturnsToChooser() {
        awaitText("Create your first profile")
        screenshot("01-welcome")
        compose.onNodeWithText("Create your first profile").performClick()
        compose.onNodeWithText("Create profile").assertIsNotEnabled()
        compose.onNodeWithText("Name *").performTextInput("Review Person")
        screenshot("01b-profile-form")
        compose.onNodeWithText("Create profile").assertIsEnabled().performClick()
        awaitText("A clearer picture of your health")
        val stored = runBlocking { app.profileRepository.getDefaultProfileSync()!! }
        assertEquals("Unknown", stored.bloodGroup)
        assertEquals("Not recorded", stored.allergies)
        assertEquals("", stored.dob)
        compose.onNodeWithContentDescription("Choose another profile").performClick()
        awaitText("Who are we\ncaring for today?")
        screenshot("02-profile-picker")
        compose.onNodeWithText("Review Person").performClick()
        awaitText("A clearer picture of your health")
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        awaitText("Who are we\ncaring for today?")
        compose.onNodeWithText("A clearer picture of your health").assertDoesNotExist()
    }
    @Test fun twoProfilesSelectExplicitlyAndDoNotMixReports() {
        runBlocking {
            val first = app.profileRepository.insertProfile(ProfileEntity(name = "Alpha", isDefault = true))
            app.profileRepository.insertProfile(ProfileEntity(name = "Beta"))
            app.labReportRepository.saveReportWithRecords(
                LabReportEntity(profileId = first, title = "Alpha only report", testDate = "2024-01-01"),
                listOf(TestRecordEntity(reportId = 0, profileId = first, testName = "Hemoglobin", canonicalName = "Hemoglobin", value = "14", numericValue = 14.0, unit = "g/dL", testDate = "2024-01-01")))
        }
        awaitText("Beta")
        compose.onNodeWithText("Alpha only report").assertDoesNotExist()
        compose.onNodeWithText("Beta").performClick()
        awaitText("A clearer picture of your health")
        compose.onNodeWithText("Alpha only report").assertDoesNotExist()
        compose.onNodeWithContentDescription("Choose another profile").performClick()
        awaitText("Alpha")
        compose.onNodeWithText("Alpha").performClick()
        awaitText("Alpha only report")
        compose.activityRule.scenario.recreate()
        awaitText("Alpha only report")
    }
    @Test fun manualEntryRejectsLettersAndInvalidDateBeforeSaving() {
        runBlocking { app.profileRepository.insertProfile(ProfileEntity(name = "Validation review", isDefault = true)) }
        awaitText("Validation review")
        compose.onNodeWithText("Validation review").performClick()
        awaitText("Type results")
        compose.onNodeWithText("Type results").performClick()
        awaitText("Add results")
        compose.onNodeWithText("Search test name *").performScrollTo().performTextInput("Hemoglobin")
        compose.onNode(hasText("Hemoglobin") and hasClickAction() and !hasSetTextAction()).performScrollTo().performClick()
        compose.onNodeWithText("Measured value *").performScrollTo().performTextInput("abc")
        closeKeyboard()
        compose.onNodeWithText("Add result").performScrollTo().performClick()
        screenshot("08-invalid-input")
        compose.onNodeWithText("Enter a valid number", substring = true).assertExists()
        compose.onNodeWithText("Measured value *").performTextReplacement("14.2")
        compose.onNodeWithText("Unit from report *").performScrollTo().assertTextContains("g/dL")
        compose.onNodeWithText("Reference range (optional)").performScrollTo().performTextInput("13-17.5")
        closeKeyboard()
        compose.onNodeWithText("Add result").performScrollTo().performClick()
        compose.onNodeWithText("Save 1 results").performClick()
        compose.onNodeWithText("Enter the date printed on the report.").assertExists()
        compose.onNodeWithText("Test date * · YYYY-MM-DD").performScrollTo().performTextInput("2024-02-30")
        compose.onNodeWithText("Save 1 results").performClick()
        compose.onAllNodesWithText("Enter a real calendar date.").onFirst().assertExists()
        compose.onNodeWithText("Test date * · YYYY-MM-DD").performTextReplacement("2024-02-29")
        compose.onNodeWithText("Save 1 results").performClick()
        awaitText("Lab report · 2024-02-29")
        screenshot("03-records")
        val profile = runBlocking { app.profileRepository.getDefaultProfileSync()!! }
        val records = runBlocking { app.labReportRepository.getAllRecordsForProfileSync(profile.id) }
        assertEquals(1, records.size)
        assertEquals(14.2, records.single().numericValue!!, 0.0)
        assertFalse(records.single().isAbnormal)
        compose.onNodeWithText("Lab report · 2024-02-29").performScrollTo().performClick()
        awaitText("Within saved range")
        screenshot("04-report-detail")
    }

    @Test fun generalRangesFollowUnitsAndKeepTheirSource() {
        runBlocking { app.profileRepository.insertProfile(ProfileEntity(name = "Reference review", isDefault = true)) }
        awaitText("Reference review")
        compose.onNodeWithText("Reference review").performClick()
        awaitText("Type results")
        compose.onNodeWithText("Type results").performClick()
        awaitText("Search test name *")
        compose.onNodeWithText("Search test name *").assertIsDisplayed().performTextInput("RBC")
        compose.onNode(hasText("RBC Count") and hasClickAction() and !hasSetTextAction()).performScrollTo().performClick()
        compose.onNodeWithText("Unit from report *").performScrollTo().assertTextContains("million/mcL")
        compose.onNodeWithText("General reference").performScrollTo().performClick()
        compose.onNodeWithText("Adult female").performScrollTo().performClick()
        screenshot("06-general-reference")
        compose.onNodeWithText("Reference range (optional)").performScrollTo().assertTextContains("4.2–5.4")
        compose.onNodeWithText("Unit from report *").performScrollTo().performTextReplacement("10^12/L")
        compose.onNodeWithText("Reference range (optional)").performScrollTo().assertTextContains("4.2–5.4")
        compose.onNodeWithText("Unit from report *").performScrollTo().performTextReplacement("unknown")
        compose.onNodeWithText("Reference range (optional)").performScrollTo().assert(
            androidx.compose.ui.test.SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.EditableText, androidx.compose.ui.text.AnnotatedString("")))
        compose.onNodeWithText("Unit from report *").performScrollTo().performTextReplacement("million/mcL")
        compose.onNodeWithText("Reference range (optional)").performScrollTo().assertTextContains("4.2–5.4")
        compose.onNodeWithText("Measured value *").performScrollTo().performTextInput("4.8")
        compose.onNodeWithText("Add result").performScrollTo().performClick()
        compose.onNodeWithText("Use today's date").performScrollTo().performClick()
        compose.onNodeWithText("Skip title").performScrollTo().performClick()
        compose.onNodeWithText("Title skipped").assertExists()
        screenshot("05-entry-date")
        compose.onNodeWithText("Save 1 results").performClick()
        awaitText("A clearer picture of your health")
        val profile = runBlocking { app.profileRepository.getDefaultProfileSync()!! }
        val row = runBlocking { app.labReportRepository.getAllRecordsForProfileSync(profile.id).single() }
        assertEquals("4.2–5.4", row.refRangeText)
        assertTrue(row.referenceSource!!.contains("Adult female"))
        assertTrue(row.referenceSource!!.contains("https://medlineplus.gov"))
    }
}
