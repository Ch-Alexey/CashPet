package io.github.chalexey.cashpet.app.vm

import io.github.chalexey.cashpet.app.FakeGameRepository
import io.github.chalexey.cashpet.app.store.GameStore
import io.github.chalexey.cashpet.content.ContentLoader
import io.github.chalexey.cashpet.core.engine.Action
import io.github.chalexey.cashpet.core.engine.GameEngine
import io.github.chalexey.cashpet.core.model.Difficulty
import io.github.chalexey.cashpet.core.model.Outcome
import io.github.chalexey.cashpet.core.model.PetLook
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val content = ContentLoader().load()
    private val engine = GameEngine(content.economy, content.catalog)
    private val store = GameStore(engine, FakeGameRepository())
    private lateinit var vm: HomeViewModel

    private val home get() = vm.uiState.value!!

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = HomeViewModel(content, engine, store)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun newGame() = runBlocking {
        store.newGame(Profile("Лис", Difficulty.EASY, Slot.CHILD), "Пончик", PetLook("fluffy", "ginger"))
    }

    private fun act(vararg actions: Action) = runBlocking { actions.forEach { store.dispatch(it) } }

    @Test
    fun `профиля нет — Дома нет, экран ведёт в онбординг`() {
        assertNull(vm.uiState.value)
    }

    @Test
    fun `новая игра — все шесть элементов главного экрана`() {
        newGame()

        // п. 2.5.3 ТЗ: кот, баланс, накопления, текущая цель, показатели, активное задание
        assertEquals("Пончик", home.petName)
        assertEquals(PetLook("fluffy", "ginger"), home.look)
        assertEquals(Stage.BABY, home.stage)
        assertEquals(content.economy.startBudget, home.top.balance)
        assertEquals(0, home.top.savings)
        assertNull(home.top.goalName)                       // «Выбери мечту»
        assertEquals(content.economy.stats.start, home.stats.satiety)
        assertEquals("0", home.activeTask!!.taskId)         // «Первый день»
        assertEquals(1, home.weekNumber)
    }

    @Test
    fun `фраза «почему сейчас так» — из texts_json, с именем кота`() {
        newGame()

        val expected = content.texts.petNow.getValue(engine.petReasonNow(home.stats)).replace("{petName}", "Пончик")
        assertEquals(expected, home.petReasonText)
        assertFalse(home.petReasonText, "{" in home.petReasonText)
        assertEquals(engine.petMood(home.stats), home.mood)
    }

    @Test
    fun `до плана «Завершить неделю» спрашивает про план`() {
        newGame()

        assertEquals(CloseWeekWarning.NO_PLAN, home.closeWarning)
        assertTrue(home.needWarning)
    }

    @Test
    fun `план есть, нужное не куплено — мягкое предупреждение`() {
        newGame()
        act(Action.SetPlan(Plan(50, 25, 25)), Action.ConfirmPlan)

        assertEquals(CloseWeekWarning.NEED_NOT_BOUGHT, home.closeWarning)
    }

    @Test
    fun `еда и уход куплены — предупреждений нет, показатели выросли`() {
        newGame()
        act(Action.SetPlan(Plan(50, 25, 25)), Action.ConfirmPlan, Action.Buy("food_basic"), Action.Buy("care_shampoo"))

        assertNull(home.closeWarning)
        assertFalse(home.needWarning)
        assertEquals(50, home.top.balance)
        assertEquals(100, home.stats.satiety)
    }

    @Test
    fun `цель и копилка — в верхней панели с полоской`() {
        newGame()
        act(Action.SetPlan(Plan(50, 0, 50)), Action.ConfirmPlan, Action.ChooseGoal("goal_house"), Action.Deposit(25))

        assertEquals("Домик", home.top.goalName)
        assertEquals(100, home.top.goalCost)
        assertEquals(25, home.top.savings)
        assertEquals(25, home.top.goalProgressPct)
    }

    @Test
    fun `активное задание — следующее открытое и не пройденное`() {
        newGame()
        act(Action.CompleteTask("0", Outcome.GOOD))
        assertEquals("1.1", home.activeTask!!.taskId)

        act(Action.CompleteTask("1.1", Outcome.GOOD))
        assertNull(home.activeTask)                         // задания 2-й недели ещё закрыты
    }

    @Test
    fun `«Завершить неделю» — неделя 2, событие для «Итога недели» один раз`() {
        newGame()

        vm.closeWeek()
        assertTrue(home.weekClosed)
        assertEquals(2, home.weekNumber)

        vm.onWeekSummaryOpened()
        assertFalse(home.weekClosed)
    }
}
