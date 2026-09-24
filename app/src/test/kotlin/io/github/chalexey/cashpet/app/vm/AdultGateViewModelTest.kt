package io.github.chalexey.cashpet.app.vm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AdultGateViewModelTest {

    private val viewModel = AdultGateViewModel(Random(seed = 7))

    private fun answer(): String = with(viewModel.uiState.value) { (a * b).toString() }

    @Test
    fun `пример — от 23 без круглых десятков и одинаковых цифр, на однозначное от 4 до 9`() {
        repeat(1000) { seed ->
            val state = AdultGateViewModel(Random(seed)).uiState.value
            assertTrue("a = ${state.a}", state.a in 23..99 && state.a % 10 != 0 && state.a % 11 != 0)
            assertTrue("b = ${state.b}", state.b in 4..9)
        }
    }

    @Test
    fun `верный ответ открывает раздел`() {
        viewModel.onInputChange(answer())
        viewModel.submit()

        assertTrue(viewModel.uiState.value.passed)
    }

    @Test
    fun `ошибка — новый пример с пустым полем, раздел закрыт`() {
        viewModel.onInputChange((answer().toInt() + 1).toString())
        viewModel.submit()

        val state = viewModel.uiState.value
        assertFalse(state.passed)
        assertTrue(state.wrongAttempt)
        assertEquals("", state.input)
    }

    @Test
    fun `после ошибки новый пример можно решить`() {
        viewModel.onInputChange("0")
        viewModel.submit()

        viewModel.onInputChange(answer())
        viewModel.submit()

        assertTrue(viewModel.uiState.value.passed)
    }

    @Test
    fun `в поле только цифры и не больше трёх`() {
        viewModel.onInputChange("4a7-1 9")

        assertEquals("471", viewModel.uiState.value.input)
    }

    @Test
    fun `пустой ответ ничего не делает`() {
        val before = viewModel.uiState.value

        viewModel.submit()

        assertEquals(before, viewModel.uiState.value)
    }
}
