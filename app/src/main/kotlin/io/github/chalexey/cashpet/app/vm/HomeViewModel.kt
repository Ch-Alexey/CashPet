package io.github.chalexey.cashpet.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.chalexey.cashpet.app.appContainer
import io.github.chalexey.cashpet.app.store.GameStore
import io.github.chalexey.cashpet.content.GameContent
import io.github.chalexey.cashpet.core.engine.Action
import io.github.chalexey.cashpet.core.engine.GameEngine
import io.github.chalexey.cashpet.core.engine.Result
import io.github.chalexey.cashpet.core.model.GameState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Дом — главный экран (п. 2.5.3 ТЗ): кот и три показателя, баланс, копилка и цель, активное задание.
 * Отсюда же «Завершить неделю». [uiState] = null — профиля нет, экран ведёт в онбординг.
 */
class HomeViewModel(
    private val content: GameContent,
    private val engine: GameEngine,
    private val store: GameStore,
) : ViewModel() {

    // Channel, а не SharedFlow: событие ждёт, пока экран его прочтёт (поворот, экран ещё не подписан),
    // и достаётся ровно одному читателю — «Итог недели» не откроется дважды и не потеряется
    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()
    private var closingWeek = false

    val uiState: StateFlow<HomeUiState?> = store.state
        .map { state -> state?.let(::toUi) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, store.state.value?.let(::toUi))

    /** «Завершить неделю»: сначала сохраняем новую неделю, затем отправляем одно событие навигации. */
    fun closeWeek() {
        if (closingWeek) return
        closingWeek = true
        viewModelScope.launch {
            try {
                if (store.dispatch(Action.CloseWeek) is Result.Ok) {
                    _events.send(HomeEvent.WeekClosed)
                }
            } finally {
                closingWeek = false
            }
        }
    }

    sealed interface HomeEvent {
        data object WeekClosed : HomeEvent
    }

    private fun toUi(state: GameState): HomeUiState {
        val stats = state.pet.stats
        val week = state.week
        val needNotBought = week.foodPoints < content.economy.needFoodPoints ||
            week.carePoints < content.economy.needCarePoints
        return HomeUiState(
            top = topBar(state, content),
            petName = state.pet.name,
            look = state.pet.look,
            stage = state.pet.stage,
            mood = engine.petMood(stats),
            stats = stats,
            petReasonText = content.texts.petNow.getValue(engine.petReasonNow(stats)).withNames(state),
            activeTask = openTasks(state, content, engine).firstOrNull(),
            weekNumber = week.number,
            needWarning = needNotBought,
            closeWarning = when {
                !week.planConfirmed -> CloseWeekWarning.NO_PLAN
                needNotBought -> CloseWeekWarning.NEED_NOT_BOUGHT
                else -> null
            },
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this[APPLICATION_KEY]!!.appContainer
                HomeViewModel(c.content, c.engine, c.gameStore)
            }
        }
    }
}
