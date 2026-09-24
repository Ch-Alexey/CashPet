package io.github.chalexey.cashpet.data

import io.github.chalexey.cashpet.core.model.GameState
import kotlinx.serialization.json.Json

/**
 * GameState ⇄ JSON-строка для таблицы game_state (docs/08-контракты.md, раздел 6).
 * Настройки Json — те же, что в core SerializationTest.
 */
object GameStateCodec {
    /** Растёт только при несовместимых изменениях GameState. Новое поле со значением по умолчанию — не повод. */
    const val SCHEMA_VERSION = 1

    // ignoreUnknownKeys — чтобы прочиталось сохранение, где есть поле, которое потом удалили;
    // encodeDefaults — чтобы в JSON были все поля, а не только отличные от значений по умолчанию
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(state: GameState): String = json.encodeToString(state)

    fun decode(text: String): GameState = json.decodeFromString(text)
}
