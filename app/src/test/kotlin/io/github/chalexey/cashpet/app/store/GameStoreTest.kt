package io.github.chalexey.cashpet.app.store

import io.github.chalexey.cashpet.content.ContentLoader
import io.github.chalexey.cashpet.core.engine.Action
import io.github.chalexey.cashpet.app.FakeGameRepository
import io.github.chalexey.cashpet.core.engine.GameEngine
import io.github.chalexey.cashpet.core.engine.Rejection
import io.github.chalexey.cashpet.core.engine.Result
import io.github.chalexey.cashpet.core.model.Difficulty
import io.github.chalexey.cashpet.core.model.PetLook
import io.github.chalexey.cashpet.core.model.Plan
import io.github.chalexey.cashpet.core.model.Profile
import io.github.chalexey.cashpet.core.model.Slot
import io.github.chalexey.cashpet.core.model.Stage
import io.github.chalexey.cashpet.core.sim.ReasonableStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStoreTest {


    // Настоящий контент из content/src/main/resources — заодно проверка, что всё собирается вместе
    private val content = ContentLoader().load()
    private val engine = GameEngine(content.economy, content.catalog)
    private val repository = FakeGameRepository()
    private val store = GameStore(engine, repository)

    private val child = Profile(playerName = "Лис", difficulty = Difficulty.EASY, slot = Slot.CHILD)
    private val demo = Profile(playerName = "Эксперт", difficulty = Difficulty.EASY, slot = Slot.DEMO)
    private val look = PetLook("fluffy", "ginger")

    private suspend fun GameStore.planned(profile: Profile = child) {
        newGame(profile, "Пончик", look)
        dispatch(Action.SetPlan(Plan(50, 25, 25)))
        dispatch(Action.ConfirmPlan)
    }

    @Test
    fun `новый игрок — профиля нет, дальше онбординг`() = runTest {
        assertNull(store.open(Slot.CHILD))
        assertNull(store.state.value)
    }

    @Test
    fun `новая игра сохраняется и показывается`() = runTest {
        val state = store.newGame(child, "Пончик", look)

        assertEquals(100, state.balance)
        assertEquals(state, store.state.value)
        assertEquals(state, repository.saved[Slot.CHILD])
    }

    @Test
    fun `действие — новое состояние сохранено и показано, с панелью «что изменилось»`() = runTest {
        store.planned()

        val result = store.dispatch(Action.Buy("food_basic")) as Result.Ok

        assertEquals(70, store.state.value!!.balance)
        assertEquals(store.state.value, repository.saved[Slot.CHILD])
        assertEquals(100, result.feedback.balanceBefore)
        assertEquals(70, result.feedback.balanceAfter)
    }

    @Test
    fun `отказ ничего не меняет и не пишет на диск`() = runTest {
        store.planned()
        val before = store.state.value
        val saves = repository.saves

        val result = store.dispatch(Action.Buy("want_scooter")) as Result.Ok   // 80 из 100 — можно
        val rejected = store.dispatch(Action.Buy("want_scooter")) as Result.Rejected

        assertTrue(rejected.reason is Rejection.NotEnoughMoney)
        assertEquals(result.state, store.state.value)
        assertEquals(saves + 1, repository.saves)
        assertTrue(before != store.state.value)
    }

    @Test
    fun `после перезапуска — ровно то же место`() = runTest {
        store.planned()
        store.dispatch(Action.Buy("food_basic"))
        store.dispatch(Action.Deposit(25))
        val beforeRestart = store.state.value

        val restarted = GameStore(engine, repository)   // новый процесс, та же база

        assertEquals(beforeRestart, restarted.open(Slot.CHILD))
    }

    @Test
    fun `запись не удалась — экран не видит несохранённого`() = runTest {
        store.planned()
        val before = store.state.value
        repository.failSave = true

        runCatching { store.dispatch(Action.Buy("food_basic")) }

        assertEquals(before, store.state.value)
    }

    @Test
    fun `действие без открытого профиля — ошибка`() = runTest {
        val error = runCatching { store.dispatch(Action.ConfirmPlan) }.exceptionOrNull()
        assertTrue(error is IllegalStateException)
    }

    @Test
    fun `демо не трогает игру ребёнка, сброс демо — тоже`() = runTest {
        store.planned(child)
        store.dispatch(Action.Buy("food_basic"))
        val childState = store.state.value

        assertNull(store.open(Slot.DEMO))
        store.newGame(demo, "Мурка", look)
        store.delete(Slot.DEMO)
        assertNull(store.state.value)

        assertEquals(childState, store.open(Slot.CHILD))
    }

    @Test
    fun `«Ускорить» в демо доводит до Взрослого и сохраняет итог`() = runTest {
        store.newGame(demo, "Мурка", look)

        val steps = store.autoplay(ReasonableStrategy(content.economy, content.catalog), Stage.ADULT)

        assertEquals(Stage.ADULT, store.state.value!!.pet.stage)
        assertEquals(store.state.value, repository.saved[Slot.DEMO])
        assertTrue(steps.count { it.feedback.weekResult != null } <= 4)
    }

    @Test
    fun `«Ускорить» в профиле ребёнка — нельзя`() = runTest {
        store.newGame(child, "Пончик", look)

        val error = runCatching { store.autoplay(ReasonableStrategy(content.economy, content.catalog), Stage.ADULT) }
            .exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals(Stage.BABY, store.state.value!!.pet.stage)
    }

    @Test
    fun `быстрые нажатия подряд не теряют ни одного действия`() = runBlocking {
        store.planned()

        // 10 взносов по 5 одновременно из разных потоков
        List(10) { launch(Dispatchers.Default) { store.dispatch(Action.Deposit(5)) } }.forEach { it.join() }

        assertEquals(50, store.state.value!!.savings.total)
        assertEquals(50, store.state.value!!.balance)
        assertEquals(store.state.value, repository.saved[Slot.CHILD])
    }
}
