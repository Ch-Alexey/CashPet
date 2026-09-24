package io.github.chalexey.cashpet.data

import android.content.Context
import io.github.chalexey.cashpet.core.engine.GameRepository

/**
 * Всё хранение приложения для app: репозитории наружу, Room — внутри модуля data.
 * Поэтому app не зависит от Room и не видит таблиц.
 */
class LocalStorage private constructor(database: CashPetDatabase) {

    val gameRepository: GameRepository = RoomGameRepository(database.gameStateDao())

    val settingsRepository: SettingsRepository = SettingsRepository(database.settingsDao())

    companion object {
        fun create(context: Context): LocalStorage = LocalStorage(CashPetDatabase.create(context))
    }
}
