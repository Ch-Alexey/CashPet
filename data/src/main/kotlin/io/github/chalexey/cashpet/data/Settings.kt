package io.github.chalexey.cashpet.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/** Настройки приложения: общие для профиля ребёнка и демо, сбросом профиля не стираются. */
data class AppSettings(
    val sound: Boolean = true,
    val animations: Boolean = true,
    val seenHints: Set<String> = emptySet(),      // id подсказок, которые уже показали при первом открытии раздела
    val demoActive: Boolean = false,              // открыт демо-профиль: после перезапуска продолжаем демо, а не профиль ребёнка
)

/** Таблица из одной строки с id = 0. */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val sound: Boolean,
    val animations: Boolean,
    @ColumnInfo(name = "seen_hints") val seenHints: String,   // JSON-массив id
    @ColumnInfo(name = "demo_active", defaultValue = "0") val demoActive: Boolean = false,   // с версии 2 базы
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 0")
    suspend fun get(): SettingsEntity?

    @Query("SELECT * FROM settings WHERE id = 0")
    fun observe(): Flow<SettingsEntity?>

    @Upsert
    suspend fun upsert(entity: SettingsEntity)
}

class SettingsRepository(private val dao: SettingsDao) {

    /** Пока строки нет — настройки по умолчанию: звук и анимации включены. */
    val settings: Flow<AppSettings> = dao.observe().map { it?.toSettings() ?: AppSettings() }

    suspend fun setSound(on: Boolean) = update { it.copy(sound = on) }

    suspend fun setAnimations(on: Boolean) = update { it.copy(animations = on) }

    suspend fun markHintSeen(hintId: String) = update { it.copy(seenHints = it.seenHints + hintId) }

    suspend fun setDemoActive(on: Boolean) = update { it.copy(demoActive = on) }

    private suspend fun update(change: (AppSettings) -> AppSettings) {
        val current = dao.get()?.toSettings() ?: AppSettings()
        dao.upsert(change(current).toEntity())
    }

    private fun SettingsEntity.toSettings() = AppSettings(
        sound = sound,
        animations = animations,
        seenHints = Json.decodeFromString<List<String>>(seenHints).toSet(),
        demoActive = demoActive,
    )

    private fun AppSettings.toEntity() = SettingsEntity(
        sound = sound,
        animations = animations,
        seenHints = Json.encodeToString(seenHints.sorted()),
        demoActive = demoActive,
    )
}
