package io.github.chalexey.cashpet.content

import io.github.chalexey.cashpet.core.model.Catalog
import io.github.chalexey.cashpet.core.model.EconomyConfig
import io.github.chalexey.cashpet.core.model.FeedbackReason
import io.github.chalexey.cashpet.core.model.GoalDef
import io.github.chalexey.cashpet.core.model.JobDef
import io.github.chalexey.cashpet.core.model.PetReason
import io.github.chalexey.cashpet.core.model.ShopItem
import io.github.chalexey.cashpet.core.task.TaskDef
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Ошибка в файле контента: в сообщении — имя файла, чтобы в логе CI сразу было видно, что чинить. */
class ContentException(val file: String, message: String, cause: Throwable? = null) :
    RuntimeException("$file: $message", cause)

/**
 * Читает JSON-контент и собирает [GameContent]. Форматы файлов — docs/08-контракты.md, раздел 7,
 * и docs/07-формат-заданий.md; образцы — content/src/test/resources/samples.
 *
 * [read] — имя файла → текст. По умолчанию файлы из src/main/resources/content.
 */
class ContentLoader(private val read: (String) -> String = ContentFiles::read) {

    // Незнакомые ключи пропускаем: в файлах Ярика есть _meta и заметки note
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): GameContent {
        val tasks = parse(TASKS, TasksFile.serializer()).tasks
        val catalog = Catalog(
            items = parse(SHOP, ShopFile.serializer()).items,
            goals = parse(GOALS, GoalsFile.serializer()).catalog,
            jobs = parse(JOBS, JobsFile.serializer()).jobs,
            tasks = tasks.map { it.meta() },
        )
        return GameContent(
            economy = parse(ECONOMY, EconomyConfig.serializer()),
            catalog = catalog,
            tasks = tasks,
            pets = parse(PET, PetOptions.serializer()),
            texts = parse(TEXTS, TextsFile.serializer()).toTexts(),
        )
    }

    private fun <T> parse(file: String, serializer: DeserializationStrategy<T>): T {
        val text = try {
            read(file)
        } catch (e: Exception) {
            throw ContentException(file, "файл не найден", e)
        }
        return try {
            json.decodeFromString(serializer, text)
        } catch (e: Exception) {
            throw ContentException(file, e.message ?: "не читается", e)
        }
    }

    private fun TextsFile.toTexts() = Texts(
        feedback = feedbackReasons.toEnumKeys("feedback_reasons", FeedbackReason.entries),
        petWeek = petReasons.week.toEnumKeys("pet_reasons.week", PetReason.entries),
        petNow = petReasons.now.toEnumKeys("pet_reasons.now", PetReason.entries),
    )

    // В texts.json ключи строчные (bought_need), в коде — имена перечислений (BOUGHT_NEED)
    private fun <E : Enum<E>> Map<String, String>.toEnumKeys(section: String, entries: List<E>): Map<E, String> =
        mapKeys { (key, _) ->
            entries.firstOrNull { it.name.lowercase() == key }
                ?: throw ContentException(TEXTS, "$section: неизвестный ключ «$key»")
        }

    companion object {
        const val TASKS = "tasks.json"
        const val SHOP = "shop.json"
        const val GOALS = "goals.json"
        const val JOBS = "jobs.json"
        const val ECONOMY = "economy.json"
        const val PET = "pet.json"
        const val TEXTS = "texts.json"
    }
}

// Корневые объекты файлов — только для чтения, наружу не выходят

@Serializable
private class TasksFile(val tasks: List<TaskDef>)

@Serializable
private class ShopFile(val items: List<ShopItem>)       // task_items — только в текстах заданий, на витрину не идут

@Serializable
private class GoalsFile(val catalog: List<GoalDef>)

@Serializable
private class JobsFile(val jobs: List<JobDef>)

@Serializable
private class TextsFile(
    @SerialName("feedback_reasons") val feedbackReasons: Map<String, String>,
    @SerialName("pet_reasons") val petReasons: PetReasonsFile,
)

@Serializable
private class PetReasonsFile(val week: Map<String, String>, val now: Map<String, String>)
