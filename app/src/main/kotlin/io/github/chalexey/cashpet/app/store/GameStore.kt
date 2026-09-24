package io.github.chalexey.cashpet.app.store

import io.github.chalexey.cashpet.core.engine.Action
import io.github.chalexey.cashpet.core.engine.GameEngine
import io.github.chalexey.cashpet.core.engine.GameRepository
import io.github.chalexey.cashpet.core.engine.Result
import io.github.chalexey.cashpet.core.model.GameState
import io.github.chalexey.cashpet.core.model.PetLook
import io.github.chalexey.cashpet.core.model.Profile
import io.github.chalexey.cashpet.core.model.Slot
import io.github.chalexey.cashpet.core.model.Stage
import io.github.chalexey.cashpet.core.sim.Autoplay
import io.github.chalexey.cashpet.core.sim.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Единственное место, где игра меняется и сохраняется (docs/08-контракты.md, раздел 1).
 * ViewModel читают [state] и отправляют действия в [dispatch]; сами ничего не считают и не сохраняют.
 *
 * Открыт всегда один профиль — ребёнка или демо ([slot]); они лежат в разных строках базы.
 * Все изменения идут по очереди под [mutex]: два быстрых нажатия не потеряют ни одно действие.
 */
class GameStore(
    private val engine: GameEngine,
    private val repository: GameRepository,
) {
    private val _state = MutableStateFlow<GameState?>(null)

    /** Состояние открытого профиля; null — профиля нет, дальше онбординг. */
    val state: StateFlow<GameState?> = _state.asStateFlow()

    var slot: Slot = Slot.CHILD
        private set

    private val mutex = Mutex()

    /** Открыть профиль: сохранённый — или null, если его ещё нет. */
    suspend fun open(slot: Slot): GameState? = mutex.withLock {
        this.slot = slot
        repository.load(slot).also { _state.value = it }
    }

    /** Новая игра после онбординга. Слот — из [profile]; он же становится открытым. */
    suspend fun newGame(profile: Profile, petName: String, look: PetLook): GameState = mutex.withLock {
        val state = engine.newGame(profile, petName, look)
        slot = profile.slot
        publish(state)
        state
    }

    /**
     * Действие ребёнка. Ok — новое состояние сохранено и показано, в нём Feedback для панели
     * «что изменилось». Rejected — ничего не меняется, экран объясняет причину.
     */
    suspend fun dispatch(action: Action): Result = mutex.withLock {
        val result = engine.apply(current(), action)
        if (result is Result.Ok) publish(result.state)
        result
    }

    /**
     * «Ускорить» в демо: недели по [strategy] до стадии [target] тем же движком, что и обычная игра.
     * Возвращает все шаги — экран показывает итог каждой недели. Только в демо-профиле.
     */
    suspend fun autoplay(strategy: Strategy, target: Stage): List<Result.Ok> = mutex.withLock {
        check(slot == Slot.DEMO) { "«Ускорить» — только в демо-профиле" }
        val steps = Autoplay.run(current(), engine, strategy, target)
        steps.lastOrNull()?.let { publish(it.state) }
        steps
    }

    /** Сброс или удаление профиля из раздела взрослого. Открытый профиль закрывается — дальше онбординг. */
    suspend fun delete(slot: Slot) = mutex.withLock {
        repository.delete(slot)
        if (this.slot == slot) _state.value = null
    }

    private fun current(): GameState = checkNotNull(_state.value) { "Профиль не открыт: сначала open() или newGame()" }

    // Сначала на диск, потом на экран: если запись упала, экран не покажет то, чего нет в сохранении
    private suspend fun publish(state: GameState) {
        repository.save(state)
        _state.value = state
    }
}
