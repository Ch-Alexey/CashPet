package io.github.chalexey.cashpet.core.sim

import io.github.chalexey.cashpet.core.engine.Action
import io.github.chalexey.cashpet.core.engine.GameEngine
import io.github.chalexey.cashpet.core.engine.Result
import io.github.chalexey.cashpet.core.model.Catalog
import io.github.chalexey.cashpet.core.model.EconomyConfig
import io.github.chalexey.cashpet.core.model.GameState
import io.github.chalexey.cashpet.core.model.Outcome
import io.github.chalexey.cashpet.core.model.Part
import io.github.chalexey.cashpet.core.model.Plan
import io.github.chalexey.cashpet.core.model.ShopItem
import io.github.chalexey.cashpet.core.model.Stage
import io.github.chalexey.cashpet.core.model.Stat

/** Стратегия игры на неделю: «разумная игра», «транжира», «скопидом» (docs/08-контракты.md, раздел 5а). */
interface Strategy {
    /** Действия одной недели из текущего состояния, последнее — CloseWeek. */
    fun weekActions(state: GameState, engine: GameEngine): List<Action>
}

/** Автопрогон для кнопки «Ускорить» и симулятора: каждое действие — через engine.apply. */
object Autoplay {
    fun run(
        state: GameState,
        engine: GameEngine,
        strategy: Strategy,
        target: Stage,
        maxWeeks: Int = 6,
    ): List<Result.Ok> {
        val results = mutableListOf<Result.Ok>()
        var current = state
        var weeks = 0
        while (current.pet.stage < target && weeks < maxWeeks) {
            for (action in strategy.weekActions(current, engine)) {
                val result = engine.apply(current, action)
                check(result is Result.Ok) { "Стратегия сделала недопустимое действие $action: $result" }
                results += result
                current = result.state
            }
            weeks++
        }
        return results
    }

    /** Ровно [weeks] недель — для симулятора и сравнения с таблицами docs/02-экономика.md. */
    fun weeks(state: GameState, engine: GameEngine, strategy: Strategy, weeks: Int): List<GameState> {
        val states = mutableListOf<GameState>()
        var current = state
        repeat(weeks) {
            for (action in strategy.weekActions(current, engine)) {
                val result = engine.apply(current, action)
                check(result is Result.Ok) { "Стратегия сделала недопустимое действие $action: $result" }
                current = result.state
            }
            states += current
        }
        return states
    }
}

/**
 * Общая основа стратегий. Решения принимаются по ходу недели: каждое действие сразу прогоняется
 * через движок, поэтому стратегия видит настоящий баланс и никогда не дублирует правила экономики.
 */
abstract class BaseStrategy(
    protected val economy: EconomyConfig,
    protected val catalog: Catalog,
    /** Порядок целей копилки; по умолчанию — как в каталоге. */
    private val goalOrder: List<String> = catalog.goals.map { it.id },
) : Strategy {

    protected class Week(private val engine: GameEngine, var state: GameState) {
        val actions = mutableListOf<Action>()

        /** Выполнить, если движок разрешает; отказ — просто пропуск. */
        fun tryDo(action: Action): Boolean {
            val result = engine.apply(state, action)
            if (result !is Result.Ok) return false
            state = result.state
            actions += action
            return true
        }
    }

    /** Еда: из товаров, которые в одиночку закрывают «нужное», — самая выгодная на монету. */
    protected val food: ShopItem get() = bestNeedItem(Stat.SATIETY, economy.needFoodPoints)

    /** Уход: так же. */
    protected val care: ShopItem get() = bestNeedItem(Stat.CARE, economy.needCarePoints)

    protected val cheapestCare: ShopItem
        get() = catalog.items.filter { it.part == Part.NEED && (it.effects[Stat.CARE] ?: 0) > 0 }.minBy { it.price }

    private fun bestNeedItem(stat: Stat, points: Int): ShopItem =
        catalog.items
            .filter { it.part == Part.NEED && (it.effects[stat] ?: 0) >= points }
            .maxWith(compareBy<ShopItem> { (it.effects[stat] ?: 0).toDouble() / it.price }.thenBy { -it.price })

    /** Пройти открытые задания; [onlyNew] — только открывшиеся на этой неделе. */
    protected fun doOpenTasks(week: Week, limit: Int = Int.MAX_VALUE, onlyNew: Boolean = false, engine: GameEngine) {
        catalog.tasks
            .filter { engine.isTaskOpen(week.state, it.id) && it.id !in week.state.tasks }
            .filter { !onlyNew || it.unlockWeek == week.state.week.number }
            .take(limit)
            .forEach { week.tryDo(Action.CompleteTask(it.id, Outcome.GOOD)) }
    }

    protected fun doJobs(week: Week, count: Int) {
        val job = catalog.jobs.firstOrNull() ?: return
        repeat(count) { week.tryDo(Action.DoJob(job.id)) }
    }

    /** Желаемое: самое дорогое, что влезает в [limit] монет, и дальше по убыванию цены. */
    protected fun buyWants(week: Week, limit: Int) {
        var left = limit
        for (item in catalog.items.filter { it.part == Part.WANT }.sortedByDescending { it.price }) {
            if (item.price <= left && week.tryDo(Action.Buy(item.id))) left -= item.price
        }
    }

    /** Выбрать следующую цель, положить [amount] и купить мечту, если хватает. */
    protected fun save(week: Week, amount: Int) {
        if (week.state.savings.activeGoalId == null) {
            goalOrder.firstOrNull { it !in week.state.savings.boughtGoalIds }?.let { week.tryDo(Action.ChooseGoal(it)) }
        }
        val deposit = minOf(amount, week.state.balance) / 5 * 5
        if (deposit > 0) week.tryDo(Action.Deposit(deposit))
        week.tryDo(Action.BuyGoal)
    }

    protected fun planOf(need: Int, want: Int, save: Int, available: Int): Plan {
        val n = minOf(need, available)
        val s = minOf(save, available - n)
        val w = minOf(want, available - n - s)
        return Plan(n, maxOf(0, w), maxOf(0, s))
    }
}

/**
 * Разумная игра: нужное = needMin, половина свободного — в копилку (не меньше минимума), остальное — желаемое.
 *
 * Доигрывает и начатую неделю — «Ускорить» в демо можно нажать посреди недели: план берётся уже
 * подтверждённый, купленное нужное не покупается второй раз, желаемое и копилка — только на остаток плана.
 */
class ReasonableStrategy(economy: EconomyConfig, catalog: Catalog, goalOrder: List<String> = catalog.goals.map { it.id }) :
    BaseStrategy(economy, catalog, goalOrder) {

    override fun weekActions(state: GameState, engine: GameEngine): List<Action> {
        val week = Week(engine, state)
        val plan = if (state.week.planConfirmed) state.week.plan ?: Plan() else newPlan(state.week.available)
        week.tryDo(Action.SetPlan(plan))
        week.tryDo(Action.ConfirmPlan)
        doOpenTasks(week, engine = engine)
        doJobs(week, economy.jobsPerWeek)
        if (week.state.week.foodPoints < economy.needFoodPoints) week.tryDo(Action.Buy(food.id))
        if (week.state.week.carePoints < economy.needCarePoints) week.tryDo(Action.Buy(care.id))
        buyWants(week, plan.want - week.state.week.spentWant)
        // Не меньше минимума: иначе иконка «Копилка растёт» в демо горит наполовину
        val savedSoFar = maxOf(0, week.state.week.deposited - week.state.week.withdrawn)
        save(week, maxOf(plan.save, economy.minSaveTarget) - savedSoFar)
        week.tryDo(Action.CloseWeek)
        return week.actions
    }

    private fun newPlan(available: Int): Plan {
        val free = maxOf(0, available - economy.needMin)
        val save = minOf(maxOf(economy.minSaveTarget, free / 2 / 5 * 5), free)
        return planOf(economy.needMin, free - save, save, available)
    }
}

/** Транжира: нужное покупает, всё остальное тратит на желаемое, копилку планирует, но не пополняет. */
class SpenderStrategy(economy: EconomyConfig, catalog: Catalog, goalOrder: List<String> = catalog.goals.map { it.id }) :
    BaseStrategy(economy, catalog, goalOrder) {

    override fun weekActions(state: GameState, engine: GameEngine): List<Action> {
        val week = Week(engine, state)
        val available = state.week.available
        val plan = planOf(economy.needMin, available - economy.needMin - economy.minSaveTarget, economy.minSaveTarget, available)
        week.tryDo(Action.SetPlan(plan))
        week.tryDo(Action.ConfirmPlan)
        doOpenTasks(week, limit = 1, onlyNew = true, engine = engine)
        doJobs(week, 1)
        week.tryDo(Action.Buy(food.id))
        week.tryDo(Action.Buy(care.id))
        buyWants(week, week.state.balance)
        week.tryDo(Action.CloseWeek)
        return week.actions
    }
}

/** Скопидом: на нужное — только корм, уход через неделю, желаемого нет, всё — в копилку. */
class SaverStrategy(economy: EconomyConfig, catalog: Catalog, goalOrder: List<String> = catalog.goals.map { it.id }) :
    BaseStrategy(economy, catalog, goalOrder) {

    override fun weekActions(state: GameState, engine: GameEngine): List<Action> {
        val week = Week(engine, state)
        val available = state.week.available
        val plan = planOf(food.price, 0, available - food.price, available)
        week.tryDo(Action.SetPlan(plan))
        week.tryDo(Action.ConfirmPlan)
        doOpenTasks(week, engine = engine)
        doJobs(week, economy.jobsPerWeek)
        week.tryDo(Action.Buy(food.id))
        if (state.week.number % 2 == 0) week.tryDo(Action.Buy(cheapestCare.id))
        save(week, week.state.balance)
        week.tryDo(Action.CloseWeek)
        return week.actions
    }
}
