package io.github.chalexey.cashpet.content

import io.github.chalexey.cashpet.core.engine.GameEngine
import io.github.chalexey.cashpet.core.model.Difficulty
import io.github.chalexey.cashpet.core.model.GameState
import io.github.chalexey.cashpet.core.model.PetLook
import io.github.chalexey.cashpet.core.model.PetMood
import io.github.chalexey.cashpet.core.model.Profile
import io.github.chalexey.cashpet.core.sim.Autoplay
import io.github.chalexey.cashpet.core.sim.ReasonableStrategy
import io.github.chalexey.cashpet.core.sim.SaverStrategy
import io.github.chalexey.cashpet.core.sim.SpenderStrategy
import io.github.chalexey.cashpet.core.sim.Strategy
import java.io.File

/**
 * Симулятор баланса для Ярика: `./gradlew :content:simulate` (на Windows — `gradlew :content:simulate`).
 * Берёт настоящие JSON из src/main/resources/content, прогоняет 5 недель трёх сценариев через движок
 * и печатает таблицы как в docs/02-экономика.md. Итог также пишется в content/build/simulate.md —
 * его удобно открыть в Android Studio, если в терминале не видно кириллицу.
 */
fun main(args: Array<String>) {
    val weeks = args.firstOrNull()?.toIntOrNull() ?: 5
    val content = ContentLoader().load()
    val report = buildString {
        appendLine("# Симулятор баланса — ${java.time.LocalDate.now()}")
        appendLine()
        appendLine("Настоящий контент, «посложнее» (допуск 10), $weeks недель. Сравнить с таблицами docs/02-экономика.md.")
        appendLine()
        appendLine("## Неравенства раздела 19")
        appendLine()
        for (c in BalanceChecks.run(content)) appendLine("- ${if (c.ok) "OK  " else "FAIL"} ${c.name}: ${c.detail}")
        val engine = GameEngine(content.economy, content.catalog)
        // Как в таблицах экономики: сначала Домик, потом Велосипед
        val goals = listOf("goal_house", "goal_bike").filter { id -> content.catalog.goals.any { it.id == id } }
            .ifEmpty { content.catalog.goals.map { it.id } }
        val scenarios = listOf(
            "Разумная игра" to ReasonableStrategy(content.economy, content.catalog, goals),
            "Транжира" to SpenderStrategy(content.economy, content.catalog, goals),
            "Скопидом" to SaverStrategy(content.economy, content.catalog, goals),
        )
        for ((name, strategy) in scenarios) {
            appendLine()
            appendLine("## $name")
            appendLine()
            append(table(engine, strategy, content, weeks))
        }
    }
    print(report)
    val out = File("build/simulate.md")
    out.parentFile.mkdirs()
    out.writeText(report)
    println("\nСохранено: ${out.absolutePath}")
}

private fun table(engine: GameEngine, strategy: Strategy, content: GameContent, weeks: Int): String = buildString {
    val start: GameState = engine.newGame(Profile("Тест", Difficulty.HARD), "Пончик", PetLook("fluffy", "ginger"))
    val states = Autoplay.weeks(start, engine, strategy, weeks)
    appendLine("| Неделя | Доступно | План нужн./жел./копилка | Заработал | Нужное | Желаемое | В копилку | Копилка | Баланс | N | M | S | GP | ΣGP | Стадия | Кот |")
    appendLine("|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|")
    states.forEachIndexed { i, s ->
        val before = if (i == 0) start else states[i - 1]
        val r = s.history.last()
        val mood = when (engine.petMood(r.statsBefore)) {
            PetMood.HAPPY -> "радуется"; PetMood.CALM -> "спокоен"; PetMood.SAD -> "грустит"
        }
        val goal = if (s.savings.boughtGoalIds.size > before.savings.boughtGoalIds.size) " ✓цель" else ""
        appendLine(
            "| ${r.number} | ${before.week.available} | ${r.plan.need}/${r.plan.want}/${r.plan.save} | ${r.earned} | " +
                "${r.factNeed} | ${r.factWant} | ${r.factSaved} | ${s.savings.total}$goal | ${s.balance - content.economy.pocketMoney} | " +
                "${r.nPct}% | ${r.mPct}% | ${r.sPct}% | ${r.gp} | ${r.totalGp} | ${r.stageAfter.ordinal + 1} | $mood |",
        )
    }
}
