package io.github.chalexey.cashpet.content

import io.github.chalexey.cashpet.core.model.Catalog
import io.github.chalexey.cashpet.core.model.EconomyConfig
import io.github.chalexey.cashpet.core.model.FeedbackReason
import io.github.chalexey.cashpet.core.model.PetReason
import io.github.chalexey.cashpet.core.task.TaskDef
import kotlinx.serialization.Serializable

/** Весь игровой контент после загрузки (docs/08-контракты.md, раздел 7). */
data class GameContent(
    val economy: EconomyConfig,
    val catalog: Catalog,                              // товары, цели, подработки, TaskMeta — то, что нужно движку
    val tasks: List<TaskDef>,                          // задания целиком — для TaskSession
    val pets: PetOptions,
    val texts: Texts,
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
