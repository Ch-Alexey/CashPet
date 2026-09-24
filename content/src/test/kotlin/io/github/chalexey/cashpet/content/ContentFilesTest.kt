package io.github.chalexey.cashpet.content

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

// Первый тест-сторож: каждый файл контента — корректный JSON-объект
class ContentFilesTest {
    @TestFactory
    fun `каждый файл контента читается`() = ContentFiles.ALL.map { name ->
        DynamicTest.dynamicTest(name) { ContentFiles.parse(name) }
    }

    @Test
    fun `заданий не меньше шести`() {
        assertTrue(ContentFiles.taskCount() >= 6, "По ТЗ нужно не меньше 6 заданий")
    }
}
