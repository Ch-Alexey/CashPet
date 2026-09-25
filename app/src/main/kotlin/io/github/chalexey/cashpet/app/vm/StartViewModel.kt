package io.github.chalexey.cashpet.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.chalexey.cashpet.app.appContainer
import io.github.chalexey.cashpet.app.store.GameStore
import io.github.chalexey.cashpet.core.model.Slot
import io.github.chalexey.cashpet.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Стартовый экран: открывает сохранённый профиль — повторный запуск продолжает ровно с того же места
 * (п. 2.5.13 ТЗ). Обычно это профиль ребёнка; если демо не закрыли — демо-профиль, чтобы эксперт увидел
 * свой прогресс после перезапуска (шаг 11 Приложения А). По «Играть» — Дом, если профиль есть, иначе онбординг.
 */
class StartViewModel(
    private val store: GameStore,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StartUiState())
    val uiState: StateFlow<StartUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val demo = settings.settings.first().demoActive
            val saved = store.open(if (demo) Slot.DEMO else Slot.CHILD)
            _uiState.value = StartUiState(loading = false, hasProfile = saved != null, demo = demo)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this[APPLICATION_KEY]!!.appContainer
                StartViewModel(c.gameStore, c.settingsRepository)
            }
        }
    }
}
