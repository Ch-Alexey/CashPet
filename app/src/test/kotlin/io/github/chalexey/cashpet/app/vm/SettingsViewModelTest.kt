package io.github.chalexey.cashpet.app.vm

import io.github.chalexey.cashpet.app.FakeSettingsDao
import io.github.chalexey.cashpet.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dao = FakeSettingsDao()
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        // viewModelScope работает на Dispatchers.Main — в тесте подменяем его
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = SettingsViewModel(SettingsRepository(dao))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `по умолчанию звук и анимации включены`() {
        assertEquals(SettingsUiState(sound = true, animations = true), viewModel.uiState.value)
    }

    @Test
    fun `выключенный звук — в состоянии экрана и в базе`() {
        viewModel.setSound(false)

        assertEquals(SettingsUiState(sound = false, animations = true), viewModel.uiState.value)
        assertFalse(dao.row.value!!.sound)
    }

    @Test
    fun `анимации выключаются отдельно от звука`() {
        viewModel.setAnimations(false)

        assertEquals(SettingsUiState(sound = true, animations = false), viewModel.uiState.value)
    }
}
