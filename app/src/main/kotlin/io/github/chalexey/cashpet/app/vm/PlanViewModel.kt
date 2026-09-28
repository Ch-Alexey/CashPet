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
import io.github.chalexey.cashpet.core.model.GameState
import io.github.chalexey.cashpet.core.model.NeedHint
import io.github.chalexey.cashpet.core.model.Part
import io.github.chalexey.cashpet.core.model.Plan
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * План недели (docs/01-функционал.md, раздел 4.2): «Нужно · Хочу · Отложить» кнопками +5 / −5,
 * «Не разложено», подсказка «коту нужно 50», советы соседей; после подтверждения — план и факт (п. 2.5.5 ТЗ).
 * Черновик сохраняется в week.plan после каждого нажатия — переживает перезапуск.
 */
class PlanViewModel(
    private val content: GameContent,
    private val store: GameStore,
) : ViewModel() {

    val uiState: StateFlow<PlanUiState?> = store.state
        .map { it?.let(::toUi) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, store.state.value?.let(::toUi))

    fun increase(part: Part) = change(part, +STEP)

    fun decrease(part: Part) = change(part, -STEP)

    /** «Подтвердить план» — после диалога «Потом план не изменить, но тратить можно по-своему». */
    fun confirm() {
        viewModelScope.launch { store.dispatch(Action.ConfirmPlan) }
    }

    // Новый план — от состояния под замком GameStore, а не от uiState: иначе быстрое второе нажатие потеряется.
    // Больше доступного движок не даст — отказ просто ничего не меняет
    private fun change(part: Part, delta: Int) {
        if (store.state.value == null) return
        viewModelScope.launch {
            store.dispatch { state ->
                val plan = state.week.plan ?: Plan()
                val next = when (part) {
                    Part.NEED -> plan.copy(need = plan.need + delta)
                    Part.WANT -> plan.copy(want = plan.want + delta)
                    Part.SAVE -> plan.copy(save = plan.save + delta)
                }
                val valid = !state.week.planConfirmed && next.need >= 0 && next.want >= 0 && next.save >= 0
                if (valid) Action.SetPlan(next) else null
            }
        }
    }

    private fun toUi(state: GameState): PlanUiState {
        val week = state.week
        val plan = week.plan ?: Plan()
        val unallocated = week.available - plan.total
        val hint = when (content.economy.needHint[state.profile.difficulty] ?: NeedHint.ALWAYS) {
            NeedHint.ALWAYS -> content.economy.needMin
            NeedHint.FIRST_WEEK -> if (week.number == 1) content.economy.needMin else null
            NeedHint.NEVER -> null
        }
        return PlanUiState(
            top = topBar(state, content),
            available = week.available,
            need = plan.need,
            want = plan.want,
            save = plan.save,
            unallocated = unallocated,
            needHint = hint,
            canIncrease = !week.planConfirmed && unallocated >= STEP,
            confirmed = week.planConfirmed,
            fact = if (!week.planConfirmed) null else PlanFactUi(
                spentNeed = week.spentNeed,
                spentWant = week.spentWant,
                saved = maxOf(0, week.deposited - week.withdrawn),   // покупка мечты — не снятие
                earned = week.earned,
            ),
            neighborTips = content.neighbors.flatMap { n ->
                n.planTips.map { NeighborTipUi(characterId = n.id, name = n.name, text = it.withNames(state)) }
            },
        )
    }

    companion object {
        const val STEP = 5                   // кнопки +5 / −5: суммы кратны 5, ребёнку — без клавиатуры

        val Factory = viewModelFactory {
            initializer {
                val c = this[APPLICATION_KEY]!!.appContainer
                PlanViewModel(c.content, c.gameStore)
            }
        }
    }
}
