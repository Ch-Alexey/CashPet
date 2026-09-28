package io.github.chalexey.cashpet.app.vm

import io.github.chalexey.cashpet.app.FakeGameRepository
import io.github.chalexey.cashpet.app.store.GameStore
import io.github.chalexey.cashpet.content.ContentLoader
import io.github.chalexey.cashpet.core.engine.Action
import io.github.chalexey.cashpet.core.engine.GameEngine
import io.github.chalexey.cashpet.core.engine.Rejection
import io.github.chalexey.cashpet.core.model.Difficulty
import io.github.chalexey.cashpet.core.model.FeedbackReason
import io.github.chalexey.cashpet.core.model.PetLook
import io.github.chalexey.cashpet.core.model.Plan
import io.github.chalexey.cashpet.core.model.Profile
import io.github.chalexey.cashpet.core.model.Slot
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
class SavingsViewModelTest {

    private val content = ContentLoader().load()
    private val engine = GameEngine(content.economy, content.catalog)
    private val store = GameStore(engine, FakeGameRepository())
    private lateinit var vm: SavingsViewModel

    private val savings get() = vm.uiState.value!!

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = SavingsViewModel(content, engine, store)
        runBlocking { store.newGame(Profile("Лис", Difficulty.EASY, Slot.CHILD), "Пончик", PetLook("fluffy", "ginger")) }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun act(vararg actions: Action) = runBlocking { actions.forEach { store.dispatch(it) } }

    private fun text(reason: FeedbackReason) = content.texts.feedback.getValue(reason)

    @Test
    fun `новая игра — мечты нет, копилка пуста, все мечты каталога`() {
        assertNull(savings.goal)
        assertEquals(0, savings.total)
        assertEquals(content.catalog.goals.map { it.id }, savings.goals.map { it.id })
        assertNull(savings.weeksLeft)
        assertFalse(savings.canBuyGoal)
        assertFalse(savings.planConfirmed)
        assertEquals(content.economy.startBudget, savings.wallet)
    }

    @Test
    fun `выбор мечты — цель с клеточками по 10 и панель «Новая мечта»`() {
        vm.chooseGoal("goal_house")

        val goal = savings.goal!!
        assertEquals("Домик", goal.name)
        assertEquals(10, goal.cells)
        assertEquals(0, goal.filledCells)
        assertEquals(text(FeedbackReason.GOAL_CHOSEN).replace("{item}", "Домик"), savings.feedback!!.reasonText)
        assertNull(savings.rejection)
    }

    @Test
    fun `пополнение до плана — отказ, экран ведёт на план`() {
        vm.chooseGoal("goal_house")

        vm.deposit(10)

        assertEquals(Rejection.PlanNotConfirmed, savings.rejection)
        assertNull(savings.feedback)
        assertEquals(0, savings.total)
    }

    @Test
    fun `пополнение — было и стало, текст с суммой, срок до мечты`() {
        act(Action.SetPlan(Plan(50, 25, 25)), Action.ConfirmPlan)
        vm.chooseGoal("goal_house")

        vm.deposit(20)

        val f = savings.feedback!!
        assertEquals(100 to 80, f.balanceBefore to f.balanceAfter)
        assertEquals(0 to 20, f.savingsBefore to f.savingsAfter)
        assertEquals(text(FeedbackReason.DEPOSIT).replace("{amount}", "20"), f.reasonText)
        assertFalse(f.reasonText, "{" in f.reasonText)
        assertEquals(20, savings.total)
        assertEquals(2, savings.goal!!.filledCells)
        assertEquals(4, savings.weeksLeft)                  // (100 − 20) / 20
    }

    @Test
    fun `пополнение больше кошелька — отказ с недостающей суммой`() {
        act(Action.SetPlan(Plan(50, 25, 25)), Action.ConfirmPlan, Action.Buy("food_basic"), Action.Buy("care_shampoo"))

        vm.deposit(60)

        val rejection = savings.rejection as Rejection.NotEnoughMoney
        assertEquals(10, rejection.missing)
        assertEquals(50, savings.wallet)
    }

    @Test
    fun `предпросмотр снятия «было → станет», копилка не меняется`() {
        act(Action.SetPlan(Plan(50, 25, 25)), Action.ConfirmPlan)
        vm.chooseGoal("goal_house")
        vm.deposit(20)

        val preview = vm.previewWithdraw(10)!!

        assertEquals(WithdrawPreviewUi(amount = 10, savedBefore = 20, savedAfter = 10, weeksBefore = 4, weeksAfter = 9), preview)
        assertEquals(20, savings.total)
        assertNull(vm.previewWithdraw(25))                  // больше, чем в копилке
        assertNull(vm.previewWithdraw(0))
    }

    @Test
    fun `снятие — деньги в кошелёк, панель «Из копилки»`() {
        act(Action.SetPlan(Plan(50, 25, 25)), Action.ConfirmPlan)
        vm.chooseGoal("goal_house")
        vm.deposit(20)

        vm.withdraw(10)

        assertEquals(10, savings.total)
        assertEquals(90, savings.wallet)
        assertEquals(text(FeedbackReason.WITHDRAW).replace("{amount}", "10"), savings.feedback!!.reasonText)
    }

    @Test
    fun `«Купить мечту» — из копилки, отметка «Уже есть», мечту выбирают заново`() {
        act(Action.SetPlan(Plan(0, 0, 100)), Action.ConfirmPlan)
        vm.chooseGoal("goal_scratcher")
        vm.deposit(60)
        assertTrue(savings.canBuyGoal)

        vm.buyGoal()

        assertEquals(text(FeedbackReason.GOAL_BOUGHT).replace("{item}", "Когтеточка"), savings.feedback!!.reasonText)
        assertEquals(40, savings.wallet)                    // кошелёк не меняется
        assertEquals(0, savings.total)
        assertNull(savings.goal)
        assertTrue(savings.goals.single { it.id == "goal_scratcher" }.bought)
        assertFalse(savings.canBuyGoal)
    }

    @Test
    fun `купленную мечту снова не выбрать`() {
        act(
            Action.SetPlan(Plan(0, 0, 100)), Action.ConfirmPlan,
            Action.ChooseGoal("goal_scratcher"), Action.Deposit(60), Action.BuyGoal,
        )

        vm.chooseGoal("goal_scratcher")

        assertEquals(Rejection.GoalAlreadyBought, savings.rejection)
        assertNull(savings.goal)
    }

    @Test
    fun `сообщение показано — панель убирается`() {
        vm.chooseGoal("goal_bike")
        vm.onMessageShown()

        assertNull(savings.feedback)
        assertNull(savings.rejection)
        assertEquals("Велосипед", savings.goal!!.name)
    }
}
