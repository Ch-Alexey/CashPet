package io.github.chalexey.cashpet.core.model

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SerializationTest {
    // Те же настройки, что обязан использовать data (docs/08-контракты.md, раздел 6)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `состояние игры сохраняется и читается без потерь`() {
        val state = GameState(
            profile = Profile(playerName = "Лис", difficulty = Difficulty.HARD),
            pet = Pet(name = "Пончик", look = PetLook("fluffy", "ginger"), totalGp = 120),
            balance = 55,
            week = Week(number = 2, available = 115, plan = Plan(50, 35, 30), planConfirmed = true),
            transactions = listOf(Transaction(1, TxSource.Deposit, -25, 25, 30, 25)),
            tasks = mapOf("1.1" to TaskRecord("1.1", Topic.PURCHASES, Outcome.GOOD, 1)),
        )
        assertEquals(state, json.decodeFromString<GameState>(json.encodeToString(state)))
    }

    @Test
    fun `сохранение из старой версии читается`() {
        // Поля, которых нет, берутся по умолчанию; незнакомые — пропускаются
        val old = """{"balance": 40, "pet": {"name": "Мурка"}, "field_from_future": 1}"""
        val state = json.decodeFromString<GameState>(old)
        assertEquals(40, state.balance)
        assertEquals("Мурка", state.pet.name)
        assertEquals(Stage.BABY, state.pet.stage)
        assertEquals(1, state.week.number)
    }

    @Test
    fun `конфиг экономики читается из json в формате economy_json`() {
        val text = """
            {
              "start_budget": 100, "pocket_money": 60, "job_reward": 10, "jobs_per_week": 2,
              "tolerance": {"easy": 20, "hard": 10},
              "need_min": 50,
              "need_hint": {"easy": "always", "hard": "first_week"},
              "weights": {"n": 40, "m": 30, "s": 30},
              "stage_thresholds": {"teen": 150, "adult": 350},
              "stats": {"start": 70, "floor": 25, "cap": 100,
                        "decay_pct": {"satiety": 75, "care": 80, "mood": 70},
                        "hungry_below": 50, "unkempt_below": 50},
              "need_food_points": 40, "need_care_points": 25, "min_save_target": 10,
              "mood": {"happy": 75, "calm": 50}
            }
        """
        val config = json.decodeFromString<EconomyConfig>(text)
        assertEquals(20, config.tolerance[Difficulty.EASY])
        assertEquals(350, config.stageThresholds[Stage.ADULT])
        assertEquals(75, config.stats.decayPct[Stat.SATIETY])
        assertEquals(NeedHint.FIRST_WEEK, config.needHint[Difficulty.HARD])
    }
}
