package com.aggreyshield.app.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.common.Loadable
import com.aggreyshield.app.core.common.OneShotEvent
import com.aggreyshield.app.core.common.map
import com.aggreyshield.app.core.common.toLoadable
import com.aggreyshield.app.domain.model.AppNotification
import com.aggreyshield.app.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val notifications: Loadable<List<AppNotification>> = Loadable.Loading,
    val unreadOnly: Boolean = false,
    val isMarkingAllRead: Boolean = false,
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events: Flow<OneShotEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    fun onRetry() = load()

    fun onUnreadOnlyToggled(value: Boolean) {
        _uiState.update { it.copy(unreadOnly = value) }
        load()
    }

    fun onNotificationClicked(notification: AppNotification) {
        if (notification.read) return
        viewModelScope.launch {
            val result = notificationRepository.markRead(notification.id)
            if (result is ApiResult.Success) load()
        }
    }

    fun onMarkAllRead() {
        _uiState.update { it.copy(isMarkingAllRead = true) }
        viewModelScope.launch {
            val result = notificationRepository.markAllRead()
            _uiState.update { it.copy(isMarkingAllRead = false) }
            when (result) {
                is ApiResult.Success -> load()
                is ApiResult.Failure -> _events.send(OneShotEvent.ShowMessage(result.error.userMessage))
            }
        }
    }

    private fun load() {
        _uiState.update { it.copy(notifications = Loadable.Loading) }
        viewModelScope.launch {
            val result = notificationRepository.list(unreadOnly = _uiState.value.unreadOnly, limit = 50, offset = 0).map { it.items }
            _uiState.update { it.copy(notifications = result.toLoadable()) }
        }
    }
}
