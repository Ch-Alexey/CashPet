package io.github.chalexey.cashpet.app.vm

import io.github.chalexey.cashpet.content.GameContent
import io.github.chalexey.cashpet.core.engine.GameEngine
import io.github.chalexey.cashpet.core.model.GameState

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
        .map { TaskCardUi(taskId = it.id, title = it.title, topic = it.topic, reward = it.reward.coins) }

/** Имена игрока и кота в тексте из контента; остальные подстановки — там, где они известны. */
fun String.withNames(state: GameState): String =
    replace("{petName}", state.pet.name).replace("{playerName}", state.profile.playerName)
