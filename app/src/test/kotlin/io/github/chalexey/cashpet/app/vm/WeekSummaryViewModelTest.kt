package io.github.chalexey.cashpet.app.vm

import io.github.chalexey.cashpet.app.FakeGameRepository
import io.github.chalexey.cashpet.app.store.GameStore
import io.github.chalexey.cashpet.content.ContentLoader
import io.github.chalexey.cashpet.content.WeekHint
import io.github.chalexey.cashpet.core.engine.Action
import io.github.chalexey.cashpet.core.engine.GameEngine
import io.github.chalexey.cashpet.core.model.Difficulty
import io.github.chalexey.cashpet.core.model.GrowthIcon
import io.github.chalexey.cashpet.core.model.Part
import io.github.chalexey.cashpet.core.model.PetLook
import io.github.chalexey.cashpet.core.model.PetReason
import io.github.chalexey.cashpet.core.model.Plan
import io.github.chalexey.cashpet.core.model.Profile
import io.github.chalexey.cashpet.core.model.Slot
import io.github.chalexey.cashpet.core.model.Stage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeekSummaryViewModelTest {

    private val content = ContentLoader().load()
    private val engine = GameEngine(content.economy, content.catalog)
    private val store = GameStore(engine, FakeGameRepository())
    private lateinit var vm: WeekSummaryViewModel

    private val summary get() = vm.uiState.value!!

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = WeekSummaryViewModel(content, engine, store)
        runBlocking { store.newGame(Profile("Лис", Difficulty.EASY, Slot.CHILD), "Пончик", PetLook("fluffy", "ginger")) }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun act(vararg actions: Action) = runBlocking { actions.forEach { store.dispatch(it) } }

    private fun hint(h: WeekHint) = content.texts.weekHints.getValue(h)

    /** Разумная неделя: нужное 50 (корм и шампунь), желаемое 15, в копилку 25 — всё по плану. */
    private val goodWeek = arrayOf(
        Action.SetPlan(Plan(50, 25, 25)), Action.ConfirmPlan,
        Action.Buy("food_basic"), Action.Buy("care_shampoo"), Action.Buy("want_ball"), Action.Deposit(25),
    )

    @Test
    fun `недель ещё не закрывали — итога нет`() {
        assertNull(vm.uiState.value)
    }

    @Test
    fun `разумная неделя — план и факт, три иконки горят, подсказки нет`() {
        act(*goodWeek, Action.CloseWeek)

        assertEquals(1, summary.weekNumber)
        assertEquals(
            listOf(PlanFactRowUi(Part.NEED, 50, 50), PlanFactRowUi(Part.WANT, 25, 15), PlanFactRowUi(Part.SAVE, 25, 25)),
            summary.rows,
        )
        assertEquals(
            listOf(GrowthIconUi(GrowthIcon.NEED, 100), GrowthIconUi(GrowthIcon.PLAN, 100), GrowthIconUi(GrowthIcon.SAVE, 100)),
            summary.icons,
        )
        assertEquals(content.economy.stageThresholds.getValue(Stage.TEEN) - 100, summary.gpToNextStage)
        assertNull(summary.stageUp)
        assertNull(summary.recoveryHint)
    }

    @Test
    fun `что стало с котом — до и после падения, причина с именем`() {
        act(*goodWeek, Action.CloseWeek)
        val result = store.state.value!!.history.last()

        assertEquals(result.statsBefore, summary.pet.before)
        assertEquals(result.statsAfter, summary.pet.after)
        assertEquals(engine.petMood(result.statsBefore), summary.pet.mood)
        assertEquals(content.texts.petWeek.getValue(result.petReason).replace("{petName}", "Пончик"), summary.pet.reasonText)
    }

    @Test
    fun `плана не было — «План удался» не горит, подсказка начать с плана`() {
        act(Action.CloseWeek)

        assertFalse(summary.planConfirmed)
        assertEquals(0, summary.icons.single { it.icon == GrowthIcon.PLAN }.pct)
        assertEquals(hint(WeekHint.NO_PLAN), summary.recoveryHint)
        assertEquals(content.texts.petWeek.getValue(PetReason.HUNGRY), summary.pet.reasonText)
    }

    @Test
    fun `траты вышли за допуск — подсказка про новый план`() {
        act(
            Action.SetPlan(Plan(50, 0, 50)), Action.ConfirmPlan,
            Action.Buy("food_basic"), Action.Buy("care_shampoo"), Action.Buy("want_bed"),   // «Хочу» 40 при плане 0
            Action.CloseWeek,
        )

        assertEquals(50, summary.icons.single { it.icon == GrowthIcon.PLAN }.pct)
        assertEquals(hint(WeekHint.PLAN), summary.recoveryHint)
    }

    @Test
    fun `отложили меньше плана — подсказка про копилку`() {
        act(
            Action.SetPlan(Plan(50, 25, 25)), Action.ConfirmPlan,
            Action.Buy("food_basic"), Action.Buy("care_shampoo"), Action.Deposit(10),
            Action.CloseWeek,
        )

        assertEquals(40, summary.icons.single { it.icon == GrowthIcon.SAVE }.pct)
        assertEquals(hint(WeekHint.SAVE), summary.recoveryHint)
    }

    @Test
    fun `две разумные недели — праздник стадии, цель с полоской`() {
        act(*goodWeek, Action.ChooseGoal("goal_house"), Action.CloseWeek)
        act(
            Action.SetPlan(Plan(50, 0, 20)), Action.ConfirmPlan,
            Action.Buy("food_basic"), Action.Buy("care_shampoo"), Action.Deposit(20),
            Action.CloseWeek,
        )

        assertEquals(2, summary.weekNumber)
        assertEquals(Stage.TEEN, summary.stageUp)
        assertEquals(content.economy.stageThresholds.getValue(Stage.ADULT) - 200, summary.gpToNextStage)
        assertEquals(45, summary.goal!!.saved)
        assertEquals(4, summary.goal!!.filledCells)
    }
}
