package io.github.chalexey.cashpet.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.chalexey.cashpet.app.appContainer
import io.github.chalexey.cashpet.app.store.GameStore
import io.github.chalexey.cashpet.content.GameContent
import io.github.chalexey.cashpet.content.NameInput
import io.github.chalexey.cashpet.content.ScreenKind
import io.github.chalexey.cashpet.core.model.Difficulty
import io.github.chalexey.cashpet.core.model.PetLook
import io.github.chalexey.cashpet.core.model.Profile
import io.github.chalexey.cashpet.core.model.Slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Онбординг с созданием питомца: экраны из onboarding.json по порядку, в конце — новый профиль в [store].
 * [slot] — ребёнок или демо: демо-режим проходит тот же онбординг в своём профиле.
 */
class OnboardingViewModel(
    private val content: GameContent,
    private val store: GameStore,
    private val slot: Slot = Slot.CHILD,
) : ViewModel() {

    private val screens = content.onboarding
    private var index = 0
    private var playerName = ""
    private var petName = ""
    private var look = PetLook(content.pets.breeds.first().id, content.pets.colors.first().id)
    private var difficulty: Difficulty? = null
    private var showError = false              // ошибку имени показываем после «Дальше», а не пока ребёнок печатает
    private var creating = false
    private var finished = false

    private val _uiState = MutableStateFlow(render())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onNameChange(text: String) {
        when (screens[index].kind) {
            ScreenKind.PLAYER_NAME -> playerName = text
            ScreenKind.PET_NAME -> petName = text
            else -> return
        }
        showError = false
        update()
    }

    fun onLookChange(look: PetLook) {
        this.look = look
        update()
    }

    /** Выбор сложности сразу ведёт дальше — на этом экране нет отдельной кнопки. */
    fun onDifficultyChoose(difficulty: Difficulty) {
        this.difficulty = difficulty
        next()
    }

    /** «Дальше» / «Готово» / «Начать». На последнем экране — создать профиль. */
    fun next() {
        if (creating || finished) return
        val screen = screens[index]
        val input = screen.input
        if (input != null && nameError(currentName(), input) != null) {
            showError = true
            update()
            return
        }
        if (screen.kind == ScreenKind.DIFFICULTY && difficulty == null) return
        if (index == screens.lastIndex) {
            create()
            return
        }
        index++
        showError = false
        update()
    }

    fun skip() {
        if (screens[index].skippable) next()
    }

    fun back() {
        if (index == 0 || creating || finished) return
        index--
        showError = false
        update()
    }

    private fun create() {
        creating = true
        update()
        viewModelScope.launch {
            val profile = Profile(playerName = playerName.trim(), difficulty = difficulty ?: Difficulty.EASY, slot = slot)
            store.newGame(profile, petName.trim(), look)
            creating = false
            finished = true
            update()
        }
    }

    private fun update() {
        _uiState.value = render()
    }

    private fun render(): OnboardingUiState {
        val screen = screens[index]
        val nameOk = screen.input?.let { nameError(currentName(), it) == null } ?: false
        return OnboardingUiState(
            kind = screen.kind,
            step = index + 1,
            steps = screens.size,
            text = fill(screen.text),
            bullets = screen.bullets.map { BulletUi(it.term, fill(it.text)) },
            finePrint = screen.finePrint?.let(::fill),
            input = screen.input?.let { input ->
                NameInputUi(
                    value = currentName(),
                    placeholder = input.placeholder,
                    hint = input.hint,
                    maxLength = input.maxLength,
                    error = if (showError) nameError(currentName(), input) else null,
                )
            },
            look = look,
            breeds = content.pets.breeds.map { PetOptionUi(it.id, it.name) },
            colors = content.pets.colors.map { PetOptionUi(it.id, it.name) },
            choices = screen.choices.mapNotNull { (id, label) ->
                val value = Difficulty.entries.firstOrNull { it.name.lowercase() == id } ?: return@mapNotNull null
                DifficultyChoiceUi(value, label, selected = value == difficulty)
            },
            button = screen.button?.let(::fill),
            petReply = if (screen.kind == ScreenKind.PET_NAME && nameOk) screen.petReply?.let(::fill) else null,
            canSkip = screen.skippable,
            canGoBack = index > 0 && !creating && !finished,
            creating = creating,
            finished = finished,
            firstTaskId = if (finished) screens.last().next else null,
        )
    }

    private fun currentName(): String = when (screens[index].kind) {
        ScreenKind.PET_NAME -> petName
        else -> playerName
    }

    // Пусто или только пробелы — «напиши хотя бы одну букву»; длиннее max_length — «давай покороче»
    private fun nameError(value: String, input: NameInput): String? {
        val name = value.trim()
        return when {
            name.isEmpty() -> input.emptyError
            name.length > input.maxLength -> input.tooLongError
            else -> null
        }
    }

    private fun fill(text: String): String =
        text.replace("{playerName}", playerName.trim()).replace("{petName}", petName.trim())

    companion object {
        /** Фабрика для экрана: `viewModel(factory = OnboardingViewModel.factory())`, в демо — `factory(Slot.DEMO)`. */
        fun factory(slot: Slot = Slot.CHILD): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = this[APPLICATION_KEY]!!.appContainer
                OnboardingViewModel(container.content, container.gameStore, slot)
            }
        }
    }
}
