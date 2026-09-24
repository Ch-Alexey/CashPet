package io.github.chalexey.cashpet.content

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/** Файлы контента из src/main/resources/content. Разбор по контрактам — [ContentLoader]. */
object ContentFiles {
    val ALL = listOf("tasks.json", "shop.json", "goals.json", "glossary.json", "neighbors.json", "onboarding.json")

    fun read(name: String): String =
        ContentFiles::class.java.getResource("/content/$name")?.readText()
            ?: error("Нет файла контента: $name")

    fun parse(name: String): JsonObject = Json.parseToJsonElement(read(name)).jsonObject

    fun taskCount(): Int = parse("tasks.json").getValue("tasks").jsonArray.size
}
