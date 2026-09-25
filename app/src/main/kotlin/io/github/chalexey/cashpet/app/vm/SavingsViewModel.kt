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
import io.github.chalexey.cashpet.core.engine.Rejection
import io.github.chalexey.cashpet.core.engine.Result
import io.github.chalexey.cashpet.core.model.GameState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Копилка (docs/01-функционал.md, раздел 4.3; п. 2.5.7 ТЗ): одна активная мечта из каталога,
 * пополнение +5 / +10 / +20, снятие с предпросмотром «было → станет», «Купить мечту».
 * После каждого действия — панель «что изменилось» ([SavingsUiState.feedback]) или причина отказа.
 */
class SavingsViewModel(
    private val content: GameContent,
    private val engine: GameEngine,
    private val store: GameStore,
) : ViewModel() {

    private data class Message(val feedback: FeedbackUi? = null, val rejection: Rejection? = null)

    private val message = MutableStateFlow(Message())

    val uiState: StateFlow<SavingsUiState?> = combine(store.state, message) { state, msg ->
        state?.let { toUi(it, msg) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, store.state.value?.let { toUi(it, Message()) })

    fun chooseGoal(goalId: String) = act(Action.ChooseGoal(goalId), item = content.catalog.goal(goalId)?.name)

    /** «Пополнить»: шаги +5 / +10 / +20. До плана — отказ PlanNotConfirmed, экран ведёт на план. */
    fun deposit(amount: Int) = act(Action.Deposit(amount), amount = amount)

    /** Что будет после снятия — показать до подтверждения; null — снять столько нельзя. */
    fun previewWithdraw(amount: Int): WithdrawPreviewUi? {
        val state = store.state.value ?: return null
        if (amount <= 0 || amount > state.savings.total) return null
        val p = engine.previewWithdraw(state, amount)
        return WithdrawPreviewUi(amount, p.savedBefore, p.savedAfter, p.weeksBefore, p.weeksAfter)
    }

    /** «Снять» — после предпросмотра и подтверждения. */
    fun withdraw(amount: Int) = act(Action.Withdraw(amount), amount = amount)

    /** «Купить мечту» — после подтверждения. Стоимость — из копилки, кошелёк не меняется. */
    fun buyGoal() {
        val goalName = store.state.value?.savings?.activeGoalId?.let(content.catalog::goal)?.name
        act(Action.BuyGoal, item = goalName)
    }

    /** Панель или сообщение показаны — убрать. */
    fun onMessageShown() {
        message.value = Message()
    }

    private fun act(action: Action, amount: Int? = null, item: String? = null) {
        viewModelScope.launch {
            message.value = when (val result = store.dispatch(action)) {
                is Result.Ok -> Message(feedback = feedbackUi(result, content, amount, item))
                is Result.Rejected -> Message(rejection = result.reason)
            }
        }
    }

    private fun toUi(state: GameState, msg: Message): SavingsUiState {
        val active = state.savings.activeGoalId?.let(content.catalog::goal)
        return SavingsUiState(
            top = topBar(state, content),
            planConfirmed = state.week.planConfirmed,
            total = state.savings.total,
            goal = active?.let { goalUi(it, state) },
            goals = content.catalog.goals.map { goalUi(it, state) },
            weeksLeft = engine.weeksToGoal(state),
            canBuyGoal = active != null && state.savings.total >= active.cost,
            wallet = state.balance,
            feedback = msg.feedback,
            rejection = msg.rejection,
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this[APPLICATION_KEY]!!.appContainer
                SavingsViewModel(c.content, c.engine, c.gameStore)
            }
        }
    }
}
