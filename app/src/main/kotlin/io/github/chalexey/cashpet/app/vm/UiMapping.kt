package io.github.chalexey.cashpet.app.vm

import io.github.chalexey.cashpet.content.GameContent
import io.github.chalexey.cashpet.core.engine.GameEngine
import io.github.chalexey.cashpet.core.engine.Rejection
import io.github.chalexey.cashpet.core.engine.Result
import io.github.chalexey.cashpet.core.model.GameState
import io.github.chalexey.cashpet.core.model.GoalDef
import io.github.chalexey.cashpet.core.task.TaskDef

// Общие преобразования GameState → UiState: верхняя панель, карточки заданий, подстановки.
// Нужны почти всем ViewModel, поэтому здесь, а не в каждой

/** Верхняя панель: баланс, копилка, текущая цель и её полоска. */
fun topBar(state: GameState, content: GameContent): TopBarUi {
    val goal = state.savings.activeGoalId?.let(content.catalog::goal)
    return TopBarUi(
        balance = state.balance,
        savings = state.savings.total,
        goalName = goal?.name,
        goalCost = goal?.cost,
        goalProgressPct = if (goal == null) 0 else minOf(100, state.savings.total * 100 / goal.cost),
    )
}

/** Открытые и ещё не пройденные задания — в порядке tasks.json. */
fun openTasks(state: GameState, content: GameContent, engine: GameEngine): List<TaskCardUi> =
    content.tasks
        .filter { engine.isTaskOpen(state, it.id) && it.id !in state.tasks }
        .map(::taskCard)

/** Пройденные задания — в порядке tasks.json. Не исчезают, их можно пройти ещё раз без монет. */
fun doneTasks(state: GameState, content: GameContent): List<TaskCardUi> =
    content.tasks.filter { it.id in state.tasks }.map(::taskCard)

private fun taskCard(task: TaskDef) =
    TaskCardUi(taskId = task.id, title = task.title, topic = task.topic, reward = task.reward.coins)

/** Имена игрока и кота в тексте из контента; остальные подстановки — там, где они известны. */
fun String.withNames(state: GameState): String =
    replace("{petName}", state.pet.name).replace("{playerName}", state.profile.playerName)

/**
 * Панель «что изменилось» после действия (п. 2.5.9 ТЗ): было → стало и фраза из texts.json.
 * [amount] и [item] — для подстановок {amount} и {item}: сумма взноса, название товара или цели.
 */
fun feedbackUi(ok: Result.Ok, content: GameContent, amount: Int? = null, item: String? = null): FeedbackUi {
    val f = ok.feedback
    val text = content.texts.feedback.getValue(f.reason)
        .replace("{amount}", amount?.toString() ?: "")
        .replace("{item}", item ?: "")
        .withNames(ok.state)
    return FeedbackUi(f.balanceBefore, f.balanceAfter, f.savingsBefore, f.savingsAfter, f.statChanges, text)
}

/** Итог последнего действия на экране: панель «что изменилось» или причина отказа. Показали — сбросили. */
data class ActionMessage(val feedback: FeedbackUi? = null, val rejection: Rejection? = null)

/** Карточка цели: «накоплено N из стоимости», полоска из клеточек по 10. Копилка общая — N у всех целей одно. */
fun goalUi(goal: GoalDef, state: GameState): GoalUi {
    val saved = minOf(state.savings.total, goal.cost)
    return GoalUi(
        id = goal.id,
        name = goal.name,
        cost = goal.cost,
        saved = saved,
        cells = goal.cost / CELL,
        filledCells = saved / CELL,
        bought = goal.id in state.savings.boughtGoalIds,
    )
}

private const val CELL = 10              // docs/01-функционал.md, «Копилка»: вид полоски, не экономика — не в конфиге
