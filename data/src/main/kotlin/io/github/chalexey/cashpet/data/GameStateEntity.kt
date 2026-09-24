package io.github.chalexey.cashpet.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import io.github.chalexey.cashpet.core.model.Slot

/** Всё состояние игры одного профиля — одной строкой JSON. */
@Entity(tableName = "game_state")
data class GameStateEntity(
    @PrimaryKey val slot: Slot,
    val json: String,
    @ColumnInfo(name = "schema_version") val schemaVersion: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Dao
interface GameStateDao {
    @Query("SELECT * FROM game_state WHERE slot = :slot")
    suspend fun get(slot: Slot): GameStateEntity?

    @Upsert
    suspend fun upsert(entity: GameStateEntity)

    @Query("DELETE FROM game_state WHERE slot = :slot")
    suspend fun delete(slot: Slot)
}
