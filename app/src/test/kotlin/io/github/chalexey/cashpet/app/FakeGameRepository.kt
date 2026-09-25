package io.github.chalexey.cashpet.app

import io.github.chalexey.cashpet.core.engine.GameRepository
import io.github.chalexey.cashpet.core.model.GameState
import io.github.chalexey.cashpet.core.model.Slot

/** Хранилище в памяти для тестов app: без Room. [failSave] — запись на диск «падает». */
class FakeGameRepository : GameRepository {
    val saved = mutableMapOf<Slot, GameState>()
    var saves = 0
    var failSave = false

    override suspend fun load(slot: Slot) = saved[slot]

    override suspend fun save(state: GameState) {
        if (failSave) error("диск недоступен")
        saves++
        saved[state.profile.slot] = state
    }

    override suspend fun delete(slot: Slot) {
        saved.remove(slot)
    }
}
