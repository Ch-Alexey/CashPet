package io.github.chalexey.cashpet.app.vm

import io.github.chalexey.cashpet.app.store.GameStore
import io.github.chalexey.cashpet.content.ContentLoader
import io.github.chalexey.cashpet.content.ScreenKind
import io.github.chalexey.cashpet.core.engine.GameEngine
import io.github.chalexey.cashpet.core.engine.GameRepository
import io.github.chalexey.cashpet.core.model.Difficulty
import io.github.chalexey.cashpet.core.model.GameState
import io.github.chalexey.cashpet.core.model.PetLook
import io.github.chalexey.cashpet.core.model.Slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

// На настоящем onboarding.json: заодно проверка, что экраны проходятся от начала до конца
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private class FakeRepository : GameRepository {
        val saved = mutableMapOf<Slot, GameState>()
        override suspend fun load(slot: Slot) = saved[slot]
        override suspend fun save(state: GameState) {
            saved[state.profile.slot] = state
        }
        override suspend fun delete(slot: Slot) {
            saved.remove(slot)
        }
    }

    private val content = ContentLoader().load()
    private val repository = FakeRepository()
    private val store = GameStore(GameEngine(content.economy, content.catalog), repository)
    private lateinit var vm: OnboardingViewModel

    private val state get() = vm.uiState.value

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = OnboardingViewModel(content, store)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** Пройти вперёд до экрана [kind], заполняя по дороге всё обязательное. */
    private fun goTo(kind: ScreenKind) {
        while (state.kind != kind) {
            when (state.kind) {
                ScreenKind.PLAYER_NAME -> vm.onNameChange("Лис")
                ScreenKind.PET_NAME -> vm.onNameChange("Пончик")
                ScreenKind.DIFFICULTY -> { vm.onDifficultyChoose(Difficulty.EASY); continue }
                else -> {}
            }
            vm.next()
        }
    }

    private fun finish() {
        while (!state.finished) {
            when (state.kind) {
                ScreenKind.PLAYER_NAME -> vm.onNameChange("Лис")
                ScreenKind.PET_NAME -> vm.onNameChange("Пончик")
                ScreenKind.DIFFICULTY -> { vm.onDifficultyChoose(Difficulty.HARD); continue }
                else -> {}
            }
            vm.next()
        }
    }

    @Test
    fun `первый экран — игровое имя, шаг 1, назад некуда`() {
        assertEquals(ScreenKind.PLAYER_NAME, state.kind)
        assertEquals(1, state.step)
        assertEquals(content.onboarding.size, state.steps)
        assertEquals("Например: Лис", state.input!!.placeholder)
        assertNull(state.input!!.error)
        assertFalse(state.canGoBack)
    }

    @Test
    fun `пустое имя — ошибка из контента после «Дальше», экран тот же`() {
        vm.onNameChange("   ")
        assertNull(state.input!!.error)                  // пока печатает — не ругаемся

        vm.next()

        assertEquals("Напиши хотя бы одну букву", state.input!!.error)
        assertEquals(1, state.step)
    }

    @Test
    fun `длинное имя — ошибка, исправление убирает её`() {
        vm.onNameChange("Очень длинное имя")
        vm.next()
        assertEquals("Давай покороче: до 12 букв", state.input!!.error)

        vm.onNameChange("Лис")
        assertNull(state.input!!.error)
    }

    @Test
    fun `имя игрока подставляется в следующие экраны, пробелы по краям убираются`() {
        vm.onNameChange("  Лис  ")
        vm.next()

        assertTrue(state.text, state.text.startsWith("Добро пожаловать в Котляндию, Лис!"))
    }

    @Test
    fun `выбор кота — 3 × 3, по умолчанию первый вариант`() {
        goTo(ScreenKind.LOOK)
        assertEquals(PetLook("fluffy", "ginger"), state.look)
        assertEquals(3, state.breeds.size)
        assertEquals(3, state.colors.size)

        vm.onLookChange(PetLook("lop", "black"))

        assertEquals(PetLook("lop", "black"), state.look)
    }

    @Test
    fun `реплика кота — только когда имя подходит`() {
        goTo(ScreenKind.PET_NAME)
        assertNull(state.petReply)

        vm.onNameChange("Пончик")

        assertEquals("Мне нравится! Теперь мы друзья", state.petReply)
    }

    @Test
    fun `пропустить можно только пропускаемые экраны`() {
        assertFalse(state.canSkip)
        vm.skip()
        assertEquals(1, state.step)                       // имя не пропустить

        goTo(ScreenKind.PET_NAME)
        vm.onNameChange("Пончик")
        vm.next()
        assertTrue(state.canSkip)                         // «три вида решений»
        val step = state.step
        vm.skip()
        assertEquals(step + 1, state.step)
    }

    @Test
    fun `назад — введённое имя сохраняется`() {
        vm.onNameChange("Лис")
        vm.next()

        vm.back()

        assertEquals("Лис", state.input!!.value)
    }

    @Test
    fun `сложность — без выбора дальше не пройти, выбор сразу ведёт дальше`() {
        goTo(ScreenKind.DIFFICULTY)
        assertNull(state.button)
        assertEquals(listOf(Difficulty.EASY, Difficulty.HARD), state.choices.map { it.difficulty })
        val step = state.step

        vm.next()
        assertEquals(step, state.step)

        vm.onDifficultyChoose(Difficulty.HARD)
        assertEquals(step + 1, state.step)
    }

    @Test
    fun `в конце — профиль ребёнка создан и сохранён, дальше первое задание`() {
        goTo(ScreenKind.LOOK)
        vm.onLookChange(PetLook("smooth", "grey"))
        finish()

        assertEquals("0", state.firstTaskId)
        val game = repository.saved.getValue(Slot.CHILD)
        assertEquals("Лис", game.profile.playerName)
        assertEquals(Difficulty.HARD, game.profile.difficulty)
        assertEquals("Пончик", game.pet.name)
        assertEquals(PetLook("smooth", "grey"), game.pet.look)
        assertEquals(content.economy.startBudget, game.balance)
        assertEquals(game, store.state.value)
    }

    @Test
    fun `демо-режим — тот же онбординг, но в демо-профиле`() {
        vm = OnboardingViewModel(content, store, Slot.DEMO)

        finish()

        assertTrue(Slot.DEMO in repository.saved)
        assertFalse(Slot.CHILD in repository.saved)
    }

    @Test
    fun `после создания профиля назад и повторное создание — нельзя`() {
        finish()
        val game = repository.saved.getValue(Slot.CHILD)

        vm.back()
        vm.next()

        assertFalse(state.canGoBack)
        assertEquals(game, repository.saved.getValue(Slot.CHILD))
    }
}
