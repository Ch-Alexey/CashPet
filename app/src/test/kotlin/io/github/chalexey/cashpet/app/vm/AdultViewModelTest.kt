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
import io.github.chalexey.cashpet.core.model.Topic
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
class AdultViewModelTest {

    private val content = ContentLoader().load()
    private val engine = GameEngine(content.economy, content.catalog)
    private val repository = FakeGameRepository()
    private val store = GameStore(engine, repository)
    private val settings = SettingsRepository(FakeSettingsDao())
    private lateinit var vm: AdultViewModel

    private val adult get() = vm.uiState.value
    private val demoActive get() = runBlocking { settings.settings.first().demoActive }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = AdultViewModel(content, store, settings)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun newGame(slot: Slot, difficulty: Difficulty = Difficulty.EASY) = runBlocking {
        store.newGame(Profile("Лис", difficulty, slot), "Пончик", PetLook("fluffy", "ginger"))
    }

    private fun act(vararg actions: Action) = runBlocking { actions.forEach { store.dispatch(it) } }

    @Test
    fun `профиля нет — прогресса нет, сбрасывать нечего`() {
        assertEquals(AdultUiState(), adult)

        vm.resetProfile()
        vm.deleteProfile()
        assertNull(adult.navigate)
    }

    @Test
    fun `новый профиль — имена, сложность, задания по темам без вводного`() {
        newGame(Slot.CHILD, Difficulty.HARD)

        assertTrue(adult.hasProfile)
        assertFalse(adult.demo)
        assertEquals("Лис" to "Пончик", adult.playerName to adult.petName)
        assertEquals(Difficulty.HARD, adult.difficulty)
        assertEquals(0, adult.weeksPlayed)
        assertEquals(0 to content.tasks.size, adult.tasksDone to adult.tasksTotal)
        assertEquals(listOf(Topic.BUDGET, Topic.SAVINGS, Topic.PURCHASES), adult.tasksByTopic.keys.toList())
        assertTrue(adult.tasksByTopic.values.all { it.done == 0 && it.total == 2 })
    }

    @Test
    fun `прогресс без оценок — неудачный выбор засчитан так же, как удачный`() {
        newGame(Slot.CHILD)
        act(
            Action.CompleteTask("0", Outcome.GOOD),
            Action.CompleteTask("1.1", Outcome.RETRY),
            Action.CloseWeek,
            Action.CompleteTask("4.2", Outcome.OK),
            Action.CloseWeek,
        )

        assertEquals(2, adult.weeksPlayed)
        assertEquals(3, adult.tasksDone)
        assertEquals(TopicProgressUi(done = 2, total = 2), adult.tasksByTopic[Topic.PURCHASES])
        assertEquals(TopicProgressUi(done = 0, total = 2), adult.tasksByTopic[Topic.BUDGET])
    }

    @Test
    fun `купленные мечты и стадия`() {
        newGame(Slot.CHILD)
        act(
            Action.SetPlan(Plan(0, 0, 100)), Action.ConfirmPlan,
            Action.ChooseGoal("goal_scratcher"), Action.Deposit(60), Action.BuyGoal,
        )

        assertEquals(1, adult.goalsBought)
        assertEquals(store.state.value!!.pet.stage, adult.stage)
    }

    @Test
    fun `«Сбросить профиль» ребёнка — сохранение стёрто, онбординг`() {
        newGame(Slot.CHILD)
        act(Action.CloseWeek)

        vm.resetProfile()

        assertEquals(AdultNav.ONBOARDING, adult.navigate)
        assertFalse(Slot.CHILD in repository.saved)
        assertFalse(adult.hasProfile)
        assertFalse(adult.demo)                             // онбординг — в профиле ребёнка

        vm.onNavigated()
        assertNull(adult.navigate)
    }

    @Test
    fun `«Удалить профиль» ребёнка — на старт, демо-профиль не тронут`() {
        newGame(Slot.DEMO)
        newGame(Slot.CHILD)

        vm.deleteProfile()

        assertEquals(AdultNav.START, adult.navigate)
        assertFalse(Slot.CHILD in repository.saved)
        assertTrue(Slot.DEMO in repository.saved)
    }

    @Test
    fun `в демо — прогресс демо-профиля, сброс оставляет демо и не трогает ребёнка`() {
        newGame(Slot.CHILD)
        runBlocking { settings.setDemoActive(true) }
        newGame(Slot.DEMO)
        act(Action.CompleteTask("3.1", Outcome.GOOD))       // в демо открыты все задания

        assertTrue(adult.demo)
        assertEquals(1, adult.tasksByTopic.getValue(Topic.SAVINGS).done)

        vm.resetProfile()

        assertEquals(AdultNav.ONBOARDING, adult.navigate)
        assertTrue(adult.demo)                              // онбординг — в демо
        assertFalse(Slot.DEMO in repository.saved)
        assertTrue(Slot.CHILD in repository.saved)
    }

    @Test
    fun `в демо «Удалить профиль» — демо закрыто, снова профиль ребёнка`() {
        val child = newGame(Slot.CHILD)
        runBlocking { settings.setDemoActive(true) }
        newGame(Slot.DEMO)

        vm.deleteProfile()

        assertEquals(AdultNav.START, adult.navigate)
        assertFalse(demoActive)
        assertFalse(adult.demo)
        assertEquals(child, store.state.value)
        assertFalse(Slot.DEMO in repository.saved)
    }
}
