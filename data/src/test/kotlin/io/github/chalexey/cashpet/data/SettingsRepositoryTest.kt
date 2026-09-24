package io.github.chalexey.cashpet.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.chalexey.cashpet.core.model.GameState
import io.github.chalexey.cashpet.core.model.Slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {

    private lateinit var db: CashPetDatabase
    private lateinit var settings: SettingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CashPetDatabase::class.java).allowMainThreadQueries().build()
        settings = SettingsRepository(db.settingsDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `по умолчанию звук и анимации включены, подсказок не было`() = runBlocking {
        assertEquals(AppSettings(), settings.settings.first())
    }

    @Test
    fun `выключенный звук сохраняется, анимации не меняются`() = runBlocking {
        settings.setSound(false)

        assertEquals(AppSettings(sound = false, animations = true), settings.settings.first())
    }

    @Test
    fun `подсказка отмечается один раз`() = runBlocking {
        settings.markHintSeen("shop")
        settings.markHintSeen("plan")
        settings.markHintSeen("shop")

        assertEquals(setOf("plan", "shop"), settings.settings.first().seenHints)
    }

    @Test
    fun `удаление профиля не сбрасывает настройки`() = runBlocking {
        val games = RoomGameRepository(db.gameStateDao())
        settings.setAnimations(false)
        games.save(GameState())

        games.delete(Slot.CHILD)

        assertEquals(false, settings.settings.first().animations)
    }
}
