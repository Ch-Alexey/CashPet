package io.github.chalexey.cashpet.app

import io.github.chalexey.cashpet.data.SettingsDao
import io.github.chalexey.cashpet.data.SettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Таблица настроек в памяти для тестов app — без Room. */
class FakeSettingsDao : SettingsDao {
    val row = MutableStateFlow<SettingsEntity?>(null)

    override suspend fun get() = row.value

    override fun observe(): Flow<SettingsEntity?> = row

    override suspend fun upsert(entity: SettingsEntity) {
        row.value = entity
    }
}
