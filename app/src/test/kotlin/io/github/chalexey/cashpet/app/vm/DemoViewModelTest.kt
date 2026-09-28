package io.github.chalexey.cashpet.app.vm

import io.github.chalexey.cashpet.app.FakeGameRepository
import io.github.chalexey.cashpet.app.FakeSettingsDao
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
import io.github.chalexey.cashpet.core.sim.ReasonableStrategy
import io.github.chalexey.cashpet.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class DemoViewModelTest {

    private val content = ContentLoader().load()
    private val engine = GameEngine(content.economy, content.catalog)
    private val repository = FakeGameRepository()
    private val store = GameStore(engine, repository)
    private val settings = SettingsRepository(FakeSettingsDao())
    private lateinit var vm: DemoViewModel

    private val demo get() = vm.uiState.value
    private val game get() = store.state.value!!
    private val demoActive get() = runBlocking { settings.settings.first().demoActive }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = DemoViewModel(content, engine, store, settings)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun newGame(slot: Slot) = runBlocking {
        store.newGame(Profile("Лис", Difficulty.EASY, slot), "Пончик", PetLook("fluffy", "ginger"))
    }

    private fun act(vararg actions: Action) = runBlocking { actions.forEach { store.dispatch(it) } }

    @Test
    fun `открыт профиль ребёнка — демо не активно, «Ускорить» нет`() {
        newGame(Slot.CHILD)

        assertFalse(demo.active)
        assertFalse(demo.canSpeedUp)
    }

    @Test
    fun `«Запустить демо» впервые — онбординг в демо, флаг в настройках`() {
        newGame(Slot.CHILD)

        vm.start()

        assertEquals(DemoNav.ONBOARDING, demo.navigate)
        assertTrue(demoActive)
        assertNull(store.state.value)                       // профиль ребёнка закрыт, но не удалён
        assertTrue(Slot.CHILD in repository.saved)

        vm.onNavigated()
        assertNull(demo.navigate)
    }

    @Test
    fun `демо-профиль уже есть — продолжаем с того же места`() {
        newGame(Slot.DEMO)
        act(Action.CloseWeek)
        newGame(Slot.CHILD)

        vm.start()

        assertEquals(DemoNav.HOME, demo.navigate)
        assertTrue(demo.active)
        assertEquals(2, game.week.number)
    }

    @Test
    fun `«Ускорить» — недели по разумной игре до Взрослого, итог каждой недели`() {
        newGame(Slot.DEMO)
        assertTrue(demo.canSpeedUp)

        vm.speedUp()

        assertEquals(Stage.ADULT, game.pet.stage)
        assertEquals(Stage.ADULT, demo.stage)
        assertEquals(game.history.size, demo.weeks.size)
        assertEquals((1..game.history.size).toList(), demo.weeks.map { it.weekNumber })
        assertEquals(Stage.ADULT, demo.weeks.last().stageUp)
        assertTrue("стадия 3 — к 4-й неделе", demo.weeks.size <= 4)
        assertFalse(demo.canSpeedUp)                        // дальше расти некуда
        assertFalse(demo.running)
        assertTrue(game.balance >= 0 && game.savings.total >= 0)
        assertEquals(game, repository.saved[Slot.DEMO])     // прогон сохранён — переживёт перезапуск

        vm.onWeeksShown()
        assertTrue(demo.weeks.isEmpty())
    }

    @Test
    fun `«Ускорить» — ровно то же, что те же решения вручную по одному`() {
        newGame(Slot.DEMO)
        val strategy = ReasonableStrategy(content.economy, content.catalog)
        val manual = GameStore(engine, FakeGameRepository())
        runBlocking {
            manual.newGame(Profile("Лис", Difficulty.EASY, Slot.DEMO), "Пончик", PetLook("fluffy", "ginger"))
            while (manual.state.value!!.pet.stage < Stage.ADULT) {
                strategy.weekActions(manual.state.value!!, engine).forEach { manual.dispatch(it) }
            }
        }

        vm.speedUp()

        assertEquals(manual.state.value, game)
    }

    @Test
    fun `«Ускорить» посреди недели — доигрывает начатую неделю по её плану`() {
        newGame(Slot.DEMO)
        act(
            Action.SetPlan(Plan(50, 25, 25)), Action.ConfirmPlan,
            Action.Buy("food_basic"), Action.CompleteTask("0", Outcome.GOOD),
        )

        vm.speedUp()

        val first = game.history.first()
        assertEquals(Plan(50, 25, 25), first.plan)
        assertEquals(50, first.factNeed)                    // корм не куплен второй раз, докуплен только уход
        assertEquals(Stage.ADULT, game.pet.stage)
    }

    @Test
    fun `после ручных недель «Ускорить» доводит до Взрослого с текущей стадии`() {
        newGame(Slot.DEMO)
        vm.speedUp()
        val weeksToAdult = game.history.size

        vm.reset()
        newGame(Slot.DEMO)
        act(
            Action.SetPlan(Plan(50, 25, 25)), Action.ConfirmPlan,
            Action.Buy("food_basic"), Action.Buy("care_shampoo"), Action.Deposit(25), Action.CloseWeek,
        )
        vm.speedUp()

        assertEquals(Stage.ADULT, game.pet.stage)
        assertEquals(weeksToAdult - 1, demo.weeks.size)     // одна неделя уже пройдена вручную
    }

    @Test
    fun `«Ускорить» только в демо — в профиле ребёнка ничего не делает`() {
        newGame(Slot.CHILD)

        vm.speedUp()

        assertEquals(1, game.week.number)
        assertTrue(demo.weeks.isEmpty())
    }

    @Test
    fun `«Сбросить демо» — к исходному состоянию, профиль удалён, снова онбординг, ребёнок не тронут`() {
        newGame(Slot.CHILD)
        vm.start()
        newGame(Slot.DEMO)
        vm.speedUp()

        vm.reset()

        assertEquals(DemoNav.ONBOARDING, demo.navigate)
        assertFalse(Slot.DEMO in repository.saved)
        assertTrue(Slot.CHILD in repository.saved)
        assertNull(store.state.value)
        assertTrue(demoActive)                              // онбординг — ещё в демо
    }

    @Test
    fun `«Выйти из демо» — снова профиль ребёнка, демо-профиль остаётся`() {
        val child = newGame(Slot.CHILD)
        vm.start()
        newGame(Slot.DEMO)

        vm.exit()

        assertEquals(DemoNav.START, demo.navigate)
        assertFalse(demo.active)
        assertFalse(demoActive)
        assertEquals(child, store.state.value)
        assertTrue(Slot.DEMO in repository.saved)
    }
}
