package io.github.chalexey.cashpet.data

import io.github.chalexey.cashpet.core.engine.GameRepository
import io.github.chalexey.cashpet.core.model.GameState
import io.github.chalexey.cashpet.core.model.Slot

/**
 * Хранение GameState в Room. Слот берётся из state.profile.slot:
 * профиль ребёнка и демо-профиль лежат в разных строках и друг друга не трогают.
 */
class RoomGameRepository(
    private val dao: GameStateDao,
    private val now: () -> Long = System::currentTimeMillis,
) : GameRepository {

    override suspend fun load(slot: Slot): GameState? =
        dao.get(slot)?.let { GameStateCodec.decode(it.json) }

    override suspend fun save(state: GameState) {
        dao.upsert(
            GameStateEntity(
                slot = state.profile.slot,
                json = GameStateCodec.encode(state),
                schemaVersion = GameStateCodec.SCHEMA_VERSION,
                updatedAt = now(),
            )
        )
    }

    override suspend fun delete(slot: Slot) = dao.delete(slot)
}
