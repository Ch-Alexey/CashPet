package io.github.chalexey.cashpet.data

import io.github.chalexey.cashpet.core.model.Difficulty
import io.github.chalexey.cashpet.core.model.GameState
import io.github.chalexey.cashpet.core.model.Outcome
import io.github.chalexey.cashpet.core.model.Pet
import io.github.chalexey.cashpet.core.model.PetLook
import io.github.chalexey.cashpet.core.model.PetReason
import io.github.chalexey.cashpet.core.model.Plan
import io.github.chalexey.cashpet.core.model.Profile
import io.github.chalexey.cashpet.core.model.Savings
import io.github.chalexey.cashpet.core.model.Slot
import io.github.chalexey.cashpet.core.model.TaskRecord
import io.github.chalexey.cashpet.core.model.Topic
import io.github.chalexey.cashpet.core.model.Transaction
import io.github.chalexey.cashpet.core.model.TxSource
import io.github.chalexey.cashpet.core.model.Week
import org.junit.Assert.assertEquals
import org.junit.Test

class GameStateCodecTest {

    @Test
    fun `состояние после кодирования читается без потерь`() {
        val state = GameState(
            profile = Profile(playerName = "Лис", difficulty = Difficulty.HARD, slot = Slot.DEMO),
            pet = Pet(name = "Мурка", look = PetLook("smooth", "grey"), totalGp = 240),
            balance = 35,
            savings = Savings(total = 90, activeGoalId = "goal_house", recentWeeklySaved = listOf(30, 35, 25)),
            week = Week(number = 3, available = 120, plan = Plan(50, 35, 35), planConfirmed = true, spentNeed = 50),
            transactions = listOf(
                Transaction(3, TxSource.Purchase("food_basic"), -30, 0, 90, 90),
                Transaction(3, TxSource.GoalPurchase("goal_scratcher"), 0, -60, 90, 30),
            ),
            wishlist = listOf("want_scooter"),
            tasks = mapOf("3.3" to TaskRecord("3.3", Topic.SAVINGS, Outcome.RETRY, 3)),
        )

        assertEquals(state, GameStateCodec.decode(GameStateCodec.encode(state)))
    }

    @Test
    fun `сохранение версии 1 читается`() {
        // Образец сохранения первой версии. Если тест упал после правки моделей core —
        // новое поле без значения по умолчанию или переименование: старые сохранения игроков не прочитаются
        val text = javaClass.getResource("/saves/v1-week2.json")!!.readText()

        val state = GameStateCodec.decode(text)

        assertEquals("Пончик", state.pet.name)
        assertEquals(60, state.balance)
        assertEquals(25, state.savings.total)
        assertEquals(2, state.week.number)
        assertEquals(Plan(50, 35, 30), state.week.plan)
        assertEquals(PetReason.JOYFUL, state.history.single().petReason)
        assertEquals(TxSource.TaskReward("0"), state.transactions[1].source)
        assertEquals(Outcome.GOOD, state.tasks.getValue("0").firstOutcome)
    }
}
