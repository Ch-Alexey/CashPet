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
import io.github.chalexey.cashpet.core.model.Topic
import io.github.chalexey.cashpet.core.sim.ReasonableStrategy
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
class ProgressViewModelTest {

    private val content = ContentLoader().load()
    private val engine = GameEngine(content.economy, content.catalog)
    private val store = GameStore(engine, FakeGameRepository())
    private lateinit var vm: ProgressViewModel

    private val progress get() = vm.uiState.value!!
    private val teen = content.economy.stageThresholds.getValue(Stage.TEEN)
    private val adult = content.economy.stageThresholds.getValue(Stage.ADULT)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = ProgressViewModel(content, engine, store)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun newGame(slot: Slot = Slot.CHILD) = runBlocking {
        store.newGame(Profile("Лис", Difficulty.EASY, slot), "Пончик", PetLook("fluffy", "ginger"))
    }

    private fun act(vararg actions: Action) = runBlocking { actions.forEach { store.dispatch(it) } }

    /** Разумная неделя: корм, шампунь, мячик, 25 в копилку — 100 очков роста. */
    private val goodWeek = arrayOf(
        Action.SetPlan(Plan(50, 25, 25)), Action.ConfirmPlan,
        Action.Buy("food_basic"), Action.Buy("care_shampoo"), Action.Buy("want_ball"), Action.Deposit(25),
        Action.CloseWeek,
    )

    /** Вторая разумная неделя — по средствам: 10 остатка + 60 карманных. */
    private val goodSecondWeek = arrayOf(
        Action.SetPlan(Plan(50, 0, 20)), Action.ConfirmPlan,
        Action.Buy("food_basic"), Action.Buy("care_shampoo"), Action.Deposit(20),
        Action.CloseWeek,
    )

    @Test
    fun `профиля нет — прогресса нет`() {
        assertNull(vm.uiState.value)
    }

    @Test
    fun `новая игра — Малыш, полоска пустая, недель и мечт нет, словарь весь`() {
        newGame()

        assertEquals(Stage.BABY, progress.stage)
        assertEquals(0, progress.totalGp)
        assertEquals(Stage.TEEN, progress.nextStage)
        assertEquals(teen, progress.gpToNextStage)
        assertEquals(0, progress.stageProgressPct)
        assertEquals(listOf(Topic.BUDGET, Topic.SAVINGS, Topic.PURCHASES), progress.tasksByTopic.keys.toList())
        assertNull(progress.goal)
        assertTrue(progress.boughtGoals.isEmpty())
        assertNull(progress.lastWeek)
        assertTrue(progress.weeks.isEmpty())
        assertEquals(content.glossary.map { it.term }, progress.glossary.map { it.term })
    }

    @Test
    fun `словарь — с именем кота вместо подстановки`() {
        newGame()

        assertTrue(progress.glossary.none { "{" in it.definition })
        assertTrue(progress.glossary.single { it.id == "stage" }.definition.contains("Пончик"))
    }

    @Test
    fun `после недели — полоска растёт, история и итог последней недели`() {
        newGame()

        act(*goodWeek)

        assertEquals(100, progress.totalGp)
        assertEquals(teen - 100, progress.gpToNextStage)
        assertEquals(100 * 100 / teen, progress.stageProgressPct)
        assertEquals(listOf(WeekLineUi(weekNumber = 1, gp = 100, stageAfter = Stage.BABY)), progress.weeks)
        assertEquals(1, progress.lastWeek!!.weekNumber)
        assertEquals(progress.stageProgressPct, progress.lastWeek!!.stageProgressPct)
    }

    @Test
    fun `Подросток — полоска считается от порога Подростка до порога Взрослого`() {
        newGame()

        act(*goodWeek, *goodSecondWeek)

        assertEquals(Stage.TEEN, progress.stage)
        assertEquals(Stage.ADULT, progress.nextStage)
        assertEquals(adult - 200, progress.gpToNextStage)
        assertEquals((200 - teen) * 100 / (adult - teen), progress.stageProgressPct)
        assertEquals(Stage.TEEN, progress.weeks.last().stageAfter)
    }

    @Test
    fun `Взрослый — дальше расти некуда, полоска полная`() {
        newGame(Slot.DEMO)
        runBlocking { store.autoplay(ReasonableStrategy(content.economy, content.catalog), Stage.ADULT) }

        assertEquals(Stage.ADULT, progress.stage)
        assertNull(progress.nextStage)
        assertNull(progress.gpToNextStage)
        assertEquals(100, progress.stageProgressPct)
    }

    @Test
    fun `цели — текущая и достигнутые с отметкой «Уже есть»`() {
        newGame()
        act(
            Action.SetPlan(Plan(0, 0, 100)), Action.ConfirmPlan,
            Action.ChooseGoal("goal_scratcher"), Action.Deposit(60), Action.BuyGoal,
            Action.ChooseGoal("goal_house"), Action.Deposit(40),
        )

        assertEquals("goal_house", progress.goal!!.id)
        assertEquals(40, progress.goal!!.saved)
        assertEquals(listOf("goal_scratcher"), progress.boughtGoals.map { it.id })
        assertTrue(progress.boughtGoals.all { it.bought })
    }

    @Test
    fun `пройденные задания по темам — вводное не считается`() {
        newGame()

        act(Action.CompleteTask("0", Outcome.GOOD), Action.CompleteTask("1.1", Outcome.RETRY))

        assertEquals(TopicProgressUi(done = 1, total = 2), progress.tasksByTopic[Topic.PURCHASES])
        assertFalse(Topic.INTRO in progress.tasksByTopic)
    }
}
