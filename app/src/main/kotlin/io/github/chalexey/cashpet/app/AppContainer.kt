package io.github.chalexey.cashpet.app

import android.content.Context
import io.github.chalexey.cashpet.content.ContentLoader
import io.github.chalexey.cashpet.content.GameContent
import io.github.chalexey.cashpet.core.engine.GameEngine
import io.github.chalexey.cashpet.core.engine.GameRepository
import io.github.chalexey.cashpet.data.LocalStorage
import io.github.chalexey.cashpet.data.SettingsRepository

/**
 * Все зависимости приложения в одном месте — ручной DI вместо Hilt (CLAUDE.md).
 * Один экземпляр на приложение, живёт в [CashPetApp]; ViewModel берут отсюда то, что им нужно.
 *
 * Всё создаётся лениво, при первом обращении: база не открывается и контент не читается,
 * пока они никому не понадобились. Так старт приложения быстрее (ТЗ: до 5 секунд).
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val storage: LocalStorage by lazy { LocalStorage.create(appContext) }

    val gameRepository: GameRepository get() = storage.gameRepository

    val settingsRepository: SettingsRepository get() = storage.settingsRepository

    /** Контент из src/main/resources/content. Ошибка в JSON — ContentException с именем файла. */
    val content: GameContent by lazy { ContentLoader().load() }

    val engine: GameEngine by lazy { GameEngine(content.economy, content.catalog) }
}
