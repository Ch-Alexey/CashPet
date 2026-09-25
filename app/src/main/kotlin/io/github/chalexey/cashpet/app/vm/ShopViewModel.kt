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
import io.github.chalexey.cashpet.core.engine.Rejection
import io.github.chalexey.cashpet.core.engine.Result
import io.github.chalexey.cashpet.core.model.GameState
import io.github.chalexey.cashpet.core.model.Part
import io.github.chalexey.cashpet.core.model.ShopItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Магазин (docs/01-функционал.md, раздел 4.3): «Нужное», «Желаемое», «Хочу потом».
 * Покупать — после плана; не хватает монет — отказ с вариантами «Что можно сделать?», среди них
 * «взять из копилки» ([buyWithSavings]). После действия — панель «что изменилось» или причина отказа.
 */
class ShopViewModel(
    private val content: GameContent,
    private val store: GameStore,
) : ViewModel() {

    private val message = MutableStateFlow(ActionMessage())

    val uiState: StateFlow<ShopUiState?> = combine(store.state, message) { state, msg ->
        state?.let { toUi(it, msg) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, store.state.value?.let { toUi(it, ActionMessage()) })

    /** «Купить» — после подтверждения. Отмены и возврата нет, путь восстановления — «Хочу потом». */
    fun buy(itemId: String) = act(Action.Buy(itemId), itemId)

    fun addToWishlist(itemId: String) = act(Action.AddToWishlist(itemId), itemId)

    fun removeFromWishlist(itemId: String) = act(Action.RemoveFromWishlist(itemId), itemId)

    /**
     * «Взять из копилки» — после отдельного подтверждения: снять недостающее и купить.
     * Панель одна на оба шага: кошелёк и копилка — от начала до конца.
     */
    fun buyWithSavings(itemId: String) {
        viewModelScope.launch {
            val state = store.state.value ?: return@launch
            val item = content.catalog.item(itemId) ?: return@launch
            // Без плана покупки нет — и снимать незачем
            if (!state.week.planConfirmed) {
                message.value = ActionMessage(rejection = Rejection.PlanNotConfirmed)
                return@launch
            }
            val missing = item.price - state.balance
            if (missing > 0) {
                val taken = store.dispatch(Action.Withdraw(missing))
                if (taken is Result.Rejected) {
                    message.value = ActionMessage(rejection = taken.reason)
                    return@launch
                }
            }
            message.value = when (val bought = store.dispatch(Action.Buy(itemId))) {
                is Result.Ok -> ActionMessage(
                    feedback = feedbackUi(bought, content, item = item.name)
                        .copy(balanceBefore = state.balance, savingsBefore = state.savings.total),
                )
                is Result.Rejected -> ActionMessage(rejection = bought.reason)
            }
        }
    }

    /** Панель или сообщение показаны — убрать. */
    fun onMessageShown() {
        message.value = ActionMessage()
    }

    private fun act(action: Action, itemId: String) {
        val name = content.catalog.item(itemId)?.name
        viewModelScope.launch {
            message.value = when (val result = store.dispatch(action)) {
                is Result.Ok -> ActionMessage(feedback = feedbackUi(result, content, item = name))
                is Result.Rejected -> ActionMessage(rejection = result.reason)
            }
        }
    }

    private fun toUi(state: GameState, msg: ActionMessage): ShopUiState {
        val week = state.week
        val plan = week.plan
        fun card(item: ShopItem) = ShopItemUi(
            id = item.id,
            name = item.name,
            price = item.price,
            part = item.part,
            effects = item.effects,
            affordable = item.price <= state.balance,
        )
        return ShopUiState(
            top = topBar(state, content),
            planConfirmed = week.planConfirmed,
            need = content.catalog.items.filter { it.part == Part.NEED }.map(::card),
            want = content.catalog.items.filter { it.part == Part.WANT }.map(::card),
            wishlist = state.wishlist.mapNotNull(content.catalog::item).map(::card),
            // «По плану на нужное осталось N»; потратили больше плана — 0, расхождение видно на Плане
            planLeftNeed = if (week.planConfirmed && plan != null) maxOf(0, plan.need - week.spentNeed) else null,
            planLeftWant = if (week.planConfirmed && plan != null) maxOf(0, plan.want - week.spentWant) else null,
            feedback = msg.feedback,
            rejection = msg.rejection,
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this[APPLICATION_KEY]!!.appContainer
                ShopViewModel(c.content, c.gameStore)
            }
        }
    }
}
