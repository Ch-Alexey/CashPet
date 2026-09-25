package io.github.chalexey.cashpet.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * База приложения — две таблицы (docs/08-контракты.md, раздел 6).
 * При изменении таблиц: version + 1 и миграция; схемы версий лежат в data/schemas.
 *
 * Версия 2 — в settings колонка demo_active. Room добавляет её сам по схемам 1 и 2, старые данные остаются.
 */
@Database(
    entities = [GameStateEntity::class, SettingsEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class CashPetDatabase : RoomDatabase() {
    abstract fun gameStateDao(): GameStateDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        private const val FILE_NAME = "cashpet.db"

        fun create(context: Context): CashPetDatabase =
            Room.databaseBuilder(context, CashPetDatabase::class.java, FILE_NAME).build()
    }
}
