package io.github.chalexey.cashpet.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.chalexey.cashpet.core.model.GameState
import io.github.chalexey.cashpet.core.model.Pet
import io.github.chalexey.cashpet.core.model.Profile
import io.github.chalexey.cashpet.core.model.Slot
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomGameRepositoryTest {

    private lateinit var db: CashPetDatabase
    private lateinit var repository: RoomGameRepository

    private val child = GameState(profile = Profile(playerName = "Лис", slot = Slot.CHILD), pet = Pet(name = "Пончик"), balance = 55)
    private val demo = GameState(profile = Profile(playerName = "Эксперт", slot = Slot.DEMO), pet = Pet(name = "Мурка"), balance = 100)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CashPetDatabase::class.java).allowMainThreadQueries().build()
        repository = RoomGameRepository(db.gameStateDao(), now = { 1_000L })
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `пустой слот — null`() = runBlocking {
        assertNull(repository.load(Slot.CHILD))
    }

    @Test
    fun `сохранённое состояние читается таким же`() = runBlocking {
        repository.save(child)

        assertEquals(child, repository.load(Slot.CHILD))
    }

    @Test
    fun `повторное сохранение заменяет прежнее`() = runBlocking {
        repository.save(child)
        repository.save(child.copy(balance = 20))

        assertEquals(20, repository.load(Slot.CHILD)?.balance)
    }

    @Test
    fun `демо-профиль не трогает профиль ребёнка`() = runBlocking {
        repository.save(child)
        repository.save(demo)

        assertEquals(child, repository.load(Slot.CHILD))
        assertEquals(demo, repository.load(Slot.DEMO))
    }

    @Test
    fun `сброс демо удаляет только демо`() = runBlocking {
        repository.save(child)
        repository.save(demo)

        repository.delete(Slot.DEMO)

        assertNull(repository.load(Slot.DEMO))
        assertEquals(child, repository.load(Slot.CHILD))
    }

    @Test
    fun `в строке хранятся версия схемы и время сохранения`() = runBlocking {
        repository.save(child)

        val row = db.gameStateDao().get(Slot.CHILD)!!
        assertEquals(GameStateCodec.SCHEMA_VERSION, row.schemaVersion)
        assertEquals(1_000L, row.updatedAt)
    }
}
