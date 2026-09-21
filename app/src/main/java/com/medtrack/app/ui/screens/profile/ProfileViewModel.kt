package com.medtrack.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtrack.app.data.local.entity.ProfileEntity
import com.medtrack.app.data.repository.ProfileRepository
import com.medtrack.app.domain.validation.HealthInput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(private val profileRepository: ProfileRepository) : ViewModel() {
    val error = MutableStateFlow<String?>(null)
    val saving = MutableStateFlow(false)
    val allProfiles: StateFlow<List<ProfileEntity>?> = profileRepository.allProfiles
        .map<List<ProfileEntity>, List<ProfileEntity>?> { it }
        .catch { error.value = "Your profiles could not be loaded. Please restart and try again." }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveProfile(profile: ProfileEntity, onSaved: (Long) -> Unit) {
        if (saving.value) return
        error.value = when {
            profile.name.isBlank() -> "A profile name is required."
            profile.name.length > 80 -> "Keep the name under 80 characters."
            else -> HealthInput.dateError(profile.dob, optional = true) ?: HealthInput.phoneError(profile.emergencyPhone)
        }
        if (error.value != null) return
        saving.value = true
        viewModelScope.launch {
            try {
                val id = if (profile.id == 0L) profileRepository.insertProfile(profile) else {
                    profileRepository.updateProfile(profile); profile.id
                }
                onSaved(id)
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { error.value = "Could not save the profile. Your changes are still here; please retry."
            } finally { saving.value = false }
        }
    }
    fun deleteProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            try { profileRepository.deleteProfile(profile) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { error.value = "Could not delete this profile. Please retry." }
        }
    }
}
