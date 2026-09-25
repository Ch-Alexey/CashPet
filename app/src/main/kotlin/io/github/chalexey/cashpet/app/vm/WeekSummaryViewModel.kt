package io.github.chalexey.cashpet.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.chalexey.cashpet.app.appContainer
import io.github.chalexey.cashpet.app.store.GameStore
import io.github.chalexey.cashpet.content.GameContent
import io.github.chalexey.cashpet.content.WeekHint
import io.github.chalexey.cashpet.core.engine.GameEngine
import io.github.chalexey.cashpet.core.model.GameState
import io.github.chalexey.cashpet.core.model.GrowthIcon
import io.github.chalexey.cashpet.core.model.Part
import io.github.chalexey.cashpet.core.model.Stage
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

    private fun toUi(state: GameState): WeekSummaryUiState? {
        val r = state.history.lastOrNull() ?: return null
        val nextStage = when (r.stageAfter) {
            Stage.BABY -> Stage.TEEN
            Stage.TEEN -> Stage.ADULT
            Stage.ADULT -> null
        }
        // Про еду и уход говорит строка про кота, здесь — план и копилка; всё получилось — подсказки нет
        val hint = when {
            !r.planConfirmed -> WeekHint.NO_PLAN
            r.mPct < 100 -> WeekHint.PLAN
            r.sPct < 100 -> WeekHint.SAVE
            else -> null
        }
        return WeekSummaryUiState(
            weekNumber = r.number,
            planConfirmed = r.planConfirmed,
            rows = listOf(
                PlanFactRowUi(Part.NEED, planned = r.plan.need, actual = r.factNeed),
                PlanFactRowUi(Part.WANT, planned = r.plan.want, actual = r.factWant),
                PlanFactRowUi(Part.SAVE, planned = r.plan.save, actual = r.factSaved),
            ),
            earned = r.earned,
            icons = listOf(
                GrowthIconUi(GrowthIcon.NEED, r.nPct),
                GrowthIconUi(GrowthIcon.PLAN, r.mPct),
                GrowthIconUi(GrowthIcon.SAVE, r.sPct),
            ),
            gpToNextStage = nextStage?.let { maxOf(0, content.economy.stageThresholds.getValue(it) - r.totalGp) },
            stageUp = r.stageAfter.takeIf { it != r.stageBefore },
            pet = PetChangeUi(
                before = r.statsBefore,
                after = r.statsAfter,
                mood = engine.petMood(r.statsBefore),     // как в движке: причина недели — по показателям до падения
                reasonText = content.texts.petWeek.getValue(r.petReason).withNames(state),
            ),
            goal = state.savings.activeGoalId?.let(content.catalog::goal)?.let { goalUi(it, state) },
            recoveryHint = hint?.let { content.texts.weekHints.getValue(it).withNames(state) },
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this[APPLICATION_KEY]!!.appContainer
                WeekSummaryViewModel(c.content, c.engine, c.gameStore)
            }
        }
    }
}
