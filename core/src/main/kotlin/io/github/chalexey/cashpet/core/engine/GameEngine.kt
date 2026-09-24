package io.github.chalexey.cashpet.core.engine

import io.github.chalexey.cashpet.core.model.Catalog
import io.github.chalexey.cashpet.core.model.EconomyConfig
import io.github.chalexey.cashpet.core.model.GameState
import io.github.chalexey.cashpet.core.model.PetLook
import io.github.chalexey.cashpet.core.model.PetMood
import io.github.chalexey.cashpet.core.model.PetReason
import io.github.chalexey.cashpet.core.model.PetStats
import io.github.chalexey.cashpet.core.model.Profile

/**
 * Движок игры: чистые функции без Android, времени и корутин.
 * Правила — docs/08-контракты.md, раздел 4; числа — из [economy].
 */
class GameEngine(private val economy: EconomyConfig, private val catalog: Catalog) {

    /** Новая игра: неделя 1, стартовый бюджет. */
    fun newGame(profile: Profile, petName: String, look: PetLook): GameState = TODO("Движок недели — следующий PR Лехи")

    fun apply(state: GameState, action: Action): Result = TODO("Движок недели — следующий PR Лехи")

    /** Срок до цели в неделях; null — нет цели или нечего усреднять. */
    fun weeksToGoal(state: GameState): Int? = TODO("Движок недели — следующий PR Лехи")

    fun previewWithdraw(state: GameState, amount: Int): WithdrawPreview = TODO("Движок недели — следующий PR Лехи")

    fun petMood(stats: PetStats): PetMood = TODO("Движок недели — следующий PR Лехи")

    /** Причина для карточки кота среди недели — только по текущим показателям. */
    fun petReasonNow(stats: PetStats): PetReason = TODO("Движок недели — следующий PR Лехи")

    /** Задание открыто: unlock_week ≤ номер недели или демо-профиль. */
    fun isTaskOpen(state: GameState, taskId: String): Boolean = TODO("Движок недели — следующий PR Лехи")
}
