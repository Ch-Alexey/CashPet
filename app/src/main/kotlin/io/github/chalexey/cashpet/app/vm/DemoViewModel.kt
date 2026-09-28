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
import io.github.chalexey.cashpet.core.model.Slot
import io.github.chalexey.cashpet.core.model.Stage
import io.github.chalexey.cashpet.core.sim.ReasonableStrategy
import io.github.chalexey.cashpet.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Демо-режим для эксперта (docs/01-функционал.md, раздел 9; п. 2.5.13 ТЗ). Запускается из раздела взрослого,
 * играет на отдельном демо-профиле по обычным правилам — все задания открыты сразу (это делает движок).
 *
 * «Ускорить» прогоняет недели по «разумной игре» тем же движком до Взрослого и отдаёт итог каждой недели.
 * Открытое демо запоминается в настройках: после перезапуска приложение продолжает демо (шаг 11 Приложения А).
 */
class DemoViewModel(
    private val content: GameContent,
    private val engine: GameEngine,
    private val store: GameStore,
    private val settings: SettingsRepository,
) : ViewModel() {

    private data class Local(
        val running: Boolean = false,
        val weeks: List<WeekSummaryUiState> = emptyList(),
        val navigate: DemoNav? = null,
    )

    private val local = MutableStateFlow(Local())

    val uiState: StateFlow<DemoUiState> = combine(store.state, local) { state, l -> toUi(state, l) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, toUi(store.state.value, Local()))

    /** «Запустить демо» в разделе взрослого: открыть демо-профиль — или онбординг, если его нет. */
    fun start() {
        viewModelScope.launch {
            settings.setDemoActive(true)
            val saved = store.open(Slot.DEMO)
            local.value = local.value.copy(navigate = if (saved != null) DemoNav.HOME else DemoNav.ONBOARDING)
        }
    }

    /** «Ускорить»: недели по «разумной игре» до Взрослого — баланс, копилка и очки роста по обычным правилам. */
    fun speedUp() {
        if (!uiState.value.canSpeedUp) return
        local.value = local.value.copy(running = true)
        viewModelScope.launch {
            val steps = store.autoplay(ReasonableStrategy(content.economy, content.catalog), Stage.ADULT)
            val weeks = steps.mapNotNull { step ->
                step.feedback.weekResult?.let { weekSummaryUi(it, step.state, content, engine) }
            }
            local.value = local.value.copy(running = false, weeks = weeks)
        }
    }

    /** Итоги недель прогона показаны. */
    fun onWeeksShown() {
        local.value = local.value.copy(weeks = emptyList())
    }

    /** «Сбросить демо» — после подтверждения: демо-профиль к исходному состоянию, то есть снова онбординг. */
    fun reset() {
        viewModelScope.launch {
            store.delete(Slot.DEMO)
            local.value = Local(navigate = DemoNav.ONBOARDING)
        }
    }

    /** «Выйти из демо»: снова профиль ребёнка — через стартовый экран. Демо-профиль остаётся до сброса. */
    fun exit() {
        viewModelScope.launch {
            settings.setDemoActive(false)
            store.open(Slot.CHILD)
            local.value = Local(navigate = DemoNav.START)
        }
    }

    /** Экран перешёл — событие обработано. */
    fun onNavigated() {
        local.value = local.value.copy(navigate = null)
    }

    private fun toUi(state: GameState?, l: Local): DemoUiState {
        val demo = state?.takeIf { it.profile.slot == Slot.DEMO }
        return DemoUiState(
            active = demo != null,
            stage = demo?.pet?.stage,
            canSpeedUp = demo != null && demo.pet.stage < Stage.ADULT && !l.running,
            running = l.running,
            weeks = l.weeks,
            navigate = l.navigate,
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this[APPLICATION_KEY]!!.appContainer
                DemoViewModel(c.content, c.engine, c.gameStore, c.settingsRepository)
            }
        }
    }
}
