package io.github.chalexey.cashpet.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoundingTest {
    @Test
    fun `половина округляется вверх`() {
        assertEquals(53, percentOf(70, 75))   // 52,5
        assertEquals(38, percentOf(50, 75))   // 37,5
    }

    @Test
    fun `целые значения не меняются`() {
        assertEquals(56, percentOf(70, 80))
        assertEquals(49, percentOf(70, 70))
        assertEquals(0, percentOf(0, 75))
    }
}
