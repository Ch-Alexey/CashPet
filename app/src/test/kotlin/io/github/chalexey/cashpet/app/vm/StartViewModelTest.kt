package io.github.chalexey.cashpet.app.vm

import io.github.chalexey.cashpet.app.FakeGameRepository
import io.github.chalexey.cashpet.app.store.GameStore
import io.github.chalexey.cashpet.content.ContentLoader
import io.github.chalexey.cashpet.core.engine.GameEngine
import io.github.chalexey.cashpet.core.model.Difficulty
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StartViewModelTest {

    private val content = ContentLoader().load()
    private val engine = GameEngine(content.economy, content.catalog)
    private val repository = FakeGameRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `первый запуск — профиля нет, «Играть» ведёт в онбординг`() {
        val vm = StartViewModel(GameStore(engine, repository))

        assertEquals(StartUiState(loading = false, hasProfile = false), vm.uiState.value)
    }

    @Test
    fun `повторный запуск — профиль открыт, «Играть» ведёт на Дом с тем же состоянием`() {
        val saved = engine.newGame(Profile("Лис", Difficulty.EASY, Slot.CHILD), "Пончик", PetLook("fluffy", "ginger"))
        runBlocking { repository.save(saved) }
        val store = GameStore(engine, repository)

        val vm = StartViewModel(store)

        assertEquals(StartUiState(loading = false, hasProfile = true), vm.uiState.value)
        assertEquals(saved, store.state.value)
    }
}
