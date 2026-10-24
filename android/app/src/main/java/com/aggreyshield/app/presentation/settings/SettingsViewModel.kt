package com.aggreyshield.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aggreyshield.app.BuildConfig
import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.common.OneShotEvent
import com.aggreyshield.app.domain.model.ServerHealth
import com.aggreyshield.app.domain.model.ThemeMode
import com.aggreyshield.app.domain.repository.HealthRepository
import com.aggreyshield.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val hideInRecents: Boolean = false,
    val serverUrlOverride: String = "",
)

sealed interface ConnectionTestState {
    data object Idle : ConnectionTestState
    data object Testing : ConnectionTestState
    data class Result(val health: ServerHealth) : ConnectionTestState
    data class Error(val message: String) : ConnectionTestState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val healthRepository: HealthRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.themeMode,
        settingsRepository.hideInRecents,
        settingsRepository.serverUrlOverride,
    ) { theme, hide, override ->
        SettingsUiState(themeMode = theme, hideInRecents = hide, serverUrlOverride = override.orEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private val _connectionTest = MutableStateFlow<ConnectionTestState>(ConnectionTestState.Idle)
    val connectionTest: StateFlow<ConnectionTestState> = _connectionTest.asStateFlow()

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events: Flow<OneShotEvent> = _events.receiveAsFlow()

    val allowServerOverride: Boolean get() = BuildConfig.ALLOW_SERVER_OVERRIDE

    fun onThemeModeSelected(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun onHideInRecentsToggled(value: Boolean) {
        viewModelScope.launch { settingsRepository.setHideInRecents(value) }
    }

    fun onServerUrlOverrideChanged(value: String) {
        viewModelScope.launch { settingsRepository.setServerUrlOverride(value.takeIf { it.isNotBlank() }) }
    }

    fun onTestConnection() {
        _connectionTest.value = ConnectionTestState.Testing
        viewModelScope.launch {
            when (val result = healthRepository.check()) {
                is ApiResult.Success -> _connectionTest.value = ConnectionTestState.Result(result.data)
                is ApiResult.Failure -> _connectionTest.value = ConnectionTestState.Error(result.error.userMessage)
            }
        }
    }
}
