package io.github.chalexey.cashpet.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.chalexey.cashpet.app.appContainer
import io.github.chalexey.cashpet.app.store.GameStore
import io.github.chalexey.cashpet.core.model.Slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Стартовый экран: открывает сохранённый профиль ребёнка — повторный запуск продолжает ровно с того же места
 * (п. 2.5.13 ТЗ). По «Играть» экран ведёт на Дом, если профиль есть, иначе в онбординг.
 */
class StartViewModel(private val store: GameStore) : ViewModel() {

    private val _uiState = MutableStateFlow(StartUiState())
    val uiState: StateFlow<StartUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = store.open(Slot.CHILD)
            _uiState.value = StartUiState(loading = false, hasProfile = saved != null)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { StartViewModel(this[APPLICATION_KEY]!!.appContainer.gameStore) }
        }
    }
}
