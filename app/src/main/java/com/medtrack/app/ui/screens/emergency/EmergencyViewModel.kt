package com.medtrack.app.ui.screens.emergency

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtrack.app.data.model.EmergencySummary
import com.medtrack.app.data.repository.LabReportRepository
import com.medtrack.app.data.repository.MedicineRepository
import com.medtrack.app.data.repository.ProfileRepository
import com.medtrack.app.domain.pdf.ClinicalPdfGenerator
import com.medtrack.app.ui.components.QrCodeGenerator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EmergencyViewModel(
    private val profileRepository: ProfileRepository,
    private val labReportRepository: LabReportRepository,
    private val medicineRepository: MedicineRepository
) : ViewModel() {

    private val _currentProfileId = MutableStateFlow<Long?>(null)

    val currentProfile = _currentProfileId.flatMapLatest { id ->
        if (id != null) profileRepository.getProfileById(id)
        else profileRepository.defaultProfile
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val emergencySummary: StateFlow<EmergencySummary?> = currentProfile.flatMapLatest { profile ->
        if (profile != null) {
            combine(
                medicineRepository.getActiveMedicinesForProfile(profile.id),
                labReportRepository.getRecentAbnormalRecords(profile.id, limit = 6)
            ) { meds, labs ->
                EmergencySummary(
                    profile = profile,
                    activeMedicines = meds,
                    abnormalLabs = labs
                )
            }
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val qrBitmap: StateFlow<ImageBitmap?> = emergencySummary.map { summary ->
        if (summary != null) {
            QrCodeGenerator.generateQrBitmap(summary.toQrPayload(), size = 480)
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setProfileId(profileId: Long) {
        _currentProfileId.value = profileId
    }

    fun exportAndSharePdf(context: Context) {
        val summary = emergencySummary.value ?: return
        viewModelScope.launch {
            val allReports = labReportRepository.getReportsForProfile(summary.profile.id).first()
            val allRecords = labReportRepository.getAllRecordsForProfile(summary.profile.id).first()
            val pdfUri = ClinicalPdfGenerator.generateClinicalReport(
                context = context,
                profile = summary.profile,
                reports = allReports,
                allRecords = allRecords,
                activeMedicines = summary.activeMedicines
            )
            val shareIntent = ClinicalPdfGenerator.createShareIntent(context, pdfUri)
            context.startActivity(Intent.createChooser(shareIntent, "Share Clinical Medical Summary"))
        }
    }
}
