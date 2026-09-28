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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

    private val weekClosed = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState?> = combine(store.state, weekClosed) { state, closed ->
        state?.let { toUi(it, closed) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, store.state.value?.let { toUi(it, false) })

    /** «Завершить неделю» — после ответа на closeWarning, если он был. Потом экран открывает «Итог недели». */
    fun closeWeek() {
        viewModelScope.launch {
            if (store.dispatch(Action.CloseWeek) is Result.Ok) weekClosed.value = true
        }
    }

    /** «Итог недели» открыт — событие обработано, повторно не открывать. */
    fun onWeekSummaryOpened() {
        weekClosed.value = false
    }

    private fun toUi(state: GameState, closed: Boolean): HomeUiState {
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
            weekClosed = closed,
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
