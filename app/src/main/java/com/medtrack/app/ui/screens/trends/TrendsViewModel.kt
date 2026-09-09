package com.medtrack.app.ui.screens.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtrack.app.data.local.entity.TestRecordEntity
import com.medtrack.app.data.model.CanonicalTest
import com.medtrack.app.data.model.MedicalDictionary
import com.medtrack.app.data.repository.LabReportRepository
import kotlinx.coroutines.flow.*

class TrendsViewModel(
    private val labReportRepository: LabReportRepository
) : ViewModel() {

    private val _currentProfileId = MutableStateFlow<Long?>(null)
    private val _selectedMetric = MutableStateFlow("Hemoglobin")

    val selectedMetric: StateFlow<String> = _selectedMetric

    val availableMetrics: StateFlow<List<String>> = _currentProfileId.flatMapLatest { profileId ->
        if (profileId != null) {
            labReportRepository.getAllDistinctCanonicalNames(profileId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val metricRecords: StateFlow<List<TestRecordEntity>> = combine(
        _currentProfileId,
        _selectedMetric
    ) { profileId, metric ->
        Pair(profileId, metric)
    }.flatMapLatest { (profileId, metric) ->
        if (profileId != null && metric.isNotBlank()) {
            labReportRepository.getRecordsForMetric(profileId, metric)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val canonicalMetadata: StateFlow<CanonicalTest?> = _selectedMetric.map { metric ->
        MedicalDictionary.findCanonical(metric)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setProfileId(profileId: Long) {
        _currentProfileId.value = profileId
    }

    fun selectMetric(metric: String) {
        _selectedMetric.value = metric
    }
}
