package io.github.chalexey.cashpet.core.engine

import io.github.chalexey.cashpet.core.model.GameState
import io.github.chalexey.cashpet.core.model.Slot

/** Хранение состояния игры. Интерфейс здесь, реализация на Room — в модуле data (docs/08-контракты.md, раздел 6). */
interface GameRepository {
    suspend fun load(slot: Slot): GameState?
    suspend fun save(state: GameState)
    suspend fun delete(slot: Slot)
}
