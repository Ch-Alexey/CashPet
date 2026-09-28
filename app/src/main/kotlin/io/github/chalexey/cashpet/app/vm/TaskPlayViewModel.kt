package io.github.chalexey.cashpet.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.chalexey.cashpet.app.appContainer
import io.github.chalexey.cashpet.app.store.GameStore
import io.github.chalexey.cashpet.content.GameContent
import io.github.chalexey.cashpet.core.engine.Action
import io.github.chalexey.cashpet.core.engine.Result
import io.github.chalexey.cashpet.core.task.Choice
import io.github.chalexey.cashpet.core.task.TaskSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Прохождение задания (docs/01-функционал.md, раздел 4.3; docs/07-формат-заданий.md): ситуация → выбор →
 * последствие в числах → разбор → «Попробовать иначе» или «Дальше». Учебный кошелёк живёт в [TaskSession]
 * и настоящий баланс не трогает; на баланс идёт только награда — с панелью «что изменилось».
 * [uiState] = null — профиля или задания нет, экран возвращается к списку.
 */
class TaskPlayViewModel(
    private val content: GameContent,
    private val store: GameStore,
    taskId: String,
) : ViewModel() {

    private val session: TaskSession?
    private val neighborLine: NeighborTipUi?

    private var blocked: Choice.Blocked? = null         // открыт диалог варианта-заглушки
    private var finished = false
    private var feedback: FeedbackUi? = null

    init {
        val state = store.state.value
        val task = content.tasks.firstOrNull { it.id == taskId }
        session = if (state == null || task == null) null else TaskSession(
            task = task,
            difficulty = state.profile.difficulty,
            petName = state.pet.name,
            values = mapOf("playerName" to state.profile.playerName),
        )
        neighborLine = task?.neighborLines?.firstOrNull()?.let { line ->
            content.neighbor(line.character)?.let { n ->
                NeighborTipUi(characterId = n.id, name = n.name, text = state?.let { line.text.withNames(it) } ?: line.text)
            }
        }
    }

    private val _uiState = MutableStateFlow(session?.let(::toUi))
    val uiState: StateFlow<TaskPlayUiState?> = _uiState.asStateFlow()

    fun choose(optionId: String) {
        val s = session ?: return
        if (s.isFinished || s.step.chosen != null) return
        blocked = s.choose(optionId) as? Choice.Blocked
        refresh()
    }

    /**
     * Кнопка в диалоге заглушки. Совпадает с вариантом шага («Купить корм») — это его выбор,
     * иначе («Мяч — в „Хочу потом“») диалог закрывается и ребёнок выбирает снова.
     */
    fun onFollowup(button: String) {
        val s = session ?: return
        blocked = null
        val option = s.step.options.firstOrNull { it.label == button }
        if (option != null) choose(option.id) else refresh()
    }

    /** «Попробовать иначе»: тот же шаг, учебный кошелёк — к исходному. */
    fun retry() {
        val s = session ?: return
        if (s.isFinished || s.step.chosen == null) return
        s.retry()
        refresh()
    }

    /** «Дальше»: следующий шаг, а после последнего — награда на настоящий баланс (только за первое прохождение). */
    fun next() {
        val s = session ?: return
        if (s.isFinished || s.step.chosen == null) return
        if (s.next()) {
            refresh()
            return
        }
        viewModelScope.launch {
            val result = store.dispatch(Action.CompleteTask(s.task.id, s.finalOutcome))
            finished = true
            feedback = (result as? Result.Ok)?.let { feedbackUi(it, content, amount = s.task.reward.coins) }
            refresh()
        }
    }

    private fun refresh() {
        _uiState.value = session?.let(::toUi)
    }

    private fun toUi(s: TaskSession): TaskPlayUiState {
        val step = s.step
        val chosen = step.chosen
        val stub = blocked
        return TaskPlayUiState(
            title = s.task.title,
            step = step.number,
            steps = step.count,
            intro = step.intro,
            situation = step.situation,
            wallet = step.wallet,
            savings = step.savings,
            options = step.options.map { OptionUi(it.id, it.label) },
            result = when {
                chosen != null -> ChoiceResultUi(
                    consequence = chosen.consequence,
                    effects = chosen.effects,
                    feedback = chosen.feedback,
                    canRetry = chosen.canRetry,
                    recovery = chosen.recovery,
                    followupButtons = emptyList(),
                )
                stub != null -> ChoiceResultUi(
                    consequence = null,
                    effects = emptyMap(),
                    feedback = stub.feedback,
                    canRetry = false,
                    recovery = null,
                    followupButtons = stub.followup?.buttons.orEmpty(),
                    followupPrompt = stub.followup?.prompt,
                )
                else -> null
            },
            neighborLine = neighborLine,
            finished = finished,
            feedback = feedback,
        )
    }

    companion object {
        /** Фабрика для экрана: `viewModel(factory = TaskPlayViewModel.factory(taskId))`. */
        fun factory(taskId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = this[APPLICATION_KEY]!!.appContainer
                TaskPlayViewModel(c.content, c.gameStore, taskId)
            }
        }
    }
}
