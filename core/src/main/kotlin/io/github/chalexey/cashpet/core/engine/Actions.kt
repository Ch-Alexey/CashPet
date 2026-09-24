package io.github.chalexey.cashpet.core.engine

import io.github.chalexey.cashpet.core.model.FeedbackReason
import io.github.chalexey.cashpet.core.model.GameState
import io.github.chalexey.cashpet.core.model.Outcome
import io.github.chalexey.cashpet.core.model.PetLook
import io.github.chalexey.cashpet.core.model.Plan
import io.github.chalexey.cashpet.core.model.Stat
import io.github.chalexey.cashpet.core.model.Way
import io.github.chalexey.cashpet.core.model.WeekResult

// Действия, отказы и результат движка — docs/08-контракты.md, раздел 4

sealed interface Action {
    data class SetPlan(val plan: Plan) : Action                  // черновик, переживает перезапуск
    data object ConfirmPlan : Action
    data class Buy(val itemId: String) : Action
    data class AddToWishlist(val itemId: String) : Action
    data class RemoveFromWishlist(val itemId: String) : Action
    data class ChooseGoal(val goalId: String) : Action
    data class Deposit(val amount: Int) : Action
    data class Withdraw(val amount: Int) : Action
    data object BuyGoal : Action                                 // «Купить мечту»
    data class CompleteTask(val taskId: String, val outcome: Outcome) : Action  // монеты — только в первый раз
    data class DoJob(val jobId: String) : Action
    data object CloseWeek : Action                               // итог, падение показателей, новая неделя
    data class ChangeLook(val look: PetLook) : Action
}

sealed interface Rejection {
    data class NotEnoughMoney(val missing: Int, val options: List<Way>) : Rejection
    data class PlanExceedsBudget(val excess: Int) : Rejection
    data object PlanAlreadyConfirmed : Rejection
    data object PlanNotConfirmed : Rejection                     // покупки и взносы — только после плана
    data class NotEnoughSavings(val missing: Int) : Rejection
    data object NoActiveGoal : Rejection
    data class GoalNotReached(val missing: Int) : Rejection
    data object GoalAlreadyBought : Rejection                    // мечту уже купили — выбрать её снова нельзя
    data object JobLimitReached : Rejection
    data object TaskClosed : Rejection
    data object InvalidAmount : Rejection                        // ≤ 0 или не кратно 5
    data class UnknownId(val id: String) : Rejection             // товара, цели, задания или подработки нет в каталоге
}

data class Feedback(
    val balanceBefore: Int,
    val balanceAfter: Int,
    val savingsBefore: Int,
    val savingsAfter: Int,
    val statChanges: Map<Stat, Int>,                             // только ненулевые
    val reason: FeedbackReason,
    val weekResult: WeekResult? = null,                          // только у CloseWeek
)

sealed interface Result {
    data class Ok(val state: GameState, val feedback: Feedback) : Result
    data class Rejected(val reason: Rejection) : Result
}

data class WithdrawPreview(
    val savedBefore: Int,
    val savedAfter: Int,
    val weeksBefore: Int?,
    val weeksAfter: Int?,
)
