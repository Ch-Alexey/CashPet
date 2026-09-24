package io.github.chalexey.cashpet.core

import io.github.chalexey.cashpet.core.engine.GameEngine
import io.github.chalexey.cashpet.core.model.Catalog
import io.github.chalexey.cashpet.core.model.Difficulty
import io.github.chalexey.cashpet.core.model.EconomyConfig
import io.github.chalexey.cashpet.core.model.GoalDef
import io.github.chalexey.cashpet.core.model.JobDef
import io.github.chalexey.cashpet.core.model.MoodThresholds
import io.github.chalexey.cashpet.core.model.NeedHint
import io.github.chalexey.cashpet.core.model.Part
import io.github.chalexey.cashpet.core.model.PetLook
import io.github.chalexey.cashpet.core.model.Profile
import io.github.chalexey.cashpet.core.model.ShopItem
import io.github.chalexey.cashpet.core.model.Slot
import io.github.chalexey.cashpet.core.model.Stage
import io.github.chalexey.cashpet.core.model.Stat
import io.github.chalexey.cashpet.core.model.StatsConfig
import io.github.chalexey.cashpet.core.model.TaskMeta
import io.github.chalexey.cashpet.core.model.Topic
import io.github.chalexey.cashpet.core.model.Weights

/** Экономика и каталог из docs/02-экономика.md — те же числа, что в tools/econ_sim.py и образцах content. */
object TestContent {
    val economy = EconomyConfig(
        startBudget = 100, pocketMoney = 60, jobReward = 10, jobsPerWeek = 2,
        tolerance = mapOf(Difficulty.EASY to 20, Difficulty.HARD to 10),
        needMin = 50,
        needHint = mapOf(Difficulty.EASY to NeedHint.ALWAYS, Difficulty.HARD to NeedHint.FIRST_WEEK),
        weights = Weights(n = 40, m = 30, s = 30),
        stageThresholds = mapOf(Stage.TEEN to 150, Stage.ADULT to 350),
        stats = StatsConfig(
            start = 70, floor = 25, cap = 100,
            decayPct = mapOf(Stat.SATIETY to 75, Stat.CARE to 80, Stat.MOOD to 70),
            hungryBelow = 50, unkemptBelow = 50,
        ),
        needFoodPoints = 40, needCarePoints = 25, minSaveTarget = 10,
        mood = MoodThresholds(happy = 75, calm = 50),
    )

    private fun need(id: String, price: Int, vararg effects: Pair<Stat, Int>) = ShopItem(id, id, Part.NEED, price, effects.toMap())
    private fun want(id: String, price: Int, mood: Int) = ShopItem(id, id, Part.WANT, price, mapOf(Stat.MOOD to mood))

    val catalog = Catalog(
        items = listOf(
            need("food_vitamins", 15, Stat.SATIETY to 20),
            need("food_basic", 30, Stat.SATIETY to 40),
            need("food_fish", 45, Stat.SATIETY to 50, Stat.MOOD to 5),
            need("food_premium", 60, Stat.SATIETY to 65, Stat.MOOD to 10),
            need("care_comb", 15, Stat.CARE to 25),
            need("care_shampoo", 20, Stat.CARE to 40),
            need("care_towel", 20, Stat.CARE to 30),
            need("care_plaster", 25, Stat.CARE to 30),
            want("want_rattle", 10, 10),
            want("want_ball", 15, 15),
            want("want_plush_mouse", 20, 18),
            want("want_bow", 25, 20),
            want("want_cap", 35, 25),
            want("want_bed", 40, 22),
            want("want_jacket", 55, 30),
            want("want_scooter", 80, 40),
        ),
        goals = listOf(GoalDef("goal_scratcher", "Когтеточка", 60), GoalDef("goal_house", "Домик", 100), GoalDef("goal_bike", "Велосипед", 150)),
        jobs = listOf(JobDef("help_home", "Помочь по дому", 10)),
        // 7 заданий MVP: награды и недели открытия — как TASKS_BY_WEEK в tools/econ_sim.py
        tasks = listOf(
            TaskMeta("0", Topic.INTRO, 1, 20),
            TaskMeta("1.1", Topic.PURCHASES, 1, 15),
            TaskMeta("4.2", Topic.PURCHASES, 2, 20),
            TaskMeta("2.3", Topic.BUDGET, 2, 20),
            TaskMeta("3.1", Topic.SAVINGS, 3, 15),
            TaskMeta("3.3", Topic.SAVINGS, 3, 25),
            TaskMeta("6.2", Topic.BUDGET, 4, 15),
        ),
    )

    val engine = GameEngine(economy, catalog)

    fun newGame(difficulty: Difficulty = Difficulty.HARD, slot: Slot = Slot.CHILD) =
        engine.newGame(Profile("Лис", difficulty, slot), "Пончик", PetLook("fluffy", "ginger"))
}
