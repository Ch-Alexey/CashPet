package io.github.chalexey.cashpet.core.task

import io.github.chalexey.cashpet.core.model.EffectKey
import io.github.chalexey.cashpet.core.model.Outcome
import io.github.chalexey.cashpet.core.model.TaskMeta
import io.github.chalexey.cashpet.core.model.Topic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Задание целиком — формат tasks.json из docs/07-формат-заданий.md. Движку нужна только TaskMeta,
// шаги и варианты читает TaskSession

@Serializable
data class TaskDef(
    val id: String,
    val module: Int,
    val title: String,
    val topic: Topic,
    val competence: Int? = null,                       // 1..6 из раздела 1 ТЗ; у вводного задания нет
    @SerialName("unlock_week") val unlockWeek: Int,
    val reward: Reward,
    @SerialName("neighbor_lines") val neighborLines: List<NeighborLine> = emptyList(),
    val intro: String? = null,
    val steps: List<StepDef>,
    @SerialName("hard_variant") val hardVariant: HardVariant? = null,
) {
    fun meta(): TaskMeta = TaskMeta(id = id, topic = topic, unlockWeek = unlockWeek, rewardCoins = reward.coins)
}

@Serializable
data class Reward(val coins: Int)

@Serializable
data class NeighborLine(val character: String, val text: String)  // character — ключ из neighbors.json

@Serializable
enum class StepTemplate {
    @SerialName("choice") CHOICE,                      // к 29.09 — только «Выбор»
}

@Serializable
data class StepDef(
    val id: String,
    val template: StepTemplate = StepTemplate.CHOICE,
    val wallet: Int? = null,                           // учебный кошелёк шага, настоящий баланс не трогает
    val savings: Int? = null,                          // учебная копилка шага
    val situation: String,
    val options: List<OptionDef>,
)

@Serializable
data class OptionDef(
    val id: String,
    val label: String,
    val effects: Map<EffectKey, Int> = emptyMap(),
    val consequence: String? = null,
    val feedback: String,
    val outcome: Outcome? = null,                      // нет только у blocked
    val recovery: String? = null,                      // обязателен при OK и RETRY
    val blocked: Boolean = false,                      // вариант-заглушка: не засчитывается, ведёт в followup
    val followup: Followup? = null,
)

@Serializable
data class Followup(val prompt: String, val buttons: List<String>)

/** Замены для «посложнее»: по id шага и варианта, только тексты и эффекты. */
@Serializable
data class HardVariant(val steps: Map<String, StepOverride> = emptyMap())

@Serializable
data class StepOverride(
    val situation: String? = null,
    val options: Map<String, OptionOverride> = emptyMap(),
)

@Serializable
data class OptionOverride(
    val label: String? = null,
    val effects: Map<EffectKey, Int>? = null,
    val consequence: String? = null,
    val feedback: String? = null,
)
