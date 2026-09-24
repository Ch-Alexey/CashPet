package io.github.chalexey.cashpet.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Все числа экономики — из economy.json (docs/02-экономика.md), в коде их нет

@Serializable
data class EconomyConfig(
    @SerialName("start_budget") val startBudget: Int,
    @SerialName("pocket_money") val pocketMoney: Int,
    @SerialName("job_reward") val jobReward: Int,
    @SerialName("jobs_per_week") val jobsPerWeek: Int,
    val tolerance: Map<Difficulty, Int>,                              // 20 / 10
    @SerialName("need_min") val needMin: Int,                         // «коту нужно 50», подстановка {needMin}
    @SerialName("need_hint") val needHint: Map<Difficulty, NeedHint>,
    val weights: Weights,
    @SerialName("stage_thresholds") val stageThresholds: Map<Stage, Int>,  // TEEN 150, ADULT 350
    val stats: StatsConfig,
    @SerialName("need_food_points") val needFoodPoints: Int,         // 40
    @SerialName("need_care_points") val needCarePoints: Int,         // 25
    @SerialName("min_save_target") val minSaveTarget: Int,           // 10
    val mood: MoodThresholds,
)

@Serializable
data class Weights(val n: Int, val m: Int, val s: Int)                // 40 / 30 / 30

@Serializable
data class StatsConfig(
    val start: Int,                                                   // 70
    val floor: Int,                                                   // 25
    val cap: Int,                                                     // 100
    @SerialName("decay_pct") val decayPct: Map<Stat, Int>,            // 75 / 80 / 70
    @SerialName("hungry_below") val hungryBelow: Int,                 // 50 — карточка кота среди недели
    @SerialName("unkempt_below") val unkemptBelow: Int,               // 50
)

@Serializable
data class MoodThresholds(val happy: Int, val calm: Int)             // 75 / 50

// Каталог — то, что движку нужно знать о контенте

@Serializable
data class ShopItem(
    val id: String,
    val name: String,
    val part: Part,                                                   // NEED или WANT
    val price: Int,
    val effects: Map<Stat, Int> = emptyMap(),
    val accessory: Boolean = false,
)

@Serializable
data class GoalDef(val id: String, val name: String, val cost: Int)

@Serializable
data class JobDef(val id: String, val title: String, val reward: Int)

/** Сведения о задании, нужные движку. Полное задание (шаги, варианты) — TaskDef в core.task. */
@Serializable
data class TaskMeta(val id: String, val topic: Topic, val unlockWeek: Int, val rewardCoins: Int)

data class Catalog(
    val items: List<ShopItem>,
    val goals: List<GoalDef>,
    val jobs: List<JobDef>,
    val tasks: List<TaskMeta>,
) {
    fun item(id: String): ShopItem? = items.firstOrNull { it.id == id }
    fun goal(id: String): GoalDef? = goals.firstOrNull { it.id == id }
    fun job(id: String): JobDef? = jobs.firstOrNull { it.id == id }
    fun task(id: String): TaskMeta? = tasks.firstOrNull { it.id == id }
}
