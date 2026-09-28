package io.github.chalexey.cashpet.app.vm

import io.github.chalexey.cashpet.app.FakeGameRepository
import io.github.chalexey.cashpet.app.store.GameStore
import io.github.chalexey.cashpet.content.ContentLoader
import io.github.chalexey.cashpet.core.engine.Action
import io.github.chalexey.cashpet.core.engine.GameEngine
import io.github.chalexey.cashpet.core.model.Difficulty
import io.github.chalexey.cashpet.core.model.EffectKey
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskPlayViewModelTest {

    private val content = ContentLoader().load()
    private val engine = GameEngine(content.economy, content.catalog)
    private val store = GameStore(engine, FakeGameRepository())

    private val game get() = store.state.value!!

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun newGame(difficulty: Difficulty = Difficulty.EASY) = runBlocking {
        store.newGame(Profile("Лис", difficulty, Slot.CHILD), "Пончик", PetLook("fluffy", "ginger"))
    }

    private fun play(taskId: String) = TaskPlayViewModel(content, store, taskId)

    private val TaskPlayViewModel.ui get() = uiState.value!!

    @Test
    fun `профиля или задания нет — экран возвращается к списку`() {
        assertNull(play("1.1").uiState.value)

        newGame()
        assertNull(play("99").uiState.value)
    }

    @Test
    fun `начало — ситуация с именем кота, учебный кошелёк, реплика соседа`() {
        newGame()

        val ui = play("1.1").ui

        assertEquals("Пустая миска и большой мяч", ui.title)
        assertEquals(1 to 1, ui.step to ui.steps)
        assertTrue(ui.situation.startsWith("Пончик ждёт ужина"))
        assertEquals(30, ui.wallet)
        assertEquals(listOf("food", "ball", "half"), ui.options.map { it.id })
        assertEquals("Торопливый", ui.neighborLine!!.name)
        assertNull(ui.result)
        assertFalse(ui.finished)
    }

    @Test
    fun `вступление — только в первом шаге, с подстановкой`() {
        newGame()

        val ui = play("0").ui

        assertEquals("Пончик получает пять пробных монет. Что с ними сделать?", ui.intro)
        assertEquals(0, ui.savings)
    }

    @Test
    fun `неудачный выбор — последствие в числах, путь восстановления, настоящий баланс не тронут`() {
        newGame()
        val vm = play("1.1")

        vm.choose("ball")

        val result = vm.ui.result!!
        assertEquals(mapOf(EffectKey.WALLET to -30, EffectKey.MOOD to 15), result.effects)
        assertEquals("Пончик играет, но всё ещё хочет есть", result.consequence)
        assertTrue(result.canRetry)
        assertEquals("Мяч можно добавить в «Хочу потом» и купить позже", result.recovery)
        assertEquals(0, vm.ui.wallet)
        assertEquals(100, game.balance)
    }

    @Test
    fun `«Попробовать иначе» — тот же шаг с исходным кошельком`() {
        newGame()
        val vm = play("1.1")
        vm.choose("ball")

        vm.retry()

        assertNull(vm.ui.result)
        assertEquals(30, vm.ui.wallet)
    }

    @Test
    fun `заглушка — диалог с вопросом и кнопками, кнопка-вариант выбирает его`() {
        newGame()
        val vm = play("1.1")

        vm.choose("half")
        val stub = vm.ui.result!!
        assertEquals("Что делаем?", stub.followupPrompt)
        assertEquals(listOf("Купить корм", "Мяч — в «Хочу потом»"), stub.followupButtons)
        assertFalse(stub.canRetry)
        assertEquals(30, vm.ui.wallet)

        vm.onFollowup("Мяч — в «Хочу потом»")
        assertNull(vm.ui.result)                                           // снова выбор

        vm.choose("half")
        vm.onFollowup("Купить корм")
        assertEquals("Миска полная, Пончик радуется", vm.ui.result!!.consequence)
        assertTrue(vm.ui.result!!.followupButtons.isEmpty())
    }

    @Test
    fun `«Дальше» после последнего шага — награда на баланс и отметка о прохождении`() {
        newGame()
        val vm = play("1.1")
        vm.choose("food")

        vm.next()

        assertTrue(vm.ui.finished)
        val f = vm.ui.feedback!!
        assertEquals(100 to 115, f.balanceBefore to f.balanceAfter)
        assertEquals(content.texts.feedback.getValue(FeedbackReason.TASK_REWARD).replace("{amount}", "15"), f.reasonText)
        assertEquals(Outcome.GOOD, game.tasks.getValue("1.1").firstOutcome)
    }

    @Test
    fun `повторное прохождение — без монет, с объяснением`() {
        newGame()
        runBlocking { store.dispatch(Action.CompleteTask("1.1", Outcome.RETRY)) }
        val vm = play("1.1")

        vm.choose("food")
        vm.next()

        assertEquals(content.texts.feedback.getValue(FeedbackReason.TASK_REPEAT), vm.ui.feedback!!.reasonText)
        assertEquals(115, game.balance)
        assertEquals(Outcome.RETRY, game.tasks.getValue("1.1").firstOutcome)   // засчитан первый исход
    }

    @Test
    fun `до выбора «Дальше» и «Попробовать иначе» ничего не делают`() {
        newGame()
        val vm = play("1.1")

        vm.next()
        vm.retry()

        assertNull(vm.ui.result)
        assertFalse(vm.ui.finished)
    }

    @Test
    fun `посложнее — ситуация и числа из hard_variant`() {
        newGame(Difficulty.HARD)
        val vm = play("1.1")

        assertTrue(vm.ui.situation.contains("со скидкой"))
        vm.choose("ball")
        assertEquals(15, vm.ui.wallet)
    }
}
