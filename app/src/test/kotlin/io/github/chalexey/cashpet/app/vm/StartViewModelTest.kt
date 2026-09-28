package io.github.chalexey.cashpet.app.vm

import io.github.chalexey.cashpet.app.FakeGameRepository
import io.github.chalexey.cashpet.app.FakeSettingsDao
import io.github.chalexey.cashpet.app.store.GameStore
import io.github.chalexey.cashpet.content.ContentLoader
import io.github.chalexey.cashpet.core.engine.GameEngine
import io.github.chalexey.cashpet.core.model.Difficulty
import io.github.chalexey.cashpet.core.model.PetLook
import io.github.chalexey.cashpet.core.model.Profile
import io.github.chalexey.cashpet.core.model.Slot
import io.github.chalexey.cashpet.data.SettingsRepository
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
    private val settings = SettingsRepository(FakeSettingsDao())

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun saved(slot: Slot) =
        engine.newGame(Profile("Лис", Difficulty.EASY, slot), "Пончик", PetLook("fluffy", "ginger"))
            .also { runBlocking { repository.save(it) } }

    @Test
    fun `первый запуск — профиля нет, «Играть» ведёт в онбординг`() {
        val vm = StartViewModel(GameStore(engine, repository), settings)

        assertEquals(StartUiState(loading = false, hasProfile = false), vm.uiState.value)
    }

    @Test
    fun `повторный запуск — профиль открыт, «Играть» ведёт на Дом с тем же состоянием`() {
        val child = saved(Slot.CHILD)
        val store = GameStore(engine, repository)

        val vm = StartViewModel(store, settings)

        assertEquals(StartUiState(loading = false, hasProfile = true), vm.uiState.value)
        assertEquals(child, store.state.value)
    }

    @Test
    fun `демо не закрыли — после перезапуска открыт демо-профиль, а не профиль ребёнка`() {
        saved(Slot.CHILD)
        val demo = saved(Slot.DEMO)
        runBlocking { settings.setDemoActive(true) }
        val store = GameStore(engine, repository)

        val vm = StartViewModel(store, settings)

        assertEquals(StartUiState(loading = false, hasProfile = true, demo = true), vm.uiState.value)
        assertEquals(demo, store.state.value)
        assertEquals(Slot.DEMO, store.slot)
    }

    @Test
    fun `профиль удалили в разделе взрослого — старт это видит, «Играть» ведёт в онбординг`() {
        saved(Slot.CHILD)
        val store = GameStore(engine, repository)
        val vm = StartViewModel(store, settings)
        assertEquals(true, vm.uiState.value.hasProfile)

        runBlocking { store.delete(Slot.CHILD) }

        assertEquals(StartUiState(loading = false, hasProfile = false), vm.uiState.value)
    }

    @Test
    fun `демо сбросили и перезапустили — онбординг в демо`() {
        runBlocking { settings.setDemoActive(true) }

        val vm = StartViewModel(GameStore(engine, repository), settings)

        assertEquals(StartUiState(loading = false, hasProfile = false, demo = true), vm.uiState.value)
    }
}
