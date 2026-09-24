package io.github.chalexey.cashpet.core.sim

import io.github.chalexey.cashpet.core.TestContent
import io.github.chalexey.cashpet.core.engine.Action
import io.github.chalexey.cashpet.core.engine.Result
import io.github.chalexey.cashpet.core.model.GameState
import io.github.chalexey.cashpet.core.model.PetMood
import io.github.chalexey.cashpet.core.model.PetMood.CALM
import io.github.chalexey.cashpet.core.model.PetMood.HAPPY
import io.github.chalexey.cashpet.core.model.Stage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/**
 * Движок на Kotlin обязан давать те же числа, что таблицы docs/02-экономика.md (tools/econ_sim.py).
 * Если экономика меняется — сначала симулятор и документ, потом эти строки.
 */
class ScenarioTest {
    private val economy = TestContent.economy
    private val catalog = TestContent.catalog
    private val engine = TestContent.engine
    private val goals = listOf("goal_house", "goal_bike")   // как GOALS в симуляторе

    /** Строка таблицы: доступно, заработал, нужное, желаемое, в копилку, копилка, баланс, GP, ΣGP, стадия, кот. */
    private data class Row(
        val available: Int, val earned: Int, val need: Int, val want: Int, val saved: Int,
        val savings: Int, val balance: Int, val gp: Int, val totalGp: Int, val stage: Int, val mood: PetMood,
    )

    private fun play(strategy: Strategy): List<Row> {
        val start = TestContent.newGame()
        val states = Autoplay.weeks(start, engine, strategy, weeks = 5)
        return states.mapIndexed { i, s ->
            val before = if (i == 0) start else states[i - 1]
            val r = s.history.last()
            Row(
                available = before.week.available, earned = r.earned, need = r.factNeed, want = r.factWant,
                saved = r.factSaved, savings = s.savings.total, balance = s.balance - economy.pocketMoney,
                gp = r.gp, totalGp = r.totalGp, stage = r.stageAfter.ordinal + 1, mood = engine.petMood(r.statsBefore),
            )
        }
    }

    private fun check(name: String, strategy: Strategy, expected: List<Row>) =
        expected.zip(play(strategy)).mapIndexed { i, (exp, act) ->
            DynamicTest.dynamicTest("$name, неделя ${i + 1}") { assertEquals(exp, act) }
        }

    @TestFactory
    fun `разумная игра`() = check("разумная", ReasonableStrategy(economy, catalog, goals), listOf(
        Row(100, 55, 50, 25, 25, 25, 55, 100, 100, 1, HAPPY),
        Row(115, 60, 50, 35, 30, 55, 60, 100, 200, 2, HAPPY),
        Row(120, 60, 50, 35, 35, 90, 60, 100, 300, 2, HAPPY),
        Row(120, 35, 50, 35, 35, 25, 35, 100, 400, 3, HAPPY),
        Row(95, 20, 50, 25, 20, 45, 20, 100, 500, 3, HAPPY),
    ))

    @TestFactory
    fun `транжира`() = check("транжира", SpenderStrategy(economy, catalog, goals), listOf(
        Row(100, 30, 50, 80, 0, 0, 0, 40, 40, 1, HAPPY),
        Row(60, 30, 50, 40, 0, 0, 0, 40, 80, 1, HAPPY),
        Row(60, 25, 50, 35, 0, 0, 0, 40, 120, 1, HAPPY),
        Row(60, 25, 50, 35, 0, 0, 0, 40, 160, 2, HAPPY),
        Row(60, 10, 50, 20, 0, 0, 0, 55, 215, 2, HAPPY),
    ))

    @TestFactory
    fun `скопидом`() = check("скопидом", SaverStrategy(economy, catalog, goals), listOf(
        Row(100, 55, 30, 0, 125, 25, 0, 80, 80, 1, HAPPY),
        Row(60, 60, 45, 0, 75, 100, 0, 93, 173, 2, CALM),
        Row(60, 60, 30, 0, 90, 40, 0, 80, 253, 2, CALM),
        Row(60, 35, 45, 0, 50, 90, 0, 93, 346, 2, CALM),
        Row(60, 20, 30, 0, 50, 140, 0, 80, 426, 3, CALM),
    ))

    @Test
    fun `«Ускорить» после любых двух недель доводит до Взрослого не больше чем за 4 недели`() {
        val empty = object : Strategy {                     // ничего не делать, только завершать неделю
            override fun weekActions(state: GameState, engine: io.github.chalexey.cashpet.core.engine.GameEngine) =
                listOf<Action>(Action.CloseWeek)
        }
        val starts = listOf(ReasonableStrategy(economy, catalog), SpenderStrategy(economy, catalog), SaverStrategy(economy, catalog), empty)
        for (first in starts) {
            val afterTwo = Autoplay.weeks(TestContent.newGame(), engine, first, weeks = 2).last()
            val run = Autoplay.run(afterTwo, engine, ReasonableStrategy(economy, catalog), Stage.ADULT, maxWeeks = 6)
            val weeks = run.count { it.feedback.weekResult != null }
            assertEquals(Stage.ADULT, run.last().state.pet.stage, "после ${first::class.simpleName}")
            assertTrue(weeks <= 4, "после ${first::class.simpleName}: $weeks недель")
            // «Ускорить» не оставляет «Копилку» наполовину: откладывает не меньше минимума
            assertTrue(run.mapNotNull { it.feedback.weekResult }.all { it.sPct == 100 })
        }
    }

    @Test
    fun `баланс и копилка не уходят в минус ни при каких действиях`() {
        val random = kotlin.random.Random(2026)
        val ids = catalog.items.map { it.id }
        repeat(300) {
            var s = TestContent.newGame()
            repeat(60) {
                val action = when (random.nextInt(11)) {
                    0 -> Action.SetPlan(io.github.chalexey.cashpet.core.model.Plan(random.nextInt(8) * 5, random.nextInt(8) * 5, random.nextInt(8) * 5))
                    1 -> Action.ConfirmPlan
                    2, 3 -> Action.Buy(ids.random(random))
                    4 -> Action.Deposit(random.nextInt(1, 12) * 5)
                    5 -> Action.Withdraw(random.nextInt(1, 12) * 5)
                    6 -> Action.ChooseGoal(catalog.goals.random(random).id)
                    7 -> Action.BuyGoal
                    8 -> Action.CompleteTask(catalog.tasks.random(random).id, io.github.chalexey.cashpet.core.model.Outcome.OK)
                    9 -> Action.DoJob("help_home")
                    else -> Action.CloseWeek
                }
                val result = engine.apply(s, action)
                if (result is Result.Ok) {
                    val stageBefore = s.pet.stage
                    s = result.state
                    assertTrue(s.balance >= 0, "баланс < 0 после $action")
                    assertTrue(s.savings.total >= 0, "копилка < 0 после $action")
                    assertTrue(s.pet.stage >= stageBefore, "стадия понизилась после $action")
                    result.feedback.weekResult?.let { assertTrue(it.gp in 0..100, "GP ${it.gp}") }
                    with(s.pet.stats) { assertTrue(listOf(satiety, care, mood).all { it in 25..100 }) }
                }
            }
        }
    }
}
