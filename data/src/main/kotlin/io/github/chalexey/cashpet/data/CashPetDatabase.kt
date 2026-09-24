package io.github.chalexey.cashpet.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * База приложения — две таблицы (docs/08-контракты.md, раздел 6).
 * При изменении таблиц: version + 1 и миграция; схемы версий лежат в data/schemas.
 */
@Database(
    entities = [GameStateEntity::class, SettingsEntity::class],
    version = 1,
    exportSchema = true,
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
