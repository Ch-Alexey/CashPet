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
 * Прогресс (📈 в верхней панели; docs/01-функционал.md, раздел 6; п. 2.5.11 ТЗ): стадия кота и полоска
 * до следующей, пройденные задания по темам, текущая и достигнутые цели, итог последней недели и история,
 * словарь. Только показывает — действий нет. [uiState] = null — профиля нет.
 */
class ProgressViewModel(
    private val content: GameContent,
    private val engine: GameEngine,
    private val store: GameStore,
) : ViewModel() {

    val uiState: StateFlow<ProgressUiState?> = store.state
        .map { it?.let(::toUi) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, store.state.value?.let(::toUi))

    private fun toUi(state: GameState): ProgressUiState {
        val pet = state.pet
        val economy = content.economy
        return ProgressUiState(
            stage = pet.stage,
            totalGp = pet.totalGp,
            gpToNextStage = gpToNextStage(pet.totalGp, pet.stage, economy),
            tasksByTopic = tasksByTopic(state, content),
            goal = state.savings.activeGoalId?.let(content.catalog::goal)?.let { goalUi(it, state) },
            boughtGoals = content.catalog.goals.filter { it.id in state.savings.boughtGoalIds }.map { goalUi(it, state) },
            lastWeek = state.history.lastOrNull()?.let { weekSummaryUi(it, state, content, engine) },
            weeks = state.history.map { WeekLineUi(weekNumber = it.number, gp = it.gp, stageAfter = it.stageAfter) },
            nextStage = nextStage(pet.stage),
            stageProgressPct = stageProgressPct(pet.totalGp, pet.stage, economy),
            glossary = content.glossary.map { GlossaryTermUi(it.id, it.term, it.definition.withNames(state)) },
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this[APPLICATION_KEY]!!.appContainer
                ProgressViewModel(c.content, c.engine, c.gameStore)
            }
        }
    }
}
