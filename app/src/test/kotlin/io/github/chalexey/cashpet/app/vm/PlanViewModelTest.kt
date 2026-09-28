package io.github.chalexey.cashpet.app.vm

import io.github.chalexey.cashpet.app.FakeGameRepository
import io.github.chalexey.cashpet.app.store.GameStore
import io.github.chalexey.cashpet.content.ContentLoader
import io.github.chalexey.cashpet.core.engine.Action
import io.github.chalexey.cashpet.core.engine.GameEngine
import io.github.chalexey.cashpet.core.model.Difficulty
import io.github.chalexey.cashpet.core.model.Part
import io.github.chalexey.cashpet.core.model.PetLook
import io.github.chalexey.cashpet.core.model.Plan
import io.github.chalexey.cashpet.core.model.Profile
import io.github.chalexey.cashpet.core.model.Slot
import kotlinx.coroutines.CompletableDeferred
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
class PlanViewModelTest {

    private val content = ContentLoader().load()
    private val engine = GameEngine(content.economy, content.catalog)
    private val repository = FakeGameRepository()
    private val store = GameStore(engine, repository)
    private lateinit var vm: PlanViewModel

    private val plan get() = vm.uiState.value!!

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = PlanViewModel(content, store)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun newGame(difficulty: Difficulty = Difficulty.EASY) = runBlocking {
        store.newGame(Profile("Лис", difficulty, Slot.CHILD), "Пончик", PetLook("fluffy", "ginger"))
    }

    private fun act(vararg actions: Action) = runBlocking { actions.forEach { store.dispatch(it) } }

    private fun tap(part: Part, times: Int) = repeat(times) { vm.increase(part) }

    @Test
    fun `профиля нет — плана нет`() {
        assertNull(vm.uiState.value)
    }

    @Test
    fun `новая неделя — всё не разложено, подсказка «коту нужно 50»`() {
        newGame()

        assertEquals(content.economy.startBudget, plan.available)
        assertEquals(Triple(0, 0, 0), Triple(plan.need, plan.want, plan.save))
        assertEquals(plan.available, plan.unallocated)
        assertEquals(content.economy.needMin, plan.needHint)
        assertTrue(plan.canIncrease)
        assertFalse(plan.confirmed)
        assertNull(plan.fact)
    }

    @Test
    fun `сложный уровень — подсказка только в первую неделю`() {
        newGame(Difficulty.HARD)
        assertEquals(content.economy.needMin, plan.needHint)

        act(Action.CloseWeek)
        assertEquals(2, store.state.value!!.week.number)
        assertNull(plan.needHint)
    }

    @Test
    fun `кнопки +5 и −5 меняют черновик и «Не разложено»`() {
        newGame()

        tap(Part.NEED, 10)
        tap(Part.WANT, 5)
        tap(Part.SAVE, 4)
        vm.decrease(Part.SAVE)

        assertEquals(Triple(50, 25, 15), Triple(plan.need, plan.want, plan.save))
        assertEquals(10, plan.unallocated)
        assertEquals(Plan(50, 25, 15), store.state.value!!.week.plan)   // черновик переживает перезапуск
    }

    @Test
    fun `два быстрых «+5», пока первое пишется на диск, — оба засчитаны`() {
        newGame()
        val save = CompletableDeferred<Unit>()
        repository.holdSave = save

        tap(Part.NEED, 2)                    // первое нажатие ждёт записи, второе — своей очереди
        save.complete(Unit)

        assertEquals(10, plan.need)
        assertEquals(Plan(10, 0, 0), store.state.value!!.week.plan)
    }

    @Test
    fun `меньше нуля не опускается`() {
        newGame()

        vm.decrease(Part.WANT)

        assertEquals(0, plan.want)
        assertEquals(plan.available, plan.unallocated)
    }

    @Test
    fun `всё разложено — «+» недоступен, больше доступного не разложить`() {
        newGame()
        tap(Part.NEED, 20)

        assertEquals(0, plan.unallocated)
        assertFalse(plan.canIncrease)

        vm.increase(Part.SAVE)
        assertEquals(0, plan.save)
        assertEquals(100, plan.need)
    }

    @Test
    fun `подтверждение — план закреплён, появляется факт`() {
        newGame()
        tap(Part.NEED, 10)
        tap(Part.WANT, 5)
        tap(Part.SAVE, 5)

        vm.confirm()

        assertTrue(plan.confirmed)
        assertFalse(plan.canIncrease)
        assertEquals(PlanFactUi(spentNeed = 0, spentWant = 0, saved = 0, earned = 0), plan.fact)
    }

    @Test
    fun `после подтверждения план не меняется, факт растёт от покупок и копилки`() {
        newGame()
        act(Action.SetPlan(Plan(50, 25, 25)), Action.ConfirmPlan)

        vm.increase(Part.NEED)
        vm.decrease(Part.WANT)
        act(Action.Buy("food_basic"), Action.ChooseGoal("goal_house"), Action.Deposit(20), Action.Withdraw(5))

        assertEquals(Triple(50, 25, 25), Triple(plan.need, plan.want, plan.save))
        assertEquals(15, plan.fact!!.saved)
        assertTrue(plan.fact!!.spentNeed > 0)
    }

    @Test
    fun `советы соседей — у кого есть plan_tips`() {
        newGame()

        assertEquals(listOf("Торопливый", "Осторожный"), plan.neighborTips.map { it.name })
        assertTrue(plan.neighborTips.all { it.text.isNotBlank() && "{" !in it.text })
    }

    @Test
    fun `черновик виден новому экрану — тот же GameStore`() {
        newGame()
        tap(Part.SAVE, 3)

        val again = PlanViewModel(content, store)

        assertEquals(15, again.uiState.value!!.save)
    }
}
