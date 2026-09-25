package io.github.chalexey.cashpet.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.chalexey.cashpet.app.appContainer
import io.github.chalexey.cashpet.app.store.GameStore
import io.github.chalexey.cashpet.content.GameContent
import io.github.chalexey.cashpet.core.engine.GameEngine
import io.github.chalexey.cashpet.core.model.GameState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Итог недели (docs/01-функционал.md, раздел 4.6) — по последней закрытой неделе из history:
 * план и факт, заработок, три иконки, полоска до стадии, что стало с котом и почему, путь восстановления.
 * [uiState] = null — закрытых недель ещё нет.
 */
class WeekSummaryViewModel(
    private val content: GameContent,
    private val engine: GameEngine,
    private val store: GameStore,
) : ViewModel() {

    val uiState: StateFlow<WeekSummaryUiState?> = store.state
        .map { it?.let(::toUi) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, store.state.value?.let(::toUi))

    private fun toUi(state: GameState): WeekSummaryUiState? =
        state.history.lastOrNull()?.let { weekSummaryUi(it, state, content, engine) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this[APPLICATION_KEY]!!.appContainer
                WeekSummaryViewModel(c.content, c.engine, c.gameStore)
            }
        }
    }
}
