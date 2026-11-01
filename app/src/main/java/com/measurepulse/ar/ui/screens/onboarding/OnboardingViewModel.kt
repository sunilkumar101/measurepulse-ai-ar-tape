package com.measurepulse.ar.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.measurepulse.ar.data.ArSupport
import com.measurepulse.ar.data.DeviceCapabilities
import com.measurepulse.ar.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    private val capabilities: DeviceCapabilities,
) : ViewModel() {

    private val _arSupport = MutableStateFlow(ArSupport.CHECKING)
    val arSupport = _arSupport.asStateFlow()

    init {
        viewModelScope.launch { _arSupport.value = capabilities.arSupport() }
    }

    fun finish(onDone: () -> Unit) {
        viewModelScope.launch {
            prefs.setOnboardingComplete()
            onDone()
        }
    }
}
