package io.github.chalexey.cashpet.app.vm

import io.github.chalexey.cashpet.app.FakeGameRepository
import io.github.chalexey.cashpet.app.store.GameStore
import io.github.chalexey.cashpet.content.ContentLoader
import io.github.chalexey.cashpet.core.engine.Action
import io.github.chalexey.cashpet.core.engine.GameEngine
import io.github.chalexey.cashpet.core.engine.Rejection
import io.github.chalexey.cashpet.core.model.Difficulty
import io.github.chalexey.cashpet.core.model.FeedbackReason
import io.github.chalexey.cashpet.core.model.Outcome
import io.github.chalexey.cashpet.core.model.PetLook
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {

    private val content = ContentLoader().load()
    private val engine = GameEngine(content.economy, content.catalog)
    private val store = GameStore(engine, FakeGameRepository())
    private lateinit var vm: TasksViewModel

    private val tasks get() = vm.uiState.value!!

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = TasksViewModel(content, engine, store)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun newGame(slot: Slot = Slot.CHILD) = runBlocking {
        store.newGame(Profile("Лис", Difficulty.EASY, slot), "Пончик", PetLook("fluffy", "ginger"))
    }

    private fun act(vararg actions: Action) = runBlocking { actions.forEach { store.dispatch(it) } }

    @Test
    fun `профиля нет — списка нет`() {
        assertNull(vm.uiState.value)
    }

    @Test
    fun `первая неделя — открыты задания первой недели, подработок две`() {
        newGame()

        assertEquals(listOf("0", "1.1"), tasks.open.map { it.taskId })
        assertTrue(tasks.done.isEmpty())
        assertEquals(content.economy.jobsPerWeek, tasks.jobsLeft)
        assertEquals(content.catalog.jobs.first().reward, tasks.jobReward)
    }

    @Test
    fun `пройденное задание — в «пройденных», не исчезает`() {
        newGame()

        act(Action.CompleteTask("0", Outcome.GOOD))

        assertEquals(listOf("1.1"), tasks.open.map { it.taskId })
        assertEquals(listOf("0"), tasks.done.map { it.taskId })
    }

    @Test
    fun `новая неделя — открываются задания второй недели`() {
        newGame()

        act(Action.CloseWeek)

        assertEquals(listOf("0", "1.1", "4.2", "2.3"), tasks.open.map { it.taskId })
    }

    @Test
    fun `в демо открыты все задания`() {
        newGame(Slot.DEMO)

        assertEquals(content.tasks.map { it.id }, tasks.open.map { it.taskId })
    }

    @Test
    fun `подработка — оплата в кошелёк и панель с суммой`() {
        newGame()

        vm.doJob()

        val expected = content.texts.feedback.getValue(FeedbackReason.JOB_REWARD).replace("{amount}", "10")
        assertEquals(expected, tasks.feedback!!.reasonText)
        assertEquals(100 to 110, tasks.feedback!!.balanceBefore to tasks.feedback!!.balanceAfter)
        assertEquals(1, tasks.jobsLeft)
    }

    @Test
    fun `подработок больше лимита — отказ, на новой неделе снова можно`() {
        newGame()
        repeat(content.economy.jobsPerWeek) { vm.doJob() }

        vm.doJob()
        assertEquals(Rejection.JobLimitReached, tasks.rejection)
        assertEquals(0, tasks.jobsLeft)

        act(Action.CloseWeek)
        assertEquals(content.economy.jobsPerWeek, tasks.jobsLeft)
    }
}
