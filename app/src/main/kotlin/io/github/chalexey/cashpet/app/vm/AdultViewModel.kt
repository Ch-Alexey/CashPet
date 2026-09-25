package io.github.chalexey.cashpet.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.chalexey.cashpet.app.appContainer
import io.github.chalexey.cashpet.app.store.GameStore
import io.github.chalexey.cashpet.content.GameContent
import io.github.chalexey.cashpet.core.model.GameState
import io.github.chalexey.cashpet.core.model.Slot
import io.github.chalexey.cashpet.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Раздел взрослого после барьера (docs/01-функционал.md, раздел 8; п. 2.5.12 ТЗ): прогресс без оценок,
 * сброс и удаление профиля. Работает с открытым профилем: обычно это профиль ребёнка, в демо — демо-профиль,
 * поэтому шаг 12 Приложения А («сброс тестового профиля») проходит здесь же. Демо-кнопки — [DemoViewModel].
 */
class AdultViewModel(
    private val content: GameContent,
    private val store: GameStore,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val navigate = MutableStateFlow<AdultNav?>(null)

    val uiState: StateFlow<AdultUiState> = combine(store.state, settings.settings, navigate) { state, s, nav ->
        toUi(state, s.demoActive, nav)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, toUi(store.state.value, false, null))

    /** «Сбросить профиль» — после подтверждения: прогресс стирается, дальше онбординг в том же слоте. */
    fun resetProfile() {
        val slot = store.state.value?.profile?.slot ?: return
        viewModelScope.launch {
            store.delete(slot)
            navigate.value = AdultNav.ONBOARDING
        }
    }

    /**
     * «Удалить профиль» — после подтверждения: данные стираются с устройства, дальше старт.
     * Удалили демо — демо закрыто, снова профиль ребёнка.
     */
    fun deleteProfile() {
        val slot = store.state.value?.profile?.slot ?: return
        viewModelScope.launch {
            store.delete(slot)
            if (slot == Slot.DEMO) {
                settings.setDemoActive(false)
                store.open(Slot.CHILD)
            }
            navigate.value = AdultNav.START
        }
    }

    /** Экран перешёл — событие обработано. */
    fun onNavigated() {
        navigate.value = null
    }

    private fun toUi(state: GameState?, demoActive: Boolean, nav: AdultNav?): AdultUiState {
        if (state == null) return AdultUiState(demo = demoActive, navigate = nav)
        return AdultUiState(
            hasProfile = true,
            demo = state.profile.slot == Slot.DEMO,
            playerName = state.profile.playerName,
            petName = state.pet.name,
            difficulty = state.profile.difficulty,
            stage = state.pet.stage,
            weeksPlayed = state.history.size,
            tasksDone = state.tasks.size,
            tasksTotal = content.tasks.size,
            tasksByTopic = tasksByTopic(state, content),
            goalsBought = state.savings.boughtGoalIds.size,
            navigate = nav,
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this[APPLICATION_KEY]!!.appContainer
                AdultViewModel(c.content, c.gameStore, c.settingsRepository)
            }
        }
    }
}
