package io.github.chalexey.cashpet.app

import io.github.chalexey.cashpet.core.engine.GameRepository
import io.github.chalexey.cashpet.core.model.GameState
import io.github.chalexey.cashpet.core.model.Slot
import kotlinx.coroutines.CompletableDeferred

/**
 * Хранилище в памяти для тестов app: без Room. [failSave] — запись на диск «падает».
 * [holdSave] — запись «идёт долго», пока тест не вызовет complete: так проверяем нажатия во время записи.
 */
class FakeGameRepository : GameRepository {
    val saved = mutableMapOf<Slot, GameState>()
    var saves = 0
    var failSave = false
    var holdSave: CompletableDeferred<Unit>? = null

    override suspend fun load(slot: Slot) = saved[slot]

    override suspend fun save(state: GameState) {
        holdSave?.await()
        if (failSave) error("диск недоступен")
        saves++
        saved[state.profile.slot] = state
    }

    override suspend fun delete(slot: Slot) {
        saved.remove(slot)
    }
}
