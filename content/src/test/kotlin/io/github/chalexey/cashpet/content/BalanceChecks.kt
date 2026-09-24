package io.github.chalexey.cashpet.content

import io.github.chalexey.cashpet.core.engine.GameEngine
import io.github.chalexey.cashpet.core.model.Difficulty
import io.github.chalexey.cashpet.core.model.Part
import io.github.chalexey.cashpet.core.model.PetLook
import io.github.chalexey.cashpet.core.model.Profile
import io.github.chalexey.cashpet.core.sim.Autoplay
import io.github.chalexey.cashpet.core.sim.ReasonableStrategy

/**
 * Неравенства баланса из раздела 19 рамок по настоящему контенту (docs/02-экономика.md).
 * Одни и те же проверки для тест-сторожа и для симулятора `./gradlew :content:simulate`.
 */
object BalanceChecks {
    data class Check(val name: String, val ok: Boolean, val detail: String)

    fun run(content: GameContent): List<Check> {
        val e = content.economy
        val wants = content.catalog.items.filter { it.part == Part.WANT }.map { it.price }
        val need = e.needMin                                                         // O — обязательное за неделю
        val earned = content.catalog.tasks.filter { it.unlockWeek == 1 }.sumOf { it.rewardCoins } +
            e.jobsPerWeek * e.jobReward                                              // заработок первой недели
        val income = e.pocketMoney + earned                                          // I
        val free = income - need                                                     // S
        val goalAvg = content.catalog.goals.map { it.cost }.average()

        // 8б: свободные монеты самой «богатой» недели разумной игры — с переносом остатка
        val engine = GameEngine(e, content.catalog)
        val start = engine.newGame(Profile("Тест", Difficulty.HARD), "Пончик", PetLook("fluffy", "ginger"))
        val weeks = Autoplay.weeks(start, engine, ReasonableStrategy(e, content.catalog), weeks = 5)
        val freeMax = weeks.mapIndexed { i, s ->
            val r = s.history.last()
            (if (i == 0) start else weeks[i - 1]).week.available + r.earned - r.factNeed
        }.max()

        return listOf(
            Check("1. O ≈ 0,5 · I", need * 10 in income * 4..income * 6, "$need из $income"),
            Check("2. P_min < S", wants.min() < free, "${wants.min()} < $free"),
            Check("3. ΣP ≥ 3 · S", wants.sum() >= 3 * free, "${wants.sum()} ≥ ${3 * free}"),
            Check("4. P_max > 0,5 · S", 2 * wants.max() > free, "${wants.max()} > ${free / 2.0}"),
            Check("5. C_средняя ≈ 3,5 · 0,5 · S", kotlin.math.abs(goalAvg - 1.75 * free) <= 25, "${"%.0f".format(goalAvg)} ≈ ${1.75 * free}"),
            // 6. U ≤ 0,8 · S — непредвиденный расход после 29.09, пластыря на витрине нет
            Check("7. заработок ≈ 0,4–0,6 · I", earned * 10 in income * 4..income * 6, "$earned из $income"),
            Check("8а. нет тупика: карманные ≥ O", e.pocketMoney >= need, "${e.pocketMoney} ≥ $need"),
            Check("8б. перенос: ΣP ≥ 2 · S_max", wants.sum() >= 2 * freeMax, "${wants.sum()} ≥ ${2 * freeMax}"),
        )
    }
}
