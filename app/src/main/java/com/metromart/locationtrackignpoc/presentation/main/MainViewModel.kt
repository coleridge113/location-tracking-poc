package com.metromart.locationtrackignpoc.presentation.main

import androidx.lifecycle.ViewModel
import com.metromart.locationtrackignpoc.data.local.repository.LocalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(
    private val repository: LocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainViewModelStateEvent.UiState())
    val uiState: StateFlow<MainViewModelStateEvent.UiState> = _uiState.asStateFlow()

    fun onEvent(event: MainViewModelStateEvent.Event) {
        when (event) {
            MainViewModelStateEvent.Event.LoadMap -> {}
            MainViewModelStateEvent.Event.CollectData -> {}
            MainViewModelStateEvent.Event.WriteToLocal -> {}
            MainViewModelStateEvent.Event.ToggleProvider -> {
                val current = _uiState.value.messagingProvider
                val next = when (current) {
                    MessagingProvider.ABLY -> MessagingProvider.PUSHER
                    MessagingProvider.PUSHER -> MessagingProvider.ABLY
                }
                _uiState.value = _uiState.value.copy(messagingProvider = next)
            }
        }
    }
}

object MainViewModelStateEvent {
    data class UiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val success: String? = null,
        val messagingProvider: MessagingProvider = MessagingProvider.PUSHER
    )

    sealed interface Event {
        data object LoadMap : Event
        data object CollectData : Event
        data object WriteToLocal : Event
        data object ToggleProvider : Event
    }

}

enum class MessagingProvider(val value: String) {
    ABLY("Ably"),
    PUSHER("Pusher")
}
