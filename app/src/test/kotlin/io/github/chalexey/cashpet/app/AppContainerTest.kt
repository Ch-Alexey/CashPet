package io.github.chalexey.cashpet.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.chalexey.cashpet.core.model.GameState
import io.github.chalexey.cashpet.core.model.Pet
import io.github.chalexey.cashpet.core.model.Slot
import io.github.chalexey.cashpet.data.AppSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppContainerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `приложение из манифеста — CashPetApp с одним контейнером`() {
        assertTrue(context is CashPetApp)
        assertSame((context as CashPetApp).container, context.appContainer)
    }

    @Test
    fun `сохранение работает через контейнер`() = runBlocking {
        val repository = context.appContainer.gameRepository
        val state = GameState(pet = Pet(name = "Пончик"), balance = 55)

        repository.save(state)

        assertEquals(state, repository.load(Slot.CHILD))
    }

    @Test
    fun `настройки работают через контейнер`() = runBlocking {
        assertEquals(AppSettings(), context.appContainer.settingsRepository.settings.first())
    }
}
