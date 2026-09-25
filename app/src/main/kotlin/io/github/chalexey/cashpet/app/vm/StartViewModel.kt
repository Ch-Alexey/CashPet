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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Стартовый экран: открывает сохранённый профиль — повторный запуск продолжает ровно с того же места
 * (п. 2.5.13 ТЗ). Обычно это профиль ребёнка; если демо не закрыли — демо-профиль, чтобы эксперт увидел
 * свой прогресс после перезапуска (шаг 11 Приложения А). По «Играть» — Дом, если профиль есть, иначе онбординг.
 *
 * После открытия состояние живое: профиль удалили в разделе взрослого или вышли из демо — старт это видит.
 */
class StartViewModel(
    private val store: GameStore,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val opened = MutableStateFlow(false)

    val uiState: StateFlow<StartUiState> = combine(opened, store.state, settings.settings) { opened, state, s ->
        if (!opened) StartUiState() else StartUiState(loading = false, hasProfile = state != null, demo = s.demoActive)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StartUiState())

    init {
        viewModelScope.launch {
            val demo = settings.settings.first().demoActive
            store.open(if (demo) Slot.DEMO else Slot.CHILD)
            opened.value = true
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
