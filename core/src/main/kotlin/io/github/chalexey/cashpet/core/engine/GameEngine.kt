package io.github.chalexey.cashpet.core.engine

import io.github.chalexey.cashpet.core.model.Catalog
import io.github.chalexey.cashpet.core.model.EconomyConfig
import io.github.chalexey.cashpet.core.model.FeedbackReason
import io.github.chalexey.cashpet.core.model.GameState
import io.github.chalexey.cashpet.core.model.Outcome
import io.github.chalexey.cashpet.core.model.Part
import io.github.chalexey.cashpet.core.model.Pet
import io.github.chalexey.cashpet.core.model.PetLook
import io.github.chalexey.cashpet.core.model.PetMood
import io.github.chalexey.cashpet.core.model.PetReason
import io.github.chalexey.cashpet.core.model.PetStats
import io.github.chalexey.cashpet.core.model.Plan
import io.github.chalexey.cashpet.core.model.Profile
import io.github.chalexey.cashpet.core.model.ShopItem
import io.github.chalexey.cashpet.core.model.Slot
import io.github.chalexey.cashpet.core.model.Stage
import io.github.chalexey.cashpet.core.model.Stat
import io.github.chalexey.cashpet.core.model.TaskRecord
import io.github.chalexey.cashpet.core.model.Transaction
import io.github.chalexey.cashpet.core.model.TxSource
import io.github.chalexey.cashpet.core.model.Way
import io.github.chalexey.cashpet.core.model.Week
import io.github.chalexey.cashpet.core.model.WeekResult
import io.github.chalexey.cashpet.core.percentOf

/**
 * Движок игры: чистые функции без Android, времени и корутин.
 * Правила — docs/08-контракты.md, раздел 4; формулы — docs/02-экономика.md; числа — из [economy].
 */
class GameEngine(private val economy: EconomyConfig, private val catalog: Catalog) {

    /** Новая игра: неделя 1, стартовый бюджет. */
    fun newGame(profile: Profile, petName: String, look: PetLook): GameState {
        val start = economy.stats.start
        val budget = economy.startBudget
        return GameState(
            profile = profile,
            pet = Pet(name = petName, look = look, stats = PetStats(start, start, start)),
            balance = budget,
            week = Week(number = 1, available = budget),
            transactions = listOf(Transaction(1, TxSource.StartBudget, budget, 0, budget, 0)),
        )
    }

    fun apply(state: GameState, action: Action): Result = when (action) {
        is Action.SetPlan -> setPlan(state, action.plan)
        Action.ConfirmPlan -> confirmPlan(state)
        is Action.Buy -> buy(state, action.itemId)
        is Action.AddToWishlist -> addToWishlist(state, action.itemId)
        is Action.RemoveFromWishlist ->
            ok(state, state.copy(wishlist = state.wishlist - action.itemId), FeedbackReason.WISHLIST_REMOVED)
        is Action.ChooseGoal -> chooseGoal(state, action.goalId)
        is Action.Deposit -> deposit(state, action.amount)
        is Action.Withdraw -> withdraw(state, action.amount)
        Action.BuyGoal -> buyGoal(state)
        is Action.CompleteTask -> completeTask(state, action.taskId, action.outcome)
        is Action.DoJob -> doJob(state, action.jobId)
        Action.CloseWeek -> closeWeek(state)
        is Action.ChangeLook ->
            ok(state, state.copy(pet = state.pet.copy(look = action.look)), FeedbackReason.LOOK_CHANGED)
    }

    /** Срок до цели в неделях; null — нет цели или нечего усреднять (docs/02-экономика.md, «Цели копилки»). */
    fun weeksToGoal(state: GameState): Int? {
        val goal = state.savings.activeGoalId?.let(catalog::goal) ?: return null
        val left = goal.cost - state.savings.total
        if (left <= 0) return 0
        // Среднее «отложено за неделю» по последним закрытым неделям, пока их нет — по текущей
        val history = state.savings.recentWeeklySaved
        val sum = if (history.isNotEmpty()) history.sum() else savedThisWeek(state.week)
        val count = if (history.isNotEmpty()) history.size else 1
        if (sum <= 0) return null
        return ceilDiv(left * count, sum)
    }

    fun previewWithdraw(state: GameState, amount: Int): WithdrawPreview {
        val savedAfter = (state.savings.total - amount).coerceAtLeast(0)
        val after = state.copy(
            savings = state.savings.copy(total = savedAfter),
            week = state.week.copy(withdrawn = state.week.withdrawn + amount),
        )
        return WithdrawPreview(state.savings.total, savedAfter, weeksToGoal(state), weeksToGoal(after))
    }

    /** Общее состояние = 0,4 · настроение + 0,3 · сытость + 0,3 · уход, в целых числах: × 10. */
    fun petMood(stats: PetStats): PetMood {
        val value10 = 4 * stats.mood + 3 * stats.satiety + 3 * stats.care
        return when {
            value10 >= 10 * economy.mood.happy -> PetMood.HAPPY
            value10 >= 10 * economy.mood.calm -> PetMood.CALM
            else -> PetMood.SAD
        }
    }

    /** Причина для карточки кота среди недели — только по текущим показателям, чтобы не пугать «без еды». */
    fun petReasonNow(stats: PetStats): PetReason = when {
        stats.satiety < economy.stats.hungryBelow -> PetReason.HUNGRY
        stats.care < economy.stats.unkemptBelow -> PetReason.UNKEMPT
        else -> moodReason(petMood(stats))
    }

    /** Задание открыто: unlock_week ≤ номер недели или демо-профиль. */
    fun isTaskOpen(state: GameState, taskId: String): Boolean {
        val task = catalog.task(taskId) ?: return false
        return state.profile.slot == Slot.DEMO || task.unlockWeek <= state.week.number
    }

    // --- План ---

    private fun setPlan(state: GameState, plan: Plan): Result {
        if (state.week.planConfirmed) return rejected(Rejection.PlanAlreadyConfirmed)
        if (listOf(plan.need, plan.want, plan.save).any { it < 0 || it % 5 != 0 }) {
            return rejected(Rejection.InvalidAmount)
        }
        if (plan.total > state.week.available) {
            return rejected(Rejection.PlanExceedsBudget(plan.total - state.week.available))
        }
        return ok(state, state.copy(week = state.week.copy(plan = plan)), FeedbackReason.PLAN_SAVED)
    }

    private fun confirmPlan(state: GameState): Result {
        if (state.week.planConfirmed) return rejected(Rejection.PlanAlreadyConfirmed)
        val week = state.week.copy(plan = state.week.plan ?: Plan(), planConfirmed = true)
        return ok(state, state.copy(week = week), FeedbackReason.PLAN_CONFIRMED)
    }

    // --- Покупки ---

    private fun buy(state: GameState, itemId: String): Result {
        val item = catalog.item(itemId)?.takeIf { it.part != Part.SAVE }
            ?: return rejected(Rejection.UnknownId(itemId))
        if (!state.week.planConfirmed) return rejected(Rejection.PlanNotConfirmed)
        if (item.price > state.balance) {
            val missing = item.price - state.balance
            return rejected(Rejection.NotEnoughMoney(missing, waysOut(state, missing, item)))
        }
        val balance = state.balance - item.price
        val week = state.week.copy(
            spentNeed = state.week.spentNeed + if (item.part == Part.NEED) item.price else 0,
            spentWant = state.week.spentWant + if (item.part == Part.WANT) item.price else 0,
            // Для N — эффект покупки до ограничения в 100, как в симуляторе
            foodPoints = state.week.foodPoints + (item.effects[Stat.SATIETY] ?: 0),
            carePoints = state.week.carePoints + (item.effects[Stat.CARE] ?: 0),
        )
        val after = state.copy(
            pet = state.pet.copy(stats = withEffects(state.pet.stats, item.effects)),
            balance = balance,
            week = week,
            wishlist = state.wishlist - itemId,
            transactions = state.transactions +
                Transaction(week.number, TxSource.Purchase(itemId), -item.price, 0, balance, state.savings.total),
        )
        val reason = if (item.part == Part.NEED) FeedbackReason.BOUGHT_NEED else FeedbackReason.BOUGHT_WANT
        return ok(state, after, reason)
    }

    private fun addToWishlist(state: GameState, itemId: String): Result {
        if (catalog.item(itemId) == null) return rejected(Rejection.UnknownId(itemId))
        val wishlist = if (itemId in state.wishlist) state.wishlist else state.wishlist + itemId
        return ok(state, state.copy(wishlist = wishlist), FeedbackReason.WISHLIST_ADDED)
    }

    /** Варианты «Что можно сделать?» — только те, что сейчас действительно доступны. */
    private fun waysOut(state: GameState, missing: Int, item: ShopItem?): List<Way> = buildList {
        if (catalog.tasks.any { isTaskOpen(state, it.id) && it.id !in state.tasks && it.rewardCoins > 0 }) {
            add(Way.DO_TASK)
        }
        if (catalog.jobs.isNotEmpty() && state.week.jobsDone < economy.jobsPerWeek) add(Way.DO_JOB)
        // item == null — не хватает на взнос: «взять из копилки», чтобы положить в копилку, не предлагаем
        if (item != null) {
            if (catalog.items.any { it.part == item.part && it.price < item.price && it.price <= state.balance }) {
                add(Way.CHEAPER_ITEM)
            }
            add(Way.WISHLIST)
            if (state.savings.total >= missing) add(Way.TAKE_FROM_SAVINGS)
        }
    }

    // --- Копилка ---

    private fun chooseGoal(state: GameState, goalId: String): Result {
        if (catalog.goal(goalId) == null) return rejected(Rejection.UnknownId(goalId))
        if (goalId in state.savings.boughtGoalIds) return rejected(Rejection.GoalAlreadyBought)
        val savings = state.savings.copy(activeGoalId = goalId)
        return ok(state, state.copy(savings = savings), FeedbackReason.GOAL_CHOSEN)
    }

    private fun deposit(state: GameState, amount: Int): Result {
        if (!isValidAmount(amount)) return rejected(Rejection.InvalidAmount)
        if (!state.week.planConfirmed) return rejected(Rejection.PlanNotConfirmed)
        if (amount > state.balance) {
            val missing = amount - state.balance
            return rejected(Rejection.NotEnoughMoney(missing, waysOut(state, missing, item = null)))
        }
        val balance = state.balance - amount
        val total = state.savings.total + amount
        val week = state.week.copy(deposited = state.week.deposited + amount)
        val after = state.copy(
            balance = balance,
            savings = state.savings.copy(total = total),
            week = week,
            transactions = state.transactions + Transaction(week.number, TxSource.Deposit, -amount, amount, balance, total),
        )
        return ok(state, after, FeedbackReason.DEPOSIT)
    }

    private fun withdraw(state: GameState, amount: Int): Result {
        if (!isValidAmount(amount)) return rejected(Rejection.InvalidAmount)
        if (amount > state.savings.total) return rejected(Rejection.NotEnoughSavings(amount - state.savings.total))
        val balance = state.balance + amount
        val total = state.savings.total - amount
        val week = state.week.copy(withdrawn = state.week.withdrawn + amount)
        val after = state.copy(
            balance = balance,
            savings = state.savings.copy(total = total),
            week = week,
            transactions = state.transactions + Transaction(week.number, TxSource.Withdraw, amount, -amount, balance, total),
        )
        return ok(state, after, FeedbackReason.WITHDRAW)
    }

    /** «Купить мечту»: из копилки, кошелёк и факт «Нужно» / «Хочу» не меняются, это не снятие. */
    private fun buyGoal(state: GameState): Result {
        val goalId = state.savings.activeGoalId ?: return rejected(Rejection.NoActiveGoal)
        val goal = catalog.goal(goalId) ?: return rejected(Rejection.UnknownId(goalId))
        if (goal.cost > state.savings.total) return rejected(Rejection.GoalNotReached(goal.cost - state.savings.total))
        val total = state.savings.total - goal.cost
        val after = state.copy(
            savings = state.savings.copy(
                total = total,
                activeGoalId = null,
                boughtGoalIds = state.savings.boughtGoalIds + goalId,
            ),
            week = state.week.copy(goalBought = true),
            transactions = state.transactions +
                Transaction(state.week.number, TxSource.GoalPurchase(goalId), 0, -goal.cost, state.balance, total),
        )
        return ok(state, after, FeedbackReason.GOAL_BOUGHT)
    }

    // --- Заработок ---

    private fun completeTask(state: GameState, taskId: String, outcome: Outcome): Result {
        val task = catalog.task(taskId) ?: return rejected(Rejection.UnknownId(taskId))
        if (!isTaskOpen(state, taskId)) return rejected(Rejection.TaskClosed)
        // Повторное прохождение — без монет: награда только за первое
        if (taskId in state.tasks) return ok(state, state, FeedbackReason.TASK_REPEAT)
        val balance = state.balance + task.rewardCoins
        val week = state.week.copy(earned = state.week.earned + task.rewardCoins)
        val after = state.copy(
            balance = balance,
            week = week,
            tasks = state.tasks + (taskId to TaskRecord(taskId, task.topic, outcome, week.number)),
            transactions = state.transactions +
                Transaction(week.number, TxSource.TaskReward(taskId), task.rewardCoins, 0, balance, state.savings.total),
        )
        return ok(state, after, FeedbackReason.TASK_REWARD)
    }

    private fun doJob(state: GameState, jobId: String): Result {
        val job = catalog.job(jobId) ?: return rejected(Rejection.UnknownId(jobId))
        if (state.week.jobsDone >= economy.jobsPerWeek) return rejected(Rejection.JobLimitReached)
        val balance = state.balance + job.reward
        val week = state.week.copy(earned = state.week.earned + job.reward, jobsDone = state.week.jobsDone + 1)
        val after = state.copy(
            balance = balance,
            week = week,
            transactions = state.transactions +
                Transaction(week.number, TxSource.Job(jobId), job.reward, 0, balance, state.savings.total),
        )
        return ok(state, after, FeedbackReason.JOB_REWARD)
    }

    // --- Конец недели ---

    private fun closeWeek(state: GameState): Result {
        val week = state.week
        val confirmed = week.planConfirmed
        // Без подтверждённого плана план считается 0 / 0 / 0, а M = 0 — неделя без действий не даёт «План удался»
        val plan = if (confirmed) week.plan ?: Plan() else Plan()

        // N: половинки — еда и уход
        val nHalves = (if (week.foodPoints >= economy.needFoodPoints) 1 else 0) +
            (if (week.carePoints >= economy.needCarePoints) 1 else 0)

        // M = max(0, 1 − D ÷ (2 · допуск)) — хранится дробью mNum / mDen
        val tolerance = economy.tolerance.getValue(state.profile.difficulty)
        require(tolerance > 0) { "Допуск в economy.json должен быть больше нуля" }
        val mDen = 2 * tolerance
        val mNum = if (!confirmed) 0 else {
            val excess = maxOf(0, week.spentNeed - (plan.need + tolerance)) +
                maxOf(0, week.spentWant - (plan.want + tolerance))
            maxOf(0, mDen - excess)
        }

        // S = min(отложено ÷ max(план копилки, минимум), 1); мечта куплена — S = 1
        val saved = savedThisWeek(week)
        val sDen = maxOf(plan.save, economy.minSaveTarget)
        val sNum = if (week.goalBought) sDen else minOf(saved, sDen)

        // GP = wN · n / 2 + wM · mNum / mDen + wS · sNum / sDen — общий знаменатель, округление только итога
        val w = economy.weights
        val den = 2L * mDen * sDen
        val num = w.n.toLong() * nHalves * mDen * sDen + 2L * w.m * mNum * sDen + 2L * w.s * sNum * mDen
        val gp = roundHalfUp(num, den)

        val totalGp = state.pet.totalGp + gp
        val stage = maxOf(state.pet.stage, stageFor(totalGp))   // стадия не понижается

        val statsBefore = state.pet.stats
        val statsAfter = PetStats(
            satiety = decay(statsBefore.satiety, Stat.SATIETY),
            care = decay(statsBefore.care, Stat.CARE),
            mood = decay(statsBefore.mood, Stat.MOOD),
        )
        val petReason = when {
            week.foodPoints < economy.needFoodPoints -> PetReason.HUNGRY
            week.carePoints < economy.needCarePoints -> PetReason.UNKEMPT
            else -> moodReason(petMood(statsBefore))
        }
        val result = WeekResult(
            number = week.number,
            plan = plan,
            planConfirmed = confirmed,
            factNeed = week.spentNeed,
            factWant = week.spentWant,
            factSaved = saved,
            earned = week.earned,
            nPct = nHalves * 50,
            mPct = roundHalfUp(100L * mNum, mDen.toLong()),
            sPct = roundHalfUp(100L * sNum, sDen.toLong()),
            gp = gp,
            totalGp = totalGp,
            stageBefore = state.pet.stage,
            stageAfter = stage,
            statsBefore = statsBefore,
            statsAfter = statsAfter,
            petReason = petReason,
        )

        // Новая неделя: доступно = остаток кошелька + карманные
        val nextNumber = week.number + 1
        val balance = state.balance + economy.pocketMoney
        val after = state.copy(
            pet = state.pet.copy(stats = statsAfter, totalGp = totalGp, stage = stage),
            balance = balance,
            savings = state.savings.copy(recentWeeklySaved = (state.savings.recentWeeklySaved + saved).takeLast(3)),
            week = Week(number = nextNumber, available = balance),
            history = state.history + result,
            transactions = state.transactions +
                Transaction(nextNumber, TxSource.PocketMoney, economy.pocketMoney, 0, balance, state.savings.total),
        )
        return ok(state, after, FeedbackReason.WEEK_CLOSED, result)
    }

    // --- Вспомогательное ---

    private fun stageFor(totalGp: Int): Stage = when {
        totalGp >= economy.stageThresholds.getValue(Stage.ADULT) -> Stage.ADULT
        totalGp >= economy.stageThresholds.getValue(Stage.TEEN) -> Stage.TEEN
        else -> Stage.BABY
    }

    private fun decay(value: Int, stat: Stat): Int =
        maxOf(economy.stats.floor, percentOf(value, economy.stats.decayPct.getValue(stat)))

    private fun withEffects(stats: PetStats, effects: Map<Stat, Int>): PetStats {
        fun add(value: Int, stat: Stat) =
            (value + (effects[stat] ?: 0)).coerceIn(economy.stats.floor, economy.stats.cap)
        return PetStats(add(stats.satiety, Stat.SATIETY), add(stats.care, Stat.CARE), add(stats.mood, Stat.MOOD))
    }

    private fun moodReason(mood: PetMood): PetReason = when (mood) {
        PetMood.SAD -> PetReason.SAD
        PetMood.HAPPY -> PetReason.JOYFUL
        PetMood.CALM -> PetReason.WELL
    }

    private fun savedThisWeek(week: Week): Int = maxOf(0, week.deposited - week.withdrawn)

    private fun isValidAmount(amount: Int) = amount > 0 && amount % 5 == 0

    private fun ceilDiv(a: Int, b: Int): Int = (a + b - 1) / b

    /** Округление «половина вверх» дроби num / den (docs/02-экономика.md). */
    private fun roundHalfUp(num: Long, den: Long): Int = ((2 * num + den) / (2 * den)).toInt()

    private fun rejected(reason: Rejection) = Result.Rejected(reason)

    private fun ok(before: GameState, after: GameState, reason: FeedbackReason, weekResult: WeekResult? = null): Result {
        val b = before.pet.stats
        val a = after.pet.stats
        val changes = mapOf(
            Stat.SATIETY to a.satiety - b.satiety,
            Stat.CARE to a.care - b.care,
            Stat.MOOD to a.mood - b.mood,
        ).filterValues { it != 0 }
        val feedback = Feedback(before.balance, after.balance, before.savings.total, after.savings.total, changes, reason, weekResult)
        return Result.Ok(after, feedback)
    }
}
