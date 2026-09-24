package io.github.chalexey.cashpet.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.chalexey.cashpet.app.appContainer
import io.github.chalexey.cashpet.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Что показывает экран настроек (docs/01-функционал.md, раздел 7). Остальные пункты — переходы, их делает экран. */
data class SettingsUiState(
    val sound: Boolean = true,
    val animations: Boolean = true,
)

class SettingsViewModel(private val settings: SettingsRepository) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = settings.settings
        .map { SettingsUiState(sound = it.sound, animations = it.animations) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    fun setSound(on: Boolean) {
        viewModelScope.launch { settings.setSound(on) }
    }

    fun setAnimations(on: Boolean) {
        viewModelScope.launch { settings.setAnimations(on) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { SettingsViewModel(this[APPLICATION_KEY]!!.appContainer.settingsRepository) }
        }
    }
}
