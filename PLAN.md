# Health Report Tracker (MedTrack) — Project Plan (Google Play Store Tech Stack)

## 1. Problem

People get diagnosed and tested many times over their lives, but paper and digital lab reports end up scattered, damaged, or lost. When a medical emergency happens or a doctor appointment takes place, there is rarely time or ability to dig through drawers and old papers. Doctors often have to make critical clinical decisions without visibility into a patient's historical medical trajectory (e.g. creatinine progression, blood glucose fluctuations, hemoglobin drops).

## 2. Solution

**MedTrack** is a secure, local-first native Android medical vault for individuals and families.
Key workflows:
1. **Photo Capture & On-Device OCR**: Photograph or select a lab report. On-device Google ML Kit extracts raw text, which is parsed into structured clinical values (test name, numeric/text value, unit, reference range, test date).
2. **Manual Entry with Autosuggest**: Quick manual logging with instant fuzzy autosuggest from a canonical lab test dictionary (e.g., CBC, Lipid Profile, Liver/Kidney Function Tests, HbA1c).
3. **Trends & Longitudinal Health Tracking**: Interactive visual charts tracking metrics over time with visual indicators for in-range vs. out-of-range values.
4. **Medicine Schedule & Reminders**: Dose alarms with meal timing (before/after food) using exact alarms and notification channels.
5. **Emergency Clinical Card & QR**: Instant one-tap access to critical health data (blood group, allergies, active prescriptions, recent critical labs) with offline QR code generation for emergency medical personnel.
6. **Clinical PDF Export**: Clean, printable PDF health history reports exportable via Android Sharesheet for doctor visits.

---

## 3. Platform & Storage

- **Platform**: Native Android (100% Google Play Store optimized).
- **Target OS**: Android 8.0 (API 26) up to Android 15 / 16 (API 35/36).
- **Local-First Architecture**: All health records, medicine schedules, and report images reside solely on the user's device in app-private sandboxed storage (`context.filesDir`).
- **Local Database**: **Android Jetpack Room** (SQLite abstraction with Kotlin Coroutines/Flow integration).
- **Data Security**: Jetpack BiometricPrompt for vault protection; SQLite database schema structured for SQLCipher encryption; zero unauthenticated external network data leaks.
- **Original Document Storage**: Scaled down and stored in app-private storage, permanently linked to database records for auditability.

---

## 4. Google Play Store Tech Stack

| Layer | Google Play Technology | Rationale & Play Store Alignment |
|---|---|---|
| **Language** | **Kotlin 2.0+** | Google's official preferred language for Android development, offering null safety, coroutines, and type safety. |
| **UI Framework** | **Jetpack Compose** (BOM 2024+) | Google's modern declarative UI toolkit. Provides reactive UI, high frame rate, and native accessibility. |
| **Design System** | **Material 3 (Material You)** | Google Play standard design language with adaptive theming, dynamic color support, and edge-to-edge system bar integration. |
| **Architecture** | **Modern Android Architecture (MVVM / UDF)** | Unidirectional Data Flow using `ViewModel`, `StateFlow`, `SharedFlow`, and Navigation Compose. Clean separation of UI, Domain, and Data layers. |
| **Local Database** | **Android Jetpack Room 2.6+** | Google's official SQLite persistence library. Compile-time query verification, reactive `Flow` observation, and migration support. |
| **On-Device OCR** | **Google Play Services ML Kit Text Recognition** | `com.google.android.gms:play-services-mlkit-text-recognition` (or standalone bundled). 100% offline, zero server cost, zero latency, maximum privacy. |
| **Media / Camera** | **Android Photo Picker & CameraX** | `ActivityResultContracts.PickVisualMedia` + `TakePicture`. Complies with Google Play's strict photo/video permission policies without requesting invasive broad storage permissions. |
| **Background & Alarms** | **Android AlarmManager & NotificationManager** | Exact alarm scheduling via `setExactAndAllowWhileIdle` and high-priority notification channels for medicine adherence. Compliant with Play Store `SCHEDULE_EXACT_ALARM` policy for health/reminder apps. |
| **Charting & Visualization** | **Jetpack Compose Canvas / Custom Charts** | Smooth, hardware-accelerated interactive biometric trend curves with normal physiological bands, tooltips, and out-of-range indicators. |
| **Security & Auth** | **AndroidX Biometric** | `BiometricPrompt` integrating fingerprint, face unlock, and device PIN/pattern fallback. |
| **Document Export** | **Android Native `PdfDocument` & `FileProvider`** | Native vector-rendered PDF reports shared via Android Sharesheet (`Intent.ACTION_SEND`). Zero third-party proprietary PDF licensing constraints. |
| **Emergency QR** | **ZXing Core (`com.google.zxing:core`)** | Standalone offline QR code matrix generation rendering emergency medical summaries. |
| **Build System** | **Gradle 8.x / 9.x + Android Gradle Plugin (AGP)** | App Bundle (`.aab`) generation, ProGuard / R8 code shrinking and resource optimization. |

---

## 5. Core Features & Specifications

### A. Multi-Profile Vault
- Support multiple family members (e.g., "Self", "Spouse", "Father", "Mother", "Child").
- Profile attributes: Name, Relationship, Date of Birth, Gender, Blood Group, Emergency Contact, Allergies, Chronic Conditions.
- Instant profile switching from the top app bar; independent test histories and medicine schedules.

### B. Report Entry: Two Pathways
1. **Photo Capture & On-Device OCR Pipeline**:
   - User picks an image via the Google Play Photo Picker or captures via device camera.
   - Preprocessing: bitmap orientation correction, scaling, and contrast tuning.
   - On-Device OCR: Google ML Kit extracts text blocks, lines, and bounding boxes.
   - Parsing Engine: Regex + canonical dictionary tokenization extracts:
     - Test Name (normalized to canonical medical terms)
     - Measured Value (numeric or textual)
     - Unit (mg/dL, mmol/L, g/dL, %, cells/mcL, etc.)
     - Reference Range (Low – High bounds printed on report)
     - Report Date (defaults to today if undetected)
   - Confidence Scoring: High-confidence items highlighted green; ambiguous or out-of-bounds items flagged yellow/red for user confirmation before saving.
   - Permanent photo linkage: original image saved to private files and linked to the report.
2. **Manual Entry with Autosuggest**:
   - Searchable autocomplete field drawing from a built-in canonical medical test dictionary (100+ standard laboratory metrics).
   - Auto-fills standard units and typical physiological reference ranges upon test selection.

### C. Structured Extraction & Clinical Normalization
- **Canonical Test Dictionary**: Covers Complete Blood Count (CBC), Metabolic Panel, Lipid Profile, Liver Function Tests (LFT), Renal Function Tests (KFT), Thyroid (TSH/T3/T4), Diabetes markers (Fasting Glucose, Post-prandial, HbA1c), Urine Routine, and inflammatory markers (CRP, ESR).
- **Unit Normalization Engine**: Converts equivalent units (e.g. Glucose `mg/dL` <-> `mmol/L`, Cholesterol `mg/dL` <-> `mmol/L`) to ensure trend graphs are mathematically sound regardless of testing lab.
- **Physiological Bounds Validation**: Detects anomalous values (e.g., a platelet count of 250,000 mistakenly read as hemoglobin 250,000) to protect clinical data integrity.

### D. Trends & Biometric Visualizations
- Longitudinal line charts per test metric over time.
- Physiological normal range zone shaded in green/subtle gray.
- Abnormal values highlighted with colored alert markers (high: red, low: orange).
- Min, Max, and Latest stats displayed with percentage delta compared to previous test.

### E. Medicine Tracker & Smart Alarms
- Profile-specific medication schedule:
  - Medicine name, strength/dosage (e.g., "500 mg", "1 tablet").
  - Frequency & Timing: Morning, Afternoon, Evening, Night.
  - Meal relation: Before meal, With meal, After meal, Empty stomach.
  - Duration & remaining pill count.
- Smart Reminders:
  - Exact alarms via Android `AlarmManager` triggering `MedicineAlarmReceiver`.
  - Notification with "Take" and "Snooze" direct action buttons.
  - Play Store compliant notification channel configuration.

### F. Emergency Clinical Access Mode
- 1-Tap Emergency View designed for first responders and ER clinicians.
- Displays: Patient Name, Age, Blood Type, Emergency Contact (with direct dial), Known Allergies, Active Medications, Critical Conditions, and Recent Flagged Labs.
- Offline QR Code Generator: Encodes the emergency medical summary into a high-density QR code for instant scanning by paramedics or doctors.

### G. Clinical PDF Generation & Export
- Formats patient demographics, active medication chart, and chronologically sorted test history into a clean multi-page clinical PDF using native `PdfDocument`.
- Secured sharing via Android `FileProvider` to WhatsApp, Email, Drive, or Print.

### H. Google Play Store Compliance & Security
- **Privacy First**: Zero third-party trackers, zero mandatory accounts, 100% offline functionality.
- **Permissions**:
  - `CAMERA`: Declared with optional feature flag `android.hardware.camera.any`.
  - `POST_NOTIFICATIONS`: Requested at runtime (Android 13+) when medicine reminders are configured.
  - `SCHEDULE_EXACT_ALARM`: Justified under medical reminder category.
  - NO broad `READ_EXTERNAL_STORAGE` or `READ_MEDIA_IMAGES` requested (uses Modern Android Photo Picker).
- **Edge-to-edge UI**: Uses `enableEdgeToEdge()` and WindowInsets padding for Android 15 compatibility.
- **App Signing & Release**: Configured for Android App Bundle (`.aab`) with R8 optimization.

---

## 6. Implementation Architecture

```
com.medtrack.app
├── data
│   ├── local
│   │   ├── MedTrackDatabase.kt
│   │   ├── entity
│   │   │   ├── ProfileEntity.kt
│   │   │   ├── LabReportEntity.kt
│   │   │   ├── TestRecordEntity.kt
│   │   │   └── MedicineEntity.kt
│   │   └── dao
│   │       ├── ProfileDao.kt
│   │       ├── LabReportDao.kt
│   │       ├── TestRecordDao.kt
│   │       └── MedicineDao.kt
│   ├── repository
│   │   ├── ProfileRepository.kt
│   │   ├── LabReportRepository.kt
│   │   └── MedicineRepository.kt
│   └── model
│       ├── CanonicalTest.kt
│       ├── EmergencySummary.kt
│       └── TestRange.kt
├── domain
│   ├── ocr
│   │   ├── ReportOcrAnalyzer.kt
│   │   ├── ClinicalRegexParser.kt
│   │   └── UnitConverter.kt
│   └── alarm
│       ├── MedicineAlarmScheduler.kt
│       ├── MedicineAlarmReceiver.kt
│       └── NotificationHelper.kt
├── ui
│   ├── theme
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── navigation
│   │   └── MedTrackNavGraph.kt
│   ├── components
│   │   ├── TrendChart.kt
│   │   ├── CanonicalTestPicker.kt
│   │   ├── ProfileHeader.kt
│   │   └── EmergencyQrDialog.kt
│   ├── screens
│   │   ├── home
│   │   │   ├── HomeScreen.kt
│   │   │   └── HomeViewModel.kt
│   │   ├── report
│   │   │   ├── ReportScanScreen.kt
│   │   │   ├── ManualEntryScreen.kt
│   │   │   ├── ReportDetailScreen.kt
│   │   │   └── ReportViewModel.kt
│   │   ├── trends
│   │   │   ├── TrendsScreen.kt
│   │   │   └── TrendsViewModel.kt
│   │   ├── medicines
│   │   │   ├── MedicineScreen.kt
│   │   │   └── MedicineViewModel.kt
│   │   ├── emergency
│   │   │   ├── EmergencyScreen.kt
│   │   │   └── EmergencyViewModel.kt
│   │   └── profile
│   │       ├── ProfileListScreen.kt
│   │       └── ProfileEditScreen.kt
│   └── pdf
│       └── ClinicalPdfGenerator.kt
└── MedTrackApp.kt
```

---

## 7. Execution Roadmap

- **Step 1: Android Native Foundation Setup**
  Configure root `build.gradle.kts`, `settings.gradle.kts`, `app/build.gradle.kts`, `AndroidManifest.xml`, vector assets, and string resources with Target SDK 35 and Jetpack Compose BOM.
- **Step 2: Room Database & Repository Layer**
  Define `ProfileEntity`, `LabReportEntity`, `TestRecordEntity`, `MedicineEntity`, DAOs, TypeConverters, and Repository classes.
- **Step 3: Clinical Domain Logic & Medical Dictionary**
  Build canonical test dictionary (100+ tests), alias lookup, unit normalizer, and OCR extraction parser with confidence heuristics.
- **Step 4: Jetpack Compose UI & Navigation System**
  Implement Material 3 edge-to-edge theme, Bottom Navigation / Drawer, Profile switcher, and all core screens.
- **Step 5: On-Device OCR Scanner & Manual Entry Flow**
  Implement ML Kit Text Recognition integration, photo picker, image preview, extracted table verification UI, and manual entry with autosuggest.
- **Step 6: Interactive Biometric Charts & Trend Visualizer**
  Implement Compose Canvas trend graphs with reference zones, min/max metrics, and abnormal flags.
- **Step 7: Medicine Alarm & Notification System**
  Implement `AlarmManager` scheduling, `MedicineAlarmReceiver`, notification channels, and medicine tracker UI.
- **Step 8: Emergency Mode & Clinical PDF Export**
  Implement 1-tap Emergency Summary with offline ZXing QR Code generation and native multi-page `PdfDocument` clinical summary generator with Android `FileProvider`.
- **Step 9: Quality Verification & Play Store Release Readiness**
  Compile, verify database migrations, unit test parsing algorithms, and validate build configuration.
