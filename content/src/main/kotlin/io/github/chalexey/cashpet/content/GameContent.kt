package io.github.chalexey.cashpet.content

import io.github.chalexey.cashpet.core.model.Catalog
import io.github.chalexey.cashpet.core.model.EconomyConfig
import io.github.chalexey.cashpet.core.model.FeedbackReason
import io.github.chalexey.cashpet.core.model.PetReason
import io.github.chalexey.cashpet.core.task.TaskDef
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Весь игровой контент после загрузки (docs/08-контракты.md, раздел 7). */
data class GameContent(
    val economy: EconomyConfig,
    val catalog: Catalog,                              // товары, цели, подработки, TaskMeta — то, что нужно движку
    val tasks: List<TaskDef>,                          // задания целиком — для TaskSession
    val pets: PetOptions,
    val texts: Texts,
    val onboarding: List<OnboardingScreen>,            // экраны знакомства по порядку
    val glossary: List<GlossaryTerm>,                  // словарь в «Прогрессе»
)

/** 3 кота × 3 окраса для экрана внешности. */
@Serializable
data class PetOptions(val breeds: List<PetOption>, val colors: List<PetOption>)

@Serializable
data class PetOption(val id: String, val name: String)

/** Фразы для кодов причин из движка. Подстановки ({petName}, {item}, {amount}) ещё не заменены. */
data class Texts(
    val feedback: Map<FeedbackReason, String>,         // панель «что изменилось»
    val petWeek: Map<PetReason, String>,               // строка про кота на итоге недели
    val petNow: Map<PetReason, String>,                // карточка кота среди недели
)

/** Что делает экран онбординга (docs/01-функционал.md, раздел 1). */
@Serializable
enum class ScreenKind {
    @SerialName("info") INFO,                          // текст и кнопка
    @SerialName("player_name") PLAYER_NAME,            // ввод игрового имени
    @SerialName("look") LOOK,                          // выбор кота 3 × 3
    @SerialName("pet_name") PET_NAME,                  // ввод имени кота
    @SerialName("difficulty") DIFFICULTY,              // «попроще / посложнее»
}

/** Экран онбординга. Тексты — с подстановками {playerName}, {petName}. */
@Serializable
data class OnboardingScreen(
    val id: String,
    val kind: ScreenKind,
    val text: String,
    val bullets: List<Bullet> = emptyList(),
    @SerialName("fine_print") val finePrint: String? = null,
    val input: NameInput? = null,                      // только у PLAYER_NAME и PET_NAME
    val choices: Map<String, String> = emptyMap(),     // только у DIFFICULTY: easy / hard → подпись кнопки
    val button: String? = null,
    @SerialName("pet_reply") val petReply: String? = null,
    val skippable: Boolean = false,
    val next: String? = null,                          // у последнего экрана — id первого задания
)

@Serializable
data class Bullet(val term: String, val text: String)

@Serializable
data class NameInput(
    val placeholder: String,
    val hint: String? = null,
    @SerialName("max_length") val maxLength: Int,
    @SerialName("empty_error") val emptyError: String,
    @SerialName("too_long_error") val tooLongError: String,
)

@Serializable
data class GlossaryTerm(val id: String, val term: String, val definition: String)
